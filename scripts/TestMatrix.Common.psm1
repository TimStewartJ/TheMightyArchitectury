function Assert-TestMatrixPowerShell {
    if ($PSVersionTable.PSVersion.Major -lt 7) {
        throw 'PowerShell 7 or newer is required.'
    }
}

function Get-TestMatrixRepoRoot {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ScriptRoot
    )

    return (Resolve-Path (Join-Path $ScriptRoot '..')).Path
}

function Get-TestGradleCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot
    )

    if ($IsWindows) {
        return Join-Path $RepoRoot 'gradlew.bat'
    }
    return Join-Path $RepoRoot 'gradlew'
}

function Get-TestNodeProperties {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

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

function Resolve-TestJava {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Version
    )

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

function Get-TestServerJar {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MinecraftVersion,
        [Parameter(Mandatory = $true)]
        [string]$RuntimeRoot
    )

    $directory = Join-Path $RuntimeRoot 'server-jars'
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $jar = Join-Path $directory "minecraft-server-$MinecraftVersion.jar"
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

function Wait-TestPortAvailable {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
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

function Stop-TestProcessTree {
    param(
        [System.Diagnostics.Process]$Process
    )

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

function Get-TestProcessCommandLine {
    param(
        [Parameter(Mandatory = $true)]
        [int]$ProcessId
    )

    if ($IsWindows) {
        return (Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId" -ErrorAction SilentlyContinue).CommandLine
    }

    $procPath = "/proc/$ProcessId/cmdline"
    if (Test-Path $procPath) {
        return (Get-Content $procPath -Raw -ErrorAction SilentlyContinue).Replace([char]0, ' ').Trim()
    }

    $command = & ps -p $ProcessId -o command= 2>$null
    return ($command -join ' ').Trim()
}

function Normalize-TestCommandLine {
    param(
        [string]$CommandLine
    )

    if (-not $CommandLine) {
        return $null
    }
    return [regex]::Replace($CommandLine, '\s+', ' ').Trim()
}

function Get-TestProcessIdentity {
    param(
        [Parameter(Mandatory = $true)]
        [System.Diagnostics.Process]$Process
    )

    $Process.Refresh()
    $executablePath = try { $Process.Path } catch { $null }
    return [pscustomobject]@{
        processId = $Process.Id
        processName = $Process.ProcessName
        startTimeUtc = $Process.StartTime.ToUniversalTime().ToString('o')
        executablePath = $executablePath
        commandLine = Normalize-TestCommandLine (Get-TestProcessCommandLine -ProcessId $Process.Id)
    }
}

function Stop-TestOwnedProcess {
    param(
        [Parameter(Mandatory = $true)]
        [psobject]$Identity
    )

    $process = Get-Process -Id $Identity.processId -ErrorAction SilentlyContinue
    if (-not $process) {
        return $false
    }

    $startTime = if ($Identity.startTimeUtc -is [DateTime]) {
        $Identity.startTimeUtc
    } else {
        [DateTime]::Parse(
            $Identity.startTimeUtc,
            [Globalization.CultureInfo]::InvariantCulture,
            [Globalization.DateTimeStyles]::RoundtripKind
        )
    }
    $currentCommandLine = Normalize-TestCommandLine (Get-TestProcessCommandLine -ProcessId $process.Id)
    $currentExecutablePath = try { $process.Path } catch { $null }
    $matches =
        $process.ProcessName -eq $Identity.processName -and
        [Math]::Abs(($process.StartTime.ToUniversalTime() - $startTime.ToUniversalTime()).TotalSeconds) -lt 2 -and
        (-not $Identity.executablePath -or $currentExecutablePath -eq $Identity.executablePath) -and
        (-not $Identity.commandLine -or $currentCommandLine -eq $Identity.commandLine)

    if (-not $matches) {
        Write-Warning "Skipping stale process identity for PID $($Identity.processId)"
        return $false
    }

    Stop-TestProcessTree -Process $process
    return $true
}

function Write-TestSessionManifest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(Mandatory = $true)]
        [string]$FileName,
        [Parameter(Mandatory = $true)]
        [psobject]$Session
    )

    $directory = Join-Path (Join-Path $RepoRoot 'build') 'kept-open-sessions'
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $manifest = Join-Path $directory $FileName
    $temporary = "$manifest.tmp"
    $Session | ConvertTo-Json -Depth 8 | Set-Content $temporary -Encoding utf8
    Move-Item $temporary $manifest -Force
    return $manifest
}

function Start-TestVanillaServer {
    param(
        [Parameter(Mandatory = $true)]
        [string]$NodeId,
        [Parameter(Mandatory = $true)]
        [hashtable]$Properties,
        [Parameter(Mandatory = $true)]
        [string]$RuntimeRoot,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [Parameter(Mandatory = $true)]
        [string]$Motd,
        [int]$StartupTimeoutSeconds = 180
    )

    $minecraftVersion = $Properties.minecraft_version
    $javaVersion = [int]$Properties.java_version
    $directory = Join-Path $RuntimeRoot $NodeId
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    Remove-Item (Join-Path $directory 'world'), (Join-Path $directory 'logs') -Recurse -Force -ErrorAction SilentlyContinue

    $jar = Get-TestServerJar -MinecraftVersion $minecraftVersion -RuntimeRoot $RuntimeRoot
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
        "motd=$Motd"
    ) | Set-Content (Join-Path $directory 'server.properties') -Encoding ascii

    $stdout = Join-Path $directory 'server.stdout.log'
    $stderr = Join-Path $directory 'server.stderr.log'
    Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue
    Wait-TestPortAvailable -Port $Port
    $process = Start-Process -FilePath (Resolve-TestJava -Version $javaVersion) `
        -ArgumentList @('-Xms512M', '-Xmx1024M', '-jar', $jar, 'nogui') `
        -WorkingDirectory $directory `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -PassThru

    try {
        $log = Join-Path (Join-Path $directory 'logs') 'latest.log'
        $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
        while ((Get-Date) -lt $deadline) {
            if ($process.HasExited) {
                throw "Vanilla server $minecraftVersion exited early with code $($process.ExitCode)"
            }
            if (Test-Path $log) {
                $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
                if ($text -match 'Done \(') {
                    return [pscustomobject]@{
                        Process = $process
                        Directory = $directory
                        Log = $log
                        Port = $Port
                    }
                }
            }
            Start-Sleep -Seconds 2
        }
        throw "Vanilla server $minecraftVersion did not become ready"
    } catch {
        Stop-TestProcessTree -Process $process
        throw
    }
}

Export-ModuleMember -Function @(
    'Assert-TestMatrixPowerShell',
    'Get-TestMatrixRepoRoot',
    'Get-TestGradleCommand',
    'Get-TestNodeProperties',
    'Resolve-TestJava',
    'Get-TestServerJar',
    'Wait-TestPortAvailable',
    'Stop-TestProcessTree',
    'Get-TestProcessCommandLine',
    'Get-TestProcessIdentity',
    'Stop-TestOwnedProcess',
    'Write-TestSessionManifest',
    'Start-TestVanillaServer'
)
