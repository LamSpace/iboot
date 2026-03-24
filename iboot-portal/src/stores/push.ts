/**
 * 推送通知 Store
 *
 * 管理 SSE 推送连接状态、未读消息数量、事件订阅等
 */

import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { pushClient, type PushEvent } from "@/utils/eventsource";
import { useUserStore } from "./user";

export interface Notification {
  id: string;
  type: string;
  title: string;
  content: string;
  messageType: string;
  priority: string;
  senderId: number;
  sentAt: string;
  read: boolean;
}

export const usePushStore = defineStore("push", () => {
  const connected = ref(false);
  const connecting = ref(false);
  const unreadCount = ref(0);
  const notifications = ref<Notification[]>([]);
  const eventListeners = ref<Set<(event: PushEvent) => void>>(new Set());

  // 是否显示新消息提示
  const showNewMessageToast = ref(false);
  const latestNotification = ref<Notification | null>(null);

  // 计算属性
  const hasUnread = computed(() => unreadCount.value > 0);
  const notificationCount = computed(() => notifications.value.length);

  /**
   * 连接推送服务
   */
  function connect() {
    if (connected.value || connecting.value) {
      return;
    }

    const userStore = useUserStore();
    const token = userStore.token;

    if (!token) {
      console.log("[PushStore] 未登录，跳过连接");
      return;
    }

    connecting.value = true;
    console.log("[PushStore] 开始连接推送服务...");

    pushClient.onMessage(handlePushEvent);
    pushClient.connect(token);

    // 轮询检查连接状态
    const checkConnection = setInterval(() => {
      if (pushClient.isConnected()) {
        connected.value = true;
        connecting.value = false;
        clearInterval(checkConnection);
        console.log("[PushStore] 推送服务连接成功");
      }
    }, 100);

    // 超时处理
    setTimeout(() => {
      if (!connected.value) {
        connecting.value = false;
        clearInterval(checkConnection);
        console.warn("[PushStore] 连接超时");
      }
    }, 10000);
  }

  /**
   * 断开推送服务
   */
  function disconnect() {
    pushClient.offMessage(handlePushEvent);
    pushClient.disconnect();
    connected.value = false;
    connecting.value = false;
    console.log("[PushStore] 推送服务已断开");
  }

  /**
   * 处理推送事件
   */
  function handlePushEvent(event: PushEvent) {
    // 通知所有监听器
    eventListeners.value.forEach((listener) => {
      try {
        listener(event);
      } catch (error) {
        console.error("[PushStore] 监听器执行失败:", error);
      }
    });

    // 处理新消息推送
    if (
      event.type === "com.iboot.push.new.message" ||
      event.type === "PUSH_NEW_MESSAGE"
    ) {
      handleNewMessage(event.data);
    }

    // 处理消息已读推送
    if (
      event.type === "com.iboot.push.message.read" ||
      event.type === "PUSH_MESSAGE_READ"
    ) {
      handleMessageRead(event.data);
    }

    // 处理连接建立事件
    if (event.type === "com.iboot.push.connection.established") {
      console.log(
        "[PushStore] 连接建立成功，在线人数:",
        event.data?.onlineCount,
      );
    }
  }

  /**
   * 处理新消息
   */
  function handleNewMessage(data: any) {
    console.log("[PushStore] 收到新消息:", data);

    const notification: Notification = {
      id: data.messageId || `msg-${Date.now()}`,
      type: data.messageType || "notification",
      title: data.title || "新消息",
      content: data.content || "",
      messageType: data.messageType || "",
      priority: data.priority || "0",
      senderId: data.senderId,
      sentAt: data.sentAt || new Date().toISOString(),
      read: false,
    };

    // 添加到通知列表
    notifications.value.unshift(notification);

    // 更新未读数
    unreadCount.value++;

    // 更新最新消息
    latestNotification.value = notification;
    showNewMessageToast.value = true;

    // 3 秒后隐藏提示
    setTimeout(() => {
      showNewMessageToast.value = false;
    }, 3000);
  }

  /**
   * 处理消息已读
   */
  function handleMessageRead(data: any) {
    console.log("[PushStore] 消息已读通知:", data);
    // 可以在这里更新 UI 状态
  }

  /**
   * 添加事件监听器
   */
  function addEventListener(listener: (event: PushEvent) => void) {
    eventListeners.value.add(listener);
  }

  /**
   * 移除事件监听器
   */
  function removeEventListener(listener: (event: PushEvent) => void) {
    eventListeners.value.delete(listener);
  }

  /**
   * 标记消息已读
   */
  function markAsRead(notificationId: string) {
    const notification = notifications.value.find(
      (n) => n.id === notificationId,
    );
    if (notification) {
      notification.read = true;
      if (unreadCount.value > 0) {
        unreadCount.value--;
      }
    }
  }

  /**
   * 清空未读消息
   */
  function clearUnread() {
    unreadCount.value = 0;
    notifications.value.forEach((n) => {
      n.read = true;
    });
  }

  /**
   * 移除通知
   */
  function removeNotification(notificationId: string) {
    const index = notifications.value.findIndex((n) => n.id === notificationId);
    if (index > -1) {
      const notification = notifications.value[index];
      if (!notification.read && unreadCount.value > 0) {
        unreadCount.value--;
      }
      notifications.value.splice(index, 1);
    }
  }

  /**
   * 重置状态
   */
  function reset() {
    disconnect();
    connected.value = false;
    connecting.value = false;
    unreadCount.value = 0;
    notifications.value = [];
    eventListeners.value.clear();
    showNewMessageToast.value = false;
    latestNotification.value = null;
  }

  return {
    // State
    connected,
    connecting,
    unreadCount,
    notifications,
    showNewMessageToast,
    latestNotification,

    // Getters
    hasUnread,
    notificationCount,

    // Actions
    connect,
    disconnect,
    addEventListener,
    removeEventListener,
    markAsRead,
    clearUnread,
    removeNotification,
    reset,
  };
});
