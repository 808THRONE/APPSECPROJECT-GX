import { config } from '../config';
import SecureApi from './secureApi';

export interface User {
    userId?: string;
    username: string;
    email: string;
    fullName: string;
    status: string;
    department?: string;
    title?: string;
    lastLoginAt?: string;
    createdAt?: string;
    mfaEnabled?: boolean;
    roles?: { roleId: string; roleName: string }[];
}

export const UserService = {
    async getUsers(): Promise<User[]> {
        return SecureApi.get(`${config.API_BASE_URL}/users`);
    },

    async getUserById(id: string): Promise<User> {
        return SecureApi.get(`${config.API_BASE_URL}/users/${id}`);
    },

    async createUser(userData: Partial<User> & { password: string }): Promise<User> {
        return SecureApi.post(`${config.API_BASE_URL}/users`, userData);
    },

    async updateUser(id: string, userData: Partial<User>): Promise<User> {
        return SecureApi.put(`${config.API_BASE_URL}/users/${id}`, userData);
    },

    async deleteUser(id: string): Promise<void> {
        return SecureApi.delete(`${config.API_BASE_URL}/users/${id}`);
    },

    async toggleMfa(id: string, enabled: boolean): Promise<User> {
        return SecureApi.post(`${config.API_BASE_URL}/users/${id}/mfa`, { enabled });
    },

    async resetPassword(id: string): Promise<{ tempPassword: string }> {
        return SecureApi.post(`${config.API_BASE_URL}/users/${id}/reset-password`, {});
    }
};

export default UserService;
