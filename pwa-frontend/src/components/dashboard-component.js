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
      position: relative;
      overflow: hidden;
    }

    .stat-card::after {
      content: '';
      position: absolute;
      bottom: 0; left: 0; right: 0;
      height: 2px;
      background: var(--gradient-primary);
      opacity: 0.5;
    }

    .stat-card:nth-child(1) { animation-delay: 0.2s; }
    .stat-card:nth-child(2) { animation-delay: 0.3s; }
    .stat-card:nth-child(3) { animation-delay: 0.4s; }
    .stat-card:nth-child(4) { animation-delay: 0.5s; }

    .stat-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: var(--space-md);
    }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: var(--font-size-2xl);
      box-shadow: 0 4px 15px rgba(0,0,0,0.3);
      border: 1px solid rgba(255,255,255,0.1);
    }

    .stat-icon.primary { background: linear-gradient(135deg, rgba(0,240,255,0.2) 0%, rgba(0,240,255,0.05) 100%); color: #00f0ff; border-color: rgba(0,240,255,0.3); }
    .stat-icon.secondary { background: linear-gradient(135deg, rgba(112,0,255,0.2) 0%, rgba(112,0,255,0.05) 100%); color: #7000ff; border-color: rgba(112,0,255,0.3); }
    .stat-icon.success { background: linear-gradient(135deg, rgba(0,255,163,0.2) 0%, rgba(0,255,163,0.05) 100%); color: #00ffa3; border-color: rgba(0,255,163,0.3); }
    .stat-icon.warning { background: linear-gradient(135deg, rgba(255,170,0,0.2) 0%, rgba(255,170,0,0.05) 100%); color: #ffaa00; border-color: rgba(255,170,0,0.3); }

    .stat-label {
      color: var(--color-text-secondary);
      font-size: var(--font-size-xs);
      text-transform: uppercase;
      letter-spacing: 0.1em;
      margin-top: var(--space-xs);
    }

    .stat-value {
      font-family: var(--font-display);
      font-size: var(--font-size-4xl);
      font-weight: var(--font-weight-extrabold);
      color: var(--color-text-primary);
      text-shadow: 0 0 20px rgba(255,255,255,0.1);
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
            <div class="stat-header">
              <div class="stat-icon primary">🌐</div>
            </div>
            <div class="stat-value">${this.stats.activeSessions}</div>
            <div class="stat-label">Active Network Sessions</div>
          </div>
        </sg-card>

        <sg-card glass>
          <div class="stat-card">
            <div class="stat-header">
              <div class="stat-icon secondary">🛡️</div>
            </div>
            <div class="stat-value">${this.stats.policies}</div>
            <div class="stat-label">Active ABAC Policies</div>
          </div>
        </sg-card>

        <sg-card glass>
          <div class="stat-card">
            <div class="stat-header">
              <div class="stat-icon success">📈</div>
            </div>
            <div class="stat-value">${this.stats.auditEvents}</div>
            <div class="stat-label">Audit Events (24H)</div>
          </div>
        </sg-card>

        <sg-card glass>
          <div class="stat-card">
            <div class="stat-header">
              <div class="stat-icon warning">⚠️</div>
            </div>
            <div class="stat-value">${this.stats.alerts}</div>
            <div class="stat-label">High-Risk Alerts</div>
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
