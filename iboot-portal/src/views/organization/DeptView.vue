<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">{{
          t("organization.dept.add")
        }}</el-button>
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("organization.dept.export") }}</el-button
        >
      </div>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('organization.dept.deptName')">
          <el-input
            v-model="queryParams.deptName"
            :placeholder="t('organization.dept.placeholder.deptName')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('organization.dept.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('organization.dept.please_select')"
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
            t("organization.dept.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("organization.dept.reset")
          }}</el-button>
        </el-form-item>
      </el-form>
      <el-table
        :data="filteredDeptTree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column
          prop="deptName"
          :label="t('organization.dept.deptName')"
          min-width="200"
        />
        <el-table-column
          prop="deptCode"
          :label="t('organization.dept.deptCode')"
          width="150"
        />
        <el-table-column
          prop="orderNum"
          :label="t('organization.dept.orderNum')"
          width="80"
          align="center"
        />
        <el-table-column
          prop="leader"
          :label="t('organization.dept.leader')"
          width="120"
        />
        <el-table-column
          prop="phone"
          :label="t('organization.dept.phone')"
          width="120"
        />
        <el-table-column
          prop="status"
          :label="t('organization.dept.status')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass('sys_normal_disable', row.status)
              "
            >
              {{ dictStore.getDictLabel("sys_normal_disable", row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          :label="t('organization.dept.createTime')"
          width="180"
        />
        <el-table-column
          :label="t('organization.dept.action')"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">{{
              t("organization.dept.edit")
            }}</el-button>
            <el-button type="primary" link @click="handleAddChild(row)">{{
              t("organization.dept.add_child")
            }}</el-button>
            <el-button type="danger" link @click="handleDelete(row)">{{
              t("organization.dept.delete")
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('organization.dept.parent')">
          <el-tree-select
            v-model="form.parentId"
            :data="deptOptions"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            check-strictly
            clearable
            :placeholder="t('organization.dept.placeholder.parent')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('organization.dept.deptCode')" prop="deptCode">
          <el-input
            v-model="form.deptCode"
            :placeholder="t('organization.dept.placeholder.deptCode')"
          />
        </el-form-item>
        <el-form-item :label="t('organization.dept.deptName')" prop="deptName">
          <el-input
            v-model="form.deptName"
            :placeholder="t('organization.dept.placeholder.deptName')"
          />
        </el-form-item>
        <el-form-item :label="t('organization.dept.orderNum')" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item :label="t('organization.dept.leader')">
          <el-input
            v-model="form.leader"
            :placeholder="t('organization.dept.placeholder.leader')"
          />
        </el-form-item>
        <el-form-item :label="t('organization.dept.phone')">
          <el-input
            v-model="form.phone"
            :placeholder="t('organization.dept.placeholder.phone')"
          />
        </el-form-item>
        <el-form-item :label="t('organization.dept.email')">
          <el-input
            v-model="form.email"
            :placeholder="t('organization.dept.placeholder.email')"
          />
        </el-form-item>
        <el-form-item :label="t('organization.dept.status')">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{
          t("organization.dept.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("organization.dept.confirm")
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
  getDeptTree,
  addDept,
  updateDept,
  deleteDept,
  exportDeptList,
  type Dept,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportDeptList({
        deptName: queryParams.deptName || undefined,
        status: queryParams.status,
      }),
    t("organization.dept.title"),
  );
};

const loading = ref(false);
const deptTree = ref<Dept[]>([]);
const deptOptions = ref<Dept[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

const queryParams = reactive({
  deptName: "" as string,
  status: undefined as number | undefined,
});

const defaultForm: Dept = {
  parentId: undefined,
  deptCode: "",
  deptName: "",
  orderNum: 0,
  leader: "",
  phone: "",
  email: "",
  status: 1,
};

const form = reactive<Dept>({ ...defaultForm });

const rules: FormRules = {
  deptCode: [
    {
      required: true,
      message: t("organization.dept.validation.deptCode_required"),
      trigger: "blur",
    },
  ],
  deptName: [
    {
      required: true,
      message: t("organization.dept.validation.deptName_required"),
      trigger: "blur",
    },
  ],
};

const filterTree = (
  nodes: Dept[],
  name: string,
  status: number | undefined,
): Dept[] => {
  return nodes.reduce((acc: Dept[], node) => {
    const children = node.children
      ? filterTree(node.children, name, status)
      : [];
    const nameMatch = !name || node.deptName.includes(name);
    const statusMatch = status === undefined || node.status === status;
    if ((nameMatch && statusMatch) || children.length > 0) {
      acc.push({
        ...node,
        children:
          children.length > 0
            ? children
            : node.children && nameMatch && statusMatch
              ? []
              : children,
      });
    }
    return acc;
  }, []);
};

const filteredDeptTree = ref<Dept[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getDeptTree();
    if (res.code === 200) {
      deptTree.value = res.data;
      filteredDeptTree.value = res.data;
      deptOptions.value = [
        {
          id: 0,
          deptName: t("organization.dept.top_level"),
          children: res.data,
        },
      ] as Dept[];
    }
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  Object.assign(form, defaultForm);
  form.id = undefined;
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = t("organization.dept.add");
  dialogVisible.value = true;
};

const handleAddChild = (row: Dept) => {
  resetForm();
  form.parentId = row.id;
  dialogTitle.value = t("organization.dept.add");
  dialogVisible.value = true;
};

const handleEdit = (row: Dept) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("organization.dept.edit");
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  if (form.id) {
    await updateDept(form);
    ElMessage.success(t("organization.dept.edit_success"));
  } else {
    await addDept(form);
    ElMessage.success(t("organization.dept.add_success"));
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: Dept) => {
  ElMessageBox.confirm(
    t("organization.dept.confirm_delete"),
    t("organization.dept.confirm_delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteDept(row.id!);
      ElMessage.success(t("organization.dept.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handleSearch = () => {
  if (!queryParams.deptName && queryParams.status === undefined) {
    filteredDeptTree.value = deptTree.value;
  } else {
    filteredDeptTree.value = filterTree(
      deptTree.value,
      queryParams.deptName,
      queryParams.status,
    );
  }
};

const handleReset = () => {
  queryParams.deptName = "";
  queryParams.status = undefined;
  handleSearch();
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
