import React, { useEffect, useState } from 'react';
import { AuthService } from '../services/authService';

export const CallbackPage: React.FC = () => {
    const [status, setStatus] = useState('Processing login...');

    useEffect(() => {
        const processCallback = async () => {
            const params = new URLSearchParams(window.location.search);
            const code = params.get('code');

            if (!code) {
                setStatus('Error: No authorization code received');
                return;
            }

            try {
                console.log('Exchanging code for token...');
                const tokens = await AuthService.handleCallback(code);
                console.log('Tokens response:', tokens);

                setStatus('Login successful! Redirecting...');
                setTimeout(() => window.location.href = '/', 500);
            } catch (error: any) {
                console.error('Callback error:', error);
                setStatus(`Login failed: ${error.message || 'Unknown error'}. Check console for details.`);
            }
        };

        processCallback();
    }, []);

    return (
        <div style={{ padding: '2rem', textAlign: 'center' }}>
            <h2>{status}</h2>
        </div>
    );
};
