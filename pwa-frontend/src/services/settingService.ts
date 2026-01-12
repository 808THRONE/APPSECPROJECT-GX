import { config } from '../config';
import SecureApi from './secureApi';

export interface SystemSetting {
    settingKey: string;
    settingValue: string;
    description: string;
    category?: string;
    dataType?: 'string' | 'number' | 'boolean' | 'json';
    isEditable?: boolean;
    lastModifiedAt?: string;
    lastModifiedBy?: string;
}

export const SettingService = {
    async getSettings(): Promise<SystemSetting[]> {
        return SecureApi.get(`${config.API_BASE_URL}/settings`);
    },

    async getSettingByKey(key: string): Promise<SystemSetting> {
        return SecureApi.get(`${config.API_BASE_URL}/settings/${key}`);
    },

    async updateSetting(key: string, value: string): Promise<SystemSetting> {
        return SecureApi.put(`${config.API_BASE_URL}/settings/${key}`, { settingKey: key, settingValue: value });
    },

    async getSettingsByCategory(category: string): Promise<SystemSetting[]> {
        return SecureApi.get(`${config.API_BASE_URL}/settings/category/${category}`);
    },

    async resetToDefaults(): Promise<void> {
        return SecureApi.post(`${config.API_BASE_URL}/settings/reset`, {});
    },

    async exportSettings(): Promise<Record<string, string>> {
        return SecureApi.get(`${config.API_BASE_URL}/settings/export`);
    },

    async importSettings(settings: Record<string, string>): Promise<void> {
        return SecureApi.post(`${config.API_BASE_URL}/settings/import`, settings);
    }
};

export default SettingService;
