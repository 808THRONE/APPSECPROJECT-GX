import React, { useState } from 'react';
import { generateCodeVerifier, generateCodeChallenge } from '../services/authService';
import { config } from '../config';

export const RegisterPage: React.FC = () => {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);

        try {
            const codeVerifier = await generateCodeVerifier();
            const codeChallenge = await generateCodeChallenge(codeVerifier);
            sessionStorage.setItem('code_verifier', codeVerifier);

            const params = new URLSearchParams({
                username,
                email,
                password,
                response_type: 'code',
                client_id: config.OAUTH.CLIENT_ID,
                redirect_uri: config.OAUTH.REDIRECT_URI,
                scope: config.OAUTH.SCOPES,
                state: crypto.randomUUID(),
                code_challenge: codeChallenge,
                code_challenge_method: 'S256'
            });

            // For now, let's just use the AuthService.register() approach but with a better UI
            // Wait, if I want to show errors HERE, I need to fetch.

            const response = await fetch(`${config.IAM_URL}/oauth2/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params.toString(),
            });

            if (response.ok) {
                // If it's a redirect, the browser might have followed it.
                // If we get a response, it's likely the final landing page or the authorize page.
                window.location.href = response.url;
            } else {
                const errorMsg = await response.text();
                // Strip HTML tags if any
                setError(errorMsg.replace(/<[^>]*>?/gm, '').replace('Invalid Password: ', ''));
            }
        } catch (err) {
            setError('Connection failed. Please check your network.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="register-page">
            <div className="register-container">
                <div className="register-header">
                    <div className="logo">
                        <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <rect width="48" height="48" rx="12" fill="url(#logo-gradient)" />
                            <path d="M24 12L32 18V30L24 36L16 30V18L24 12Z" stroke="white" strokeWidth="2" fill="none" />
                            <circle cx="24" cy="24" r="4" fill="white" />
                            <defs>
                                <linearGradient id="logo-gradient" x1="0" y1="0" x2="48" y2="48">
                                    <stop stopColor="#818cf8" />
                                    <stop offset="1" stopColor="#6366f1" />
                                </linearGradient>
                            </defs>
                        </svg>
                    </div>
                    <h1 className="register-title">Join SecureGate</h1>
                    <p className="register-subtitle">Advanced Identity Protection Awaits</p>
                </div>

                <div className="register-card">
                    <form onSubmit={handleSubmit}>
                        {error && (
                            <div className="error-box">
                                {error}
                            </div>
                        )}

                        <div className="form-group">
                            <label>Username</label>
                            <input
                                type="text"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                placeholder="Choose a unique username"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label>Email Address</label>
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="name@company.com"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label>Password</label>
                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="At least 12 characters"
                                required
                            />
                            <p className="hint">Must contain uppercase, number, and special character.</p>
                        </div>

                        <button type="submit" className="btn-register" disabled={loading}>
                            {loading ? 'Creating Account...' : 'Create Account'}
                        </button>
                    </form>

                    <div className="register-footer">
                        <p>By signing up, you agree to our <a href="#">Security Policies</a></p>
                        <hr />
                        <p>Already have an account? <a href="/" className="login-link">Sign In</a></p>
                    </div>
                </div>
            </div>

            <style>{`
                .register-page {
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: #0f172a;
                    font-family: 'Inter', system-ui, -apple-system, sans-serif;
                    padding: 2rem;
                }

                .register-container {
                    width: 100%;
                    max-width: 440px;
                    animation: slideUp 0.5s ease-out;
                }

                @keyframes slideUp {
                    from { transform: translateY(20px); opacity: 0; }
                    to { transform: translateY(0); opacity: 1; }
                }

                .register-header {
                    text-align: center;
                    margin-bottom: 2rem;
                }

                .logo {
                    margin-bottom: 1rem;
                }

                .register-title {
                    font-size: 2.25rem;
                    font-weight: 800;
                    color: white;
                    margin: 0;
                    letter-spacing: -0.025em;
                }

                .register-subtitle {
                    color: #94a3b8;
                    font-size: 1rem;
                    margin-top: 0.5rem;
                }

                .register-card {
                    background: #1e293b;
                    border: 1px solid #334155;
                    border-radius: 1.5rem;
                    padding: 2.5rem;
                    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
                }

                .error-box {
                    background: #fef2f2;
                    border-left: 4px solid #ef4444;
                    color: #991b1b;
                    padding: 1rem;
                    border-radius: 0.5rem;
                    margin-bottom: 1.5rem;
                    font-size: 0.875rem;
                }

                .form-group {
                    margin-bottom: 1.5rem;
                }

                .form-group label {
                    display: block;
                    color: #cbd5e1;
                    font-size: 0.875rem;
                    font-weight: 600;
                    margin-bottom: 0.5rem;
                }

                .form-group input {
                    width: 100%;
                    padding: 0.75rem 1rem;
                    background: #0f172a;
                    border: 1px solid #334155;
                    border-radius: 0.75rem;
                    color: white;
                    transition: all 0.2s;
                    box-sizing: border-box;
                }

                .form-group input:focus {
                    outline: none;
                    border-color: #6366f1;
                    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
                }

                .hint {
                    color: #64748b;
                    font-size: 0.75rem;
                    margin-top: 0.5rem;
                    line-height: 1.4;
                }

                .btn-register {
                    width: 100%;
                    padding: 0.875rem;
                    background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
                    color: white;
                    border: none;
                    border-radius: 0.75rem;
                    font-size: 1rem;
                    font-weight: 700;
                    cursor: pointer;
                    transition: transform 0.2s, opacity 0.2s;
                    margin-top: 1rem;
                }

                .btn-register:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.4);
                }

                .btn-register:active {
                    transform: translateY(0);
                }

                .btn-register:disabled {
                    opacity: 0.6;
                    cursor: not-allowed;
                    transform: none;
                }

                .register-footer {
                    margin-top: 2rem;
                    text-align: center;
                }

                .register-footer p {
                    color: #64748b;
                    font-size: 0.875rem;
                }

                .register-footer a {
                    color: #818cf8;
                    text-decoration: none;
                    font-weight: 600;
                }

                .register-footer a:hover {
                    text-decoration: underline;
                }

                .register-footer hr {
                    border: 0;
                    border-top: 1px solid #334155;
                    margin: 1.5rem 0;
                }

                .login-link {
                    font-size: 1rem;
                }
            `}</style>
        </div>
    );
};
