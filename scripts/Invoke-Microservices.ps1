# Compatibility for the previous launcher; the active backend runs one JVM.
param([string]$Task='Start',[string]$Service='')
if($Task -notin @('Build','Start','Stop','Status','Logs')){throw 'This task belonged to the archived microservice build. See docs/DEPLOY_RENDER.md.'}
& (Join-Path $PSScriptRoot 'Invoke-Backend.ps1') -Task $Task
