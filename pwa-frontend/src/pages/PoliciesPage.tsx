import React, { useState, useEffect } from 'react';
import { PolicyService } from '../services/policyService';
import type { Policy } from '../services/policyService';

export const PoliciesPage: React.FC = () => {
    const [selectedPolicyId, setSelectedPolicyId] = useState<string | null>(null);
    const [policies, setPolicies] = useState<Policy[]>([]);
    const [loading, setLoading] = useState(true);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [formData, setFormData] = useState<Policy>({
        name: '',
        effect: 'PERMIT',
        resource: '*',
        action: '*',
        description: '',
        conditions: ''
    });

    const loadPolicies = async () => {
        try {
            setLoading(true);
            const data = await PolicyService.getPolicies();
            setPolicies(data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPolicies();
    }, []);

    const handleCreate = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await PolicyService.createPolicy(formData);
            setIsCreateModalOpen(false);
            setFormData({ name: '', effect: 'PERMIT', resource: '*', action: '*', description: '', conditions: '' });
            loadPolicies();
        } catch (err: any) {
            alert(err.message);
        }
    };

    const handleUpdate = async () => {
        if (!selectedPolicyId) return;
        const current = policies.find(p => p.policyId === selectedPolicyId);
        if (!current) return;
        try {
            await PolicyService.updatePolicy(selectedPolicyId, current);
            alert('Policy updated successfully');
            loadPolicies();
        } catch (err: any) {
            alert(err.message);
        }
    };

    const handleFieldChange = (field: keyof Policy, value: any) => {
        setPolicies(prev => prev.map(p =>
            p.policyId === selectedPolicyId ? { ...p, [field]: value } : p
        ));
    };

    const selectedPolicy = policies.find(p => p.policyId === selectedPolicyId);

    return (
        <div className="policies-page animate-fade-in">
            {/* Page Header */}
            <div className="page-header">
                <div>
                    <h1>Access Policies</h1>
                    <p>Define and manage RBAC/ABAC access control rules</p>
                </div>
                <button className="btn btn-primary" onClick={() => setIsCreateModalOpen(true)}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    Create Policy
                </button>
            </div>

            <div className="policies-layout">
                {/* Policy List */}
                <div className="policies-list">
                    {loading ? (
                        <div className="empty-state">Loading...</div>
                    ) : policies.map((policy) => (
                        <div
                            key={policy.policyId}
                            className={`policy-card ${selectedPolicyId === policy.policyId ? 'active' : ''}`}
                            onClick={() => setSelectedPolicyId(policy.policyId || null)}
                        >
                            <div className="policy-card-header">
                                <span className={`effect-badge ${policy.effect === 'PERMIT' ? 'permit' : 'deny'}`}>
                                    {policy.effect}
                                </span>
                                <h3 className="policy-name">{policy.name}</h3>
                            </div>
                            <p className="policy-desc">{policy.description}</p>
                            <div className="policy-meta">
                                <span className="meta-item">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                                    </svg>
                                    {policy.resource}
                                </span>
                                <span className="meta-item">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
                                    </svg>
                                    {policy.action}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Policy Editor */}
                <div className="policy-editor card">
                    {selectedPolicy ? (
                        <>
                            <div className="card-header">
                                <h3 className="card-title">Policy Details</h3>
                                <div className="editor-actions">
                                    <button className="btn btn-ghost btn-sm" onClick={() => setSelectedPolicyId(null)}>Cancel</button>
                                    <button className="btn btn-primary btn-sm" onClick={handleUpdate}>Save Changes</button>
                                </div>
                            </div>
                            <div className="editor-form">
                                <div className="form-group">
                                    <label className="form-label">Policy Name</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        value={selectedPolicy.name}
                                        onChange={e => handleFieldChange('name', e.target.value)}
                                    />
                                </div>
                                <div className="form-row">
                                    <div className="form-group">
                                        <label className="form-label">Effect</label>
                                        <select
                                            className="form-input"
                                            value={selectedPolicy.effect}
                                            onChange={e => handleFieldChange('effect', e.target.value)}
                                        >
                                            <option value="PERMIT">PERMIT</option>
                                            <option value="DENY">DENY</option>
                                        </select>
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Resource</label>
                                        <input
                                            type="text"
                                            className="form-input"
                                            value={selectedPolicy.resource}
                                            onChange={e => handleFieldChange('resource', e.target.value)}
                                        />
                                    </div>
                                    <div className="form-group">
                                        <label className="form-label">Action</label>
                                        <input
                                            type="text"
                                            className="form-input"
                                            value={selectedPolicy.action}
                                            onChange={e => handleFieldChange('action', e.target.value)}
                                        />
                                    </div>
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Conditions (Expression)</label>
                                    <textarea
                                        className="form-input code-input"
                                        rows={4}
                                        value={selectedPolicy.conditions}
                                        onChange={e => handleFieldChange('conditions', e.target.value)}
                                    />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">Description</label>
                                    <textarea
                                        className="form-input"
                                        rows={2}
                                        value={selectedPolicy.description}
                                        onChange={e => handleFieldChange('description', e.target.value)}
                                    />
                                </div>
                            </div>
                        </>
                    ) : (
                        <div className="empty-state">
                            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                <polyline points="14 2 14 8 20 8" />
                                <line x1="16" y1="13" x2="8" y2="13" />
                                <line x1="16" y1="17" x2="8" y2="17" />
                                <polyline points="10 9 9 9 8 9" />
                            </svg>
                            <h3>Select a Policy</h3>
                            <p>Choose a policy from the list to view and edit its details</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Create Policy Modal */}
            {isCreateModalOpen && (
                <div className="modal-overlay">
                    <div className="modal-content" style={{ width: '600px' }}>
                        <h3>Create New Policy</h3>
                        <form onSubmit={handleCreate}>
                            <div className="form-group">
                                <label>Policy Name</label>
                                <input
                                    type="text"
                                    required
                                    className="form-input"
                                    value={formData.name}
                                    onChange={e => setFormData({ ...formData, name: e.target.value })}
                                />
                            </div>
                            <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                <div className="form-group">
                                    <label>Effect</label>
                                    <select
                                        className="form-input"
                                        value={formData.effect}
                                        onChange={e => setFormData({ ...formData, effect: e.target.value as any })}
                                    >
                                        <option value="PERMIT">PERMIT</option>
                                        <option value="DENY">DENY</option>
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label>Resource</label>
                                    <input
                                        type="text"
                                        required
                                        className="form-input"
                                        value={formData.resource}
                                        onChange={e => setFormData({ ...formData, resource: e.target.value })}
                                    />
                                </div>
                            </div>
                            <div className="form-group">
                                <label>Action</label>
                                <input
                                    type="text"
                                    required
                                    className="form-input"
                                    value={formData.action}
                                    onChange={e => setFormData({ ...formData, action: e.target.value })}
                                />
                            </div>
                            <div className="form-group">
                                <label>Conditions</label>
                                <textarea
                                    className="form-input code-input"
                                    rows={3}
                                    value={formData.conditions}
                                    onChange={e => setFormData({ ...formData, conditions: e.target.value })}
                                />
                            </div>
                            <div className="form-group">
                                <label>Description</label>
                                <textarea
                                    className="form-input"
                                    rows={2}
                                    value={formData.description}
                                    onChange={e => setFormData({ ...formData, description: e.target.value })}
                                />
                            </div>
                            <div className="modal-actions">
                                <button type="button" className="btn btn-secondary" onClick={() => setIsCreateModalOpen(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">Create Policy</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            <style>{`
                .page-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                    margin-bottom: var(--space-xl);
                }

                .page-header h1 {
                    margin-bottom: var(--space-xs);
                }

                .page-header p {
                    color: var(--text-muted);
                    margin: 0;
                }

                .policies-layout {
                    display: grid;
                    grid-template-columns: 400px 1fr;
                    gap: var(--space-xl);
                }

                .policies-list {
                    display: flex;
                    flex-direction: column;
                    gap: var(--space-md);
                }

                .policy-card {
                    background: var(--bg-card);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-lg);
                    padding: var(--space-lg);
                    cursor: pointer;
                    transition: all var(--transition-fast);
                }

                .policy-card:hover {
                    border-color: var(--border-hover);
                }

                .policy-card.active {
                    border-color: var(--accent-primary);
                    background: rgba(99, 102, 241, 0.05);
                }

                .policy-card-header {
                    display: flex;
                    align-items: center;
                    gap: var(--space-md);
                    margin-bottom: var(--space-sm);
                }

                .effect-badge {
                    padding: 0.25rem 0.5rem;
                    border-radius: var(--radius-sm);
                    font-size: 0.6875rem;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 0.05em;
                }

                .effect-badge.permit {
                    background: rgba(16, 185, 129, 0.15);
                    color: var(--success);
                }

                .effect-badge.deny {
                    background: rgba(239, 68, 68, 0.15);
                    color: var(--danger);
                }

                .policy-name {
                    font-size: 1rem;
                    font-weight: 600;
                    margin: 0;
                }

                .policy-desc {
                    font-size: 0.8125rem;
                    color: var(--text-muted);
                    margin: 0 0 var(--space-md);
                    line-height: 1.5;
                }

                .policy-meta {
                    display: flex;
                    gap: var(--space-lg);
                }

                .meta-item {
                    display: flex;
                    align-items: center;
                    gap: var(--space-xs);
                    font-size: 0.75rem;
                    color: var(--text-secondary);
                }

                .policy-editor {
                    min-height: 500px;
                }

                .editor-actions {
                    display: flex;
                    gap: var(--space-sm);
                }

                .editor-form {
                    display: flex;
                    flex-direction: column;
                    gap: var(--space-lg);
                }

                .form-row {
                    display: grid;
                    grid-template-columns: repeat(3, 1fr);
                    gap: var(--space-lg);
                }

                .code-input {
                    font-family: var(--font-mono);
                    font-size: 0.875rem;
                }

                .empty-state {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    height: 100%;
                    text-align: center;
                    color: var(--text-muted);
                    padding: var(--space-2xl);
                }

                .empty-state svg {
                    margin-bottom: var(--space-lg);
                    opacity: 0.5;
                }

                .empty-state h3 {
                    margin-bottom: var(--space-sm);
                    color: var(--text-secondary);
                }

                .empty-state p {
                    margin: 0;
                    font-size: 0.875rem;
                }

                @media (max-width: 1024px) {
                    .policies-layout {
                        grid-template-columns: 1fr;
                    }
                }
            `}</style>
        </div>
    );
};
