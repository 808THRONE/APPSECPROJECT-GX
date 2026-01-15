# 📱 SecureGate UI Documentation - Progressive Web App

The **SecureGate UI** (`pwa-frontend/`) is a modern, React-based **Progressive Web App (PWA)** that provides a high-fidelity interface for the IAM ecosystem. It emphasizes visual excellence, security, and real-time interaction.

---

## 🏗️ Technical Stack

- **Framework**: React 19 (TypeScript)
- **Build Tool**: Vite 7
- **Security**: JWT-based session management, DOMPurify for XSS protection.
- **Protocol**: RESTful communication via the **API Gateway**.

---

## 📂 File-to-Feature Mapping

### 1. Core Application
| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[App.tsx](file:///c:/Users/808th/OneDrive/Desktop/A%20try/pwa-frontend/src/App.tsx)** | Root Controller | Manages global routing, authentication state, and theme providers. |
| **[main.tsx](file:///c:/Users/808th/OneDrive/Desktop/A%20try/pwa-frontend/src/main.tsx)** | Entry Point | Mounts the React application and initializes the PWA service worker. |
| **[config.ts](file:///c:/Users/808th/OneDrive/Desktop/A%20try/pwa-frontend/src/config.ts)** | Configuration | Environment-specific API base URLs and feature toggles. |

### 2. Services (API Integration)
| File Name | Responsibility | Aspect Implemented |
|:----------|:---------------|:-------------------|
| **[authService.ts](file:///c:/Users/808th/OneDrive/Desktop/A%20try/pwa-frontend/src/services/authService.ts)** | Auth Logic | Handles OAuth2 flows, login, registration, and MFA verification. |
| **[stegoService.ts](file:///c:/Users/808th/OneDrive/Desktop/A%20try/pwa-frontend/src/services/stegoService.ts)** | Steganography | Integrates with the Stego Module for data embedding/extraction. |
| **[policyService.ts](file:///c:/Users/808th/OneDrive/Desktop/A%20try/pwa-frontend/src/services/policyService.ts)** | RBAC/ABAC | Manages security policies and evaluates permissions locally. |
| **[secureApi.ts](file:///c:/Users/808th/OneDrive/Desktop/A%20try/pwa-frontend/src/services/secureApi.ts)** | Axios Wrapper | Intercepts requests to inject JWTs and handle 401/403 errors. |

### 3. Key Pages
| Page | File | Purpose |
|:-----|:-----|:--------|
| **Login** | `pages/LoginPage.tsx` | Secure credential entry with CSRF and MFA support. |
| **Dashboard** | `pages/Dashboard.tsx` | Central command view showing system health and user status. |
| **Audit Logs** | `pages/AuditPage.tsx` | View for the hash-chained, tamper-evident security logs. |
| **Ploicy Mgmt** | `pages/PolicyPage.tsx` | Administrative interface for ABAC rule configuration. |

---

## 🔐 Frontend Security Measures

### 1. XSS Mitigation
All dynamic content is sanitized using **DOMPurify** before being injected into the DOM, preventing script injection even if the API Gateway's filters are bypassed.

### 2. Secure Token Storage
Access tokens are handled with strict scopes. The application uses a "Silent Refresh" strategy to maintain sessions without exposing long-lived credentials to `localStorage` where possible.

### 3. PWA Capabilities
- **Offline Support**: Service workers cache critical assets for availability during network instability.
- **App-Like Feel**: Configured with a `manifest.json` for installation on mobile and desktop.

---

## 🚀 Build & Development

- **Developer Mode**: `npm run dev` (Runs on `http://localhost:5173`)
- **Production Build**: `npm run build`
- **Deployment**: Optimized for hosting behind Nginx with the API Gateway as the backend proxy.
