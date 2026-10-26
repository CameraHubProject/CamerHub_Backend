param()
$ErrorActionPreference = 'Stop'
$platformRoot = Split-Path -Parent $PSScriptRoot
$keyDirectory = Join-Path $platformRoot '.local/render/keys'
& (Join-Path $PSScriptRoot 'Prepare-InternalKey.ps1')
$javaCommand = Get-Command java -ErrorAction SilentlyContinue
if ($javaCommand) { $javaExecutable = $javaCommand.Source }
elseif ($env:JAVA_HOME) { $javaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe' }
else { throw 'Install Java 21 or set JAVA_HOME first.' }
& $javaExecutable (Join-Path $PSScriptRoot 'Generate-JwtKeys.java') $keyDirectory
if ($LASTEXITCODE -ne 0) { throw 'JWT key generation failed.' }
$lines = foreach ($entry in @(@('JWT_PUBLIC_KEY_B64', 'public.pem'), @('JWT_PRIVATE_KEY_B64', 'private.pem'))) {
    $bytes = [IO.File]::ReadAllBytes((Join-Path $keyDirectory $entry[1]))
    $encoded = [Convert]::ToBase64String($bytes)
    [IO.File]::WriteAllText((Join-Path $keyDirectory ($entry[0] + '.txt')), $encoded)
    $entry[0] + '=' + $encoded
}
[IO.File]::WriteAllLines((Join-Path $keyDirectory 'identity-jwt.env'), $lines)
Write-Host 'Render JWT values are ready in .local/render/keys (git-ignored).'
Write-Host 'Enter these values only in the Render identity service environment settings.'
