param(
    [string[]]$Versions = @('1.21.1', '1.21.4', '1.21.6', '1.21.8', '1.21.10', '1.21.11', '26.1'),
    [string[]]$Loaders = @('fabric', 'neoforge')
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force
Assert-TestMatrixPowerShell

$Versions = Expand-TestListArgument -Value $Versions
$Loaders = Expand-TestListArgument -Value $Loaders -Allowed @('fabric', 'neoforge')

$RepoRoot = Get-TestMatrixRepoRoot -ScriptRoot $PSScriptRoot
$Gradle = Get-TestGradleCommand -RepoRoot $RepoRoot

function Get-ProductionJar {
    param(
        [string]$Version,
        [string]$Loader
    )

    $libs = Join-Path (Join-Path (Join-Path (Join-Path $RepoRoot $Loader) 'versions') $Version) 'build/libs'
    $jar = Get-ChildItem $libs -Filter "*-$Loader.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|dev|raw|client-test' } |
        Select-Object -First 1
    if (-not $jar) {
        throw "Production jar not found for $Version/$Loader in $libs"
    }
    return $jar.FullName
}

function Get-ClientTestJar {
    param(
        [string]$Version,
        [string]$Loader
    )

    $libs = Join-Path (Join-Path (Join-Path (Join-Path $RepoRoot $Loader) 'versions') $Version) 'build/libs'
    $jar = Get-ChildItem $libs -Filter "*-$Loader-client-test.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'client-test-dev' } |
        Select-Object -First 1
    if (-not $jar) {
        throw "Client-test jar not found for $Version/$Loader in $libs"
    }
    return $jar.FullName
}

$tasks = [System.Collections.Generic.List[string]]::new()
foreach ($version in $Versions) {
    foreach ($loader in $Loaders) {
        $tasks.Add(":${loader}:${version}:build")
        $tasks.Add(":${loader}:${version}:buildClientTestMod")
    }
}

Write-Host "Preparing runtime artifacts for $($Versions.Count * $Loaders.Count) target(s)"
$arguments = $tasks + @('--console=plain', '-Porg.gradle.java.installations.fromEnv=JAVA_HOME_21_X64,JAVA_HOME_25_X64')
& $Gradle @arguments
if ($LASTEXITCODE -ne 0) {
    throw "Gradle artifact preparation failed with exit code $LASTEXITCODE"
}

$targets = [System.Collections.Generic.List[object]]::new()
foreach ($version in $Versions) {
    $properties = Get-TestNodeProperties -RepoRoot $RepoRoot -Version $version
    foreach ($loader in $Loaders) {
        $modJar = Get-ProductionJar -Version $version -Loader $loader
        $testJar = Get-ClientTestJar -Version $version -Loader $loader

        $dependencyJars = [System.Collections.Generic.List[string]]::new()
        if ($loader -eq 'fabric') {
            $dependencyJars.Add((Find-TestGradleArtifact -Group 'net.fabricmc.fabric-api' -Module 'fabric-api' -Version $properties.fabric_api_version))
        }

        $targets.Add([ordered]@{
            version = $version
            loader = $loader
            minecraftVersion = $properties.minecraft_version
            javaVersion = [int]$properties.java_version
            fabricLoaderVersion = $properties.fabric_loader_version
            neoforgeVersion = $properties.neoforge_version
            modJar = $modJar
            modJarSha256 = (Get-FileHash $modJar -Algorithm SHA256).Hash
            testJar = $testJar
            testJarSha256 = (Get-FileHash $testJar -Algorithm SHA256).Hash
            dependencyJars = @($dependencyJars)
        })
    }
}

$gitCommit = (& git -C $RepoRoot rev-parse HEAD 2>$null)
$manifest = [ordered]@{
    createdAt = (Get-Date).ToUniversalTime().ToString('o')
    gitCommit = if ($gitCommit) { $gitCommit.Trim() } else { $null }
    targets = @($targets)
}

$manifestPath = Get-RuntimeArtifactManifestPath -RepoRoot $RepoRoot
New-Item -ItemType Directory -Force -Path (Split-Path $manifestPath) | Out-Null
$temporary = "$manifestPath.tmp"
$manifest | ConvertTo-Json -Depth 8 | Set-Content $temporary -Encoding utf8
Move-Item $temporary $manifestPath -Force

Write-Host "Wrote $($targets.Count) target(s) to $manifestPath"
