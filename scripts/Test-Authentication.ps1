param([string]$BaseUrl='http://127.0.0.1:18080')
$ErrorActionPreference='Stop'
$authChecks=0
function Assert-Auth($Condition,[string]$Message){if(-not $Condition){throw $Message};$script:authChecks++;Write-Output "PASS $Message"}
function Csrf-Auth($Session){return (Invoke-RestMethod "$BaseUrl/api/auth/csrf" -WebSession $Session).data.token}
$authSession=[Microsoft.PowerShell.Commands.WebRequestSession]::new()
$authPhone='09'+(Get-Random -Minimum 10000000 -Maximum 99999999)
$authEmail='micro-'+[Guid]::NewGuid().ToString('N')+'@test.invalid'
$authPassword=[Guid]::NewGuid().ToString('N')+'Aa12!'
$authCsrf=Csrf-Auth $authSession
$authInvalid=Invoke-WebRequest "$BaseUrl/api/auth/register" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=$authCsrf} -ContentType application/json -Body '{"email":"bad"}' -SkipHttpErrorCheck
$authInvalidBody=$authInvalid.Content | ConvertFrom-Json
Assert-Auth ($authInvalid.StatusCode -eq 400 -and $authInvalidBody.code -eq 400 -and $authInvalidBody.errors.PSObject.Properties.Count -gt 0) 'Validation errors originate from backend with field errors'
$authUser=Invoke-RestMethod "$BaseUrl/api/auth/register" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=$authCsrf} -ContentType application/json -Body (@{email=$authEmail;password=$authPassword;fullName='Microservices JWT smoke';phone=$authPhone}|ConvertTo-Json)
Assert-Auth ($authUser.code -eq 201 -and $authUser.data.id -and -not ($authUser.data.PSObject.Properties.Name -contains 'passwordHash')) 'JPA registration maps a safe response DTO'
$authDuplicate=Invoke-WebRequest "$BaseUrl/api/auth/register" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=(Csrf-Auth $authSession)} -ContentType application/json -Body (@{email=$authEmail;password=$authPassword;fullName='Microservices duplicate';phone=$authPhone}|ConvertTo-Json) -SkipHttpErrorCheck
$authDuplicateBody=$authDuplicate.Content | ConvertFrom-Json
Assert-Auth ($authDuplicate.StatusCode -eq 409 -and $authDuplicateBody.errorCode -eq 'IDENTITY_EMAIL_ALREADY_USED' -and $authDuplicateBody.errors.email) 'Duplicate email has a domain error code and backend field message'
$authPhoneDuplicate=Invoke-WebRequest "$BaseUrl/api/auth/register" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=(Csrf-Auth $authSession)} -ContentType application/json -Body (@{email=('micro-'+[Guid]::NewGuid().ToString('N')+'@test.invalid');password=$authPassword;fullName='Microservices duplicate';phone=$authPhone}|ConvertTo-Json) -SkipHttpErrorCheck
Assert-Auth (($authPhoneDuplicate.Content|ConvertFrom-Json).errorCode -eq 'IDENTITY_PHONE_ALREADY_USED') 'Duplicate phone has a distinct domain error code'
$authConfirmation=Invoke-WebRequest "$BaseUrl/api/auth/register" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=(Csrf-Auth $authSession)} -ContentType application/json -Body (@{email=$authEmail;password=$authPassword;confirmPassword='different-password';fullName='Microservices confirmation';phone=$authPhone}|ConvertTo-Json) -SkipHttpErrorCheck
Assert-Auth (($authConfirmation.Content|ConvertFrom-Json).errorCode -eq 'IDENTITY_PASSWORD_CONFIRM_MISMATCH') 'Backend service validates password confirmation'
$authLogin=Invoke-WebRequest "$BaseUrl/api/auth/login" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=(Csrf-Auth $authSession)} -Body @{email=$authEmail;password=$authPassword}
$authToken=($authLogin.Content|ConvertFrom-Json).data.accessToken
Assert-Auth ($authLogin.StatusCode -eq 200 -and $authToken.Split('.').Count -eq 3) 'Login issues a signed JWT'
$authCookie=@($authLogin.Headers['Set-Cookie']) -join ';'
Assert-Auth ($authCookie -match 'CAMERAHUB_ACCESS=' -and $authCookie -match 'HttpOnly' -and $authCookie -match 'SameSite=Lax') 'Browser JWT uses HttpOnly SameSite cookies'
$authBearer=Invoke-RestMethod "$BaseUrl/api/auth/me" -Headers @{Authorization="Bearer $authToken"}
Assert-Auth ($authBearer.data.id -eq $authUser.data.id -and $authBearer.data.status -eq 'ACTIVE') 'Bearer authentication works without browser session'
$authOrders=Invoke-RestMethod "$BaseUrl/api/marketplace/orders" -Headers @{Authorization="Bearer $authToken"}
Assert-Auth ($authOrders.code -eq 200) 'Downstream service validates JWT and revocation'
$authParts=$authToken.Split('.');$authParts[2]= $(if($authParts[2][0] -eq 'a'){'b'}else{'a'})+$authParts[2].Substring(1)
$authTampered=Invoke-WebRequest "$BaseUrl/api/auth/me" -Headers @{Authorization=('Bearer '+($authParts -join '.'))} -SkipHttpErrorCheck
Assert-Auth ($authTampered.StatusCode -eq 401 -and ($authTampered.Content|ConvertFrom-Json).code -eq 401) 'Tampered JWT is rejected with standard error'
$authDenied=Invoke-WebRequest "$BaseUrl/api/admin/listings" -Headers @{Authorization="Bearer $authToken"} -SkipHttpErrorCheck
Assert-Auth ($authDenied.StatusCode -eq 403 -and ($authDenied.Content|ConvertFrom-Json).code -eq 403) 'Backend denies Admin routes to normal users'
$authRefreshed=Invoke-RestMethod "$BaseUrl/api/auth/refresh" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=(Csrf-Auth $authSession)}
Assert-Auth ($authRefreshed.data.accessToken -and $authRefreshed.data.accessToken -ne $authToken) 'Refresh rotates the JWT session'
$authOld=Invoke-WebRequest "$BaseUrl/api/marketplace/orders" -Headers @{Authorization="Bearer $authToken"} -SkipHttpErrorCheck
Assert-Auth ($authOld.StatusCode -eq 401) 'Refresh revokes the previous access token across services'
$authNew=$authRefreshed.data.accessToken
Invoke-RestMethod "$BaseUrl/api/auth/logout" -Method Post -WebSession $authSession -Headers @{'X-CSRF-TOKEN'=(Csrf-Auth $authSession)} | Out-Null
$authAfterLogout=Invoke-WebRequest "$BaseUrl/api/marketplace/orders" -Headers @{Authorization="Bearer $authNew"} -SkipHttpErrorCheck
Assert-Auth ($authAfterLogout.StatusCode -eq 401) 'Logout revokes the signed token at downstream services'
Write-Output "$authChecks authentication checks passed"
