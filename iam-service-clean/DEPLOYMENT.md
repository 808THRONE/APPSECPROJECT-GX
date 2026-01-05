# Deployment Guide - Clean IAM Service

## Quick Start (5 minutes)

### Option 1: H2 Database (Development)

```bash
cd iam-service-clean

# Build
mvn clean package

# Deploy to WildFly
cp target/iam-service.war $WILDFLY_HOME/standalone/deployments/

# Access at
http://localhost:8080/iam/authorize?client_id=demo-client&response_type=code&redirect_uri=http://localhost:3000/callback&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256
```

### Option 2: PostgreSQL (Production - iam.mortadha.me)

## Production Deployment Steps

### 1. Database Setup

```bash
# Create database
sudo -u postgres psql -c "CREATE DATABASE iam_production;"
sudo -u postgres psql -c "CREATE USER iam_user WITH ENCRYPTED PASSWORD 'YOUR_SECURE_PASSWORD';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE iam_production TO iam_user;"

# Load schema
sudo -u postgres psql iam_production < src/main/resources/schema.sql
```

### 2. WildFly Datasource

```bash
# Connect to CLI
/opt/wildfly/bin/jboss-cli.sh --connect

# Add datasource
data-source add \
    --name=PostgresDS \
    --jndi-name=java:jboss/datasources/PostgresDS \
    --driver-name=postgresql \
    --connection-url=jdbc:postgresql://localhost:5432/iam_production \
    --user-name=iam_user \
    --password=YOUR_SECURE_PASSWORD \
    --enabled=true
```

### 3. Generate Production Password Hash

```bash
# On your development machine, create a small Java class:
import me.mortadha.iam.security.Argon2Utility;

public class HashGenerator {
    public static void main(String[] args) {
        String password = "YourSecurePassword123!";
        String hash = Argon2Utility.hashFromString(password);
        System.out.println("Hash: " + hash);
    }
}
```

### 4. Update Database with Real Data

```sql
-- Update demo user password with real hash
UPDATE identities 
SET password = '$argon2id$v=19$m=97579,t=23,p=2$YOUR_GENERATED_HASH'
WHERE username = 'admin@mortadha.me';

-- Update tenant secret
UPDATE tenants
SET tenant_secret = 'YOUR_PRODUCTION_SECRET',
    redirect_uri = 'https://mortadha.me/callback'
WHERE tenant_id = 'demo-client';
```

### 5. Build and Deploy

```powershell
# On Windows
cd "c:\Users\Mortadha Jabari\Cours\appsec_project\AppSec\APPSECPROJECT-GX\iam-service-clean"
mvn clean package

# Transfer to VPS
scp target/iam-service.war user@YOUR_VPS:/tmp/
```

```bash
# On VPS
sudo cp /tmp/iam-service.war /opt/wildfly/standalone/deployments/
sudo chown wildfly:daemon /opt/wildfly/standalone/deployments/iam-service.war

# Watch deployment
tail -f /opt/wildfly/standalone/log/server.log
```

### 6. DNS Configuration

Add DNS A record:
```
Type: A
Name: iam
Value: YOUR_VPS_IP
TTL: 3600
```

### 7. Test

```bash
# Test JWKS endpoint
curl https://iam.mortadha.me/iam/jwk

# Test OAuth flow (will redirect to login)
curl -L "https://iam.mortadha.me/iam/authorize?client_id=demo-client&response_type=code&redirect_uri=https://mortadha.me/callback&code_challenge_method=S256&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
```

## API Endpoints

- `GET /iam/authorize` - OAuth 2.1 authorization endpoint
- `POST /iam/login` - User authentication
- `POST /iam/token` - Token exchange endpoint
- `GET /iam/jwk` - Public keys (JWKS)

## Testing OAuth Flow

### 1. Authorization Request

```
GET /iam/authorize?
  client_id=demo-client&
  response_type=code&
  redirect_uri=http://localhost:3000/callback&
  code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&
  code_challenge_method=S256&
  scope=openid profile email&
  state=xyz123
```

### 2. User Login

- Enter credentials: `admin@mortadha.me` / `Admin123!`
- Approve consent (if first time)

### 3. Exchange Code for Token

```bash
curl -X POST https://iam.mortadha.me/iam/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=YOUR_AUTH_CODE" \
  -d "code_verifier=YOUR_CODE_VERIFIER" \
  -d "redirect_uri=http://localhost:3000/callback"
```

## Security Features

✅ **PKCE Mandatory** - All authorization code flows require PKCE  
✅ **EdDSA Signatures** - JWT signed with Ed25519 (faster than RS256)  
✅ **Key Rotation** - Automatic Ed25519 key pair rotation  
✅ **Argon2id** - State-of-the-art password hashing  
✅ **ChaCha20-Poly1305** - Authorization code encryption  
✅ **Secure Cookies** - HttpOnly, Secure, SameSite=Strict  

## Differences from Original

| Feature | Original (Phoenix) | New (Clean) |
|---------|-------------------|-------------|
| Package | `xyz.kaaniche.phoenix` | `me.mortadha.iam` |
| Database | MySQL config (wrong) | PostgreSQL + H2 |
| Duplicates | Yes (2 OAuth impls) | No |
| TODOs | 3 incomplete | All complete |
| Lines of Code | ~2000 | ~1200 |
| Complexity | High | Medium |
| Production Ready | After fixes | Yes |

## Next Steps

1. ✅ Build and test locally with H2
2. ✅ Deploy to VPS with PostgreSQL
3. ⏳ Add refresh token support
4. ⏳ Add rate limiting
5. ⏳ Add audit logging
6. ⏳ Add health check endpoint
7. ⏳ Set up monitoring

## Support

- Issues: Check WildFly logs
- Database: Verify datasource connection
- JWT: Check `/iam/jwk` endpoint
