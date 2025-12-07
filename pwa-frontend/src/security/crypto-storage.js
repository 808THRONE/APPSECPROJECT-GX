/**
 * Web Crypto API - Encrypted Storage
 * Used for encrypting sensitive data in IndexedDB/localStorage
 */

const ENCRYPTION_KEY_NAME = 'securegate-encryption-key';
const IV_LENGTH = 12; // 96 bits for GCM

/**
 * Generate or retrieve encryption key
 */
async function getEncryptionKey() {
    // Try to retrieve existing key
    const keyData = sessionStorage.getItem(ENCRYPTION_KEY_NAME);

    if (keyData) {
        const rawKey = Uint8Array.from(atob(keyData), c => c.charCodeAt(0));
        return await crypto.subtle.importKey(
            'raw',
            rawKey,
            { name: 'AES-GCM' },
            false,
            ['encrypt', 'decrypt']
        );
    }

    // Generate new key
    const key = await crypto.subtle.generateKey(
        { name: 'AES-GCM', length: 256 },
        true,
        ['encrypt', 'decrypt']
    );

    // Export and store in sessionStorage (cleared on tab close)
    const exportedKey = await crypto.subtle.exportKey('raw', key);
    const keyString = btoa(String.fromCharCode(...new Uint8Array(exportedKey)));
    sessionStorage.setItem(ENCRYPTION_KEY_NAME, keyString);

    return key;
}

/**
 * Encrypt data using AES-256-GCM
 */
export async function encryptData(data) {
    try {
        const key = await getEncryptionKey();
        const iv = crypto.getRandomValues(new Uint8Array(IV_LENGTH));

        const encoder = new TextEncoder();
        const encodedData = encoder.encode(JSON.stringify(data));

        const encryptedData = await crypto.subtle.encrypt(
            { name: 'AES-GCM', iv },
            key,
            encodedData
        );

        // Combine IV + encrypted data
        const combined = new Uint8Array(iv.length + encryptedData.byteLength);
        combined.set(iv, 0);
        combined.set(new Uint8Array(encryptedData), iv.length);

        // Return as base64
        return btoa(String.fromCharCode(...combined));
    } catch (error) {
        console.error('Encryption failed:', error);
        throw error;
    }
}

/**
 * Decrypt data using AES-256-GCM
 */
export async function decryptData(encryptedString) {
    try {
        const key = await getEncryptionKey();

        // Decode from base64
        const combined = Uint8Array.from(atob(encryptedString), c => c.charCodeAt(0));

        // Extract IV and encrypted data
        const iv = combined.slice(0, IV_LENGTH);
        const encryptedData = combined.slice(IV_LENGTH);

        const decryptedData = await crypto.subtle.decrypt(
            { name: 'AES-GCM', iv },
            key,
            encryptedData
        );

        const decoder = new TextDecoder();
        const jsonString = decoder.decode(decryptedData);
        return JSON.parse(jsonString);
    } catch (error) {
        console.error('Decryption failed:', error);
        throw error;
    }
}

/**
 * Clear encryption key (logout)
 */
export function clearEncryptionKey() {
    sessionStorage.removeItem(ENCRYPTION_KEY_NAME);
}
