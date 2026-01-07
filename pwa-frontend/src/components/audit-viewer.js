import { LitElement, html, css } from 'lit';
import { useStore } from '../store/store.js';
import { websocketManager } from '../websocket/websocket-manager.js';
import './common/card.js';

export class AuditViewer extends LitElement {
    static properties = {
        logs: { type: Array },
        filter: { type: String },
        connected: { type: Boolean },
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

    h1 {
      font-family: var(--font-display);
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-sm);
    }

    .connection-status {
      display: inline-flex;
      align-items: center;
      gap: var(--space-sm);
      padding: var(--space-sm) var(--space-md);
      border-radius: var(--radius-full);
      font-size: var(--font-size-sm);
      font-weight: var(--font-weight-medium);
    }

    .connection-status.connected {
      background: rgba(34, 197, 94, 0.2);
      color: var(--color-success);
    }

    .connection-status.disconnected {
      background: rgba(239, 68, 68, 0.2);
      color: var(--color-error);
    }

    .status-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: currentColor;
    }

    .status-indicator.pulse {
      animation: pulse 2s ease-in-out infinite;
    }

    .filters {
      margin-bottom: var(--space-lg);
    }

    .filter-input {
      width: 100%;
      max-width: 400px;
      padding: var(--space-md);
      border: 1px solid var(--color-bg-tertiary);
      border-radius: var(--radius-md);
      background: var(--color-bg-tertiary);
      color: var(--color-text-primary);
      font-family: var(--font-body);
    }

    .logs-container {
      display: grid;
      gap: var(--space-md);
      max-height: 70vh;
      overflow-y: auto;
    }

    .log-card {
      padding: var(--space-lg);
      font-family: var(--font-mono);
      font-size: var(--font-size-sm);
      animation: fadeInUp 0.3s ease-out;
    }

    .log-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-sm);
    }

    .log-type {
      padding: var(--space-xs) var(--space-sm);
      border-radius: var(--radius-sm);
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-semibold);
      text-transform: uppercase;
    }

    .log-type.auth {
      background: rgba(59, 130, 246, 0.2);
      color: var(--color-primary);
    }

    .log-type.access {
      background: rgba(34, 197, 94, 0.2);
      color: var(--color-success);
    }

    .log-type.error {
      background: rgba(239, 68, 68, 0.2);
      color: var(--color-error);
    }

    .log-type.security {
      background: rgba(249, 115, 22, 0.2);
      color: var(--color-warning);
    }

    .log-timestamp {
      color: var(--color-text-tertiary);
      font-size: var(--font-size-xs);
    }

    .log-message {
      color: var(--color-text-primary);
      margin-bottom: var(--space-sm);
      word-break: break-word;
    }

    .log-details {
      color: var(--color-text-secondary);
      font-size: var(--font-size-xs);
      display: grid;
      gap: var(--space-xs);
    }

    .empty-state {
      text-align: center;
      padding: var(--space-4xl);
      color: var(--color-text-secondary);
    }

    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes pulse {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: 0.5;
      }
    }

    @media (max-width: 768px) {
      :host {
        padding: var(--space-lg);
      }
    }
  `;

    constructor() {
        super();
        this.logs = [];
        this.filter = '';
        this.connected = false;
        this._loadDemoLogs();
    }

    connectedCallback() {
        super.connectedCallback();
        // Try to connect to WebSocket (will fail gracefully if backend not available)
        this._connectWebSocket();
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        websocketManager.disconnect();
    }

    _connectWebSocket() {
        const wsUrl = import.meta.env.VITE_WS_URL || 'wss://api.yourdomain.me/audit';
        const accessToken = useStore.getState().accessToken;

        websocketManager.on('connected', () => {
            this.connected = true;
        });

        websocketManager.on('disconnected', () => {
            this.connected = false;
        });

        websocketManager.on('audit-event', (event) => {
            this.logs = [event, ...this.logs].slice(0, 100);
        });

        // Only connect if we have token (backend available)
        if (accessToken) {
            websocketManager.connect(wsUrl, accessToken);
        }
    }

    _loadDemoLogs() {
        this.logs = [
            {
                id: '001',
                type: 'auth',
                message: 'User authentication successful',
                timestamp: new Date().toISOString(),
                user: 'admin@securegate.com',
                ip: '192.168.1.100',
                device: 'Chrome on Windows'
            },
            {
                id: '002',
                type: 'access',
                message: 'ABAC policy evaluation: PERMIT',
                timestamp: new Date(Date.now() - 60000).toISOString(),
                user: 'admin@securegate.com',
                resource: 'audit_logs',
                policy: 'p001'
            },
            {
                id: '003',
                type: 'security',
                message: 'Token replay attempt detected',
                timestamp: new Date(Date.now() - 120000).toISOString(),
                ip: '10.0.0.1',
                jti: 'abc123...'
            },
            {
                id: '004',
                type: 'error',
                message: 'Failed authentication attempt',
                timestamp: new Date(Date.now() - 180000).toISOString(),
                ip: '192.168.1.200',
                reason: 'Invalid credentials'
            }
        ];
    }

    handleFilterChange(e) {
        this.filter = e.target.value.toLowerCase();
    }

    get filteredLogs() {
        if (!this.filter) return this.logs;
        return this.logs.filter(log =>
            log.message.toLowerCase().includes(this.filter) ||
            log.type.toLowerCase().includes(this.filter)
        );
    }

    formatTimestamp(timestamp) {
        const date = new Date(timestamp);
        return date.toLocaleString();
    }

    render() {
        return html`
      <div class="header">
        <h1>📝 Audit Logs</h1>
        <div class="connection-status ${this.connected ? 'connected' : 'disconnected'}">
          <div class="status-indicator ${this.connected ? 'pulse' : ''}"></div>
          ${this.connected ? 'Real-time Connected' : 'Offline Mode'}
        </div>
      </div>

      <div class="filters">
        <input
          type="text"
          class="filter-input"
          placeholder="🔍 Filter logs..."
          @input="${this.handleFilterChange}"
        >
      </div>

      ${this.filteredLogs.length > 0 ? html`
        <div class="logs-container">
          ${this.filteredLogs.map(log => html`
            <sg-card glass>
              <div class="log-card">
                <div class="log-header">
                  <div class="log-type ${log.type}">${log.type}</div>
                  <div class="log-timestamp">${this.formatTimestamp(log.timestamp)}</div>
                </div>
                <div class="log-message">${log.message}</div>
                <div class="log-details">
                  ${log.user ? html`<div>User: ${log.user}</div>` : ''}
                  ${log.ip ? html`<div>IP: ${log.ip}</div>` : ''}
                  ${log.device ? html`<div>Device: ${log.device}</div>` : ''}
                  ${log.resource ? html`<div>Resource: ${log.resource}</div>` : ''}
                  ${log.policy ? html`<div>Policy: ${log.policy}</div>` : ''}
                  ${log.jti ? html`<div>JTI: ${log.jti}</div>` : ''}
                  ${log.reason ? html`<div>Reason: ${log.reason}</div>` : ''}
                </div>
              </div>
            </sg-card>
          `)}
        </div>
      ` : html`
        <sg-card glass>
          <div class="empty-state">
            <div style="font-size: 64px; margin-bottom: var(--space-lg);">📭</div>
            <h2>No Logs Found</h2>
            <p>No audit events match your filter</p>
          </div>
        </sg-card>
      `}
    `;
    }
}

customElements.define('audit-viewer', AuditViewer);
