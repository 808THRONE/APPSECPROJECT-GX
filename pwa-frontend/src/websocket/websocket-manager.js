import ReconnectingWebSocket from 'reconnecting-websocket';

/**
 * WebSocket Manager with Auto-Reconnection
 * Handles real-time communication with the backend
 */

class WebSocketManager {
    constructor() {
        this.ws = null;
        this.listeners = new Map();
        this.isConnected = false;
        this.url = null;
    }

    /**
     * Connect to WebSocket server
     */
    connect(url, accessToken = null) {
        this.url = url;

        const options = {
            maxRetries: 10,
            connectionTimeout: 5000,
            maxReconnectionDelay: 10000,
            minReconnectionDelay: 1000,
            reconnectionDelayGrowFactor: 1.3,
        };

        // Add token to URL or headers if provided
        const wsUrl = accessToken ? `${url}?token=${accessToken}` : url;

        this.ws = new ReconnectingWebSocket(wsUrl, [], options);

        this.ws.addEventListener('open', () => {
            this.isConnected = true;
            console.log('[WebSocket] Connected to', url);
            this.emit('connected');
        });

        this.ws.addEventListener('close', () => {
            this.isConnected = false;
            console.log('[WebSocket] Disconnected');
            this.emit('disconnected');
        });

        this.ws.addEventListener('error', (error) => {
            console.error('[WebSocket] Error:', error);
            this.emit('error', error);
        });

        this.ws.addEventListener('message', (event) => {
            try {
                const data = JSON.parse(event.data);
                this.handleMessage(data);
            } catch (error) {
                console.error('[WebSocket] Failed to parse message:', error);
            }
        });
    }

    /**
     * Handle incoming messages
     */
    handleMessage(data) {
        const { type, payload } = data;

        if (type) {
            this.emit(type, payload);
        }
    }

    /**
     * Send message
     */
    send(type, payload) {
        if (!this.isConnected) {
            console.warn('[WebSocket] Not connected. Message queued.');
            // Queue message for when connection is restored
            this.once('connected', () => {
                this.send(type, payload);
            });
            return;
        }

        const message = JSON.stringify({ type, payload });
        this.ws.send(message);
    }

    /**
     * Register event listener
     */
    on(event, callback) {
        if (!this.listeners.has(event)) {
            this.listeners.set(event, []);
        }
        this.listeners.get(event).push(callback);
    }

    /**
     * Register one-time event listener
     */
    once(event, callback) {
        const wrappedCallback = (...args) => {
            callback(...args);
            this.off(event, wrappedCallback);
        };
        this.on(event, wrappedCallback);
    }

    /**
     * Remove event listener
     */
    off(event, callback) {
        if (!this.listeners.has(event)) return;

        const callbacks = this.listeners.get(event);
        const index = callbacks.indexOf(callback);

        if (index > -1) {
            callbacks.splice(index, 1);
        }
    }

    /**
     * Emit event to all listeners
     */
    emit(event, data) {
        if (!this.listeners.has(event)) return;

        this.listeners.get(event).forEach(callback => {
            try {
                callback(data);
            } catch (error) {
                console.error(`[WebSocket] Error in ${event} listener:`, error);
            }
        });
    }

    /**
     * Disconnect
     */
    disconnect() {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
            this.isConnected = false;
            this.listeners.clear();
        }
    }

    /**
     * Reconnect
     */
    reconnect() {
        if (this.ws) {
            this.ws.reconnect();
        } else if (this.url) {
            this.connect(this.url);
        }
    }
}

// Singleton instance
export const websocketManager = new WebSocketManager();
