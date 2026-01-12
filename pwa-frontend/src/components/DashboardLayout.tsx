import React from 'react';
import { NotificationService } from '../services/notificationService';
import type { Notification } from '../services/notificationService';
import { SettingService } from '../services/settingService';
import type { SystemSetting } from '../services/settingService';

interface LayoutProps {
    children: React.ReactNode;
    onLogout: () => void;
}

const navItems = [
    { name: 'Dashboard', path: '/', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
    { name: 'Users', path: '/users', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z' },
    { name: 'Policies', path: '/policies', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' },
    { name: 'Audit Logs', path: '/audit', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01' },
    { name: 'Stego Tool', path: '/stego', icon: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z' },
];

export const DashboardLayout: React.FC<LayoutProps> = ({ children, onLogout }) => {
    const currentPath = window.location.pathname;
    const [notifications, setNotifications] = React.useState<Notification[]>([]);
    const [showNotifications, setShowNotifications] = React.useState(false);
    const [settings, setSettings] = React.useState<SystemSetting[]>([]);
    const [showSettings, setShowSettings] = React.useState(false);

    const navigate = (path: string) => {
        window.history.pushState({}, '', path);
        window.dispatchEvent(new PopStateEvent('popstate'));
    };

    const loadData = async () => {
        try {
            console.log('[DashboardLayout] Loading dashboard data...');
            const [notes, conf] = await Promise.all([
                NotificationService.getNotifications().catch(e => {
                    console.error('Notes load failed:', e);
                    return [];
                }),
                SettingService.getSettings().catch(e => {
                    console.error('Settings load failed:', e);
                    return [];
                })
            ]);

            // Critical double-check: ensure we got arrays
            setNotifications(Array.isArray(notes) ? notes : []);
            setSettings(Array.isArray(conf) ? conf : []);
            console.log(`[DashboardLayout] Loaded ${Array.isArray(notes) ? notes.length : 0} notes and ${Array.isArray(conf) ? conf.length : 0} settings`);
        } catch (err) {
            console.error('Failed to load dashboard data', err);
            setNotifications([]);
            setSettings([]);
        }
    };

    React.useEffect(() => {
        loadData();
        const interval = setInterval(loadData, 30000); // Polling for notifications
        return () => clearInterval(interval);
    }, []);

    const markRead = async (id: string) => {
        await NotificationService.markAsRead(id);
        setNotifications(prev => prev.map(n => n.notificationId === id ? { ...n, read: true } : n));
    };

    const updateConf = async (key: string, value: string) => {
        await SettingService.updateSetting(key, value);
        loadData();
    };

    return (
        <div className="dashboard-layout">
            {/* Sidebar */}
            <aside className="sidebar">
                <div className="sidebar-header">
                    <div className="sidebar-logo">
                        <svg width="32" height="32" viewBox="0 0 48 48" fill="none">
                            <rect width="48" height="48" rx="10" fill="url(#sidebar-logo-grad)" />
                            <path d="M24 12L32 18V30L24 36L16 30V18L24 12Z" stroke="white" strokeWidth="2" fill="none" />
                            <circle cx="24" cy="24" r="3" fill="white" />
                            <defs>
                                <linearGradient id="sidebar-logo-grad" x1="0" y1="0" x2="48" y2="48">
                                    <stop stopColor="#6366f1" />
                                    <stop offset="1" stopColor="#8b5cf6" />
                                </linearGradient>
                            </defs>
                        </svg>
                        <span className="sidebar-title">SecureGate</span>
                    </div>
                </div>

                <nav className="sidebar-nav">
                    {navItems.map((item) => (
                        <a
                            key={item.name}
                            href={item.path}
                            className={`nav-item ${currentPath === item.path ? 'active' : ''}`}
                            onClick={(e) => {
                                e.preventDefault();
                                navigate(item.path);
                            }}
                        >
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                <path d={item.icon} />
                            </svg>
                            <span>{item.name}</span>
                        </a>
                    ))}
                </nav>

                <div className="sidebar-footer">
                    <div className="user-info">
                        <div className="user-avatar">A</div>
                        <div className="user-details">
                            <span className="user-name">Admin User</span>
                            <span className="user-role">Super Admin</span>
                        </div>
                    </div>
                    <button onClick={onLogout} className="logout-btn">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                            <polyline points="16 17 21 12 16 7" />
                            <line x1="21" y1="12" x2="9" y2="12" />
                        </svg>
                    </button>
                </div>
            </aside>

            {/* Main Content */}
            <main className="main-content">
                <header className="top-bar">
                    <div className="search-box">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="11" cy="11" r="8" />
                            <path d="m21 21-4.35-4.35" />
                        </svg>
                        <input type="text" placeholder="Search..." className="search-input" />
                    </div>
                    <div className="top-bar-actions">
                        <div style={{ position: 'relative' }}>
                            <button className="btn-icon" onClick={() => setShowNotifications(!showNotifications)}>
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                                </svg>
                                {Array.isArray(notifications) && notifications.filter(n => !n.read).length > 0 && (
                                    <span style={{ position: 'absolute', top: '5px', right: '5px', width: '8px', height: '8px', background: '#ef4444', borderRadius: '50%' }}></span>
                                )}
                            </button>
                            {showNotifications && (
                                <div className="dropdown notifications-dropdown">
                                    <div className="dropdown-header">Notifications</div>
                                    <div className="dropdown-list">
                                        {(!Array.isArray(notifications) || notifications.length === 0) ? (
                                            <div className="dropdown-item empty">No notifications</div>
                                        ) : notifications.map(n => (
                                            <div key={n.notificationId} className={`dropdown-item ${n.read ? 'read' : ''}`} onClick={() => markRead(n.notificationId)}>
                                                <div className="item-title">{n.title}</div>
                                                <div className="item-msg">{n.message}</div>
                                                <div className="item-time">{new Date(n.createdAt).toLocaleTimeString()}</div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                        <button className="btn-icon" onClick={() => setShowSettings(!showSettings)}>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                <circle cx="12" cy="12" r="3" />
                                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
                            </svg>
                        </button>
                    </div>
                </header>

                <div className="page-content">
                    {children}
                </div>

                {showSettings && (
                    <div className="modal-overlay" onClick={() => setShowSettings(false)}>
                        <div className="modal-content" style={{ width: '450px' }} onClick={e => e.stopPropagation()}>
                            <h3>System Parameters</h3>
                            <div style={{ marginTop: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                                {Array.isArray(settings) ? settings.map(s => (
                                    <div key={s.settingKey} className="form-group">
                                        <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{s.description || s.settingKey}</label>
                                        <input
                                            type="text"
                                            className="form-input"
                                            value={s.settingValue}
                                            onChange={e => updateConf(s.settingKey, e.target.value)}
                                        />
                                    </div>
                                )) : <div className="dropdown-item empty">Failed to load settings</div>}
                            </div>
                            <div className="modal-actions">
                                <button className="btn btn-primary" onClick={() => setShowSettings(false)}>Close</button>
                            </div>
                        </div>
                    </div>
                )}
            </main>

            <style>{`
                .dashboard-layout {
                    display: flex;
                    min-height: 100vh;
                    background: var(--bg-primary);
                }

                .sidebar {
                    width: 260px;
                    background: var(--bg-secondary);
                    border-right: 1px solid var(--border-color);
                    display: flex;
                    flex-direction: column;
                }

                .sidebar-header {
                    padding: var(--space-lg);
                    border-bottom: 1px solid var(--border-color);
                }

                .sidebar-logo {
                    display: flex;
                    align-items: center;
                    gap: var(--space-md);
                }

                .sidebar-title {
                    font-size: 1.25rem;
                    font-weight: 700;
                    background: var(--accent-gradient);
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                }

                .sidebar-nav {
                    flex: 1;
                    padding: var(--space-md);
                    display: flex;
                    flex-direction: column;
                    gap: var(--space-xs);
                }

                .nav-item {
                    display: flex;
                    align-items: center;
                    gap: var(--space-md);
                    padding: 0.75rem 1rem;
                    color: var(--text-secondary);
                    text-decoration: none;
                    border-radius: var(--radius-md);
                    transition: all var(--transition-fast);
                    position: relative;
                }

                .nav-item:hover {
                    color: var(--text-primary);
                    background: var(--bg-glass);
                }

                .nav-item.active {
                    color: var(--accent-primary);
                    background: rgba(99, 102, 241, 0.1);
                }

                .nav-item.active::before {
                    content: '';
                    position: absolute;
                    left: 0;
                    width: 3px;
                    height: 24px;
                    background: var(--accent-primary);
                    border-radius: 0 2px 2px 0;
                }

                .sidebar-footer {
                    padding: var(--space-lg);
                    border-top: 1px solid var(--border-color);
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }

                .user-info {
                    display: flex;
                    align-items: center;
                    gap: var(--space-md);
                }

                .user-avatar {
                    width: 40px;
                    height: 40px;
                    background: var(--accent-gradient);
                    border-radius: var(--radius-md);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: 600;
                    color: white;
                }

                .user-details {
                    display: flex;
                    flex-direction: column;
                }

                .user-name {
                    font-size: 0.875rem;
                    font-weight: 500;
                    color: var(--text-primary);
                }

                .user-role {
                    font-size: 0.75rem;
                    color: var(--text-muted);
                }

                .logout-btn {
                    background: transparent;
                    border: none;
                    color: var(--text-muted);
                    padding: 0.5rem;
                    border-radius: var(--radius-sm);
                    cursor: pointer;
                    transition: all var(--transition-fast);
                }

                .logout-btn:hover {
                    color: var(--danger);
                    background: rgba(239, 68, 68, 0.1);
                }

                .main-content {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }

                .top-bar {
                    height: 64px;
                    background: var(--bg-secondary);
                    border-bottom: 1px solid var(--border-color);
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    padding: 0 var(--space-xl);
                }

                .search-box {
                    display: flex;
                    align-items: center;
                    gap: var(--space-sm);
                    background: var(--bg-tertiary);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-md);
                    padding: 0.5rem 1rem;
                    width: 300px;
                }

                .search-box svg {
                    color: var(--text-muted);
                }

                .search-input {
                    flex: 1;
                    background: transparent;
                    border: none;
                    color: var(--text-primary);
                    font-size: 0.875rem;
                    outline: none;
                }

                .search-input::placeholder {
                    color: var(--text-muted);
                }

                .top-bar-actions {
                    display: flex;
                    gap: var(--space-sm);
                }

                .btn-icon {
                    width: 40px;
                    height: 40px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    background: transparent;
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-md);
                    color: var(--text-secondary);
                    cursor: pointer;
                    transition: all var(--transition-fast);
                    position: relative;
                }

                .btn-icon:hover {
                    background: var(--bg-glass);
                    border-color: var(--border-hover);
                    color: var(--text-primary);
                }

                .page-content {
                    flex: 1;
                    padding: var(--space-xl);
                    overflow: auto;
                }

                .dropdown {
                    position: absolute;
                    top: calc(100% + 10px);
                    right: 0;
                    width: 320px;
                    background: var(--bg-secondary);
                    border: 1px solid var(--border-color);
                    border-radius: var(--radius-lg);
                    box-shadow: var(--shadow-xl);
                    z-index: 100;
                    overflow: hidden;
                    animation: slideDown 0.2s ease-out;
                }

                .dropdown-header {
                    padding: 1rem;
                    font-weight: 600;
                    border-bottom: 1px solid var(--border-color);
                    background: var(--bg-tertiary);
                }

                .dropdown-list {
                    max-height: 400px;
                    overflow-y: auto;
                }

                .dropdown-item {
                    padding: 1rem;
                    border-bottom: 1px solid var(--border-color);
                    cursor: pointer;
                    transition: background 0.2s;
                }

                .dropdown-item:hover {
                    background: var(--bg-glass);
                }

                .dropdown-item.read {
                    opacity: 0.6;
                }

                .dropdown-item.empty {
                    text-align: center;
                    color: var(--text-muted);
                    padding: 2rem;
                }

                .item-title {
                    font-size: 0.875rem;
                    font-weight: 600;
                    margin-bottom: 0.25rem;
                }

                .item-msg {
                    font-size: 0.8125rem;
                    color: var(--text-secondary);
                    line-height: 1.4;
                }

                .item-time {
                    font-size: 0.75rem;
                    color: var(--text-muted);
                    margin-top: 0.5rem;
                }

                @keyframes slideDown {
                    from { transform: translateY(-10px); opacity: 0; }
                    to { transform: translateY(0); opacity: 1; }
                }
            `}</style>
        </div>
    );
};
