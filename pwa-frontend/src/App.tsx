import React, { useEffect, useState } from 'react';
import { LoginPage } from './pages/LoginPage';
import { CallbackPage } from './pages/CallbackPage';
import { DashboardLayout } from './components/DashboardLayout';
import { UsersPage } from './pages/UsersPage';
import { PoliciesPage } from './pages/PoliciesPage';
import { AuditPage } from './pages/AuditPage';
import { StegoPage } from './pages/StegoPage';
import { RegisterPage } from './pages/RegisterPage';
import AuthService from './services/authService';

const App: React.FC = () => {
  const [route, setRoute] = useState(window.location.pathname);
  const [userInfo, setUserInfo] = useState<any>(null);
  const [isCheckingAuth, setIsCheckingAuth] = useState(AuthService.isAuthenticated());

  useEffect(() => {
    const handlePopState = () => setRoute(window.location.pathname);
    window.addEventListener('popstate', handlePopState);

    console.log('[App] Route change detected:', window.location.pathname);

    if (AuthService.isAuthenticated()) {
      console.log('[App] Auth hint found, verifying session...');
      setIsCheckingAuth(true);
      AuthService.getUserInfo()
        .then(info => {
          if (info) {
            console.log('[App] Session verified for:', info.username);
            setUserInfo(info);
          } else {
            console.warn('[App] Hint exists but session is invalid. Clearing.');
            localStorage.removeItem('auth_hint');
            if (route !== '/') setRoute('/');
          }
        })
        .catch(err => {
          console.error('[App] UserInfo fetch failed:', err);
          // If we can't reach the server, don't clear the hint immediately, 
          // but stop the loading state.
        })
        .finally(() => {
          setIsCheckingAuth(false);
        });
    } else {
      console.log('[App] No auth hint found.');
      setIsCheckingAuth(false);
    }

    return () => window.removeEventListener('popstate', handlePopState);
  }, [route]);

  const handleLogout = () => {
    console.log('[App] Handling logout');
    AuthService.logout();
  };

  if (route === '/callback') return <CallbackPage />;
  if (route === '/register') return <RegisterPage />;

  // While checking auth, show a loading screen or nothing (to prevent flicker)
  if (isCheckingAuth && !userInfo) {
    return (
      <div style={{ background: '#0f172a', color: 'white', height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', flexDirection: 'column' }}>
        <div style={{ width: '40px', height: '40px', border: '3px solid rgba(255,255,255,0.1)', borderTopColor: '#6366f1', borderRadius: '50%', animation: 'spin 1s linear infinite' }}></div>
        <p style={{ marginTop: '1rem', color: '#94a3b8' }}>Verifying secure session...</p>
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (!AuthService.isAuthenticated()) {
    console.log('[App] Rendering LoginPage');
    return <LoginPage />;
  }

  const renderContent = () => {
    console.log('[App] Rendering content for:', route);
    if (route === '/users') return <UsersPage />;
    if (route === '/policies') return <PoliciesPage />;
    if (route === '/audit') return <AuditPage />;
    if (route === '/stego') return <StegoPage />;
    if (route === '/') {
      return (
        <div>
          <h1>Dashboard Overview</h1>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginTop: '20px' }}>
            <div style={{ background: 'white', padding: '20px', borderRadius: '8px', borderLeft: '5px solid #3498db', color: '#333' }}>
              <h3>Total Users</h3>
              <p style={{ fontSize: '2rem', margin: 0 }}>142</p>
            </div>
            <div style={{ background: 'white', padding: '20px', borderRadius: '8px', borderLeft: '5px solid #e74c3c', color: '#333' }}>
              <h3>Security Alerts</h3>
              <p style={{ fontSize: '2rem', margin: 0 }}>3</p>
            </div>
            <div style={{ background: 'white', padding: '20px', borderRadius: '8px', borderLeft: '5px solid #2ecc71', color: '#333' }}>
              <h3>Active Sessions</h3>
              <p style={{ fontSize: '2rem', margin: 0 }}>28</p>
            </div>
          </div>

          <div style={{ marginTop: '2rem', padding: '1.5rem', background: 'white', borderRadius: '8px', color: '#333', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)' }}>
            <h3 style={{ marginTop: 0 }}>Welcome back, <span style={{ color: '#4f46e5' }}>{userInfo?.username || 'User'}</span>!</h3>
            <p>Your session is protected with RS256, HttpOnly Cookies, and CSRF isolation.</p>
            <div style={{ marginTop: '1rem', padding: '1rem', background: '#f8fafc', borderRadius: '6px', fontSize: '0.875rem' }}>
              <strong>System Integrity:</strong> All modules are currently operational.
            </div>
          </div>
        </div>
      );
    }
    return <div>404 - Page Not Found</div>;
  };

  return (
    <DashboardLayout onLogout={handleLogout}>
      {renderContent()}
    </DashboardLayout>
  );
};

export default App;
