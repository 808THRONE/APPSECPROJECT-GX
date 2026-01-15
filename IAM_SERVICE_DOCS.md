# 🆔 IAM Service Documentation - SecureGate IAM

The **IAM Service** (`iam-service.war`) is the "Source of Truth" for the SecureGate portal. It is a robust **Identity Provider (IdP)** and **Policy Decision Point (PDP)**, managing the entire lifecycle of users, credentials, and access roles.

---

## 🏗️ Core Responsibilities

The IAM Service is the brain of the system, handling:
1.  **Identity Management**: Secure storage and lifecycle of user accounts and roles.
2.  **Authentication (OIDC/OAuth2)**: Issuing high-security tokens (Ed25519-signed JWTs).
3.  **Multi-Factor Authentication (MFA)**: Enforcing time-based one-time passwords (TOTP).
4.  **Authorization (ABAC)**: Evaluating fine-grained access policies.
5.  **Tamper-Evident Auditing**: Creating an immutable trail of security events.

---

## 📂 File-to-Feature Mapping

### 1. Identity & Credentials
| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[User.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/model/User.java)** | Identity Model | JPA entity defining user attributes, status, and password hashes. |
| **[CryptoService.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/service/CryptoService.java)** | Cryptography | Implements **Argon2id** password hashing for secure credential storage. |
| **[UserResource.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/resources/UserResource.java)** | User API | Provides endpoints for user registration, profile management, and promotion. |

### 2. Authentication & JWT
| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[OAuth2Resource.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/oauth/OAuth2Resource.java)** | Auth Protocol | Implements the OAuth2 Authorization Code flow (Login, Callback, Register). |
| **[TokenService.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/service/TokenService.java)** | Token Minting | Generates and signs JWTs using the **Ed25519 (EdDSA)** algorithm. |
| **[KeyManagementService.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/service/KeyManagementService.java)** | Key Lifecycle | Manages the **ephemeral** signing keys used for JWT production. |

### 3. Multi-Factor Authentication (MFA)
| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[TotpService.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/mfa/TotpService.java)** | MFA Logic | Generates QR codes and validates 6-digit TOTP codes. |
| **[MfaResource.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/mfa/MfaResource.java)** | MFA API | REST endpoints for MFA setup, verification, and status checks. |

### 4. Policy Engine (PDP)
| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[PdpService.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/service/PdpService.java)** | Policy Engine | Evaluates **Attribute-Based Access Control (ABAC)** rules. |
| **[Policy.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/model/Policy.java)** | Rule Model | Defines the `resource`, `action`, and `condition` for an access rule. |
| **[PolicyResource.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/resources/PolicyResource.java)** | Policy API | Endpoints for managing and evaluating security policies. |

### 5. Audit & Integrity
| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[AuditService.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/service/AuditService.java)** | Integrity | Implements **Hash-Chaining** to detect log tampering. |
| **[AuditLogRepository.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/iam-service/src/main/java/com/securegate/iam/repository/AuditLogRepository.java)** | Persistence | Data access layer for security logs. |

---

## 🔐 Advanced Security Features

### 💎 Post-Quantum Ready JWTs
- **Algorithm**: EdDSA (Ed25519) via `TokenService.java`.
- **Rationale**: Ed25519 is faster, more secure than RSA, and resistant to side-channel attacks.
- **Forward Secrecy**: Keys are stored in-memory and re-generated on server restart.

### 🛡️ Argon2id Password Hashing
- **File**: `CryptoService.java`
- **Configuration**: Uses 64MB memory, 3 iterations, and 4 degrees of parallelism to maximize resistance to GPU-based password cracking.

### 🧱 Blockchain-Lite Auditing
- **Mechanism**: Every new audit log contains the SHA-256 hash of the previous log entry.
- **Verification**: If a single entry is modified, the hash chain breaks, immediately flagging a security compromise.

---

## ⚙️ Configuration & Environment

The IAM service relies on the following key settings (managed via `persistence.xml` or system properties):

| Aspect | Variable / Target | Note |
|:-------|:------------------|:-----|
| **Database** | `java:jboss/datasources/SecureGateDS` | WildFly JNDI Datasource for PostgreSQL. |
| **Issuer** | `jwt.issuer` | The OIDC issuer URL (e.g., https://iam.securegate.io). |
| **Expiration** | `jwt.expiration.seconds` | Lifespan of the issued access tokens. |
| **MFA Name** | `mfa.issuer.name` | Name displayed in the Authenticator App (e.g., SecureGate). |

---

## 🚀 Execution & Maintenance
- **Build**: `mvn clean package -pl iam-service`
- **Seeding**: Initial roles and an admin user are seeded by `DatabaseInitializer.java` on first startup.
- **Logs**: System logs can be monitored in the WildFly `server.log`.
