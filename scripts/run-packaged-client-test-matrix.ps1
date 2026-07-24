param(
    [string[]]$Versions = @('1.21.1', '1.21.4', '1.21.6', '1.21.8', '1.21.10', '1.21.11', '26.1'),
    [ValidateSet('fabric', 'neoforge')]
    [string[]]$Loaders = @('fabric', 'neoforge'),
    [int]$Port = 25565,
    [int]$ClientTimeoutSeconds = 600,
    [switch]$KeepOpen
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force
Assert-TestMatrixPowerShell

if (-not $IsWindows) {
    throw 'The packaged matrix currently requires Prism Launcher on Windows.'
}

$RepoRoot = Get-TestMatrixRepoRoot -ScriptRoot $PSScriptRoot
$PrismExe = Join-Path $env:LOCALAPPDATA 'Programs\PrismLauncher\prismlauncher.exe'
$PrismRoot = Join-Path $env:APPDATA 'PrismLauncher'
$InstancesRoot = Join-Path $PrismRoot 'instances'
$RuntimeRoot = Join-Path $RepoRoot 'build\client-test-runtime'
$ResultsRoot = Join-Path $RepoRoot 'build\packaged-client-test-results'
$Gradle = Get-TestGradleCommand -RepoRoot $RepoRoot

if ($KeepOpen -and ($Versions.Count -ne 1 -or $Loaders.Count -ne 1)) {
    throw '-KeepOpen requires exactly one version and one loader. Start multiple invocations with distinct -Port values for simultaneous clients.'
}

function Get-InstanceProcesses {
    param([string]$InstancePath)

    $escaped = [regex]::Escape($InstancePath)
    $escapedForward = [regex]::Escape($InstancePath.Replace('\', '/'))
    $instanceName = [regex]::Escape((Split-Path $InstancePath -Leaf))
    $pathPattern = "$escaped(?![A-Za-z0-9._-])"
    $forwardPathPattern = "$escapedForward(?![A-Za-z0-9._-])"
    $namePattern = "$instanceName(?![A-Za-z0-9._-])"
    return Get-CimInstance Win32_Process | Where-Object {
        $_.ProcessId -ne $PID -and
        $_.Name -match '^(java|javaw|prismlauncher)' -and
        $_.CommandLine -and
        ($_.CommandLine -match $pathPattern -or $_.CommandLine -match $forwardPathPattern -or $_.CommandLine -match $namePattern)
    }
}

function Stop-InstanceProcesses {
    param([string]$InstancePath)

    foreach ($process in @(Get-InstanceProcesses $InstancePath)) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

function Start-PrismClient {
    param(
        [hashtable]$Instance,
        [string]$LauncherDirectory
    )

    $mutex = [Threading.Mutex]::new($false, 'Global\MightyArchitectPrismLaunch')
    $acquired = $false
    $ownedLaunchers = @()
    try {
        $acquired = $mutex.WaitOne([TimeSpan]::FromMinutes(5))
        if (-not $acquired) {
            throw 'Timed out waiting for another packaged Prism launch to finish'
        }

        $existingLaunchers = @(Get-Process -Name 'prismlauncher' -ErrorAction SilentlyContinue)
        if ($existingLaunchers.Count -gt 0) {
            throw 'Prism Launcher is already open. Close it before running packaged automation; running Minecraft instances may remain open.'
        }

        $launcher = Start-Process -FilePath $PrismExe `
            -ArgumentList @('--launch', $Instance.Name) `
            -RedirectStandardOutput (Join-Path $LauncherDirectory 'prism.stdout.log') `
            -RedirectStandardError (Join-Path $LauncherDirectory 'prism.stderr.log') `
            -PassThru
        $ownedLaunchers = @($launcher)

        $deadline = (Get-Date).AddMinutes(3)
        do {
            $clients = @(
                Get-InstanceProcesses $Instance.Path |
                    Where-Object { $_.Name -match '^(java|javaw)' }
            )
            if ($clients.Count -gt 0) {
                return $clients
            }
            if ($launcher.HasExited) {
                throw "Prism exited before launching instance $($Instance.Name)"
            }
            Start-Sleep -Seconds 1
        } while ((Get-Date) -lt $deadline)

        throw "Prism did not launch instance $($Instance.Name)"
    } finally {
        foreach ($process in $ownedLaunchers) {
            if (-not $process.HasExited) {
                Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
                $process.WaitForExit()
            }
        }
        if ($acquired) {
            $mutex.ReleaseMutex()
        }
        $mutex.Dispose()
    }
}

function Write-PrismInstance {
    param(
        [string]$Version,
        [string]$Loader,
        [hashtable]$Properties,
        [switch]$KeepOpen
    )

    $instanceName = if ($KeepOpen) {
        "MightyArchitect-Manual-$Version-$Loader-$Port"
    } else {
        "MightyArchitect-Matrix-$Version-$Loader-$Port"
    }
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

    $java = (Resolve-TestJava -Version ([int]$Properties.java_version)).Replace('\', '/').Replace('java.exe', 'javaw.exe')
    $jvmArgs = "-Dmightyarchitect.clientTest.enabled=true " +
        "-Dmightyarchitect.clientTest.server=127.0.0.1:$Port " +
        "-Dmightyarchitect.clientTest.result=client-test-result.json " +
        "-Dmightyarchitect.clientTest.keepOpen=$($KeepOpen.IsPresent.ToString().ToLowerInvariant())"
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
        [string]$ServerLog,
        [System.Diagnostics.Process]$ServerProcess,
        [switch]$KeepOpen
    )

    Write-Host "=== PACKAGED CLIENT TEST $Version / $Loader ==="
    $instance = Write-PrismInstance -Version $Version -Loader $Loader -Properties $Properties -KeepOpen:$KeepOpen
    Stop-InstanceProcesses $instance.Path
    Install-Mods $Version $Loader $Properties $instance.MinecraftDirectory

    Remove-Item (Join-Path $instance.MinecraftDirectory 'logs'),
        (Join-Path $instance.MinecraftDirectory 'crash-reports'),
        (Join-Path $instance.MinecraftDirectory 'screenshots') -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item (Join-Path $instance.MinecraftDirectory 'client-test-result.json') -Force -ErrorAction SilentlyContinue

    $launcherDirectory = Join-Path $instance.MinecraftDirectory 'logs'
    New-Item -ItemType Directory -Force -Path $launcherDirectory | Out-Null
    $launchedClients = @(Start-PrismClient -Instance $instance -LauncherDirectory $launcherDirectory)

    $resultPath = Join-Path $instance.MinecraftDirectory 'client-test-result.json'
    $deadline = (Get-Date).AddSeconds($ClientTimeoutSeconds)
    $launchStarted = Get-Date
    $leaveRunning = $false
    $artifactsCopied = $false
    try {
        while ((Get-Date) -lt $deadline) {
            Assert-NoCrash $instance.MinecraftDirectory
            if (Test-Path $resultPath) {
                $result = Get-Content $resultPath -Raw | ConvertFrom-Json
                Copy-ResultArtifacts $Version $Loader $instance.MinecraftDirectory $ServerLog
                if ($result.status -ne 'passed') {
                    throw "Client test failed in $($result.stage): $($result.error)"
                }
                if ($KeepOpen -and -not $result.keepOpen) {
                    throw 'Client passed but did not acknowledge keep-open mode'
                }
                Write-Host "PASS packaged $Version / $Loader ($($result.checks.Count) checks)"
                if ($KeepOpen) {
                    Copy-ResultArtifacts $Version $Loader $instance.MinecraftDirectory $ServerLog
                    $artifactsCopied = $true
                    $clientIdentities = @(
                        $launchedClients |
                            ForEach-Object { Get-Process -Id $_.ProcessId -ErrorAction SilentlyContinue } |
                            Where-Object { $_ } |
                            ForEach-Object { Get-TestProcessIdentity -Process $_ }
                    )
                    if ($clientIdentities.Count -eq 0) {
                        throw 'Client passed but no owned Minecraft process remained for keep-open mode'
                    }
                    $session = [pscustomobject]@{
                        mode = 'packaged'
                        version = $Version
                        loader = $Loader
                        port = $Port
                        serverProcess = Get-TestProcessIdentity -Process $ServerProcess
                        clientProcesses = $clientIdentities
                        instancePath = $instance.Path
                        createdAt = (Get-Date).ToString('o')
                    }
                    $manifest = Write-TestSessionManifest -RepoRoot $RepoRoot `
                        -FileName "packaged-$Version-$Loader-$Port.json" -Session $session
                    $leaveRunning = $true
                    return [pscustomobject]@{
                        ManifestPath = $manifest
                        Session = $session
                    }
                }
                return
            }
            if (((Get-Date) - $launchStarted).TotalSeconds -gt 30 -and @(Get-InstanceProcesses $instance.Path).Count -eq 0) {
                throw 'Packaged client exited before writing a test result'
            }
            Start-Sleep -Seconds 2
        }
        throw "Packaged client test exceeded ${ClientTimeoutSeconds}s timeout"
    } finally {
        if (-not $leaveRunning) {
            Stop-InstanceProcesses $instance.Path
        }
        if (-not $artifactsCopied) {
            Copy-ResultArtifacts $Version $Loader $instance.MinecraftDirectory $ServerLog
        }
    }
}

if (-not (Test-Path $PrismExe)) {
    throw "Prism Launcher not found: $PrismExe"
}

New-Item -ItemType Directory -Force -Path $RuntimeRoot, $ResultsRoot, $InstancesRoot | Out-Null
$failures = [System.Collections.Generic.List[string]]::new()
$keptOpenSession = $null

foreach ($version in $Versions) {
    $properties = Get-TestNodeProperties -RepoRoot $RepoRoot -Version $version
    $server = $null
    try {
        $serverNodeId = if ($KeepOpen) { "$version-$($Loaders[0])-$Port" } else { $version }
        $server = Start-TestVanillaServer -NodeId $serverNodeId -Properties $properties -RuntimeRoot $RuntimeRoot `
            -Port $Port -Motd 'Mighty Architect Packaged Client Test'
        foreach ($loader in $Loaders) {
            try {
                $clientSession = Invoke-PackagedClientTest -Version $version -Loader $loader -Properties $properties `
                    -ServerLog $server.Log -ServerProcess $server.Process -KeepOpen:$KeepOpen
                if ($KeepOpen) {
                    $keptOpenSession = $clientSession
                }
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
        if (-not $keptOpenSession) {
            Stop-TestProcessTree -Process $server.Process
        }
    }
}

if ($failures.Count -gt 0) {
    throw "Packaged client-test matrix failed:`n - $($failures -join "`n - ")"
}

if ($keptOpenSession) {
    $session = $keptOpenSession.Session
    $stopScript = Join-Path $PSScriptRoot 'stop-kept-open-clients.ps1'
    Write-Host "KEEP OPEN packaged $($session.version) / $($session.loader)"
    Write-Host "Server: 127.0.0.1:$Port (PID $($session.serverProcess.processId))"
    Write-Host "Instance: $($session.instancePath)"
    Write-Host "Manifest: $($keptOpenSession.ManifestPath)"
    Write-Host "Stop with: pwsh -File `"$stopScript`" -ManifestPath `"$($keptOpenSession.ManifestPath)`""
    return
}

Write-Host "All $($Versions.Count * $Loaders.Count) packaged client-test nodes passed."
