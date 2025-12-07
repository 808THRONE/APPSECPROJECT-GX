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
      padding: var(--space-xl);
      display: flex;
      flex-direction: column;
      gap: var(--space-lg);
      position: fixed;
      height: 100vh;
      overflow-y: auto;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: var(--space-md);
      padding: var(--space-md);
      margin-bottom: var(--space-lg);
    }

    .logo-icon {
      width: 40px;
      height: 40px;
      background: var(--gradient-primary);
      border-radius: var(--radius-lg);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--font-size-xl);
    }

    .logo-text {
      font-family: var(--font-display);
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
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
      padding: var(--space-md);
      border-radius: var(--radius-lg);
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: all var(--transition-fast);
      font-weight: var(--font-weight-medium);
    }

    .nav-item:hover {
      background: var(--color-bg-tertiary);
      color: var(--color-text-primary);
    }

    .nav-item.active {
      background: var(--gradient-primary);
      color: var(--color-text-primary);
      box-shadow: var(--shadow-glow);
    }

    .nav-icon {
      font-size: var(--font-size-xl);
    }

    .main-content {
      flex: 1;
      margin-left: var(--sidebar-width);
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
            <div class="logo-icon">🔐</div>
            <div class="logo-text">SecureGate</div>
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
