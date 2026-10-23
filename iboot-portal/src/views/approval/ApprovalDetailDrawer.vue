<template>
  <el-drawer
    :model-value="modelValue"
    :title="t('approval.detail.title')"
    size="480px"
    @update:model-value="emit('update:modelValue', $event)"
    @open="loadDetail"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('approval.columns.applicant')">
            {{ detail.application.applicantName || detail.application.applicantId }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('approval.columns.approver')">
            {{ detail.application.approverName || detail.application.approverId }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('approval.columns.leaveType')">
            {{ t(`approval.leaveType.${detail.application.leaveType}`) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('approval.columns.period')">
            {{ formatDateTime(detail.application.startTime) }} ~
            {{ formatDateTime(detail.application.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('approval.columns.reason')">
            {{ detail.application.reason }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('approval.columns.status')">
            <el-tag :type="statusType(detail.application.status)">
              {{ t(`approval.status.${detail.application.status}`) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item
            v-if="detail.application.approveComment"
            :label="t('approval.columns.comment')"
          >
            {{ detail.application.approveComment }}
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="history-title">{{ t("approval.detail.history") }}</h3>
        <el-timeline v-if="detail.history.length">
          <el-timeline-item
            v-for="(item, index) in detail.history"
            :key="index"
            :timestamp="formatDateTime(item.time)"
            :type="timelineType(item)"
          >
            <div class="history-item">
              <strong>{{ item.activityName }}</strong>
              <span v-if="item.actorName"> · {{ item.actorName }}</span>
              <div v-if="item.comment" class="history-comment">
                {{ item.comment }}
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else :description="t('approval.detail.emptyHistory')" />

        <div v-if="canDecide" class="decision">
          <el-input
            v-model="comment"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            :placeholder="t('approval.form.rejectCommentPlaceholder')"
          />
          <div class="decision-actions">
            <el-button type="primary" :loading="deciding" @click="decide(true)">
              {{ t("approval.actions.approve") }}
            </el-button>
            <el-button type="danger" :loading="deciding" @click="decide(false)">
              {{ t("approval.actions.reject") }}
            </el-button>
          </div>
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import {
  approveApplication,
  getApprovalDetail,
  rejectApplication,
  type ApprovalDetail,
  type ApprovalHistoryItem,
} from "@/api/approval";
import { usePermissionStore } from "@/stores/permission";
import { useUserStore } from "@/stores/user";
import { formatDateTime } from "@/utils/datetime";

const props = defineProps<{
  modelValue: boolean;
  applicationId: number | null;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  decided: [];
}>();

const { t } = useI18n();
const permissionStore = usePermissionStore();
const userStore = useUserStore();

const detail = ref<ApprovalDetail | null>(null);
const loading = ref(false);
const deciding = ref(false);
const comment = ref("");

const canDecide = computed(() => {
  const application = detail.value?.application;
  if (!application || application.status !== "PENDING") return false;
  return (
    permissionStore.hasPermission("approval:approve") &&
    application.approverId === userStore.userInfo?.id
  );
});

const loadDetail = async () => {
  if (!props.applicationId) return;
  loading.value = true;
  comment.value = "";
  try {
    const res = await getApprovalDetail(props.applicationId);
    if (res.code === 200) {
      detail.value = res.data;
    }
  } finally {
    loading.value = false;
  }
};

const decide = async (approved: boolean) => {
  if (!props.applicationId) return;
  if (!approved && !comment.value.trim()) {
    ElMessage.warning(t("approval.validation.rejectCommentRequired"));
    return;
  }
  deciding.value = true;
  try {
    const res = approved
      ? await approveApplication(props.applicationId, comment.value)
      : await rejectApplication(props.applicationId, comment.value.trim());
    if (res.code === 200) {
      ElMessage.success(
        t(approved ? "approval.messages.approved" : "approval.messages.rejected"),
      );
      emit("decided");
      emit("update:modelValue", false);
    }
  } finally {
    deciding.value = false;
  }
};

const statusType = (status: string): "success" | "warning" | "danger" => {
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "warning";
};

const timelineType = (item: ApprovalHistoryItem): "primary" | "success" | "danger" => {
  if (item.approved === true) return "success";
  if (item.approved === false) return "danger";
  return "primary";
};
</script>

<style scoped>
.history-title {
  margin: 20px 0 12px;
  font-size: 14px;
  font-weight: 600;
}

.history-item {
  font-size: 13px;
}

.history-comment {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}

.decision {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.decision-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
