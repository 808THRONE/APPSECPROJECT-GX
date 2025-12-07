# SecureGate IAM Portal

**Suggested Implementation Based on Technical Specification**

> **Note**: This is a proposed implementation of the SecureGate IAM Portal based on the provided technical specification and notes taken from class. The architecture, technology choices, and implementation details are suggestions that can be modified to fit your specific requirements.

**To Bypass login use this : window.useStore.getState().setUser({ name: 'Test Admin', email: 'admin@securegate.com' });**
 

## Project Overview

A comprehensive identity and access management system featuring:

- **Zero Trust Security** - Continuous verification with no implicit trust
- **OAuth 2.1 + PASETO v4** - Modern authentication with algorithm confusion protection
- **ABAC Policy Engine** - Attribute-Based Access Control with visual policy builder
- **AES-256-GCM + I forgot which dissimulation algorithm ** - Covert data transmission
- **Progressive Web App** - Offline-first with service worker
- **Multi-Factor Authentication** - TOTP-based 2FA with QR enrollment
- **Real-time Audit Logging** - WebSocket-based security event streaming

---

## System Architecture

### Three-Tier Domain Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    Internet / Users                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
            ┌──────────────────────┐
            │   Traefik (TLS 1.3)  │
            │   + ModSecurity WAF   │
            └──────────┬───────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
┌────────────┐  ┌────────────┐  ┌────────────┐
│    www     │  │    iam     │  │    api     │
│ (Frontend) │  │  (OAuth)   │  │ (Backend)  │
│  Lit PWA   │  │  WildFly   │  │  WildFly   │
└────────────┘  └────────────┘  └─────┬──────┘
                       │               │
                       │      mTLS     │
                       └───────────────┘
                              │
                    ┌─────────┼─────────┐
                    │         │         │
                    ▼         ▼         ▼
            ┌──────────┐ ┌──────┐ ┌──────┐
            │PostgreSQL│ │Redis │ │MinIO │
            │   16.1   │ │ 7.2  │ │ S3   │
            └──────────┘ └──────┘ └──────┘
```

### Technology Stack

#### Frontend (`www.yourdomain.me`)
- **Framework**: Lit 3.x (Web Components)
- **State**: Zustand 4.x + Encrypted IndexedDB
- **PWA**: Workbox 7.x (offline-first)
- **Real-time**: WebSocket + Auto-reconnect
- **Security**: DOMPurify, SRI, CSP Level 3

#### Backend API (`api.yourdomain.me`)
- **Server**: WildFly 38.0.1.Final (Jakarta EE 11)
- **REST**: JAX-RS 4.0 + JSON-B 3.0
- **WebSocket**: Jakarta WebSocket 2.2
- **ABAC**: Custom Elytron Security Realm
- **Messaging**: Apache Artemis 2.42.0

#### IAM Service (`iam.yourdomain.me`)
- **OAuth 2.1**: WildFly Elytron + PKCE
- **Tokens**: PASETO v4 (internal) + JWT (OAuth)
- **2FA**: TOTP with QR codes (ZXing)
- **Sessions**: Redis Cluster + Sentinel

#### Data Layer
- **Database**: PostgreSQL 16.1 (RLS, JSONB, pgcrypto)
- **Cache**: Redis 7.2 Cluster (6 nodes)
- **Storage**: MinIO (steganography cover images)

---

##  Project Structure

```
APPSECPROJECT-GX/
├── README.md                          # This file
├── package.json                       # Frontend dependencies
├── pwa-frontend/                      # Progressive Web App
│   ├── src/
│   │   ├── index.html                 # Entry point with CSP
│   │   ├── app.js                     # Main application
│   │   ├── service-worker.js          # Workbox PWA service worker
│   │   ├── manifest.json              # PWA manifest
│   │   ├── styles/
│   │   │   ├── design-system.css      # Design tokens & variables
│   │   │   └── global.css             # Global styles
│   │   ├── components/
│   │   │   ├── login-component.js     # Login with OAuth + 2FA
│   │   │   ├── dashboard-component.js # Main dashboard
│   │   │   ├── policy-builder.js      # ABAC visual builder
│   │   │   ├── audit-viewer.js        # Real-time logs
│   │   │   ├── profile-component.js   # User profile & 2FA
│   │   │   └── common/
│   │   │       ├── button.js          # Reusable button
│   │   │       ├── card.js            # Card component
│   │   │       └── modal.js           # Modal dialog
│   │   ├── store/
│   │   │   └── store.js               # Zustand state management
│   │   ├── security/
│   │   │   ├── device-fingerprint.js  # Browser fingerprinting
│   │   │   ├── sanitizer.js           # DOMPurify wrapper
│   │   │   └── crypto-storage.js      # Encrypted IndexedDB
│   │   ├── websocket/
│   │   │   └── websocket-manager.js   # WebSocket + reconnect
│   │   └── utils/
│   │       ├── oauth-client.js        # OAuth 2.1 PKCE client
│   │       └── api-client.js          # Fetch wrapper
│   └── public/
│       ├── icons/                     # PWA icons
│       └── images/                    # Static assets
│
├── iam-service/                       # OAuth 2.1 + PASETO (Future)
│   ├── pom.xml
│   ├── wildfly-config/
│   │   └── standalone.xml
│   └── src/main/java/com/securegate/
│       ├── oauth/
│       ├── tokens/
│       ├── mfa/
│       └── session/
│
├── api-gateway/                       # REST API + ABAC (Future)
│   ├── pom.xml
│   └── src/main/java/com/securegate/
│       ├── api/
│       ├── abac/
│       └── audit/
│
├── stego-module/                      # Steganography (Future)
│   ├── pom.xml
│   └── src/main/java/com/securegate/
│       └── stego/
│
├── infrastructure/                    # Docker Compose setup (Future)
│   ├── docker-compose.yml
│   ├── traefik.yml
│   ├── postgres-init.sql
│   └── redis.conf
│
├── monitoring/                        # Prometheus + Grafana (Future)
│   ├── prometheus.yml
│   ├── grafana-dashboard.json
│   ├── loki-config.yml
│   └── alert-rules.yml
│
└── security-tests/                    # Security test suite (Future)
    ├── security-tests.sh
    └── dependency-check.sh
```

---

## Quick Start (Frontend Only)

### Prerequisites

- Node.js 18+ and npm
- Modern browser (Chrome/Edge/Firefox/Safari)

### Installation

```bash
# Navigate to project directory
cd 

# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Development URLs

- **Frontend Dev**: http://localhost:5173
- **Backend API**: (Not yet implemented - will be `https://api.yourdomain.me`)
- **IAM Service**: (Not yet implemented - will be `https://iam.yourdomain.me`)

---

##  Frontend Features

### Current Implementation

 **Design System**
- Modern dark mode with vibrant gradients
- Glassmorphism effects
- Smooth micro-animations
- Google Fonts (Inter + Outfit)
- CSS custom properties for theming

 **Web Components (Lit 3.x)**
- Login component with OAuth 2.1 PKCE flow
- Dashboard with navigation
- ABAC policy builder (drag-and-drop UI)
- Real-time audit log viewer
- User profile with 2FA management

 **PWA Infrastructure**
- Service worker with Workbox
- Offline-first caching strategy
- Background sync for failed requests
- App manifest with icons

 **Security Features**
- DOMPurify XSS sanitization
- CSP Level 3 headers
- Device fingerprinting (Canvas + WebGL + Audio)
- Encrypted IndexedDB storage
- SRI hashes for external scripts

 **State Management**
- Zustand 4.x global store
- Encrypted session persistence
- No tokens in service worker (in-memory only)

 **Real-time Communication**
- WebSocket manager with auto-reconnect
- Exponential backoff retry logic
- SSE fallback support

---

## 🔐 Security Highlights

### CVE Mitigations Implemented

| CVE | Description | Status |
|-----|-------------|--------|
| CVE-2022-23529 | JWT Algorithm Confusion |  Frontend: Algorithm whitelist RS256/ES256 |
| PWA Injection | Service Worker Script Injection |  SRI hashes + CSP worker-src |
| XSS (CWE-79) | Cross-Site Scripting |  DOMPurify sanitization |
| Token Theft | Offline Token Theft |  No service worker token caching |

### Security Best Practices

-  **No Token Caching**: Tokens stored in-memory only, re-auth on reload
-  **Device Fingerprinting**: Multi-factor device identification
-  **Content Security Policy**: Nonce-based script execution
-  **Encrypted Storage**: Web Crypto API for IndexedDB encryption
-  **Input Sanitization**: All user inputs sanitized with DOMPurify
-  **SRI Hashes**: Subresource Integrity for external dependencies

---

##  Implementation Roadmap

###  Phase 5: PWA Frontend (Current)
- [x] Design system with modern aesthetics
- [x] Lit component library
- [x] Service worker with offline support
- [x] WebSocket real-time communication
- [x] Security features (DOMPurify, CSP, SRI)
- [x] OAuth 2.1 PKCE client (ready for backend)

###  Phase 1: Infrastructure (Next)
- [ ] Docker Compose setup
- [ ] PostgreSQL 16.1 with pgcrypto
- [ ] Redis 7.2 Cluster
- [ ] Traefik + TLS 1.3
- [ ] HashiCorp Vault

###  Phase 2: IAM Service (Next)
- [ ] OAuth 2.1 authorization server
- [ ] PASETO v4 token service
- [ ] TOTP 2FA enrollment
- [ ] User realm with bcrypt
- [ ] Rate limiting

###  Phase 3: API Gateway & ABAC
- [ ] JAX-RS REST API
- [ ] ABAC policy engine
- [ ] WebSocket audit streaming
- [ ] mTLS iam ↔ api

###  Phase 4: Steganography Module
- [ ] AES-256-GCM encryption
- [ ] LSB-DCT steganography (OpenCV)
- [ ] MinIO integration
- [ ] PSNR validation (≥45dB)

###  Phase 6: Security Hardening
- [ ] OWASP ZAP scanning
- [ ] Trivy container scanning
- [ ] Penetration testing
- [ ] Prometheus + Grafana

###  Phase 7: Production Deployment
- [ ] VM provisioning
- [ ] TLS 1.3 enforcement
- [ ] HSTS preload
- [ ] Compliance audit

---

##  Testing

### Frontend Tests (Current)

```bash
# Run unit tests (when implemented)
npm test

# Run E2E tests (when implemented)
npm run test:e2e

# Check bundle size
npm run build -- --analyze
```

### Security Testing (Future)

```bash
# Algorithm confusion attack test
./security-tests/security-tests.sh algorithm-confusion

# Token replay attack test
./security-tests/security-tests.sh token-replay

# XSS fuzzing
./security-tests/security-tests.sh xss

# Container CVE scan
trivy image securegate-frontend:latest
```

---

##  Development Guide

### Adding New Components

```javascript
// pwa-frontend/src/components/my-component.js
import { LitElement, html, css } from 'lit';

export class MyComponent extends LitElement {
  static styles = css`
    :host {
      display: block;
    }
  `;

  render() {
    return html`<div>My Component</div>`;
  }
}

customElements.define('my-component', MyComponent);
```

### Using the Store

```javascript
import { useStore } from '../store/store.js';

const user = useStore.getState().user;
useStore.getState().setUser({ name: 'John' });
```

### WebSocket Communication

```javascript
import { websocketManager } from '../websocket/websocket-manager.js';

websocketManager.connect('wss://api.yourdomain.me/audit');
websocketManager.on('audit-event', (event) => {
  console.log('New audit event:', event);
});
```

---

##  Configuration

### Environment Variables

Create a `.env` file in the root:

```env
# API Endpoints (will be configured when backend is ready)
VITE_IAM_URL=https://iam.yourdomain.me
VITE_API_URL=https://api.yourdomain.me

# OAuth 2.1 Configuration
VITE_OAUTH_CLIENT_ID=securegate-pwa
VITE_OAUTH_REDIRECT_URI=https://www.yourdomain.me/callback

# WebSocket
VITE_WS_URL=wss://api.yourdomain.me/audit
```

---

## 📚 Resources & References

### Security Standards
- [OAuth 2.1 Specification](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-07)
- [PASETO Tokens](https://paseto.io/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Zero Trust Architecture](https://www.nist.gov/publications/zero-trust-architecture)

### Technologies
- [Lit - Web Components](https://lit.dev/)
- [Workbox - PWA](https://developers.google.com/web/tools/workbox)
- [Zustand - State Management](https://github.com/pmndrs/zustand)
- [DOMPurify - XSS Prevention](https://github.com/cure53/DOMPurify)

### CVE Documentation
- [CVE-2022-23529 - JWT Algorithm Confusion](https://nvd.nist.gov/vuln/detail/CVE-2022-23529)
- [CVE-2025-23367 - WildFly RBAC Escalation](https://www.wiz.io/vulnerability-database/cve/cve-2025-23367)







-  Zero HIGH/CRITICAL CVEs

---

**Document Version**: 1.0  
**Last Updated**: December 7, 2025  
**Status**: Phase 5 (Frontend) Complete - Backend Phases Pending
