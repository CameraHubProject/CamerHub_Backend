param([string]$BaseUrl = 'http://127.0.0.1:8080', [switch]$ExerciseRestart)
$ErrorActionPreference = 'Stop'
$platformRoot = Split-Path -Parent $PSScriptRoot
$platformCompose = @('compose', '--project-name', 'camerahub-platform', '--env-file', (Join-Path $platformRoot '.local/microservices.env'), '-f', (Join-Path $platformRoot 'microservices/compose.yaml'))
$platformResults = [Collections.Generic.List[string]]::new()
function Assert-Platform($Condition, [string]$Message) { if (-not $Condition) { throw $Message }; $platformResults.Add($Message); Write-Output "PASS $Message" }
function Call-Platform($Session, [string]$Path, $Body = $null, [string]$Method = 'GET') {
    $platformHeaders = @{}
    if ($Method -ne 'GET') { $platformCsrf = (Invoke-RestMethod "$BaseUrl/api/auth/csrf" -WebSession $Session).data; $platformHeaders[$platformCsrf.headerName] = $platformCsrf.token }
    $platformArguments = @{Uri = "$BaseUrl$Path"; Method = $Method; WebSession = $Session; Headers = $platformHeaders; SkipHttpErrorCheck = $true; TimeoutSec = 40 }
    if ($null -ne $Body) { $platformArguments.ContentType = 'application/json'; $platformArguments.Body = ConvertTo-Json -InputObject $Body -Depth 15 -Compress }
    $platformResponse = Invoke-WebRequest @platformArguments
    if ([int]$platformResponse.StatusCode -ge 400) { throw "$Method $Path failed with $($platformResponse.StatusCode): $($platformResponse.Content)" }
    if ($platformResponse.Content) { $platformJson = $platformResponse.Content | ConvertFrom-Json -Depth 30; if ($platformJson.PSObject.Properties.Name -contains 'data') { return $platformJson.data }; return $platformJson }
}
function New-PlatformMember([string]$Name) {
    $platformSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new(); $platformNonce = [Guid]::NewGuid().ToString('N'); $platformPassword = [Guid]::NewGuid().ToString('N') + 'aA12!'; $platformEmail = "micro-$platformNonce@test.invalid"; $platformPhone = '09' + (Get-Random -Minimum 10000000 -Maximum 99999999)
    $platformAccount = Call-Platform $platformSession '/api/auth/register' @{email = $platformEmail; password = $platformPassword; fullName = $Name; phone = $platformPhone } 'POST'
    $platformCsrf = (Invoke-RestMethod "$BaseUrl/api/auth/csrf" -WebSession $platformSession).data
    $platformLogin = Invoke-WebRequest "$BaseUrl/api/auth/login" -Method Post -WebSession $platformSession -Headers @{'X-CSRF-TOKEN' = $platformCsrf.token } -Body @{email = $platformEmail; password = $platformPassword } -SkipHttpErrorCheck
    if ($platformLogin.StatusCode -ne 200) { throw "Login failed: $($platformLogin.StatusCode)" }
    return @{Session = $platformSession; Id = $platformAccount.id; Phone = $platformPhone }
}
function Wait-Listing($Session, [string]$Id, [string[]]$Expected) {
    for ($platformAttempt = 0; $platformAttempt -lt 45; $platformAttempt++) { $platformItem = @(Call-Platform $Session '/api/marketplace/listings') | Where-Object id -EQ $Id; if ($platformItem.listingStatus -in $Expected) { return $platformItem }; Start-Sleep -Milliseconds 1000 }
    throw "Listing did not reach $($Expected -join ','): $($platformItem.listingStatus)"
}
foreach ($platformPort in 8081..8085) { $platformHealth = Invoke-RestMethod "http://127.0.0.1:$platformPort/api/health"; Assert-Platform ($platformHealth.status -eq 'UP') "Health $($platformHealth.service)" }
$platformHealth=Invoke-RestMethod "$BaseUrl/api/health";Assert-Platform ($platformHealth.status -eq 'UP') 'Health Gateway'
$platformSeller = New-PlatformMember 'Microservices smoke seller'; $platformBuyer = New-PlatformMember 'Microservices smoke buyer'; $platformOther = New-PlatformMember 'Microservices smoke other'
Assert-Platform ((Call-Platform $platformSeller.Session '/api/auth/me').id -eq $platformSeller.Id) 'Identity login through Gateway'
$platformUpdated = Call-Platform $platformSeller.Session '/api/auth/me' @{fullName = 'Microservices updated seller'; phone = $platformSeller.Phone } 'POST'
Assert-Platform ($platformUpdated.fullName -eq 'Microservices updated seller') 'Update profile and phone'
$platformNoCsrf = Invoke-WebRequest "$BaseUrl/api/marketplace/listings" -Method Post -WebSession $platformSeller.Session -Headers @{'X-CSRF-TOKEN' = 'invalid-token' } -ContentType application/json -Body '{}' -SkipHttpErrorCheck
Assert-Platform ($platformNoCsrf.StatusCode -eq 403) 'Downstream rejects mutation with invalid CSRF'
$platformInternal = Invoke-WebRequest "$BaseUrl/api/../internal/session" -SkipHttpErrorCheck
Assert-Platform ($platformInternal.StatusCode -in @(403, 404)) 'Gateway blocks internal routes'
$platformBlocked = Invoke-WebRequest "$BaseUrl/api/admin/listings" -Headers @{'X-CameraHub-Key' = 'spoofed' } -SkipHttpErrorCheck
Assert-Platform ($platformBlocked.StatusCode -eq 401) 'Spoofed internal key cannot grant admin access'
$platformBrands = Call-Platform $platformSeller.Session '/api/public/brands'; $platformBrand = @($platformBrands)[0].id
$platformFixture = @{brandId = $platformBrand; deviceType = 'BODY'; title = 'Máy kiểm thử microservice ' + [Guid]::NewGuid().ToString('N'); priceVnd = '10000000'; shutterCount = 100; description = 'Thiết bị kiểm thử luồng đặt hàng qua các dịch vụ độc lập. Ảnh chụp riêng cho phiên kiểm thử này.'; conditionNotes = 'Ngoại hình tốt, các chức năng hoạt động.'; accessories = 'Pin'; serialNumber = [Guid]::NewGuid().ToString('N'); purposes = @('BEGINNER') }
$platformDraft = Call-Platform $platformSeller.Session '/api/marketplace/listings' $platformFixture 'POST'
Add-Type -AssemblyName System.Drawing
$platformPhoto = Join-Path $platformRoot '.local/microservice-smoke.png'; $platformBitmap = [Drawing.Bitmap]::new(360, 260)
try {
    $platformGraphics = [Drawing.Graphics]::FromImage($platformBitmap)
    try {
        # Each run needs a different perceptual image, while both sellers in this run reuse the same bytes.
        $platformRandom = [Random]::new()
        for ($platformY = 0; $platformY -lt 8; $platformY++) {
            for ($platformX = 0; $platformX -lt 8; $platformX++) {
                $platformColor = [Drawing.Color]::FromArgb($platformRandom.Next(256), $platformRandom.Next(256), $platformRandom.Next(256))
                $platformBrush = [Drawing.SolidBrush]::new($platformColor)
                try { $platformGraphics.FillRectangle($platformBrush, $platformX * 40, $platformY * 30, 40, 30) }
                finally { $platformBrush.Dispose() }
            }
        }
    }
    finally { $platformGraphics.Dispose() }
    $platformBitmap.Save($platformPhoto, [Drawing.Imaging.ImageFormat]::Png)
}
finally { $platformBitmap.Dispose() }
function Upload-Platform($Session, $Id) { $platformCsrf = (Invoke-RestMethod "$BaseUrl/api/auth/csrf" -WebSession $Session).data; Invoke-RestMethod "$BaseUrl/api/marketplace/listings/$Id/media" -Method Post -WebSession $Session -Headers @{'X-CSRF-TOKEN' = $platformCsrf.token } -Form @{file = Get-Item -LiteralPath $platformPhoto } | Out-Null }
Upload-Platform $platformSeller.Session $platformDraft.id
if ($ExerciseRestart) { & docker @platformCompose stop moderation | Out-Null; if ($LASTEXITCODE -ne 0) { throw 'Could not stop Moderation for outage test' } }
Call-Platform $platformSeller.Session "/api/marketplace/listings/$($platformDraft.id)/submit" @{} 'POST' | Out-Null
if ($ExerciseRestart) { Start-Sleep -Milliseconds 2000; $platformRetained = @(Call-Platform $platformSeller.Session '/api/marketplace/listings') | Where-Object id -EQ $platformDraft.id; Assert-Platform ($platformRetained.listingStatus -eq 'AI_REVIEW') 'Submission retained while Moderation is offline'; & docker @platformCompose start moderation | Out-Null; if ($LASTEXITCODE -ne 0) { throw 'Could not restart Moderation' } }
$platformWaiting = Wait-Listing $platformSeller.Session $platformDraft.id @('PENDING_REVIEW')
Assert-Platform ($platformWaiting.moderationSource -eq 'AI') 'Unavailable AI routes only uncertain listing to Admin'
# Promote only this newly created smoke account in the new local Identity database.
$platformCompose = @('compose', '--project-name', 'camerahub-platform', '--env-file', (Join-Path $platformRoot '.local/microservices.env'), '-f', (Join-Path $platformRoot 'microservices/compose.yaml'))
& docker @platformCompose exec -T identity-db psql -U camerahub_identity -d camerahub_identity -c "INSERT INTO camerahub.user_roles(user_id,role_code) VALUES ('$($platformOther.Id)','ADMIN');" | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Smoke admin setup failed' }
$platformQueue = @(Call-Platform $platformOther.Session '/api/admin/listings'); $platformReview = $platformQueue | Where-Object id -EQ $platformDraft.id
Assert-Platform ($null -ne $platformReview) 'Admin queue contains uncertain listing'
Call-Platform $platformOther.Session "/api/admin/listings/$($platformDraft.id)/review" @{approve = $true; note = 'Duyệt dữ liệu kiểm thử'; revision = $platformReview.moderationRevision } 'POST' | Out-Null
Wait-Listing $platformSeller.Session $platformDraft.id @('PUBLISHED') | Out-Null
Assert-Platform ($true) 'Admin decision delivered to Marketplace'
$platformFixture.title = 'Tin khác ' + [Guid]::NewGuid().ToString('N'); $platformFixture.description = 'Nội dung khác để chỉ đối chiếu ảnh trùng giữa hai người bán.'; $platformFixture.serialNumber = [Guid]::NewGuid().ToString('N')
$platformDuplicate = Call-Platform $platformOther.Session '/api/marketplace/listings' $platformFixture 'POST'; Upload-Platform $platformOther.Session $platformDuplicate.id
Call-Platform $platformOther.Session "/api/marketplace/listings/$($platformDuplicate.id)/submit" @{} 'POST' | Out-Null
$platformRejected = Wait-Listing $platformOther.Session $platformDuplicate.id @('REJECTED')
Assert-Platform ($platformRejected.moderationNote -match 'Ảnh') 'Duplicate photo across sellers is rejected'
$platformPrivate = Invoke-WebRequest "$BaseUrl/api/marketplace/listings/$($platformDuplicate.id)/draft" -WebSession $platformBuyer.Session -SkipHttpErrorCheck
Assert-Platform ($platformPrivate.StatusCode -in @(404, 409)) 'Other buyer cannot read private draft'
$platformCheckout = @{requestId = [Guid]::NewGuid().ToString(); productIds = @($platformDraft.id); delivery = 'DELIVERY'; address = @{recipientName = 'Smoke buyer'; phone = $platformBuyer.Phone; province = 'Hà Nội'; streetAddress = 'Địa chỉ kiểm thử'; isDefault = $false } }
$platformOrders = @(Call-Platform $platformBuyer.Session '/api/marketplace/checkout' $platformCheckout 'POST'); $platformAgain = @(Call-Platform $platformBuyer.Session '/api/marketplace/checkout' $platformCheckout 'POST')
Assert-Platform ($platformOrders.Count -eq 1 -and $platformOrders[0].id -eq $platformAgain[0].id) 'Checkout retry is idempotent across service boundary'
Assert-Platform ($platformOrders[0].priceVnd -eq '10000000') 'Order uses server-owned reserved price'
$platformFailed = $false; try { $platformCheckout.requestId = [Guid]::NewGuid().ToString(); Call-Platform $platformOther.Session '/api/marketplace/checkout' $platformCheckout 'POST' | Out-Null }catch { $platformFailed = $_.Exception.Message -match '"errorCode"\s*:\s*"MARKETPLACE_(OUT_OF_STOCK|PRODUCT_RESERVED)"' }
Assert-Platform $platformFailed 'A second buyer cannot buy reserved inventory'
Call-Platform $platformBuyer.Session "/api/marketplace/orders/$($platformOrders[0].id)/cancel" @{} 'POST' | Out-Null
Wait-Listing $platformSeller.Session $platformDraft.id @('PUBLISHED') | Out-Null
Assert-Platform ($true) 'Cancellation releases reservation through Commerce confirmation'
Call-Platform $platformBuyer.Session '/api/marketplace/addresses' @{recipientName = 'Smoke buyer'; phone = $platformBuyer.Phone; province = 'Hà Nội'; streetAddress = 'Địa chỉ kiểm thử'; isDefault = $true } 'POST' | Out-Null
Assert-Platform (@(Call-Platform $platformBuyer.Session '/api/marketplace/addresses').Count -eq 1) 'Addresses route to Identity database'
Assert-Platform ((Call-Platform $platformSeller.Session '/api/ai/conversation').messages.Count -eq 0) 'Advisor conversation has independent storage'
$platformSummary = @{passed = $platformResults.Count; checks = $platformResults; listingId = $platformDraft.id; sellerId = $platformSeller.Id; buyerId = $platformBuyer.Id; otherId = $platformOther.Id; orderId = $platformOrders[0].id; duplicateId = $platformDuplicate.id }
$platformSummary | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $platformRoot '.local/microservices-smoke-result.json')
Write-Output "$($platformResults.Count) checks passed"

