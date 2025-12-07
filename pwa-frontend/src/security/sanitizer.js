import DOMPurify from 'dompurify';

/**
 * XSS Sanitization using DOMPurify
 * Wrapper for consistent sanitization across the app
 */

/**
 * Sanitize HTML content
 */
export function sanitizeHtml(dirty) {
    return DOMPurify.sanitize(dirty, {
        ALLOWED_TAGS: [
            'p', 'br', 'strong', 'em', 'u', 'a', 'ul', 'ol', 'li',
            'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'code', 'pre',
            'blockquote', 'span', 'div'
        ],
        ALLOWED_ATTR: ['href', 'target', 'rel', 'class'],
        ALLOW_DATA_ATTR: false,
    });
}

/**
 * Sanitize text (strip all HTML)
 */
export function sanitizeText(dirty) {
    return DOMPurify.sanitize(dirty, {
        ALLOWED_TAGS: [],
        ALLOWED_ATTR: [],
    });
}

/**
 * Sanitize URL
 */
export function sanitizeUrl(url) {
    // Only allow http, https, and mailto protocols
    const allowedProtocols = ['http:', 'https:', 'mailto:'];

    try {
        const parsed = new URL(url);
        if (allowedProtocols.includes(parsed.protocol)) {
            return DOMPurify.sanitize(url);
        }
    } catch (error) {
        // Invalid URL
    }

    return '#';
}

/**
 * Configure DOMPurify hooks
 */
DOMPurify.addHook('afterSanitizeAttributes', function (node) {
    // Add rel="noopener noreferrer" to external links
    if (node.tagName === 'A' && node.hasAttribute('href')) {
        const href = node.getAttribute('href');
        if (href.startsWith('http') && !href.includes(window.location.hostname)) {
            node.setAttribute('target', '_blank');
            node.setAttribute('rel', 'noopener noreferrer');
        }
    }
});

export default {
    sanitizeHtml,
    sanitizeText,
    sanitizeUrl,
};
