@echo off
setlocal
set "BACKEND_TASK=%~1"
if not defined BACKEND_TASK set "BACKEND_TASK=Start"
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Invoke-Microservices.ps1" -Task "%BACKEND_TASK%" -Service "%~2"
exit /b %errorlevel%
