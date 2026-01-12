# SecureGate IAM Portal - Quick Deployment Script
# This script helps deploy the application to WildFly

Write-Host "`n=== SecureGate IAM Portal - Deployment Helper ===" -ForegroundColor Cyan
Write-Host "This script will help you deploy to WildFly`n" -ForegroundColor White

# Paths
$projectRoot = "c:\Users\808th\OneDrive\Desktop\A try"
$wildflyRoot = "$projectRoot\wildfly-38.0.0.Final"
$deploymentsDir = "$wildflyRoot\standalone\deployments"

# Check if WildFly exists
if (-not (Test-Path $wildflyRoot)) {
    Write-Host "❌ WildFly not found at: $wildflyRoot" -ForegroundColor Red
    exit 1
}

Write-Host "✅ WildFly found at: $wildflyRoot" -ForegroundColor Green

# Check if WAR files exist
$wars = @(
    "$projectRoot\iam-service\target\iam-service.war",
    "$projectRoot\api-gateway\target\api-gateway.war",
    "$projectRoot\stego-module\target\stego-module.war"
)

$allWarsExist = $true
foreach ($war in $wars) {
    if (Test-Path $war) {
        $warName = Split-Path $war -Leaf
        Write-Host "✅ Found: $warName" -ForegroundColor Green
    }
    else {
        $warName = Split-Path $war -Leaf
        Write-Host "❌ Missing: $warName" -ForegroundColor Red
        $allWarsExist = $false
    }
}

if (-not $allWarsExist) {
    Write-Host "`n❌ Some WAR files are missing. Run 'mvn clean install' first." -ForegroundColor Red
    exit 1
}

# Menu
Write-Host "`nWhat would you like to do?" -ForegroundColor Yellow
Write-Host "1. Deploy WAR files to WildFly"
Write-Host "2. Start WildFly"
Write-Host "3. Deploy + Start WildFly"
Write-Host "4. Check WildFly status"
Write-Host "5. View deployment logs"
Write-Host "6. Exit"

$choice = Read-Host "`nEnter choice (1-6)"

switch ($choice) {
    "1" {
        Write-Host "`n📦 Deploying WAR files..." -ForegroundColor Cyan
        
        foreach ($war in $wars) {
            $warName = Split-Path $war -Leaf
            Write-Host "Copying $warName..." -ForegroundColor White
            Copy-Item $war -Destination $deploymentsDir -Force
        }
        
        Write-Host "`n✅ All WAR files deployed to: $deploymentsDir" -ForegroundColor Green
        Write-Host "If WildFly is running, deployments should start automatically." -ForegroundColor Yellow
        Write-Host "Watch the WildFly console for deployment messages." -ForegroundColor Yellow
    }
    
    "2" {
        Write-Host "`n🚀 Starting WildFly..." -ForegroundColor Cyan
        Write-Host "WildFly will start in this window. Press Ctrl+C to stop.`n" -ForegroundColor Yellow
        
        Set-Location $wildflyRoot
        .\bin\standalone.ps1
    }
    
    "3" {
        Write-Host "`n📦 Deploying WAR files..." -ForegroundColor Cyan
        
        foreach ($war in $wars) {
            $warName = Split-Path $war -Leaf
            Write-Host "Copying $warName..." -ForegroundColor White
            Copy-Item $war -Destination $deploymentsDir -Force
        }
        
        Write-Host "✅ WAR files deployed" -ForegroundColor Green
        Write-Host "`n🚀 Starting WildFly..." -ForegroundColor Cyan
        Write-Host "WildFly will start in this window. Press Ctrl+C to stop.`n" -ForegroundColor Yellow
        
        Set-Location $wildflyRoot
        .\bin\standalone.ps1
    }
    
    "4" {
        Write-Host "`n🔍 Checking WildFly status..." -ForegroundColor Cyan
        
        # Check if WildFly process is running
        $wildflyProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue | 
        Where-Object { $_.Path -like "*wildfly*" }
        
        if ($wildflyProcess) {
            Write-Host "✅ WildFly is running (PID: $($wildflyProcess.Id))" -ForegroundColor Green
        }
        else {
            Write-Host "❌ WildFly is not running" -ForegroundColor Red
        }
        
        # Check deployed files
        Write-Host "`n📂 Deployed files:" -ForegroundColor Cyan
        $deployedFiles = Get-ChildItem $deploymentsDir -Filter "*.war*"
        
        if ($deployedFiles.Count -gt 0) {
            foreach ($file in $deployedFiles) {
                if ($file.Name -like "*.deployed") {
                    Write-Host "✅ $($file.Name)" -ForegroundColor Green
                }
                elseif ($file.Name -like "*.failed") {
                    Write-Host "❌ $($file.Name)" -ForegroundColor Red
                }
                else {
                    Write-Host "⏳ $($file.Name)" -ForegroundColor Yellow
                }
            }
        }
        else {
            Write-Host "No deployments found" -ForegroundColor Yellow
        }
        
        # Test health endpoints
        Write-Host "`n🏥 Testing health endpoints:" -ForegroundColor Cyan
        
        $endpoints = @{
            "IAM Service"  = "http://localhost:8081/iam-service/api/health"
            "API Gateway"  = "http://localhost:8080/api-gateway/api/health"
            "Stego Module" = "http://localhost:8084/stego-module/api/health"
        }
        
        foreach ($service in $endpoints.Keys) {
            $url = $endpoints[$service]
            try {
                $response = Invoke-WebRequest -Uri $url -TimeoutSec 5 -UseBasicParsing
                if ($response.StatusCode -eq 200) {
                    Write-Host "✅ $service is UP" -ForegroundColor Green
                }
                else {
                    Write-Host "⚠️  $service returned: $($response.StatusCode)" -ForegroundColor Yellow
                }
            }
            catch {
                Write-Host "❌ $service is DOWN or unreachable" -ForegroundColor Red
            }
        }
    }
    
    "5" {
        Write-Host "`n📋 Showing last 50 lines of WildFly log..." -ForegroundColor Cyan
        $logFile = "$wildflyRoot\standalone\log\server.log"
        
        if (Test-Path $logFile) {
            Get-Content $logFile -Tail 50
        }
        else {
            Write-Host "❌ Log file not found: $logFile" -ForegroundColor Red
        }
    }
    
    "6" {
        Write-Host "`nGoodbye!" -ForegroundColor Cyan
        exit 0
    }
    
    default {
        Write-Host "`n❌ Invalid choice. Please run the script again." -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n" -ForegroundColor White
