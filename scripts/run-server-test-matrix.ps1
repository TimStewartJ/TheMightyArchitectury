param(
    [string[]]$Versions = @('1.21.1', '1.21.4', '1.21.6', '1.21.8', '1.21.10', '1.21.11', '26.1'),
    [ValidateSet('fabric', 'neoforge')]
    [string[]]$Loaders = @('fabric', 'neoforge'),
    [int]$TimeoutSeconds = 600
)

$ErrorActionPreference = 'Stop'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'PowerShell 7 or newer is required.'
}

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$ResultsRoot = Join-Path (Join-Path $RepoRoot 'build') 'server-test-results'
$Gradle = if ($IsWindows) {
    Join-Path $RepoRoot 'gradlew.bat'
} else {
    Join-Path $RepoRoot 'gradlew'
}

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

function Stop-ProcessTree {
    param([System.Diagnostics.Process]$Process)

    if (-not $Process -or $Process.HasExited) {
        return
    }
    try {
        $Process.Kill($true)
        $Process.WaitForExit()
    } catch {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-ServerTest {
    param(
        [string]$Version,
        [string]$Loader
    )

    Write-Host "=== SERVER TEST $Version / $Loader ==="
    $projectDirectory = Join-Path (Join-Path (Join-Path $RepoRoot $Loader) 'versions') $Version
    $runDirectory = Join-Path $projectDirectory 'run-server-test'
    $outputDirectory = Join-Path (Join-Path $projectDirectory 'build') 'server-test'
    $gradleStdout = Join-Path $outputDirectory 'gradle.stdout.log'
    $gradleStderr = Join-Path $outputDirectory 'gradle.stderr.log'

    Remove-Item $runDirectory -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $outputDirectory -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $runDirectory, $outputDirectory | Out-Null
    Set-Content (Join-Path $runDirectory 'eula.txt') 'eula=true' -Encoding ascii
    @(
        'server-port=0'
        'online-mode=false'
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
    $arguments.Add('-Porg.gradle.java.installations.fromEnv=JAVA_HOME_21_X64,JAVA_HOME_25_X64')
    $process = Start-Process -FilePath $Gradle `
        -ArgumentList $arguments `
        -WorkingDirectory $RepoRoot `
        -RedirectStandardOutput $gradleStdout `
        -RedirectStandardError $gradleStderr `
        -PassThru

    $serverLog = Join-Path (Join-Path $runDirectory 'logs') 'latest.log'
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    try {
        while ((Get-Date) -lt $deadline) {
            $logPaths = @($serverLog, $gradleStdout, $gradleStderr) | Where-Object { Test-Path $_ }
            if ($logPaths.Count -gt 0) {
                $text = ($logPaths | ForEach-Object { Get-Content $_ -Raw -ErrorAction SilentlyContinue }) -join "`n"
                $bad = Select-String -Path $logPaths -Pattern 'MixinApplyError|InvalidInjectionException|NoClassDefFoundError|ClassNotFoundException|Cannot load class|Could not execute entrypoint|Failed to start the minecraft server|Failed to remap|Exception in thread "main"|Exception in server tick loop|Preparing crash report|\[.*FATAL\]' -ErrorAction SilentlyContinue
                if ($bad) {
                    throw "Server crash marker: $($bad[0].Line)"
                }
                if ($text -match 'Done \(') {
                    Write-Host "PASS server $Version / $Loader"
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
        Stop-ProcessTree $process
        Copy-ServerArtifacts $Version $Loader $runDirectory $gradleStdout $gradleStderr
    }
}

New-Item -ItemType Directory -Force -Path $ResultsRoot | Out-Null
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($version in $Versions) {
    foreach ($loader in $Loaders) {
        $passed = $false
        for ($attempt = 1; $attempt -le 2 -and -not $passed; $attempt++) {
            try {
                Invoke-ServerTest $version $loader
                $passed = $true
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
}

if ($failures.Count -gt 0) {
    throw "Server-test matrix failed:`n - $($failures -join "`n - ")"
}

Write-Host "All $($Versions.Count * $Loaders.Count) server-test nodes passed."
