import { config } from '../config';
import SecureApi from './secureApi';

export interface AuditLog {
    logId: string;
    timestamp: string;
    actor: string;
    actorId?: string;
    action: string;
    resource: string;
    resourceId?: string;
    ip: string;
    status: 'success' | 'warning' | 'danger';
    details: string;
    userAgent?: string;
    sessionId?: string;
}

export interface AuditFilter {
    startDate?: string;
    endDate?: string;
    actor?: string;
    action?: string;
    status?: string;
    limit?: number;
    offset?: number;
}

export const AuditService = {
    async getLogs(filter?: AuditFilter): Promise<AuditLog[]> {
        const params = new URLSearchParams();
        if (filter) {
            Object.entries(filter).forEach(([key, value]) => {
                if (value !== undefined) params.append(key, String(value));
            });
        }

        const url = params.toString()
            ? `${config.API_BASE_URL}/audit?${params}`
            : `${config.API_BASE_URL}/audit`;

        return SecureApi.get(url);
    },

    async getLogById(id: string): Promise<AuditLog> {
        return SecureApi.get(`${config.API_BASE_URL}/audit/${id}`);
    },

    async exportLogs(filter?: AuditFilter): Promise<Blob> {
        const params = new URLSearchParams();
        if (filter) {
            Object.entries(filter).forEach(([key, value]) => {
                if (value !== undefined) params.append(key, String(value));
            });
        }

        const csvData = await SecureApi.getText(`${config.API_BASE_URL}/audit/export?${params}`);

        return new Blob([csvData], { type: 'text/csv' });
    },

    async getStats(): Promise<{
        totalEvents: number;
        successCount: number;
        warningCount: number;
        dangerCount: number;
        topActors: { actor: string; count: number }[];
        topActions: { action: string; count: number }[];
    }> {
        return SecureApi.get(`${config.API_BASE_URL}/audit/stats`);
    }
};

export default AuditService;
