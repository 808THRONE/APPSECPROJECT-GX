# 🖼️ Steganography Module Documentation - SecureGate IAM

The **Stego Module** (`stego-module.war`) provides an **Overt Channel** for ultra-secure communication. It allows sensitive data to be hidden inside media files, making the transmission invisible to casual observers and automated traffic Analyzers.

---

## 🏗️ Core Responsibilities

The Stego Module handles:
1.  **Authenticated Encryption**: Encrypting data before embedding.
2.  **Matrix Embedding (STC)**: Hiding data within cover media with high efficiency.
3.  **Adaptive Security**: Modifying embedding rates based on content complexity.
4.  **Payload Extraction**: Recovering hidden data from valid stego-carriers.

---

## 📂 File-to-Feature Mapping

| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[StcEngine.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/stego-module/src/main/java/com/securegate/stego/StcEngine.java)** | Steganography | Implements **Syndrome Trellis Codes (STC)** for efficient data embedding. |
| **[EncryptionService.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/stego-module/src/main/java/com/securegate/stego/EncryptionService.java)** | Pre-Processing | Uses **ChaCha20-Poly1305** for authenticated encryption of payloads. |
| **[StegoResource.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/stego-module/src/main/java/com/securegate/stego/resources/StegoResource.java)** | REST API | Provides `/embed` and `/extract` endpoints for external integration. |
| **[StegoApplication.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/stego-module/src/main/java/com/securegate/stego/StegoApplication.java)** | Entry Point | Jakarta REST configuration for the module. |
| **[StegoProducers.java](file:///c:/Users/808th/OneDrive/Desktop/A%20try/stego-module/src/main/java/com/securegate/stego/StegoProducers.java)** | Dependency Injection | CDI producers for internal stego-engine components. |

---

## 🔐 Security & Algorithms

### 1. Authenticated Encryption (AEAD)
- **Algorithm**: ChaCha20-Poly1305.
- **Why**: It is a modern, high-speed cipher that provides both confidentiality and integrity. Unlike AES-GCM, it is not vulnerable to nonce-misuse and has no complex hardware requirements.

### 2. Syndrome Trellis Codes (STC)
- **Concept**: STC is a state-of-the-art framework for minimizing embedding distortion. 
- **Efficiency**: It allows for a high payload capacity while keeping the statistical changes to the carrier minimal, thwarting most steganalysis tools.

---

## 🚦 Integration with API Gateway

The **API Gateway** automatically utilizes this module for sensitive responses. 
1.  Gateway receives a JSON response from IAM.
2.  Gateway calls the Stego Module to `/embed` the JSON.
3.  The final result sent to the user is a `stego` JSON object containing the masked carrier data.

---

## 🚀 Build & Deployment
- **Packaging**: `mvn clean package -pl stego-module`
- **Environment**: Requires `STEGO_ENCRYPTION_KEY` for payload protection.
