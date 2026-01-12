import React, { useState, useEffect } from 'react';
import { UserService } from '../services/userService';
import type { User } from '../services/userService';

export const UsersPage: React.FC = () => {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        fullName: ''
    });

    const loadUsers = async () => {
        try {
            setLoading(true);
            const data = await UserService.getUsers();
            setUsers(data);
            setError(null);
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadUsers();
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await UserService.createUser(formData);
            setIsModalOpen(false);
            setFormData({ username: '', email: '', password: '', fullName: '' });
            loadUsers();
        } catch (err: any) {
            alert(err.message);
        }
    };

    return (
        <div className="users-page animate-fade-in">
            {/* Page Header */}
            <div className="page-header">
                <div>
                    <h1>User Management</h1>
                    <p>Manage user accounts, roles, and permissions</p>
                </div>
                <button className="btn btn-primary" onClick={() => setIsModalOpen(true)}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="12" y1="5" x2="12" y2="19" />
                        <line x1="5" y1="12" x2="19" y2="12" />
                    </svg>
                    Add User
                </button>
            </div>

            {/* Stats Cards */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon stat-icon-blue">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                            <circle cx="9" cy="7" r="4" />
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                        </svg>
                    </div>
                    <div className="stat-content">
                        <span className="stat-value">{users.length}</span>
                        <span className="stat-label">Total Users</span>
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon stat-icon-green">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                            <polyline points="22 4 12 14.01 9 11.01" />
                        </svg>
                    </div>
                    <div className="stat-content">
                        <span className="stat-value">{users.filter(u => u.status === 'ACTIVE').length}</span>
                        <span className="stat-label">Active Users</span>
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon stat-icon-yellow">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="12" y1="8" x2="12" y2="12" />
                            <line x1="12" y1="16" x2="12.01" y2="16" />
                        </svg>
                    </div>
                    <div className="stat-content">
                        <span className="stat-value">0</span>
                        <span className="stat-label">Pending</span>
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon stat-icon-red">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="15" y1="9" x2="9" y2="15" />
                            <line x1="9" y1="9" x2="15" y2="15" />
                        </svg>
                    </div>
                    <div className="stat-content">
                        <span className="stat-value">0</span>
                        <span className="stat-label">Inactive</span>
                    </div>
                </div>
            </div>

            {/* Users Table */}
            <div className="card">
                <div className="card-header">
                    <h3 className="card-title">All Users</h3>
                    <div className="table-actions">
                        <div className="search-filter">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="11" cy="11" r="8" />
                                <path d="m21 21-4.35-4.35" />
                            </svg>
                            <input type="text" placeholder="Search users..." />
                        </div>
                    </div>
                </div>
                {loading ? (
                    <div style={{ padding: '2rem', textAlign: 'center' }}>Loading users...</div>
                ) : error ? (
                    <div style={{ padding: '2rem', textAlign: 'center', color: '#ef4444' }}>Error: {error}</div>
                ) : (
                    <table className="table">
                        <thead>
                            <tr>
                                <th>User</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((user) => (
                                <tr key={user.userId}>
                                    <td>
                                        <div className="user-cell">
                                            <div className="user-avatar-sm">{user.username.charAt(0).toUpperCase()}</div>
                                            <div>
                                                <div className="user-name-cell">{user.fullName}</div>
                                                <div className="user-email-cell">{user.email}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td><span className="role-badge">USER</span></td>
                                    <td>
                                        <span className={`badge badge-${user.status === 'ACTIVE' ? 'success' : 'danger'}`}>
                                            {user.status}
                                        </span>
                                    </td>
                                    <td>
                                        <div className="action-buttons">
                                            <button className="btn-icon-sm" title="Edit">
                                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                                                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                                                </svg>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>

            {/* Add User Modal */}
            {isModalOpen && (
                <div className="modal-overlay">
                    <div className="modal-content">
                        <h3>Add New User</h3>
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label>Username</label>
                                <input
                                    type="text"
                                    required
                                    value={formData.username}
                                    onChange={e => setFormData({ ...formData, username: e.target.value })}
                                />
                            </div>
                            <div className="form-group">
                                <label>Full Name</label>
                                <input
                                    type="text"
                                    required
                                    value={formData.fullName}
                                    onChange={e => setFormData({ ...formData, fullName: e.target.value })}
                                />
                            </div>
                            <div className="form-group">
                                <label>Email</label>
                                <input
                                    type="email"
                                    required
                                    value={formData.email}
                                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                                />
                            </div>
                            <div className="form-group">
                                <label>Password</label>
                                <input
                                    type="password"
                                    required
                                    value={formData.password}
                                    onChange={e => setFormData({ ...formData, password: e.target.value })}
                                />
                            </div>
                            <div className="modal-actions">
                                <button type="button" className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>Cancel</button>
                                <button type="submit" className="btn btn-primary">Create User</button>
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

                .stats-grid {
                    display: grid;
                    grid-template-columns: repeat(4, 1fr);
                    gap: var(--space-lg);
                    margin-bottom: var(--space-xl);
                }

                .stat-card {
                    background: var(--bg-card);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-lg);
                    padding: var(--space-lg);
                    display: flex;
                    align-items: center;
                    gap: var(--space-lg);
                    backdrop-filter: blur(20px);
                }

                .stat-icon {
                    width: 48px;
                    height: 48px;
                    border-radius: var(--radius-md);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }

                .stat-icon-blue { background: rgba(59, 130, 246, 0.15); color: #3b82f6; }
                .stat-icon-green { background: rgba(16, 185, 129, 0.15); color: #10b981; }
                .stat-icon-yellow { background: rgba(245, 158, 11, 0.15); color: #f59e0b; }
                .stat-icon-red { background: rgba(239, 68, 68, 0.15); color: #ef4444; }

                .stat-content {
                    display: flex;
                    flex-direction: column;
                }

                .stat-value {
                    font-size: 1.5rem;
                    font-weight: 700;
                    color: var(--text-primary);
                }

                .stat-label {
                    font-size: 0.875rem;
                    color: var(--text-muted);
                }

                .table-actions {
                    display: flex;
                    gap: var(--space-md);
                }

                .search-filter {
                    display: flex;
                    align-items: center;
                    gap: var(--space-sm);
                    background: var(--bg-tertiary);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-md);
                    padding: 0.5rem 0.75rem;
                }

                .search-filter input {
                    background: transparent;
                    border: none;
                    color: var(--text-primary);
                    font-size: 0.875rem;
                    outline: none;
                    width: 150px;
                }

                .search-filter svg {
                    color: var(--text-muted);
                }

                .user-cell {
                    display: flex;
                    align-items: center;
                    gap: var(--space-md);
                }

                .user-avatar-sm {
                    width: 36px;
                    height: 36px;
                    background: var(--accent-gradient);
                    border-radius: var(--radius-md);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: 600;
                    font-size: 0.875rem;
                    color: white;
                }

                .user-name-cell {
                    font-weight: 500;
                    color: var(--text-primary);
                }

                .user-email-cell {
                    font-size: 0.8125rem;
                    color: var(--text-muted);
                }

                .role-badge {
                    background: var(--bg-tertiary);
                    padding: 0.25rem 0.75rem;
                    border-radius: var(--radius-full);
                    font-size: 0.75rem;
                    font-weight: 500;
                    color: var(--text-secondary);
                }

                .action-buttons {
                    display: flex;
                    gap: var(--space-sm);
                }

                .btn-icon-sm {
                    width: 32px;
                    height: 32px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: transparent;
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-sm);
                    color: var(--text-muted);
                    cursor: pointer;
                    transition: all var(--transition-fast);
                }

                .btn-icon-sm:hover {
                    background: var(--bg-glass);
                    color: var(--text-primary);
                    border-color: var(--border-hover);
                }

                @media (max-width: 1024px) {
                    .stats-grid {
                        grid-template-columns: repeat(2, 1fr);
                    }
                }

                @media (max-width: 768px) {
                    .stats-grid {
                        grid-template-columns: 1fr;
                    }
                    .page-header {
                        flex-direction: column;
                        gap: var(--space-md);
                    }
                }

                .modal-overlay {
                    position: fixed;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                    background: rgba(0, 0, 0, 0.7);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    z-index: 1000;
                    backdrop-filter: blur(4px);
                }

                .modal-content {
                    background: var(--bg-card);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-lg);
                    padding: var(--space-xl);
                    width: 400px;
                    max-width: 90%;
                    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5);
                }

                .modal-content h3 {
                    margin-bottom: var(--space-lg);
                }

                .form-group {
                    margin-bottom: var(--space-md);
                }

                .form-group label {
                    display: block;
                    margin-bottom: 0.5rem;
                    font-size: 0.875rem;
                    color: var(--text-secondary);
                }

                .form-group input {
                    width: 100%;
                    padding: 0.75rem;
                    background: var(--bg-tertiary);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-md);
                    color: var(--text-primary);
                    outline: none;
                }

                .form-group input:focus {
                    border-color: var(--color-primary);
                }

                .modal-actions {
                    display: flex;
                    justify-content: flex-end;
                    gap: var(--space-md);
                    margin-top: var(--space-xl);
                }
            `}</style>
        </div>
    );
};
