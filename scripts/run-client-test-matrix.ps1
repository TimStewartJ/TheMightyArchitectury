param(
    [string[]]$Versions = @('1.21.1', '1.21.4', '1.21.6', '1.21.8', '1.21.10', '1.21.11', '26.1'),
    [ValidateSet('fabric', 'neoforge')]
    [string[]]$Loaders = @('fabric', 'neoforge'),
    [int]$Port = 25565,
    [int]$ClientTimeoutSeconds = 600
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw 'PowerShell 7 or newer is required.'
}

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$BuildRoot = Join-Path $RepoRoot 'build'
$RuntimeRoot = Join-Path $BuildRoot 'client-test-runtime'
$ResultsRoot = Join-Path $BuildRoot 'client-test-results'
$Gradle = if ($IsWindows) {
    Join-Path $RepoRoot 'gradlew.bat'
} else {
    Join-Path $RepoRoot 'gradlew'
}

function Get-NodeProperties {
    param([string]$Version)

    $path = Join-Path (Join-Path (Join-Path $RepoRoot 'versions') $Version) 'gradle.properties'
    if (-not (Test-Path $path)) {
        throw "Version properties not found: $path"
    }

    $properties = @{}
    foreach ($line in Get-Content $path) {
        if ($line -match '^\s*([^#][^=]*)=(.*)$') {
            $properties[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
    return $properties
}

function Resolve-Java {
    param([int]$Version)

    $environmentName = "JAVA_HOME_${Version}_X64"
    $javaHome = [Environment]::GetEnvironmentVariable($environmentName)
    if ($javaHome) {
        $candidate = Join-Path $javaHome $(if ($IsWindows) { 'bin\java.exe' } else { 'bin/java' })
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    if ($IsWindows) {
        $known = if ($Version -eq 25) {
            Join-Path $env:USERPROFILE '.jdks\jdk-25.0.3+9\bin\java.exe'
        } else {
            'C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot\bin\java.exe'
        }
        if (Test-Path $known) {
            return $known
        }
    }

    $command = Get-Command java -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    throw "Unable to find Java $Version ($environmentName is unset)"
}

function Get-ServerJar {
    param(
        [string]$MinecraftVersion,
        [string]$Directory
    )

    New-Item -ItemType Directory -Force -Path $Directory | Out-Null
    $jar = Join-Path $Directory "minecraft-server-$MinecraftVersion.jar"
    if (Test-Path $jar) {
        return $jar
    }

    Write-Host "Downloading vanilla server $MinecraftVersion"
    $manifest = Invoke-RestMethod 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
    $entry = $manifest.versions | Where-Object { $_.id -eq $MinecraftVersion } | Select-Object -First 1
    if (-not $entry) {
        throw "No Mojang version manifest entry for $MinecraftVersion"
    }
    $detail = Invoke-RestMethod $entry.url
    Invoke-WebRequest -Uri $detail.downloads.server.url -OutFile $jar
    return $jar
}

function Wait-ForPortAvailable {
    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        $client = [System.Net.Sockets.TcpClient]::new()
        try {
            $client.Connect('127.0.0.1', $Port)
        } catch {
            return
        } finally {
            $client.Dispose()
        }
        Start-Sleep -Milliseconds 250
    }
    throw "TCP port $Port is still in use"
}

function Start-TestServer {
    param(
        [string]$NodeVersion,
        [hashtable]$Properties
    )

    $minecraftVersion = $Properties.minecraft_version
    $javaVersion = [int]$Properties.java_version
    $directory = Join-Path $RuntimeRoot $NodeVersion
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    Remove-Item (Join-Path $directory 'world'), (Join-Path $directory 'logs') -Recurse -Force -ErrorAction SilentlyContinue

    $jar = Get-ServerJar $minecraftVersion $directory
    Set-Content (Join-Path $directory 'eula.txt') 'eula=true' -Encoding ascii
    @(
        "server-port=$Port"
        'online-mode=false'
        'enforce-secure-profile=false'
        'spawn-protection=0'
        'view-distance=3'
        'simulation-distance=3'
        'generate-structures=false'
        'level-type=minecraft:flat'
        'motd=Mighty Architect Client Test'
    ) | Set-Content (Join-Path $directory 'server.properties') -Encoding ascii

    $stdout = Join-Path $directory 'server.stdout.log'
    $stderr = Join-Path $directory 'server.stderr.log'
    Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue
    $java = Resolve-Java $javaVersion
    Wait-ForPortAvailable
    $process = Start-Process -FilePath $java `
        -ArgumentList @('-Xms512M', '-Xmx1024M', '-jar', $jar, 'nogui') `
        -WorkingDirectory $directory `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru

    try {
        $log = Join-Path (Join-Path $directory 'logs') 'latest.log'
        $deadline = (Get-Date).AddSeconds(180)
        while ((Get-Date) -lt $deadline) {
            if ($process.HasExited) {
                throw "Vanilla server $minecraftVersion exited early with code $($process.ExitCode)"
            }
            if (Test-Path $log) {
                $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
                if ($text -match 'Done \(') {
                    return @{
                        Process = $process
                        Directory = $directory
                        Log = $log
                    }
                }
            }
            Start-Sleep -Seconds 2
        }
        throw "Vanilla server $minecraftVersion did not become ready"
    } catch {
        if (-not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            $process.WaitForExit()
        }
        throw
    }
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
        foreach ($property in @('baselineScreenshot', 'blueprintScreenshot')) {
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
        [string]$ServerLog
    )

    $projectDirectory = Join-Path (Join-Path (Join-Path $RepoRoot $Loader) 'versions') $Version
    $projectBuildDirectory = Join-Path $projectDirectory 'build'
    $clientTestBuildDirectory = Join-Path $projectBuildDirectory 'client-test'
    $resultPath = Join-Path $clientTestBuildDirectory 'result.json'
    $runDirectory = Join-Path $projectDirectory 'run-client-test'
    $gradleStdout = Join-Path $clientTestBuildDirectory 'gradle.stdout.log'
    $gradleStderr = Join-Path $clientTestBuildDirectory 'gradle.stderr.log'
    New-Item -ItemType Directory -Force -Path (Split-Path $gradleStdout) | Out-Null
    Remove-Item $resultPath -Force -ErrorAction SilentlyContinue
    Remove-Item $runDirectory -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $gradleStdout, $gradleStderr -Force -ErrorAction SilentlyContinue

    Write-Host "=== CLIENT TEST $Version / $Loader ==="
    $env:MIGHTYARCHITECT_CLIENT_TEST_SERVER = "127.0.0.1:$Port"
    $gradleProcess = $null
    try {
        $gradleArguments = [System.Collections.Generic.List[string]]::new()
        $gradleArguments.Add(":${Loader}:${Version}:runAutomatedClientTest")
        $gradleArguments.Add('--console=plain')
        $gradleArguments.Add('--no-daemon')
        $gradleArguments.Add('-Porg.gradle.java.installations.fromEnv=JAVA_HOME_21_X64,JAVA_HOME_25_X64')
        $gradleProcess = Start-Process -FilePath $Gradle `
            -ArgumentList $gradleArguments `
            -WorkingDirectory $RepoRoot `
            -RedirectStandardOutput $gradleStdout `
            -RedirectStandardError $gradleStderr `
            -PassThru
        if (-not $gradleProcess.WaitForExit($ClientTimeoutSeconds * 1000)) {
            try {
                $gradleProcess.Kill($true)
                $gradleProcess.WaitForExit()
            } catch {
                Stop-Process -Id $gradleProcess.Id -Force -ErrorAction SilentlyContinue
            }
            throw "Client test exceeded ${ClientTimeoutSeconds}s timeout"
        }
        $gradleExit = $gradleProcess.ExitCode
    } finally {
        Remove-Item Env:MIGHTYARCHITECT_CLIENT_TEST_SERVER -ErrorAction SilentlyContinue
        if ($gradleProcess -and -not $gradleProcess.HasExited) {
            try {
                $gradleProcess.Kill($true)
                $gradleProcess.WaitForExit()
            } catch {
                Stop-Process -Id $gradleProcess.Id -Force -ErrorAction SilentlyContinue
            }
        }
        Copy-ResultArtifacts $Version $Loader $resultPath $runDirectory $ServerLog $gradleStdout $gradleStderr
    }

    if ($gradleExit -ne 0) {
        if (Test-Path $gradleStdout) {
            Get-Content $gradleStdout -Tail 30 | ForEach-Object { Write-Host $_ }
        }
        if (Test-Path $gradleStderr) {
            Get-Content $gradleStderr -Tail 30 | ForEach-Object { Write-Host $_ -ForegroundColor Red }
        }
        throw "Gradle client test exited $gradleExit"
    }
    if (-not (Test-Path $resultPath)) {
        throw "Client test produced no result: $resultPath"
    }

    $result = Get-Content $resultPath -Raw | ConvertFrom-Json
    if ($result.status -ne 'passed') {
        throw "Client test failed in $($result.stage): $($result.error)"
    }
    Write-Host "PASS $Version / $Loader ($($result.checks.Count) checks)"
}

New-Item -ItemType Directory -Force -Path $RuntimeRoot, $ResultsRoot | Out-Null
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($version in $Versions) {
    $properties = Get-NodeProperties $version
    $server = $null
    try {
        $server = Start-TestServer $version $properties
        foreach ($loader in $Loaders) {
            $passed = $false
            for ($attempt = 1; $attempt -le 2 -and -not $passed; $attempt++) {
                try {
                    Invoke-ClientTest $version $loader $server.Log
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
    } catch {
        $message = "$version / server - $($_.Exception.Message)"
        $failures.Add($message)
        Write-Host "FAIL $message" -ForegroundColor Red
    } finally {
        if ($server -and $server.Process -and -not $server.Process.HasExited) {
            Stop-Process -Id $server.Process.Id -Force -ErrorAction SilentlyContinue
            $server.Process.WaitForExit()
        }
    }
}

if ($failures.Count -gt 0) {
    throw "Client-test matrix failed:`n - $($failures -join "`n - ")"
}

Write-Host "All $($Versions.Count * $Loaders.Count) client-test nodes passed."
