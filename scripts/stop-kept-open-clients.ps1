param(
    [string]$ManifestPath,
    [switch]$All
)

$ErrorActionPreference = 'Stop'

Import-Module (Join-Path $PSScriptRoot 'TestMatrix.Common.psm1') -Force
Assert-TestMatrixPowerShell

$repoRoot = Get-TestMatrixRepoRoot -ScriptRoot $PSScriptRoot
$manifestDirectory = Join-Path (Join-Path $repoRoot 'build') 'kept-open-sessions'

if ($All) {
    $manifests = @(Get-ChildItem $manifestDirectory -Filter '*.json' -File -ErrorAction SilentlyContinue)
} elseif ($ManifestPath) {
    $manifests = @(Get-Item $ManifestPath)
} else {
    throw 'Provide -ManifestPath <path> or -All.'
}

if ($manifests.Count -eq 0) {
    Write-Host 'No kept-open client manifests found.'
    return
}

foreach ($manifest in $manifests) {
    $session = Get-Content $manifest.FullName -Raw | ConvertFrom-Json
    Write-Host "Stopping $($session.mode) $($session.version) / $($session.loader) on port $($session.port)"

    foreach ($identity in @($session.clientProcesses)) {
        Stop-TestOwnedProcess -Identity $identity | Out-Null
    }

    Stop-TestOwnedProcess -Identity $session.serverProcess | Out-Null
    Remove-Item $manifest.FullName -Force
}

Write-Host "Stopped $($manifests.Count) kept-open session(s)."
