[CmdletBinding()]
param(
    [string]$Version = '1.20.2',
    [string]$Loader = 'fabric',
    [int]$Port = 25566,
    [int]$TimeoutSeconds = 900
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

if (-not $IsLinux) {
    throw 'The recorded player journey currently requires Linux/X11 for xdotool and x11grab.'
}
if ($Loader -ne 'fabric') {
    throw 'The initial recorded player journey supports the packaged Fabric client only.'
}
foreach ($command in @('ffmpeg', 'xdotool')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "$command is required for the recorded player journey."
    }
}
if (-not $env:DISPLAY) {
    throw 'DISPLAY is not set. Run this script inside Xvfb.'
}

Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force
Assert-TestMatrixPowerShell

$repoRoot = Get-TestMatrixRepoRoot -ScriptRoot $PSScriptRoot
$properties = Get-TestNodeProperties -RepoRoot $repoRoot -Version $Version
$gradle = Get-TestGradleCommand -RepoRoot $repoRoot
$projectDirectory = Join-Path (Join-Path (Join-Path $repoRoot $Loader) 'versions') $Version
$runDirectory = Join-Path $projectDirectory 'run-player-journey'
$resultDirectory = Join-Path (Join-Path (Join-Path $repoRoot 'build') 'player-journey-results') "$Version-$Loader"
$statePath = Join-Path (Join-Path $projectDirectory 'build') 'player-journey/state.json'
$resultPath = Join-Path $resultDirectory 'result.json'
$timelinePath = Join-Path $resultDirectory 'timeline.md'
$videoPath = Join-Path $resultDirectory 'journey.mp4'
$contactSheetPath = Join-Path $resultDirectory 'contact-sheet.png'
$gradleStdout = Join-Path $resultDirectory 'gradle.stdout.log'
$gradleStderr = Join-Path $resultDirectory 'gradle.stderr.log'
$ffmpegLog = Join-Path $resultDirectory 'ffmpeg.log'
$serverRuntimeRoot = Join-Path (Join-Path $repoRoot 'build') 'player-journey-runtime'
$checks = [Collections.Generic.List[string]]::new()
$timelineStart = Get-Date
$clientProcess = $null
$server = $null
$recorder = $null
$failure = $null

Remove-Item $resultDirectory, $runDirectory -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $statePath -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $resultDirectory | Out-Null
Write-TestClientOptions -MinecraftDirectory $runDirectory
Add-Content (Join-Path $runDirectory 'options.txt') @(
    'rawMouseInput:false'
    'overrideWidth:1280'
    'overrideHeight:720'
) -Encoding ascii

@(
    '# Player journey timeline'
    ''
    "| Time | Action |"
    "| ---: | --- |"
) | Set-Content $timelinePath -Encoding utf8

function Add-JourneyTimeline {
    param([string]$Action)

    $elapsed = (Get-Date) - $timelineStart
    $stamp = '{0:mm\:ss\.fff}' -f $elapsed
    "| $stamp | $($Action.Replace('|', '\|')) |" | Add-Content $timelinePath -Encoding utf8
    Write-Host "[$stamp] $Action"
}

function Read-JourneyState {
    if (-not (Test-Path $statePath)) {
        return $null
    }
    try {
        return Get-Content $statePath -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Wait-JourneyState {
    param(
        [string]$Description,
        [scriptblock]$Predicate,
        [int]$Seconds = 120
    )

    $deadline = (Get-Date).AddSeconds($Seconds)
    while ((Get-Date) -lt $deadline) {
        if ($clientProcess -and $clientProcess.HasExited) {
            $tail = if (Test-Path $gradleStdout) { (Get-Content $gradleStdout -Tail 40) -join "`n" } else { '' }
            throw "Client exited while waiting for $Description (code $($clientProcess.ExitCode))`n$tail"
        }
        $state = Read-JourneyState
        if ($state -and (& $Predicate $state)) {
            Add-JourneyTimeline "Observed: $Description"
            return $state
        }
        Start-Sleep -Milliseconds 250
    }
    $last = Read-JourneyState
    throw "Timed out waiting for $Description. Last state: $($last | ConvertTo-Json -Compress)"
}

function Add-JourneyCheck {
    param([string]$Description)
    $checks.Add($Description)
    Add-JourneyTimeline "PASS: $Description"
}

function Invoke-Xdotool {
    param([string[]]$Arguments)
    Add-JourneyTimeline "Input: xdotool $($Arguments -join ' ')"
    & xdotool @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "xdotool exited ${LASTEXITCODE}: $($Arguments -join ' ')"
    }
}

function Send-JourneyKey {
    param([string]$Key)
    Invoke-Xdotool -Arguments @('key', '--clearmodifiers', $Key)
}

function Send-JourneyText {
    param([string]$Text)
    Invoke-Xdotool -Arguments @('type', '--clearmodifiers', '--delay', '8', $Text)
}

function Send-JourneyCommand {
    param([string]$Command)
    Send-JourneyKey 't'
    Wait-JourneyState 'chat screen opened' { param($s) $s.screen -match 'ChatScreen' } | Out-Null
    Send-JourneyText $Command
    Send-JourneyKey 'Return'
    Wait-JourneyState 'chat command submitted' { param($s) -not $s.screen } | Out-Null
}

function Focus-JourneyWindow {
    $deadline = (Get-Date).AddSeconds(180)
    while ((Get-Date) -lt $deadline) {
        $windowIds = @(& xdotool search --onlyvisible --name 'Minecraft' 2>$null)
        if ($LASTEXITCODE -eq 0 -and $windowIds.Count -gt 0) {
            $windowId = "$($windowIds[-1])".Trim()
            Invoke-Xdotool -Arguments @('windowactivate', '--sync', $windowId)
            Invoke-Xdotool -Arguments @('windowsize', $windowId, '1280', '720')
            Invoke-Xdotool -Arguments @('windowmove', $windowId, '0', '0')
            return $windowId
        }
        Start-Sleep -Seconds 1
    }
    throw 'Minecraft window did not appear.'
}

function Get-JourneyWindowOrigin {
    param([string]$WindowId)

    $geometry = @(& xdotool getwindowgeometry --shell $WindowId)
    $values = @{}
    foreach ($line in $geometry) {
        if ($line -match '^([^=]+)=(.*)$') {
            $values[$matches[1]] = $matches[2]
        }
    }
    if (-not $values.ContainsKey('X') -or -not $values.ContainsKey('Y')) {
        throw "Unable to read window geometry for $WindowId"
    }
    return [pscustomobject]@{ X = [int]$values.X; Y = [int]$values.Y }
}

function Aim-JourneyAt {
    param(
        [int]$X,
        [int]$Z
    )

    $xCoordinate = ($X + 0.5).ToString([Globalization.CultureInfo]::InvariantCulture)
    $zCoordinate = ($Z + 0.5).ToString([Globalization.CultureInfo]::InvariantCulture)
    Send-JourneyCommand "/tp ArchitectTest $xCoordinate 7 $zCoordinate 0 90"
    Wait-JourneyState "crosshair targets $X,3,${Z}" {
        param($s)
        $null -ne $s.targetX -and [int]$s.targetX -eq $X -and [int]$s.targetY -eq 3 -and
            [int]$s.targetZ -eq $Z
    } | Out-Null
}

function Start-JourneyComposition {
    Send-JourneyKey 'g'
    Wait-JourneyState 'architect menu opened' { param($s) $s.screen -eq 'ArchitectMenuScreen' } | Out-Null
    Send-JourneyKey '1'
    Wait-JourneyState 'composer entered from theme selection' {
        param($s)
        $s.phase -eq 'Composing' -and -not $s.screen -and $s.themeName -eq 'Medieval'
    } | Out-Null

    Aim-JourneyAt -X 0 -Z 0
    $before = Read-JourneyState
    Invoke-Xdotool -Arguments @('click', '3')
    Wait-JourneyState 'first room corner accepted through mouse hook' {
        param($s)
        [int]$s.mouseButtonEvents -gt [int]$before.mouseButtonEvents
    } | Out-Null

    Aim-JourneyAt -X 4 -Z 4
    $before = Read-JourneyState
    Invoke-Xdotool -Arguments @('click', '3')
    Wait-JourneyState '5x5 room created' {
        param($s)
        [int]$s.mouseButtonEvents -gt [int]$before.mouseButtonEvents -and [int]$s.roomCount -eq 1
    } | Out-Null

    $before = Read-JourneyState
    Invoke-Xdotool -Arguments @('click', '4')
    $after = Wait-JourneyState 'room scroll added a floor without cycling the hotbar' {
        param($s)
        [int]$s.scrollEvents -gt [int]$before.scrollEvents -and [int]$s.roomCount -ge 2
    }
    if ([int]$after.selectedHotbarSlot -ne [int]$before.selectedHotbarSlot) {
        throw "Composer scroll also changed hotbar slot $($before.selectedHotbarSlot) -> $($after.selectedHotbarSlot)"
    }
    Add-JourneyCheck 'real wheel input reached the production scroll hook and was consumed by the room tool'

    Send-JourneyKey 'g'
    Wait-JourneyState 'composer menu opened for finish' { param($s) $s.screen -eq 'ArchitectMenuScreen' } | Out-Null
    Send-JourneyKey 'f'
    Wait-JourneyState 'real ground plan generated a rendered preview' {
        param($s)
        $s.phase -eq 'Previewing' -and -not $s.screen -and [int]$s.previewBlocks -gt 0 -and
            [bool]$s.rendererGeometry
    } -Seconds 180 | Out-Null
    Add-JourneyCheck 'theme selection, two world clicks, floor scroll, and finish produced a non-empty preview'
}

function Select-JourneyPalette {
    param([string]$WindowId)

    $before = Read-JourneyState
    Send-JourneyKey 'g'
    Wait-JourneyState 'preview menu opened for palette selection' {
        param($s) $s.screen -eq 'ArchitectMenuScreen'
    } | Out-Null
    Send-JourneyKey 'c'
    $picker = Wait-JourneyState 'palette picker exposed a real clickable palette target' {
        param($s)
        $s.screen -eq 'PalettePickerScreen' -and $null -ne $s.paletteTargetX
    }

    $origin = Get-JourneyWindowOrigin -WindowId $WindowId
    $targetX = $origin.X + [int][Math]::Round([double]$picker.paletteTargetX)
    $targetY = $origin.Y + [int][Math]::Round([double]$picker.paletteTargetY)
    Invoke-Xdotool -Arguments @('mousemove', '--sync', "$targetX", "$targetY")
    Invoke-Xdotool -Arguments @('click', '1')
    Wait-JourneyState 'palette click changed the active palette' {
        param($s)
        $s.screen -eq 'PalettePickerScreen' -and $s.paletteName -eq $picker.paletteTargetName
    } | Out-Null
    Send-JourneyKey 'e'
    Wait-JourneyState 'palette picker closed back to preview' {
        param($s) $s.phase -eq 'Previewing' -and -not $s.screen
    } | Out-Null
    Add-JourneyCheck 'real pointer movement and click changed the preview palette'
}

function Save-JourneyBuild {
    Send-JourneyKey 'g'
    Wait-JourneyState 'preview menu opened for save' { param($s) $s.screen -eq 'ArchitectMenuScreen' } | Out-Null
    Send-JourneyKey 's'
    Wait-JourneyState 'save-name prompt opened' { param($s) $s.screen -eq 'TextInputPromptScreen' } | Out-Null
    Send-JourneyText 'e2e_journey'
    Send-JourneyKey 'Return'
    Wait-JourneyState 'save completed and architect unloaded' {
        param($s) $s.phase -eq 'Empty' -and -not $s.screen
    } | Out-Null

    $saved = Join-Path (Join-Path $runDirectory 'schematics') 'e2e_journey.nbt'
    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline -and -not (Test-Path $saved)) {
        Start-Sleep -Milliseconds 250
    }
    if (-not (Test-Path $saved) -or (Get-Item $saved).Length -le 0) {
        throw "Saved schematic was not created: $saved"
    }
    Add-JourneyCheck 'real text entry and Enter saved a non-empty schematic file'
}

function Print-JourneyBuild {
    Aim-JourneyAt -X 7 -Z 7
    Send-JourneyKey 'g'
    Wait-JourneyState 'preview menu opened for print' { param($s) $s.screen -eq 'ArchitectMenuScreen' } | Out-Null
    Send-JourneyKey 'p'
    Wait-JourneyState 'vanilla-server command fallback started' {
        param($s) $s.phase -eq 'PrintingToMultiplayer'
    } | Out-Null
    $printed = Wait-JourneyState 'printed block identities match the preview in the server world' {
        param($s)
        $s.phase -eq 'Empty' -and [bool]$s.printedWorldBlockTypesMatch -and
            [int]$s.expectedPrintedBlocks -gt 0
    } -Seconds 240
    Add-JourneyCheck "real menu input printed all $($printed.expectedPrintedBlocks) block identities through the vanilla command fallback ($($printed.matchingPrintedBlocks) exact settled states)"
}

function Write-JourneyResult {
    param(
        [string]$Status,
        [string]$ErrorMessage
    )

    $state = Read-JourneyState
    $result = [ordered]@{
        status = $Status
        version = $Version
        loader = $Loader
        checks = @($checks)
        durationSeconds = [Math]::Round(((Get-Date) - $timelineStart).TotalSeconds, 3)
        finalState = $state
    }
    if ($ErrorMessage) {
        $result['error'] = $ErrorMessage
    }
    $result | ConvertTo-Json -Depth 10 | Set-Content $resultPath -Encoding utf8

    @(
        '# Recorded player journey'
        ''
        "| Field | Value |"
        "| --- | --- |"
        "| Target | Minecraft $Version / $Loader |"
        "| Status | **$Status** |"
        "| Checks | $($checks.Count) |"
        "| Duration | $([Math]::Round($result.durationSeconds, 1)) seconds |"
        "| Recording | ``journey.mp4`` in the uploaded artifact |"
        ''
        '## Checks'
        ''
        $(if ($checks.Count) { $checks | ForEach-Object { "- [x] $_" } } else { '- [ ] No journey check completed.' })
        $(if ($ErrorMessage) { '', '## Failure', '', "``$ErrorMessage``" })
    ) | Set-Content (Join-Path $resultDirectory 'summary.md') -Encoding utf8
}

try {
    Add-JourneyTimeline 'Starting vanilla fixture server'
    $server = Start-TestVanillaServer -NodeId "$Version-$Loader-$Port" -Properties $properties `
        -RuntimeRoot $serverRuntimeRoot -Port $Port -Motd 'Mighty Architect Player Journey' `
        -OfflineOperator 'ArchitectTest' -Creative

    $recorderArguments = @(
        '-y', '-nostdin', '-loglevel', 'warning',
        '-f', 'x11grab', '-draw_mouse', '1', '-framerate', '20',
        '-video_size', '1280x720', '-i', "$($env:DISPLAY).0+0,0",
        '-an', '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '28',
        '-pix_fmt', 'yuv420p', '-movflags', '+faststart', '-t', "$TimeoutSeconds",
        $videoPath
    )
    $recorder = Start-Process -FilePath 'ffmpeg' -ArgumentList $recorderArguments `
        -RedirectStandardError $ffmpegLog -PassThru
    Start-Sleep -Seconds 1
    if ($recorder.HasExited) {
        $tail = if (Test-Path $ffmpegLog) { (Get-Content $ffmpegLog -Tail 30) -join "`n" } else { '' }
        throw "FFmpeg exited before the client launched (code $($recorder.ExitCode))`n$tail"
    }
    Add-JourneyTimeline 'FFmpeg recording started'

    $env:MIGHTYARCHITECT_PLAYER_JOURNEY_SERVER = "127.0.0.1:$Port"
    $gradleArguments = @(
        ":${Loader}:${Version}:runProductionPlayerJourney",
        '--console=plain',
        '--no-daemon',
        '-Porg.gradle.java.installations.fromEnv=JAVA_HOME_17_X64,JAVA_HOME_21_X64,JAVA_HOME_25_X64'
    )
    $clientProcess = Start-Process -FilePath $gradle -ArgumentList $gradleArguments `
        -WorkingDirectory $repoRoot -RedirectStandardOutput $gradleStdout `
        -RedirectStandardError $gradleStderr -PassThru

    $windowId = Focus-JourneyWindow
    Wait-JourneyState 'packaged client joined the fixture world' {
        param($s) [bool]$s.worldReady -and -not [bool]$s.overlayVisible -and -not $s.screen
    } -Seconds 240 | Out-Null
    Add-JourneyCheck 'packaged production client launched, focused, and joined a vanilla server'

    Send-JourneyCommand '/fill -8 3 -8 8 3 8 minecraft:stone'
    Aim-JourneyAt -X 0 -Z 0
    Add-JourneyCheck 'real chat input prepared a deterministic world fixture'

    $beforeIdleScroll = Read-JourneyState
    Invoke-Xdotool -Arguments @('click', '5')
    $afterIdleScroll = Wait-JourneyState 'idle wheel input cycled the vanilla hotbar' {
        param($s)
        [int]$s.scrollEvents -gt [int]$beforeIdleScroll.scrollEvents -and
            [int]$s.selectedHotbarSlot -ne [int]$beforeIdleScroll.selectedHotbarSlot
    }
    Add-JourneyCheck "idle scroll changed hotbar slot $($beforeIdleScroll.selectedHotbarSlot) -> $($afterIdleScroll.selectedHotbarSlot)"

    Start-JourneyComposition
    Select-JourneyPalette -WindowId $windowId
    Send-JourneyKey 'F2'
    Save-JourneyBuild

    Start-JourneyComposition
    Send-JourneyKey 'F2'
    Print-JourneyBuild
    Send-JourneyKey 'F2'

    Add-JourneyCheck 'packaged client remained responsive through both complete journeys'
    Write-JourneyResult -Status 'passed' -ErrorMessage ''
} catch {
    $failure = $_
    Add-JourneyTimeline "FAIL: $($_.Exception.Message)"
    Write-JourneyResult -Status 'failed' -ErrorMessage $_.Exception.Message
} finally {
    Remove-Item Env:MIGHTYARCHITECT_PLAYER_JOURNEY_SERVER -ErrorAction SilentlyContinue

    if ($clientProcess) {
        Stop-TestProcessTree -Process $clientProcess
    }
    if ($server) {
        Stop-TestProcessTree -Process $server.Process
        if (Test-Path $server.Log) {
            Copy-Item $server.Log (Join-Path $resultDirectory 'server.log') -Force
        }
    }
    if ($recorder -and -not $recorder.HasExited) {
        & /bin/kill -INT $recorder.Id
        if (-not $recorder.WaitForExit(30000)) {
            Stop-TestProcessTree -Process $recorder
        }
    }

    if (Test-Path $videoPath) {
        & ffmpeg -y -loglevel error -i $videoPath `
            -vf 'fps=1/12,scale=320:-1,tile=4x4' -frames:v 1 $contactSheetPath
    }
    $screenshots = Join-Path $runDirectory 'screenshots'
    if (Test-Path $screenshots) {
        Copy-Item $screenshots (Join-Path $resultDirectory 'screenshots') -Recurse -Force
    }
}

if (-not $failure -and (-not (Test-Path $videoPath) -or (Get-Item $videoPath).Length -le 0)) {
    $failure = [InvalidOperationException]::new('The journey passed but its MP4 recording was not produced.')
    Add-JourneyTimeline "FAIL: $($failure.Message)"
    Write-JourneyResult -Status 'failed' -ErrorMessage $failure.Message
}

if ($failure) {
    throw $failure
}

Write-Host "Recorded player journey passed. Evidence: $resultDirectory"
