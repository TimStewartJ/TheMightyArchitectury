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
        'difficulty=peaceful'
        "motd=$Motd"
    ) | Set-Content (Join-Path $directory 'server.properties') -Encoding ascii

    $stdout = Join-Path $directory 'server.stdout.log'
    $stderr = Join-Path $directory 'server.stderr.log'
    Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue
    Wait-TestPortAvailable -Port $Port
    $hiddenWindow = Get-TestHiddenWindowOption
    $process = Start-Process -FilePath (Resolve-TestJava -Version $javaVersion) `
        -ArgumentList @('-Xms512M', '-Xmx1024M', '-jar', $jar, 'nogui') `
        -WorkingDirectory $directory `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        @hiddenWindow `
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

function Get-TestHiddenWindowOption {
    <#
        Returns splat arguments that hide the console window on Windows. -WindowStyle is not
        supported on non-Windows PowerShell editions, so it must be omitted there (CI runs Linux).
    #>
    param()

    if ($IsWindows) {
        return @{ WindowStyle = 'Hidden' }
    }
    return @{}
}

function Write-TestClientOptions {
    <#
        Pins the client's GUI scale for tests. At the default scale the harness window is only
        427 GUI units wide, which is below the threshold where the mod tucks its composer menu
        away sideways and leaves almost nothing on screen - that makes HUD assertions measure
        noise. Scale 1 keeps the full HUD visible and makes screenshots comparable across nodes.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [string]$MinecraftDirectory
    )

    New-Item -ItemType Directory -Force -Path $MinecraftDirectory | Out-Null
    @(
        'guiScale:1'
        'pauseOnLostFocus:false'
        'fov:1.0'
    ) | Set-Content (Join-Path $MinecraftDirectory 'options.txt') -Encoding ascii
}

function Expand-TestListArgument {
    <#
        Normalizes list parameters so that both `-Versions a,b` (PowerShell array) and
        `pwsh -File script.ps1 -Versions a,b` (single literal string) behave identically.
    #>
    param(
        [string[]]$Value,
        [string[]]$Allowed
    )

    $expanded = @(
        $Value |
            Where-Object { $_ } |
            ForEach-Object { $_ -split ',' } |
            ForEach-Object { $_.Trim() } |
            Where-Object { $_ }
    )
    if ($Allowed) {
        foreach ($item in $expanded) {
            if ($item -notin $Allowed) {
                throw "Invalid value '$item'. Expected one of: $($Allowed -join ', ')"
            }
        }
    }
    return $expanded
}

function Get-TestVersionLoaders {
    <#
        Returns the loaders a Minecraft version actually ships, mirroring the branch scoping in
        settings.gradle. NeoForge does not exist before 1.20.2 and cannot be built at all for
        1.20.2/1.20.3 (no build in those lines publishes the Gradle module metadata ModDevGradle
        resolves through), so 1.19.4 and 1.20.1 ship Forge instead and 1.20.2 is Fabric-only.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    if ($Version -in @('1.19.4', '1.20.1')) {
        return @('fabric', 'forge')
    }
    if ($Version -eq '1.20.2') {
        return @('fabric')
    }
    return @('fabric', 'neoforge')
}

function Select-TestVersionLoaders {
    <#
        Intersects the requested loaders with the ones this version actually ships, keeping the
        caller's ability to narrow the matrix with -Loaders without inventing missing targets.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [string]$Version,
        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [string[]]$Requested
    )

    return @(Get-TestVersionLoaders -Version $Version | Where-Object { $_ -in $Requested })
}

<#
.SYNOPSIS
Decides whether a matrix failure is worth retrying.

.DESCRIPTION
Retrying is only ever safe for failures caused by the environment, never for failures
caused by the mod. A retried mod defect looks like a pass roughly half the time, which
turns the matrix into a coin flip that reports green.

Infrastructure failures seen so far: Mojang/Loom CDN timeouts while downloading a client,
server or mapping file, and a HashMap treeify ClassCastException thrown from the loader's
own bootstrap thread before any mod class loads.
#>
function Test-TestRetryableFailure {
    param([string]$Message)

    if (-not $Message) {
        return $false
    }

    $infrastructurePatterns = @(
        'DownloadException',
        'Failed to download',
        'Read timed out',
        'Connection reset',
        'Connection timed out',
        # Loom's asset downloader losing its own thread pool mid-run, seen as
        # `:downloadAssets` failing with a RejectedExecutionException.
        'rejected from java\.util\.concurrent\.ThreadPoolExecutor',
        'HashMap\$Node cannot be cast to class java\.util\.HashMap\$TreeNode'
    )

    foreach ($pattern in $infrastructurePatterns) {
        if ($Message -match $pattern) {
            return $true
        }
    }

    return $false
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
    'Start-TestVanillaServer',
    'Get-TestHiddenWindowOption',
    'Write-TestClientOptions',
    'Expand-TestListArgument',
    'Get-TestVersionLoaders',
    'Select-TestVersionLoaders',
    'Test-TestRetryableFailure'
)
