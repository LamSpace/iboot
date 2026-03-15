<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">新增角色</el-button>
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
      </div>
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="queryParams.roleCode" placeholder="请输入角色编码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="Number(item.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="roleList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="roleCode" label="角色编码" width="150" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="orderNum" label="排序" width="80" />
        <el-table-column prop="dataScope" label="数据权限" width="130">
          <template #default="{ row }">
            <el-tag>{{ dataScopeLabel(row.dataScope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
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
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleMenuPerms(row)">菜单权限</el-button>
            <el-button type="primary" link @click="handleDataScope(row)">数据权限</el-button>
            <el-button type="danger" link @click="handleDelete(row)" :disabled="row.roleCode === 'ROLE_ADMIN'">删除</el-button>
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
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
            >{{ item.dictLabel }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 菜单权限对话框 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="500px">
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
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitMenuPerms">确定</el-button>
      </template>
    </el-dialog>

    <!-- 数据权限对话框 -->
    <el-dialog v-model="dataScopeDialogVisible" title="设置数据权限" width="500px">
      <el-form label-width="100px">
        <el-form-item label="角色名称">
          <el-input :value="currentRole?.roleName" disabled />
        </el-form-item>
        <el-form-item label="数据权限">
          <el-select v-model="dataScopeForm.dataScope" style="width: 100%">
            <el-option :value="1" label="全部数据权限" />
            <el-option :value="2" label="自定义数据权限" />
            <el-option :value="3" label="本部门数据权限" />
            <el-option :value="4" label="本部门及以下数据权限" />
            <el-option :value="5" label="仅本人数据权限" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围" v-if="dataScopeForm.dataScope === 2">
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
        <el-button @click="dataScopeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitDataScope">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { ElTree } from 'element-plus'
import {
  getRoleList, addRole, updateRole, deleteRole, changeRoleStatus,
  getRoleById, setRoleDataScope,
  getMenuTree, getDeptTree, exportRoleList,
  type Role, type Menu, type Dept
} from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportRoleList({
      roleName: queryParams.roleName,
      roleCode: queryParams.roleCode,
      status: queryParams.status
    }),
    '角色列表'
  )
}

const loading = ref(false)
const roleList = ref<Role[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: undefined as string | undefined,
  roleCode: undefined as string | undefined,
  status: undefined as number | undefined
})
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

// 菜单权限
const menuDialogVisible = ref(false)
const menuTreeRef = ref<InstanceType<typeof ElTree>>()
const menuTree = ref<Menu[]>([])
const checkedMenuIds = ref<number[]>([])
const currentRole = ref<Role | null>(null)

// 数据权限
const dataScopeDialogVisible = ref(false)
const deptTreeRef = ref<InstanceType<typeof ElTree>>()
const deptTree = ref<Dept[]>([])
const checkedDeptIds = ref<number[]>([])
const dataScopeForm = reactive({ dataScope: 1, deptIds: [] as number[] })

const defaultForm: Role = {
  roleCode: '',
  roleName: '',
  orderNum: 0,
  dataScope: 1,
  status: 1,
  remark: ''
}

const form = reactive<Role>({ ...defaultForm })

const rules: FormRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

const dataScopeMap: Record<number, string> = {
  1: '全部',
  2: '自定义',
  3: '本部门',
  4: '本部门及以下',
  5: '仅本人'
}
const dataScopeLabel = (scope?: number) => dataScopeMap[scope ?? 1] || '全部'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRoleList(queryParams)
    if (res.code === 200) {
      roleList.value = res.data.data
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, defaultForm)
  form.id = undefined
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

const handleEdit = (row: Role) => {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑角色'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (form.id) {
    await updateRole(form)
    ElMessage.success('修改成功')
  } else {
    await addRole(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const handleDelete = (row: Role) => {
  ElMessageBox.confirm('确认删除该角色吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteRole(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

const handleStatusChange = async (row: Role) => {
  await changeRoleStatus(row.id!, row.status!)
  ElMessage.success('状态修改成功')
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadData()
}

const handleReset = () => {
  queryParams.roleName = undefined
  queryParams.roleCode = undefined
  queryParams.status = undefined
  handleSearch()
}

// 菜单权限
const handleMenuPerms = async (row: Role) => {
  currentRole.value = row
  // 加载菜单树
  const menuRes = await getMenuTree()
  if (menuRes.code === 200) {
    menuTree.value = menuRes.data
  }
  // 获取角色已分配的菜单ID
  const roleRes = await getRoleById(row.id!)
  if (roleRes.code === 200) {
    checkedMenuIds.value = roleRes.data.menuIds || []
  }
  menuDialogVisible.value = true
}

const handleSubmitMenuPerms = async () => {
  const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) as number[]
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() as number[]
  const menuIds = [...checkedKeys, ...halfCheckedKeys]
  await updateRole({ ...currentRole.value!, menuIds })
  ElMessage.success('菜单权限分配成功')
  menuDialogVisible.value = false
  loadData()
}

// 数据权限
const handleDataScope = async (row: Role) => {
  currentRole.value = row
  dataScopeForm.dataScope = row.dataScope || 1
  // 加载部门树
  const deptRes = await getDeptTree()
  if (deptRes.code === 200) {
    deptTree.value = deptRes.data
  }
  // 获取角色已分配的部门ID
  const roleRes = await getRoleById(row.id!)
  if (roleRes.code === 200) {
    checkedDeptIds.value = roleRes.data.deptIds || []
  }
  dataScopeDialogVisible.value = true
}

const handleSubmitDataScope = async () => {
  const deptIds = dataScopeForm.dataScope === 2
    ? (deptTreeRef.value?.getCheckedKeys(false) as number[])
    : []
  await setRoleDataScope({
    id: currentRole.value!.id,
    roleCode: currentRole.value!.roleCode,
    roleName: currentRole.value!.roleName,
    dataScope: dataScopeForm.dataScope,
    deptIds
  })
  ElMessage.success('数据权限设置成功')
  dataScopeDialogVisible.value = false
  loadData()
}

onMounted(() => {
  dictStore.loadDicts('sys_normal_disable')
  loadData()
})
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