
# verification script

Write-Host "Waiting for services to be ready..."
Start-Sleep -Seconds 30

$apiGatewayUrl = "http://localhost:8082/api-gateway/api/v1"
$stegoUrl = "http://localhost:8084/stego-module/api"

# 1. GET Settings (Should be Stego Encoded)
Write-Host "Fetching Settings from API Gateway..."
try {
    $settingsResponse = Invoke-RestMethod -Uri "$apiGatewayUrl/settings" -Method Get -Headers @{ "Content-Type" = "application/json" }
    
    # Save raw response
    $settingsResponse | ConvertTo-Json -Depth 10 | Out-File "api_test/settings_raw.json"
    Write-Host "Saved raw response to api_test/settings_raw.json"

    # Check if it has 'stego' field
    if ($settingsResponse.stego -and $settingsResponse.length) {
        Write-Host "Response appears to be Stego encoded." -ForegroundColor Green
        
        # 2. Extract using Stego Module
        Write-Host "Extracting payload via Stego Module..."
        
        # Invoke-RestMethod automatically parses JSON response to object, so we convert it back to JSON for the POST body
        $bodyJson = $settingsResponse | ConvertTo-Json -Depth 10 -Compress
        
        $decoded = Invoke-RestMethod -Uri "$stegoUrl/stego/extract" -Method Post -Body $bodyJson -Headers @{ "Content-Type" = "application/json" }
        
        $decoded | ConvertTo-Json -Depth 10 | Out-File "api_test/settings_decoded.json"
        Write-Host "Saved decoded response to api_test/settings_decoded.json"
        Write-Host "Decoded Content:"
        Write-Host $decoded
    }
    else {
        Write-Host "Response was NOT Stego encoded:" -ForegroundColor Red
        Write-Host ($settingsResponse | ConvertTo-Json -Depth 5)
    }

}
catch {
    Write-Error "Request failed: $_"
    if ($_.Response) {
        Write-Host "Status Code: $($_.Response.StatusCode)"
        $stream = $_.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Response Body: $($reader.ReadToEnd())"
    }
}
