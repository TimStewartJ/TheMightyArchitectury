param(
    [string[]]$Versions = @('1.19.4', '1.20.1', '1.20.2', '1.20.4', '1.20.6', '1.21.1', '1.21.4', '1.21.6', '1.21.8', '1.21.10', '1.21.11', '26.1', '26.2', '26.3'),
    [string[]]$Loaders = @('fabric', 'neoforge', 'forge'),
    [int]$TimeoutSeconds = 600,
    # After the print test passes, keep that modded server up and join it with a client that has no
    # mods at all - the player a server running the mod must not turn away. Needs a display.
    [switch]$JoinTest,
    [int]$JoinTimeoutSeconds = 480,
    [int]$JoinStallSeconds = 120
)

$ErrorActionPreference = 'Stop'

Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force
Assert-TestMatrixPowerShell

$Versions = Expand-TestListArgument -Value $Versions
$Loaders = Expand-TestListArgument -Value $Loaders -Allowed @('fabric', 'neoforge', 'forge')

$RepoRoot = Get-TestMatrixRepoRoot -ScriptRoot $PSScriptRoot
$ResultsRoot = Join-Path (Join-Path $RepoRoot 'build') 'server-test-results'
$Gradle = Get-TestGradleCommand -RepoRoot $RepoRoot

function Copy-ServerArtifacts {
    param(
        [string]$Version,
        [string]$Loader,
        [string]$RunDirectory,
        [string]$GradleStdout,
        [string]$GradleStderr
    )

    $target = Join-Path (Join-Path $ResultsRoot $Version) $Loader
    Remove-Item $target -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $target | Out-Null
    $serverLog = Join-Path (Join-Path $RunDirectory 'logs') 'latest.log'
    if (Test-Path $serverLog) {
        Copy-Item $serverLog (Join-Path $target 'server.log')
    }
    if (Test-Path $GradleStdout) {
        Copy-Item $GradleStdout (Join-Path $target 'gradle.stdout.log')
    }
    if (Test-Path $GradleStderr) {
        Copy-Item $GradleStderr (Join-Path $target 'gradle.stderr.log')
    }
    $crashDirectory = Join-Path $RunDirectory 'crash-reports'
    if (Test-Path $crashDirectory) {
        Copy-Item $crashDirectory (Join-Path $target 'crash-reports') -Recurse
    }
}

function Get-FreeTcpPort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    try {
        return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

<#
    Joins the running modded server with a client that has no mods (see runModlessJoinClient in
    fabric/build.gradle) and waits for the server to admit it. The client is always the Fabric
    node's, whatever loader the server runs: it carries nothing a server could recognise.
#>
function Invoke-ModlessJoin {
    param(
        [string]$Version,
        [string]$Loader,
        [string]$ServerLog,
        [int]$Port,
        [System.Diagnostics.Process]$ServerProcess
    )

    $player = 'VanillaJoiner'
    $fabricProject = Join-Path (Join-Path (Join-Path $RepoRoot 'fabric') 'versions') $Version
    $runDirectory = Join-Path $fabricProject 'run-join-test'
    $outputDirectory = Join-Path (Join-Path $fabricProject 'build') 'join-test'
    $clientStdout = Join-Path $outputDirectory 'gradle.stdout.log'
    $clientStderr = Join-Path $outputDirectory 'gradle.stderr.log'
    Remove-Item $runDirectory, $outputDirectory -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $runDirectory, $outputDirectory | Out-Null
    Write-TestClientOptions -MinecraftDirectory $runDirectory

    # Only what the server logs from here on is about this client.
    $skip = @(Get-Content $ServerLog -ErrorAction SilentlyContinue).Count
    $env:MIGHTYARCHITECT_JOIN_TEST_SERVER = "127.0.0.1:$Port"
    $arguments = @(
        ":fabric:${Version}:runModlessJoinClient",
        '--console=plain',
        '--no-daemon',
        '-Porg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64,JAVA_HOME_21_X64,JAVA_HOME_25_X64'
    )
    $hiddenWindow = Get-TestHiddenWindowOption
    Write-Host "=== JOIN TEST $Version / $Loader server, client without mods on port $Port ==="
    $client = Start-Process -FilePath $Gradle `
        -ArgumentList $arguments `
        -WorkingDirectory $RepoRoot `
        -RedirectStandardOutput $clientStdout `
        -RedirectStandardError $clientStderr `
        @hiddenWindow `
        -PassThru

    $script:JoinStarted = $true
    $deadline = (Get-Date).AddSeconds($JoinTimeoutSeconds)
    $clientLog = Join-Path (Join-Path $runDirectory 'logs') 'latest.log'
    $outputSize = -1
    $lastOutput = Get-Date
    try {
        while ((Get-Date) -lt $deadline) {
            $lines = @(Get-Content $ServerLog -ErrorAction SilentlyContinue | Select-Object -Skip $skip)
            if ($lines | Where-Object { $_ -match "\b$player joined the game" }) {
                Write-Host "PASS join $Version / $Loader (a player without the mod was admitted)"
                return
            }
            # Refused during login or configuration: the line carries the reason the player saw.
            $refused = $lines | Where-Object { $_ -match 'lost connection: |Disconnecting ' } | Select-Object -First 1
            if ($refused) {
                throw "A player without the mod was refused: $refused"
            }
            if ($ServerProcess.HasExited) {
                throw "The modded server stopped while the client was joining (code $($ServerProcess.ExitCode))"
            }
            if ($client.HasExited) {
                $tail = if (Test-Path $clientStdout) { (Get-Content $clientStdout -Tail 30) -join "`n" } else { '' }
                throw "The client without mods exited before joining (code $($client.ExitCode))`n$tail"
            }
            # Same rule as the client lanes: a client that has started logging and then goes quiet
            # does not recover.
            $size = (@($clientStdout, $clientStderr, $clientLog) | Where-Object { Test-Path $_ } |
                ForEach-Object { (Get-Item $_).Length } | Measure-Object -Sum).Sum
            if ($size -ne $outputSize) {
                $outputSize = $size
                $lastOutput = Get-Date
            } elseif ((Test-Path $clientLog) -and ((Get-Date) - $lastOutput).TotalSeconds -ge $JoinStallSeconds) {
                $last = (Get-Content $clientLog -Tail 1)
                # This client loads no mods at all, so a hang before its window exists cannot be
                # the mod's; seen once on 1.20.4 under Xvfb, the line names it for the retry.
                if ($last -match 'Backend library: LWJGL') {
                    throw "The client without mods hung opening its window before connecting: $last"
                }
                throw "The client without mods logged nothing for ${JoinStallSeconds}s; last line: $last"
            }
            Start-Sleep -Seconds 2
        }
        throw "No player without the mod joined within ${JoinTimeoutSeconds}s"
    } finally {
        Stop-TestProcessTree -Process $client
        Remove-Item Env:MIGHTYARCHITECT_JOIN_TEST_SERVER -ErrorAction SilentlyContinue
    }
}

# Called after Copy-ServerArtifacts, which replaces this loader's whole results folder.
function Copy-JoinArtifacts {
    param(
        [string]$Version,
        [string]$Loader
    )

    $fabricProject = Join-Path (Join-Path (Join-Path $RepoRoot 'fabric') 'versions') $Version
    $target = Join-Path (Join-Path (Join-Path $ResultsRoot $Version) $Loader) 'join-test'
    New-Item -ItemType Directory -Force -Path $target | Out-Null
    $outputDirectory = Join-Path (Join-Path $fabricProject 'build') 'join-test'
    foreach ($name in @('gradle.stdout.log', 'gradle.stderr.log')) {
        $file = Join-Path $outputDirectory $name
        if (Test-Path $file) {
            Copy-Item $file $target
        }
    }
    $clientLog = Join-Path (Join-Path (Join-Path $fabricProject 'run-join-test') 'logs') 'latest.log'
    if (Test-Path $clientLog) {
        Copy-Item $clientLog (Join-Path $target 'client.log')
    }
}

function Invoke-ServerTest {
    param(
        [string]$Version,
        [string]$Loader
    )

    Write-Host "=== SERVER TEST $Version / $Loader ==="
    $script:JoinStarted = $false
    $projectDirectory = Join-Path (Join-Path (Join-Path $RepoRoot $Loader) 'versions') $Version
    $runDirectory = Join-Path $projectDirectory 'run-server-test'
    $outputDirectory = Join-Path (Join-Path $projectDirectory 'build') 'server-test'
    $gradleStdout = Join-Path $outputDirectory 'gradle.stdout.log'
    $gradleStderr = Join-Path $outputDirectory 'gradle.stderr.log'

    Remove-Item $runDirectory -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $outputDirectory -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $runDirectory, $outputDirectory | Out-Null
    Set-Content (Join-Path $runDirectory 'eula.txt') 'eula=true' -Encoding ascii
    # Nothing connects to the plain print test, so it binds any free port; the join test needs to
    # know which one.
    $port = if ($JoinTest) { Get-FreeTcpPort } else { 0 }
    @(
        "server-port=$port"
        'online-mode=false'
        # 26.3 turned the whitelist on by default; an offline test server has no list to be on.
        'white-list=false'
        'enforce-secure-profile=false'
        'spawn-protection=0'
        'view-distance=3'
        'simulation-distance=3'
        'generate-structures=false'
        'level-type=minecraft:flat'
        'motd=Mighty Architect Server Test'
    ) | Set-Content (Join-Path $runDirectory 'server.properties') -Encoding ascii

    $arguments = [System.Collections.Generic.List[string]]::new()
    $arguments.Add(":${Loader}:${Version}:runAutomatedServerTest")
    $arguments.Add('--console=plain')
    $arguments.Add('--no-daemon')
    $arguments.Add('-Porg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64,JAVA_HOME_21_X64,JAVA_HOME_25_X64')
    $hiddenWindow = Get-TestHiddenWindowOption
    $process = Start-Process -FilePath $Gradle `
        -ArgumentList $arguments `
        -WorkingDirectory $RepoRoot `
        -RedirectStandardOutput $gradleStdout `
        -RedirectStandardError $gradleStderr `
        @hiddenWindow `
        -PassThru

    $serverLog = Join-Path (Join-Path $runDirectory 'logs') 'latest.log'
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    try {
        while ((Get-Date) -lt $deadline) {
            $logPaths = @($serverLog, $gradleStdout, $gradleStderr) | Where-Object { Test-Path $_ }
            if ($logPaths.Count -gt 0) {
                $text = ($logPaths | ForEach-Object { Get-Content $_ -Raw -ErrorAction SilentlyContinue }) -join "`n"
                # The print-to-world test reports its own verdict; check it before the generic
                # crash markers so a failed assertion is reported as such rather than as a crash.
                $testFailure = Select-String -Path $logPaths -Pattern '\[SERVER-TEST\] RESULT FAIL' -ErrorAction SilentlyContinue
                if ($testFailure) {
                    throw "Print-to-world test failed: $($testFailure[0].Line)"
                }
                $bad = Select-String -Path $logPaths -Pattern 'MixinApplyError|InvalidInjectionException|NoClassDefFoundError|ClassNotFoundException|Cannot load class|Could not execute entrypoint|Failed to start the minecraft server|Failed to remap|Exception in thread "main"|Exception in server tick loop|Preparing crash report|\[.*FATAL\]' -ErrorAction SilentlyContinue
                if ($bad) {
                    throw "Server crash marker: $($bad[0].Line)"
                }
                # Booting is necessary but not sufficient: wait for the print-to-world verdict so
                # a server that comes up without ever running the test counts as a failure.
                if ($text -match '\[SERVER-TEST\] RESULT PASS \((\d+) checks\)') {
                    Write-Host "PASS server $Version / $Loader ($($Matches[1]) print checks)"
                    if ($JoinTest) {
                        Invoke-ModlessJoin -Version $Version -Loader $Loader -ServerLog $serverLog `
                            -Port $port -ServerProcess $process
                    }
                    return
                }
            }
            if ($process.HasExited) {
                $tail = if (Test-Path $gradleStdout) { (Get-Content $gradleStdout -Tail 30) -join "`n" } else { '' }
                throw "Server exited before ready (code $($process.ExitCode))`n$tail"
            }
            Start-Sleep -Seconds 2
        }
        throw "Server test exceeded ${TimeoutSeconds}s timeout"
    } finally {
        Stop-TestProcessTree -Process $process
        Copy-ServerArtifacts $Version $Loader $runDirectory $gradleStdout $gradleStderr
        if ($script:JoinStarted) {
            Copy-JoinArtifacts $Version $Loader
        }
    }
}

New-Item -ItemType Directory -Force -Path $ResultsRoot | Out-Null
$failures = [System.Collections.Generic.List[string]]::new()

$nodeCount = 0
foreach ($version in $Versions) {
    foreach ($loader in (Select-TestVersionLoaders -Version $version -Requested $Loaders)) {
        $nodeCount++
        $passed = $false
        for ($attempt = 1; $attempt -le 2 -and -not $passed; $attempt++) {
            try {
                Invoke-ServerTest $version $loader
                $passed = $true
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
}

if ($failures.Count -gt 0) {
    throw "Server-test matrix failed:`n - $($failures -join "`n - ")"
}

Write-Host "All $nodeCount server-test nodes passed."
