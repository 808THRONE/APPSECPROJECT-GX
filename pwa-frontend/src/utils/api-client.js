/**
 * API Client with Token Management
 * Fetch wrapper with automatic token refresh and error handling
 */

import { useStore } from '../store/store.js';
import { refreshAccessToken } from './oauth-client.js';

// Use proxy paths for local development, environment variables for production
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

/**
 * Make authenticated API request
 */
export async function apiRequest(endpoint, options = {}) {
    const { accessToken, refreshToken } = useStore.getState();

    const config = {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
    };

    // Add authorization header if token exists
    if (accessToken) {
        config.headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

    try {
        let response = await fetch(url, config);

        // Handle 401 Unauthorized - try to refresh token
        if (response.status === 401 && refreshToken) {
            try {
                const newTokens = await refreshAccessToken(refreshToken);
                useStore.getState().setTokens(newTokens.accessToken, newTokens.refreshToken);

                // Retry request with new token
                config.headers['Authorization'] = `Bearer ${newTokens.accessToken}`;
                response = await fetch(url, config);
            } catch (refreshError) {
                // Refresh failed, clear tokens and redirect to login
                useStore.getState().clearTokens();
                useStore.getState().setCurrentView('login');
                throw new Error('Session expired. Please login again.');
            }
        }

        // Handle other errors
        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Request failed' }));
            throw new Error(error.message || `HTTP ${response.status}`);
        }

        // Parse JSON response
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        }

        return response;
    } catch (error) {
        console.error('[API Error]', endpoint, error);
        throw error;
    }
}

/**
 * GET request
 */
export async function get(endpoint) {
    return apiRequest(endpoint, { method: 'GET' });
}

/**
 * POST request
 */
export async function post(endpoint, data) {
    return apiRequest(endpoint, {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

/**
 * PUT request
 */
export async function put(endpoint, data) {
    return apiRequest(endpoint, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

/**
 * DELETE request
 */
export async function del(endpoint) {
    return apiRequest(endpoint, { method: 'DELETE' });
}

/**
 * Upload file
 */
export async function uploadFile(endpoint, file, additionalData = {}) {
    const { accessToken } = useStore.getState();

    const formData = new FormData();
    formData.append('file', file);

    Object.keys(additionalData).forEach(key => {
        formData.append(key, additionalData[key]);
    });

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${accessToken}`,
        },
        body: formData,
    });

    if (!response.ok) {
        throw new Error('Upload failed');
    }

    return response.json();
}
