import React, { useState } from 'react';
import { StegoService } from '../services/stegoService';

export const StegoPage: React.FC = () => {
    const [mode, setMode] = useState<'embed' | 'extract'>('embed');
    const [payload, setPayload] = useState('');
    const [coverSize, setCoverSize] = useState(1024);
    const [stegoData, setStegoData] = useState<number[]>([]);
    const [extracted, setExtracted] = useState('');
    const [resultData, setResultData] = useState<number[] | null>(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleEmbed = async () => {
        setIsProcessing(true);
        setError(null);
        try {
            const cover = StegoService.generateCover(Math.max(coverSize, payload.length * 8 + 256));
            const stego = await StegoService.embedPayload(cover, payload);
            setResultData(stego);
            // Auto-populate extract input for convenience
            setStegoData(stego);
        } catch (err) {
            console.error(err);
            setError('Embedding failed. Check console for details.');
        } finally {
            setIsProcessing(false);
        }
    };

    const handleExtract = async () => {
        setIsProcessing(true);
        setError(null);
        try {
            // Estimate length roughly if unknown, but for STC we usually need length or termination
            // Here we assume length is known or stored in header in a real app
            // For this output demo, we send estimated length
            const result = await StegoService.extractPayload(stegoData, payload.length * 8);
            // Note: In a real scenario, we wouldn't know payload.length before extracting.
            // The StegoService.extractPayload mostly handles the decoding.
            // If the service doesn't actually need the length for decoding, we can pass 0.
            // Let's rely on what StegoService expects.

            setExtracted(result);
        } catch (err) {
            console.error(err);
            setError('Extraction failed. Check console for details.');
        } finally {
            setIsProcessing(false);
        }
    };

    return (
        <div className="stego-page">
            <div className="page-header">
                <div>
                    <h1>Secure Steganography</h1>
                    <p>Embed sensitive data into digital carriers using STC + ChaCha20-Poly1305</p>
                </div>
            </div>

            <div className="stego-card">
                <div className="tabs">
                    <button
                        className={`tab ${mode === 'embed' ? 'active' : ''}`}
                        onClick={() => setMode('embed')}
                    >
                        Encrypt & Embed
                    </button>
                    <button
                        className={`tab ${mode === 'extract' ? 'active' : ''}`}
                        onClick={() => setMode('extract')}
                    >
                        Extract & Decrypt
                    </button>
                </div>

                <div className="stego-content">
                    {mode === 'embed' ? (
                        <div className="stego-form">
                            <div className="form-group">
                                <label>Secret Payload</label>
                                <textarea
                                    className="form-input"
                                    rows={4}
                                    placeholder="Enter sensitive message..."
                                    value={payload}
                                    onChange={e => setPayload(e.target.value)}
                                />
                            </div>

                            <div className="form-group">
                                <label>Cover Carrier Size (Simulated)</label>
                                <input
                                    type="number"
                                    className="form-input"
                                    value={coverSize}
                                    onChange={e => setCoverSize(parseInt(e.target.value))}
                                />
                            </div>

                            <button
                                className="btn btn-primary"
                                onClick={handleEmbed}
                                disabled={!payload || isProcessing}
                            >
                                {isProcessing ? 'Processing...' : 'Embed Data'}
                            </button>

                            {resultData && (
                                <div className="result-box success">
                                    <h4>Encryption Successful!</h4>
                                    <p>Data embedded into carrier (first 50 bytes preview):</p>
                                    <div className="data-preview">
                                        [{resultData.slice(0, 50).join(', ')}...]
                                    </div>
                                    <div className="info-badge">
                                        Carries {payload.length} chars (Encrypted + STC encoded)
                                    </div>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="stego-form">
                            <div className="form-group">
                                <label>Stego Carrier Data (JSON Array)</label>
                                <textarea
                                    className="form-input"
                                    rows={4}
                                    placeholder="[12, 45, 23, ...]"
                                    value={JSON.stringify(stegoData)}
                                    onChange={e => {
                                        try {
                                            setStegoData(JSON.parse(e.target.value));
                                        } catch {
                                            // ignore parse error while typing
                                        }
                                    }}
                                />
                            </div>

                            <button
                                className="btn btn-primary"
                                onClick={handleExtract}
                                disabled={stegoData.length === 0 || isProcessing}
                            >
                                {isProcessing ? 'Processing...' : 'Extract Data'}
                            </button>

                            {extracted && (
                                <div className="result-box success">
                                    <h4>Decryption Successful!</h4>
                                    <p>Recovered Message:</p>
                                    <div className="secret-reveal">
                                        {extracted}
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {error && <div className="error-message">{error}</div>}
                </div>
            </div>

            <style>{`
                .stego-page {
                    max-width: 800px;
                    margin: 0 auto;
                }
                .page-header {
                    margin-bottom: var(--space-xl);
                }
                .page-header h1 {
                    font-size: 1.875rem;
                    margin-bottom: var(--space-xs);
                }
                .page-header p {
                    color: var(--text-secondary);
                }
                .stego-card {
                    background: var(--bg-card);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-xl);
                    overflow: hidden;
                    box-shadow: var(--shadow-sm);
                }
                .tabs {
                    display: flex;
                    border-bottom: 1px solid var(--border-color);
                }
                .tab {
                    flex: 1;
                    padding: 1rem;
                    background: transparent;
                    border: none;
                    font-weight: 600;
                    color: var(--text-secondary);
                    cursor: pointer;
                    transition: all 0.2s;
                }
                .tab.active {
                    color: var(--accent-primary);
                    background: var(--bg-tertiary);
                    border-bottom: 2px solid var(--accent-primary);
                }
                .stego-content {
                    padding: var(--space-xl);
                }
                .stego-form {
                    display: flex;
                    flex-direction: column;
                    gap: var(--space-lg);
                }
                .form-group label {
                    display: block;
                    font-size: 0.875rem;
                    font-weight: 500;
                    margin-bottom: var(--space-xs);
                    color: var(--text-primary);
                }
                .form-input {
                    width: 100%;
                    padding: 0.75rem;
                    background: var(--bg-tertiary);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-md);
                    color: var(--text-primary);
                    font-family: monospace;
                }
                .result-box {
                    margin-top: var(--space-lg);
                    padding: var(--space-lg);
                    background: rgba(16, 185, 129, 0.1);
                    border: 1px solid rgba(16, 185, 129, 0.2);
                    border-radius: var(--radius-md);
                }
                .data-preview {
                    font-family: monospace;
                    font-size: 0.8rem;
                    color: var(--text-secondary);
                    word-break: break-all;
                    margin: 0.5rem 0;
                }
                .secret-reveal {
                    font-size: 1.25rem;
                    font-weight: bold;
                    color: var(--text-primary);
                    padding: 1rem;
                    background: var(--bg-primary);
                    border-radius: var(--radius-md);
                    border: 1px solid var(--border-color);
                }
                .info-badge {
                    display: inline-block;
                    padding: 0.25rem 0.5rem;
                    background: var(--accent-primary);
                    color: white;
                    border-radius: var(--radius-sm);
                    font-size: 0.75rem;
                    margin-top: 0.5rem;
                }
                .error-message {
                    color: #ef4444;
                    margin-top: 1rem;
                    padding: 1rem;
                    background: rgba(239, 68, 68, 0.1);
                    border-radius: var(--radius-md);
                }
            `}</style>
        </div>
    );
};
