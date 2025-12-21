import { LitElement, html, css } from 'lit';
import { useStore } from '../store/store.js';
import './common/button.js';
import './common/card.js';

export class ProfileComponent extends LitElement {
    static properties = {
        user: { type: Object },
        twoFactorEnabled: { type: Boolean },
        showQrCode: { type: Boolean },
    };

    static styles = css`
    :host {
      display: block;
      padding: var(--space-2xl);
      max-width: 800px;
      margin: 0 auto;
    }

    h1 {
      font-family: var(--font-display);
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-2xl);
    }

    .profile-header {
      display: flex;
      align-items: center;
      gap: var(--space-xl);
      padding: var(--space-xl);
      margin-bottom: var(--space-lg);
    }

    .avatar {
      width: 100px;
      height: 100px;
      border-radius: var(--radius-full);
      background: var(--gradient-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--font-size-4xl);
      font-weight: var(--font-weight-bold);
      color: white;
      box-shadow: var(--shadow-glow);
    }

    .user-info h2 {
      font-family: var(--font-display);
      font-size: var(--font-size-2xl);
      margin-bottom: var(--space-xs);
      color: var(--color-text-primary);
    }

    .user-email {
      color: var(--color-text-secondary);
      font-size: var(--font-size-lg);
    }

    .section {
      margin-bottom: var(--space-xl);
    }

    .section-title {
      font-family: var(--font-display);
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-md);
    }

    .info-grid {
      display: grid;
      gap: var(--space-md);
      padding: var(--space-xl);
    }

    .info-row {
      display: grid;
      grid-template-columns: 200px 1fr;
      gap: var(--space-md);
      padding: var(--space-md) 0;
      border-bottom: 1px solid var(--color-bg-tertiary);
    }

    .info-row:last-child {
      border-bottom: none;
    }

    .info-label {
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-secondary);
    }

    .info-value {
      color: var(--color-text-primary);
    }

    .mfa-section {
      padding: var(--space-xl);
    }

    .mfa-status {
      display: inline-flex;
      align-items: center;
      gap: var(--space-sm);
      padding: var(--space-sm) var(--space-md);
      border-radius: var(--radius-full);
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-semibold);
      margin-bottom: var(--space-lg);
    }

    .mfa-status.enabled {
      background: rgba(34, 197, 94, 0.2);
      color: var(--color-success);
    }

    .mfa-status.disabled {
      background: rgba(239, 68, 68, 0.2);
      color: var(--color-error);
    }

    .qr-code {
      width: 200px;
      height: 200px;
      background: white;
      border-radius: var(--radius-lg);
      margin: var(--space-lg) 0;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--font-size-xs);
      color: #000;
    }

    .recovery-codes {
      background: var(--color-bg-tertiary);
      padding: var(--space-lg);
      border-radius: var(--radius-md);
      font-family: var(--font-mono);
      font-size: var(--font-size-sm);
      margin: var(--space-lg) 0;
    }

    .recovery-code {
      padding: var(--space-sm) 0;
      color: var(--color-text-secondary);
    }

    .actions {
      display: flex;
      gap: var(--space-md);
      margin-top: var(--space-lg);
    }

    @media (max-width: 768px) {
      :host {
        padding: var(--space-lg);
      }

      .profile-header {
        flex-direction: column;
        text-align: center;
      }

      .info-row {
        grid-template-columns: 1fr;
        gap: var(--space-xs);
      }
    }
  `;

    constructor() {
        super();
        this.user = useStore.getState().user || {
            name: 'Administrator',
            email: 'admin@securegate.com',
            role: 'Security Administrator',
            department: 'Security Operations',
            lastLogin: new Date().toISOString(),
        };
        this.twoFactorEnabled = false;
        this.showQrCode = false;
    }

    handleEnableTwoFactor() {
        this.showQrCode = true;
    }

    handleConfirmTwoFactor() {
        this.twoFactorEnabled = true;
        this.showQrCode = false;
    }

    handleDisableTwoFactor() {
        this.twoFactorEnabled = false;
    }

    handleLogout() {
        useStore.getState().clearTokens();
        sessionStorage.removeItem('pkce_verifier');
        sessionStorage.removeItem('oauth_state');
        sessionStorage.removeItem('device_fingerprint');
        window.history.replaceState({}, document.title, '/');
        useStore.getState().setCurrentView('login');
    }


    render() {
        const initials = this.user.name
            .split(' ')
            .map(n => n[0])
            .join('')
            .toUpperCase();

        return html`
      <h1>👤 Profile & Settings</h1>

      <sg-card glass>
        <div class="profile-header">
          <div class="avatar">${initials}</div>
          <div class="user-info">
            <h2>${this.user.name}</h2>
            <div class="user-email">${this.user.email}</div>
          </div>
        </div>
      </sg-card>

      <div class="section">
        <h3 class="section-title">Account Information</h3>
        <sg-card glass>
          <div class="info-grid">
            <div class="info-row">
              <div class="info-label">Email</div>
              <div class="info-value">${this.user.email}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Role</div>
              <div class="info-value">${this.user.role}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Department</div>
              <div class="info-value">${this.user.department}</div>
            </div>
            <div class="info-row">
              <div class="info-label">Last Login</div>
              <div class="info-value">${new Date(this.user.lastLogin).toLocaleString()}</div>
            </div>
          </div>
        </sg-card>
      </div>

      <div class="section">
        <h3 class="section-title">Two-Factor Authentication</h3>
        <sg-card glass>
          <div class="mfa-section">
            <div class="mfa-status ${this.twoFactorEnabled ? 'enabled' : 'disabled'}">
              ${this.twoFactorEnabled ? '✓ Enabled' : '✗ Disabled'}
            </div>

            ${!this.twoFactorEnabled && !this.showQrCode ? html`
              <p>Add an extra layer of security to your account with two-factor authentication.</p>
              <div class="actions">
                <sg-button @sg-click="${this.handleEnableTwoFactor}">
                  Enable 2FA
                </sg-button>
              </div>
            ` : ''}

            ${this.showQrCode ? html`
              <p>Scan this QR code with your authenticator app:</p>
              <div class="qr-code">
                [QR Code Here]
              </div>
              <p>Or enter this secret manually: <code>ABCD-EFGH-IJKL-MNOP</code></p>
              
              <div style="margin-top: var(--space-lg);">
                <p style="font-weight: var(--font-weight-semibold);">Recovery Codes</p>
                <p style="font-size: var(--font-size-sm); color: var(--color-text-secondary);">
                  Save these codes in a secure location. Each can only be used once.
                </p>
                <div class="recovery-codes">
                  <div class="recovery-code">1. ABCD-1234-EFGH-5678</div>
                  <div class="recovery-code">2. IJKL-9012-MNOP-3456</div>
                  <div class="recovery-code">3. QRST-7890-UVWX-1234</div>
                  <div class="recovery-code">4. YZAB-5678-CDEF-9012</div>
                  <div class="recovery-code">5. GHIJ-3456-KLMN-7890</div>
                </div>
              </div>

              <div class="actions">
                <sg-button variant="ghost" @sg-click="${() => this.showQrCode = false}">
                  Cancel
                </sg-button>
                <sg-button @sg-click="${this.handleConfirmTwoFactor}">
                  Confirm & Enable
                </sg-button>
              </div>
            ` : ''}

            ${this.twoFactorEnabled ? html`
              <p>Two-factor authentication is currently enabled for your account.</p>
              <div class="actions">
                <sg-button variant="danger" @sg-click="${this.handleDisableTwoFactor}">
                  Disable 2FA
                </sg-button>
              </div>
            ` : ''}
          </div>
        </sg-card>
      </div>

      <div class="section">
        <h3 class="section-title">Session Management</h3>
        <sg-card glass>
          <div class="info-grid">
            <p>Logging out will end your current session and require re-authentication.</p>
            <div class="actions">
              <sg-button variant="danger" @sg-click="${this.handleLogout}">
                Logout
              </sg-button>
            </div>
          </div>
        </sg-card>
      </div>
    `;
    }
}

customElements.define('profile-component', ProfileComponent);
