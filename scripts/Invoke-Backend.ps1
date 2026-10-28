param([ValidateSet('Build','Start','Stop','Status','Logs')][string]$Task='Start', [string]$EnvironmentFile='')
$ErrorActionPreference='Stop'
$backendRoot=Split-Path -Parent $PSScriptRoot
Push-Location $backendRoot
try {
    . (Join-Path $PSScriptRoot 'Import-LocalEnvironment.ps1')
    if ($EnvironmentFile) {
        foreach ($backendLine in [IO.File]::ReadAllLines($EnvironmentFile)) {
            if ($backendLine -match '^([A-Z][A-Z0-9_]*)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2].Trim('"'), 'Process') }
        }
    }
    $backendPort=if($env:PORT){$env:PORT}else{'8080'}
    $backendRuntime=Join-Path $backendRoot '.local/backend'
    [IO.Directory]::CreateDirectory($backendRuntime) | Out-Null
    $backendPidFile=Join-Path $backendRuntime ("backend-"+$backendPort+'.pid')
    $backendLog=Join-Path $backendRuntime ("backend-"+$backendPort+'.log')
    $backendErr=Join-Path $backendRuntime ("backend-"+$backendPort+'.err.log')
    $backendJar=Join-Path $backendRoot 'target/camerahub-backend-0.1.0-SNAPSHOT.jar'
    $backendProcess=$null
    if(Test-Path -LiteralPath $backendPidFile) {
        $backendPid=[int][IO.File]::ReadAllText($backendPidFile)
        $backendProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$backendPid" -ErrorAction SilentlyContinue
        if($backendProcess -and ($backendProcess.Name -ne 'java.exe' -or $backendProcess.CommandLine -notlike "*$backendJar*")){throw 'PID belongs to another application; refusing to control it.'}
    }
    if($Task -eq 'Stop'){if($backendProcess){Stop-Process -Id $backendProcess.ProcessId;Write-Host 'Backend stopped.'}else{Write-Host 'Backend is not running.'};return}
    if($Task -eq 'Logs'){if(Test-Path -LiteralPath $backendLog){Get-Content -LiteralPath $backendLog -Tail 80};if(Test-Path -LiteralPath $backendErr){Get-Content -LiteralPath $backendErr -Tail 20};return}
    if($Task -eq 'Status'){if($backendProcess){Invoke-RestMethod "http://127.0.0.1:$backendPort/api/health" -TimeoutSec 20 | Format-List}else{Write-Host 'Backend is not running.'};return}
    if(-not $env:JAVA_HOME -and (Test-Path -LiteralPath 'C:\Program Files\Java\jdk-21.0.11')){$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'}
    $backendJava=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{(Get-Command java -ErrorAction Stop).Source}
    if($Task -eq 'Build' -or -not(Test-Path -LiteralPath $backendJar)) {
        if($backendProcess){throw 'Stop the backend before building: backend.cmd Stop'}
        & (Join-Path $backendRoot 'mvnw.cmd') --batch-mode --no-transfer-progress verify
        if($LASTEXITCODE -ne 0){throw 'Maven verification failed.'}
        if($Task -eq 'Build'){return}
    }
    if($backendProcess){Write-Host "Backend already running on port $backendPort.";return}
    foreach($backendRequired in @('DB_URL','DB_USERNAME','DB_PASSWORD')){if(-not[Environment]::GetEnvironmentVariable($backendRequired,'Process')){throw "Missing $backendRequired. Configure .env first."}}
    & (Join-Path $PSScriptRoot 'Prepare-RenderSecrets.ps1')
    $backendKeys=Join-Path $backendRoot '.local/render/keys'
    if(-not $env:SERVICE_INTERNAL_KEY){$env:SERVICE_INTERNAL_KEY=[IO.File]::ReadAllText((Join-Path $backendKeys 'SERVICE_INTERNAL_KEY.txt')).Trim()}
    if(-not $env:JWT_PUBLIC_KEY_PATH){$env:JWT_PUBLIC_KEY_PATH=Join-Path $backendKeys 'public.pem'}
    if(-not $env:JWT_PRIVATE_KEY_PATH){$env:JWT_PRIVATE_KEY_PATH=Join-Path $backendKeys 'private.pem'}
    $backendStarted=Start-Process -FilePath $backendJava -WindowStyle Hidden -ArgumentList @('-Xmx256m','-XX:+ExitOnOutOfMemoryError','-jar',('"'+$backendJar+'"')) -WorkingDirectory $backendRoot -RedirectStandardOutput $backendLog -RedirectStandardError $backendErr -PassThru
    [IO.File]::WriteAllText($backendPidFile,[string]$backendStarted.Id)
    Write-Host "Backend starting on port $backendPort. Use backend.cmd Status or backend.cmd Logs."
} finally {Pop-Location}
