<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">{{
          t("organization.post.add")
        }}</el-button>
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("organization.post.export") }}</el-button
        >
      </div>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('organization.post.postName')">
          <el-input
            v-model="queryParams.postName"
            :placeholder="t('organization.post.placeholder.postName')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('organization.post.postCode')">
          <el-input
            v-model="queryParams.postCode"
            :placeholder="t('organization.post.placeholder.postCode')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('organization.post.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('organization.post.please_select')"
            clearable
          >
            <el-option
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="Number(item.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("organization.post.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("organization.post.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="postList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column
          prop="postCode"
          :label="t('organization.post.postCode')"
          width="150"
        />
        <el-table-column
          prop="postName"
          :label="t('organization.post.postName')"
          width="150"
        />
        <el-table-column
          prop="orderNum"
          :label="t('organization.post.orderNum')"
          width="80"
          align="center"
        />
        <el-table-column
          prop="status"
          :label="t('organization.post.status')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          :label="t('organization.post.remark')"
          show-overflow-tooltip
        />
        <el-table-column
          prop="createTime"
          :label="t('organization.post.createTime')"
          width="180"
        />
        <el-table-column
          :label="t('organization.post.action')"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">{{
              t("organization.post.edit_action")
            }}</el-button>
            <el-button type="danger" link @click="handleDelete(row)">{{
              t("organization.post.delete")
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>

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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('organization.post.postCode')" prop="postCode">
          <el-input
            v-model="form.postCode"
            :placeholder="t('organization.post.placeholder.postCode')"
          />
        </el-form-item>
        <el-form-item :label="t('organization.post.postName')" prop="postName">
          <el-input
            v-model="form.postName"
            :placeholder="t('organization.post.placeholder.postName')"
          />
        </el-form-item>
        <el-form-item :label="t('organization.post.orderNum')">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item :label="t('organization.post.status')">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('organization.post.remark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="t('organization.post.placeholder.remark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{
          t("organization.post.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("organization.post.confirm")
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
  getPostList,
  addPost,
  updatePost,
  deletePost,
  changePostStatus,
  exportPostList,
  type Post,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportPostList({
        postName: queryParams.postName,
        postCode: queryParams.postCode,
        status: queryParams.status,
      }),
    t("organization.post.title"),
  );
};

const loading = ref(false);
const postList = ref<Post[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  postName: undefined as string | undefined,
  postCode: undefined as string | undefined,
  status: undefined as number | undefined,
});

const defaultForm: Post = {
  postCode: "",
  postName: "",
  orderNum: 0,
  status: 1,
  remark: "",
};

const form = reactive<Post>({ ...defaultForm });

const rules: FormRules = {
  postCode: [
    {
      required: true,
      message: t("organization.post.validation.postCode_required"),
      trigger: "blur",
    },
  ],
  postName: [
    {
      required: true,
      message: t("organization.post.validation.postName_required"),
      trigger: "blur",
    },
  ],
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getPostList(queryParams);
    if (res.code === 200) {
      postList.value = res.data.data;
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
  queryParams.postName = undefined;
  queryParams.postCode = undefined;
  queryParams.status = undefined;
  handleSearch();
};

const resetForm = () => {
  Object.assign(form, defaultForm);
  form.id = undefined;
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = t("organization.post.add");
  dialogVisible.value = true;
};

const handleEdit = (row: Post) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("organization.post.edit");
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  if (form.id) {
    await updatePost(form);
    ElMessage.success(t("organization.post.edit_success"));
  } else {
    await addPost(form);
    ElMessage.success(t("organization.post.add_success"));
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: Post) => {
  ElMessageBox.confirm(
    t("organization.post.confirm_delete"),
    t("organization.post.confirm_delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deletePost(row.id!);
      ElMessage.success(t("organization.post.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handleStatusChange = async (row: Post) => {
  await changePostStatus(row.id!, row.status!);
  ElMessage.success(t("organization.post.status_change_success"));
};

onMounted(() => {
  dictStore.loadDicts("sys_normal_disable");
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
