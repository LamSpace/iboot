<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :title="t('message.notice.popup.title')"
    width="650px"
    :close-on-click-modal="false"
  >
    <div v-if="loading" v-loading="true" style="min-height: 200px"></div>
    <div v-else-if="notices.length === 0" class="empty-notice">
      <el-empty
        :description="t('message.notice.popup.no_notice')"
        :image-size="80"
      />
    </div>
    <div v-else class="notice-list">
      <div
        v-for="notice in notices"
        :key="notice.id"
        class="notice-item"
        :class="{ expanded: expandedId === notice.id }"
      >
        <div class="notice-header" @click="toggleExpand(notice)">
          <div class="notice-title-row">
            <el-tag
              v-if="notice.isTop === 1"
              type="danger"
              size="small"
              class="top-tag"
              >{{ t("message.notice.popup.top") }}</el-tag
            >
            <el-tag
              :type="
                dictStore.getDictListClass('sys_notice_type', notice.noticeType)
              "
              size="small"
              >{{
                dictStore.getDictLabel("sys_notice_type", notice.noticeType)
              }}</el-tag
            >
            <span class="notice-title">{{ notice.noticeTitle }}</span>
          </div>
          <div class="notice-meta">
            <span class="notice-author">{{ notice.createBy }}</span>
            <span class="notice-time">{{ notice.createTime }}</span>
          </div>
        </div>
        <div v-if="expandedId === notice.id" class="notice-content">
          <div class="notice-text">{{ notice.noticeContent }}</div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleMarkAllRead" :disabled="notices.length === 0">{{
        t("message.notice.popup.mark_all_read")
      }}</el-button>
      <el-button type="primary" @click="$emit('update:modelValue', false)">{{
        t("message.notice.popup.close")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import {
  getPublishedNotices,
  markNoticeAsRead,
  type PublishedNotice,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";

const { t } = useI18n();
const dictStore = useDictStore();

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  "read-change": [];
}>();

const loading = ref(false);
const notices = ref<PublishedNotice[]>([]);
const expandedId = ref<number | undefined>();

const loadNotices = async () => {
  loading.value = true;
  try {
    await dictStore.loadDicts("sys_notice_type");
    const res = await getPublishedNotices();
    if (res.code === 200) {
      // 只显示未读公告
      notices.value = (res.data || []).filter((n) => !n.isRead);
    }
  } finally {
    loading.value = false;
  }
};

const toggleExpand = async (notice: PublishedNotice) => {
  if (expandedId.value === notice.id) {
    expandedId.value = undefined;
    return;
  }
  expandedId.value = notice.id;
  // 展开即标记已读
  if (!notice.isRead && notice.id) {
    try {
      await markNoticeAsRead(notice.id);
      notice.isRead = true;
      emit("read-change");
    } catch {
      // 标记失败不影响查看
    }
  }
};

const handleMarkAllRead = async () => {
  const unread = notices.value.filter((n) => !n.isRead);
  for (const notice of unread) {
    if (notice.id) {
      try {
        await markNoticeAsRead(notice.id);
        notice.isRead = true;
      } catch {
        // continue
      }
    }
  }
  ElMessage.success(t("message.notice.messages.mark_all_read_success"));
  emit("read-change");
  emit("update:modelValue", false);
};

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      expandedId.value = undefined;
      loadNotices();
    }
  },
);
</script>

<style scoped>
.notice-list {
  max-height: 450px;
  overflow-y: auto;
}

.notice-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 10px;
  transition: box-shadow 0.2s;
}

.notice-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.notice-item.expanded {
  border-color: #409eff;
}

.notice-header {
  padding: 12px 16px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.notice-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.top-tag {
  flex-shrink: 0;
}

.notice-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 16px;
}

.notice-content {
  padding: 0 16px 12px;
  border-top: 1px solid #f0f0f0;
}

.notice-text {
  padding-top: 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}

.empty-notice {
  padding: 20px 0;
}
</style>
