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

function Find-TestGradleArtifact {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Group,
        [Parameter(Mandatory = $true)]
        [string]$Module,
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    $directory = Join-Path $env:USERPROFILE ".gradle/caches/modules-2/files-2.1/$Group/$Module/$Version"
    $jar = Get-ChildItem $directory -Recurse -Filter "$Module-$Version.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|javadoc' } |
        Select-Object -First 1
    if (-not $jar) {
        throw "Gradle artifact not found: $Group`:$Module`:$Version"
    }
    return $jar.FullName
}

function Get-RuntimeArtifactManifestPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot
    )

    return Join-Path (Join-Path (Join-Path $RepoRoot 'build') 'runtime-artifacts') 'manifest.json'
}

function Read-RuntimeArtifactManifest {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot
    )

    $path = Get-RuntimeArtifactManifestPath -RepoRoot $RepoRoot
    if (-not (Test-Path $path)) {
        return $null
    }
    return Get-Content $path -Raw | ConvertFrom-Json
}

function Get-RuntimeArtifactTarget {
    param(
        [psobject]$Manifest,
        [Parameter(Mandatory = $true)]
        [string]$Version,
        [Parameter(Mandatory = $true)]
        [string]$Loader
    )

    if (-not $Manifest) {
        return $null
    }
    return $Manifest.targets | Where-Object { $_.version -eq $Version -and $_.loader -eq $Loader } | Select-Object -First 1
}

function Test-RuntimeArtifactStale {
    <#
        Returns a reason string when the prebuilt artifacts cannot be trusted for the
        requested targets, or $null when they are usable as-is.
    #>
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(Mandatory = $true)]
        [string[]]$Versions,
        [Parameter(Mandatory = $true)]
        [string[]]$Loaders
    )

    $manifest = Read-RuntimeArtifactManifest -RepoRoot $RepoRoot
    if (-not $manifest) {
        return 'no artifact manifest'
    }

    $currentCommit = (& git -C $RepoRoot rev-parse HEAD 2>$null)
    if ($currentCommit -and $manifest.gitCommit -and $currentCommit.Trim() -ne $manifest.gitCommit) {
        return "git commit changed ($($manifest.gitCommit) -> $($currentCommit.Trim()))"
    }

    foreach ($version in $Versions) {
        foreach ($loader in $Loaders) {
            $target = Get-RuntimeArtifactTarget -Manifest $manifest -Version $version -Loader $loader
            if (-not $target) {
                return "missing target $version/$loader"
            }
            foreach ($jar in @($target.modJar, $target.testJar) + @($target.dependencyJars)) {
                if (-not (Test-Path $jar)) {
                    return "missing jar $jar"
                }
            }
        }
    }

    $builtAt = if ($manifest.createdAt -is [DateTime]) {
        $manifest.createdAt.ToUniversalTime()
    } else {
        [DateTime]::Parse(
            $manifest.createdAt,
            [Globalization.CultureInfo]::InvariantCulture,
            [Globalization.DateTimeStyles]::RoundtripKind
        ).ToUniversalTime()
    }
    $sourceRoots = @('common/src', 'fabric/src', 'neoforge/src', 'client-test/src') |
        ForEach-Object { Join-Path $RepoRoot $_ } |
        Where-Object { Test-Path $_ }
    $sourceRoots += @(Join-Path $RepoRoot 'buildSrc') | Where-Object { Test-Path $_ }
    foreach ($root in $sourceRoots) {
        $newer = Get-ChildItem $root -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -notmatch '[\\/](build|\.gradle)[\\/]' -and $_.LastWriteTimeUtc -gt $builtAt } |
            Select-Object -First 1
        if ($newer) {
            return "source newer than artifacts ($($newer.Name))"
        }
    }

    $buildInputs = @('build.gradle', 'settings.gradle', 'stonecutter.gradle', 'gradle.properties',
        'common/build.gradle', 'fabric/build.gradle', 'neoforge/build.gradle')
    foreach ($version in $Versions) {
        $buildInputs += "versions/$version/gradle.properties"
    }
    $buildInputFiles = $buildInputs |
        ForEach-Object { Join-Path $RepoRoot $_ } |
        Where-Object { Test-Path $_ } |
        ForEach-Object { Get-Item $_ }
    foreach ($buildInput in $buildInputFiles) {
        if ($buildInput.LastWriteTimeUtc -gt $builtAt) {
            return "build input newer than artifacts ($($buildInput.FullName.Substring($RepoRoot.Length).TrimStart('\', '/')))"
        }
    }

    return $null
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
    'Expand-TestListArgument',
    'Find-TestGradleArtifact',
    'Get-RuntimeArtifactManifestPath',
    'Read-RuntimeArtifactManifest',
    'Get-RuntimeArtifactTarget',
    'Test-RuntimeArtifactStale'
)
