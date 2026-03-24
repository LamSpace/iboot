<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAddType">{{
          t("system.dict.add_type")
        }}</el-button>
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExportType"
          >{{ t("system.dict.export_type") }}</el-button
        >
        <el-button
          type="success"
          :loading="exportDataLoading"
          @click="handleExportData"
          :disabled="!currentType"
          >{{ t("system.dict.export_data") }}</el-button
        >
      </div>
      <el-row :gutter="20">
        <!-- 左侧：字典类型列表 -->
        <el-col :span="12">
          <el-card>
            <template #header>{{ t("system.dict.dict_type") }}</template>
            <el-table
              :data="typeList"
              style="width: 100%"
              v-loading="typeLoading"
              @row-click="handleTypeClick"
              highlight-current-row
            >
              <el-table-column prop="dictName" :label="t('system.dict.name')" />
              <el-table-column prop="dictType" :label="t('system.dict.type')" />
              <el-table-column
                prop="status"
                :label="t('system.dict.status')"
                width="80"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="
                      dictStore.getDictListClass(
                        'sys_normal_disable',
                        row.status,
                      )
                    "
                    size="small"
                  >
                    {{
                      dictStore.getDictLabel("sys_normal_disable", row.status)
                    }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('system.dict.action')" width="300">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click.stop="handleEditType(row)"
                    >{{ t("system.dict.edit") }}</el-button
                  >
                  <el-button
                    type="danger"
                    link
                    size="small"
                    @click.stop="handleDeleteType(row)"
                    >{{ t("system.dict.delete") }}</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              class="pagination"
              small
              v-model:current-page="typePageNum"
              v-model:page-size="typePageSize"
              :total="typeTotal"
              layout="total, prev, pager, next"
              @current-change="loadTypeData"
            />
          </el-card>
        </el-col>

        <!-- 右侧：字典数据列表 -->
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="data-header">
                <span
                  >{{ t("system.dict.dict_data") }}
                  {{ currentType ? `- ${currentType.dictName}` : "" }}</span
                >
                <el-button
                  type="primary"
                  size="small"
                  @click="handleAddData"
                  :disabled="!currentType"
                  >{{ t("system.dict.add_item") }}</el-button
                >
              </div>
            </template>
            <el-table
              :data="dataList"
              style="width: 100%"
              v-loading="dataLoading"
            >
              <el-table-column
                prop="dictLabel"
                :label="t('system.dict.label')"
              />
              <el-table-column
                prop="dictValue"
                :label="t('system.dict.value')"
              />
              <el-table-column
                prop="orderNum"
                :label="t('system.dict.sort')"
                width="80"
                align="center"
              />
              <el-table-column
                prop="status"
                :label="t('system.dict.status')"
                width="80"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="
                      dictStore.getDictListClass(
                        'sys_normal_disable',
                        row.status,
                      )
                    "
                    size="small"
                  >
                    {{
                      dictStore.getDictLabel("sys_normal_disable", row.status)
                    }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('system.dict.action')" width="300">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click="handleEditData(row)"
                    >{{ t("system.dict.edit") }}</el-button
                  >
                  <el-button
                    type="danger"
                    link
                    size="small"
                    @click="handleDeleteData(row)"
                    >{{ t("system.dict.delete") }}</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 字典类型对话框 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogTitle"
      width="500px"
    >
      <el-form
        ref="typeFormRef"
        :model="typeForm"
        :rules="typeRules"
        label-width="100px"
      >
        <el-form-item :label="t('system.dict.name')" prop="dictName">
          <el-input
            v-model="typeForm.dictName"
            :placeholder="t('system.dict.placeholder.name')"
          />
        </el-form-item>
        <el-form-item :label="t('system.dict.type')" prop="dictType">
          <el-input
            v-model="typeForm.dictType"
            :placeholder="t('system.dict.placeholder.type')"
          />
        </el-form-item>
        <el-form-item :label="t('system.dict.status')">
          <el-radio-group v-model="typeForm.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.dict.remark')">
          <el-input
            v-model="typeForm.remark"
            type="textarea"
            :placeholder="t('system.dict.placeholder.remark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">{{
          t("system.dict.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmitType">{{
          t("system.dict.confirm")
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据对话框 -->
    <el-dialog
      v-model="dataDialogVisible"
      :title="dataDialogTitle"
      width="500px"
    >
      <el-form
        ref="dataFormRef"
        :model="dataForm"
        :rules="dataRules"
        label-width="100px"
      >
        <el-form-item :label="t('system.dict.label')" prop="dictLabel">
          <el-input
            v-model="dataForm.dictLabel"
            :placeholder="t('system.dict.placeholder.label')"
          />
        </el-form-item>
        <el-form-item :label="t('system.dict.value')" prop="dictValue">
          <el-input
            v-model="dataForm.dictValue"
            :placeholder="t('system.dict.placeholder.value')"
          />
        </el-form-item>
        <el-form-item :label="t('system.dict.sort')">
          <el-input-number v-model="dataForm.orderNum" :min="0" />
        </el-form-item>
        <el-form-item :label="t('system.dict.css_class')">
          <el-input
            v-model="dataForm.cssClass"
            :placeholder="t('system.dict.placeholder.css_class')"
          />
        </el-form-item>
        <el-form-item :label="t('system.dict.list_class')">
          <el-select v-model="dataForm.listClass" placeholder="请选择">
            <el-option :label="t('system.dict.default')" value="" />
            <el-option label="Primary" value="primary" />
            <el-option label="Success" value="success" />
            <el-option label="Info" value="info" />
            <el-option label="Warning" value="warning" />
            <el-option label="Danger" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('system.dict.is_default')">
          <el-radio-group v-model="dataForm.isDefault">
            <el-radio :value="1">{{ t("common.yes") }}</el-radio>
            <el-radio :value="0">{{ t("common.no") }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.dict.status')">
          <el-radio-group v-model="dataForm.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.dict.remark')">
          <el-input
            v-model="dataForm.remark"
            type="textarea"
            :placeholder="t('system.dict.placeholder.remark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">{{
          t("system.dict.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmitData">{{
          t("system.dict.confirm")
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
  getDictTypeList,
  addDictType,
  updateDictType,
  deleteDictType,
  getDictDataByType,
  addDictData,
  updateDictData,
  deleteDictData,
  exportDictTypeList,
  exportDictDataList,
  type DictType,
  type DictData,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();
const { exportLoading: exportDataLoading, handleExport: performExportData } =
  useExport();

const handleExportType = () => {
  performExport(() => exportDictTypeList({}), t("system.dict.dict_type"));
};

const handleExportData = () => {
  if (!currentType.value) return;
  performExportData(
    () => exportDictDataList({ dictType: currentType.value!.dictType }),
    `${currentType.value.dictType}`,
  );
};

// 字典类型相关
const typeLoading = ref(false);
const typeList = ref<DictType[]>([]);
const typeTotal = ref(0);
const typePageNum = ref(1);
const typePageSize = ref(10);
const typeDialogVisible = ref(false);
const typeDialogTitle = ref("");
const typeFormRef = ref<FormInstance>();
const currentType = ref<DictType | null>(null);

const defaultTypeForm: DictType = {
  dictName: "",
  dictType: "",
  status: 1,
  remark: "",
};
const typeForm = reactive<DictType>({ ...defaultTypeForm });
const typeRules: FormRules = {
  dictName: [
    {
      required: true,
      message: t("system.dict.validation.name_required"),
      trigger: "blur",
    },
  ],
  dictType: [
    {
      required: true,
      message: t("system.dict.validation.type_required"),
      trigger: "blur",
    },
  ],
};

// 字典数据相关
const dataLoading = ref(false);
const dataList = ref<DictData[]>([]);
const dataDialogVisible = ref(false);
const dataDialogTitle = ref("");
const dataFormRef = ref<FormInstance>();

const defaultDataForm: DictData = {
  dictType: "",
  dictLabel: "",
  dictValue: "",
  orderNum: 0,
  cssClass: "",
  listClass: "",
  isDefault: 0,
  status: 1,
  remark: "",
};
const dataForm = reactive<DictData>({ ...defaultDataForm });
const dataRules: FormRules = {
  dictLabel: [
    {
      required: true,
      message: t("system.dict.validation.label_required"),
      trigger: "blur",
    },
  ],
  dictValue: [
    {
      required: true,
      message: t("system.dict.validation.value_required"),
      trigger: "blur",
    },
  ],
};

const loadTypeData = async () => {
  typeLoading.value = true;
  try {
    const res = await getDictTypeList(typePageNum.value, typePageSize.value);
    if (res.code === 200) {
      typeList.value = res.data.data;
      typeTotal.value = res.data.total;
    }
  } finally {
    typeLoading.value = false;
  }
};

const loadDataList = async () => {
  if (!currentType.value) return;
  dataLoading.value = true;
  try {
    const res = await getDictDataByType(currentType.value.dictType);
    if (res.code === 200) {
      dataList.value = res.data;
    }
  } finally {
    dataLoading.value = false;
  }
};

const handleTypeClick = (row: DictType) => {
  currentType.value = row;
  loadDataList();
};

const handleAddType = () => {
  Object.assign(typeForm, defaultTypeForm);
  typeForm.id = undefined;
  typeDialogTitle.value = t("system.dict.add_type");
  typeDialogVisible.value = true;
};

const handleEditType = (row: DictType) => {
  Object.assign(typeForm, row);
  typeDialogTitle.value = t("system.dict.edit_type");
  typeDialogVisible.value = true;
};

const handleSubmitType = async () => {
  await typeFormRef.value?.validate();
  if (typeForm.id) {
    await updateDictType(typeForm);
    ElMessage.success(t("system.dict.edit_success"));
  } else {
    await addDictType(typeForm);
    ElMessage.success(t("system.dict.add_success"));
  }
  typeDialogVisible.value = false;
  loadTypeData();
};

const handleDeleteType = (row: DictType) => {
  ElMessageBox.confirm(
    t("system.dict.confirm_delete_type"),
    t("system.dict.confirm_delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteDictType(row.id!);
      ElMessage.success(t("system.dict.delete_success"));
      if (currentType.value?.id === row.id) {
        currentType.value = null;
        dataList.value = [];
      }
      loadTypeData();
    })
    .catch(() => {});
};

const handleAddData = () => {
  Object.assign(dataForm, defaultDataForm);
  dataForm.id = undefined;
  dataForm.dictType = currentType.value!.dictType;
  dataDialogTitle.value = t("system.dict.add_item");
  dataDialogVisible.value = true;
};

const handleEditData = (row: DictData) => {
  Object.assign(dataForm, row);
  dataDialogTitle.value = t("system.dict.edit_item");
  dataDialogVisible.value = true;
};

const handleSubmitData = async () => {
  await dataFormRef.value?.validate();
  if (dataForm.id) {
    await updateDictData(dataForm);
    ElMessage.success(t("system.dict.edit_success"));
  } else {
    await addDictData(dataForm);
    ElMessage.success(t("system.dict.add_success"));
  }
  dataDialogVisible.value = false;
  loadDataList();
};

const handleDeleteData = (row: DictData) => {
  ElMessageBox.confirm(
    t("system.dict.confirm_delete_item"),
    t("system.dict.confirm_delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteDictData(row.id!);
      ElMessage.success(t("system.dict.delete_success"));
      loadDataList();
    })
    .catch(() => {});
};

onMounted(() => {
  dictStore.loadDicts("sys_normal_disable");
  loadTypeData();
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

.data-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
