Write-Host "Deploying SecureGate UI..."

$wildflyDir = "wildfly-38.0.0.Final"
$deployDir = "$wildflyDir\standalone\deployments"

if (-not (Test-Path $deployDir)) {
    Write-Error "WildFly deployment directory not found at $deployDir"
    exit 1
}

Write-Host "Copying securegate-ui.war..."
if (Test-Path "securegate-ui\target\securegate-ui.war") {
    Copy-Item "securegate-ui\target\securegate-ui.war" "$deployDir\" -Force
    Write-Host "Deployed successfully."
}
else {
    Write-Error "WAR file not found. Did you build it?"
}
