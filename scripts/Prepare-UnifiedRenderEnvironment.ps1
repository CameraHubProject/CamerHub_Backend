$ErrorActionPreference='Stop'
$backendRoot=Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'Import-LocalEnvironment.ps1')
& (Join-Path $PSScriptRoot 'Prepare-RenderSecrets.ps1')
$backendKeys=Join-Path $backendRoot '.local/render/keys'
$backendValues=[ordered]@{
    DB_URL=$env:DB_URL
    DB_USERNAME=$env:DB_USERNAME
    DB_PASSWORD=$env:DB_PASSWORD
    JWT_PUBLIC_KEY_B64=[IO.File]::ReadAllText((Join-Path $backendKeys 'JWT_PUBLIC_KEY_B64.txt')).Trim()
    JWT_PRIVATE_KEY_B64=[IO.File]::ReadAllText((Join-Path $backendKeys 'JWT_PRIVATE_KEY_B64.txt')).Trim()
    SERVICE_INTERNAL_KEY=[IO.File]::ReadAllText((Join-Path $backendKeys 'SERVICE_INTERNAL_KEY.txt')).Trim()
    FRONTEND_URL='https://camerahubweb.vercel.app'
    SESSION_COOKIE_SECURE='true'
    GOOGLE_LOGIN_ENABLED='false'
    AI_ENABLED='true'
    OPENAI_API_KEY=$env:OPENAI_API_KEY
    OPENAI_MODEL=if($env:OPENAI_MODEL){$env:OPENAI_MODEL}else{'gpt-5.4-mini'}
}
foreach($backendRequired in @('DB_URL','DB_USERNAME','DB_PASSWORD')){if(-not $backendValues[$backendRequired]){throw "Missing $backendRequired in .env"}}
$backendOutput=Join-Path $backendRoot '.local/render/unified-backend.env'
$backendLines=foreach($backendEntry in $backendValues.GetEnumerator()){if($backendEntry.Value -match '[\r\n]'){throw 'Environment values must use one line.'};$backendEntry.Key+'='+$backendEntry.Value}
[IO.File]::WriteAllLines($backendOutput,$backendLines,(New-Object Text.UTF8Encoding($false)))
Write-Host 'Prepared .local/render/unified-backend.env (private, git-ignored). Import it only into the existing backend service on Render.'
