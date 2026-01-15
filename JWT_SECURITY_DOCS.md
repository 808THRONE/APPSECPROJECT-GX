# 🎫 JWT Token Specification & Security - SecureGate IAM

The **JSON Web Token (JWT)** is the primary vessel for identity and authorization within the SecureGate ecosystem. This document details the token's structure, cryptographic security, and how it enables **Role-Based Access Control (RBAC)**.

---

## 🏗️ Token Structure

SecureGate utilizes the **EdDSA (Ed25519)** algorithm for token signing. This is a modern, high-performance Edwards-curve signature scheme that provides superior security and speed compared to traditional RSA.

### 1. Header (JOSE Header)
The header identifies the algorithm and the specific ephemeral key used for signing.

```json
{
  "alg": "EdDSA",
  "typ": "JWT",
  "kid": "sg-ephemeral-uuid-..."
}
```
- **alg (Algorithm)**: Set to `EdDSA`, indicating the use of Ed25519.
- **kid (Key ID)**: A unique identifier allowing the API Gateway to fetch the correct public key from the IAM Service's JWKS endpoint.

### 2. Payload (Claims)
The payload contains the user's identity, **roles**, and session-specific metadata.

| Claim | Key | Description |
|:------|:----|:------------|
| **Subject** | `sub` | The unique, immutable UUID of the user. |
| **Issuer** | `iss` | The URI of the IAM Service (`https://iam.securegate.io`). |
| **Roles** | `roles` | **YES**. An array of strings (e.g., `["ADMIN", "USER"]`) used for RBAC/ABAC. |
| **Username** | `preferred_username` | The human-readable username for UI display. |
| **Audience** | `aud` | Targeted at `securegate-api`. |
| **Device ID** | `device_id` | Bound to the hardware/session that initiated the request. |
| **Issued At** | `iat` | Unix timestamp of when the token was created. |
| **Expiration** | `exp` | Token expiry (default: 900 seconds / 15 minutes). |
| **JWT ID** | `jti` | A unique nonce to prevent Replay Attacks. |

### 3. Signature
The signature is created using the IAM Service's **Private Ed25519 Key**. The API Gateway validates this using the **Public Ed25519 Key**.

---

## 🔐 Security Hardening Measures

### 1. Ed25519 Asymmetric Cryptography
- **Efficiency**: Ed25519 signatures are significantly smaller and faster to verify than RSA, reducing latency at the API Gateway.
- **Resilience**: It is immune to many common attacks that affect RSA (e.g., padding oracles) and is designed for side-channel resistance.

### 2. Ephemeral Key Lifecycle (Forward Secrecy)
- **In-Memory Storage**: The Ed25519 key pair is generated **dynamically** in the IAM Service's RAM on startup.
- **Auto-Revocation**: If the server is seized or rebooted, all existing tokens immediately become invalid because the signing key is lost forever.

### 3. Role-Based Access Control (RBAC)
By including the `roles` claim directly in the JWT, the **API Gateway** can make instant authorization decisions without performing a database lookup on every request. This ensures high performance for policy enforcement.

---

## 🚦 Validation Flow (API Gateway)

When a request arrives at the Gateway:
1.  **Extraction**: The filter pulls the token from the `Authorization: Bearer` header or cookie.
2.  **KID Lookup**: The Gateway checks the `kid` and fetches the Ed25519 Public Key if not cached.
3.  **Signature Verification**: The `Ed25519Verifier` validates the signature.
4.  **Claim Verification**: The Gateway checks `exp`, `aud`, and the `roles` required for the endpoint.
