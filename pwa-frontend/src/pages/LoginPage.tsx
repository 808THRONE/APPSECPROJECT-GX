import React from 'react';
import { AuthService } from '../services/authService';

export const LoginPage: React.FC = () => {
    const handleLogin = async () => {
        try {
            await AuthService.login();
        } catch (error) {
            console.error('Login failed', error);
            alert('Failed to initiate login');
        }
    };

    return (
        <div className="login-page">
            <div className="login-container">
                {/* Logo and Branding */}
                <div className="login-header">
                    <div className="logo">
                        <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <rect width="48" height="48" rx="12" fill="url(#logo-gradient)" />
                            <path d="M24 12L32 18V30L24 36L16 30V18L24 12Z" stroke="white" strokeWidth="2" fill="none" />
                            <circle cx="24" cy="24" r="4" fill="white" />
                            <defs>
                                <linearGradient id="logo-gradient" x1="0" y1="0" x2="48" y2="48">
                                    <stop stopColor="#6366f1" />
                                    <stop offset="1" stopColor="#8b5cf6" />
                                </linearGradient>
                            </defs>
                        </svg>
                    </div>
                    <h1 className="login-title">SecureGate</h1>
                    <p className="login-subtitle">Enterprise Identity & Access Management</p>
                </div>

                {/* Login Card */}
                <div className="login-card">
                    <div className="login-card-header">
                        <h2>Welcome back</h2>
                        <p>Sign in to access your dashboard</p>
                    </div>

                    <div className="login-card-body">
                        <button onClick={handleLogin} className="btn btn-primary btn-lg login-btn">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                                <polyline points="10 17 15 12 10 7" />
                                <line x1="15" y1="12" x2="3" y2="12" />
                            </svg>
                            Sign In with SSO
                        </button>

                        <div className="login-divider">
                            <span>or</span>
                        </div>

                        <button onClick={() => window.location.href = '/register'} className="btn btn-outline-primary btn-lg login-btn" style={{ background: 'transparent', border: '1px solid #6366f1', color: '#6366f1' }}>
                            Create New Account
                        </button>
                    </div>

                    <div className="login-card-footer">
                        <p>Enterprise-grade identity protection</p>
                        <div className="security-badges">
                            <span className="badge badge-info">OAuth 2.1</span>
                            <span className="badge badge-info">PKCE</span>
                            <span className="badge badge-info">MFA</span>
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="login-footer">
                    <p>© 2026 SecureGate. All rights reserved.</p>
                </div>
            </div>

            <style>{`
                .login-page {
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 2rem;
                    background: #0f172a;
                }

                .login-container {
                    width: 100%;
                    max-width: 420px;
                    animation: fadeIn 0.6s ease-out;
                }

                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }

                .login-header {
                    text-align: center;
                    margin-bottom: 2rem;
                }

                .logo {
                    display: inline-flex;
                    margin-bottom: 1rem;
                }

                .login-title {
                    font-size: 2.25rem;
                    font-weight: 800;
                    color: white;
                    margin-bottom: 0.25rem;
                }

                .login-subtitle {
                    color: #94a3b8;
                    font-size: 0.9375rem;
                }

                .login-card {
                    background: #1e293b;
                    border: 1px solid #334155;
                    border-radius: 1.25rem;
                    padding: 2.5rem;
                    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3);
                }

                .login-card-header {
                    text-align: center;
                    margin-bottom: 2rem;
                }

                .login-card-header h2 {
                    font-size: 1.5rem;
                    color: white;
                    margin-bottom: 0.5rem;
                }

                .login-card-header p {
                    color: #94a3b8;
                }

                .login-btn {
                    width: 100%;
                    padding: 0.875rem;
                    font-size: 1rem;
                    font-weight: 600;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 0.75rem;
                    cursor: pointer;
                    border-radius: 0.75rem;
                    transition: all 0.2s;
                }

                .btn-primary {
                    background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
                    color: white;
                    border: none;
                }

                .btn-primary:hover {
                    opacity: 0.9;
                    transform: translateY(-1px);
                }

                .login-divider {
                    display: flex;
                    align-items: center;
                    margin: 1.5rem 0;
                }

                .login-divider::before,
                .login-divider::after {
                    content: '';
                    flex: 1;
                    height: 1px;
                    background: #334155;
                }

                .login-divider span {
                    padding: 0 1rem;
                    color: #64748b;
                    font-size: 0.875rem;
                }

                .login-card-footer {
                    margin-top: 2rem;
                    padding-top: 1.5rem;
                    border-top: 1px solid #334155;
                    text-align: center;
                }

                .security-badges {
                    display: flex;
                    justify-content: center;
                    gap: 0.5rem;
                    margin-top: 0.75rem;
                }

                .badge {
                    padding: 0.25rem 0.5rem;
                    border-radius: 9999px;
                    font-size: 0.75rem;
                    font-weight: 500;
                }

                .badge-info {
                    background: rgba(99, 102, 241, 0.1);
                    color: #818cf8;
                }

                .login-footer {
                    text-align: center;
                    margin-top: 1.5rem;
                    color: #64748b;
                    font-size: 0.875rem;
                }
            `}</style>
        </div>
    );
};
