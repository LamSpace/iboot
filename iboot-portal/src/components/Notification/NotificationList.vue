<template>
  <div class="notification-list">
    <div class="list-header">
      <div class="header-title">
        <span>{{ t("message.messageCenter.notification.title") }}</span>
        <el-badge
          :value="unreadCount"
          :hidden="unreadCount === 0"
          class="header-badge"
        />
      </div>
      <div class="header-actions">
        <el-button
          v-if="hasUnread"
          link
          type="primary"
          size="small"
          @click="handleMarkAllAsRead"
        >
          {{ t("message.messageCenter.notification.mark_all_read") }}
        </el-button>
        <el-button link type="info" size="small" @click="$emit('close')">
          {{ t("message.messageCenter.notification.close") }}
        </el-button>
      </div>
    </div>

    <el-divider style="margin: 8px 0" />

    <!-- 消息类型筛选 -->
    <div class="filter-tabs">
      <el-radio-group
        v-model="filterType"
        size="small"
        @change="handleFilterChange"
      >
        <el-radio-button label="">{{
          t("message.messageCenter.notification.filter.all")
        }}</el-radio-button>
        <el-radio-button label="notification">{{
          t("message.messageCenter.notification.filter.notification")
        }}</el-radio-button>
        <el-radio-button label="system">{{
          t("message.messageCenter.notification.filter.system")
        }}</el-radio-button>
        <el-radio-button label="urgent">{{
          t("message.messageCenter.notification.filter.important")
        }}</el-radio-button>
      </el-radio-group>
    </div>

    <el-divider style="margin: 8px 0" />

    <!-- 消息列表 -->
    <div class="list-content">
      <template v-if="filteredNotifications.length === 0">
        <div class="empty-state">
          <el-icon :size="64" color="#dcdfe6">
            <Bell />
          </el-icon>
          <p class="empty-text">
            {{ t("message.messageCenter.notification.no_message") }}
          </p>
        </div>
      </template>

      <template v-else>
        <div
          v-for="notification in filteredNotifications"
          :key="notification.id"
          :class="['list-item', { unread: !notification.read }]"
          @click="handleItemClick(notification)"
        >
          <div class="item-icon">
            <el-icon :size="24" :class="getIconClass(notification)">
              <component :is="getMessageIcon(notification)" />
            </el-icon>
          </div>
          <div class="item-content">
            <div class="item-header">
              <span class="item-title">{{ notification.title }}</span>
              <el-tag
                v-if="notification.priority === '2'"
                size="small"
                type="danger"
                effect="plain"
              >
                {{ t("message.messageCenter.notification.urgent") }}
              </el-tag>
              <el-tag
                v-else-if="notification.priority === '1'"
                size="small"
                type="warning"
                effect="plain"
              >
                {{ t("message.messageCenter.notification.important") }}
              </el-tag>
            </div>
            <p class="item-message">{{ notification.content }}</p>
            <div class="item-footer">
              <span class="item-time">{{
                formatTime(notification.sentAt)
              }}</span>
              <el-tag
                v-if="!notification.read"
                size="small"
                type="primary"
                effect="plain"
              >
                {{ t("message.messageCenter.notification.unread") }}
              </el-tag>
            </div>
          </div>
          <div class="item-actions">
            <el-button
              link
              type="danger"
              size="small"
              @click.stop="handleDelete(notification)"
            >
              {{ t("message.messageCenter.notification.delete") }}
            </el-button>
          </div>
        </div>
      </template>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="list-pagination">
      <el-pagination
        layout="prev, pager, next"
        :total="notifications.length"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
        size="small"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import {
  Bell,
  Message,
  Warning,
  DocumentChecked,
  InfoFilled,
} from "@element-plus/icons-vue";
import { usePush } from "@/composables/usePush";
import { useRouter } from "vue-router";
import type { Notification } from "@/stores/push";

const { t } = useI18n();

const router = useRouter();
const {
  unreadCount,
  notifications,
  hasUnread,
  markAsRead,
  removeNotification,
  clearUnread,
} = usePush();

const emit = defineEmits<{
  close: [];
  view: [notification: Notification];
}>();

const filterType = ref("");
const currentPage = ref(1);
const pageSize = ref(10);

// 按类型筛选
const filteredNotifications = computed(() => {
  let result = [...notifications.value];

  if (filterType.value === "urgent") {
    result = result.filter((n) => n.priority === "1" || n.priority === "2");
  } else if (filterType.value) {
    result = result.filter((n) => n.type === filterType.value);
  }

  return result;
});

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredNotifications.value.length / pageSize.value);
});

// 当前页的消息
const displayNotifications = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredNotifications.value.slice(start, end);
});

// 获取消息图标
const getMessageIcon = (notification: Notification) => {
  if (notification.priority === "2") {
    return Warning;
  }
  if (notification.priority === "1") {
    return InfoFilled;
  }
  if (notification.type === "system") {
    return DocumentChecked;
  }
  return Message;
};

// 获取图标样式类
const getIconClass = (notification: Notification) => {
  if (notification.priority === "2") {
    return "icon-danger";
  }
  if (notification.priority === "1") {
    return "icon-warning";
  }
  return "icon-primary";
};

// 处理筛选变化
const handleFilterChange = () => {
  currentPage.value = 1;
};

// 处理分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page;
};

// 处理 item 点击
const handleItemClick = (notification: Notification) => {
  if (!notification.read) {
    markAsRead(notification.id);
  }
  emit("view", notification);
  router.push("/message/messages");
};

// 标记全部已读
const handleMarkAllAsRead = () => {
  clearUnread();
};

// 删除消息
const handleDelete = (notification: Notification) => {
  removeNotification(notification.id);
};

// 格式化时间
const formatTime = (timeString: string) => {
  if (!timeString) return "";
  const date = new Date(timeString);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  if (diff < 60 * 1000)
    return t("message.messageCenter.notification.time.just_now");
  if (diff < 60 * 60 * 1000) {
    const minutes = Math.floor(diff / (60 * 1000));
    return t("message.messageCenter.notification.time.minutes_ago").replace(
      "{0}",
      String(minutes),
    );
  }
  if (diff < 24 * 60 * 60 * 1000) {
    const hours = Math.floor(diff / (60 * 60 * 1000));
    return t("message.messageCenter.notification.time.hours_ago").replace(
      "{0}",
      String(hours),
    );
  }
  const locale = document.documentElement.lang || "zh-CN";
  return date.toLocaleDateString(locale, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};
</script>

<style scoped>
.notification-list {
  width: 100%;
  background: #fff;
  border-radius: 8px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #303133;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-tabs {
  padding: 0 16px;
}

.list-content {
  max-height: 400px;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 16px;
  color: #909399;
}

.empty-text {
  margin-top: 16px;
  font-size: 14px;
}

.list-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.list-item:hover {
  background-color: #f5f7fa;
}

.list-item.unread {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.list-item.unread:hover {
  background-color: #d9ecff;
}

.item-icon {
  margin-right: 12px;
  display: flex;
  align-items: center;
}

.icon-primary {
  color: #409eff;
}

.icon-warning {
  color: #e6a23c;
}

.icon-danger {
  color: #f56c6c;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.item-title {
  font-weight: 500;
  color: #303133;
  font-size: 14px;
}

.item-message {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-time {
  font-size: 12px;
  color: #909399;
}

.item-actions {
  display: flex;
  align-items: center;
  margin-left: 8px;
}

.list-pagination {
  display: flex;
  justify-content: center;
  padding: 12px;
  border-top: 1px solid #ebeef5;
}

/* 滚动条样式 */
.list-content::-webkit-scrollbar {
  width: 6px;
}

.list-content::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.list-content::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>
