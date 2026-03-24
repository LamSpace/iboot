<template>
  <div class="notification-bell" @click="toggleDropdown">
    <el-badge
      :value="unreadCount"
      :hidden="unreadCount === 0"
      class="bell-badge"
    >
      <el-tooltip
        :content="t('message.messageCenter.notification.title')"
        placement="bottom"
      >
        <el-icon :size="20" class="bell-icon">
          <Bell />
        </el-icon>
      </el-tooltip>
    </el-badge>

    <!-- 消息列表下拉框 -->
    <div v-if="showDropdown" class="notification-dropdown" @click.stop>
      <div class="dropdown-header">
        <span class="header-title">{{
          t("message.messageCenter.notification.title")
        }}</span>
        <div class="header-actions">
          <el-link
            type="primary"
            :underline="false"
            @click="markAllAsRead"
            v-if="hasUnread"
          >
            {{ t("message.messageCenter.notification.mark_all_read") }}
          </el-link>
          <el-link type="info" :underline="false" @click="closeDropdown">
            {{ t("message.messageCenter.notification.close") }}
          </el-link>
        </div>
      </div>

      <el-divider style="margin: 8px 0" />

      <div class="dropdown-content">
        <template v-if="notifications.length === 0">
          <div class="empty-state">
            <el-icon :size="48" color="#dcdfe6">
              <Bell />
            </el-icon>
            <p class="empty-text">
              {{ t("message.messageCenter.notification.no_message") }}
            </p>
          </div>
        </template>

        <template v-else>
          <div
            v-for="notification in displayNotifications"
            :key="notification.id"
            :class="['notification-item', { unread: !notification.read }]"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-body">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-content">{{ notification.content }}</div>
              <div class="notification-meta">
                <span class="notification-time">{{
                  formatTime(notification.sentAt)
                }}</span>
                <el-tag
                  v-if="notification.priority === '2'"
                  size="small"
                  type="danger"
                  >{{ t("message.messageCenter.notification.urgent") }}</el-tag
                >
                <el-tag
                  v-else-if="notification.priority === '1'"
                  size="small"
                  type="warning"
                  >{{
                    t("message.messageCenter.notification.important")
                  }}</el-tag
                >
              </div>
            </div>
            <div class="notification-actions">
              <el-button
                link
                type="danger"
                size="small"
                @click.stop="removeNotification(notification.id)"
              >
                {{ t("message.messageCenter.notification.delete") }}
              </el-button>
            </div>
          </div>
        </template>
      </div>

      <div v-if="notifications.length > 0" class="dropdown-footer">
        <el-link type="primary" :underline="false" @click="viewAll">
          {{ t("message.messageCenter.notification.view_all") }}
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { Bell } from "@element-plus/icons-vue";
import { usePush } from "@/composables/usePush";
import { useRouter } from "vue-router";
import type { Notification } from "@/stores/push";

const { t } = useI18n();

const router = useRouter();
const {
  unreadCount,
  notifications,
  hasUnread,
  connect,
  markAsRead,
  removeNotification,
  clearUnread,
} = usePush();

const showDropdown = ref(false);

// 最多显示 10 条消息
const MAX_DISPLAY = 10;
const displayNotifications = computed(() => {
  return notifications.value.slice(0, MAX_DISPLAY);
});

// 切换下拉框
const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value;
};

// 关闭下拉框
const closeDropdown = () => {
  showDropdown.value = false;
};

// 标记全部已读
const markAllAsRead = () => {
  clearUnread();
  // TODO: 调用 API 更新后端状态
};

// 查看消息详情
const handleNotificationClick = (notification: Notification) => {
  if (!notification.read) {
    markAsRead(notification.id);
  }
  // 跳转到消息中心或消息详情
  router.push("/message/messages");
  closeDropdown();
};

// 删除消息
const handleRemoveNotification = (notificationId: string) => {
  removeNotification(notificationId);
};

// 查看全部
const viewAll = () => {
  router.push("/message/messages");
  closeDropdown();
};

// 格式化时间
const formatTime = (timeString: string) => {
  if (!timeString) return "";
  const date = new Date(timeString);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  // 1 分钟内
  if (diff < 60 * 1000) {
    return t("message.messageCenter.notification.time.just_now");
  }
  // 1 小时内
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000));
    return t("message.messageCenter.notification.time.minutes_ago").replace(
      "{0}",
      String(minutes),
    );
  }
  // 24 小时内
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000));
    return t("message.messageCenter.notification.time.hours_ago").replace(
      "{0}",
      String(hours),
    );
  }
  // 更久
  const locale = document.documentElement.lang || "zh-CN";
  return date.toLocaleDateString(locale, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

// 点击外部关闭下拉框
const handleClickOutside = (event: MouseEvent) => {
  if (showDropdown.value) {
    showDropdown.value = false;
  }
};

onMounted(() => {
  document.addEventListener("click", handleClickOutside);
  // 连接推送服务
  connect();
});

onUnmounted(() => {
  document.removeEventListener("click", handleClickOutside);
  // 断开连接（可选，通常页面关闭时会自动断开）
  // disconnect()
});

defineExpose({
  handleRemoveNotification,
});
</script>

<style scoped>
.notification-bell {
  position: relative;
  display: inline-block;
  cursor: pointer;
}

.bell-badge {
  padding: 4px;
}

.bell-icon {
  color: #606266;
  transition: color 0.3s;
}

.bell-icon:hover {
  color: #409eff;
}

.notification-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  width: 360px;
  max-height: 480px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  margin-top: 8px;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
}

.header-title {
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.dropdown-content {
  max-height: 360px;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  color: #909399;
}

.empty-icon {
  margin-bottom: 16px;
}

.empty-text {
  margin: 0;
  font-size: 14px;
}

.notification-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #ecf5ff;
}

.notification-item.unread:hover {
  background-color: #e1f3d8;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  font-size: 14px;
}

.notification-content {
  color: #606266;
  font-size: 13px;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

.notification-actions {
  display: flex;
  align-items: center;
}

.dropdown-footer {
  padding: 12px 16px;
  text-align: center;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
}

/* 滚动条样式 */
.dropdown-content::-webkit-scrollbar {
  width: 6px;
}

.dropdown-content::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.dropdown-content::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>
