import { LitElement, html, css } from 'lit';
import { generateDeviceFingerprint } from '../security/device-fingerprint.js';
import './common/button.js';
import './common/card.js';

export class SignupComponent extends LitElement {
    static properties = {
        loading: { type: Boolean },
        error: { type: String },
        successMessage: { type: String },
        username: { type: String },
        password: { type: String },
        confirmPassword: { type: String },
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

    .signup-container {
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

    .error {
      background: rgba(239, 68, 68, 0.1);
      color: var(--color-error);
      padding: var(--space-md);
      border-radius: var(--radius-md);
      margin-bottom: var(--space-lg);
      border: 1px solid var(--color-error);
    }
    
    .success {
      background: rgba(16, 185, 129, 0.1);
      color: var(--color-success);
      padding: var(--space-md);
      border-radius: var(--radius-md);
      margin-bottom: var(--space-lg);
      border: 1px solid var(--color-success);
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
      from { from { opacity: 0; transform: translateY(30px); } to { opacity: 1; transform: translateY(0); } }
    }
  `;

    constructor() {
        super();
        this.loading = false;
        this.error = '';
        this.successMessage = '';
        this.username = '';
        this.password = '';
        this.confirmPassword = '';
    }

    handleInput(e) {
        const { name, value } = e.target;
        this[name] = value;
    }

    async handleSignup(e) {
        if (e) e.preventDefault();

        if (this.password !== this.confirmPassword) {
            this.error = "Passwords do not match";
            return;
        }

        this.loading = true;
        this.error = '';
        this.successMessage = '';

        try {
            const signupResponse = await fetch('/rest-iam/auth/signup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    username: this.username,
                    password: this.password,
                    roles: ["user"]
                })
            });

            if (!signupResponse.ok) {
                const errorData = await signupResponse.json();
                throw new Error(errorData.error_description || 'Signup failed');
            }

            this.successMessage = "Account created successfully! Redirecting to login...";

            // Wait 2 seconds then navigate to login
            setTimeout(() => {
                this.navigateToLogin();
            }, 2000);

        } catch (err) {
            this.error = err.message || 'Failed to sign up';
            this.loading = false;
        }
    }

    navigateToLogin(e) {
        if (e) e.preventDefault();
        this.dispatchEvent(new CustomEvent('navigate', {
            detail: { view: 'login' },
            bubbles: true,
            composed: true
        }));
    }

    render() {
        return html`
      <div class="signup-container">
        <div class="logo">
          <div class="logo-icon">📝</div>
          <div class="logo-text">SecureGate</div>
          <div class="logo-subtitle">Create Account</div>
        </div>

        <sg-card glass>
          <div class="card-content">
            <h1>Create Account</h1>
            <p>Join the secure enterprise network</p>

            ${this.error ? html`<div class="error">${this.error}</div>` : ''}
            ${this.successMessage ? html`<div class="success">${this.successMessage}</div>` : ''}

            <form @submit="${this.handleSignup}">
                <div class="input-group">
                    <label for="username">Username</label>
                    <input 
                        type="text" 
                        id="username" 
                        name="username" 
                        .value="${this.username}"
                        @input="${this.handleInput}"
                        required
                        placeholder="Choose a username"
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
                        placeholder="Choose a strong password"
                    >
                </div>

                <div class="input-group">
                    <label for="confirmPassword">Confirm Password</label>
                    <input 
                        type="password" 
                        id="confirmPassword" 
                        name="confirmPassword" 
                        .value="${this.confirmPassword}"
                        @input="${this.handleInput}"
                        required
                        placeholder="Confirm your password"
                    >
                </div>

                <sg-button
                  variant="primary"
                  size="lg"
                  fullWidth
                  ?loading="${this.loading}"
                  type="submit"
                >
                  ${this.loading ? 'Creating Account...' : 'Sign Up'}
                </sg-button>
            </form>

            <div class="link-text">
                Already have an account? <a @click="${this.navigateToLogin}">Log in here</a>
            </div>
          </div>
        </sg-card>
      </div>
    `;
    }
}

customElements.define('signup-component', SignupComponent);
