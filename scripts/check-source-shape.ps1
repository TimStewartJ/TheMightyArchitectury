<#
.SYNOPSIS
    Fails when a source file carries more than one whole-file copy of itself.

.DESCRIPTION
    A Stonecutter guard is meant to wrap the lines that genuinely differ between Minecraft
    versions. When instead it wraps the *entire file*, every version gets its own copy of the
    shared logic and a fix applied to one copy silently misses the others - which is exactly how
    PR #34 fixed two HudTextBuffer arms while PR #32 concurrently added two more with the old
    constant.

    Marker counts do not find this: a whole-file duplicate is one guard wrapping everything, so it
    scores two marker lines across hundreds of duplicated ones.

    The reliable signal is a repeated `package` declaration. Java allows exactly one per
    compilation unit, so a file that contains two has two copies of itself - and, unlike counting
    repeated type declarations, it does not fire on a file that merely guards its `extends`,
    `implements` or annotation line, where there is only one body and nothing can drift.

.PARAMETER Path
    Source roots to scan. Defaults to the four loader source trees.

.PARAMETER Detail
    Also print the line number of each package declaration found.
#>
[CmdletBinding()]
param(
    [string[]]$Path = @('common/src', 'fabric/src', 'neoforge/src', 'forge/src'),
    [switch]$Detail
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

# Matches an active `package a.b.c;` and the commented-out form Stonecutter writes for an inactive
# arm, `/*package a.b.c;`, which is the form a duplicated copy actually appears in.
$packageDeclaration = '^\s*(/\*)?package\s+[A-Za-z_][A-Za-z0-9_.]*\s*;'

$roots = foreach ($candidate in $Path) {
    $full = Join-Path $repoRoot $candidate
    if (Test-Path -LiteralPath $full) { $full }
}

if (-not $roots) {
    throw "None of the requested source roots exist under $repoRoot : $($Path -join ', ')"
}

$offenders = @()
$scanned = 0

foreach ($file in Get-ChildItem -Path $roots -Recurse -Filter *.java -File) {
    $scanned++
    $hits = @(Select-String -LiteralPath $file.FullName -Pattern $packageDeclaration)
    if ($hits.Count -le 1) { continue }

    $offenders += [pscustomobject]@{
        Path        = [System.IO.Path]::GetRelativePath($repoRoot, $file.FullName).Replace('\', '/')
        Copies      = $hits.Count
        LineNumbers = @($hits.LineNumber)
    }
}

Write-Host "Scanned $scanned Java files under: $($Path -join ', ')"

if (-not $offenders) {
    Write-Host 'No whole-file guarded duplicates found.'
    exit 0
}

Write-Host ''
foreach ($offender in $offenders | Sort-Object -Property Copies -Descending) {
    $message = "$($offender.Path) declares its package $($offender.Copies) times - the whole file is duplicated per Stonecutter arm. Guard only the lines that differ."
    if ($env:GITHUB_ACTIONS -eq 'true') {
        Write-Host "::error file=$($offender.Path),line=$($offender.LineNumbers[1])::$message"
    }
    else {
        Write-Host "ERROR: $message"
    }
    if ($Detail) {
        Write-Host "       package declarations at lines: $($offender.LineNumbers -join ', ')"
    }
}

Write-Host ''
Write-Host "$($offenders.Count) file(s) contain whole-file guarded duplicates."
exit 1
