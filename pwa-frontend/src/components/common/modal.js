import { LitElement, html, css } from 'lit';

export class SgModal extends LitElement {
    static properties = {
        open: { type: Boolean },
        title: { type: String },
    };

    static styles = css`
    :host {
      display: none;
    }

    :host([open]) {
      display: block;
    }

    .overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: var(--z-modal-backdrop);
      animation: fadeIn 0.2s ease-out;
    }

    .modal {
      background: var(--color-bg-secondary);
      border-radius: var(--radius-2xl);
      box-shadow: var(--shadow-xl);
      max-width: 600px;
      width: 90%;
      max-height: 90vh;
      overflow: hidden;
      z-index: var(--z-modal);
      animation: fadeInUp 0.3s ease-out;
    }

    .header {
      padding: var(--space-lg);
      border-bottom: 1px solid var(--color-bg-tertiary);
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .title {
      font-family: var(--font-display);
      font-size: var(--font-size-xl);
      font-weight: var(--font-weight-bold);
      color: var(--color-text-primary);
      margin: 0;
    }

    .close-btn {
      background: none;
      border: none;
      color: var(--color-text-secondary);
      cursor: pointer;
      font-size: var(--font-size-2xl);
      line-height: 1;
      padding: var(--space-sm);
      transition: color var(--transition-fast);
    }

    .close-btn:hover {
      color: var(--color-text-primary);
    }

    .body {
      padding: var(--space-lg);
      max-height: calc(90vh - 140px);
      overflow-y: auto;
    }

    .footer {
      padding: var(--space-lg);
      border-top: 1px solid var(--color-bg-tertiary);
      display: flex;
      gap: var(--space-md);
      justify-content: flex-end;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
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
  `;

    constructor() {
        super();
        this.open = false;
        this.title = '';
    }

    render() {
        if (!this.open) return html``;

        return html`
      <div class="overlay" @click="${this._handleOverlayClick}">
        <div class="modal" @click="${this._stopPropagation}">
          <div class="header">
            <h2 class="title">${this.title}</h2>
            <button class="close-btn" @click="${this.close}">×</button>
          </div>
          <div class="body">
            <slot></slot>
          </div>
          <div class="footer">
            <slot name="footer"></slot>
          </div>
        </div>
      </div>
    `;
    }

    close() {
        this.open = false;
        this.dispatchEvent(new CustomEvent('sg-close', {
            bubbles: true,
            composed: true,
        }));
    }

    _handleOverlayClick() {
        this.close();
    }

    _stopPropagation(e) {
        e.stopPropagation();
    }
}

customElements.define('sg-modal', SgModal);
