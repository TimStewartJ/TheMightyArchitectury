param(
    [string[]]$Versions = @('1.21.1', '1.21.4', '1.21.6', '1.21.8', '1.21.10', '1.21.11', '26.1'),
    [string[]]$Loaders = @('fabric', 'neoforge'),
    [int]$Port = 25565,
    [int]$ClientTimeoutSeconds = 600,
    [switch]$KeepOpen
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force
Assert-TestMatrixPowerShell

$Versions = Expand-TestListArgument -Value $Versions
$Loaders = Expand-TestListArgument -Value $Loaders -Allowed @('fabric', 'neoforge')

$RepoRoot = Get-TestMatrixRepoRoot -ScriptRoot $PSScriptRoot
$BuildRoot = Join-Path $RepoRoot 'build'
$RuntimeRoot = Join-Path $BuildRoot 'client-test-runtime'
$ResultsRoot = Join-Path $BuildRoot 'client-test-results'
$Gradle = Get-TestGradleCommand -RepoRoot $RepoRoot

if ($KeepOpen -and ($Versions.Count -ne 1 -or $Loaders.Count -ne 1)) {
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
    $clientTestDirectoryName = if ($KeepOpen) { "client-test-${ServerPort}" } else { 'client-test' }
    $runDirectoryName = if ($KeepOpen) { "run-client-test-${ServerPort}" } else { 'run-client-test' }
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

    Write-Host "=== CLIENT TEST $Version / $Loader ==="
    $env:MIGHTYARCHITECT_CLIENT_TEST_SERVER = "127.0.0.1:$ServerPort"
    $env:MIGHTYARCHITECT_CLIENT_TEST_KEEP_OPEN = $KeepOpen.IsPresent.ToString().ToLowerInvariant()
    if ($KeepOpen) {
        $env:MIGHTYARCHITECT_CLIENT_TEST_SESSION_ID = $ServerPort.ToString()
    }
    $gradleProcess = $null
    $leaveRunning = $false
    $artifactsCopied = $false
    try {
        $gradleArguments = [System.Collections.Generic.List[string]]::new()
        $gradleArguments.Add(":${Loader}:${Version}:runAutomatedClientTest")
        $gradleArguments.Add('--console=plain')
        $gradleArguments.Add('--no-daemon')
        if ($Loader -eq 'neoforge') {
            $gradleArguments.Add('-PenableClientTestMod=true')
        }
        $gradleArguments.Add('-Porg.gradle.java.installations.fromEnv=JAVA_HOME_21_X64,JAVA_HOME_25_X64')
        $hiddenWindow = Get-TestHiddenWindowOption
        $gradleProcess = Start-Process -FilePath $Gradle `
            -ArgumentList $gradleArguments `
            -WorkingDirectory $RepoRoot `
            -RedirectStandardOutput $gradleStdout `
            -RedirectStandardError $gradleStderr `
            @hiddenWindow `
            -PassThru

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
            if (-not $gradleProcess.WaitForExit($ClientTimeoutSeconds * 1000)) {
                throw "Client test exceeded ${ClientTimeoutSeconds}s timeout"
            }
            $gradleExit = $gradleProcess.ExitCode
            if ($gradleExit -ne 0) {
                if (Test-Path $gradleStdout) {
                    Get-Content $gradleStdout -Tail 30 | ForEach-Object { Write-Host $_ }
                }
                if (Test-Path $gradleStderr) {
                    Get-Content $gradleStderr -Tail 30 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
                }
                throw "Gradle client test exited $gradleExit"
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
                mode = 'dev'
                version = $Version
                loader = $Loader
                port = $ServerPort
                serverProcess = Get-TestProcessIdentity -Process $ServerProcess
                clientProcesses = @(Get-TestProcessIdentity -Process $gradleProcess)
                instancePath = $null
                createdAt = (Get-Date).ToString('o')
            }
            $manifest = Write-TestSessionManifest -RepoRoot $RepoRoot `
                -FileName "dev-$Version-$Loader-$ServerPort.json" -Session $session
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

foreach ($version in $Versions) {
    $properties = Get-TestNodeProperties -RepoRoot $RepoRoot -Version $version
    $server = $null
    try {
        # Key the server runtime directory by port so concurrent invocations on different
        # ports never share a world/logs/server.properties directory.
        $serverNodeId = if ($KeepOpen) { "$version-$($Loaders[0])-$Port" } else { "$version-$Port" }
        $server = Start-TestVanillaServer -NodeId $serverNodeId -Properties $properties -RuntimeRoot $RuntimeRoot `
            -Port $Port -Motd 'Mighty Architect Client Test'
        foreach ($loader in $Loaders) {
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
                    if ($attempt -lt 2) {
                        Write-Host "RETRY $version / $loader after: $($_.Exception.Message)" -ForegroundColor Yellow
                        Start-Sleep -Seconds 2
                    } else {
                        $message = "$version / $loader - $($_.Exception.Message)"
                        $failures.Add($message)
                        Write-Host "FAIL $message" -ForegroundColor Red
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
    Write-Host "KEEP OPEN dev $($session.version) / $($session.loader)"
    Write-Host "Server: 127.0.0.1:$Port (PID $($session.serverProcess.processId))"
    Write-Host "Manifest: $($keptOpenSession.ManifestPath)"
    Write-Host "Stop with: pwsh -File `"$stopScript`" -ManifestPath `"$($keptOpenSession.ManifestPath)`""
    return
}

Write-Host "All $($Versions.Count * $Loaders.Count) client-test nodes passed."
