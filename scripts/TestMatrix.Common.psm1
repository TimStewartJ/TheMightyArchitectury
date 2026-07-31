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
        # A fresh profile otherwise opens the accessibility onboarding screen over the world. The
        # Gradle-launched lanes reuse a warm profile and never see it; a HeadlessMc instance is
        # fresh every run.
        'onboardAccessibility:false'
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

# --------------------------------------------------------------------------------------------
# HeadlessMc: the production launcher for the Forge family.
#
# Fabric Loom ships ClientProductionRunTask, so `-Mode prod` on a Fabric node needs no launcher.
# ModDevGradle ships no equivalent and NeoForge keeps its own production-test tasks inside an
# unpublished buildSrc plugin, so the Forge-family nodes need one. HeadlessMc (MIT) installs a
# real Minecraft plus loader and launches it from a mods folder, which is exactly the shape of
# a launcher and exactly what these jars have never been loaded by.
#
# It is used only as an installer and a launcher. Nothing it does reaches a shipped artifact, and
# the fallback if it ever goes away is the same one NeoForge uses on itself: run the installer with
# --install-client and assemble the command line from the version profile it writes.
# --------------------------------------------------------------------------------------------

$script:HeadlessMcVersion = '2.10.0'

function Get-TestHeadlessMcRoot {
    param([Parameter(Mandatory = $true)][string]$RepoRoot)
    return Join-Path (Join-Path $RepoRoot 'build') 'headlessmc'
}

function Get-TestHeadlessMcLauncher {
    <#
        Downloads the pinned HeadlessMc launcher once per checkout. Pinned deliberately: the
        project releases in bursts and a floating "latest" would let an upstream release change
        what this matrix means without a commit here.
    #>
    param([Parameter(Mandatory = $true)][string]$RepoRoot)

    $root = Get-TestHeadlessMcRoot -RepoRoot $RepoRoot
    New-Item -ItemType Directory -Force -Path $root | Out-Null
    $jar = Join-Path $root "headlessmc-launcher-$($script:HeadlessMcVersion).jar"
    if (Test-Path $jar) {
        return $jar
    }

    $url = "https://github.com/headlesshq/headlessmc/releases/download/$($script:HeadlessMcVersion)/headlessmc-launcher-$($script:HeadlessMcVersion).jar"
    Write-Host "Downloading HeadlessMc $($script:HeadlessMcVersion)"
    $temporary = "$jar.tmp"
    Invoke-WebRequest -Uri $url -OutFile $temporary
    Move-Item $temporary $jar -Force
    return $jar
}

function Invoke-TestHeadlessMc {
    <#
        Runs one HeadlessMc command. HeadlessMc reads HeadlessMC/config.properties relative to the
        working directory, so every invocation has to run from the same root.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Launcher,
        [Parameter(Mandatory = $true)][string]$Java,
        [Parameter(Mandatory = $true)][string]$Command,
        [switch]$Quiet
    )

    Push-Location $Root
    try {
        $output = & $Java '-jar' $Launcher '--command' $Command 2>&1
        if (-not $Quiet) {
            $output | Where-Object { $_ -notmatch 'Not running from the headlessmc-launcher-wrapper' } |
                ForEach-Object { Write-Host "  hmc| $_" }
        }
        return $output
    } finally {
        Pop-Location
    }
}

function Initialize-TestHeadlessMcInstance {
    <#
        Installs Minecraft and the requested loader into an isolated .minecraft, and returns the
        launchable version name. Idempotent: both steps are skipped when the version is already
        present, which is what makes the directory worth caching in CI.

        The loader build is pinned with --uid to the version this node actually targets. Without
        it HeadlessMc installs the newest build, and a fresh upstream release could turn the
        matrix red with no change on our side.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$MinecraftVersion,
        [Parameter(Mandatory = $true)][ValidateSet('neoforge', 'forge')][string]$Loader,
        [Parameter(Mandatory = $true)][string]$LoaderUid,
        [Parameter(Mandatory = $true)][int]$JavaVersion,
        [Parameter(Mandatory = $true)][string]$GameDirectory
    )

    $root = Get-TestHeadlessMcRoot -RepoRoot $RepoRoot
    $launcher = Get-TestHeadlessMcLauncher -RepoRoot $RepoRoot
    $java = Resolve-TestJava -Version $JavaVersion
    $mcDir = Join-Path $root 'mc'
    New-Item -ItemType Directory -Force -Path $mcDir, $GameDirectory, (Join-Path $root 'HeadlessMC') | Out-Null

    # Forward slashes throughout: HeadlessMc splits --jvm on whitespace and drops backslashes on
    # Windows, and the JVM accepts forward slashes on every platform.
    $slash = { param($p) ($p -replace '\\', '/') }
    @(
        "hmc.java.versions=$(& $slash $java)"
        "hmc.mcdir=$(& $slash $mcDir)"
        "hmc.gamedir=$(& $slash $GameDirectory)"
        'hmc.offline=true'
        'hmc.offline.username=ArchitectTest'
        'hmc.rethrow.launch.exceptions=true'
        'hmc.exit.on.failed.command=true'
        # Real textures: the palette-grid and HUD assertions count distinct colours, so the dummy
        # assets HeadlessMc offers for smaller runs would fail them for the wrong reason.
        'hmc.assets.dummy=false'
        'hmc.jline.enabled=false'
        # NeoForge can sit on a crash screen forever instead of exiting; this ends the process.
        'hmc.crash.report.watcher=true'
    ) | Set-Content (Join-Path $root 'HeadlessMC\config.properties') -Encoding ascii

    $installed = Get-TestHeadlessMcVersions -Root $root -Launcher $launcher -Java $java
    if (-not ($installed | Where-Object { $_.Name -eq $MinecraftVersion })) {
        Write-Host "HeadlessMc: downloading Minecraft $MinecraftVersion"
        Invoke-TestHeadlessMc -Root $root -Launcher $launcher -Java $java -Command "download $MinecraftVersion" | Out-Null
    }

    $existing = $installed | Where-Object { $_.Parent -eq $MinecraftVersion -and $_.Name -match $Loader }
    if (-not $existing) {
        Write-Host "HeadlessMc: installing $Loader $LoaderUid for $MinecraftVersion"
        Invoke-TestHeadlessMc -Root $root -Launcher $launcher -Java $java `
            -Command "$Loader $MinecraftVersion --uid $LoaderUid --java $JavaVersion" | Out-Null
        $installed = Get-TestHeadlessMcVersions -Root $root -Launcher $launcher -Java $java
        $existing = $installed | Where-Object { $_.Parent -eq $MinecraftVersion -and $_.Name -match $Loader }
    }

    if (-not $existing) {
        throw "HeadlessMc did not install $Loader $LoaderUid for Minecraft $MinecraftVersion"
    }

    return @($existing)[0].Name
}

function Get-TestHeadlessMcVersions {
    <#
        Parses `versions` into objects. The launchable name is what `launch` wants; matching on it
        is safer than a regex over the loader name, because one .minecraft holds every version and
        `.*forge.*` would also match neoforge.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][string]$Launcher,
        [Parameter(Mandatory = $true)][string]$Java
    )

    $output = Invoke-TestHeadlessMc -Root $Root -Launcher $Launcher -Java $Java -Command 'versions' -Quiet
    $versions = [System.Collections.Generic.List[object]]::new()
    foreach ($line in $output) {
        $text = "$line".Trim()
        if ($text -match '^(\d+)\s+(\S+)(?:\s+(\S+))?$') {
            $versions.Add([pscustomobject]@{
                Id = [int]$matches[1]
                Name = $matches[2]
                Parent = if ($matches[3]) { $matches[3] } else { '' }
            })
        }
    }
    return $versions
}

function Start-TestHeadlessMcClient {
    <#
        Stages the production jars into the game directory's mods folder and launches the client.
        This is the whole point of the lane: the jars are discovered from a mods folder by a real
        loader, exactly as a player's launcher would discover them.
    #>
    param(
        [Parameter(Mandatory = $true)][string]$RepoRoot,
        [Parameter(Mandatory = $true)][string]$LaunchVersion,
        [Parameter(Mandatory = $true)][int]$JavaVersion,
        [Parameter(Mandatory = $true)][string[]]$ModJars,
        [Parameter(Mandatory = $true)][string]$GameDirectory,
        [Parameter(Mandatory = $true)][string]$ResultPath,
        [Parameter(Mandatory = $true)][int]$ServerPort,
        [Parameter(Mandatory = $true)][string]$StandardOutput,
        [Parameter(Mandatory = $true)][string]$StandardError,
        [switch]$KeepOpen
    )

    $root = Get-TestHeadlessMcRoot -RepoRoot $RepoRoot
    $launcher = Get-TestHeadlessMcLauncher -RepoRoot $RepoRoot
    $java = Resolve-TestJava -Version $JavaVersion

    $mods = Join-Path $GameDirectory 'mods'
    New-Item -ItemType Directory -Force -Path $mods | Out-Null
    Remove-Item (Join-Path $mods '*.jar') -Force -ErrorAction SilentlyContinue
    foreach ($jar in $ModJars) {
        Copy-Item $jar $mods
        Write-Host "  staged $(Split-Path $jar -Leaf)"
    }

    $jvm = @(
        '-Dmightyarchitect.clientTest.enabled=true'
        "-Dmightyarchitect.clientTest.server=127.0.0.1:$ServerPort"
        "-Dmightyarchitect.clientTest.result=$($ResultPath -replace '\\', '/')"
        "-Dmightyarchitect.clientTest.keepOpen=$($KeepOpen.IsPresent.ToString().ToLowerInvariant())"
    ) -join ' '

    # Delivered through the config file rather than `--command "launch x --jvm "..."" `: that form
    # needs quotes nested two deep, and Start-Process quotes its own arguments differently on
    # Windows and Linux. A properties file has no quoting semantics at all. hmc.jvmargs is
    # space-delimited, so none of these values may contain a space.
    Add-Content -Path (Join-Path $root 'HeadlessMC\config.properties') -Value "hmc.jvmargs=$jvm" -Encoding ascii

    # -Dhmc.check.xvfb=true is what makes an offline account usable with real rendering: HeadlessMc
    # otherwise forces its LWJGL stub when offline, which produces empty framebuffers and would
    # fail every screenshot assertion. Under a virtual framebuffer it leaves rendering alone.
    $arguments = @(
        '-Dhmc.check.xvfb=true'
        '-jar', $launcher
        '--command', "launch $LaunchVersion"
    )

    $hiddenWindow = Get-TestHiddenWindowOption
    return Start-Process -FilePath $java `
        -ArgumentList $arguments `
        -WorkingDirectory $root `
        -RedirectStandardOutput $StandardOutput `
        -RedirectStandardError $StandardError `
        @hiddenWindow `
        -PassThru
}

function Get-TestLoaderUid {
    <#
        The loader build this node targets, in the form HeadlessMc's --uid wants. NeoForge records
        it bare (21.8.54); Forge records it prefixed with the Minecraft version (1.20.1-47.3.0).
    #>
    param(
        [Parameter(Mandatory = $true)][hashtable]$Properties,
        [Parameter(Mandatory = $true)][ValidateSet('neoforge', 'forge')][string]$Loader
    )

    if ($Loader -eq 'neoforge') {
        $uid = $Properties.neoforge_version
    } else {
        $uid = ($Properties.forge_version -replace "^$([regex]::Escape($Properties.minecraft_version))-", '')
    }

    if (-not $uid) {
        throw "No $Loader version recorded for Minecraft $($Properties.minecraft_version)"
    }
    return $uid
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
    'Get-TestHeadlessMcRoot',
    'Get-TestHeadlessMcLauncher',
    'Get-TestHeadlessMcVersions',
    'Initialize-TestHeadlessMcInstance',
    'Start-TestHeadlessMcClient',
    'Get-TestLoaderUid',
    'Test-TestRetryableFailure'
)
