# Compatibility alias. Only use a disposable test database.
param([string]$BaseUrl='http://127.0.0.1:18080')
& (Join-Path $PSScriptRoot 'Test-UnifiedBackend.ps1') -BaseUrl $BaseUrl
