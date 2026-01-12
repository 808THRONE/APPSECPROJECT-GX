import { config } from '../config';

// PKCE Helper Functions
export async function generateCodeVerifier() {
    const array = new Uint8Array(32);
    crypto.getRandomValues(array);
    return base64UrlEncode(array);
}

export async function generateCodeChallenge(verifier: string) {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const hash = await crypto.subtle.digest('SHA-256', data);
    return base64UrlEncode(new Uint8Array(hash));
}

function base64UrlEncode(array: Uint8Array) {
    let str = '';
    array.forEach(byte => {
        str += String.fromCharCode(byte);
    });
    return btoa(str)
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}

export const AuthService = {
    async login() {
        const codeVerifier = await generateCodeVerifier();
        const codeChallenge = await generateCodeChallenge(codeVerifier);

        // Store verifier for callback
        sessionStorage.setItem('code_verifier', codeVerifier);

        const params = new URLSearchParams({
            response_type: 'code',
            client_id: config.OAUTH.CLIENT_ID,
            redirect_uri: config.OAUTH.REDIRECT_URI,
            scope: config.OAUTH.SCOPES,
            code_challenge: codeChallenge,
            code_challenge_method: 'S256',
            state: crypto.randomUUID()
        });

        window.location.href = `${config.IAM_URL}/oauth2/authorize?${params.toString()}`;
    },

    async register() {
        const codeVerifier = await generateCodeVerifier();
        const codeChallenge = await generateCodeChallenge(codeVerifier);
        sessionStorage.setItem('code_verifier', codeVerifier);

        const params = new URLSearchParams({
            response_type: 'code',
            client_id: config.OAUTH.CLIENT_ID,
            redirect_uri: config.OAUTH.REDIRECT_URI,
            scope: config.OAUTH.SCOPES,
            code_challenge: codeChallenge,
            code_challenge_method: 'S256',
            state: crypto.randomUUID()
        });

        window.location.href = `${config.IAM_URL}/oauth2/register?${params.toString()}`;
    },

    async handleCallback(code: string) {
        const codeVerifier = sessionStorage.getItem('code_verifier');
        if (!codeVerifier) throw new Error('No code verifier found');

        const params = new URLSearchParams({
            grant_type: 'authorization_code',
            code: code,
            redirect_uri: config.OAUTH.REDIRECT_URI,
            client_id: config.OAUTH.CLIENT_ID,
            code_verifier: codeVerifier
        });

        const response = await fetch(`${config.IAM_URL}/oauth2/token`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            body: params
        });

        if (!response.ok) throw new Error('Token exchange failed');

        const data = await response.json();
        if (data.username) {
            console.log('Auth hint set in localStorage for:', data.username);
            localStorage.setItem('auth_hint', 'true');
            localStorage.setItem('username', data.username);
        } else {
            console.warn('Token exchange response missing username');
        }
        return data;
    },

    async refreshToken() {
        const response = await fetch(`${config.IAM_URL}/oauth2/refresh`, {
            method: 'POST',
            credentials: 'include'
        });

        if (!response.ok) {
            this.logout();
            throw new Error('Token refresh failed');
        }

        return await response.json();
    },

    async logout() {
        try {
            await fetch(`${config.IAM_URL}/oauth2/logout`, {
                method: 'POST',
                credentials: 'include'
            });
        } catch (e) {
            console.error('Logout request failed', e);
        }
        localStorage.clear();
        sessionStorage.clear();
        console.log('Cleared session and redirecting to /');
        window.location.href = '/';
    },

    getToken(): string | null {
        // Can't get HttpOnly token from JS
        return null;
    },

    isAuthenticated(): boolean {
        // Hint-based. Real check happens on 401.
        const hint = localStorage.getItem('auth_hint');
        console.log('Checking isAuthenticated. Hint:', hint);
        return hint === 'true';
    },

    async getUserInfo() {
        const response = await fetch(`${config.IAM_URL}/oauth2/userinfo`, {
            credentials: 'include'
        });
        if (!response.ok) return null;
        return await response.json();
    }
};

export default AuthService;
