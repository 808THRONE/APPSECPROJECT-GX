const express = require('express');
const cors = require('cors');

const app = express();
const PORT = 9000;

app.use(cors());
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// In-memory store for codes (cleanup in production!)
const authCodes = new Map();

// Helper to generate a dummy JWT
function generateIdToken(nonce = 'nonce') {
    const header = {
        alg: 'HS256',
        typ: 'JWT'
    };
    
    // Valid for 1 hour
    const now = Math.floor(Date.now() / 1000);
    const payload = {
        sub: 'mock-user-123',
        name: 'Mock User',
        email: 'user@example.com',
        aud: 'securegate-pwa',
        iss: 'http://localhost:9000',
        iat: now,
        exp: now + 3600,
        nonce: nonce
    };

    const encode = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
    const signature = 'mock-signature'; // We don't verify signature in client for this exercise

    return \\.\.\\;
}

// Authorization Endpoint
app.get('/authorize', (req, res) => {
    const { 
        response_type, 
        client_id, 
        redirect_uri, 
        state, 
        code_challenge, 
        code_challenge_method 
    } = req.query;

    console.log('Authorize request:', req.query);

    if (response_type !== 'code') {
        return res.status(400).send('Unsupported response_type');
    }

    if (!code_challenge) {
        return res.status(400).send('Missing code_challenge');
    }

    // Auto-approve and redirect back
    const code = Math.random().toString(36).substring(7);
    
    // Store code with verifier requirement
    authCodes.set(code, {
        clientId: client_id,
        redirectUri: redirect_uri,
        codeChallenge: code_challenge,
        codeChallengeMethod: code_challenge_method
    });

    const redirectUrl = new URL(redirect_uri);
    redirectUrl.searchParams.set('code', code);
    if (state) redirectUrl.searchParams.set('state', state);

    console.log(\Redirecting to: \\);
    res.redirect(redirectUrl.toString());
});

// Token Endpoint
app.post('/token', (req, res) => {
    const { 
        grant_type, 
        code, 
        redirect_uri, 
        client_id, 
        code_verifier,
        refresh_token 
    } = req.body;

    console.log('Token request:', req.body);

    if (grant_type === 'authorization_code') {
        const authData = authCodes.get(code);

        if (!authData) {
            return res.status(400).json({ error: 'invalid_grant', error_description: 'Invalid code' });
        }

        // Verify needed params (simplified for mock)
        // In real PKCE, we hash code_verifier and compare with code_challenge

        const accessToken = 'mock_access_token_' + Math.random().toString(36).substring(7);
        const refreshToken = 'mock_refresh_token_' + Math.random().toString(36).substring(7);
        const idToken = generateIdToken();

        authCodes.delete(code); // One-time use
        return res.json({
            access_token: accessToken,
            token_type: 'Bearer',
            expires_in: 3600,
            refresh_token: refreshToken,
            id_token: idToken
        });
    }

    if (grant_type === 'refresh_token') {
        // Just return new tokens
        return res.json({
            access_token: 'mock_refreshed_access_token_' + Math.random().toString(36).substring(7),
            token_type: 'Bearer',
            expires_in: 3600,
            refresh_token: 'mock_new_refresh_token_' + Math.random().toString(36).substring(7)
        });
    }

    res.status(400).json({ error: 'unsupported_grant_type' });
});

app.listen(PORT, () => {
    console.log(\Mock IAM Server running on http://localhost:\\);
});
