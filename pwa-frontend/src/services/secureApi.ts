import { config } from '../config';
import StegoService from './stegoService';

export const SecureApi = {
    async request(url: string, options: RequestInit = {}, responseType: 'json' | 'text' = 'json'): Promise<any> {
        let body = options.body;

        // Handle request stego embedding
        if (config.STEGO_ENABLED && body && typeof body === 'string' && !url.includes('/stego/')) {
            try {
                const { stego, length } = await StegoService.secureTransmit(body);
                body = JSON.stringify({ stego, length });
            } catch (error) {
                console.error('Stego embed failed, falling back to plain text', error);
            }
        }

        // CSRF Protection & Credentials
        const csrfToken = document.cookie.split('; ')
            .find(row => row.startsWith('XSRF-TOKEN='))
            ?.split('=')[1];

        const headers = new Headers(options.headers || {});
        if (csrfToken) {
            headers.set('X-XSRF-TOKEN', csrfToken);
        }
        if (body && typeof body === 'string' && !headers.has('Content-Type')) {
            headers.set('Content-Type', 'application/json');
        }

        options.headers = headers;
        options.credentials = 'include';

        console.log(`[SecureApi] Requesting ${options.method || 'GET'} ${url}`);
        const response = await fetch(url, { ...options, body });

        console.log(`[SecureApi] Response ${response.status} from ${url}`);

        if (!response.ok) {
            const errorText = await response.text();
            console.error(`[SecureApi] API Error ${response.status}:`, errorText);
            throw new Error(errorText || `API Error: ${response.status} ${response.statusText}`);
        }

        const text = await response.text();
        console.log(`[SecureApi] Raw text response (snippet): ${text.substring(0, 50)}...`);

        // Try to detect stego response
        let decodedText = text;
        if (config.STEGO_ENABLED) {
            try {
                const json = JSON.parse(text);
                if (json && typeof json === 'object' && 'stego' in json && 'length' in json) {
                    decodedText = await StegoService.secureReceive(json.stego, json.length);
                }
            } catch (e) {
                // Not JSON or not stego, use original text
            }
        }

        if (responseType === 'json') {
            try {
                if (!decodedText || decodedText.trim() === '') return null;
                return JSON.parse(decodedText);
            } catch (e) {
                console.warn('Response is not valid JSON, returning text:', decodedText);
                if (decodedText.includes('<html>')) {
                    throw new Error('Received HTML instead of JSON. Possible session timeout or server error.');
                }
                return decodedText; // Fallback
            }
        }

        return decodedText;
    },

    get(url: string, headers: HeadersInit = {}) {
        return this.request(url, { method: 'GET', headers }, 'json');
    },

    post(url: string, data: any, headers: HeadersInit = {}) {
        return this.request(url, {
            method: 'POST',
            headers,
            body: JSON.stringify(data)
        }, 'json');
    },

    put(url: string, data: any, headers: HeadersInit = {}) {
        return this.request(url, {
            method: 'PUT',
            headers,
            body: JSON.stringify(data)
        }, 'json');
    },

    delete(url: string, headers: HeadersInit = {}) {
        return this.request(url, { method: 'DELETE', headers }, 'json');
    },

    async getText(url: string, headers: HeadersInit = {}) {
        return this.request(url, { method: 'GET', headers }, 'text');
    }
};

export default SecureApi;
