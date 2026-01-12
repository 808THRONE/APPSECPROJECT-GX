// SecureGate Frontend Configuration
// Environment-based settings for production deployment

const getEnvVar = (key: string, defaultValue: string): string => {
    // Vite exposes env vars with VITE_ prefix at build time
    // Access via import.meta.env
    if (typeof import.meta !== 'undefined' && import.meta.env) {
        const value = (import.meta.env as Record<string, string | undefined>)[key];
        if (value) return value;
    }
    return defaultValue;
};

export const config = {
    // API Gateway URL - all requests route through the gateway
    API_BASE_URL: getEnvVar('VITE_API_URL', '/api-gateway/api/v1'),

    // IAM Service URL (for OAuth flows that need direct access)
    IAM_URL: getEnvVar('VITE_IAM_URL', '/iam-service/api'),

    // Stego Module URL (for direct stego operations if needed)
    STEGO_URL: getEnvVar('VITE_STEGO_URL', '/stego-module/api'),

    // Feature flags
    STEGO_ENABLED: getEnvVar('VITE_STEGO_ENABLED', 'true') === 'true',

    // OAuth Configuration
    OAUTH: {
        CLIENT_ID: getEnvVar('VITE_OAUTH_CLIENT_ID', 'securegate-frontend'),
        REDIRECT_URI: typeof window !== 'undefined'
            ? `${window.location.origin}/callback`
            : 'http://localhost/callback',
        SCOPES: 'openid profile email'
    },

    // Application metadata
    APP: {
        NAME: 'SecureGate',
        VERSION: '1.0.0',
        ENVIRONMENT: getEnvVar('VITE_ENV', 'development')
    }
};

export default config;
