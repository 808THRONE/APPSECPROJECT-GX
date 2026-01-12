# SecureGate IAM Portal - Setup Documentation

## Overview

Complete guide for setting up the SecureGate IAM Portal with all configuration fixes applied.

## Architecture

```
┌─────────────┐
│   Nginx     │  Port 80/443 (Reverse Proxy)
│  (Docker)   │
└──────┬──────┘
       │
       ├──> Frontend (React PWA) - Port 80 (internal)
       ├──> IAM Service (WildFly) - Port 8080 (internal, 8081 external)
       ├──> API Gateway (WildFly) - Port 8080 (internal, 8080 external)
       └──> Stego Module (WildFly) - Port 8080 (internal, 8084 external)
```

## Prerequisites

### Local Development (Non-Docker)
1. **Java 21** - OpenJDK or Oracle JDK
2. **Maven 3.9+**
3. **Node.js 20+** with npm
4. **PostgreSQL 16+**
5. **WildFly 38.0.0.Final** (already in project)

### Docker Deployment
1. **Docker** 24.0+  
2. **Docker Compose** 2.20+

---

## Configuration Files Fixed

### ✅ Frontend (`pwa-frontend/`)
- **vite.config.ts** - Added proxy configuration for local dev
- **Dockerfile** - Fixed build args for environment variables
- **nginx.conf** - SPA routing already configured

### ✅ Infrastructure (`infrastructure/`)
- **nginx/nginx.conf** - Fixed proxy paths to preserve context roots
- **.env** - Environment variables configured

### ✅ Backend Services
All three modules (iam-service, api-gateway, stego-module):
- **beans.xml** - CDI activation
- **CorsFilter.java** - Enhanced with env var support
- **persistence.xml** (IAM only) - PostgreSQL configuration

### ✅ WildFly Configuration
- **postgresql-datasource.cli** - Script to add PostgreSQL support
- **modules/org/postgresql/main/module.xml** - PostgreSQL driver module

---

## Setup Instructions

### Option 1: Docker Compose Deployment (Recommended)

#### Step 1: Navigate to infrastructure directory
```powershell
cd "c:\Users\808th\OneDrive\Desktop\A try\infrastructure"
```

#### Step 2: Review environment variables
Edit `.env` if needed (defaults are suitable for local dev):
```bash
DB_PASSWORD=dev_postgres_pass_2026
REDIS_PASSWORD=dev_redis_pass_2026
JWT_SECRET_KEY=DevSecretKey256BitForLocalTestingOnly_ChangeInProd_32Chars!
```

#### Step 3: Build and start all services
```powershell
docker-compose up --build
```

#### Step 4: Access the application
- **Frontend**: http://localhost
- **IAM Direct**: http://localhost:8081/iam-service/api
- **API Gateway Direct**: http://localhost:8080/api-gateway/api
- **Stego Direct**: http://localhost:8084/stego-module/api

#### Default Credentials
- Username: `admin`
- Password: `admin123`

---

### Option 2: Local WildFly Deployment

#### Step 1: Setup PostgreSQL
```powershell
# Install PostgreSQL 16 if not already installed
# Create database
psql -U postgres
CREATE DATABASE securegate_iam;
CREATE USER securegate WITH PASSWORD 'securegate_password';
GRANT ALL PRIVILEGES ON DATABASE securegate_iam TO securegate;
\q
```

#### Step 2: Download PostgreSQL JDBC Driver
```powershell
# Download to WildFly modules directory
$url = "https://jdbc.postgresql.org/download/postgresql-42.7.1.jar"
$dest = "c:\Users\808th\OneDrive\Desktop\A try\wildfly-38.0.0.Final\modules\system\layers\base\org\postgresql\main\postgresql-42.7.1.jar"
Invoke-WebRequest -Uri $url -OutFile $dest
```

#### Step 3: Configure WildFly Datasource
```powershell
# Start WildFly
cd "c:\Users\808th\OneDrive\Desktop\A try\wildfly-38.0.0.Final"
.\bin\standalone.ps1

# In another terminal, run CLI script
.\bin\jboss-cli.ps1 --connect --file=standalone\configuration\postgresql-datasource.cli
```

#### Step 4: Build Backend Modules
```powershell
cd "c:\Users\808th\OneDrive\Desktop\A try"

# Build all modules
mvn clean install

# Deploy to WildFly
Copy-Item "iam-service\target\iam-service.war" -Destination "wildfly-38.0.0.Final\standalone\deployments\"
Copy-Item "api-gateway\target\api-gateway.war" -Destination "wildfly-38.0.0.Final\standalone\deployments\"
Copy-Item "stego-module\target\stego-module.war" -Destination "wildfly-38.0.0.Final\standalone\deployments\"
```

#### Step 5: Set Environment Variables
```powershell
# Set for current session
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_USER="securegate"
$env:DB_PASS="securegate_password"
$env:DB_NAME="securegate_iam"
$env:PUBLIC_BASE_URL="http://localhost:8081"

# Restart WildFly to pick up environment variables
```

#### Step 6: Run Frontend Dev Server
```powershell
cd "c:\Users\808th\OneDrive\Desktop\A try\pwa-frontend"
npm install
npm run dev
```

Frontend will be available at http://localhost:5173 with proxy to backend services.

---

## Configuration Changes Summary

### 1. Nginx Proxy Fixes ✅
**Issue**: Proxy paths didn't preserve context roots
**Fix**: Changed to `proxy_pass http://iam/iam-service/` with trailing slashes
**Impact**: OAuth2 redirects now work correctly

### 2. Frontend Build Args ✅
**Issue**: Docker build args not wired to Vite
**Fix**: Added ARG and ENV declarations in Dockerfile
**Impact**: Environment-specific API URLs work in containerized deployments

### 3. Vite Dev Proxy ✅
**Issue**: No proxy for local development
**Fix**: Added proxy configuration in vite.config.ts
**Impact**: Local dev server can proxy to backend without CORS issues

### 4. PostgreSQL Integration ✅
**Issue**: H2 in-memory database not suitable for production
**Fix**: 
- Added PostgreSQL driver module
- Created datasource configuration script
- Updated persistence.xml
**Impact**: Persistent data storage

### 5. CORS Configuration ✅
**Issue**: Missing ports, no environment configuration
**Fix**: 
- Added all necessary ports (5173, 8080, 8081, 8084)
- Added CORS_ALLOWED_ORIGINS environment variable support
- Added Max-Age header
**Impact**: Frontend can call backend from any configured origin

### 6. CDI Activation ✅
**Issue**: Missing beans.xml files
**Fix**: Created beans.xml in all three modules
**Impact**: Dependency injection works properly

---

### Option 3: Full Stack on Local WildFly (Recommended for Production) ✅

Run everything (Frontend + Backend) on a single server (`localhost:8080`).

#### 1. Configure Frontend
Create `.env.production` in `pwa-frontend/`:
```env
VITE_API_URL=/api-gateway/api/v1
VITE_IAM_URL=/iam-service/api
VITE_STEGO_URL=/stego-module/api
VITE_STEGO_ENABLED=true
VITE_OAUTH_CLIENT_ID=securegate-frontend
```

#### 2. Build Frontend
```powershell
cd pwa-frontend
npm run build
```

#### 3. Deploy to WildFly (as ROOT.war)
This method ensures Client-Side Routing (SPA) works correctly (fixes 404s).

```powershell
# 1. Set Redirect Base URL (Run once)
cd "..\wildfly-38.0.0.Final"
.\bin\jboss-cli.ps1 --connect --command="/system-property=PUBLIC_BASE_URL:add(value=http://localhost:8080)"

# 2. Prepare ROOT.war directory
$rootWar = "standalone\deployments\ROOT.war"
New-Item -ItemType Directory -Force -Path $rootWar
New-Item -ItemType Directory -Force -Path "$rootWar\WEB-INF"

# 3. Create web.xml for SPA Routing
Set-Content -Path "$rootWar\WEB-INF\web.xml" -Value @"
<?xml version=""1.0"" encoding=""UTF-8""?>
<web-app xmlns=""https://jakarta.ee/xml/ns/jakartaee""
         xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance""
         xsi:schemaLocation=""https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd""
         version=""6.0"">
    <display-name>SecureGate Frontend</display-name>
    <error-page>
        <error-code>404</error-code>
        <location>/index.html</location>
    </error-page>
</web-app>
"@

# 4. Copy Frontend Assets
Copy-Item -Path "..\pwa-frontend\dist\*" -Destination $rootWar -Recurse -Force

# 5. Deploy
New-Item -Path "standalone\deployments\ROOT.war.dodeploy" -ItemType File -Force
```

#### 4. Access Application
Open **http://localhost:8080**
- Login and Signup will work correctly.

---

## Verification

### Check Backend Health
```powershell
# IAM Service
curl http://localhost:8081/iam-service/api/health

# API Gateway
curl http://localhost:8080/api-gateway/api/health

# Stego Module
curl http://localhost:8084/stego-module/api/health
```

### Check Frontend
Visit http://localhost (Docker) or http://localhost:5173 (local dev)

### Test OAuth2 Flow
1. Navigate to frontend
2. Click "Sign In" - should redirect to IAM login page
3. Enter admin/admin123
4. Should redirect back with access token

---

## Troubleshooting

### PostgreSQL Driver Not Found
**Symptom**: `WFLYCTL0180: Services with missing/unavailable dependencies`
**Solution**: 
1. Verify postgresql JAR is in modules directory
2. Check module.xml syntax
3. Restart WildFly

### CORS Errors
**Symptom**: Frontend shows CORS policy errors in browser console
**Solution**:
1. Verify backend CORS filters are deployed
2. Check Nginx is forwarding Origin header
3. Add origin to CORS_ALLOWED_ORIGINS environment variable

### OAuth2 Double-Pathing
**Symptom**: Redirect URLs have `/iam-service/iam-service/`
**Solution**:
- Ensure PUBLIC_BASE_URL doesn't include context root
- Should be `http://localhost` not `http://localhost/iam-service`

### WildFly Deployment Failed
**Symptom**: WAR files in `deployments/` folder with `.failed` extension
**Solution**:
1. Check `standalone/log/server.log` for specific errors
2. Verify all dependencies are in pom.xml
3. Ensure beans.xml exists in WEB-INF

---

## Production Considerations

### Security
1. **Change JWT_SECRET_KEY** - Use 256-bit random key
2. **Use HTTPS** - Configure SSL certificates in Nginx
3. **Database Passwords** - Use strong passwords, never commit to Git
4. **CORS Origins** - Restrict to specific domains

### Performance
1. **Database Connection Pool** - Tune max-pool-size in datasource
2. **JVM Heap** - Adjust in standalone.conf based on load
3. **Build Production Frontend** - `npm run build` instead of dev server

### Monitoring
1. **WildFly Management Console** - http://localhost:9990
2. **Database Monitoring** - pgAdmin or similar
3. **Application Logs** - `wildfly/standalone/log/server.log`

---

## Next Steps

1. ✅ All configuration issues fixed
2. ⏭️ Build and test locally or with Docker
3. ⏭️ Create user accounts and test features
4. ⏭️ Deploy to staging environment
5. ⏭️ Load testing and security audit
6. ⏭️ Production deployment

---

## Support Files Created

- `wildfly-38.0.0.Final/standalone/configuration/postgresql-datasource.cli`
- `wildfly-38.0.0.Final/modules/system/layers/base/org/postgresql/main/module.xml`
- `iam-service/src/main/webapp/WEB-INF/beans.xml`
- `api-gateway/src/main/webapp/WEB-INF/beans.xml`
- `stego-module/src/main/webapp/WEB-INF/beans.xml`

## Modified Files

- `infrastructure/nginx/nginx.conf` - Fixed proxy paths + CORS preflight
- `pwa-frontend/vite.config.ts` - Added dev proxy
- `pwa-frontend/Dockerfile` - Fixed build args
- `iam-service/src/main/resources/META-INF/persistence.xml` - PostgreSQL
- All `CorsFilter.java` files - Enhanced with env var support
