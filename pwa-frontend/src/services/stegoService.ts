import { config } from '../config';

export interface StegoEmbedRequest {
    cover: number[];
    payload: string;
}

export interface StegoExtractRequest {
    stego: number[];
    length: number;
}

export interface StegoResponse {
    stego: number[];
}

/**
 * Steganography Service
 * Provides secure data transmission using STC (Syndrome Trellis Codes) steganography
 * with ChaCha20-Poly1305 encryption.
 */
export const StegoService = {
    /**
     * Embed a payload into cover data using steganography
     */
    async embedPayload(cover: number[], payload: string): Promise<number[]> {
        if (!config.STEGO_ENABLED) {
            console.warn('Stego module disabled, returning raw payload encoding');
            return this.encodeForTransmission(payload);
        }

        const response = await fetch(`${config.API_BASE_URL}/stego/embed`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ cover, payload } as StegoEmbedRequest)
        });

        if (!response.ok) {
            throw new Error(`Stego embed failed: ${response.statusText}`);
        }

        const result: StegoResponse = await response.json();
        return result.stego;
    },

    /**
     * Extract a payload from stego data
     */
    async extractPayload(stego: number[], length: number): Promise<string> {
        if (!config.STEGO_ENABLED) {
            console.warn('Stego module disabled, decoding raw data');
            return this.decodeFromTransmission(stego);
        }

        const response = await fetch(`${config.API_BASE_URL}/stego/extract`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ stego, length } as StegoExtractRequest)
        });

        if (!response.ok) {
            throw new Error(`Stego extract failed: ${response.statusText}`);
        }

        return response.text();
    },

    /**
     * Generate a random cover array for embedding
     */
    generateCover(size: number = 1024): number[] {
        const cover: number[] = [];
        for (let i = 0; i < size; i++) {
            cover.push(Math.floor(Math.random() * 256));
        }
        return cover;
    },

    /**
     * Encode string data to number array for transmission (fallback when stego disabled)
     */
    encodeForTransmission(data: string): number[] {
        const encoder = new TextEncoder();
        const bytes = encoder.encode(data);
        return Array.from(bytes);
    },

    /**
     * Decode number array back to string (fallback when stego disabled)
     */
    decodeFromTransmission(stego: number[]): string {
        const bytes = new Uint8Array(stego);
        const decoder = new TextDecoder();
        return decoder.decode(bytes);
    },

    /**
     * Securely transmit sensitive data using steganography
     * This wraps the full embed workflow
     */
    async secureTransmit(sensitiveData: string): Promise<{ stego: number[]; length: number }> {
        const cover = this.generateCover(sensitiveData.length * 8 + 256);
        const stego = await this.embedPayload(cover, sensitiveData);
        return {
            stego,
            length: sensitiveData.length * 8
        };
    },

    /**
     * Securely receive data that was transmitted via steganography
     */
    async secureReceive(stego: number[], length: number): Promise<string> {
        return this.extractPayload(stego, length);
    }
};

export default StegoService;
