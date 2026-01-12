import React, { useState, useEffect } from 'react';
import { AuditService } from '../services/auditService';
import type { AuditLog } from '../services/auditService';

export const AuditPage: React.FC = () => {
    const [filter, setFilter] = useState('all');
    const [logs, setLogs] = useState<AuditLog[]>([]);
    const [loading, setLoading] = useState(true);

    const loadLogs = async () => {
        try {
            setLoading(true);
            const data = await AuditService.getLogs();
            setLogs(data);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadLogs();
    }, []);

    const filteredLogs = filter === 'all' ? logs : logs.filter(log => log.status === filter);

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'success':
                return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="20 6 9 17 4 12" /></svg>;
            case 'warning':
                return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" /><line x1="12" y1="9" x2="12" y2="13" /><line x1="12" y1="17" x2="12.01" y2="17" /></svg>;
            case 'danger':
                return <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" /></svg>;
            default:
                return null;
        }
    };

    return (
        <div className="audit-page animate-fade-in">
            {/* Page Header */}
            <div className="page-header">
                <div>
                    <h1>Audit Logs</h1>
                    <p>Security event monitoring and compliance tracking</p>
                </div>
                <div className="header-actions">
                    <button className="btn btn-secondary" onClick={loadLogs}>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
                        </svg>
                        Refresh
                    </button>
                    <button className="btn btn-secondary">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                            <polyline points="7 10 12 15 17 10" />
                            <line x1="12" y1="15" x2="12" y2="3" />
                        </svg>
                        Export
                    </button>
                </div>
            </div>

            {/* Filter Tabs */}
            <div className="filter-tabs">
                {['all', 'success', 'warning', 'danger'].map((status) => (
                    <button
                        key={status}
                        className={`filter-tab ${filter === status ? 'active' : ''}`}
                        onClick={() => setFilter(status)}
                    >
                        {status === 'all' && 'All Events'}
                        {status === 'success' && '✓ Success'}
                        {status === 'warning' && '⚠ Warnings'}
                        {status === 'danger' && '✕ Critical'}
                        <span className="tab-count">
                            {status === 'all' ? logs.length : logs.filter(l => l.status === status).length}
                        </span>
                    </button>
                ))}
            </div>

            {/* Audit Log Timeline */}
            <div className="audit-timeline">
                {loading ? (
                    <div className="empty-state">Loading logs...</div>
                ) : filteredLogs.length === 0 ? (
                    <div className="empty-state">No logs found matching criteria.</div>
                ) : filteredLogs.map((log, index) => (
                    <div key={log.logId} className={`timeline-item ${log.status}`} style={{ animationDelay: `${index * 50}ms` }}>
                        <div className="timeline-marker">
                            <div className={`marker-dot ${log.status}`}>
                                {getStatusIcon(log.status)}
                            </div>
                            {index < filteredLogs.length - 1 && <div className="marker-line" />}
                        </div>
                        <div className="timeline-content card">
                            <div className="log-header">
                                <div className="log-action">
                                    <span className={`action-badge ${log.status}`}>{log.action}</span>
                                    <span className="log-resource">{log.resource}</span>
                                </div>
                                <span className="log-timestamp">{new Date(log.timestamp).toLocaleString()}</span>
                            </div>
                            <div className="log-body">
                                <div className="log-details">{log.details}</div>
                                <div className="log-meta">
                                    <span className="meta-item">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                                            <circle cx="12" cy="7" r="4" />
                                        </svg>
                                        {log.actor}
                                    </span>
                                    <span className="meta-item">
                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" /><circle cx="12" cy="9" r="2.5" />
                                        </svg>
                                        {log.ip}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                ))}
            </div>


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

                .filter-tabs {
                    display: flex;
                    gap: var(--space-sm);
                    margin-bottom: var(--space-xl);
                    padding-bottom: var(--space-lg);
                    border-bottom: 1px solid var(--border-color);
                }

                .filter-tab {
                    display: flex;
                    align-items: center;
                    gap: var(--space-sm);
                    padding: 0.75rem 1.25rem;
                    background: transparent;
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-md);
                    color: var(--text-secondary);
                    font-size: 0.875rem;
                    font-weight: 500;
                    cursor: pointer;
                    transition: all var(--transition-fast);
                }

                .filter-tab:hover {
                    background: var(--bg-glass);
                    border-color: var(--border-hover);
                }

                .filter-tab.active {
                    background: var(--accent-primary);
                    border-color: var(--accent-primary);
                    color: white;
                }

                .tab-count {
                    background: rgba(255, 255, 255, 0.2);
                    padding: 0.125rem 0.5rem;
                    border-radius: var(--radius-full);
                    font-size: 0.75rem;
                }

                .filter-tab:not(.active) .tab-count {
                    background: var(--bg-tertiary);
                }

                .audit-timeline {
                    display: flex;
                    flex-direction: column;
                }

                .timeline-item {
                    display: flex;
                    gap: var(--space-lg);
                    animation: fadeIn 0.4s ease-out forwards;
                    opacity: 0;
                }

                .timeline-marker {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    width: 40px;
                    flex-shrink: 0;
                }

                .marker-dot {
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 1;
                }

                .marker-dot.success { background: rgba(16, 185, 129, 0.15); color: var(--success); }
                .marker-dot.warning { background: rgba(245, 158, 11, 0.15); color: var(--warning); }
                .marker-dot.danger { background: rgba(239, 68, 68, 0.15); color: var(--danger); }

                .marker-line {
                    width: 2px;
                    flex: 1;
                    background: var(--border-color);
                    margin: var(--space-sm) 0;
                }

                .timeline-content {
                    flex: 1;
                    margin-bottom: var(--space-lg);
                    padding: var(--space-lg);
                }

                .log-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    margin-bottom: var(--space-md);
                }

                .log-action {
                    display: flex;
                    align-items: center;
                    gap: var(--space-md);
                }

                .action-badge {
                    padding: 0.25rem 0.75rem;
                    border-radius: var(--radius-sm);
                    font-size: 0.75rem;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 0.02em;
                }

                .action-badge.success { background: rgba(16, 185, 129, 0.15); color: var(--success); }
                .action-badge.warning { background: rgba(245, 158, 11, 0.15); color: var(--warning); }
                .action-badge.danger { background: rgba(239, 68, 68, 0.15); color: var(--danger); }

                .log-resource {
                    font-size: 0.875rem;
                    color: var(--text-muted);
                }

                .log-timestamp {
                    font-size: 0.8125rem;
                    color: var(--text-muted);
                    font-family: var(--font-mono);
                }

                .log-details {
                    color: var(--text-primary);
                    margin-bottom: var(--space-md);
                }

                .log-meta {
                    display: flex;
                    gap: var(--space-xl);
                }

                .meta-item {
                    display: flex;
                    align-items: center;
                    gap: var(--space-xs);
                    font-size: 0.8125rem;
                    color: var(--text-muted);
                }

                @media (max-width: 768px) {
                    .filter-tabs {
                        flex-wrap: wrap;
                    }
                    
                    .log-header {
                        flex-direction: column;
                        align-items: flex-start;
                        gap: var(--space-sm);
                    }
                }

                .empty-state {
                    text-align: center;
                    padding: 3rem;
                    background: var(--bg-secondary);
                    border: 1px dashed var(--border-color);
                    border-radius: var(--radius-lg);
                    color: var(--text-muted);
                }
            `}</style>
        </div>
    );
};
