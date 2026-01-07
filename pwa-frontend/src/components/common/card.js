import { LitElement, html, css } from 'lit';

export class SgCard extends LitElement {
    static properties = {
        glass: { type: Boolean },
        padding: { type: String },
    };

    static styles = css`
    :host {
      display: block;
    }

    .card {
      background: var(--color-bg-secondary);
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-md);
      overflow: hidden;
      transition: all var(--transition-base);
    }

    .card.glass {
      background: var(--glass-bg);
      backdrop-filter: var(--glass-blur);
      -webkit-backdrop-filter: var(--glass-blur);
      border: 1px solid var(--glass-border);
    }

    .card:hover {
      box-shadow: var(--shadow-lg);
      transform: translateY(-2px);
    }

    .content {
      padding: var(--space-lg);
    }

    .content.sm {
      padding: var(--space-md);
    }

    .content.md {
      padding: var(--space-lg);
    }

    .content.lg {
      padding: var(--space-xl);
    }
  `;

    constructor() {
        super();
        this.glass = false;
        this.padding = 'md';
    }

    render() {
        const cardClasses = this.glass ? 'card glass' : 'card';
        const contentClasses = `content ${this.padding}`;

        return html`
      <div class="${cardClasses}">
        <div class="${contentClasses}">
          <slot></slot>
        </div>
      </div>
    `;
    }
}

customElements.define('sg-card', SgCard);
