param([ValidateSet('Build', 'Start', 'Stop', 'Status', 'Logs', 'Import', 'Audit', 'CleanupSmoke', 'Index')][string]$Task = 'Status', [string]$Service = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Import-LocalEnvironment.ps1')
$platformRoot = Split-Path -Parent $PSScriptRoot
$platformEnv = Join-Path $platformRoot '.local/microservices.env'
if (-not (Test-Path -LiteralPath $platformEnv)) {
    New-Item -ItemType Directory -Path (Join-Path $platformRoot '.local') -Force | Out-Null
    $platformKeys = @('SERVICE_INTERNAL_KEY', 'IDENTITY_DB_PASSWORD', 'MARKETPLACE_DB_PASSWORD', 'MODERATION_DB_PASSWORD', 'COMMERCE_DB_PASSWORD', 'ADVISOR_DB_PASSWORD')
    $platformRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $platformLines = foreach ($platformKey in $platformKeys) {
            $platformBytes = New-Object byte[] 32
            $platformRandom.GetBytes($platformBytes)
            $platformHex = [BitConverter]::ToString($platformBytes).Replace('-', '').ToLowerInvariant()
            "$platformKey=$platformHex"
        }
    }
    finally { $platformRandom.Dispose() }
    [IO.File]::WriteAllLines($platformEnv, @('GATEWAY_PORT=8086') + $platformLines)
}
foreach ($platformLine in [IO.File]::ReadAllLines($platformEnv)) { if ($platformLine -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process') } }
$platformCompose = @('compose', '--project-name', 'camerahub-platform', '--env-file', $platformEnv, '-f', (Join-Path $platformRoot 'microservices/compose.yaml'))
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.11' }
if ($Task -in @('Build', 'Start')) {
    & "$env:JAVA_HOME/bin/java.exe" (Join-Path $PSScriptRoot 'Generate-JwtKeys.java') (Join-Path $platformRoot '.local/jwt')
    if ($LASTEXITCODE -ne 0) { throw 'JWT key initialization failed' }
}
Push-Location $platformRoot
try {
    $global:LASTEXITCODE = 0
    switch ($Task) {
        'Build' { & ./mvnw.cmd -f microservices/pom.xml --batch-mode --no-transfer-progress verify; if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed' }; & docker @platformCompose build }
        'Start' { & docker @platformCompose up -d --build }
        'Stop' { & docker @platformCompose stop }
        'Status' { & docker @platformCompose ps }
        'Logs' { if ($Service) { & docker @platformCompose logs --tail 80 $Service }else { & docker @platformCompose logs --tail 15 } }
        'Index' { Invoke-RestMethod -Method Post -Uri 'http://127.0.0.1:8083/internal/index/bootstrap' -Headers @{'X-CameraHub-Key' = $env:SERVICE_INTERNAL_KEY } }
        { $_ -in @('Import', 'Audit', 'CleanupSmoke') } {
            $platformDriver = Get-ChildItem "$env:USERPROFILE/.m2/repository/org/postgresql/postgresql/*/postgresql-*.jar" |
                Where-Object { $_.Name -match '^postgresql-\d+(\.\d+)+\.jar$' } |
                Sort-Object { [version]($_.BaseName -replace '^postgresql-', '') } -Descending | Select-Object -First 1
            if (-not $platformDriver) { throw 'Build services first to obtain PostgreSQL JDBC driver' }
            $platformTool = switch ($Task) { 'Audit' { 'microservices/tools/AuditLegacy.java' } 'CleanupSmoke' { 'microservices/tools/CleanupSmoke.java' } default { 'microservices/tools/ImportLegacy.java' } }
            & "$env:JAVA_HOME/bin/java.exe" --class-path $platformDriver.FullName $platformTool
        }
    }
    if ($LASTEXITCODE -ne 0) { throw "Microservices $Task failed" }
}
finally { Pop-Location }
