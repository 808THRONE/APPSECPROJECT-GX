# SecureGate IAM Portal - Verification Test Plan

## Build Verification ✅

### Maven Build Status
- ✅ Parent POM configured correctly
- ✅ All modules build successfully
- ✅ WAR files generated

### Generated Artifacts
Check these files exist:
- `iam-service/target/iam-service.war`
- `api-gateway/target/api-gateway.war`
- `stego-module/target/stego-module.war`

---

## Deployment Options

### Option 1: Docker Compose (Recommended for Quick Testing)

#### Prerequisites
- Docker Desktop running
- Docker Compose installed

#### Steps
```powershell
cd "c:\Users\808th\OneDrive\Desktop\A try\infrastructure"
docker-compose up --build
```

#### Expected Result
All services start and are accessible:
- Frontend: http://localhost
- IAM: http://localhost:8081/iam-service/api/health
- API Gateway: http://localhost:8080/api-gateway/api/health
- Stego: http://localhost:8084/stego-module/api/health

---

### Option 2: Local WildFly (For Development)

#### Prerequisites Checklist
- [ ] PostgreSQL 16 installed and running
- [ ] Database `securegate_iam` created
- [ ] PostgreSQL JDBC driver downloaded (42.7.1)
- [ ] WildFly 38.0.0.Final ready

#### Step 1: Setup PostgreSQL Database
```sql
-- Run in PostgreSQL (psql)
CREATE DATABASE securegate_iam;
CREATE USER securegate WITH PASSWORD 'securegate_password';
GRANT ALL PRIVILEGES ON DATABASE securegate_iam TO securegate;
```

#### Step 2: Download PostgreSQL JDBC Driver
```powershell
# Download to WildFly modules directory
$driverDir = "c:\Users\808th\OneDrive\Desktop\A try\wildfly-38.0.0.Final\modules\system\layers\base\org\postgresql\main"
$driverUrl = "https://jdbc.postgresql.org/download/postgresql-42.7.1.jar"
$driverPath = "$driverDir\postgresql-42.7.1.jar"

# Create directory if it doesn't exist
New-Item -ItemType Directory -Force -Path $driverDir

# Download driver
Invoke-WebRequest -Uri $driverUrl -OutFile $driverPath

# Verify download
if (Test-Path $driverPath) {
    Write-Host "✅ PostgreSQL driver downloaded successfully" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to download driver" -ForegroundColor Red
}
```

#### Step 3: Start WildFly
```powershell
cd "c:\Users\808th\OneDrive\Desktop\A try\wildfly-38.0.0.Final"
.\bin\standalone.ps1
```

Wait for: `WFLYSRV0025: WildFly Full 38.0.0.Final (WildFly Core 20.0.0.Final) started`

#### Step 4: Configure PostgreSQL Datasource
```powershell
# In a NEW terminal (keep WildFly running)
cd "c:\Users\808th\OneDrive\Desktop\A try\wildfly-38.0.0.Final"
.\bin\jboss-cli.ps1 --connect --file=standalone\configuration\postgresql-datasource.cli
```

Expected output: `{"outcome" => "success"}`

#### Step 5: Deploy WAR Files
```powershell
# Copy WAR files to WildFly deployments directory
$wildflyBase = "c:\Users\808th\OneDrive\Desktop\A try\wildfly-38.0.0.Final"
$deploymentsDir = "$wildflyBase\standalone\deployments"

Copy-Item "c:\Users\808th\OneDrive\Desktop\A try\iam-service\target\iam-service.war" -Destination $deploymentsDir -Force
Copy-Item "c:\Users\808th\OneDrive\Desktop\A try\api-gateway\target\api-gateway.war" -Destination $deploymentsDir -Force
Copy-Item "c:\Users\808th\OneDrive\Desktop\A try\stego-module\target\stego-module.war" -Destination $deploymentsDir -Force

Write-Host "✅ WAR files copied to deployments directory" -ForegroundColor Green
```

Watch WildFly console for deployment messages:
- `Deployed "iam-service.war"`
- `Deployed "api-gateway.war"`
- `Deployed "stego-module.war"`

#### Step 6: Start Frontend Dev Server
```powershell
# In a NEW terminal
cd "c:\Users\808th\OneDrive\Desktop\A try\pwa-frontend"
npm install
npm run dev
```

Frontend will be available at: http://localhost:5173

---

## Verification Tests

### Test 1: Backend Health Checks ✅

```powershell
# IAM Service
curl http://localhost:8081/iam-service/api/health

# API Gateway
curl http://localhost:8080/api-gateway/api/health

# Stego Module
curl http://localhost:8084/stego-module/api/health
```

**Expected Response**: `{"status":"UP"}` for each service

### Test 1b: Full Stack Single Server (Option 3) ✅

1. Deploy frontend as `ROOT.war` (see updated SETUP.md Option 3)
2. Open http://localhost:8080
3. Verify Login/Signup works (Redirects to proper port, SPA routing handles 404s)
4. Check Network Tab:
   - Requests go to `http://localhost:8080/api-gateway/...`

---

### Test 2: Frontend Loads ✅

1. Open browser to http://localhost:5173 (local) or http://localhost (Docker)
2. Should see SecureGate login page
3. No console errors related to CORS or missing resources

---

### Test 3: OAuth2 Authentication Flow ✅

#### Step-by-Step Test
1. **Navigate to frontend**
   - URL: http://localhost:5173
   
2. **Click "Sign In" button**
   - Should redirect to: `http://localhost:8081/iam-service/api/oauth2/authorize?...`
   - Should see IAM login form (dark blue theme)
   
3. **Enter credentials**
   - Username: `admin`
   - Password: `admin123`
   - Click "Sign In"
   
4. **Verify redirect back**
   - Should redirect to: `http://localhost:5173/callback?code=...&state=...`
   - Callback page should exchange code for token
   - Should redirect to dashboard
   
5. **Check token storage**
   - Open browser DevTools → Application → Local Storage
   - Should see `access_token` key with JWT value
   
6. **Verify dashboard loads**
   - Should see "Dashboard Overview" with user stats
   - Should see navigation menu with Users, Policies, Audit, Stego
   - Should see decoded token info at bottom

---

### Test 4: API Calls with Authentication ✅

```javascript
// In browser console at http://localhost:5173
// Test authenticated API call

fetch('/api-gateway/api/v1/users', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
  }
})
.then(r => r.json())
.then(console.log)
.catch(console.error)
```

**Expected**: Should return user list or empty array (no CORS errors)

---

### Test 5: Registration Flow ✅

1. On login page, click "Sign Up" link
2. Should navigate to registration form
3. Fill in:
   - Username: `testuser`
   - Email: `test@example.com`
   - Password: `Test123!`
4. Click "Create Account"
5. Should auto-login and redirect to dashboard

---

### Test 6: SPA Routing (Deep Links) ✅

1. Navigate to dashboard: http://localhost:5173/users
2. Refresh page (F5)
3. Should stay on `/users` route (not 404)
4. Try other routes:
   - http://localhost:5173/policies
   - http://localhost:5173/audit
   - http://localhost:5173/stego

**Expected**: All routes work after refresh

---

## Troubleshooting

### Issue: PostgreSQL Driver Not Found
**Symptom**: `WFLYCTL0180: Services with missing/unavailable dependencies`

**Solution**:
1. Verify `postgresql-42.7.1.jar` exists in modules directory
2. Check `module.xml` syntax is correct
3. Restart WildFly completely: `Ctrl+C` then `.\bin\standalone.ps1`

---

### Issue: Datasource Creation Failed
**Symptom**: CLI script returns error

**Solution**:
```powershell
# Manual datasource creation via CLI
.\bin\jboss-cli.ps1 --connect

# In CLI prompt:
data-source add --name=SecureGateDS --jndi-name=java:jboss/datasources/SecureGateDS --driver-name=postgresql --connection-url=jdbc:postgresql://localhost:5432/securegate_iam --user-name=securegate --password=securegate_password --enabled=true

/subsystem=datasources/data-source=SecureGateDS:test-connection-in-pool

reload
```

---

### Issue: WAR Deployment Failed
**Symptom**: `.war.failed` file appears in deployments directory

**Solution**:
1. Check `wildfly-38.0.0.Final/standalone/log/server.log`
2. Look for specific error messages
3. Common causes:
   - Missing datasource → Configure PostgreSQL first
   - CDI issues → Verify beans.xml exists
   - Compilation errors → Rebuild with `mvn clean install`

---

### Issue: CORS Errors in Browser
**Symptom**: `Access to fetch at ... has been blocked by CORS policy`

**Solution**:
1. Verify backend is running and accessible
2. Check origin in browser console error message
3. If using non-standard port/domain, add to `.env`:
   ```bash
   CORS_ALLOWED_ORIGINS=http://localhost:3000,https://app.example.com
   ```
4. Restart backend services

---

### Issue: Frontend Can't Connect to Backend
**Symptom**: Network errors, 404s

**Solution**:
1. **Docker**: Ensure nginx is running: `docker ps`
2. **Local**: Check Vite proxy in `vite.config.ts`
3. Verify backend health endpoints respond
4. Check firewall isn't blocking ports 8080, 8081, 8084

---

## Success Criteria

✅ All three WAR files build successfully  
✅ PostgreSQL datasource configured in WildFly  
✅ All three services deploy without errors  
✅ Health endpoints return `{"status":"UP"}`  
✅ Frontend loads without errors  
✅ Login redirects to IAM service  
✅ OAuth2 flow completes with token  
✅ Dashboard loads with user info  
✅ SPA routing works (deep links + refresh)  
✅ API calls work with JWT authentication  

---

## Next Steps After Verification

1. ✅ **Create test users** - Test user management features
2. ✅ **Test policies** - Create and assign policies
3. ✅ **Test audit logs** - Verify logging works
4. ✅ **Test MFA** - Enable TOTP authentication
5. ✅ **Test stego module** - Encrypt/decrypt data
6. ⏭️ **Performance testing** - Load test with multiple users
7. ⏭️ **Security audit** - Penetration testing
8. ⏭️ **Production deployment** - Deploy to staging/production

---

## Support Files

- **SETUP.md** - Detailed setup instructions
- **FIXES_SUMMARY.md** - Quick reference of all changes
- **task.md** - Implementation progress tracking
- **walkthrough.md** - Detailed before/after comparison

All configuration issues have been resolved. The application should now work correctly! 🎉
