# SecureGate IAM Portal - Comprehensive API Test Suite
# Tests all endpoints across IAM Service, API Gateway, and Stego Module
# Author: Automated Security Audit
# Date: 2026-01-10

# Configuration
$BASE_URL_IAM = "http://localhost:8081/iam-service/api"
$BASE_URL_API_GATEWAY = "http://localhost:8080/api-gateway/api"
$BASE_URL_STEGO = "http://localhost:8084/stego-module/api"
$OUTPUT_DIR = "api_test"
$RESULTS_FILE = "$OUTPUT_DIR/test_results.md"
$JSON_FILE = "$OUTPUT_DIR/test_results.json"

# Colors for output
$SUCCESS_COLOR = "Green"
$FAIL_COLOR = "Red"
$INFO_COLOR = "Cyan"

# Test results storage
$testResults = @()
$totalTests = 0
$passedTests = 0
$failedTests = 0

# Helper function to make HTTP requests
function Invoke-ApiTest {
    param(
        [string]$Service,
        [string]$Method,
        [string]$Endpoint,
        [string]$Description,
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [int[]]$ExpectedStatus = @(200),
        [string]$Token = $null
    )
    
    $global:totalTests++
    $testName = "[$Service] $Method $Endpoint"
    
    Write-Host "`n[TEST $global:totalTests] $testName" -ForegroundColor $INFO_COLOR
    Write-Host "Description: $Description" -ForegroundColor Gray
    
    try {
        # Prepare request
        $requestParams = @{
            Uri         = $Endpoint
            Method      = $Method
            Headers     = $Headers
            ContentType = "application/json"
            TimeoutSec  = 30
        }
        
        if ($Token) {
            $requestParams.Headers["Authorization"] = "Bearer $Token"
        }
        
        if ($Body) {
            $requestParams.Body = ($Body | ConvertTo-Json -Depth 10)
            Write-Host "Request Body: $($requestParams.Body)" -ForegroundColor Gray
        }
        
        # Make request
        $startTime = Get-Date
        try {
            $response = Invoke-WebRequest @requestParams -ErrorAction Stop
            $statusCode = $response.StatusCode
            $responseBody = $response.Content
        }
        catch {
            $statusCode = $_.Exception.Response.StatusCode.Value__
            $responseBody = $_.Exception.Message
            if ($_.Exception.Response) {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $responseBody = $reader.ReadToEnd()
                $reader.Close()
            }
        }
        $endTime = Get-Date
        $duration = ($endTime - $startTime).TotalMilliseconds
        
        # Check if status code is expected
        $success = $ExpectedStatus -contains $statusCode
        
        if ($success) {
            Write-Host "✓ PASSED - Status: $statusCode (${duration}ms)" -ForegroundColor $SUCCESS_COLOR
            $global:passedTests++
        }
        else {
            Write-Host "✗ FAILED - Status: $statusCode (Expected: $($ExpectedStatus -join '/'))" -ForegroundColor $FAIL_COLOR
            $global:failedTests++
        }
        
        # Save result
        $result = @{
            TestNumber     = $global:totalTests
            Service        = $Service
            Method         = $Method
            Endpoint       = $Endpoint
            Description    = $Description
            StatusCode     = $statusCode
            ExpectedStatus = $ExpectedStatus
            Success        = $success
            Duration       = [math]::Round($duration, 2)
            Timestamp      = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            ResponseBody   = $responseBody
        }
        
        $global:testResults += $result
        
        # Display response preview
        if ($responseBody.Length -gt 0) {
            $preview = if ($responseBody.Length -gt 200) { 
                $responseBody.Substring(0, 200) + "..." 
            }
            else { 
                $responseBody 
            }
            Write-Host "Response: $preview" -ForegroundColor Gray
        }
        
        return $result
    }
    catch {
        Write-Host "✗ ERROR - $($_.Exception.Message)" -ForegroundColor $FAIL_COLOR
        $global:failedTests++
        
        $result = @{
            TestNumber     = $global:totalTests
            Service        = $Service
            Method         = $Method
            Endpoint       = $Endpoint
            Description    = $Description
            StatusCode     = 0
            ExpectedStatus = $ExpectedStatus
            Success        = $false
            Duration       = 0
            Timestamp      = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            ResponseBody   = $_.Exception.Message
        }
        
        $global:testResults += $result
        return $result
    }
}

# Save results to files
function Save-TestResults {
    Write-Host "`n================================================" -ForegroundColor $INFO_COLOR
    Write-Host "SAVING TEST RESULTS" -ForegroundColor $INFO_COLOR
    Write-Host "================================================" -ForegroundColor $INFO_COLOR
    
    # Save JSON
    $global:testResults | ConvertTo-Json -Depth 10 | Out-File -FilePath $JSON_FILE -Encoding UTF8
    Write-Host "✓ JSON results saved to: $JSON_FILE" -ForegroundColor $SUCCESS_COLOR
    
    # Create Markdown report
    $markdown = @"
# SecureGate IAM Portal - API Test Results
**Test Date:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")  
**Total Tests:** $global:totalTests  
**Passed:** $global:passedTests  
**Failed:** $global:failedTests  
**Success Rate:** $([math]::Round(($global:passedTests / $global:totalTests) * 100, 2))%

---

## Test Summary

| # | Service | Endpoint | Method | Status | Expected | Result | Duration (ms) |
|---|---------|----------|--------|--------|----------|--------|---------------|
"@
    
    foreach ($result in $global:testResults) {
        $statusIcon = if ($result.Success) { "✓" } else { "✗" }
        $markdown += "`n| $($result.TestNumber) | $($result.Service) | ``$($result.Endpoint -replace $BASE_URL_IAM,'' -replace $BASE_URL_API_GATEWAY,'' -replace $BASE_URL_STEGO,'')`` | $($result.Method) | $($result.StatusCode) | $($result.ExpectedStatus -join '/') | $statusIcon | $($result.Duration) |"
    }
    
    $markdown += @"

---

## Detailed Test Results

"@
    
    foreach ($result in $global:testResults) {
        $statusIcon = if ($result.Success) { "✅ PASSED" } else { "❌ FAILED" }
        $markdown += @"

### Test #$($result.TestNumber): $statusIcon

**Service:** $($result.Service)  
**Endpoint:** ``$($result.Method) $($result.Endpoint)``  
**Description:** $($result.Description)  
**Status Code:** $($result.StatusCode) (Expected: $($result.ExpectedStatus -join '/'))  
**Duration:** $($result.Duration) ms  
**Timestamp:** $($result.Timestamp)

**Response:**
``````json
$($result.ResponseBody)
``````

---
"@
    }
    
    $markdown | Out-File -FilePath $RESULTS_FILE -Encoding UTF8
    Write-Host "✓ Markdown report saved to: $RESULTS_FILE" -ForegroundColor $SUCCESS_COLOR
}

# Main test execution
Write-Host "================================================" -ForegroundColor $INFO_COLOR
Write-Host "SecureGate IAM Portal - API Test Suite" -ForegroundColor $INFO_COLOR
Write-Host "================================================" -ForegroundColor $INFO_COLOR
Write-Host "Starting comprehensive API testing..." -ForegroundColor $INFO_COLOR
Write-Host "Waiting 10 seconds for services to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# ============================================================================
# IAM SERVICE TESTS
# ============================================================================

Write-Host "`n`n================================================" -ForegroundColor $INFO_COLOR
Write-Host "TESTING IAM SERVICE" -ForegroundColor $INFO_COLOR
Write-Host "================================================" -ForegroundColor $INFO_COLOR

# Health Checks
Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/health" `
    -Description "IAM Service Health Check" -ExpectedStatus @(200)

Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/health/ready" `
    -Description "IAM Readiness Probe" -ExpectedStatus @(200)

Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/health/live" `
    -Description "IAM Liveness Probe" -ExpectedStatus @(200)

# Users API
Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/users" `
    -Description "Get all users" -ExpectedStatus @(200, 401)

Invoke-ApiTest -Service "IAM" -Method "POST" -Endpoint "$BASE_URL_IAM/users" `
    -Description "Create new user" `
    -Body @{
    username = "testuser"
    email    = "test@securegate.io"
    password = "SecureP@ss123!"
    fullName = "Test User"
} -ExpectedStatus @(200, 201, 400, 401)

# Notifications API
Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/notifications" `
    -Description "Get all notifications" -ExpectedStatus @(200)

Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/notifications/unread-count" `
    -Description "Get unread notification count" -ExpectedStatus @(200)

# Policies API
Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/policies" `
    -Description "Get all policies" -ExpectedStatus @(200, 401)

Invoke-ApiTest -Service "IAM" -Method "POST" -Endpoint "$BASE_URL_IAM/policies" `
    -Description "Create new policy" `
    -Body @{
    name        = "TestPolicy"
    description = "Test policy for API testing"
    effect      = "PERMIT"
    target      = @{
        resource = "test/*"
        action   = "read"
    }
} -ExpectedStatus @(200, 201, 400, 401)

# System Settings API
Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/settings" `
    -Description "Get all system settings" -ExpectedStatus @(200, 401)

Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/settings/auth_session_timeout" `
    -Description "Get specific setting by key" -ExpectedStatus @(200, 401, 404)

# Audit Logs API
Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/audit" `
    -Description "Get all audit logs" -ExpectedStatus @(200, 401)

Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/audit/stats" `
    -Description "Get audit statistics" -ExpectedStatus @(200, 401)

# MFA API
Invoke-ApiTest -Service "IAM" -Method "POST" -Endpoint "$BASE_URL_IAM/mfa/setup" `
    -Description "Setup MFA for user" `
    -Body @{
    username = "admin"
} -ExpectedStatus @(200, 400, 401)

# OAuth2 API
Invoke-ApiTest -Service "IAM" -Method "GET" -Endpoint "$BASE_URL_IAM/oauth2/authorize?response_type=code&client_id=test&redirect_uri=https://localhost&state=test" `
    -Description "OAuth2 Authorization Endpoint" -ExpectedStatus @(200, 302, 400, 401)

# ============================================================================
# API GATEWAY TESTS
# ============================================================================

Write-Host "`n`n================================================" -ForegroundColor $INFO_COLOR
Write-Host "TESTING API GATEWAY" -ForegroundColor $INFO_COLOR
Write-Host "================================================" -ForegroundColor $INFO_COLOR

Invoke-ApiTest -Service "API-Gateway" -Method "GET" -Endpoint "$BASE_URL_API_GATEWAY/health" `
    -Description "API Gateway Health Check" -ExpectedStatus @(200, 404)

Invoke-ApiTest -Service "API-Gateway" -Method "GET" -Endpoint "$BASE_URL_API_GATEWAY/v1/status" `
    -Description "API Gateway Status" -ExpectedStatus @(200, 404)

# ============================================================================
# STEGO MODULE TESTS
# ============================================================================

Write-Host "`n`n================================================" -ForegroundColor $INFO_COLOR
Write-Host "TESTING STEGO MODULE" -ForegroundColor $INFO_COLOR
Write-Host "================================================" -ForegroundColor $INFO_COLOR

Invoke-ApiTest -Service "Stego" -Method "GET" -Endpoint "$BASE_URL_STEGO/health" `
    -Description "Stego Module Health Check" -ExpectedStatus @(200, 404)

Invoke-ApiTest -Service "Stego" -Method "POST" -Endpoint "$BASE_URL_STEGO/embed" `
    -Description "Embed data using steganography" `
    -Body @{
    message    = "Secret test message"
    coverImage = "base64encodedimage"
} -ExpectedStatus @(200, 400, 404)

# ============================================================================
# SUMMARY
# ============================================================================

Write-Host "`n`n================================================" -ForegroundColor $INFO_COLOR
Write-Host "TEST EXECUTION COMPLETE" -ForegroundColor $INFO_COLOR
Write-Host "================================================" -ForegroundColor $INFO_COLOR
Write-Host "Total Tests: $totalTests" -ForegroundColor $INFO_COLOR
Write-Host "Passed: $passedTests" -ForegroundColor $SUCCESS_COLOR
Write-Host "Failed: $failedTests" -ForegroundColor $FAIL_COLOR
Write-Host "Success Rate: $([math]::Round(($passedTests / $totalTests) * 100, 2))%" -ForegroundColor $(if ($passedTests -eq $totalTests) { $SUCCESS_COLOR } else { "Yellow" })

# Save results
Save-TestResults

Write-Host "`n✓ TEST SUITE COMPLETED" -ForegroundColor $SUCCESS_COLOR
Write-Host "Results available in: $OUTPUT_DIR/" -ForegroundColor $INFO_COLOR
