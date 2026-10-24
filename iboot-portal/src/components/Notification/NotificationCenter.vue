<template>
  <el-dropdown
    trigger="click"
    placement="bottom-end"
    popper-class="notification-center-menu"
    @command="handleCommand"
  >
    <button
      type="button"
      class="notification-trigger"
      :aria-label="t('message.messageCenter.notification.title')"
      :title="t('message.messageCenter.notification.title')"
    >
      <el-badge
        :value="totalUnread"
        :hidden="totalUnread === 0"
        :max="99"
        class="notification-badge"
      >
        <el-icon :size="20"><Bell /></el-icon>
      </el-badge>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="messages">
          <el-icon class="menu-icon"><ChatDotRound /></el-icon>
          <span>{{ t("message.messageCenter.notification.title") }}</span>
          <span v-if="messageUnread > 0" class="menu-count">{{
            formatCount(messageUnread)
          }}</span>
        </el-dropdown-item>
        <el-dropdown-item command="notices">
          <el-icon class="menu-icon"><Notification /></el-icon>
          <span>{{ t("message.notice.title") }}</span>
          <span v-if="noticeUnread > 0" class="menu-count">{{
            formatCount(noticeUnread)
          }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <MessagePopup v-model="messagePopupVisible" @read-change="hydrate" />
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { Bell, ChatDotRound, Notification } from "@element-plus/icons-vue";
import { usePush } from "@/composables/usePush";
import MessagePopup from "@/components/MessagePopup.vue";

/**
 * 通知中心：用一个入口聚合站内消息与系统公告。
 *
 * <p>铃铛上的角标为两类未读数之和；下拉菜单分别打开个人收件箱和系统公告弹窗。
 * 站内消息未读数来自推送 store，公告未读数由父组件传入。
 */
const props = defineProps<{
  /** 系统公告未读数 */
  noticeUnread: number;
}>();

const emit = defineEmits<{
  /** 用户选择系统公告入口 */
  "open-notice": [];
}>();

const { t } = useI18n();
const { unreadCount, hydrate, connect } = usePush();
const messagePopupVisible = ref(false);

const messageUnread = computed(() => unreadCount.value);
const totalUnread = computed(
  () => messageUnread.value + (props.noticeUnread || 0),
);

/**
 * 把未读数格式化为菜单里展示的文本。
 *
 * @param count 未读数量
 * @return 未读文本，超过 99 时显示为 99+
 */
function formatCount(count: number): string {
  return count > 99 ? "99+" : String(count);
}

/**
 * 处理通知中心下拉菜单的选择。
 *
 * @param command 菜单命令：messages 或 notices
 */
function handleCommand(command: string): void {
  if (command === "messages") {
    messagePopupVisible.value = true;
    return;
  }
  if (command === "notices") {
    emit("open-notice");
  }
}

onMounted(() => {
  // 补齐离线期间未读消息，并建立实时推送连接。
  hydrate();
  connect();
});
</script>

<style scoped>
.notification-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--ib-radius-full);
  background: transparent;
  color: var(--ib-color-steel);
  cursor: pointer;
  transition:
    color var(--ib-duration-fast) var(--ib-ease),
    background var(--ib-duration-fast) var(--ib-ease);
}

.notification-trigger:hover {
  color: var(--ib-color-ink);
  background: var(--ib-color-surface);
}

.notification-trigger:focus-visible {
  outline: 2px solid var(--ib-color-brand-blue-deep);
  outline-offset: 2px;
}

.notification-badge {
  display: inline-flex;
}

:global(.notification-center-menu .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 168px;
  padding-right: 12px;
}

:global(.notification-center-menu .menu-icon) {
  color: var(--ib-color-steel);
}

:global(.notification-center-menu .menu-count) {
  margin-left: auto;
  min-width: 20px;
  padding: 0 6px;
  border-radius: var(--ib-radius-full);
  background: var(--ib-color-surface);
  color: var(--ib-color-steel);
  font-size: 12px;
  line-height: 18px;
  text-align: center;
}
</style>
