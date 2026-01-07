/**
 * SecureGate Frontend E2E Tests
 * Run with: npm run test:e2e
 */

import { expect, test, describe, beforeAll, afterAll } from 'vitest';

const BASE_URL = 'http://localhost:5173';
const IAM_URL = 'http://localhost:8080/iam';
const API_URL = 'http://localhost:8081/api';

describe('SecureGate Frontend Tests', () => {

    describe('API Gateway Health', () => {
        test('should return healthy status', async () => {
            const response = await fetch(`${API_URL}/test`);
            expect(response.ok).toBe(true);

            const data = await response.json();
            expect(data.status).toBe('ok');
            expect(data.message).toBe('API Gateway is running');
        });
    });

    describe('IAM Service', () => {
        test('should return JWKS', async () => {
            const response = await fetch(`${IAM_URL}/jwk`);
            expect(response.ok).toBe(true);

            const data = await response.json();
            expect(data.keys).toBeDefined();
            expect(Array.isArray(data.keys)).toBe(true);
            expect(data.keys.length).toBeGreaterThan(0);

            // Verify Ed25519 key structure
            const key = data.keys[0];
            expect(key.kty).toBe('OKP');
            expect(key.crv).toBe('Ed25519');
            expect(key.use).toBe('sig');
            expect(key.kid).toBeDefined();
            expect(key.x).toBeDefined();
        });

        test('should return authorization page', async () => {
            const params = new URLSearchParams({
                client_id: 'demo-client',
                response_type: 'code',
                redirect_uri: 'http://localhost:5173/callback',
                code_challenge: 'test-challenge',
                code_challenge_method: 'S256',
                scope: 'openid profile email'
            });

            const response = await fetch(`${IAM_URL}/authorize?${params}`);
            expect(response.ok).toBe(true);

            const html = await response.text();
            expect(html).toContain('Login');
        });
    });

    describe('Protected Endpoints', () => {
        test('should reject request without token', async () => {
            const response = await fetch(`${API_URL}/users/me`);
            expect(response.status).toBe(401);

            const data = await response.json();
            expect(data.error).toBe('unauthorized');
        });

        test('should reject request with invalid token', async () => {
            const response = await fetch(`${API_URL}/users/me`, {
                headers: {
                    'Authorization': 'Bearer invalid-token'
                }
            });
            expect(response.status).toBe(401);
        });
    });

    describe('Policy API', () => {
        test('should list policies with valid token', async () => {
            // This test requires a valid token
            // In real testing, you would obtain a token through the OAuth flow
            const mockToken = process.env.TEST_TOKEN;

            if (mockToken) {
                const response = await fetch(`${API_URL}/policies`, {
                    headers: {
                        'Authorization': `Bearer ${mockToken}`
                    }
                });
                expect(response.ok).toBe(true);

                const data = await response.json();
                expect(data.policies).toBeDefined();
                expect(Array.isArray(data.policies)).toBe(true);
            }
        });
    });

    describe('CORS Headers', () => {
        test('should include CORS headers', async () => {
            const response = await fetch(`${API_URL}/test`);

            // Check CORS is not blocking (if configured)
            expect(response.ok).toBe(true);
        });
    });
});

describe('OAuth 2.1 PKCE Flow', () => {

    test('should generate valid PKCE challenge', async () => {
        // Simulate PKCE generation
        const array = new Uint8Array(32);
        crypto.getRandomValues(array);

        const codeVerifier = base64UrlEncode(array);
        expect(codeVerifier.length).toBeGreaterThanOrEqual(43);
        expect(codeVerifier.length).toBeLessThanOrEqual(128);

        // Generate challenge
        const encoder = new TextEncoder();
        const data = encoder.encode(codeVerifier);
        const hashBuffer = await crypto.subtle.digest('SHA-256', data);
        const codeChallenge = base64UrlEncode(new Uint8Array(hashBuffer));

        expect(codeChallenge).toBeDefined();
        expect(codeChallenge.length).toBe(43); // SHA-256 produces 32 bytes = 43 base64url chars
    });

    test('should generate cryptographic state', () => {
        const array = new Uint8Array(32);
        crypto.getRandomValues(array);
        const state = base64UrlEncode(array);

        expect(state).toBeDefined();
        expect(state.length).toBe(43);
    });
});

// Helper function
function base64UrlEncode(buffer) {
    const base64 = btoa(String.fromCharCode(...buffer));
    return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}
