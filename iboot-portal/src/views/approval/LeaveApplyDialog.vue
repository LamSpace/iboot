<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('approval.actions.apply')"
    width="520px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @open="loadApprovers"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-form-item :label="t('approval.form.approver')" prop="approverId">
        <el-select
          v-model="form.approverId"
          :placeholder="t('approval.form.approverPlaceholder')"
          filterable
          style="width: 100%"
        >
          <el-option
            v-for="user in approvers"
            :key="user.id"
            :label="user.nickname || user.username"
            :value="user.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('approval.form.leaveType')" prop="leaveType">
        <el-select
          v-model="form.leaveType"
          :placeholder="t('approval.form.leaveTypePlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="type in leaveTypes"
            :key="type"
            :label="t(`approval.leaveType.${type}`)"
            :value="type"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('approval.form.startTime')" prop="startTime">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('approval.form.endTime')" prop="endTime">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('approval.form.reason')" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          :placeholder="t('approval.form.reasonPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t("approval.actions.cancel") }}
      </el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        {{ t("approval.actions.submit") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { applyLeave } from "@/api/approval";
import { getUserList, type User } from "@/api/system";
import { useUserStore } from "@/stores/user";

defineProps<{ modelValue: boolean }>();
const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  success: [];
}>();

const { t } = useI18n();
const userStore = useUserStore();

const leaveTypes = ["ANNUAL", "SICK", "PERSONAL"];

const formRef = ref<FormInstance>();
const approvers = ref<User[]>([]);
const submitting = ref(false);

const form = reactive({
  approverId: undefined as number | undefined,
  leaveType: "ANNUAL",
  startTime: "",
  endTime: "",
  reason: "",
});

const rules: FormRules = {
  approverId: [
    { required: true, message: t("approval.validation.approverRequired"), trigger: "change" },
  ],
  leaveType: [
    { required: true, message: t("approval.validation.leaveTypeRequired"), trigger: "change" },
  ],
  startTime: [
    { required: true, message: t("approval.validation.startRequired"), trigger: "change" },
  ],
  endTime: [
    { required: true, message: t("approval.validation.endRequired"), trigger: "change" },
  ],
  reason: [
    { required: true, message: t("approval.validation.reasonRequired"), trigger: "blur" },
  ],
};

const loadApprovers = async () => {
  const res = await getUserList({ pageNum: 1, pageSize: 200, status: 1 });
  if (res.code === 200) {
    approvers.value = res.data.data.filter(
      (user) => user.id !== userStore.userInfo?.id,
    );
  }
};

const submit = async () => {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid || !form.approverId) return;
  submitting.value = true;
  try {
    const res = await applyLeave({
      approverId: form.approverId,
      leaveType: form.leaveType,
      startTime: form.startTime,
      endTime: form.endTime,
      reason: form.reason,
    });
    if (res.code === 200) {
      ElMessage.success(t("approval.messages.applied"));
      emit("update:modelValue", false);
      emit("success");
      formRef.value.resetFields();
    }
  } finally {
    submitting.value = false;
  }
};
</script>
