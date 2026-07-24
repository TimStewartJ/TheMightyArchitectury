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

if (-not $IsWindows) {
    throw 'The packaged matrix currently requires Prism Launcher on Windows.'
}

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$PrismExe = Join-Path $env:LOCALAPPDATA 'Programs\PrismLauncher\prismlauncher.exe'
$PrismRoot = Join-Path $env:APPDATA 'PrismLauncher'
$InstancesRoot = Join-Path $PrismRoot 'instances'
$RuntimeRoot = Join-Path $RepoRoot 'build\client-test-runtime'
$ResultsRoot = Join-Path $RepoRoot 'build\packaged-client-test-results'
$Gradle = Join-Path $RepoRoot 'gradlew.bat'

function Get-NodeProperties {
    param([string]$Version)

    $properties = @{}
    foreach ($line in Get-Content (Join-Path $RepoRoot "versions\$Version\gradle.properties")) {
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
        $candidate = Join-Path $javaHome 'bin\java.exe'
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    $known = if ($Version -eq 25) {
        Join-Path $env:USERPROFILE '.jdks\jdk-25.0.3+9\bin\java.exe'
    } else {
        'C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot\bin\java.exe'
    }
    if (Test-Path $known) {
        return $known
    }
    throw "Unable to find Java $Version"
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

    $directory = Join-Path $RuntimeRoot $NodeVersion
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    Remove-Item (Join-Path $directory 'world'), (Join-Path $directory 'logs') -Recurse -Force -ErrorAction SilentlyContinue

    $jar = Get-ServerJar $Properties.minecraft_version $directory
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
        'motd=Mighty Architect Packaged Client Test'
    ) | Set-Content (Join-Path $directory 'server.properties') -Encoding ascii

    Wait-ForPortAvailable
    $process = Start-Process -FilePath (Resolve-Java ([int]$Properties.java_version)) `
        -ArgumentList @('-Xms512M', '-Xmx1024M', '-jar', $jar, 'nogui') `
        -WorkingDirectory $directory `
        -RedirectStandardOutput (Join-Path $directory 'server.stdout.log') `
        -RedirectStandardError (Join-Path $directory 'server.stderr.log') `
        -PassThru

    try {
        $log = Join-Path $directory 'logs\latest.log'
        $deadline = (Get-Date).AddSeconds(180)
        while ((Get-Date) -lt $deadline) {
            if ($process.HasExited) {
                throw "Vanilla server $($Properties.minecraft_version) exited early with code $($process.ExitCode)"
            }
            if (Test-Path $log) {
                $text = Get-Content $log -Raw -ErrorAction SilentlyContinue
                if ($text -match 'Done \(') {
                    return @{ Process = $process; Log = $log }
                }
            }
            Start-Sleep -Seconds 2
        }
        throw "Vanilla server $($Properties.minecraft_version) did not become ready"
    } catch {
        if (-not $process.HasExited) {
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            $process.WaitForExit()
        }
        throw
    }
}

function Get-InstanceProcesses {
    param([string]$InstancePath)

    $escaped = [regex]::Escape($InstancePath)
    $escapedForward = [regex]::Escape($InstancePath.Replace('\', '/'))
    $instanceName = [regex]::Escape((Split-Path $InstancePath -Leaf))
    return Get-CimInstance Win32_Process | Where-Object {
        $_.ProcessId -ne $PID -and
        $_.Name -match '^(java|javaw|prismlauncher)' -and
        $_.CommandLine -and
        ($_.CommandLine -match $escaped -or $_.CommandLine -match $escapedForward -or $_.CommandLine -match $instanceName)
    }
}

function Stop-InstanceProcesses {
    param([string]$InstancePath)

    foreach ($process in @(Get-InstanceProcesses $InstancePath)) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Write-PrismInstance {
    param(
        [string]$Version,
        [string]$Loader,
        [hashtable]$Properties
    )

    $instanceName = "MightyArchitect-Matrix-$Version-$Loader"
    $instancePath = Join-Path $InstancesRoot $instanceName
    $minecraftDirectory = Join-Path $instancePath '.minecraft'
    New-Item -ItemType Directory -Force -Path (Join-Path $minecraftDirectory 'mods') | Out-Null

    $minecraftVersion = $Properties.minecraft_version
    $lwjglVersion = if ($Version -eq '26.1') { '3.4.1' } else { '3.3.3' }
    $components = [System.Collections.Generic.List[object]]::new()
    $components.Add([ordered]@{
        cachedName = 'LWJGL 3'
        cachedVersion = $lwjglVersion
        cachedVolatile = $true
        dependencyOnly = $true
        uid = 'org.lwjgl3'
        version = $lwjglVersion
    })
    $components.Add([ordered]@{
        cachedName = 'Minecraft'
        cachedRequires = @([ordered]@{ suggests = $lwjglVersion; uid = 'org.lwjgl3' })
        cachedVersion = $minecraftVersion
        important = $true
        uid = 'net.minecraft'
        version = $minecraftVersion
    })

    if ($Loader -eq 'fabric') {
        $components.Add([ordered]@{
            cachedName = 'Intermediary Mappings'
            cachedRequires = @([ordered]@{ equals = $minecraftVersion; uid = 'net.minecraft' })
            cachedVersion = $minecraftVersion
            cachedVolatile = $true
            dependencyOnly = $true
            uid = 'net.fabricmc.intermediary'
            version = $minecraftVersion
        })
        $components.Add([ordered]@{
            cachedName = 'Fabric Loader'
            cachedRequires = @([ordered]@{ uid = 'net.fabricmc.intermediary' })
            cachedVersion = $Properties.fabric_loader_version
            uid = 'net.fabricmc.fabric-loader'
            version = $Properties.fabric_loader_version
        })
    } else {
        $components.Add([ordered]@{
            cachedName = 'NeoForge'
            cachedRequires = @([ordered]@{ equals = $minecraftVersion; uid = 'net.minecraft' })
            cachedVersion = $Properties.neoforge_version
            uid = 'net.neoforged'
            version = $Properties.neoforge_version
        })
    }

    [ordered]@{ components = $components; formatVersion = 1 } |
        ConvertTo-Json -Depth 10 |
        Set-Content (Join-Path $instancePath 'mmc-pack.json') -Encoding utf8

    $java = (Resolve-Java ([int]$Properties.java_version)).Replace('\', '/').Replace('java.exe', 'javaw.exe')
    $jvmArgs = "-Dmightyarchitect.clientTest.enabled=true " +
        "-Dmightyarchitect.clientTest.server=127.0.0.1:$Port " +
        '-Dmightyarchitect.clientTest.result=client-test-result.json'
    @"
[General]
ConfigVersion=1.2
iconKey=default
name=Mighty Architect Matrix $Version $Loader
InstanceType=OneSix
ManagedPack=false
UseAccountForInstance=false
OverrideMemory=true
MinMemAlloc=512
MaxMemAlloc=4096
OverrideJava=true
OverrideJavaLocation=true
JavaPath=$java
IgnoreJavaCompatibility=true
OverrideJavaArgs=true
JvmArgs=$jvmArgs
ShowConsole=false
ShowConsoleOnError=true
AutoCloseConsole=false
CloseAfterLaunch=false
MinecraftWinWidth=854
MinecraftWinHeight=480
notes=Disposable automated production-jar client-test instance.
"@ | Set-Content (Join-Path $instancePath 'instance.cfg') -Encoding utf8

    return @{
        Name = $instanceName
        Path = $instancePath
        MinecraftDirectory = $minecraftDirectory
    }
}

function Find-GradleArtifact {
    param(
        [string]$Group,
        [string]$Module,
        [string]$Version
    )

    $directory = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\$Group\$Module\$Version"
    $jar = Get-ChildItem $directory -Recurse -Filter "$Module-$Version.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|javadoc' } |
        Select-Object -First 1
    if (-not $jar) {
        throw "Gradle artifact not found: $Group`:$Module`:$Version"
    }
    return $jar.FullName
}

function Install-Mods {
    param(
        [string]$Version,
        [string]$Loader,
        [hashtable]$Properties,
        [string]$MinecraftDirectory
    )

    & $Gradle ":${Loader}:${Version}:build" ":${Loader}:${Version}:buildClientTestMod" '--console=plain'
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed for $Version / $Loader"
    }

    $libs = Join-Path $RepoRoot "$Loader\versions\$Version\build\libs"
    $mainJar = Get-ChildItem $libs -Filter "*-$Loader.jar" |
        Where-Object { $_.Name -notmatch 'sources|dev|raw|client-test' } |
        Select-Object -First 1
    $testJar = Get-ChildItem $libs -Filter "*-$Loader-client-test.jar" |
        Where-Object { $_.Name -notmatch 'client-test-dev' } |
        Select-Object -First 1
    if (-not $mainJar -or -not $testJar) {
        throw "Production or client-test jar missing in $libs"
    }

    $mods = Join-Path $MinecraftDirectory 'mods'
    Remove-Item $mods -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $mods | Out-Null
    Copy-Item $mainJar.FullName, $testJar.FullName $mods

    $architecturyModule = "architectury-$Loader"
    Copy-Item (Find-GradleArtifact 'dev.architectury' $architecturyModule $Properties.architectury_version) $mods
    if ($Loader -eq 'fabric') {
        Copy-Item (Find-GradleArtifact 'net.fabricmc.fabric-api' 'fabric-api' $Properties.fabric_api_version) $mods
    }
}

function Assert-NoCrash {
    param([string]$MinecraftDirectory)

    $crashDirectory = Join-Path $MinecraftDirectory 'crash-reports'
    if (Test-Path $crashDirectory) {
        $crashes = @(Get-ChildItem $crashDirectory -File -ErrorAction SilentlyContinue)
        if ($crashes.Count -gt 0) {
            throw "Crash report generated: $($crashes[0].Name)"
        }
    }
    $log = Join-Path $MinecraftDirectory 'logs\latest.log'
    if (Test-Path $log) {
        $bad = Select-String -Path $log -Pattern 'Preparing crash report|Game crashed|MixinApplyError|InvalidInjectionException|IllegalAccessError' -ErrorAction SilentlyContinue
        if ($bad) {
            throw "Crash marker: $($bad[0].Line)"
        }
    }
}

function Copy-ResultArtifacts {
    param(
        [string]$Version,
        [string]$Loader,
        [string]$MinecraftDirectory,
        [string]$ServerLog
    )

    $target = Join-Path $ResultsRoot "$Version\$Loader"
    Remove-Item $target -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $target | Out-Null
    $resultPath = Join-Path $MinecraftDirectory 'client-test-result.json'
    if (Test-Path $resultPath) {
        Copy-Item $resultPath (Join-Path $target 'result.json')
        $result = Get-Content $resultPath -Raw | ConvertFrom-Json
        foreach ($property in @('baselineScreenshot', 'blueprintScreenshot')) {
            if ($result.$property -and (Test-Path $result.$property)) {
                Copy-Item $result.$property (Join-Path $target "$property.png")
            }
        }
    }
    $clientLog = Join-Path $MinecraftDirectory 'logs\latest.log'
    if (Test-Path $clientLog) {
        Copy-Item $clientLog (Join-Path $target 'client.log')
    }
    if (Test-Path $ServerLog) {
        Copy-Item $ServerLog (Join-Path $target 'server.log')
    }
}

function Invoke-PackagedClientTest {
    param(
        [string]$Version,
        [string]$Loader,
        [hashtable]$Properties,
        [string]$ServerLog
    )

    Write-Host "=== PACKAGED CLIENT TEST $Version / $Loader ==="
    $instance = Write-PrismInstance $Version $Loader $Properties
    Stop-InstanceProcesses $instance.Path
    Install-Mods $Version $Loader $Properties $instance.MinecraftDirectory

    Remove-Item (Join-Path $instance.MinecraftDirectory 'logs'),
        (Join-Path $instance.MinecraftDirectory 'crash-reports'),
        (Join-Path $instance.MinecraftDirectory 'screenshots') -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item (Join-Path $instance.MinecraftDirectory 'client-test-result.json') -Force -ErrorAction SilentlyContinue

    $launcherDirectory = Join-Path $instance.MinecraftDirectory 'logs'
    New-Item -ItemType Directory -Force -Path $launcherDirectory | Out-Null
    Start-Process -FilePath $PrismExe `
        -ArgumentList @('--launch', $instance.Name) `
        -RedirectStandardOutput (Join-Path $launcherDirectory 'prism.stdout.log') `
        -RedirectStandardError (Join-Path $launcherDirectory 'prism.stderr.log') | Out-Null

    $resultPath = Join-Path $instance.MinecraftDirectory 'client-test-result.json'
    $deadline = (Get-Date).AddSeconds($ClientTimeoutSeconds)
    $launchStarted = Get-Date
    try {
        while ((Get-Date) -lt $deadline) {
            Assert-NoCrash $instance.MinecraftDirectory
            if (Test-Path $resultPath) {
                $result = Get-Content $resultPath -Raw | ConvertFrom-Json
                Copy-ResultArtifacts $Version $Loader $instance.MinecraftDirectory $ServerLog
                if ($result.status -ne 'passed') {
                    throw "Client test failed in $($result.stage): $($result.error)"
                }
                Write-Host "PASS packaged $Version / $Loader ($($result.checks.Count) checks)"
                return
            }
            if (((Get-Date) - $launchStarted).TotalSeconds -gt 30 -and @(Get-InstanceProcesses $instance.Path).Count -eq 0) {
                throw 'Packaged client exited before writing a test result'
            }
            Start-Sleep -Seconds 2
        }
        throw "Packaged client test exceeded ${ClientTimeoutSeconds}s timeout"
    } finally {
        Stop-InstanceProcesses $instance.Path
        Copy-ResultArtifacts $Version $Loader $instance.MinecraftDirectory $ServerLog
    }
}

if (-not (Test-Path $PrismExe)) {
    throw "Prism Launcher not found: $PrismExe"
}

New-Item -ItemType Directory -Force -Path $RuntimeRoot, $ResultsRoot, $InstancesRoot | Out-Null
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($version in $Versions) {
    $properties = Get-NodeProperties $version
    $server = $null
    try {
        $server = Start-TestServer $version $properties
        foreach ($loader in $Loaders) {
            try {
                Invoke-PackagedClientTest $version $loader $properties $server.Log
            } catch {
                $message = "$version / $loader - $($_.Exception.Message)"
                $failures.Add($message)
                Write-Host "FAIL $message" -ForegroundColor Red
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
    throw "Packaged client-test matrix failed:`n - $($failures -join "`n - ")"
}

Write-Host "All $($Versions.Count * $Loaders.Count) packaged client-test nodes passed."
