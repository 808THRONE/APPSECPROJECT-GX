import { LitElement, html, css } from 'lit';
import { useStore } from '../store/store.js';
import './common/button.js';
import './common/card.js';
import './common/modal.js';

export class PolicyBuilder extends LitElement {
    static properties = {
        policies: { type: Array },
        showModal: { type: Boolean },
        editingPolicy: { type: Object },
    };

    static styles = css`
    :host {
      display: block;
      padding: var(--space-2xl);
      max-width: var(--container-max-width);
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--space-2xl);
    }

    h1 {
      font-family: var(--font-display);
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
    }

    .policies-grid {
      display: grid;
      gap: var(--space-lg);
    }

    .policy-card {
      padding: var(--space-xl);
      cursor: pointer;
      transition: all var(--transition-base);
    }

    .policy-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: var(--space-md);
    }

    .policy-id {
      font-family: var(--font-mono);
      font-size: var(--font-size-xs);
      color: var(--color-text-tertiary);
      background: var(--color-bg-tertiary);
      padding: var(--space-xs) var(--space-sm);
      border-radius: var(--radius-sm);
    }

    .policy-effect {
      padding: var(--space-xs) var(--space-sm);
      border-radius: var(--radius-sm);
      font-size: var(--font-size-xs);
      font-weight: var(--font-weight-semibold);
      text-transform: uppercase;
    }

    .policy-effect.permit {
      background: rgba(34, 197, 94, 0.2);
      color: var(--color-success);
    }

    .policy-effect.deny {
      background: rgba(239, 68, 68, 0.2);
      color: var(--color-error);
    }

    .policy-details {
      color: var(--color-text-secondary);
      font-size: var(--font-size-sm);
    }

    .policy-section {
      margin-top: var(--space-md);
    }

    .policy-section-title {
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-xs);
    }

    .policy-tags {
      display: flex;
      flex-wrap: wrap;
      gap: var(--space-sm);
      margin-top: var(--space-xs);
    }

    .policy-tag {
      background: var(--color-bg-tertiary);
      padding: var(--space-xs) var(--space-sm);
      border-radius: var(--radius-sm);
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
    }

    .empty-state {
      text-align: center;
      padding: var(--space-4xl) var(--space-2xl);
      color: var(--color-text-secondary);
    }

    .empty-icon {
      font-size: 64px;
      margin-bottom: var(--space-lg);
    }

    .form-group {
      margin-bottom: var(--space-lg);
    }

    label {
      display: block;
      font-weight: var(--font-weight-semibold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-sm);
    }

    input, select, textarea {
      width: 100%;
      padding: var(--space-md);
      border: 1px solid var(--color-bg-tertiary);
      border-radius: var(--radius-md);
      background: var(--color-bg-tertiary);
      color: var(--color-text-primary);
      font-family: var(--font-body);
      font-size: var(--font-size-base);
    }

    input:focus, select:focus, textarea:focus {
      outline: none;
      border-color: var(--color-primary);
    }

    textarea {
      min-height: 100px;
      resize: vertical;
    }

    .modal-actions {
      display: flex;
      gap: var(--space-md);
    }

    @media (max-width: 768px) {
      :host {
        padding: var(--space-lg);
      }

      .header {
        flex-direction: column;
        align-items: flex-start;
        gap: var(--space-md);
      }
    }
  `;

    constructor() {
        super();
        this.policies = [];
        this.showModal = false;
        this.editingPolicy = null;
        this._loadPolicies();
    }

    _loadPolicies() {
        const store = useStore.getState();
        this.policies = store.policies.length > 0 ? store.policies : this._getDemoPolicies();
    }

    _getDemoPolicies() {
        return [
            {
                policy_id: 'p001',
                effect: 'permit',
                subject: {
                    department: ['engineering', 'security'],
                    clearance_level: { min: 3 }
                },
                resource: {
                    type: 'audit_logs',
                    sensitivity: 'high'
                },
                environment: {
                    time: { start: '09:00', end: '18:00' },
                    location: { countries: ['US', 'CA'] }
                }
            },
            {
                policy_id: 'p002',
                effect: 'deny',
                subject: {
                    department: ['contractor']
                },
                resource: {
                    type: 'admin_panel'
                },
                environment: {}
            }
        ];
    }

    handleCreatePolicy() {
        this.editingPolicy = null;
        this.showModal = true;
    }

    handleEditPolicy(policy) {
        this.editingPolicy = policy;
        this.showModal = true;
    }

    handleCloseModal() {
        this.showModal = false;
        this.editingPolicy = null;
    }

    handleSavePolicy(e) {
        e.preventDefault();
        // In real app, would call API
        console.log('Save policy:', this.editingPolicy);
        this.handleCloseModal();
    }

    render() {
        return html`
      <div class="header">
        <h1>🛡️ ABAC Policy Builder</h1>
        <sg-button @sg-click="${this.handleCreatePolicy}">
          + Create Policy
        </sg-button>
      </div>

      ${this.policies.length > 0 ? html`
        <div class="policies-grid">
          ${this.policies.map(policy => html`
            <sg-card glass @click="${() => this.handleEditPolicy(policy)}">
              <div class="policy-card">
                <div class="policy-header">
                  <div class="policy-id">${policy.policy_id}</div>
                  <div class="policy-effect ${policy.effect}">${policy.effect}</div>
                </div>

                <div class="policy-section">
                  <div class="policy-section-title">Subject</div>
                  <div class="policy-tags">
                    ${policy.subject.department ? policy.subject.department.map(dept => html`
                      <div class="policy-tag">Dept: ${dept}</div>
                    `) : ''}
                    ${policy.subject.clearance_level ? html`
                      <div class="policy-tag">Clearance ≥ ${policy.subject.clearance_level.min}</div>
                    ` : ''}
                  </div>
                </div>

                <div class="policy-section">
                  <div class="policy-section-title">Resource</div>
                  <div class="policy-tags">
                    ${policy.resource.type ? html`
                      <div class="policy-tag">Type: ${policy.resource.type}</div>
                    ` : ''}
                    ${policy.resource.sensitivity ? html`
                      <div class="policy-tag">Sensitivity: ${policy.resource.sensitivity}</div>
                    ` : ''}
                  </div>
                </div>

                ${policy.environment.time || policy.environment.location ? html`
                  <div class="policy-section">
                    <div class="policy-section-title">Environment</div>
                    <div class="policy-tags">
                      ${policy.environment.time ? html`
                        <div class="policy-tag">
                          ${policy.environment.time.start} - ${policy.environment.time.end}
                        </div>
                      ` : ''}
                      ${policy.environment.location ? html`
                        <div class="policy-tag">
                          ${policy.environment.location.countries.join(', ')}
                        </div>
                      ` : ''}
                    </div>
                  </div>
                ` : ''}
              </div>
            </sg-card>
          `)}
        </div>
      ` : html`
        <sg-card glass>
          <div class="empty-state">
            <div class="empty-icon">📋</div>
            <h2>No Policies Yet</h2>
            <p>Create your first ABAC policy to get started</p>
            <sg-button @sg-click="${this.handleCreatePolicy}">
              Create First Policy
            </sg-button>
          </div>
        </sg-card>
      `}

      <sg-modal
        ?open="${this.showModal}"
        title="${this.editingPolicy ? 'Edit Policy' : 'Create New Policy'}"
        @sg-close="${this.handleCloseModal}"
      >
        <form @submit="${this.handleSavePolicy}">
          <div class="form-group">
            <label>Policy ID</label>
            <input type="text" placeholder="p003" required>
          </div>

          <div class="form-group">
            <label>Effect</label>
            <select required>
              <option value="permit">Permit</option>
              <option value="deny">Deny</option>
            </select>
          </div>

          <div class="form-group">
            <label>Subject (JSON)</label>
            <textarea placeholder='{"department": ["engineering"]}'></textarea>
          </div>

          <div class="form-group">
            <label>Resource (JSON)</label>
            <textarea placeholder='{"type": "audit_logs"}'></textarea>
          </div>

          <div slot="footer" class="modal-actions">
            <sg-button variant="ghost" @sg-click="${this.handleCloseModal}">
              Cancel
            </sg-button>
            <sg-button type="submit">
              ${this.editingPolicy ? 'Update' : 'Create'} Policy
            </sg-button>
          </div>
        </form>
      </sg-modal>
    `;
    }
}

customElements.define('policy-builder', PolicyBuilder);
