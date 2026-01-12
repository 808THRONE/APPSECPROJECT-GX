# SecureGate IAM Portal - API Testing Guide

This guide provides the necessary information to test and verify the local deployment of the SecureGate system.

## Test Credentials

| Username | Password | Role | Purpose |
| :--- | :--- | :--- | :--- |
| `admin` | (Auto-created) | ADMIN | Full system access via SSO |
| `testuser` | `password123` | USER | General testing |
| `finaluser` | `password123` | USER | Verification of recent deployment |

## Service Endpoints

| Service | Base URL | Note |
| :--- | :--- | :--- |
| **PWA Frontend** | `http://localhost:5173` | Main user interface |
| **IAM Service** | `http://localhost:8080/iam-service/api` | Authentication & User Management |
| **Stego Module** | `http://localhost:8080/stego-module/api` | Steganography & Encryption |
| **API Gateway** | `http://localhost:8080/api-gateway/api` | Resource Gateway |

## 1. Testing Steganography (Stego Module)

Use the following PowerShell script to verify the **STC Steganography** module. It embeds a secret message into a "cover" bit array and then extracts it back.

```powershell
# 1. Prepare Payload
$cover = 1..1000 | ForEach-Object { Get-Random -Minimum 0 -Maximum 2 }
$body = @{ cover = $cover; payload = "Hello SecureGate" } | ConvertTo-Json

# 2. Embed Secret Message
$response = Invoke-RestMethod -Uri "http://localhost:8080/stego-module/api/stego/embed" -Method Post -ContentType "application/json" -Body $body
$stego = $response.stego
Write-Host "Stego message embedded into $($stego.Count) bits."

# 3. Extract Secret Message
$extractBody = @{ stego = $stego; length = 1000 } | ConvertTo-Json
$decrypted = Invoke-RestMethod -Uri "http://localhost:8080/stego-module/api/stego/extract" -Method Post -ContentType "application/json" -Body $extractBody
Write-Host "Decrypted message: $decrypted"
```

## 2. Testing User Management (IAM Service)

To create a new user manually for database verification:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/iam-service/api/users" -Method Post -ContentType "application/json" -Body @{
    username = "new_tester"
    email = "tester@securegate.local"
    password = "password123"
    fullName = "API Tester"
} | ConvertTo-Json
```

## 3. Database Verification (H2)

The system uses an **H2 In-Memory Database** for local development. 
- **JDBC URL:** `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
- **User:** `sa`
- **Password:** (blank)

> [!NOTE]
> Direct database access via H2 Console is currently disabled for maximum deployment stability. Use the **IAM API** to verify data persistence.

## 4. Troubleshooting

- **404 Not Found:** Ensure WildFly is running (`./standalone.ps1`) and the WAR files are in `standalone/deployments`.
- **CORS Errors:** The APIs are configured to allow `http://localhost:5173`. Ensure you are accessing the frontend via this URL.
