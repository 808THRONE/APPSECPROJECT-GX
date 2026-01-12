<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# SecureGate – Identity \& Access Management (IAM) Portal

End‑to‑End Technical Architecture \& Implementation Blueprint

***

## 1. Functional Overview

### 1.1 System Purpose

SecureGate is an enterprise-grade IAM portal delivered as a Zero Trust, security-first Progressive Web Application (PWA). It provides:

- Centralized identity management for workforce, partners, and service accounts
- Strong, standards-based authentication (OAuth 2.1 + PKCE, MFA)
- Fine-grained authorization (hybrid RBAC + ABAC)
- Policy-based access control for applications, APIs, and data
- Full-lifecycle account management and governance
- High-assurance audit logging and compliance reporting

All components are locally deployable:

- PWA Frontend: `https://frontend.local`
- Core API: `https://api.local`
- IAM Service: `https://iam.local`
- Database Cluster: local network (e.g., `db01.local`, `db02.local`)


### 1.2 Primary Capabilities

1. **Enterprise Identity \& Access Management**
    - Central identity store (employees, contractors, service accounts)
    - Identity onboarding via HR feed / CSV / admin UI
    - Integration with corporate directory (e.g., LDAP/AD) via IAM service connector
    - Application registration \& client management (OAuth 2.1 clients)
2. **User Lifecycle Management**
    - **Provisioning**
        - Create account from HR or admin
        - Assign primary roles (e.g., Employee, Manager, Admin)
        - Enroll MFA
        - Issue initial credentials or delegated login link
    - **In-life changes**
        - Role and attribute changes based on HR updates (department, manager, location)
        - Temporary privilege elevation with time-bound policies
    - **De-provisioning**
        - Automatic disablement on HR termination
        - Immediate token revocation and session invalidation
        - Reassignment or revocation of owned resources
        - Audit trail of every lifecycle change
3. **Authentication**
    - OAuth 2.1 Authorization Code Flow + PKCE
    - TOTP-based MFA (RFC 6238) with QR-based enrollment
    - Login risk checks (device posture, geo, time-of-day attributes)
    - Strong password policy + breached password checks
    - Secure session management in PWA (no long-lived tokens in browser storage)
4. **Authorization**
    - Hybrid **RBAC** + **ABAC**:
        - RBAC for coarse-grained role permissions (Admin, Security Analyst, App Owner, User)
        - ABAC for contextual, fine-grained decisions (department, clearance, device_trust, geo, time, sensitivity)
    - Policy Decision Point (PDP) in IAM service
    - Policy Enforcement Points (PEPs) in:
        - API gateway layer (`api.local`)
        - Backend microservices
        - PWA (UI-level feature gating)
    - Enforcement at API, method, and data level
5. **Policy Enforcement \& Governance**
    - Central policy editor (e.g. JSON/YAML-based, XACML-inspired)
    - Policy versioning, approvals, and change history
    - Just-in-time access request and approval workflows
    - Entitlement review dashboards (who has access to what, why, and for how long)
6. **Audit Logging \& Compliance**
    - Tamper-evident audit log store with cryptographic chaining (hash-linked log records)
    - Detailed events:
        - Auth attempts (success/failure), MFA challenges, token issuance, revocation
        - Policy evaluations and decisions
        - Lifecycle changes, admin actions
    - Searchable timeline and export for SIEM
    - Data retention \& secure backup strategy
7. **Admin Dashboards**
    - Overview: login success/failure, MFA coverage, risk indicators
    - User and role management
    - Token \& session inventory
    - Policy effectiveness and denied access heatmaps
    - Compliance reports (dormant accounts, orphaned privileges)
8. **Secure Session Handling**
    - All browser <-> backend traffic over hardened TLS (1.2 / 1.3)
    - Short-lived JWT access tokens + opaque refresh tokens
    - Refresh token rotation with revocation on reuse
    - Optional token binding to device / key material
    - PWA uses in-memory storage for access tokens; HttpOnly, SameSite cookies for refresh tokens
9. **Device Trust \& Zero Trust Enforcement**
    - Device attributes included in access tokens and evaluated by PDP:
        - `device_trust_level`, `os`, `managed=true/false`, `last_attested`
    - Access decisions consider:
        - Identity, Role, Device trust, Network zone, Sensitivity, Time-of-day
    - Continuous authorization: critical operations require re-check and sometimes step-up MFA
    - No implicit trust based on network; internal and external traffic treated equally.

### 1.3 User Roles \& Enterprise Scenarios

**Roles**

- **End User** (Employee/Contractor)
- **Application Owner** (manages client registrations \& app policies)
- **Security Administrator** (global policies, audit)
- **Compliance Officer** (read-only access to logs \& reports)
- **System Administrator** (infrastructure config, not IAM decisions)

**Example Scenarios**

1. **New Employee Onboarding**
    - HR feed -> IAM sync job creates identity
    - Default roles assigned (Employee, Department role)
    - Enrollment email -> user logs into PWA -> forced password set + MFA enrollment
    - Access to department apps is granted via RBAC; context and device checks via ABAC
2. **Sensitive Transaction from Untrusted Device**
    - User attempts to approve large financial transaction from unmanaged laptop
    - PDP evaluates rule: “transactions over X require managed device AND MFA within last 5 minutes”
    - Policy denies or enforces step-up MFA; event is logged for audit.
3. **Admin Privilege Elevation**
    - Security Admin requests temporary `SecurityAdmin` role for incident
    - Approval workflow triggers; if approved, PDP adds ephemeral entitlement with expiry
    - All sensitive actions (policy changes, log exports) require fresh MFA
4. **Termination**
    - HR marks user as terminated
    - IAM job disables account, revokes tokens, invalidates sessions, deactivates MFA secrets
    - Logs and delegated ownership actions recorded

***

## 2. Architecture Overview

### 2.1 High-Level Component Model (Textual Diagram)

**Logical view (top‑down):**

- **Client Layer**
    - PWA (SecureGate UI) – `https://frontend.local`
        - Service Worker
        - Local cache (limited, non-sensitive)
        - In-memory token store
- **API Layer**
    - SecureGate Core API – `https://api.local`
        - Jakarta RESTful endpoints
        - Business services (User, Role, Policy, Audit, Reports)
        - PEP interceptors and filters
    - IAM Service – `https://iam.local`
        - Authorization Server (OAuth 2.1)
        - Token Service (JWT / opaque)
        - MFA Service
        - Policy Decision Point (RBAC + ABAC)
        - Device Trust Evaluator
- **Data \& Integration Layer**
    - RDBMS cluster (e.g., PostgreSQL) – encrypted volumes
        - Identity \& Accounts schema
        - Tokens \& Sessions
        - Policy store
        - Audit \& Events
        - Secrets \& Key metadata (but not cryptographic keys themselves)
    - Key Management (HSM or local KMS service)
    - HR / LDAP connectors (optional, local)

Arrows (simplified):

- Browser (PWA) → `api.local` (REST)
- PWA → `iam.local` (OAuth 2.1 auth endpoints)
- `api.local` → `iam.local` (introspection/policy/tokens via mTLS)
- Both `api.local` and `iam.local` → DB + KMS


### 2.2 Frontend (PWA) Design

- Deployed at: `https://frontend.local`
- Tech stack (example):
    - HTML5, CSS3, TypeScript, Lit/React, Web Components
    - Service worker: `service-worker.js` on same origin
- Security characteristics:
    - All resources loaded over HTTPS
    - Strict CSP + Subresource Integrity (SRI)
    - Service worker scope minimal: e.g. `/app/` only
    - No inline JS; use hashes/nonces where inline unavoidable
    - Trusted Types to mitigate DOM XSS
- Offline mode:
    - Limited offline: cached shell and read-only views of *non-sensitive* content
    - No caching of access tokens, secrets, or sensitive user data
    - Offline actions are queued locally (encrypted with XChaCha20-Poly1305) and replayed on reconnect after re-auth


### 2.3 Backend API (`api.local`)

- Jakarta EE 11 on WildFly 38.0.1.Final
- Stateless, REST-first design:
    - JAX-RS (`jakarta.ws.rs`) resources
    - Stateless EJB / CDI services
- Security Integration:
    - Jakarta Security for authn integration
    - Custom JAX-RS filters as PEPs:
        - Verify JWT access token (signature, issuer, audience, expiry, etc.)
        - Call `iam.local` PDP when necessary
    - Method-level authorization via annotations (e.g., `@RolesAllowed`, custom `@RequiresPolicy`)
- Token-based authentication:
    - Bearer JWT access tokens, short-lived (~5–15 min)
    - Opaque refresh tokens stored in DB, bound to device \& client
- Example major resource modules:
    - `/users`, `/roles`, `/policies`, `/sessions`, `/audit`, `/apps`


### 2.4 IAM Service (`iam.local`)

- Also Jakarta EE 11 on WildFly 38.0.1.Final, but separated deployment
- Responsibilities:
    - OAuth 2.1 Authorization Server:
        - `/oauth2/authorize`, `/oauth2/token`, `/oauth2/revoke`, `/oauth2/introspect`
    - MFA service:
        - TOTP registration \& verification
        - Backup codes management
    - Token service:
        - JWT access token signing with Ed25519 or ECDSA
        - Opaque refresh tokens
    - PDP:
        - Evaluate RBAC + ABAC policies
        - Provide decisions to `api.local` and other clients
    - Identity \& session management APIs


### 2.5 Data Layer

- **Database**
    - RDBMS (e.g., PostgreSQL) on `db01.local` / `db02.local`
    - Data-at-rest encryption:
        - Full-disk / tablespace encryption at storage level
        - Additional column-level crypto for extremely sensitive fields (secrets, MFA seeds)
    - Strict schema separation:
        - `iam_identity`
        - `iam_policy`
        - `iam_audit`
        - `iam_tokens`
        - `iam_stego` (for dissimulation payloads)
- **Key Management**
    - Local HSM / KMS instance (e.g., `kms.local`)
    - API used over mTLS
    - Keys:
        - TLS keys
        - JWT signing keys
        - Data encryption keys (DEKs)
        - STC-related secret keys (for embedded control data)
- **Backup**
    - Encrypted, integrity-checked backups
    - Cryptographic sealing of backup chains (hash of backup + previous hash)
    - Periodic restore tests

***

## 3. Security Model – Zero Trust Architecture

### 3.1 Core Principles

- Never trust, always verify
- Assume breach
- Enforce least privilege and continuous verification
- Context-aware access decisions (identity + device + environment + resource)


### 3.2 Authentication \& Authorization

#### 3.2.1 OAuth 2.1 Flows

- **Authorization Code + PKCE (mandatory)** for browser → `iam.local`:
    - PWA generates:
        - `code_verifier` (high entropy, >= 128 bits, secure random)
        - `code_challenge = base64url(SHA-256(code_verifier))`
    - Authorization request:
        - `response_type=code`
        - `client_id=securegate-frontend`
        - `redirect_uri=https://frontend.local/callback`
        - `scope=openid profile email securegate.api`
        - `state` (CSRF protection)
        - `code_challenge`, `code_challenge_method=S256`
    - Token request:
        - `grant_type=authorization_code`
        - Authorization `code`
        - `code_verifier`
        - Verified using TLS and strict redirect URI matching
- **Short-lived access tokens**
    - Lifetime: 5–15 minutes
    - Audience: `api.local`
    - Scope-based privileges
    - JTI (unique ID) to support revocation
- **Refresh Token Rotation**
    - Opaque refresh token stored server-side with:
        - user_id, client_id, device_id, IP, user agent, created_at, last_used_at, previous_token_id
    - On every refresh:
        - New refresh token issued, old one invalidated
        - If an old token is reused → immediate revocation of chain and session
- **Token Binding**
    - Token metadata includes device-bound claims:
        - `device_id` (pseudonymous ID)
        - `public_key_fingerprint` of device key (if available)
    - On use, `api.local` and `iam.local` verify:
        - Token’s device matching the incoming client context
    - For browser-based PWA, token binding can be done via:
        - TLS client certs (WebPKI, enterprise only), or
        - Signed proof-of-possession tokens (where supported)
- **Mutual Trust Validation**
    - All internal service-to-service calls (`api.local ↔ iam.local`) use mTLS
    - Client certs with restricted SANs \& EKU for service authentication


#### 3.2.2 Multi-Factor Authentication (MFA)

- **TOTP-based 2FA (RFC 6238)**
    - Secret generation:
        - 160-bit random secret from CSPRNG
        - Encoded as Base32
    - QR enrollment:
        - `otpauth://totp/SecureGate:username?secret=...&issuer=SecureGate&digits=6&period=30`
    - Verification:
        - Accept codes within ±1 time step (configurable)
        - Rate limiting per user per IP
    - Secrets stored encrypted at rest with per-tenant DEKs
- **Backup Codes**
    - Per user, 8–10 single-use codes
    - 10–12 characters, high entropy
    - Hashed using Argon2id or scrypt, not stored in plaintext
    - UI for regeneration and revocation
- **Anti-replay**
    - Store last used TOTP code and time interval per user
    - Reject repeated use of identical code in same or adjacent window
    - Token binding prevents replay of tokens across devices


#### 3.2.3 Access Control

- **RBAC**
    - Roles:
        - `ROLE_USER`, `ROLE_APP_OWNER`, `ROLE_SECURITY_ADMIN`, `ROLE_AUDITOR`, etc.
    - Role assignments stored in `iam_identity.user_roles`
    - Used for coarse grants (module visibility, baseline APIs)
- **ABAC**
    - Attributes:
        - User: dept, job_title, clearance, employment_status
        - Device: device_trust_level, managed, platform, last_attested
        - Environment: ip_geo_region, network_zone, time_of_day, risk_score
        - Resource: data_sensitivity, asset_owner, regulatory_tag (GDPR/PCI)
    - Policies example (pseudo):

```json
{
  "policyId": "approve-payment",
  "effect": "PERMIT",
  "target": {
    "action": "PAYMENT_APPROVE",
    "resourceSensitivity": "HIGH"
  },
  "condition": {
    "allOf": [
      "user.role == 'FINANCE_MANAGER'",
      "user.clearance >= 3",
      "device.trust_level == 'HIGH'",
      "env.timeOfDay in ['BUSINESS_HOURS']",
      "env.riskScore < 70"
    ]
  }
}
```

- **Policy Evaluation Engine**
    - PDP:
        - Evaluates policies at request time
        - Combines RBAC (role check) + ABAC (attribute conditions)
    - PIPs (policy information points):
        - Device posture source
        - GeoIP / threat intel
        - User directory
    - PEP:
        - In `api.local` JAX-RS filters:
            - Extract token claims
            - Query PDP with action, resource, and attributes
            - Deny or allow with reasons (for audit)
- **Fine-Grained Levels**
    - **API level:** Which endpoints can be called (`/users`, `/policies`, `/payments`)
    - **Method level:** HTTP verbs + operations (`POST /users` vs `GET /users/{id}`)
    - **Data level:** Row/column filters:
        - PDP returns obligations such as:
            - Allowed attributes to return
            - Row filters (e.g., `dept == user.dept`)

***

## 4. Cryptography \& Data Protection

### 4.1 Communication Security

- **TLS**
    - Protocols: TLS 1.2 / 1.3 only
    - Cipher suites (example for TLS 1.3):
        - `TLS_AES_256_GCM_SHA384`
        - `TLS_CHACHA20_POLY1305_SHA256`
    - WildFly Elytron configuration:
        - `server-ssl-context` with restricted `protocols="TLSv1.3 TLSv1.2"`
        - `enabled-cipher-suites` limited to strong, forward-secret options
    - mTLS between `api.local`, `iam.local`, DB, KMS
- **Application-Layer AEAD (XChaCha20-Poly1305)**
    - For **sensitive fields** and offline payloads:
        - Use libsodium / Tink via Java bindings (e.g. through JNI or Tink)
        - Nonce: 192-bit random (24 bytes), never reused per key
        - Key: 256-bit
        - Associated Data: context (e.g., userId, type, version, timestamp)
    - Example usage:
        - Encrypt offline operation queues in PWA
        - Encrypt high-sensitivity DB columns (MFA seeds, backup codes pre-hash, STC payloads)
    - Forward secrecy:
        - Achieved at TLS layer (ECDHE)
        - Application-level key rotation for DEKs, with master key in KMS


### 4.2 Data Protection at Rest

- **Encryption at Rest**
    - Storage-level (disk, volume, or tablespace)
    - Column-level encryption for:
        - MFA secrets
        - Backup codes seed
        - Private keys or secrets (when not offloaded to HSM)
    - Envelope encryption:
        - Data encrypted with DEK
        - DEK wrapped by KEK from KMS
- **Password Hashing**
    - Argon2id (preferred) or scrypt:
        - High memory cost, configurable iterations
        - Unique salt per password
    - Avoid PBKDF2 unless constrained
- **Secure Random**
    - `SecureRandom.getInstanceStrong()` or `SecureRandom` with strong algorithm (e.g. `NativePRNGNonBlocking`)
    - Use only for secrets, tokens, codes, and STC keys
- **Secrets Storage**
    - No secrets in source control
    - Application properties referencing environment or KMS
    - Secrets:
        - JWT signing keys
        - TOTP seeds
        - DB credentials
        - STC stego keys
- **Anti-Tampering**
    - Hash-chained audit logs:
        - Each log record includes `prev_hash` and `current_hash`
    - Signed configuration:
        - Config bundles signed and verified on startup
    - Binary integrity:
        - Optionally sign WAR files and verify signatures during deployment

***

## 5. Data Dissimulation via STC (Advanced)

### 5.1 Concept and Use in SecureGate

SecureGate uses **Syndrome Trellis Codes (STC)** à la Filler–Fridrich to embed certain security metadata in **innocuous-looking internal artifacts** (e.g., control images, padded log blobs). This is *not* a user-facing steganography feature; it is used to:

- Protect integrity of high-value audit trails and policies against insider tampering
- Hide “canary” markers or watermarking signals that indicate tampering
- Provide a covert verification channel for compliance auditors


### 5.2 Where STC is Applied

1. **Audit Log Watermarking**
    - At periodic intervals, an image or binary blob representing a “checkpoint” is generated and stored alongside logs.
    - STC embeds:
        - Hash chain root
        - Time stamping authority (TSA) data
        - Integrity verification bits
    - The cover medium can be:
        - PNG/JPEG image used in admin dashboard
        - Synthetic noise data stored in `iam_stego.stc_blobs`
2. **Policy Integrity Tokens**
    - Policy bundles (JSON) include a reference to a stego object containing:
        - Policy version ID
        - Critical checksum
    - Extraction allows verifying that the policy set loaded into PDP is legitimately issued.

### 5.3 Threat Model \& Justification

- **Threats Mitigated**
    - Malicious insider with DB access trying to:
        - Manipulate audit logs, remove incriminating events
        - Modify policy records while covering tracks
    - Attacker who gains DB dumps and attempts to repudiate actions
- **Why STC**
    - Near-optimal embedding with minimal cover distortion
    - Harder to detect than explicit out-of-band integrity markers
    - Adds a stealth integrity “tripwire” on top of explicit cryptographic protections


### 5.4 Security Guarantees

- **Embedding**
    - Using multi-layer STC (per Filler–Fridrich):
        - Minimizes additive distortion in cover objects
        - Achieves close-to-theoretical payload efficiency
    - Universal: same code works across different distortion profiles
- **Extraction**
    - Requires knowledge of:
        - STC parity-check matrix
        - Secret keys / embedding profile
    - Without them, detecting or modifying embedded payload is hard.
- **Limits**
    - STC does *not* replace cryptographic integrity:
        - Hash-chaining and digital signatures remain primary
    - STC acts as **defense in depth** and steganographic tamper-evidence layer


### 5.5 Performance Considerations

- Embedding and extraction are CPU-intensive but:
    - Only applied on periodic checkpoints, not per-request
    - Executed asynchronously (background jobs)
- Implementation detail:
    - Use native library (C++ STC toolbox) via JNI or a service wrapper
    - Precompute and cache STC structures in long-lived JVMs

***

## 6. Vulnerability \& CVE Hardening

### 6.1 General OWASP \& Zero Trust Risks

- OWASP Top 10:
    - A01: Broken Access Control → Use PDP / PEP pattern, least privilege
    - A02: Cryptographic Failures → Standardized libs, key mgmt, TLS hardening
    - A03: Injection → Strict parameter binding, prepared statements, validation
    - A04: Insecure Design → Zero Trust by design, policy-first
    - A05: Security Misconfiguration → Harden WildFly, containers, PWA headers
    - A06: Vulnerable Components → SBOM, regular scanning
    - A07: Identification \& Auth Failures → OAuth 2.1, MFA, token revocation
    - A08: Software \& Data Integrity Failures → Signed deploys, hash-chained logs
    - A09: Security Logging \& Monitoring Failures → Centralized logs, alerts
    - A10: SSRF → Egress controls, safe HTTP clients


### 6.2 PWA-Specific Risks \& Mitigations

- **Service Worker Hijacking / Shadow Workers**
    - Mitigation:
        - Register only from `https://frontend.local`
        - Pin exact path `/app/service-worker.js`
        - CSP disallow remote scripts
        - Regularly validate SW hash (SRI-like validation)
        - Unregister old workers on version mismatches
- **XSS leading to SW abuse**
    - Strict CSP, Trusted Types, DOM sanitization
    - No inline JS, avoid `eval`/`new Function`
- **Cache Poisoning**
    - Whitelist assets for caching
    - Avoid caching API responses with sensitive data
    - Versioned cache keys and periodic purge


### 6.3 Jakarta EE / WildFly-Specific Hardening

- Consider historical CVEs:
    - Deserialization RCE
    - JNDI Injection
    - Weak default Elytron / Undertow configs
- Mitigations:
    - Disable Java serialization for remote interfaces
    - Avoid Java object serialization in REST (use JSON only)
    - Whitelist JNDI contexts, or disable where not needed
    - Configure Undertow:
        - Disable HTTP TRACE
        - Set strict timeouts and size limits
    - Elytron:
        - Disable weak ciphers and protocols
    - Keep WildFly 38 patched with security updates


### 6.4 API \& Application Risks

- **CSRF**
    - OAuth flows protected with `state` parameter
    - Core APIs require Bearer tokens and SameSite=Lax/Strict cookies
- **XSS**
    - See PWA measures; server-side HTML uses contextual encoding
- **SSRF**
    - Outbound HTTP clients:
        - Deny internal address ranges by default
        - Use explicit allow-lists
- **RCE**
    - No dynamic compilation of untrusted code
    - No user-controlled templates
- **Deserialization**
    - Only JSON via Jackson/Jakarta JSON-B with hardened configuration
    - Disable polymorphic deserialization by default


### 6.5 Secure Headers \& Configurations

On `frontend.local` and `api.local` responses:

- `Content-Security-Policy`
    - Example (simplified):

```
default-src 'self';
script-src 'self';
style-src 'self' 'unsafe-inline';
img-src 'self' data:;
connect-src 'self' https://api.local https://iam.local;
frame-ancestors 'none';
base-uri 'self';
object-src 'none';
```

- `Strict-Transport-Security: max-age=31536000; includeSubDomains`
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: no-referrer`
- `Permissions-Policy` limiting sensors, camera, etc.
- `Cache-Control` tuned per endpoint

***

## 7. API Design

### 7.1 REST Principles

- Resource-oriented endpoints:
    - `/v1/users`, `/v1/roles`, `/v1/policies`, `/v1/tokens`, `/v1/audit`
- JSON payloads, HAL/JSON:API style optional
- Use proper HTTP verbs and status codes


### 7.2 Versioning Strategy

- URI versioning:
    - `/api/v1/...`
- Major breaking changes → new version; old versions deprecated but kept read-only for a period
- Version negotiation via `Accept` header optional


### 7.3 Request Validation

- Bean Validation annotations on DTOs (`jakarta.validation`)
- Custom validators for:
    - Password complexity
    - Role names / reserved names
    - Policy structure (JSON schema validation)
- Reject unknown fields (strict JSON parsing)


### 7.4 Rate Limiting \& Anti-Automation

- IP + user + client rate limiting:
    - e.g. `50 requests/min` baseline; stricter for auth endpoints
- CAPTCHA / proof-of-work on suspicious login behavior
- Exponential backoff on repeated failed logins / MFA attempts


### 7.5 Secure Error Handling

- Never leak internal details:
    - Generic `401 Unauthorized` / `403 Forbidden` where appropriate
    - Validation errors summarized, but no stack traces
- Mask resource existence:
    - For example, user enumeration prevented by same error message for “user not found” vs “wrong password”
- Central exception mapper:
    - Maps exceptions to safe HTTP responses

***

## 8. Implementation Constraints

- **Language**: Java (JDK 21)
- **Platform**: Jakarta EE 11
- **Server**: WildFly 38.0.1.Final (Preview – Jakarta EE 11)
- **Deployment**:
    - Local: `frontend.local`, `api.local`, `iam.local`
    - No cloud dependencies; can run entirely in a local network
- **Libraries (examples)**
    - Security:
        - Tink / libsodium binding for XChaCha20-Poly1305
        - Bouncy Castle for EdDSA/ECDSA, Argon2id (if needed)
    - OAuth / JWT:
        - `nimbus-jose-jwt` or similar for JWT handling
    - PWA:
        - Workbox for service worker caching (with strict configuration)

***

## 9. Flows, Threat Models, and Checklists

### 9.1 Authentication Flow (Simplified)

1. User opens `https://frontend.local/app`
2. PWA checks for existing valid access token in memory:
    - If absent/expired, redirect to `https://iam.local/oauth2/authorize` with PKCE
3. User authenticates with username/password
4. If required, system prompts for TOTP
5. IAM issues authorization code -> redirects back to PWA with `code` + `state`
6. PWA sends `code` + `code_verifier` to `/oauth2/token`
7. IAM verifies PKCE, issues short-lived access token + refresh token (in HttpOnly cookie)
8. PWA uses access token in `Authorization: Bearer ...` to call `https://api.local/v1/...`

Threats mitigated:

- Code interception: PKCE (S256)
- CSRF: `state` parameter
- Token theft: short lifetimes, rotation, token binding, HttpOnly cookies


### 9.2 Cryptographic Flow (XChaCha20-Poly1305 Example)

1. PWA wants to store offline queue:
    - Gets per-user AEAD key derived from server-provided key (HKDF)
2. Generates 24-byte random nonce
3. Encrypts payload with AEAD:
    - `ciphertext = AEAD_Encrypt(key, nonce, plaintext, AAD)`
4. Stores `{nonce, ciphertext}` in IndexedDB or other storage
5. On reconnect:
    - PWA re-authenticates, obtains valid token
    - Decrypts payload, replays to `api.local`
    - Deletes local offline data

### 9.3 Threat Models (High Level)

- **External Attacker**
    - Attempts XSS, CSRF, credential stuffing, token theft
    - Mitigations:
        - CSP, rate limiting, MFA, lockout, short tokens, PKCE, TLS, CSP, cookie flags
- **Compromised Client / Device**
    - Browser malware, stolen device
    - Mitigations:
        - Token binding, continuous risk assessment, forced re-auth for high-risk contexts
- **Malicious Insider (DB / Admin)**
    - Direct DB tampering, log manipulation
    - Mitigations:
        - Encryption at rest, limited DB privileges, hash-chained logs, STC stego watermarks
- **Service Worker Abuse**
    - Attacker tries to install malicious SW or exploit offline caching
    - Mitigations:
        - HTTPS-only, restricted SW scope, SW hash verification, no sensitive data in cache


### 9.4 Hardening Checklist (Abbreviated)

**Frontend PWA**

- [ ] Strict CSP without `unsafe-inline` for scripts
- [ ] Service worker registered only from secured path
- [ ] Cache only static, non-sensitive resources
- [ ] Input validation \& output encoding on all dynamic content
- [ ] Tokens stored in memory only; refresh in HttpOnly cookies

**API (`api.local`)**

- [ ] Global auth filter verifying JWT
- [ ] PEP integrated with PDP
- [ ] All DB access via prepared statements / ORM
- [ ] Granular RBAC/ABAC enforcement on endpoints \& data
- [ ] Logged security events with correlation IDs

**IAM (`iam.local`)**

- [ ] OAuth 2.1 Authorization Code + PKCE only
- [ ] MFA enforced for sensitive scopes
- [ ] Refresh token rotation, revocation on reuse
- [ ] Strong password hashing, lockout policies
- [ ] Policy editor with approval workflow \& audit

**Data \& Crypto**

- [ ] All sensitive data encrypted at rest
- [ ] DEK rotation policy documented and implemented
- [ ] TLS 1.2/1.3 with strong ciphers, mTLS for internal calls
- [ ] Hash-chained, tamper-evident logs + STC watermarks
- [ ] Regular key rotation \& backup verification

***

This architecture is directly implementable on Jakarta EE 11 with WildFly 38, and aligned with modern Zero Trust, OAuth 2.1, cryptographic, and PWA security practices.
<span style="display:none">[^1][^10][^11][^12][^13][^14][^15][^16][^17][^18][^19][^2][^20][^21][^22][^23][^24][^25][^26][^27][^28][^29][^3][^30][^4][^5][^6][^7][^8][^9]</span>

<div align="center">⁂</div>

[^1]: https://prefactor.tech/blog/pkce-oauth-ai-agents-best-practices

[^2]: https://groups.google.com/g/wildfly/c/B4okXDQAyAU

[^3]: https://www.zentera.net/cybersecurity/role-based-access-control-rbac-zero-trust-guide

[^4]: https://aembit.io/blog/mcp-oauth-2-1-pkce-and-the-future-of-ai-authorization/

[^5]: https://www.reddit.com/r/java/comments/viblqk/securing_jakarta_enterprise_beans_with_mutual_tls/

[^6]: https://www.kiteworks.com/risk-compliance-glossary/attribute-based-access-control/

[^7]: https://dev.to/kimmaida/oauth-20-security-best-practices-for-developers-2ba5

[^8]: https://stackoverflow.com/questions/28430149/how-to-enable-certain-cipher-suites-in-wildfly

[^9]: https://www.cyber.gc.ca/en/guidance/zero-trust-approach-security-architecture-itsm10008

[^10]: https://www.linkedin.com/posts/abhaybhargav_ive-reviewed-dozens-of-oauth-implementations-activity-7372285873234694144-iieM

[^11]: https://docs.redhat.com/en/documentation/red_hat_jboss_enterprise_application_platform/7.4/html-single/how_to_configure_server_security/index

[^12]: https://identitymanagementinstitute.org/zero-trust-and-fine-grained-abac/

[^13]: https://jakarta.ee/learn/specification-guides/security-authorization-and-authentication-explained/

[^14]: http://docs.wildfly.org/38/Client_Guide.html

[^15]: https://www.barracuda.com/support/glossary/rbac-vs-abac

[^16]: http://dde.binghamton.edu/filler/pdf/Fill10tifs-stc.pdf

[^17]: https://stackoverflow.com/questions/77776618/decryption-function-for-xchacha20-poly1305

[^18]: https://blog.nviso.eu/2020/01/16/deep-dive-into-the-security-of-progressive-web-apps/

[^19]: https://www.ijcsit.com/docs/Volume 6/vol6issue05/ijcsit20150605122.pdf

[^20]: https://mojoauth.com/hashing/poly1305-aes-in-java/

[^21]: https://www.zeepalm.com/blog/service-worker-security-best-practices-2024-guide

[^22]: http://dde.binghamton.edu/download/syndrome/

[^23]: https://developers.google.com/tink/aead

[^24]: https://blog.pixelfreestudio.com/best-practices-for-pwa-security/

[^25]: https://github.com/daniellerch/pySTC

[^26]: https://mkyong.com/java/java-11-chacha20-poly1305-encryption-examples/

[^27]: https://tangiblebytes.co.uk/2021/pwa-workbox-csp-caching/

[^28]: https://library.imaging.org/ei/articles/31/5/art00014

[^29]: https://github.com/ConsenSys/cava/blob/master/crypto/src/main/java/net/consensys/cava/crypto/sodium/XChaCha20Poly1305.java

[^30]: https://samsunginternet.github.io/pwa-series-service-workers-the-basics-about-offline/

