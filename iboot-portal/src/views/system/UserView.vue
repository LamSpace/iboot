<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">新增用户</el-button>
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
      </div>
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="queryParams.phone" placeholder="请输入手机号" clearable />
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

      <el-table :data="userList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_user_gender', row.gender)" size="small">
              {{ dictStore.getDictLabel('sys_user_gender', row.gender) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="row.userType === 1"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="danger" link @click="handleDelete(row)" :disabled="row.userType === 1">删除</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio
              v-for="item in dictStore.getDict('sys_user_gender')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
            >{{ item.dictLabel }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-tree-select
            v-model="form.deptId"
            :data="deptTree"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择部门"
            clearable
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.postIds" multiple placeholder="请选择岗位" style="width: 100%">
            <el-option v-for="item in postList" :key="item.id" :label="item.postName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleList" :key="item.id" :label="item.roleName" :value="item.id!" />
          </el-select>
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

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetPwdDialogVisible" title="重置密码" width="400px">
      <el-form ref="resetPwdFormRef" :model="resetPwdForm" :rules="resetPwdRules" label-width="100px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetPwdForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getUserList, addUser, updateUser, deleteUser, changeUserStatus, resetUserPassword,
  getDeptTree, getAllPosts, getAllRoles, exportUserList,
  type User, type UserQuery, type Dept, type Post, type Role
} from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportUserList({
      username: queryParams.username,
      phone: queryParams.phone,
      status: queryParams.status
    }),
    '用户列表'
  )
}

const loading = ref(false)
const userList = ref<User[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

// 搜索参数
const queryParams = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  username: undefined,
  phone: undefined,
  status: undefined
})

// 关联数据
const deptTree = ref<Dept[]>([])
const postList = ref<Post[]>([])
const roleList = ref<Role[]>([])

// 重置密码
const resetPwdDialogVisible = ref(false)
const resetPwdFormRef = ref<FormInstance>()
const resetPwdUserId = ref<number>()
const resetPwdForm = reactive({ newPassword: '' })
const resetPwdRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20之间', trigger: 'blur' }
  ]
}

const defaultForm: User = {
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  deptId: undefined,
  postIds: [],
  roleIds: [],
  status: 1,
  remark: ''
}

const form = reactive<User>({ ...defaultForm })

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 30, message: '用户名长度必须在2-30之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20之间', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList(queryParams)
    if (res.code === 200) {
      userList.value = res.data.data
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const loadRelatedData = async () => {
  const [deptRes, postRes, roleRes] = await Promise.all([
    getDeptTree(),
    getAllPosts(),
    getAllRoles()
  ])
  if (deptRes.code === 200) deptTree.value = deptRes.data
  if (postRes.code === 200) postList.value = postRes.data
  if (roleRes.code === 200) roleList.value = roleRes.data
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadData()
}

const handleReset = () => {
  queryParams.username = undefined
  queryParams.phone = undefined
  queryParams.status = undefined
  handleSearch()
}

const resetForm = () => {
  Object.assign(form, defaultForm)
  form.id = undefined
  form.postIds = []
  form.roleIds = []
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

const handleEdit = (row: User) => {
  resetForm()
  Object.assign(form, { ...row, password: undefined })
  dialogTitle.value = '编辑用户'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (form.id) {
    await updateUser(form)
    ElMessage.success('修改成功')
  } else {
    await addUser(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const handleDelete = (row: User) => {
  ElMessageBox.confirm('确认删除该用户吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteUser(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

const handleStatusChange = async (row: User) => {
  await changeUserStatus(row.id!, row.status!)
  ElMessage.success('状态修改成功')
}

const handleResetPwd = (row: User) => {
  if (row.userType === 1) {
    ElMessageBox.confirm('该用户为系统内置管理员，确认要重置其密码吗？', '安全提示', {
      type: 'warning',
      confirmButtonText: '确认重置',
      cancelButtonText: '取消'
    }).then(() => {
      resetPwdUserId.value = row.id
      resetPwdForm.newPassword = ''
      resetPwdDialogVisible.value = true
    }).catch(() => {})
  } else {
    resetPwdUserId.value = row.id
    resetPwdForm.newPassword = ''
    resetPwdDialogVisible.value = true
  }
}

const handleSubmitResetPwd = async () => {
  await resetPwdFormRef.value?.validate()
  await resetUserPassword(resetPwdUserId.value!, resetPwdForm.newPassword)
  ElMessage.success('密码重置成功')
  resetPwdDialogVisible.value = false
}

onMounted(() => {
  dictStore.loadDicts('sys_user_gender', 'sys_normal_disable')
  loadData()
  loadRelatedData()
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
