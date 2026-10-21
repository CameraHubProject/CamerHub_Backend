param([ValidateSet('Verify', 'Run', 'Integration')][string]$Task = 'Verify')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Import-LocalEnvironment.ps1')
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $taskJavaRoot = Join-Path $env:ProgramFiles 'Java'
    $taskJava = Get-ChildItem -LiteralPath $taskJavaRoot -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $taskJava) { throw 'Set JAVA_HOME to your Java 21 JDK directory.' }
    $env:JAVA_HOME = $taskJava.FullName
}
if ($Task -ne 'Verify') {
    foreach ($taskKey in @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD')) {
        if (-not [Environment]::GetEnvironmentVariable($taskKey)) {
            throw "Set $taskKey in the local .env file."
        }
    }
    if ($env:DB_URL -notmatch '^jdbc:postgresql://' -or
        $env:DB_URL -match '://[^/]*@|[?&](password|user)=') {
        throw 'DB_URL must be a PostgreSQL JDBC URL without embedded credentials.'
    }
    if ($env:DB_URL -match '-pooler\.') {
        throw 'Use a direct Neon endpoint for migration session locks; remove the -pooler suffix.'
    }
}
Push-Location $taskRoot
try {
    switch ($Task) {
        'Verify' { & .\mvnw.cmd --batch-mode --no-transfer-progress verify }
        'Run' { & .\mvnw.cmd --batch-mode --no-transfer-progress spring-boot:run }
        'Integration' { & .\mvnw.cmd --batch-mode --no-transfer-progress -Pneon-it verify }
    }
    if ($LASTEXITCODE -ne 0) { throw "Backend $Task failed (exit $LASTEXITCODE)." }
} finally { Pop-Location }
