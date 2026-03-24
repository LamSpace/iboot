<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-upload
          ref="uploadRef"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :data="uploadData"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :show-file-list="false"
          multiple
        >
          <el-button type="primary"
            ><el-icon><Upload /></el-icon>
            {{ t("file.fileManage.actions.upload") }}</el-button
          >
        </el-upload>
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("file.fileManage.actions.export") }}</el-button
        >
      </div>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('file.fileManage.search.file_name')">
          <el-input
            v-model="queryParams.fileName"
            :placeholder="t('file.fileManage.search.file_name_placeholder')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('file.fileManage.search.file_category')">
          <el-select
            v-model="queryParams.fileCategory"
            :placeholder="t('file.fileManage.search.select_category')"
            clearable
          >
            <el-option
              v-for="item in dictStore.getDict('sys_file_category')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("file.fileManage.actions.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("file.fileManage.actions.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 文件列表 -->
      <el-table :data="fileList" style="width: 100%" v-loading="loading">
        <el-table-column
          prop="id"
          :label="t('file.fileManage.table.id')"
          width="80"
          align="center"
        />
        <el-table-column
          prop="fileName"
          :label="t('file.fileManage.table.file_name')"
          min-width="200"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-icon v-if="row.fileCategory === 'image'" color="#67C23A"
              ><Picture
            /></el-icon>
            <el-icon v-else-if="row.fileCategory === 'video'" color="#E6A23C"
              ><VideoCamera
            /></el-icon>
            <el-icon v-else-if="row.fileCategory === 'audio'" color="#409EFF"
              ><Headset
            /></el-icon>
            <el-icon v-else-if="row.fileCategory === 'archive'" color="#F56C6C"
              ><Box
            /></el-icon>
            <el-icon v-else color="#909399"><Document /></el-icon>
            <span style="margin-left: 6px">{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="readableSize"
          :label="t('file.fileManage.table.file_size')"
          width="120"
          align="center"
        />
        <el-table-column
          prop="fileExt"
          :label="t('file.fileManage.table.file_ext')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag size="small" v-if="row.fileExt">.{{ row.fileExt }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="fileCategory"
          :label="t('file.fileManage.table.file_category')"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass(
                  'sys_file_category',
                  row.fileCategory,
                )
              "
            >
              {{
                dictStore.getDictLabel("sys_file_category", row.fileCategory)
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="uploadBy"
          :label="t('file.fileManage.table.upload_by')"
          width="120"
          align="center"
        />
        <el-table-column
          prop="uploadTime"
          :label="t('file.fileManage.table.upload_time')"
          width="180"
        />
        <el-table-column
          :label="t('file.fileManage.table.actions')"
          width="240"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">{{
              t("file.fileManage.actions.download")
            }}</el-button>
            <el-button type="warning" link @click="handleEdit(row)">{{
              t("file.fileManage.actions.edit")
            }}</el-button>
            <el-button type="danger" link @click="handleDelete(row)">{{
              t("file.fileManage.actions.delete")
            }}</el-button>
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

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="t('file.fileManage.dialog.edit_title')"
      width="500px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item
          :label="t('file.fileManage.dialog.file_name_label')"
          prop="fileName"
        >
          <el-input
            v-model="form.fileName"
            :placeholder="t('file.fileManage.dialog.file_name_placeholder')"
          />
        </el-form-item>
        <el-form-item :label="t('file.fileManage.dialog.file_category_label')">
          <el-select
            v-model="form.fileCategory"
            :placeholder="t('file.fileManage.dialog.file_category_placeholder')"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in dictStore.getDict('sys_file_category')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('file.fileManage.dialog.remark_label')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="t('file.fileManage.dialog.remark_placeholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{
          t("file.fileManage.dialog.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("file.fileManage.dialog.confirm")
        }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRawFile,
} from "element-plus";
import {
  Upload,
  Picture,
  VideoCamera,
  Headset,
  Box,
  Document,
} from "@element-plus/icons-vue";
import {
  getFileList,
  uploadFile,
  updateFileInfo,
  deleteFile,
  getFileDownloadUrl,
  exportFileList,
  type FileInfo,
  type FileUpdateParams,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useUserStore } from "@/stores/user";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const userStore = useUserStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportFileList({
        fileName: queryParams.fileName,
        fileCategory: queryParams.fileCategory,
      }),
    t("file.fileManage.title"),
  );
};

const loading = ref(false);
const fileList = ref<FileInfo[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const uploadRef = ref();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  fileName: undefined as string | undefined,
  fileCategory: undefined as string | undefined,
});

const form = reactive<FileUpdateParams>({
  id: 0,
  fileName: "",
  fileCategory: "",
  remark: "",
});

const rules: FormRules = {
  fileName: [
    {
      required: true,
      message: t("file.fileManage.validation.file_name_required"),
      trigger: "blur",
    },
  ],
};

// 上传配置
const uploadUrl = computed(() => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || "/iboot/api";
  return `${baseURL}/file/upload`;
});

const uploadHeaders = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : "",
}));

const uploadData = ref<Record<string, string>>({});

const beforeUpload = (file: UploadRawFile) => {
  const maxSize = 50 * 1024 * 1024; // 50MB
  if (file.size > maxSize) {
    ElMessage.error(t("file.fileManage.messages.file_size_limit"));
    return false;
  }
  return true;
};

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    ElMessage.success(t("file.fileManage.messages.upload_success"));
    loadData();
  } else {
    ElMessage.error(
      response.message || t("file.fileManage.messages.upload_failed"),
    );
  }
};

const handleUploadError = () => {
  ElMessage.error(t("file.fileManage.messages.upload_error"));
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getFileList(queryParams);
    if (res.code === 200) {
      fileList.value = res.data.data;
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
  queryParams.fileName = undefined;
  queryParams.fileCategory = undefined;
  handleSearch();
};

const handleEdit = (row: FileInfo) => {
  form.id = row.id!;
  form.fileName = row.fileName;
  form.fileCategory = row.fileCategory || "";
  form.remark = row.remark || "";
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  await updateFileInfo(form);
  ElMessage.success(t("file.fileManage.messages.update_success"));
  dialogVisible.value = false;
  loadData();
};

const handleDownload = (row: FileInfo) => {
  const url = getFileDownloadUrl(row.id!);
  const link = document.createElement("a");
  link.href = url;
  // 附加 token 用于认证
  const token = userStore.token;
  if (token) {
    link.href = url + "?token=" + encodeURIComponent(token);
  }
  link.target = "_blank";
  link.click();
};

const handleDelete = (row: FileInfo) => {
  ElMessageBox.confirm(
    t("file.fileManage.messages.delete_confirm"),
    t("file.fileManage.table.file_name"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteFile(row.id!);
      ElMessage.success(t("file.fileManage.messages.delete_success"));
      loadData();
    })
    .catch(() => {});
};

onMounted(() => {
  dictStore.loadDicts("sys_file_category");
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
