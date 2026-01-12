$urls = @(
    "http://localhost:8080/iam-service/api/health",
    "http://localhost:8080/api-gateway/api/health",
    "http://localhost:8080/stego-module/api/health"
)

foreach ($url in $urls) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            Write-Output "SUCCESS: $url is UP"
        }
        else {
            Write-Output "WARNING: $url returned $($response.StatusCode)"
        }
    }
    catch {
        Write-Output "FAILURE: $url - Error: $($_.Exception.Message)"
    }
}
