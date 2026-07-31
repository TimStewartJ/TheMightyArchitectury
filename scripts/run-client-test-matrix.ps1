param(
    [string[]]$Versions = @('1.19.4', '1.20.1', '1.20.2', '1.20.4', '1.20.6', '1.21.1', '1.21.4', '1.21.6', '1.21.8', '1.21.10', '1.21.11', '26.1', '26.2'),
    [string[]]$Loaders = @('fabric', 'neoforge', 'forge'),
    [ValidateSet('dev', 'prod')]
    [string]$Mode = 'dev',
    [int]$Port = 25565,
    [int]$ClientTimeoutSeconds = 600,
    [switch]$KeepOpen
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force
Assert-TestMatrixPowerShell

$Versions = Expand-TestListArgument -Value $Versions
$Loaders = Expand-TestListArgument -Value $Loaders -Allowed @('fabric', 'neoforge', 'forge')

# `prod` launches the packaged jars. On Fabric that is Loom's ClientProductionRunTask; on the
# Forge family, where ModDevGradle ships no equivalent, it is HeadlessMc installing a real
# Minecraft plus loader and launching the jars out of a mods folder. Both drive the same harness.
$RepoRoot = Get-TestMatrixRepoRoot -ScriptRoot $PSScriptRoot
$BuildRoot = Join-Path $RepoRoot 'build'
$RuntimeRoot = Join-Path $BuildRoot 'client-test-runtime'
$ResultsRoot = Join-Path $BuildRoot $(if ($Mode -eq 'prod') { 'prod-client-test-results' } else { 'client-test-results' })
$Gradle = Get-TestGradleCommand -RepoRoot $RepoRoot

if ($KeepOpen -and ($Versions.Count -ne 1 -or (Select-TestVersionLoaders -Version $Versions[0] -Requested $Loaders).Count -ne 1)) {
    throw '-KeepOpen requires exactly one version and one loader. Start multiple invocations with distinct -Port values for simultaneous clients.'
}

function Copy-ResultArtifacts {
    param(
        [string]$Version,
        [string]$Loader,
        [string]$ResultPath,
        [string]$RunDirectory,
        [string]$ServerLog,
        [string]$GradleStdout,
        [string]$GradleStderr
    )

    $target = Join-Path (Join-Path $ResultsRoot $Version) $Loader
    Remove-Item $target -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $target | Out-Null
    if (Test-Path $ResultPath) {
        Copy-Item $ResultPath (Join-Path $target 'result.json')
        $result = Get-Content $ResultPath -Raw | ConvertFrom-Json
        foreach ($property in @($result.PSObject.Properties.Name | Where-Object { $_ -like '*Screenshot' })) {
            $source = $result.$property
            if ($source -and (Test-Path $source)) {
                Copy-Item $source (Join-Path $target "$property.png")
            }
        }
    }
    $clientLog = Join-Path (Join-Path $RunDirectory 'logs') 'latest.log'
    if (Test-Path $clientLog) {
        Copy-Item $clientLog (Join-Path $target 'client.log')
    }
    if (Test-Path $ServerLog) {
        Copy-Item $ServerLog (Join-Path $target 'server.log')
    }
    if (Test-Path $GradleStdout) {
        Copy-Item $GradleStdout (Join-Path $target 'gradle.stdout.log')
    }
    if (Test-Path $GradleStderr) {
        Copy-Item $GradleStderr (Join-Path $target 'gradle.stderr.log')
    }
}

function Start-ForgeFamilyProductionClient {
    <#
        Builds this node's production jars, makes sure HeadlessMc has a matching Minecraft and
        loader installed, and launches the client with the jars in its mods folder.

        The Gradle call only produces artifacts - it never launches anything - so a CI job that has
        already built this node finds everything up to date.
    #>
    param(
        [string]$Version,
        [string]$Loader,
        [int]$ServerPort,
        [string]$ResultPath,
        [string]$RunDirectory,
        [string]$StandardOutput,
        [string]$StandardError,
        [switch]$KeepOpen
    )

    $properties = Get-TestNodeProperties -RepoRoot $RepoRoot -Version $Version
    $javaVersion = [int]$properties.java_version

    $buildLog = Join-Path (Split-Path $StandardOutput) 'artifacts.log'
    # gradlew is invoked by absolute path, but Gradle still locates the project from the working
    # directory, so this has to run from the repository root.
    Push-Location $RepoRoot
    try {
        & $Gradle ":${Loader}:${Version}:build" ":${Loader}:${Version}:buildClientTestMod" `
            '--console=plain' '--no-daemon' `
            '-Porg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64,JAVA_HOME_21_X64,JAVA_HOME_25_X64' `
            *> $buildLog
        $buildExit = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($buildExit -ne 0) {
        $tail = if (Test-Path $buildLog) { (Get-Content $buildLog -Tail 30) -join "`n" } else { '' }
        throw "Gradle artifact build for $Version/$Loader exited $buildExit`n$tail"
    }

    $libraries = Join-Path (Join-Path (Join-Path (Join-Path $RepoRoot $Loader) 'versions') $Version) 'build/libs'
    $modJar = Get-ChildItem $libraries -Filter "*-$Loader.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|dev|raw|client-test|server-test' } | Select-Object -First 1
    $companionJar = Get-ChildItem $libraries -Filter "*-$Loader-client-test.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'client-test-dev' } | Select-Object -First 1
    if (-not $modJar) { throw "No production jar for $Version/$Loader in $libraries" }
    if (-not $companionJar) { throw "No client-test companion jar for $Version/$Loader in $libraries" }

    $launchVersion = Initialize-TestHeadlessMcInstance -RepoRoot $RepoRoot `
        -MinecraftVersion $properties.minecraft_version -Loader $Loader `
        -LoaderUid (Get-TestLoaderUid -Properties $properties -Loader $Loader) `
        -JavaVersion $javaVersion -GameDirectory $RunDirectory

    Write-Host "  launching $launchVersion"
    return Start-TestHeadlessMcClient -RepoRoot $RepoRoot -LaunchVersion $launchVersion `
        -JavaVersion $javaVersion -ModJars @($modJar.FullName, $companionJar.FullName) `
        -GameDirectory $RunDirectory -ResultPath $ResultPath -ServerPort $ServerPort `
        -StandardOutput $StandardOutput -StandardError $StandardError -KeepOpen:$KeepOpen
}

function Invoke-ClientTest {
    param(
        [string]$Version,
        [string]$Loader,
        [string]$ServerLog,
        [int]$ServerPort,
        [System.Diagnostics.Process]$ServerProcess,
        [switch]$KeepOpen
    )

    $projectDirectory = Join-Path (Join-Path (Join-Path $RepoRoot $Loader) 'versions') $Version
    $projectBuildDirectory = Join-Path $projectDirectory 'build'
    # Dev and production runs must not share a result file or a game directory: the verdict is read
    # back from disk, and a stale one from the other mode would be indistinguishable from this run's.
    $prefix = if ($Mode -eq 'prod') { 'prod-client-test' } else { 'client-test' }
    $clientTestDirectoryName = if ($KeepOpen) { "${prefix}-${ServerPort}" } else { $prefix }
    $runDirectoryName = if ($KeepOpen) { "run-${prefix}-${ServerPort}" } else { "run-${prefix}" }
    $gradleTask = if ($Mode -eq 'prod') { 'runProductionClientTest' } else { 'runAutomatedClientTest' }
    # Loom launches the packaged Fabric jars itself; the Forge family has no such task and goes
    # through HeadlessMc instead.
    $useHeadlessMc = ($Mode -eq 'prod' -and $Loader -ne 'fabric')
    $clientTestBuildDirectory = Join-Path $projectBuildDirectory $clientTestDirectoryName
    $resultPath = Join-Path $clientTestBuildDirectory 'result.json'
    $runDirectory = Join-Path $projectDirectory $runDirectoryName
    $gradleStdout = Join-Path $clientTestBuildDirectory 'gradle.stdout.log'
    $gradleStderr = Join-Path $clientTestBuildDirectory 'gradle.stderr.log'
    New-Item -ItemType Directory -Force -Path (Split-Path $gradleStdout) | Out-Null
    Remove-Item $resultPath -Force -ErrorAction SilentlyContinue
    Remove-Item $runDirectory -Recurse -Force -ErrorAction SilentlyContinue
    Write-TestClientOptions -MinecraftDirectory $runDirectory
    Remove-Item $gradleStdout, $gradleStderr -Force -ErrorAction SilentlyContinue

    Write-Host "=== CLIENT TEST ($Mode) $Version / $Loader ==="
    $env:MIGHTYARCHITECT_CLIENT_TEST_SERVER = "127.0.0.1:$ServerPort"
    $env:MIGHTYARCHITECT_CLIENT_TEST_KEEP_OPEN = $KeepOpen.IsPresent.ToString().ToLowerInvariant()
    if ($KeepOpen) {
        $env:MIGHTYARCHITECT_CLIENT_TEST_SESSION_ID = $ServerPort.ToString()
    }
    $gradleProcess = $null
    $leaveRunning = $false
    $artifactsCopied = $false
    try {
        if ($useHeadlessMc) {
            $gradleProcess = Start-ForgeFamilyProductionClient -Version $Version -Loader $Loader `
                -ServerPort $ServerPort -ResultPath $resultPath -RunDirectory $runDirectory `
                -StandardOutput $gradleStdout -StandardError $gradleStderr -KeepOpen:$KeepOpen
        } else {
        $gradleArguments = [System.Collections.Generic.List[string]]::new()
        $gradleArguments.Add(":${Loader}:${Version}:${gradleTask}")
        $gradleArguments.Add('--console=plain')
        $gradleArguments.Add('--no-daemon')
        # Fabric runs the companion from its own source set; the Forge-family loaders load it as a
        # local runtime mod jar instead. The production run always consumes the packaged companion
        # jar, so it needs no such flag.
        if ($Mode -eq 'dev' -and ($Loader -eq 'neoforge' -or $Loader -eq 'forge')) {
            $gradleArguments.Add('-PenableClientTestMod=true')
        }
        $gradleArguments.Add('-Porg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64,JAVA_HOME_21_X64,JAVA_HOME_25_X64')
        $hiddenWindow = Get-TestHiddenWindowOption
        $gradleProcess = Start-Process -FilePath $Gradle `
            -ArgumentList $gradleArguments `
            -WorkingDirectory $RepoRoot `
            -RedirectStandardOutput $gradleStdout `
            -RedirectStandardError $gradleStderr `
            @hiddenWindow `
            -PassThru
        }

        if ($KeepOpen) {
            $deadline = (Get-Date).AddSeconds($ClientTimeoutSeconds)
            while ((Get-Date) -lt $deadline -and -not (Test-Path $resultPath)) {
                if ($gradleProcess.HasExited) {
                    $tail = if (Test-Path $gradleStdout) { (Get-Content $gradleStdout -Tail 30) -join "`n" } else { '' }
                    throw "Client exited before writing a keep-open result (code $($gradleProcess.ExitCode))`n$tail"
                }
                Start-Sleep -Seconds 2
            }
            if (-not (Test-Path $resultPath)) {
                throw "Client test exceeded ${ClientTimeoutSeconds}s timeout"
            }
        } else {
            # The client writes its verdict to result.json and then asks Minecraft to stop. On 26.2
            # that teardown is unreliable: the Fabric client misses vanilla's 15-second post-main
            # deadline and gets System.exit(-8) from ClientShutdownWatchdog, and the NeoForge client
            # can hang in shutdown indefinitely. Neither shows a mod frame in the dump, and the mod
            # starts no threads of its own. What this matrix verifies is the verdict, so wait for the
            # verdict first and treat process teardown as a bounded courtesy afterwards.
            $deadline = (Get-Date).AddSeconds($ClientTimeoutSeconds)
            while ((Get-Date) -lt $deadline -and -not $gradleProcess.HasExited -and -not (Test-Path $resultPath)) {
                Start-Sleep -Seconds 2
            }

            $stoppedAfterVerdict = $false
            if (-not $gradleProcess.HasExited) {
                if (-not (Test-Path $resultPath)) {
                    throw "Client test exceeded ${ClientTimeoutSeconds}s timeout"
                }
                # Verdict is in. Give the client a short window to exit on its own, then stop waiting.
                if (-not $gradleProcess.WaitForExit(60 * 1000)) {
                    Write-Host "WARN $Version / $Loader - client wrote its result but did not exit; stopping it" -ForegroundColor Yellow
                    Stop-TestProcessTree -Process $gradleProcess
                    $stoppedAfterVerdict = $true
                }
            }

            $gradleExit = if ($gradleProcess.HasExited) { $gradleProcess.ExitCode } else { 0 }
            if ($gradleExit -ne 0 -and -not $stoppedAfterVerdict) {
                $stdoutText = if (Test-Path $gradleStdout) { Get-Content $gradleStdout -Raw } else { '' }
                $shutdownWatchdog = $stdoutText -match 'Client shutdown from post-main'
                $resultPassed = $false
                if (Test-Path $resultPath) {
                    $resultPassed = ((Get-Content $resultPath -Raw | ConvertFrom-Json).status -eq 'passed')
                }

                if ($shutdownWatchdog -and $resultPassed) {
                    Write-Host "WARN $Version / $Loader - checks passed; client missed the 15s shutdown deadline and vanilla force-exited it ($gradleExit)" -ForegroundColor Yellow
                } else {
                    $stdoutTail = if (Test-Path $gradleStdout) { (Get-Content $gradleStdout -Tail 30) -join "`n" } else { '' }
                    $stderrTail = if (Test-Path $gradleStderr) { (Get-Content $gradleStderr -Tail 30) -join "`n" } else { '' }
                    if ($stdoutTail) { Write-Host $stdoutTail }
                    if ($stderrTail) { Write-Host $stderrTail -ForegroundColor Red }
                    # The tail travels in the message so infrastructure failures inside the launcher
                    # (asset downloads and the like) can be classified as retryable, the way the
                    # server matrix already does.
                    $launcher = if ($useHeadlessMc) { 'HeadlessMc' } else { 'Gradle' }
                    throw "$launcher client test exited $gradleExit`n$stdoutTail`n$stderrTail"
                }
            }
        }

        if (-not (Test-Path $resultPath)) {
            throw "Client test produced no result: $resultPath"
        }

        $result = Get-Content $resultPath -Raw | ConvertFrom-Json
        if ($result.status -ne 'passed') {
            throw "Client test failed in $($result.stage): $($result.error)"
        }
        if ($KeepOpen -and -not $result.keepOpen) {
            throw 'Client passed but did not acknowledge keep-open mode'
        }

        Write-Host "PASS $Version / $Loader ($($result.checks.Count) checks)"
        if ($KeepOpen) {
            Copy-ResultArtifacts $Version $Loader $resultPath $runDirectory $ServerLog $gradleStdout $gradleStderr
            $artifactsCopied = $true
            $session = [pscustomobject]@{
                mode = $Mode
                version = $Version
                loader = $Loader
                port = $ServerPort
                serverProcess = Get-TestProcessIdentity -Process $ServerProcess
                clientProcesses = @(Get-TestProcessIdentity -Process $gradleProcess)
                createdAt = (Get-Date).ToString('o')
            }
            $manifest = Write-TestSessionManifest -RepoRoot $RepoRoot `
                -FileName "$Mode-$Version-$Loader-$ServerPort.json" -Session $session
            $leaveRunning = $true
            return [pscustomobject]@{
                ManifestPath = $manifest
                Session = $session
            }
        }
    } finally {
        Remove-Item Env:MIGHTYARCHITECT_CLIENT_TEST_SERVER -ErrorAction SilentlyContinue
        Remove-Item Env:MIGHTYARCHITECT_CLIENT_TEST_KEEP_OPEN -ErrorAction SilentlyContinue
        Remove-Item Env:MIGHTYARCHITECT_CLIENT_TEST_SESSION_ID -ErrorAction SilentlyContinue
        if (-not $leaveRunning) {
            Stop-TestProcessTree -Process $gradleProcess
        }
        if (-not $artifactsCopied) {
            Copy-ResultArtifacts $Version $Loader $resultPath $runDirectory $ServerLog $gradleStdout $gradleStderr
        }
    }
}

New-Item -ItemType Directory -Force -Path $RuntimeRoot, $ResultsRoot | Out-Null
$failures = [System.Collections.Generic.List[string]]::new()
$keptOpenSession = $null

$nodeCount = 0
foreach ($version in $Versions) {
    $properties = Get-TestNodeProperties -RepoRoot $RepoRoot -Version $version
    $server = $null
    try {
        # Key the server runtime directory by port so concurrent invocations on different
        # ports never share a world/logs/server.properties directory.
        $serverNodeId = if ($KeepOpen) { "$version-$((Select-TestVersionLoaders -Version $version -Requested $Loaders)[0])-$Port" } else { "$version-$Port" }
        $server = Start-TestVanillaServer -NodeId $serverNodeId -Properties $properties -RuntimeRoot $RuntimeRoot `
            -Port $Port -Motd 'Mighty Architect Client Test'
        foreach ($loader in (Select-TestVersionLoaders -Version $version -Requested $Loaders)) {
            $nodeCount++
            $passed = $false
            for ($attempt = 1; $attempt -le 2 -and -not $passed; $attempt++) {
                try {
                    $clientSession = Invoke-ClientTest -Version $version -Loader $loader -ServerLog $server.Log `
                        -ServerPort $Port -ServerProcess $server.Process -KeepOpen:$KeepOpen
                    $passed = $true
                    if ($KeepOpen) {
                        $keptOpenSession = $clientSession
                    }
                } catch {
                    $reason = $_.Exception.Message
                    if ($attempt -lt 2 -and (Test-TestRetryableFailure -Message $reason)) {
                        Write-Host "RETRY $version / $loader after infrastructure failure: $reason" -ForegroundColor Yellow
                        Start-Sleep -Seconds 2
                    } else {
                        $message = "$version / $loader - $reason"
                        $failures.Add($message)
                        Write-Host "FAIL $message" -ForegroundColor Red
                        break
                    }
                }
            }
        }
    } catch {
        $message = "$version / server - $($_.Exception.Message)"
        $failures.Add($message)
        Write-Host "FAIL $message" -ForegroundColor Red
    } finally {
        if (-not $keptOpenSession) {
            Stop-TestProcessTree -Process $server.Process
        }
    }
}

if ($failures.Count -gt 0) {
    throw "Client-test matrix failed:`n - $($failures -join "`n - ")"
}

if ($keptOpenSession) {
    $session = $keptOpenSession.Session
    $stopScript = Join-Path $PSScriptRoot 'stop-kept-open-clients.ps1'
    Write-Host "KEEP OPEN $Mode $($session.version) / $($session.loader)"
    Write-Host "Server: 127.0.0.1:$Port (PID $($session.serverProcess.processId))"
    Write-Host "Manifest: $($keptOpenSession.ManifestPath)"
    Write-Host "Stop with: pwsh -File `"$stopScript`" -ManifestPath `"$($keptOpenSession.ManifestPath)`""
    return
}

Write-Host "All $nodeCount client-test nodes passed ($Mode)."
