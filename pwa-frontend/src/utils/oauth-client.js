/**
 * OAuth 2.1 PKCE Client
 * Implements OAuth 2.1 with PKCE for secure authentication
 */

const OAUTH_CONFIG = {
    authorizationEndpoint: import.meta.env.VITE_IAM_URL || 'https://iam.yourdomain.me/oauth/authorize',
    tokenEndpoint: import.meta.env.VITE_IAM_URL || 'https://iam.yourdomain.me/oauth/token',
    clientId: import.meta.env.VITE_OAUTH_CLIENT_ID || 'securegate-pwa',
    redirectUri: import.meta.env.VITE_OAUTH_REDIRECT_URI || window.location.origin + '/callback',
    scope: 'openid profile email',
};

/**
 * Generate PKCE code verifier and challenge
 */
async function generatePKCE() {
    // Generate code verifier (43-128 characters)
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    const codeVerifier = base64UrlEncode(array);

    // Generate code challenge (SHA-256 hash of verifier)
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const codeChallenge = base64UrlEncode(new Uint8Array(hashBuffer));

    return {
        codeVerifier,
        codeChallenge,
    };
}

/**
 * Base64 URL encoding
 */
function base64UrlEncode(buffer) {
    const base64 = btoa(String.fromCharCode(...buffer));
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

/**
 * Generate cryptographic state parameter
 */
function generateState() {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return base64UrlEncode(array);
}

/**
 * Start OAuth 2.1 authorization flow with PKCE
 */
export async function startAuthorizationFlow() {
    const { codeVerifier, codeChallenge } = await generatePKCE();
    const state = generateState();

    // Store PKCE verifier and state in sessionStorage
    sessionStorage.setItem('oauth_code_verifier', codeVerifier);
    sessionStorage.setItem('oauth_state', state);

    // Build authorization URL
    const params = new URLSearchParams({
        response_type: 'code',
        client_id: OAUTH_CONFIG.clientId,
        redirect_uri: OAUTH_CONFIG.redirectUri,
        scope: OAUTH_CONFIG.scope,
        state: state,
        code_challenge: codeChallenge,
        code_challenge_method: 'S256',
    });

    const authUrl = `${OAUTH_CONFIG.authorizationEndpoint}?${params.toString()}`;

    // Redirect to authorization endpoint
    window.location.href = authUrl;
}

/**
 * Handle OAuth callback
 */
export async function handleOAuthCallback() {
    const urlParams = new URLSearchParams(window.location.search);
    const code = urlParams.get('code');
    const state = urlParams.get('state');
    const error = urlParams.get('error');

    // Check for errors
    if (error) {
        throw new Error(`OAuth error: ${error}`);
    }

    // Validate state parameter (CSRF protection)
    const storedState = sessionStorage.getItem('oauth_state');
    if (!state || state !== storedState) {
        throw new Error('Invalid state parameter');
    }

    // Retrieve code verifier
    const codeVerifier = sessionStorage.getItem('oauth_code_verifier');
    if (!codeVerifier) {
        throw new Error('Missing code verifier');
    }

    // Exchange authorization code for tokens
    const tokens = await exchangeCodeForTokens(code, codeVerifier);

    // Clean up
    sessionStorage.removeItem('oauth_code_verifier');
    sessionStorage.removeItem('oauth_state');

    return tokens;
}

/**
 * Exchange authorization code for access/refresh tokens
 */
async function exchangeCodeForTokens(code, codeVerifier) {
    const params = new URLSearchParams({
        grant_type: 'authorization_code',
        code: code,
        redirect_uri: OAUTH_CONFIG.redirectUri,
        client_id: OAUTH_CONFIG.clientId,
        code_verifier: codeVerifier,
    });

    const response = await fetch(OAUTH_CONFIG.tokenEndpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString(),
    });

    if (!response.ok) {
        const error = await response.json();
        throw new Error(`Token exchange failed: ${error.error_description || error.error}`);
    }

    const tokens = await response.json();
    return {
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        expiresIn: tokens.expires_in,
        idToken: tokens.id_token,
    };
}

/**
 * Refresh access token using refresh token
 */
export async function refreshAccessToken(refreshToken) {
    const params = new URLSearchParams({
        grant_type: 'refresh_token',
        refresh_token: refreshToken,
        client_id: OAUTH_CONFIG.clientId,
    });

    const response = await fetch(OAUTH_CONFIG.tokenEndpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString(),
    });

    if (!response.ok) {
        throw new Error('Token refresh failed');
    }

    const tokens = await response.json();
    return {
        accessToken: tokens.access_token,
        refreshToken: tokens.refresh_token,
        expiresIn: tokens.expires_in,
    };
}

/**
 * Parse JWT (client-side only for display, NEVER for validation)
 */
export function parseJWT(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split('')
                .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(jsonPayload);
    } catch (error) {
        console.error('Failed to parse JWT:', error);
        return null;
    }
}
