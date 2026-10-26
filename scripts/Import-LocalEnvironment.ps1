$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskEnvPath = Join-Path $taskRoot '.env'
if (Test-Path -LiteralPath $taskEnvPath) {
    foreach ($taskLine in [System.IO.File]::ReadAllLines($taskEnvPath)) {
        $taskTrimmed = $taskLine.Trim()
        if (-not $taskTrimmed -or $taskTrimmed.StartsWith('#')) { continue }
        if ($taskTrimmed -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') {
            throw 'Invalid .env assignment. Use KEY=value; shell expressions are not evaluated.'
        }
        $taskKey = $Matches[1]
        $taskValue = $Matches[2]
        if ($taskValue.Length -ge 2 -and
            (($taskValue.StartsWith('"') -and $taskValue.EndsWith('"')) -or
            ($taskValue.StartsWith("'") -and $taskValue.EndsWith("'")))) {
            $taskValue = $taskValue.Substring(1, $taskValue.Length - 2)
        }
        if ($taskValue) { [Environment]::SetEnvironmentVariable($taskKey, $taskValue, 'Process') }
    }
}
