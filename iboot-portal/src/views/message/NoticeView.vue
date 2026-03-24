<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button
          type="primary"
          v-permission="'notice:add'"
          @click="handleAdd"
          >{{ t("message.notice.actions.add") }}</el-button
        >
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("message.notice.actions.export") }}</el-button
        >
      </div>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('message.notice.search.title')">
          <el-input
            v-model="queryParams.noticeTitle"
            :placeholder="t('message.notice.search.placeholder')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('message.notice.search.type')">
          <el-select
            v-model="queryParams.noticeType"
            :placeholder="t('message.notice.search.placeholder')"
            clearable
          >
            <el-option
              v-for="item in dictStore.getDict('sys_notice_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('message.notice.search.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('message.notice.search.placeholder')"
            clearable
          >
            <el-option
              v-for="item in dictStore.getDict('sys_notice_status')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("message.notice.actions.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("message.notice.actions.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="noticeList" style="width: 100%" v-loading="loading">
        <el-table-column
          prop="id"
          :label="t('message.notice.table.id')"
          width="80"
          align="center"
        />
        <el-table-column
          prop="noticeTitle"
          :label="t('message.notice.table.title')"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="noticeType"
          :label="t('message.notice.table.type')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass('sys_notice_type', row.noticeType)
              "
            >
              {{ dictStore.getDictLabel("sys_notice_type", row.noticeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          :label="t('message.notice.table.status')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass('sys_notice_status', row.status)
              "
            >
              {{ dictStore.getDictLabel("sys_notice_status", row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="isTop"
          :label="t('message.notice.table.is_top')"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-tag v-if="row.isTop === 1" type="danger" size="small">{{
              t("message.notice.table.is_top")
            }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="createBy"
          :label="t('message.notice.table.create_by')"
          width="120"
        />
        <el-table-column
          prop="createTime"
          :label="t('message.notice.table.create_time')"
          width="180"
        />
        <el-table-column
          :label="t('message.notice.table.actions')"
          width="280"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-permission="'notice:edit'"
              type="primary"
              link
              @click="handleEdit(row)"
              :disabled="row.status === '1'"
              >{{ t("message.notice.actions.edit") }}</el-button
            >
            <el-button
              v-permission="'notice:publish'"
              type="success"
              link
              @click="handlePublish(row)"
              v-if="row.status !== '1'"
              >{{ t("message.notice.actions.publish") }}</el-button
            >
            <el-button
              v-permission="'notice:publish'"
              type="warning"
              link
              @click="handleRevoke(row)"
              v-if="row.status === '1'"
              >{{ t("message.notice.actions.revoke") }}</el-button
            >
            <el-button
              v-permission="'notice:remove'"
              type="danger"
              link
              @click="handleDelete(row)"
              :disabled="row.status === '1'"
              >{{ t("message.notice.actions.delete") }}</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="pagination"
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item
          :label="t('message.notice.dialog.title_label')"
          prop="noticeTitle"
        >
          <el-input
            v-model="form.noticeTitle"
            :placeholder="t('message.notice.dialog.title_placeholder')"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          :label="t('message.notice.dialog.type_label')"
          prop="noticeType"
        >
          <el-radio-group v-model="form.noticeType">
            <el-radio
              v-for="item in dictStore.getDict('sys_notice_type')"
              :key="item.dictValue"
              :value="item.dictValue"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="t('message.notice.dialog.content_label')"
          prop="noticeContent"
        >
          <el-input
            v-model="form.noticeContent"
            type="textarea"
            :rows="8"
            :placeholder="t('message.notice.dialog.content_placeholder')"
          />
        </el-form-item>
        <el-form-item :label="t('message.notice.dialog.is_top_label')">
          <el-switch
            v-model="form.isTop"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
        <el-form-item :label="t('message.notice.dialog.remark_label')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('message.notice.dialog.remark_placeholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{
          t("message.notice.dialog.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("message.notice.dialog.confirm")
        }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";
import {
  getNoticeList,
  addNotice,
  updateNotice,
  deleteNotice,
  publishNotice,
  revokeNotice,
  exportNoticeList,
  type Notice,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportNoticeList({
        noticeTitle: queryParams.noticeTitle,
        noticeType: queryParams.noticeType,
        status: queryParams.status,
      }),
    t("message.notice.title"),
  );
};

const loading = ref(false);
const noticeList = ref<Notice[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  noticeTitle: undefined as string | undefined,
  noticeType: undefined as string | undefined,
  status: undefined as string | undefined,
});

const defaultForm: Notice = {
  noticeTitle: "",
  noticeType: "1",
  noticeContent: "",
  isTop: 0,
  remark: "",
};

const form = reactive<Notice>({ ...defaultForm });

const rules: FormRules = {
  noticeTitle: [
    {
      required: true,
      message: t("message.notice.validation.title_required"),
      trigger: "blur",
    },
  ],
  noticeType: [
    {
      required: true,
      message: t("message.notice.validation.type_required"),
      trigger: "change",
    },
  ],
  noticeContent: [
    {
      required: true,
      message: t("message.notice.validation.content_required"),
      trigger: "blur",
    },
  ],
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getNoticeList(queryParams);
    if (res.code === 200) {
      noticeList.value = res.data.data;
      total.value = res.data.total;
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNum = 1;
  loadData();
};

const handleReset = () => {
  queryParams.noticeTitle = undefined;
  queryParams.noticeType = undefined;
  queryParams.status = undefined;
  handleSearch();
};

const resetForm = () => {
  Object.assign(form, defaultForm);
  form.id = undefined;
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = t("message.notice.dialog.add_title");
  dialogVisible.value = true;
};

const handleEdit = (row: Notice) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("message.notice.dialog.edit_title");
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  if (form.id) {
    await updateNotice(form);
    ElMessage.success(t("message.notice.messages.update_success"));
  } else {
    await addNotice(form);
    ElMessage.success(t("message.notice.messages.save_success"));
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: Notice) => {
  ElMessageBox.confirm(
    t("message.notice.messages.delete_confirm"),
    t("message.notice.table.title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteNotice(row.id!);
      ElMessage.success(t("message.notice.messages.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handlePublish = (row: Notice) => {
  ElMessageBox.confirm(
    t("message.notice.messages.publish_confirm"),
    t("message.notice.table.title"),
    { type: "info" },
  )
    .then(async () => {
      await publishNotice(row.id!);
      ElMessage.success(t("message.notice.messages.publish_success"));
      loadData();
    })
    .catch(() => {});
};

const handleRevoke = (row: Notice) => {
  ElMessageBox.confirm(
    t("message.notice.messages.revoke_confirm"),
    t("message.notice.table.title"),
    { type: "warning" },
  )
    .then(async () => {
      await revokeNotice(row.id!);
      ElMessage.success(t("message.notice.messages.revoke_success"));
      loadData();
    })
    .catch(() => {});
};

onMounted(() => {
  dictStore.loadDicts("sys_notice_type", "sys_notice_status");
  loadData();
});
</script>

<style scoped>
.content-wrapper {
  padding: 20px;
}

.content-body {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.action-bar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
