<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">{{
          t("system.config.add")
        }}</el-button>
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("system.config.export") }}</el-button
        >
        <el-button type="warning" @click="handleRefreshCache">{{
          t("system.config.refresh_cache")
        }}</el-button>
      </div>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('system.config.configName')">
          <el-input
            v-model="queryParams.configName"
            :placeholder="t('system.config.placeholder.name')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('system.config.configKey')">
          <el-input
            v-model="queryParams.configKey"
            :placeholder="t('system.config.placeholder.key')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('system.config.configType')">
          <el-select
            v-model="queryParams.configType"
            :placeholder="t('system.config.please_select')"
            clearable
          >
            <el-option
              v-for="item in dictStore.getDict('sys_config_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="Number(item.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("system.config.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("system.config.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="configList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column
          prop="configName"
          :label="t('system.config.configName')"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="configKey"
          :label="t('system.config.configKey')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="configValue"
          :label="t('system.config.configValue')"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="configType"
          :label="t('system.config.type')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass('sys_config_type', row.configType)
              "
            >
              {{ dictStore.getDictLabel("sys_config_type", row.configType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          :label="t('system.config.remark')"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="createTime"
          :label="t('system.config.createTime')"
          width="180"
        />
        <el-table-column
          :label="t('system.config.action')"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">{{
              t("system.config.edit")
            }}</el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
              :disabled="row.configType === 1"
              >{{ t("system.config.delete") }}</el-button
            >
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
        <el-form-item :label="t('system.config.configName')" prop="configName">
          <el-input
            v-model="form.configName"
            :placeholder="t('system.config.placeholder.name')"
          />
        </el-form-item>
        <el-form-item :label="t('system.config.configKey')" prop="configKey">
          <el-input
            v-model="form.configKey"
            :placeholder="t('system.config.placeholder.key')"
          />
        </el-form-item>
        <el-form-item
          :label="t('system.config.configValue')"
          prop="configValue"
        >
          <el-input
            v-model="form.configValue"
            type="textarea"
            :rows="3"
            :placeholder="t('system.config.placeholder.value')"
          />
        </el-form-item>
        <el-form-item :label="t('system.config.configType')">
          <el-radio-group v-model="form.configType">
            <el-radio
              v-for="item in dictStore.getDict('sys_config_type')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.config.remark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="t('system.config.placeholder.remark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{
          t("system.config.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("system.config.confirm")
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
  getConfigList,
  addConfig,
  updateConfig,
  deleteConfig,
  refreshConfigCache,
  exportConfigList,
  type Config,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportConfigList({
        configName: queryParams.configName,
        configKey: queryParams.configKey,
        configType: queryParams.configType,
      }),
    t("system.config.title"),
  );
};

const loading = ref(false);
const configList = ref<Config[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  configName: undefined as string | undefined,
  configKey: undefined as string | undefined,
  configType: undefined as number | undefined,
});

const defaultForm: Config = {
  configName: "",
  configKey: "",
  configValue: "",
  configType: 0,
  remark: "",
};

const form = reactive<Config>({ ...defaultForm });

const rules: FormRules = {
  configName: [
    {
      required: true,
      message: t("system.config.validation.name_required"),
      trigger: "blur",
    },
  ],
  configKey: [
    {
      required: true,
      message: t("system.config.validation.key_required"),
      trigger: "blur",
    },
  ],
  configValue: [
    {
      required: true,
      message: t("system.config.validation.value_required"),
      trigger: "blur",
    },
  ],
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getConfigList(queryParams);
    if (res.code === 200) {
      configList.value = res.data.data;
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
  queryParams.configName = undefined;
  queryParams.configKey = undefined;
  queryParams.configType = undefined;
  handleSearch();
};

const resetForm = () => {
  Object.assign(form, defaultForm);
  form.id = undefined;
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = t("system.config.add");
  dialogVisible.value = true;
};

const handleEdit = (row: Config) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("system.config.edit");
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  if (form.id) {
    await updateConfig(form);
    ElMessage.success(t("system.config.edit_success"));
  } else {
    await addConfig(form);
    ElMessage.success(t("system.config.add_success"));
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: Config) => {
  ElMessageBox.confirm(
    t("system.config.confirm_delete"),
    t("system.config.confirm_delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteConfig(row.id!);
      ElMessage.success(t("system.config.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handleRefreshCache = async () => {
  await refreshConfigCache();
  ElMessage.success(t("system.config.refresh_cache_success"));
};

onMounted(() => {
  dictStore.loadDicts("sys_config_type");
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
