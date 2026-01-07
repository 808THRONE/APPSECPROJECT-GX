import { LitElement, html, css } from 'lit';
import { useStore } from '../store/store.js';
import './common/button.js';
import './common/card.js';

export class DashboardComponent extends LitElement {
    static properties = {
        stats: { type: Object },
    };

    static styles = css`
    :host {
      display: block;
      padding: var(--space-2xl);
      max-width: var(--container-max-width);
      margin: 0 auto;
    }

    .header {
      margin-bottom: var(--space-2xl);
    }

    .welcome {
      font-family: var(--font-display);
      font-size: var(--font-size-4xl);
      font-weight: var(--font-weight-extrabold);
      margin-bottom: var(--space-sm);
      background: var(--gradient-primary);
      -webkit-background-clip: text;
      background-clip: text;
      -webkit-text-fill-color: transparent;
      animation: fadeInDown 0.6s ease-out;
    }

    .subtitle {
      color: var(--color-text-secondary);
      font-size: var(--font-size-lg);
      animation: fadeInUp 0.6s ease-out 0.1s backwards;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: var(--space-lg);
      margin-bottom: var(--space-2xl);
    }

    .stat-card {
      padding: var(--space-xl);
      animation: fadeInUp 0.6s ease-out backwards;
    }

    .stat-card:nth-child(1) { animation-delay: 0.2s; }
    .stat-card:nth-child(2) { animation-delay: 0.3s; }
    .stat-card:nth-child(3) { animation-delay: 0.4s; }
    .stat-card:nth-child(4) { animation-delay: 0.5s; }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-lg);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: var(--space-md);
      font-size: var(--font-size-2xl);
    }

    .stat-icon.primary {
      background: var(--gradient-primary);
    }

    .stat-icon.secondary {
      background: var(--gradient-secondary);
    }

    .stat-icon.success {
      background: linear-gradient(135deg, var(--color-success) 0%, var(--color-accent) 100%);
    }

    .stat-icon.warning {
      background: linear-gradient(135deg, var(--color-warning) 0%, var(--color-error) 100%);
    }

    .stat-label {
      color: var(--color-text-tertiary);
      font-size: var(--font-size-sm);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      margin-bottom: var(--space-xs);
    }

    .stat-value {
      font-family: var(--font-display);
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
    }

    .quick-actions {
      margin-bottom: var(--space-2xl);
    }

    .section-title {
      font-family: var(--font-display);
      font-size: var(--font-size-2xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-lg);
    }

    .actions-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: var(--space-lg);
    }

    .action-card {
      padding: var(--space-xl);
      cursor: pointer;
      transition: all var(--transition-base);
    }

    .action-card:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-xl);
    }

    .action-header {
      display: flex;
      align-items: center;
      gap: var(--space-md);
      margin-bottom: var(--space-md);
    }

    .action-icon {
      font-size: var(--font-size-3xl);
    }

    .action-title {
      font-family: var(--font-display);
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
    }

    .action-description {
      color: var(--color-text-secondary);
      font-size: var(--font-size-sm);
    }

    @keyframes fadeInDown {
      from {
        opacity: 0;
        transform: translateY(-20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @media (max-width: 768px) {
      :host {
        padding: var(--space-lg);
      }

      .stats-grid,
      .actions-grid {
        grid-template-columns: 1fr;
      }
    }
  `;

    constructor() {
        super();
        this.stats = {
            activeSessions: 127,
            policies: 42,
            auditEvents: 1542,
            alerts: 3,
        };
    }

    handleNavigate(view) {
        useStore.getState().setCurrentView(view);
    }

    render() {
        const user = useStore.getState().user;
        const userName = user?.name || 'Administrator';

        return html`
      <div class="header">
        <h1 class="welcome">Welcome back, ${userName}</h1>
        <p class="subtitle">Dashboard Overview - SecureGate IAM Portal</p>
      </div>

      <div class="stats-grid">
        <sg-card glass>
          <div class="stat-card">
            <div class="stat-icon primary">👥</div>
            <div class="stat-label">Active Sessions</div>
            <div class="stat-value">${this.stats.activeSessions}</div>
          </div>
        </sg-card>

        <sg-card glass>
          <div class="stat-card">
            <div class="stat-icon secondary">📋</div>
            <div class="stat-label">ABAC Policies</div>
            <div class="stat-value">${this.stats.policies}</div>
          </div>
        </sg-card>

        <sg-card glass>
          <div class="stat-card">
            <div class="stat-icon success">📊</div>
            <div class="stat-label">Audit Events</div>
            <div class="stat-value">${this.stats.auditEvents}</div>
          </div>
        </sg-card>

        <sg-card glass>
          <div class="stat-card">
            <div class="stat-icon warning">⚠️</div>
            <div class="stat-label">Security Alerts</div>
            <div class="stat-value">${this.stats.alerts}</div>
          </div>
        </sg-card>
      </div>

      <div class="quick-actions">
        <h2 class="section-title">Quick Actions</h2>
        <div class="actions-grid">
          <sg-card glass @click="${() => this.handleNavigate('policies')}">
            <div class="action-card">
              <div class="action-header">
                <div class="action-icon">🛡️</div>
                <div class="action-title">Policy Builder</div>
              </div>
              <p class="action-description">
                Create and manage ABAC policies with visual rule composer
              </p>
            </div>
          </sg-card>

          <sg-card glass @click="${() => this.handleNavigate('audit')}">
            <div class="action-card">
              <div class="action-header">
                <div class="action-icon">📝</div>
                <div class="action-title">Audit Logs</div>
              </div>
              <p class="action-description">
                View real-time security events and authentication history
              </p>
            </div>
          </sg-card>

          <sg-card glass @click="${() => this.handleNavigate('profile')}">
            <div class="action-card">
              <div class="action-header">
                <div class="action-icon">👤</div>
                <div class="action-title">Profile & 2FA</div>
              </div>
              <p class="action-description">
                Manage your account settings and two-factor authentication
              </p>
            </div>
          </sg-card>
        </div>
      </div>
    `;
    }
}

customElements.define('dashboard-component', DashboardComponent);
