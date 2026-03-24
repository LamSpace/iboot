<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">{{
          t("system.role.add")
        }}</el-button>
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("system.role.export") }}</el-button
        >
      </div>
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('system.role.roleName')">
          <el-input
            v-model="queryParams.roleName"
            :placeholder="t('system.role.placeholder.roleName')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('system.role.roleCode')">
          <el-input
            v-model="queryParams.roleCode"
            :placeholder="t('system.role.placeholder.roleCode')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('system.role.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('system.role.please_select')"
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
            t("system.role.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("system.role.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="roleList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column
          prop="roleCode"
          :label="t('system.role.roleCode')"
          width="150"
        />
        <el-table-column
          prop="roleName"
          :label="t('system.role.roleName')"
          width="150"
        />
        <el-table-column
          prop="orderNum"
          :label="t('system.role.orderNum')"
          width="80"
        />
        <el-table-column
          prop="dataScope"
          :label="t('system.role.dataScope')"
          width="130"
        >
          <template #default="{ row }">
            <el-tag>{{ dataScopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          :label="t('system.role.status')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="row.roleCode === 'ROLE_ADMIN'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          :label="t('system.role.remark')"
          show-overflow-tooltip
        />
        <el-table-column
          prop="createTime"
          :label="t('system.role.createTime')"
          width="180"
        />
        <el-table-column
          :label="t('system.role.action')"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">{{
              t("system.role.edit")
            }}</el-button>
            <el-button type="primary" link @click="handleMenuPerms(row)">{{
              t("system.role.menu_permission")
            }}</el-button>
            <el-button type="primary" link @click="handleDataScope(row)">{{
              t("system.role.data_permission")
            }}</el-button>
            <el-button
              type="danger"
              link
              @click="handleDelete(row)"
              :disabled="row.roleCode === 'ROLE_ADMIN'"
              >{{ t("system.role.delete") }}</el-button
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
        <el-form-item :label="t('system.role.roleCode')" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            :placeholder="t('system.role.placeholder.roleCode')"
          />
        </el-form-item>
        <el-form-item :label="t('system.role.roleName')" prop="roleName">
          <el-input
            v-model="form.roleName"
            :placeholder="t('system.role.placeholder.roleName')"
          />
        </el-form-item>
        <el-form-item :label="t('system.role.orderNum')">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item :label="t('system.role.status')">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.role.remark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="t('system.role.placeholder.remark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{
          t("system.role.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("system.role.confirm")
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 菜单权限对话框 -->
    <el-dialog
      v-model="menuDialogVisible"
      :title="t('system.role.assign_menu_permission')"
      width="500px"
    >
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'menuName', children: 'children' }"
        show-checkbox
        node-key="id"
        :default-checked-keys="checkedMenuIds"
        check-strictly
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">{{
          t("system.role.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmitMenuPerms">{{
          t("system.role.confirm")
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 数据权限对话框 -->
    <el-dialog
      v-model="dataScopeDialogVisible"
      :title="t('system.role.assign_data_permission')"
      width="500px"
    >
      <el-form label-width="100px">
        <el-form-item :label="t('system.role.roleName')">
          <el-input :value="currentRole?.roleName" disabled />
        </el-form-item>
        <el-form-item :label="t('system.role.dataScope')">
          <el-select v-model="dataScopeForm.dataScope" style="width: 100%">
            <el-option
              :value="1"
              :label="t('system.role.data_scope_types.all')"
            />
            <el-option
              :value="2"
              :label="t('system.role.data_scope_types.custom')"
            />
            <el-option
              :value="3"
              :label="t('system.role.data_scope_types.dept')"
            />
            <el-option
              :value="4"
              :label="t('system.role.data_scope_types.dept_below')"
            />
            <el-option
              :value="5"
              :label="t('system.role.data_scope_types.self')"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="t('system.role.data_permission')"
          v-if="dataScopeForm.dataScope === 2"
        >
          <el-tree
            ref="deptTreeRef"
            :data="deptTree"
            :props="{ label: 'deptName', children: 'children' }"
            show-checkbox
            node-key="id"
            :default-checked-keys="checkedDeptIds"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataScopeDialogVisible = false">{{
          t("system.role.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmitDataScope">{{
          t("system.role.confirm")
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
import type { ElTree } from "element-plus";
import {
  getRoleList,
  addRole,
  updateRole,
  deleteRole,
  changeRoleStatus,
  getRoleById,
  setRoleDataScope,
  getMenuTree,
  getDeptTree,
  exportRoleList,
  type Role,
  type Menu,
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
      exportRoleList({
        roleName: queryParams.roleName,
        roleCode: queryParams.roleCode,
        status: queryParams.status,
      }),
    t("system.role.title"),
  );
};

const loading = ref(false);
const roleList = ref<Role[]>([]);
const total = ref(0);
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: undefined as string | undefined,
  roleCode: undefined as string | undefined,
  status: undefined as number | undefined,
});
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

// 菜单权限
const menuDialogVisible = ref(false);
const menuTreeRef = ref<InstanceType<typeof ElTree>>();
const menuTree = ref<Menu[]>([]);
const checkedMenuIds = ref<number[]>([]);
const currentRole = ref<Role | null>(null);

// 数据权限
const dataScopeDialogVisible = ref(false);
const deptTreeRef = ref<InstanceType<typeof ElTree>>();
const deptTree = ref<Dept[]>([]);
const checkedDeptIds = ref<number[]>([]);
const dataScopeForm = reactive({ dataScope: 1, deptIds: [] as number[] });

const defaultForm: Role = {
  roleCode: "",
  roleName: "",
  orderNum: 0,
  dataScope: 1,
  status: 1,
  remark: "",
};

const form = reactive<Role>({ ...defaultForm });

const rules: FormRules = {
  roleCode: [
    {
      required: true,
      message: t("system.role.validation.roleCode_required"),
      trigger: "blur",
    },
  ],
  roleName: [
    {
      required: true,
      message: t("system.role.validation.roleName_required"),
      trigger: "blur",
    },
  ],
};

const dataScopeMap: Record<number, string> = {
  1: t("system.role.data_scope_types.all"),
  2: t("system.role.data_scope_types.custom"),
  3: t("system.role.data_scope_types.dept"),
  4: t("system.role.data_scope_types.dept_below"),
  5: t("system.role.data_scope_types.self"),
};
const dataScopeLabel = (scope?: number) =>
  dataScopeMap[scope ?? 1] || t("system.role.data_scope_types.all");

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getRoleList(queryParams);
    if (res.code === 200) {
      roleList.value = res.data.data;
      total.value = res.data.total;
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
  dialogTitle.value = t("system.role.add");
  dialogVisible.value = true;
};

const handleEdit = (row: Role) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("system.role.edit");
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  if (form.id) {
    await updateRole(form);
    ElMessage.success(t("system.role.edit_success"));
  } else {
    await addRole(form);
    ElMessage.success(t("system.role.add_success"));
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: Role) => {
  ElMessageBox.confirm(
    t("system.role.confirm_delete"),
    t("system.role.confirm_delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteRole(row.id!);
      ElMessage.success(t("system.role.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handleStatusChange = async (row: Role) => {
  await changeRoleStatus(row.id!, row.status!);
  ElMessage.success(t("system.role.status_change_success"));
};

const handleSearch = () => {
  queryParams.pageNum = 1;
  loadData();
};

const handleReset = () => {
  queryParams.roleName = undefined;
  queryParams.roleCode = undefined;
  queryParams.status = undefined;
  handleSearch();
};

// 菜单权限
const handleMenuPerms = async (row: Role) => {
  currentRole.value = row;
  // 加载菜单树
  const menuRes = await getMenuTree();
  if (menuRes.code === 200) {
    menuTree.value = menuRes.data;
  }
  // 获取角色已分配的菜单ID
  const roleRes = await getRoleById(row.id!);
  if (roleRes.code === 200) {
    checkedMenuIds.value = roleRes.data.menuIds || [];
  }
  menuDialogVisible.value = true;
};

const handleSubmitMenuPerms = async () => {
  const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) as number[];
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() as number[];
  const menuIds = [...checkedKeys, ...halfCheckedKeys];
  await updateRole({ ...currentRole.value!, menuIds });
  ElMessage.success(t("system.role.menu_permission_success"));
  menuDialogVisible.value = false;
  loadData();
};

// 数据权限
const handleDataScope = async (row: Role) => {
  currentRole.value = row;
  dataScopeForm.dataScope = row.dataScope || 1;
  // 加载部门树
  const deptRes = await getDeptTree();
  if (deptRes.code === 200) {
    deptTree.value = deptRes.data;
  }
  // 获取角色已分配的部门ID
  const roleRes = await getRoleById(row.id!);
  if (roleRes.code === 200) {
    checkedDeptIds.value = roleRes.data.deptIds || [];
  }
  dataScopeDialogVisible.value = true;
};

const handleSubmitDataScope = async () => {
  const deptIds =
    dataScopeForm.dataScope === 2
      ? (deptTreeRef.value?.getCheckedKeys(false) as number[])
      : [];
  await setRoleDataScope({
    id: currentRole.value!.id,
    roleCode: currentRole.value!.roleCode,
    roleName: currentRole.value!.roleName,
    dataScope: dataScopeForm.dataScope,
    deptIds,
  });
  ElMessage.success(t("system.role.data_permission_success"));
  dataScopeDialogVisible.value = false;
  loadData();
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
