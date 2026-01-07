/**
 * Device Fingerprinting for Zero Trust Security
 * Collects browser/device attributes for authentication
 */

/**
 * Generate device fingerprint
 * Combines multiple browser attributes for unique identification
 */
export async function generateDeviceFingerprint() {
    const fingerprint = {
        userAgent: navigator.userAgent,
        language: navigator.language,
        languages: navigator.languages?.join(','),
        platform: navigator.platform,
        hardwareConcurrency: navigator.hardwareConcurrency,
        deviceMemory: navigator.deviceMemory,
        screenResolution: `${screen.width}x${screen.height}`,
        screenColorDepth: screen.colorDepth,
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        timezoneOffset: new Date().getTimezoneOffset(),
        touchSupport: navigator.maxTouchPoints > 0,
        canvasFingerprint: await getCanvasFingerprint(),
        webglFingerprint: await getWebGLFingerprint(),
        audioFingerprint: await getAudioFingerprint(),
    };

    // Hash the fingerprint
    const fingerprintString = JSON.stringify(fingerprint);
    const hash = await hashString(fingerprintString);

    return {
        hash,
        details: fingerprint,
    };
}

/**
 * Canvas fingerprinting
 */
async function getCanvasFingerprint() {
    try {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');

        // Draw text
        ctx.textBaseline = 'top';
        ctx.font = '14px Arial';
        ctx.textBaseline = 'alphabetic';
        ctx.fillStyle = '#f60';
        ctx.fillRect(125, 1, 62, 20);
        ctx.fillStyle = '#069';
        ctx.fillText('SecureGate IAM', 2, 15);
        ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
        ctx.fillText('SecureGate IAM', 4, 17);

        const dataUrl = canvas.toDataURL();
        return await hashString(dataUrl);
    } catch (error) {
        return 'canvas-unavailable';
    }
}

/**
 * WebGL fingerprinting
 */
async function getWebGLFingerprint() {
    try {
        const canvas = document.createElement('canvas');
        const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');

        if (!gl) return 'webgl-unavailable';

        const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
        const vendor = debugInfo ? gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) : '';
        const renderer = debugInfo ? gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) : '';

        const fingerprint = `${vendor}~${renderer}`;
        return await hashString(fingerprint);
    } catch (error) {
        return 'webgl-unavailable';
    }
}

/**
 * Audio fingerprinting
 */
async function getAudioFingerprint() {
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return 'audio-unavailable';

        const context = new AudioContext();
        const oscillator = context.createOscillator();
        const analyser = context.createAnalyser();
        const gainNode = context.createGain();
        const scriptProcessor = context.createScriptProcessor(4096, 1, 1);

        gainNode.gain.value = 0; // Mute
        oscillator.connect(analyser);
        analyser.connect(scriptProcessor);
        scriptProcessor.connect(gainNode);
        gainNode.connect(context.destination);

        oscillator.start(0);

        return new Promise((resolve) => {
            scriptProcessor.onaudioprocess = function (event) {
                const output = event.outputBuffer.getChannelData(0);
                const fingerprint = Array.from(output.slice(0, 30)).join(',');

                oscillator.stop();
                scriptProcessor.disconnect();
                analyser.disconnect();
                gainNode.disconnect();
                context.close();

                hashString(fingerprint).then(resolve);
            };
        });
    } catch (error) {
        return 'audio-unavailable';
    }
}

/**
 * Hash string using SHA-256
 */
async function hashString(str) {
    const encoder = new TextEncoder();
    const data = encoder.encode(str);
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Get geolocation (if permitted)
 */
export async function getGeolocation() {
    return new Promise((resolve) => {
        if (!navigator.geolocation) {
            resolve(null);
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                resolve({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    accuracy: position.coords.accuracy,
                });
            },
            () => resolve(null),
            { timeout: 5000 }
        );
    });
}
