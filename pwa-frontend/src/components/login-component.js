import { LitElement, html, css } from 'lit';
import { useStore } from '../store/store.js';
import { startAuthorizationFlow } from '../utils/oauth-client.js';
import { generateDeviceFingerprint } from '../security/device-fingerprint.js';
import './common/button.js';
import './common/card.js';

export class LoginComponent extends LitElement {
  static properties = {
    loading: { type: Boolean },
    error: { type: String },
    username: { type: String },
    password: { type: String },
  };

  static styles = css`
    :host {
      display: block;
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: var(--space-lg);
    }

    .login-container {
      width: 100%;
      max-width: 450px;
      animation: fadeInUp 0.6s ease-out;
    }

    .logo {
      text-align: center;
      margin-bottom: var(--space-2xl);
    }

    .logo-icon {
      width: 80px;
      height: 80px;
      background: var(--gradient-primary);
      border-radius: var(--radius-xl);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto var(--space-lg);
      box-shadow: var(--shadow-glow);
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-extrabold);
      color: white;
    }

    .logo-text {
      font-family: var(--font-display);
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-extrabold);
      background: var(--gradient-primary);
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
      margin-bottom: var(--space-sm);
    }

    .logo-subtitle {
      color: var(--color-text-secondary);
      font-size: var(--font-size-base);
    }

    .card-content {
      padding: var(--space-2xl);
    }

    h1 {
      font-family: var(--font-display);
      font-size: var(--font-size-2xl);
      margin-bottom: var(--space-sm);
      color: var(--color-text-primary);
    }

    p {
      color: var(--color-text-secondary);
      margin-bottom: var(--space-xl);
    }

    .features {
      margin: var(--space-xl) 0;
      padding: 0;
      list-style: none;
    }

    .features li {
      display: flex;
      align-items: center;
      gap: var(--space-md);
      margin-bottom: var(--space-md);
      color: var(--color-text-secondary);
    }

    .feature-icon {
      width: 24px;
      height: 24px;
      border-radius: var(--radius-md);
      background: var(--gradient-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      font-size: 14px;
      color: white;
    }

    .error {
      background: rgba(239, 68, 68, 0.1);
      color: var(--color-error);
      padding: var(--space-md);
      border-radius: var(--radius-md);
      margin-bottom: var(--space-lg);
      border: 1px solid var(--color-error);
    }

    .input-group {
      margin-bottom: var(--space-lg);
    }

    label {
      display: block;
      margin-bottom: var(--space-xs);
      color: var(--color-text-secondary);
      font-size: var(--font-size-sm);
    }

    input {
      width: 100%;
      padding: var(--space-md);
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--glass-border);
      border-radius: var(--radius-md);
      color: var(--color-text-primary);
      font-size: var(--font-size-base);
      transition: all var(--transition-fast);
    }

    input:focus {
      outline: none;
      border-color: var(--color-primary);
      background: rgba(255, 255, 255, 0.1);
    }

    .security-note {
      margin-top: var(--space-xl);
      padding: var(--space-md);
      background: rgba(59, 130, 246, 0.1);
      border-radius: var(--radius-md);
      border: 1px solid rgba(59, 130, 246, 0.3);
      font-size: var(--font-size-sm);
      color: var(--color-text-tertiary);
    }
    
    .link-text {
        text-align: center;
        margin-top: var(--space-md);
        color: var(--color-text-secondary);
        font-size: var(--font-size-sm);
    }
    
    .link-text a {
        color: var(--color-primary);
        text-decoration: none;
        cursor: pointer;
    }
    
    .link-text a:hover {
        text-decoration: underline;
    }

    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(30px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `;

  constructor() {
    super();
    this.loading = false;
    this.error = '';
    this.username = '';
    this.password = '';
  }

  handleInput(e) {
    const { name, value } = e.target;
    this[name] = value;
  }

  async handleLogin(e) {
    if (e) e.preventDefault();
    this.loading = true;
    this.error = '';

    try {
      const fingerprint = await generateDeviceFingerprint();
      sessionStorage.setItem('device_fingerprint', fingerprint.hash);

      const loginResponse = await fetch('/rest-iam/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: this.username,
          password: this.password
        })
      });

      if (!loginResponse.ok) {
        const errorData = await loginResponse.json();
        throw new Error(errorData.error_description || 'Authentication failed');
      }

      // Proceed to OAuth flow
      await startAuthorizationFlow();
    } catch (err) {
      this.error = err.message || 'Failed to initiate login';
      this.loading = false;
    }
  }

  navigateToSignup(e) {
    e.preventDefault();
    // Dispatch event for app shell to handle navigation
    this.dispatchEvent(new CustomEvent('navigate', {
      detail: { view: 'signup' },
      bubbles: true,
      composed: true
    }));
  }

  render() {
    return html`
      <div class="login-container">
        <div class="logo">
          <div class="logo-icon">🔐</div>
          <div class="logo-text">SecureGate</div>
          <div class="logo-subtitle">IAM Portal</div>
        </div>

        <sg-card glass>
          <div class="card-content">
            <h1>Welcome Back</h1>
            <p>Enterprise Identity & Access Management</p>

            ${this.error ? html`
              <div class="error">${this.error}</div>
            ` : ''}

            <form @submit="${this.handleLogin}">
                <div class="input-group">
                    <label for="username">Username</label>
                    <input 
                        type="text" 
                        id="username" 
                        name="username" 
                        .value="${this.username}"
                        @input="${this.handleInput}"
                        required
                        placeholder="Enter your username"
                    >
                </div>

                <div class="input-group">
                    <label for="password">Password</label>
                    <input 
                        type="password" 
                        id="password" 
                        name="password" 
                        .value="${this.password}"
                        @input="${this.handleInput}"
                        required
                        placeholder="Enter your password"
                    >
                </div>

                <sg-button
                  variant="primary"
                  size="lg"
                  fullWidth
                  ?loading="${this.loading}"
                  type="submit"
                >
                  ${this.loading ? 'Connecting...' : 'Secure Login'}
                </sg-button>
            </form>

            <div class="link-text">
                Don't have an account? <a @click="${this.navigateToSignup}">Sign up here</a>
            </div>

            <div class="security-note">
              🔒 Your device fingerprint will be collected for security purposes.
            </div>
          </div>
        </sg-card>
      </div>
    `;
  }
}

customElements.define('login-component', LoginComponent);
