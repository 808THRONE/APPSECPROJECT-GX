import { LitElement, html, css } from 'lit';
import { useStore } from './store/store.js';
import { handleOAuthCallback } from './utils/oauth-client.js';

// Expose store globally for development/demo purposes
window.useStore = useStore;

// Import all components
import './components/login-component.js';
import './components/dashboard-component.js';
import './components/policy-builder.js';
import './components/audit-viewer.js';
import './components/profile-component.js';

class SecureGateApp extends LitElement {
  static properties = {
    currentView: { type: String },
    isAuthenticated: { type: Boolean },
    loading: { type: Boolean },
  };

  static styles = css`
    :host {
      display: block;
      min-height: 100vh;
    }

    .app-container {
      display: flex;
      min-height: 100vh;
    }

    .sidebar {
      width: var(--sidebar-width);
      background: var(--glass-bg);
      backdrop-filter: var(--glass-blur);
      -webkit-backdrop-filter: var(--glass-blur);
      border-right: 1px solid var(--glass-border);
      padding: var(--space-xl) var(--space-lg);
      display: flex;
      flex-direction: column;
      gap: var(--space-2xl);
      position: fixed;
      height: 100vh;
      overflow-y: auto;
      box-shadow: 4px 0 24px rgba(0, 0, 0, 0.4);
    }

    .logo {
      display: flex;
      align-items: center;
      gap: var(--space-md);
      padding: 0 var(--space-sm);
    }

    .logo-icon {
      width: 44px;
      height: 44px;
      background: var(--gradient-primary);
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--font-size-xl);
      box-shadow: var(--shadow-glow);
      border: 1px solid rgba(255,255,255,0.2);
    }

    .logo-text {
      font-family: var(--font-display);
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-extrabold);
      color: var(--color-text-primary);
      letter-spacing: 0.05em;
      text-transform: uppercase;
    }

    .logo-text span {
      color: var(--color-primary);
    }

    .nav {
      display: flex;
      flex-direction: column;
      gap: var(--space-sm);
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: var(--space-md);
      padding: var(--space-md) var(--space-lg);
      border-radius: var(--radius-md);
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: all var(--transition-base);
      font-weight: var(--font-weight-semibold);
      letter-spacing: 0.02em;
      border: 1px solid transparent;
      position: relative;
      overflow: hidden;
    }

    .nav-item::before {
      content: '';
      position: absolute;
      top: 0; left: 0; width: 4px; height: 100%;
      background: var(--color-primary);
      opacity: 0;
      transition: opacity var(--transition-base);
    }

    .nav-item:hover {
      background: rgba(255, 255, 255, 0.03);
      color: var(--color-text-primary);
      border-color: rgba(255, 255, 255, 0.05);
    }

    .nav-item.active {
      background: linear-gradient(90deg, rgba(0, 240, 255, 0.1) 0%, transparent 100%);
      color: var(--color-primary);
      border-color: rgba(0, 240, 255, 0.2);
      box-shadow: inset 2px 0 10px rgba(0, 240, 255, 0.05);
    }

    .nav-item.active::before {
      opacity: 1;
      box-shadow: 0 0 10px var(--color-primary);
    }

    .nav-icon {
      font-size: var(--font-size-lg);
      opacity: 0.8;
      transition: opacity var(--transition-base), transform var(--transition-base);
    }

    .nav-item:hover .nav-icon {
      opacity: 1;
      transform: scale(1.1);
    }

    .nav-item.active .nav-icon {
      opacity: 1;
      color: var(--color-primary);
      filter: drop-shadow(0 0 5px var(--color-primary));
    }

    .main-content {
      flex: 1;
      margin-left: var(--sidebar-width);
      padding: var(--space-xl);
      background: radial-gradient(circle at top right, rgba(0, 240, 255, 0.03), transparent 40%);
      min-height: 100vh;
    }

    @media (max-width: 768px) {
      .sidebar {
        display: none;
      }

      .main-content {
        margin-left: 0;
      }
    }
  `;

  constructor() {
    super();
    this.currentView = 'login';
    this.isAuthenticated = false;
    this.loading = true;
    this._storeUnsubscribe = null;
  }

  async connectedCallback() {
    super.connectedCallback();

    // Handle OAuth callback
    if (window.location.search.includes('code=')) {
      await this._handleOAuthCallback();
    }

    // Subscribe to store changes
    this._storeUnsubscribe = useStore.subscribe((state) => {
      this.currentView = state.currentView;
      this.isAuthenticated = state.isAuthenticated;
    });

    // Initialize state from store
    const state = useStore.getState();
    this.currentView = state.currentView;
    this.isAuthenticated = state.isAuthenticated;

    // If authenticated, show dashboard by default
    if (this.isAuthenticated && this.currentView === 'login') {
      useStore.getState().setCurrentView('dashboard');
    }

    this.loading = false;

    // Register service worker
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.register('/service-worker.js')
        .then(() => console.log('Service Worker registered'))
        .catch((err) => console.error('Service Worker registration failed:', err));
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    if (this._storeUnsubscribe) {
      this._storeUnsubscribe();
    }
  }

  async _handleOAuthCallback() {
    try {
      const tokens = await handleOAuthCallback();
      useStore.getState().setTokens(tokens.accessToken, tokens.refreshToken);

      // Parse user info from ID token (in real app, validate first!)
      const idTokenPayload = JSON.parse(atob(tokens.idToken.split('.')[1]));
      useStore.getState().setUser({
        name: idTokenPayload.name || 'User',
        email: idTokenPayload.email,
      });

      // Navigate to dashboard
      useStore.getState().setCurrentView('dashboard');

      // Clean URL
      window.history.replaceState({}, document.title, '/');
    } catch (error) {
      console.error('OAuth callback error:', error);
      useStore.getState().setCurrentView('login');
    }
  }

  handleNavigate(view) {
    useStore.getState().setCurrentView(view);
  }

  renderContent() {
    if (this.loading) {
      return html`
        <div class="loading-container">
          <div class="loading-spinner"></div>
          <p>Loading...</p>
        </div>
      `;
    }

    if (!this.isAuthenticated) {
      return html`<login-component></login-component>`;
    }

    switch (this.currentView) {
      case 'dashboard':
        return html`<dashboard-component></dashboard-component>`;
      case 'policies':
        return html`<policy-builder></policy-builder>`;
      case 'audit':
        return html`<audit-viewer></audit-viewer>`;
      case 'profile':
        return html`<profile-component></profile-component>`;
      default:
        return html`<dashboard-component></dashboard-component>`;
    }
  }

  render() {
    if (!this.isAuthenticated) {
      return this.renderContent();
    }

    return html`
      <div class="app-container">
        <nav class="sidebar">
          <div class="logo">
            <div class="logo-icon">🛡️</div>
            <div class="logo-text">Secure<span>Gate</span></div>
          </div>

          <div class="nav">
            <div
              class="nav-item ${this.currentView === 'dashboard' ? 'active' : ''}"
              @click="${() => this.handleNavigate('dashboard')}"
            >
              <span class="nav-icon">📊</span>
              <span>Dashboard</span>
            </div>

            <div
              class="nav-item ${this.currentView === 'policies' ? 'active' : ''}"
              @click="${() => this.handleNavigate('policies')}"
            >
              <span class="nav-icon">🛡️</span>
              <span>Policy Builder</span>
            </div>

            <div
              class="nav-item ${this.currentView === 'audit' ? 'active' : ''}"
              @click="${() => this.handleNavigate('audit')}"
            >
              <span class="nav-icon">📝</span>
              <span>Audit Logs</span>
            </div>

            <div
              class="nav-item ${this.currentView === 'profile' ? 'active' : ''}"
              @click="${() => this.handleNavigate('profile')}"
            >
              <span class="nav-icon">👤</span>
              <span>Profile & 2FA</span>
            </div>
          </div>
        </nav>

        <main class="main-content">
          ${this.renderContent()}
        </main>
      </div>
    `;
  }
}

customElements.define('securegate-app', SecureGateApp);

// Mount the app
document.addEventListener('DOMContentLoaded', () => {
  const appRoot = document.getElementById('app');
  appRoot.innerHTML = '<securegate-app></securegate-app>';
});
