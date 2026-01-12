# SecureGate IAM Portal - API Test Suite
# Comprehensive API testing for all services

$ErrorActionPreference = "Continue"

# Configuration
$IAM_BASE = "http://127.0.0.1:8081/iam-service/api"
$GATEWAY_BASE = "http://127.0.0.1:8080/api-gateway/api"
$STEGO_BASE = "http://127.0.0.1:8084/stego-module/api"

# Initialize results
$results = @()
$passed = 0
$failed = 0

function Test-Endpoint {
    param([string]$Name, [string]$Url, [string]$Method = "GET", [object]$Body = $null)
    
    Write-Host "`n[TEST] $Name" -ForegroundColor Cyan
    Write-Host "URL: $Method $Url" -ForegroundColor Gray
    
    try {
        $params = @{
            Uri        = $Url
            Method     = $Method
            TimeoutSec = 10
        }
        
        if ($Body) {
            $params.Body = ($Body | ConvertTo-Json)
            $params.ContentType = "application/json"
        }
        
        $response = Invoke-WebRequest @params -ErrorAction Stop
        $status = $response.StatusCode
        $content = $response.Content
        
        Write-Host "PASSED - Status: $status" -ForegroundColor Green
        $script:passed++
        
        $script:results += [PSCustomObject]@{
            Test     = $Name
            Method   = $Method
            Endpoint = $Url
            Status   = $status
            Result   = "PASSED"
            Response = $content.Substring(0, [Math]::Min(200, $content.Length))
        }
        
        return $true
    }
    catch {
        $status = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.Value__ } else { "ERROR" }
        Write-Host "FAILED - Status: $status - $($_.Exception.Message)" -ForegroundColor Red
        $script:failed++
        
        $script:results += [PSCustomObject]@{
            Test     = $Name
            Method   = $Method
            Endpoint = $Url
            Status   = $status
            Result   = "FAILED"
            Response = $_.Exception.Message
        }
        
        return $false
    }
}

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "SecureGate API Test Suite" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow

# Wait for services
Write-Host "`nWaiting for services to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# IAM SERVICE TESTS
Write-Host "`n--- IAM SERVICE TESTS ---" -ForegroundColor Yellow

Test-Endpoint "IAM Health Check" "$IAM_BASE/health"
Test-Endpoint "IAM Ready Check" "$IAM_BASE/health/ready"
Test-Endpoint "IAM Live Check" "$IAM_BASE/health/live"
Test-Endpoint "Get All Users" "$IAM_BASE/users"
Test-Endpoint "Get Notifications" "$IAM_BASE/notifications"
Test-Endpoint "Unread Count" "$IAM_BASE/notifications/unread-count"
Test-Endpoint "Get Policies" "$IAM_BASE/policies"
Test-Endpoint "Get Settings" "$IAM_BASE/settings"
Test-Endpoint "Get Audit Logs" "$IAM_BASE/audit"
Test-Endpoint "Audit Statistics" "$IAM_BASE/audit/stats"

$newUser = @{
    username = "apitest"
    email    = "apitest@test.com"
    password = "TestPass123!"
    fullName = "API Test User"
}
Test-Endpoint "Create User" "$IAM_BASE/users" "POST" $newUser

# API GATEWAY TESTS  
Write-Host "`n--- API GATEWAY TESTS ---" -ForegroundColor Yellow

Test-Endpoint "Gateway Health" "$GATEWAY_BASE/health"

# STEGO MODULE TESTS
Write-Host "`n--- STEGO MODULE TESTS ---" -ForegroundColor Yellow

Test-Endpoint "Stego Health" "$STEGO_BASE/health"

# GENERATE REPORT
Write-Host "`n========================================" -ForegroundColor Yellow
Write-Host "TEST SUMMARY" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "Total Tests: $($passed + $failed)" -ForegroundColor White
Write-Host "Passed: $passed" -ForegroundColor Green
Write-Host "Failed: $failed" -ForegroundColor Red
$successRate = [Math]::Round(($passed / ($passed + $failed)) * 100, 2)
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Yellow" })

# Save results to JSON
$results | ConvertTo-Json -Depth 5 | Out-File "test_results.json" -Encoding UTF8 -Force
Write-Host "`nResults saved to: test_results.json" -ForegroundColor Green

# Save results to Markdown
$markdown = @"
# API Test Results
**Date:** $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
**Total Tests:** $($passed + $failed)  
**Passed:** $passed  
**Failed:** $failed  
**Success Rate:** $successRate%

## Test Details

| Test | Method | Status | Result |
|------|--------|---------|--------|
"@

foreach ($r in $results) {
    $icon = if ($r.Result -eq "PASSED") { "✅" } else { "❌" }
    $endpoint = $r.Endpoint -replace $IAM_BASE, "" -replace $GATEWAY_BASE, "" -replace $STEGO_BASE, ""
    $markdown += "`n| $($r.Test) | $($r.Method) | $($r.Status) | $icon |"
}

$markdown += "`n`n## Detailed Responses`n`n"

foreach ($r in $results) {
    $markdown += @"
### $($r.Test)
- **Method:** $($r.Method)
- **Endpoint:** $($r.Endpoint)
- **Status:** $($r.Status)
- **Result:** $($r.Result)
- **Response:** ``$($r.Response)``

"@
}

$markdown | Out-File "test_results.md" -Encoding UTF8 -Force
Write-Host "Markdown report saved to: test_results.md" -ForegroundColor Green

Write-Host "`nTest suite completed!" -ForegroundColor Green
