Write-Host "Building and Deploying SecureGate Full Stack..."

# 1. Build All
Write-Host "Building all modules..."
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed."
    exit 1
}

# 2. Deploy
$wildflyDir = "wildfly-38.0.0.Final"
$deployDir = "$wildflyDir\standalone\deployments"

if (-not (Test-Path $deployDir)) {
    Write-Error "WildFly deployment directory not found at $deployDir"
    exit 1
}

$modules = @("iam-service", "api-gateway", "stego-module", "securegate-ui")

foreach ($mod in $modules) {
    $warPath = "$mod\target\$mod.war"
    if (Test-Path $warPath) {
        Write-Host "Deploying $mod..."
        Copy-Item $warPath "$deployDir\" -Force
    }
    else {
        Write-Warning "WAR for $mod not found!"
    }
}

Write-Host "All services deployed."
Write-Host "Access UI at http://localhost:8080/"
