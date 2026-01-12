import { config } from '../config';
import SecureApi from './secureApi';

export interface Policy {
    policyId?: string;
    name: string;
    effect: 'PERMIT' | 'DENY';
    resource: string;
    action: string;
    description: string;
    conditions: string;
    priority?: number;
    isActive?: boolean;
    createdAt?: string;
    createdBy?: string;
}

export const PolicyService = {
    async getPolicies(): Promise<Policy[]> {
        return SecureApi.get(`${config.API_BASE_URL}/policies`);
    },

    async getPolicyById(id: string): Promise<Policy> {
        return SecureApi.get(`${config.API_BASE_URL}/policies/${id}`);
    },

    async createPolicy(policy: Policy): Promise<Policy> {
        return SecureApi.post(`${config.API_BASE_URL}/policies`, policy);
    },

    async updatePolicy(id: string, policy: Policy): Promise<Policy> {
        return SecureApi.put(`${config.API_BASE_URL}/policies/${id}`, policy);
    },

    async deletePolicy(id: string): Promise<void> {
        return SecureApi.delete(`${config.API_BASE_URL}/policies/${id}`);
    },

    async togglePolicy(id: string, isActive: boolean): Promise<Policy> {
        return SecureApi.post(`${config.API_BASE_URL}/policies/${id}/toggle`, { isActive });
    },

    async evaluatePolicy(resource: string, action: string, context: Record<string, unknown>): Promise<{ decision: 'PERMIT' | 'DENY'; matchedPolicy?: string }> {
        return SecureApi.post(`${config.API_BASE_URL}/policies/evaluate`, { resource, action, context });
    }
};

export default PolicyService;
