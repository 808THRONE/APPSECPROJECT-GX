# 🛡️ API Gateway Documentation - SecureGate IAM

The **API Gateway** (`api-gateway.war`) is the resilient edge component of the SecureGate portal. It serves as a **Security Enforcement Point (SEP)** and **Intelligent Reverse Proxy**, shielding internal microservices from direct public exposure.

---

## 🏗️ Architecture Role

The Gateway operates at the boundary of the "Trust Zone". Its primary responsibilities include:
1.  **Edge Authentication**: Validating user identity before request propagation.
2.  **Authorization Enforcement**: Checking permissions against defined policies.
3.  **Cross-Origin Control**: Managing browser-based access from trusted domains.
4.  **Backend Sealing**: Routing requests to internal services (IAM, Stego) via a private network.
5.  **Stego Integration**: Automatically masking sensitive payloads in "Overt Channels".

---

## � File-to-Feature Mapping

| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[JwtAuthenticationFilter.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/filter/JwtAuthenticationFilter.java)** | Authentication | Parses and validates RS256 JWT signatures for every request. |
| **[PolicyEnforcementFilter.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/filter/PolicyEnforcementFilter.java)** | Authorization | Evaluates access permissions using the `@RequiresPolicy` annotation. |
| **[SecurityHeadersFilter.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/filter/SecurityHeadersFilter.java)** | Hardening | Injects HSTS, CSP, X-Frame-Options, and Referrer policies. |
| **[CorsFilter.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/filter/CorsFilter.java)** | Network Sec | Manages the dynamic CORS whitelist and pre-flight `OPTIONS` requests. |
| **[ProxyResource.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/resources/ProxyResource.java)** | Routing | Maps `/v1/` REST endpoints to internal service calls. |
| **[IamProxyClient.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/service/IamProxyClient.java)** | Integration | Handles HTTP communication with the IAM service and injects Steganography. |
| **[ApiGatewayConfig.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/config/ApiGatewayConfig.java)** | Config | Centralized loader for environment variables and system properties. |
| **[ApiApplication.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/api-gateway/src/main/java/com/securegate/api/ApiApplication.java)** | Entry Point | Boots the Jakarta REST (JAX-RS) application. |

---

## 🔐 Security Deep Dive

### 1. Edge Authentication
**File**: `JwtAuthenticationFilter.java`
- **Mechanism**: Intercepts every request.
- **Verification**: Parses the `Authorization: Bearer` token or `access_token` cookie. Validates the **RS256 signature** using a public key.
- **Identity Context**: Populates the internal `SecurityContext` with the user's principal name and roles.

### 2. Policy Enforcement
**File**: `PolicyEnforcementFilter.java`
- **Mechanism**: Bound to methods via `RequiresPolicy.java` annotation.
- **Logic**: Evaluates if the authenticated subject has permission for specific `actions` on `resources`.
- **Default Deny**: Fail-closed model—unmatched requests return `403 Forbidden`.

### 3. Global Security Headers
**File**: `SecurityHeadersFilter.java`
- **HSTS**: `Strict-Transport-Security: max-age=31536000` (1 year).
- **CSP**: `Content-Security-Policy: default-src 'self'...`
- **Anti-Clickjacking**: `X-Frame-Options: DENY`.
- **MIME Protection**: `X-Content-Type-Options: nosniff`.

---

## 🚦 Routing Logic

### Intelligent Proxy
- **Proxy Resource**: Dispatches calls to `iam-service` or `stego-module`.
- **Stego Enhancement**: In `IamProxyClient.java`, sensitive JSON responses are automatically wrapped in a steganographic carrier if `STEGO_ENABLED` is set to `true`.

---

## ⚙️ Configuration Variables

| Variable | Source File | Purpose |
|----------|-------------|---------|
| `IAM_SERVICE_URL` | `ApiGatewayConfig.java` | API endpoint for IAM. |
| `JWT_PUBLIC_KEY` | `ApiGatewayConfig.java` | Base64 RSA public key for signature verification. |
| `STEGO_ENABLED` | `ApiGatewayConfig.java` | Toggles automatic payload masking. |
| `CORS_ALLOWED_ORIGINS` | `CorsFilter.java` | Whitelist for trusted domains. |

---

## 🚀 Deployment Context
- **Packaging**: `mvn clean package -pl api-gateway`
- **Container**: `Dockerfile` implements a secure, minimal runtime image.
- **Target**: WildFly 38+ / Jakarta EE 11.
