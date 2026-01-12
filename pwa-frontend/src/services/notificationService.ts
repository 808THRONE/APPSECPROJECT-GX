import { config } from '../config';
import SecureApi from './secureApi';

export interface Notification {
    notificationId: string;
    title: string;
    message: string;
    type: 'INFO' | 'WARNING' | 'DANGER' | 'SUCCESS';
    category?: string;
    createdAt: string;
    read: boolean;
    actionUrl?: string;
    expiresAt?: string;
}

export const NotificationService = {
    async getNotifications(): Promise<Notification[]> {
        return SecureApi.get(`${config.API_BASE_URL}/notifications`);
    },

    async getUnreadCount(): Promise<number> {
        const data = await SecureApi.get(`${config.API_BASE_URL}/notifications/unread-count`);
        return data.count;
    },

    async markAsRead(id: string): Promise<void> {
        return SecureApi.post(`${config.API_BASE_URL}/notifications/${id}/read`, {});
    },

    async markAllAsRead(): Promise<void> {
        return SecureApi.post(`${config.API_BASE_URL}/notifications/mark-all-read`, {});
    },

    async deleteNotification(id: string): Promise<void> {
        return SecureApi.delete(`${config.API_BASE_URL}/notifications/${id}`);
    },

    async clearAll(): Promise<void> {
        return SecureApi.delete(`${config.API_BASE_URL}/notifications/clear`);
    }
};

export default NotificationService;
