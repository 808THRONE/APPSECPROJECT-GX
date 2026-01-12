# SecureGate Local Launcher
# Builds and runs all services locally without Docker

Write-Host "Building Backend Modules..." -ForegroundColor Cyan
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed!"
    exit 1
}

Write-Host "Starting IAM Service (Port 8080)..." -ForegroundColor Green
Start-Process mvn -ArgumentList "-f", "iam-service/pom.xml", "wildfly:run", "-Dwildfly.version=38.0.0.Final", "-Djboss.socket.binding.port-offset=0" -NoNewWindow
Start-Sleep -Seconds 5

Write-Host "Starting API Gateway (Port 8082)..." -ForegroundColor Green
Start-Process mvn -ArgumentList "-f", "api-gateway/pom.xml", "wildfly:run", "-Dwildfly.version=38.0.0.Final", "-Djboss.socket.binding.port-offset=2", "-Diam.service.url=http://localhost:8080/iam-service/api", "-Dstego.service.url=http://localhost:8084/stego-module/api" -NoNewWindow
Start-Sleep -Seconds 5

Write-Host "Starting Stego Module (Port 8084)..." -ForegroundColor Green
Start-Process mvn -ArgumentList "-f", "stego-module/pom.xml", "wildfly:run", "-Dwildfly.version=38.0.0.Final", "-Djboss.socket.binding.port-offset=4" -NoNewWindow

Write-Host "Starting Frontend (Port 5173)..." -ForegroundColor Yellow
Set-Location pwa-frontend
npm install
npm run dev
