<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">{{
          t("system.menu.add")
        }}</el-button>
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("system.menu.export") }}</el-button
        >
      </div>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('system.menu.name')">
          <el-input
            v-model="queryParams.menuName"
            :placeholder="t('system.menu.placeholder.name')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('system.menu.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('system.menu.please_select')"
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
            t("system.menu.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("system.menu.reset")
          }}</el-button>
        </el-form-item>
      </el-form>
      <el-table
        :data="filteredMenuTree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column
          prop="menuName"
          :label="t('system.menu.name')"
          min-width="180"
        />
        <el-table-column prop="icon" :label="t('system.menu.icon')" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column
          prop="orderNum"
          :label="t('system.menu.sort')"
          width="80"
          align="center"
        />
        <el-table-column
          prop="perms"
          :label="t('system.menu.permission')"
          width="180"
        />
        <el-table-column
          prop="component"
          :label="t('system.menu.component')"
          width="180"
        />
        <el-table-column
          prop="menuType"
          :label="t('system.menu.type')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getMenuTypeTag(row.menuType)">
              {{ getMenuTypeLabel(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          :label="t('system.menu.status')"
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
          :label="t('system.menu.action')"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">{{
              t("system.menu.edit")
            }}</el-button>
            <el-button
              type="primary"
              link
              @click="handleAddChild(row)"
              v-if="row.menuType !== 'F'"
              >{{ t("system.menu.add_child") }}</el-button
            >
            <el-button type="danger" link @click="handleDelete(row)">{{
              t("system.menu.delete")
            }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('system.menu.parent')">
          <el-tree-select
            v-model="form.parentId"
            :data="menuOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            check-strictly
            clearable
            :placeholder="t('system.menu.placeholder.parent')"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('system.menu.type')" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">{{ t("system.menu.directory") }}</el-radio>
            <el-radio value="C">{{ t("system.menu.menu") }}</el-radio>
            <el-radio value="F">{{ t("system.menu.button") }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.menu.name')" prop="menuName">
          <el-input
            v-model="form.menuName"
            :placeholder="t('system.menu.placeholder.name')"
          />
        </el-form-item>
        <el-form-item :label="t('system.menu.sort')">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item
          :label="t('system.menu.path')"
          v-if="form.menuType !== 'F'"
        >
          <el-input
            v-model="form.path"
            :placeholder="t('system.menu.placeholder.path')"
          />
        </el-form-item>
        <el-form-item
          :label="t('system.menu.component')"
          v-if="form.menuType === 'C'"
        >
          <el-input
            v-model="form.component"
            :placeholder="t('system.menu.placeholder.component')"
          />
        </el-form-item>
        <el-form-item
          :label="t('system.menu.permission')"
          v-if="form.menuType !== 'M'"
        >
          <el-input
            v-model="form.perms"
            :placeholder="t('system.menu.placeholder.permission')"
          />
        </el-form-item>
        <el-form-item
          :label="t('system.menu.icon')"
          v-if="form.menuType !== 'F'"
        >
          <el-input
            v-model="form.icon"
            :placeholder="t('system.menu.placeholder.icon')"
          />
        </el-form-item>
        <el-form-item
          :label="t('system.menu.is_frame')"
          v-if="form.menuType !== 'F'"
        >
          <el-radio-group v-model="form.isFrame">
            <el-radio :value="0">{{ t("common.no") }}</el-radio>
            <el-radio :value="1">{{ t("common.yes") }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="t('system.menu.is_cache')"
          v-if="form.menuType === 'C'"
        >
          <el-radio-group v-model="form.isCache">
            <el-radio :value="1">{{ t("common.yes") }}</el-radio>
            <el-radio :value="0">{{ t("common.no") }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="t('system.menu.visible')"
          v-if="form.menuType !== 'F'"
        >
          <el-radio-group v-model="form.visible">
            <el-radio
              v-for="item in dictStore.getDict('sys_show_hide')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('system.menu.status')">
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
          t("system.menu.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("system.menu.confirm")
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
  getMenuTree,
  addMenu,
  updateMenu,
  deleteMenu,
  exportMenuList,
  type Menu,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportMenuList({
        menuName: queryParams.menuName || undefined,
        status: queryParams.status,
      }),
    t("system.menu.title"),
  );
};

const loading = ref(false);
const menuTree = ref<Menu[]>([]);
const menuOptions = ref<Menu[]>([]);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

const queryParams = reactive({
  menuName: "" as string,
  status: undefined as number | undefined,
});

const defaultForm: Menu = {
  parentId: undefined,
  menuName: "",
  menuType: "M",
  path: "",
  component: "",
  perms: "",
  icon: "",
  orderNum: 0,
  isFrame: 0,
  isCache: 1,
  visible: 1,
  status: 1,
};

const form = reactive<Menu>({ ...defaultForm });

const rules: FormRules = {
  menuName: [
    {
      required: true,
      message: t("system.menu.validation.name_required"),
      trigger: "blur",
    },
  ],
  menuType: [{ required: true, message: "请选择菜单类型", trigger: "change" }],
};

const getMenuTypeLabel = (type: string) => {
  const map: Record<string, string> = { M: "目录", C: "菜单", F: "按钮" };
  return map[type] || type;
};

const getMenuTypeTag = (type: string) => {
  const map: Record<string, string> = { M: "", C: "success", F: "warning" };
  return map[type] || "";
};

const filterTree = (
  nodes: Menu[],
  name: string,
  status: number | undefined,
): Menu[] => {
  return nodes.reduce((acc: Menu[], node) => {
    const children = node.children
      ? filterTree(node.children, name, status)
      : [];
    const nameMatch = !name || node.menuName.includes(name);
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

const filteredMenuTree = ref<Menu[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getMenuTree();
    if (res.code === 200) {
      menuTree.value = res.data;
      filteredMenuTree.value = res.data;
      menuOptions.value = [
        { id: 0, menuName: "主目录", children: res.data },
      ] as Menu[];
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
  dialogTitle.value = t("system.menu.add");
  dialogVisible.value = true;
};

const handleAddChild = (row: Menu) => {
  resetForm();
  form.parentId = row.id;
  dialogTitle.value = t("system.menu.add");
  dialogVisible.value = true;
};

const handleEdit = (row: Menu) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("system.menu.edit");
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  if (form.id) {
    await updateMenu(form);
    ElMessage.success(t("system.menu.edit_success"));
  } else {
    await addMenu(form);
    ElMessage.success(t("system.menu.add_success"));
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: Menu) => {
  ElMessageBox.confirm(
    t("system.menu.confirm_delete"),
    t("system.menu.confirm_delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteMenu(row.id!);
      ElMessage.success(t("system.menu.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handleSearch = () => {
  if (!queryParams.menuName && queryParams.status === undefined) {
    filteredMenuTree.value = menuTree.value;
  } else {
    filteredMenuTree.value = filterTree(
      menuTree.value,
      queryParams.menuName,
      queryParams.status,
    );
  }
};

const handleReset = () => {
  queryParams.menuName = "";
  queryParams.status = undefined;
  handleSearch();
};

onMounted(() => {
  dictStore.loadDicts("sys_normal_disable", "sys_show_hide");
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
