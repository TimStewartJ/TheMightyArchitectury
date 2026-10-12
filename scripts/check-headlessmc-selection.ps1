$ErrorActionPreference = 'Stop'
$module = Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force -PassThru
$testRoot = Join-Path (Join-Path (Split-Path $PSScriptRoot) 'build') 'headlessmc-selection-test'

# Exercise the real initializer with a fake launcher inventory; no downloads or Java processes.
& $module {
    param($TestRoot)

    function script:Get-TestHeadlessMcLauncher { param($RepoRoot) return 'fake-launcher.jar' }
    function script:Resolve-TestJava { param($Version) return 'fake-java' }
    function script:Get-TestHeadlessMcVersions {
        param($Root, $Launcher, $Java)
        return $script:Installed
    }
    function script:Invoke-TestHeadlessMc {
        param($Root, $Launcher, $Java, $Command)
        $script:InstallCommand = $Command
        $script:Installed = $script:AfterInstall
    }

    $vanilla = [pscustomobject]@{ Name = '26.3'; Parent = '' }
    $old = [pscustomobject]@{ Name = 'neoforge-26.3.0.6-beta'; Parent = '26.3' }
    $pinned = [pscustomobject]@{ Name = 'neoforge-26.3.0.76'; Parent = '26.3' }
    $wrongParent = [pscustomobject]@{ Name = $pinned.Name; Parent = '26.2' }
    $forgeVanilla = [pscustomobject]@{ Name = '1.20.1'; Parent = '' }
    $forge = [pscustomobject]@{ Name = '1.20.1-forge-47.3.0'; Parent = '1.20.1' }
    $otherLoader = [pscustomobject]@{ Name = 'neoforge-47.3.0'; Parent = '1.20.1' }
    $cases = @(
        @{ Name = 'cached different loader must install the pinned version'; MC = '26.3'; Loader = 'neoforge'; Uid = '26.3.0.76'; Before = @($vanilla, $old); After = @($vanilla, $old, $pinned); Expected = $pinned.Name; Install = $true }
        @{ Name = 'matching loader is reused'; MC = '26.3'; Loader = 'neoforge'; Uid = '26.3.0.76'; Before = @($vanilla, $pinned); Expected = $pinned.Name; Install = $false }
        @{ Name = 'old and matching loaders select the exact pin'; MC = '26.3'; Loader = 'neoforge'; Uid = '26.3.0.76'; Before = @($vanilla, $old, $pinned); Expected = $pinned.Name; Install = $false }
        @{ Name = 'matching name with wrong parent must not be reused'; MC = '26.3'; Loader = 'neoforge'; Uid = '26.3.0.76'; Before = @($vanilla, $wrongParent); After = @($vanilla, $wrongParent, $pinned); Expected = $pinned.Name; Install = $true }
        @{ Name = 'NeoForge must not satisfy a Forge request'; MC = '1.20.1'; Loader = 'forge'; Uid = '47.3.0'; Before = @($forgeVanilla, $otherLoader); After = @($forgeVanilla, $otherLoader, $forge); Expected = $forge.Name; Install = $true }
        @{ Name = 'matching Forge loader is reused'; MC = '1.20.1'; Loader = 'forge'; Uid = '47.3.0'; Before = @($forgeVanilla, $forge); Expected = $forge.Name; Install = $false }
        @{ Name = 'installer missing requested version must fail'; MC = '26.3'; Loader = 'neoforge'; Uid = '26.3.0.76'; Before = @($vanilla, $old); After = @($vanilla, $old); Failure = 'HeadlessMc did not install neoforge 26.3.0.76 for Minecraft 26.3'; Install = $true }
    )

    foreach ($case in $cases) {
        $script:Installed = $case.Before
        $script:AfterInstall = $case.After
        $script:InstallCommand = $null
        $failure = $null
        $selected = $null
        try {
            $selected = Initialize-TestHeadlessMcInstance -RepoRoot $TestRoot -MinecraftVersion $case.MC `
                -Loader $case.Loader -LoaderUid $case.Uid -JavaVersion 25 -GameDirectory (Join-Path $TestRoot 'game')
        } catch {
            $failure = $_.Exception.Message
        }
        if ($failure -ne $case.Failure -or $selected -ne $case.Expected) {
            throw "$($case.Name): selected '$selected', failure '$failure'"
        }
        $expectedCommand = if ($case.Install) { "$($case.Loader) $($case.MC) --uid $($case.Uid) --java 25" } else { $null }
        if ($script:InstallCommand -ne $expectedCommand) {
            throw "$($case.Name): install command '$($script:InstallCommand)', expected '$expectedCommand'"
        }
        Write-Host "PASS $($case.Name)"
    }
    Write-Host "All $($cases.Count) HeadlessMc selection checks passed."
} $testRoot
