Write-Host "Deploying SecureGate to Local WildFly..."

$wildflyDir = "wildfly-38.0.0.Final"
$deployDir = "$wildflyDir\standalone\deployments"

if (-not (Test-Path $deployDir)) {
    Write-Error "WildFly deployment directory not found at $deployDir"
    exit 1
}

# Clean old deployments (optional, but good for reset)
# Remove-Item "$deployDir\*.war*" -Force -ErrorAction SilentlyContinue

Write-Host "Copying iam-service.war..."
Copy-Item "iam-service\target\iam-service.war" "$deployDir\" -Force

Write-Host "Copying api-gateway.war..."
Copy-Item "api-gateway\target\api-gateway.war" "$deployDir\" -Force

Write-Host "Copying stego-module.war..."
Copy-Item "stego-module\target\stego-module.war" "$deployDir\" -Force

Write-Host "Deployment artifacts copied successfully."
Write-Host "Now start WildFly by running: .\$wildflyDir\bin\standalone.bat"
Write-Host "And start Frontend by running: cd pwa-frontend ; npm run dev"
