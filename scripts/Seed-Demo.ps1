param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Import-LocalEnvironment.ps1')

if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $demoJava = Get-ChildItem -LiteralPath (Join-Path $env:ProgramFiles 'Java') -Directory -Filter 'jdk-21*' |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $demoJava) { throw 'Java 21 is required.' }
    $env:JAVA_HOME = $demoJava.FullName
}
foreach ($demoKey in @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($demoKey)) { throw "Missing $demoKey in the backend .env." }
}
if ($env:DB_URL -notmatch '^jdbc:postgresql://[^/]+/camerahub_mvp_dev(?:\?|$)' -or
    $env:DB_URL -match '://[^/]*@|[?&](password|user)=|-pooler\.') {
    throw 'Demo seeding is restricted to the dedicated camerahub_mvp_dev database and a direct endpoint.'
}

# Reuse exactly the JDBC/crypto/logging versions packaged in this backend's last successful build.
$demoBootJar = Join-Path $taskRoot 'target\camerahub-backend-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $demoBootJar)) { throw 'Run scripts/Invoke-Backend.ps1 Verify first.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$demoArchive = [System.IO.Compression.ZipFile]::OpenRead($demoBootJar)
try {
    $demoLibraryNames = @($demoArchive.Entries | Where-Object {
        $_.FullName -match '^BOOT-INF/lib/(postgresql|spring-security-crypto|spring-jcl)-[^/]+\.jar$'
    } | ForEach-Object { $_.Name })
} finally { $demoArchive.Dispose() }
if ($demoLibraryNames.Count -ne 3) { throw 'The built backend must contain JDBC, security crypto and spring-jcl.' }
$demoLibraries = foreach ($demoLibraryName in $demoLibraryNames) {
    $demoMatches = @(Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.m2\repository\org') -Filter $demoLibraryName -File -Recurse)
    if ($demoMatches.Count -ne 1) { throw "Cannot uniquely resolve cached dependency $demoLibraryName." }
    $demoMatches[0].FullName
}

Push-Location $taskRoot
try {
    & (Join-Path $env:JAVA_HOME 'bin\java.exe') '-Djava.awt.headless=true' '--class-path' ($demoLibraries -join [IO.Path]::PathSeparator) (Join-Path $PSScriptRoot 'Seed-Demo.java')
    if ($LASTEXITCODE -ne 0) { throw 'Demo seed failed. No partial database seed was committed.' }
} finally { Pop-Location }
