import { LitElement, html, css } from 'lit';

export class SgButton extends LitElement {
    static properties = {
        variant: { type: String },
        size: { type: String },
        disabled: { type: Boolean },
        loading: { type: Boolean },
        fullWidth: { type: Boolean },
    };

    static styles = css`
    :host {
      display: inline-block;
    }

    button {
      font-family: var(--font-display);
      font-weight: var(--font-weight-semibold);
      border: none;
      border-radius: var(--radius-lg);
      cursor: pointer;
      transition: all var(--transition-base);
      position: relative;
      overflow: hidden;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-sm);
    }

    button:focus-visible {
      outline: 2px solid var(--color-primary-light);
      outline-offset: 2px;
    }

    button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    /* Variants */
    .primary {
      background: var(--gradient-primary);
      color: var(--color-text-primary);
      box-shadow: var(--shadow-md);
    }

    .primary:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: var(--shadow-lg), var(--shadow-glow);
    }

    .secondary {
      background: transparent;
      color: var(--color-primary);
      border: 2px solid var(--color-primary);
    }

    .secondary:hover:not(:disabled) {
      background: var(--color-primary);
      color: var(--color-text-primary);
    }

    .ghost {
      background: transparent;
      color: var(--color-text-secondary);
    }

    .ghost:hover:not(:disabled) {
      background: var(--color-bg-tertiary);
      color: var(--color-text-primary);
    }

    .danger {
      background: var(--color-error);
      color: var(--color-text-primary);
    }

    .danger:hover:not(:disabled) {
      filter: brightness(1.2);
    }

    /* Sizes */
    .sm {
      padding: var(--space-sm) var(--space-md);
      font-size: var(--font-size-sm);
    }

    .md {
      padding: var(--space-md) var(--space-lg);
      font-size: var(--font-size-base);
    }

    .lg {
      padding: var(--space-lg) var(--space-xl);
      font-size: var(--font-size-lg);
    }

    .full-width {
      width: 100%;
    }

    .loading-spinner {
      width: 16px;
      height: 16px;
      border: 2px solid currentColor;
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `;

    constructor() {
        super();
        this.variant = 'primary';
        this.size = 'md';
        this.disabled = false;
        this.loading = false;
        this.fullWidth = false;
    }

    render() {
        const classes = [
            this.variant,
            this.size,
            this.fullWidth ? 'full-width' : '',
        ].filter(Boolean).join(' ');

        return html`
      <button
        class="${classes}"
        ?disabled="${this.disabled || this.loading}"
        @click="${this._handleClick}"
      >
        ${this.loading ? html`<div class="loading-spinner"></div>` : ''}
        <slot></slot>
      </button>
    `;
    }

    _handleClick(e) {
        if (!this.disabled && !this.loading) {
            this.dispatchEvent(new CustomEvent('sg-click', {
                bubbles: true,
                composed: true,
            }));
        }
    }
}

customElements.define('sg-button', SgButton);
