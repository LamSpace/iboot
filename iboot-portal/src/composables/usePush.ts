/**
 * Push Composable
 *
 * 提供推送通知相关的 Vue Composable API
 */

import { computed } from 'vue'
import { usePushStore, type Notification } from '@/stores/push'
import type { PushEvent } from '@/utils/eventsource'

export function usePush() {
  const pushStore = usePushStore()

  /**
   * 连接推送服务
   */
  const connect = () => {
    pushStore.connect()
  }

  /**
   * 断开推送服务
   */
  const disconnect = () => {
    pushStore.disconnect()
  }

  /**
   * 监听推送事件
   */
  const onPushEvent = (callback: (event: PushEvent) => void) => {
    pushStore.addEventListener(callback)

    // 返回取消订阅函数
    return () => {
      pushStore.removeEventListener(callback)
    }
  }

  /**
   * 监听新消息
   */
  const onNewMessage = (callback: (notification: Notification) => void) => {
    const handler = (event: PushEvent) => {
      if (event.type === 'com.iboot.push.new.message' || event.type === 'PUSH_NEW_MESSAGE') {
        callback(event.data)
      }
    }

    pushStore.addEventListener(handler)

    return () => {
      pushStore.removeEventListener(handler)
    }
  }

  /**
   * 标记消息已读
   */
  const markAsRead = (notificationId: string) => {
    pushStore.markAsRead(notificationId)
  }

  /**
   * 移除通知
   */
  const removeNotification = (notificationId: string) => {
    pushStore.removeNotification(notificationId)
  }

  /**
   * 清空未读消息
   */
  const clearUnread = () => {
    pushStore.clearUnread()
  }

  // 计算属性
  const connected = computed(() => pushStore.connected)
  const connecting = computed(() => pushStore.connecting)
  const unreadCount = computed(() => pushStore.unreadCount)
  const notifications = computed(() => pushStore.notifications)
  const hasUnread = computed(() => pushStore.hasUnread)
  const showNewMessageToast = computed(() => pushStore.showNewMessageToast)
  const latestNotification = computed(() => pushStore.latestNotification)

  return {
    // State
    connected,
    connecting,
    unreadCount,
    notifications,
    hasUnread,
    showNewMessageToast,
    latestNotification,

    // Actions
    connect,
    disconnect,
    onPushEvent,
    onNewMessage,
    markAsRead,
    removeNotification,
    clearUnread
  }
}
