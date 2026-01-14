# SecureGate IAM Portal: Technical Architecture & Security Manual

**SecureGate** is a high-assurance **Identity and Access Management (IAM)** framework engineered to demonstrate **Zero-Trust architecture**, **Post-Quantum Ready Cryptography**, and **Overt Channel Security** (Steganography).

This document serves as the **Definitive Technical Manual**, providing a transparent, granular look into the system's architecture, cryptographic primitives, and mitigation strategies against the OWASP Top 10.

---

## 1. System Architecture Diagram

The system operates on a unified trust model where the **API Gateway** acts as the enforcement point for all traffic, while the **IAM Service** is the comprehensive Source of Truth.

```mermaid
graph TD
    User([User / Browser]) -->|HTTPS/TLS 1.3| UI[SecureGate UI (PWA)]
    UI -->|REST / JSON| Gateway[API Gateway]
    
    subgraph "Trust Zone: Internal Network"
        Gateway -->|Verify JWT + Inject Headers| IAM[IAM Service]
        Gateway -->|Verify JWT + Inject Headers| Stego[Stego Module]
        
        IAM -->|Auth & Policy| DB[(PostgreSQL)]
        IAM -.->|Fetch Keys| Gateway
    end

    style Gateway fill:#f9f,stroke:#333,stroke-width:2px
    style IAM fill:#bbf,stroke:#333,stroke-width:2px
    style Stego fill:#bfb,stroke:#333,stroke-width:2px
```

---

## 2. Project Architecture & Micro-Modular Design

The project utilizes a **Micro-Modular Monolith** pattern. While deployed as separate Web Archive (WAR) modules, they share a unified trust domain within the application server.

```text
/securegate-iam
├── infrastructure/      # Deployment configurations (Docker, Bash Scripts)
├── iam-service/         # [CORE] Identity Provider & Policy Engine
│   ├── src/main/java/com/securegate/iam/
│   │   ├── audit/          # Blockchain-like Hash Chaining Logs
│   │   ├── crypto/         # Argon2id & Ed25519 Implementations
│   │   ├── model/          # JPA Entities (User, Role, Policy)
│   │   ├── oauth/          # OAuth2 Authorization Code Flow Logic
│   │   └── service/        # Business Logic (PdpService, TokenService)
│   └── resources/       # Persistence.xml & Import.sql
├── api-gateway/         # [EDGE] Reverse Proxy & Security Filter
│   ├── src/main/java/com/securegate/api/
│   │   ├── filter/         # JwtAuthenticationFilter & XssSanitizationFilter
│   │   └── resources/      # Proxy endpoints (Internal Identity Propagation)
├── stego-module/        # [ADVANCED] Steganography Engine
│   ├── src/main/java/com/securegate/stego/
│   │   ├── EncryptionService.java  # ChaCha20-Poly1305 Wrapper
│   │   └── StcEngine.java          # Syndrome Trellis Codes (Matrix Embedding)
└── securegate-ui/       # [FRONTEND] Jakarta Faces PWA
    ├── src/main/webapp/    # XHTML Pages & Resources
    └── src/main/java/...   # Backing Beans (Controllers)
```

---

## 3. Core Service Deep Dive

### A. Identity Provider (iam-service)
**Implementation**: This module implements the OpenID Connect (OIDC) and OAuth2 protocols, serving as the central authority for user identity and access policies.
*   **User Repository**: Stores identities. Passwords are **never** stored in plain text. We utilize `Argon2id` (the winner of the Password Hashing Competition) with the following parameters:
    *   **Memory Cost**: 64 MB (Hardens against ASIC/GPU attacks)
    *   **Time Cost**: 3 Iterations
    *   **Parallelism**: 4 Threads
*   **Token Service**: Responsible for minting JSON Web Tokens (JWT).
    *   **Algorithm**: EdDSA (Ed25519)
    *   **Key Lifecycle**: Private keys are **ephemeral**, generated in-memory via `SecureRandom` on startup. This ensures **Forward Secrecy**—if the server is seized and powered down, the private key is irretrievably lost, rendering all prior tokens indecipherable to an attacker hoping to mint new ones.
*   **ABAC Policy Engine (PdpService)**:
    *   Implements **Attribute-Based Access Control**.
    *   Evaluates `Policy` entities against the tuples: `(User, Action, Resource, Context)`.
    *   **Logic**: `Permit = EXISTS(Policy p WHERE p.resource == req.resource AND p.action == req.action AND p.condition.matches(user))`.
    *   **Default Deny**: If no policy explicitly permits an action, access is denied.

**Security**: Secured by internal-only network access (via Gateway), rigorous input validations, and strict JPA parameterized queries to prevent SQL Injection.

### B. API Gateway (api-gateway)
**Implementation**: Acts as the intelligent Reverse Proxy and Security Enforcement Point. It does not contain business logic but enforces global security policies.
*   **Edge Authentication**: Intercepts every inbound HTTP request to validate headers and protocols.
*   **Dynamic Trust Federation**: On startup, it fetches the `jwks.json` (JSON Web Key Set) from the IAM Service to establish trust.
*   **Identity Propagation**:
    1.  Validates the incoming `Authorization: Bearer <JWT>` header.
    2.  Strips any client-injected `X-User-Principal` or `X-User-Roles` headers (Header Injection mitigation).
    3.  Injects its own verified `X-User-Principal` header before forwarding the request to internal microservices.
*   **Global Input Firewall (`XssSanitizationFilter`)**:
    *   Intercepts `POST`/`PUT` JSON payloads.
    *   Recursively parses the JSON tree.
    *   Sanitizes all String values using an allow-list strategy (escaping `<script>`, `<iframe>`, etc.) before rebuilding the stream.

**Security**: Secured by TLS 1.3 termination (via Nginx), Rate Limiting filters (Leaky Bucket algorithm), and strict CORS whitelist enforcement.

### C. Steganography Module (stego-module)
**Implementation**: Provides an **Overt Channel** for secure communication, hiding data in plain sight within media files.
*   **Encryption Layer**: Before embedding, all secrets are encrypted using `ChaCha20-Poly1305`.
    *   **ChaCha20**: A high-speed stream cipher, immune to timing attacks.
    *   **Poly1305**: Provides integrity (MAC). If the ciphertext is modified, decryption fails instantly.
*   **Embedding Layer (`StcEngine`)**:
    *   Uses **Matrix Embedding** principles to minimize the Hamming distance between the cover image and the stego-image.
    *   In the current implementation, we utilize a simplified LSB (Least Significant Bit) replacement strategy optimized for PNG lossless compression.

**Security**: Secured by Authenticated Encryption (AEAD) which ensures that any tampering with the image results in a decryption failure, preventing bit-flipping attacks. Keys are handled only in ephemeral memory.

### D. SecureGate UI (securegate-ui)
**Implementation**: A **Progressive Web App (PWA)** built on Jakarta Faces 4.0 that provides the user interface for Login, Registration, and Dashboard operations.
*   **Server-Side Rendering (SSR)**: Critical logic executes on the server, not the client, reducing the attack surface for XSS.
*   **CSRF Protection**: Native Jakarta Faces ViewState tokens prevent Cross-Site Request Forgery.
*   **State Management**: Complex flows (like MFA) leverage server-side caching (Redis-backed session replication in production) rather than client-side cookies.

**Security**: Secured by HTTP-Only/Secure cookies for session management and Content Security Policy (CSP) headers.

---

## 4. Security Vulnerability Remediations

### 1. The "Endianness" Cryptographic Mismatch
**Vulnerability**: Java's `BigInteger` implementation treats byte arrays as Big-Endian signed integers. However, **RFC 8032 (EdDSA)** mandates Little-Endian scalar encoding. Using standard libraries often results in invalid signatures when verifying against non-Java systems.
**Remediation**: In `TokenService.java`, we implemented a custom `KeyAdaptor` that manually reverses the byte order of the scalar parameters during the Ed25519 signing process, ensuring strict RFC compliance and interoperability.

### 2. Stateless MFA Password Exposure
**Vulnerability**: In the initial design, to maintain state between the "Login" page and the "MFA Verification" page, the user's password was hashed and sent back to the browser as a hidden HTML form field.
**Remediation**: We implemented a split-state flow:
1.  **Phase 1 (Login)**: Credentials are verified. If valid, a temporary, random `MFE_SESSION_ID` is generated and stored in a short-lived cache (Memory/Redis).
2.  **Phase 2 (MFA)**: The browser is redirected with the `MFE_SESSION_ID`. The password never leaves the secure server context.
3.  **Phase 3 (Finalize)**: Upon successful TOTP verification, the `MFE_SESSION_ID` is exchanged for the final JWT.

### 3. Hardcoded "Admin" Bypass
**Vulnerability**: The `PolicyEnforcementFilter` contained a debug line: `if (username.equals("admin")) return;`. This allowed any user named "admin" to bypass all ABAC checks.
**Remediation**: The line was removed. The "admin" user is now treated exactly like any other user, subject to the same `PdpService` policy evaluation. This enforces the Principle of Least Privilege.

### 4. CORS Misconfiguration & Localhost Leaks
**Vulnerability**: Hardcoded references to `http://localhost:8080` caused redirection loops in production and allowed arbitrary origins during development.
**Remediation**:
*   **Dynamic Configuration**: Implemented a `CorsFilter` that reads a `CORS_ALLOWED_ORIGINS` environment variable.
*   **Strict Whitelisting**: The filter checks the `Origin` header against the allowed list. If no match is found, the request is rejected (no `Access-Control-Allow-Origin` header is returned).

---

## 5. Software Bill of Materials (SBOM) - Security Libraries

Overview of key third-party security libraries and their roles:

| Library | Version | Purpose |
| :--- | :--- | :--- |
| **Nimbus JOSE + JWT** | 9.37.3 | Industry-standard implementation for JWT creation, signing (Ed25519), and validation. |
| **Google Tink** | 1.12.0 | Provides "BoringSSL" cryptographic primitives, ensuring correct usage of complex algorithms like ChaCha20-Poly1305. |
| **Bouncy Castle (bcprov)** | 1.77 | Cryptographic Provider used to backfill algorithms not present in the base JDK. |
| **GoogleAuth** | 1.5.0 | Implements RFC 6238 (TOTP) for Multi-Factor Authentication. |
| **Jedis** | 5.1.0 | High-performance Redis client for session caching and distributed locking. |
| **Jakarta Security** | 3.0 | Java EE standard for Authentication mechanisms and Identity Stores. |

---

## 6. Advanced Features: Tamper-Evident Auditing

We implemented a **Blockchain-Lite** audit trail in `AuditService.java`.

**Mechanism**:
1.  Every `AuditLog` entity has a `prev_hash` column.
2.  When a new log is created, its hash is calculated as:
    `SHA-256( Timestamp + Actor + Action + Resource + PREVIOUS_HASH )`
3.  **Tamper Evidence**: If a malicious administrator deletes a log entry from the database (e.g., using SQL), the hash chain is broken. The `prev_hash` of the subsequent record will no longer match the hash of the tampered record. This provides a mathematically verifiable history of events.

---

**SecureGate Architecture Group** | *Production Release 2.1 - Jan 2026*
