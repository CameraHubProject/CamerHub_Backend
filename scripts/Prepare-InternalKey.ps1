param()
$ErrorActionPreference = 'Stop'
$internalDirectory = Join-Path (Split-Path -Parent $PSScriptRoot) '.local/render/keys'
[IO.Directory]::CreateDirectory($internalDirectory) | Out-Null
$internalKeyPath = Join-Path $internalDirectory 'SERVICE_INTERNAL_KEY.txt'
if (Test-Path -LiteralPath $internalKeyPath) {
    $internalValue = [IO.File]::ReadAllText($internalKeyPath).Trim()
    if ($internalValue -cnotmatch '^[0-9a-f]{64}$') {
        throw 'The existing internal key is invalid; it has not been replaced.'
    }
}
else {
    $internalBytes = New-Object byte[] 32
    $internalRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $internalRandom.GetBytes($internalBytes) }
    finally { $internalRandom.Dispose() }
    $internalValue = [BitConverter]::ToString($internalBytes).Replace('-', '').ToLowerInvariant()
    [IO.File]::WriteAllText($internalKeyPath, $internalValue)
}
[IO.File]::WriteAllText((Join-Path $internalDirectory 'shared-internal.env'), ('SERVICE_INTERNAL_KEY=' + $internalValue + [Environment]::NewLine))
Write-Host 'Shared backend key ready in .local/render/keys/SERVICE_INTERNAL_KEY.txt (git-ignored).'
Write-Host 'Use the same value for SERVICE_INTERNAL_KEY in all backend services. Keep it out of the frontend.'
