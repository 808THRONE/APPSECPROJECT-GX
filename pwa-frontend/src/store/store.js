import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { encryptData, decryptData } from '../security/crypto-storage.js';

/**
 * Zustand Global Store with Encrypted Persistence
 * SECURITY: Tokens stored in-memory ONLY (not persisted)
 */
const useStore = create(
    persist(
        (set, get) => ({
            // === Authentication State ===
            user: null,
            isAuthenticated: false,
            accessToken: null,  // IN-MEMORY ONLY - not persisted
            refreshToken: null, // IN-MEMORY ONLY - not persisted

            // === UI State ===
            currentView: 'login',
            sidebarOpen: true,
            theme: 'dark',

            // === Audit Logs ===
            auditLogs: [],

            // === ABAC Policies ===
            policies: [],

            // === Actions ===

            setUser: (user) => set({ user, isAuthenticated: !!user }),

            setTokens: (accessToken, refreshToken) => set({
                accessToken,
                refreshToken,
                isAuthenticated: true
            }),

            clearTokens: () => set({
                accessToken: null,
                refreshToken: null,
                isAuthenticated: false,
                user: null
            }),

            setCurrentView: (view) => set({ currentView: view }),

            toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),

            addAuditLog: (log) => set((state) => ({
                auditLogs: [log, ...state.auditLogs].slice(0, 1000) // Keep last 1000
            })),

            setPolicies: (policies) => set({ policies }),

            addPolicy: (policy) => set((state) => ({
                policies: [...state.policies, policy]
            })),

            updatePolicy: (policyId, updates) => set((state) => ({
                policies: state.policies.map(p =>
                    p.policy_id === policyId ? { ...p, ...updates } : p
                )
            })),

            deletePolicy: (policyId) => set((state) => ({
                policies: state.policies.filter(p => p.policy_id !== policyId)
            })),
        }),
        {
            name: 'securegate-storage',

            // Custom storage with encryption
            storage: {
                getItem: async (name) => {
                    const encrypted = localStorage.getItem(name);
                    if (!encrypted) return null;
                    try {
                        const decrypted = await decryptData(encrypted);
                        return decrypted;
                    } catch (error) {
                        console.error('Failed to decrypt state:', error);
                        return null;
                    }
                },

                setItem: async (name, value) => {
                    try {
                        const encrypted = await encryptData(value);
                        localStorage.setItem(name, encrypted);
                    } catch (error) {
                        console.error('Failed to encrypt state:', error);
                    }
                },

                removeItem: (name) => {
                    localStorage.removeItem(name);
                },
            },

            // Exclude sensitive data from persistence
            partialize: (state) => ({
                user: state.user,
                currentView: state.currentView,
                sidebarOpen: state.sidebarOpen,
                theme: state.theme,
                policies: state.policies,
                // EXCLUDE: accessToken, refreshToken (in-memory only)
            }),
        }
    )
);

export { useStore };
