# SecureGate Integration Test Suite (PowerShell)
# Run: .\security-tests\integration-tests.ps1

param(
    [string]$IamUrl = "http://localhost:8080/iam",
    [string]$ApiUrl = "http://localhost:8081/api",
    [string]$FrontendUrl = "http://localhost:5173"
)

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  SecureGate Integration Test Suite" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "IAM URL: $IamUrl"
Write-Host "API URL: $ApiUrl"
Write-Host "Frontend URL: $FrontendUrl"
Write-Host ""

$Passed = 0
$Failed = 0

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = "",
        [string]$ExpectedContent = "",
        [int]$ExpectedStatus = 200
    )
    
    Write-Host -NoNewline "Testing: $Name... "
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            ErrorAction = "Stop"
        }
        
        if ($Headers.Count -gt 0) {
            $params.Headers = $Headers
        }
        
        if ($Body -ne "") {
            $params.Body = $Body
            if (-not $Headers.ContainsKey("Content-Type")) {
                $params.ContentType = "application/x-www-form-urlencoded"
            }
        }
        
        $response = Invoke-WebRequest @params
        
        if ($ExpectedContent -ne "" -and $response.Content -notmatch $ExpectedContent) {
            Write-Host "FAILED" -ForegroundColor Red
            Write-Host "  Expected content: $ExpectedContent" -ForegroundColor Yellow
            $script:Failed++
            return $false
        }
        
        Write-Host "PASSED" -ForegroundColor Green
        $script:Passed++
        return $true
    }
    catch {
        # For tests expecting errors
        if ($_.Exception.Response.StatusCode.value__ -eq 401 -or 
            $_.Exception.Response.StatusCode.value__ -eq 400) {
            if ($ExpectedStatus -eq 401 -or $ExpectedStatus -eq 400) {
                Write-Host "PASSED" -ForegroundColor Green
                $script:Passed++
                return $true
            }
        }
        Write-Host "FAILED" -ForegroundColor Red
        Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Yellow
        $script:Failed++
        return $false
    }
}

Write-Host ""
Write-Host "--- Backend Health Checks ---" -ForegroundColor Yellow

# Test 1: API Gateway health
Test-Endpoint -Name "API Gateway health" `
    -Url "$ApiUrl/test" `
    -ExpectedContent "ok"

# Test 2: IAM JWKS endpoint
Test-Endpoint -Name "IAM JWKS endpoint" `
    -Url "$IamUrl/jwk" `
    -ExpectedContent "keys"

# Test 3: JWKS contains Ed25519 keys
Test-Endpoint -Name "JWKS contains Ed25519 keys" `
    -Url "$IamUrl/jwk" `
    -ExpectedContent "Ed25519"

Write-Host ""
Write-Host "--- OAuth 2.1 Endpoints ---" -ForegroundColor Yellow

# Test 4: Authorization endpoint
$authUrl = "$IamUrl/authorize?client_id=demo-client&response_type=code&redirect_uri=http://localhost:5173/callback&code_challenge=test&code_challenge_method=S256"
Test-Endpoint -Name "Authorization endpoint" `
    -Url $authUrl `
    -ExpectedContent "Login|login|form"

Write-Host ""
Write-Host "--- API Gateway Security ---" -ForegroundColor Yellow

# Test 5: Protected endpoint requires token
try {
    $response = Invoke-WebRequest -Uri "$ApiUrl/users/me" -ErrorAction Stop
    Write-Host "Testing: Protected endpoint requires auth... FAILED" -ForegroundColor Red
    $Failed++
}
catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "Testing: Protected endpoint requires auth... PASSED" -ForegroundColor Green
        $Passed++
    } else {
        Write-Host "Testing: Protected endpoint requires auth... FAILED" -ForegroundColor Red
        $Failed++
    }
}

# Test 6: Invalid token rejected
try {
    $response = Invoke-WebRequest -Uri "$ApiUrl/users/me" `
        -Headers @{"Authorization" = "Bearer invalid-token"} `
        -ErrorAction Stop
    Write-Host "Testing: Invalid token rejected... FAILED" -ForegroundColor Red
    $Failed++
}
catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "Testing: Invalid token rejected... PASSED" -ForegroundColor Green
        $Passed++
    } else {
        Write-Host "Testing: Invalid token rejected... FAILED" -ForegroundColor Red
        $Failed++
    }
}

# Test 7: Public health endpoint
Test-Endpoint -Name "Public health endpoint" `
    -Url "$ApiUrl/users/health" `
    -ExpectedContent "healthy"

Write-Host ""
Write-Host "--- Frontend Availability ---" -ForegroundColor Yellow

# Test 8: Frontend accessible
Test-Endpoint -Name "Frontend accessible" `
    -Url $FrontendUrl `
    -ExpectedContent "html"

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Test Results" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Passed: $Passed" -ForegroundColor Green
Write-Host "Failed: $Failed" -ForegroundColor $(if($Failed -eq 0){"Green"}else{"Red"})
Write-Host ""

if ($Failed -eq 0) {
    Write-Host "All tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "Some tests failed!" -ForegroundColor Red
    exit 1
}
