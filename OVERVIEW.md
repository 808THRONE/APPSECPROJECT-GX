# 🛡️ SecureGate IAM: Enterprise Security Portal

**SecureGate** is a next-generation Identity and Access Management (IAM) system designed for high-security environments. It provides a robust, sovereign framework for managing user identities, fine-grained access policies, and secure data transmission using advanced steganography.

---

## 🎯 Purpose

In an era of increasing data breaches and sophisticated cyber-attacks, SecureGate serves as a defensive shield for enterprise resources. Its primary goals are:

1.  **Sovereign Identity**: Providing a centralized, internal identity provider that eliminates reliance on third-party services.
2.  **Zero-Trust Authorization**: Implementing deep policy enforcement (RBAC and ABAC) at every layer—from the Gateway to the Service.
3.  **Covert Data Transmission**: Utilizing steganography (STC Engine) to hide sensitive data within seemingly innocent carriers, protecting against traffic analysis and interception.
4.  **Audit Integrity**: Ensuring that all security events are logged with cryptographic chaining to prevent tampering.

---

## 🏗️ Architecture

SecureGate follow a modern microservices architecture, built on **Jakarta EE 10/11** and deployed on **WildFly**.

### 1. **IAM Service (The Core)**
The brain of the system.
- Handles OAuth2/OpenID Connect flows.
- Manages User accounts, Roles, and Policies via JPA/Hibernate.
- Provides Multi-Factor Authentication (TOTP/Google Authenticator).
- Issues and rotates cryptographically signed JWTs (RS256).

### 2. **API Gateway (The Shield)**
The single entry point for all API traffic.
- Strictly verifies JWT signatures.
- Performs real-time token revocation checks against a shared Redis store.
- Enforces Global Security Policies before requests reach internal services.

### 3. **Stego Module (The Ghost)**
A specialized service for data protection.
- Uses **Syndrome Trellis Codes (STC)** for optimal steganographic embedding.
- Encrypts payloads with **ChaCha20-Poly1305** before hiding them.
- Allows for the "invisible" transmission of tokens or secrets.

### 4. **SecureGate UI (The Management Hub)**
A high-performance portal built with **Jakarta Faces (JSF)** and **PrimeFaces**.
- Vibrant, dark-themed responsive dashboard.
- Real-time charts and usage statistics.
- Management consoles for Policies, Users, and MFA setup.

---

## 🚀 Core Features

- **OAuth2 + PKCE**: Modern, secure authentication flow for both web and mobile clients.
- **Dynamic MFA State Management**: Secure authentication state tracking that never exposes passwords to the frontend.
- **ABAC Policy Engine**: Define complex rules like "Deny access to PAYMENT service if user location is outside the corporate network."
- **Argon2id Hashing**: Industry-leading password protection.
- **JTI Blacklisting**: Instant, system-wide logout and token revocation via a global Redis store.
- **Audit Chaining**: Every log entry contains the hash of the previous one, creating an immutable audit trail.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Runtime** | WildFly 38+, Java 21 |
| **Backend** | Jakarta EE (CDI, JAX-RS, JPA), Hibernate |
| **Security** | Nimbus JOSE+JWT, Argon2, ChaCha20, TOTP |
| **Frontend** | Jakarta Faces (JSF), PrimeFaces (Arya Theme) |
| **Storage** | PostgreSQL (Production) / H2 (Development), Redis |
| **Steganography** | STC (Syndrome Trellis Codes) |

---

## 🔒 Security Posture

SecureGate is hardened against the OWASP Top 10:
- **Injection**: Secured through JPA parameterized queries.
- **Broken Auth**: Secured via secure session-based MFA and Argon2.
- **Sensitive Data Exposure**: Protected via Steganography and Poly1305 encryption.
- **Broken Access Control**: Enforced through a centralized Policy Enforcement Filter in the Gateway.
