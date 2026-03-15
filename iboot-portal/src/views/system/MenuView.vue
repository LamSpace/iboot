<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">新增菜单</el-button>
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
      </div>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="菜单名称">
          <el-input v-model="queryParams.menuName" placeholder="请输入菜单名称" clearable />
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
      <el-table
        :data="filteredMenuTree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="icon" label="图标" width="80">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column prop="perms" label="权限标识" width="180" />
        <el-table-column prop="component" label="组件路径" width="180" />
        <el-table-column prop="menuType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getMenuTypeTag(row.menuType)">
              {{ getMenuTypeLabel(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_normal_disable', row.status)">
              {{ dictStore.getDictLabel('sys_normal_disable', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link @click="handleAddChild(row)" v-if="row.menuType !== 'F'">新增</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuOptions"
            :props="{ label: 'menuName', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="请选择上级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="form.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="路由地址" v-if="form.menuType !== 'F'">
          <el-input v-model="form.path" placeholder="请输入路由地址" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="form.menuType === 'C'">
          <el-input v-model="form.component" placeholder="请输入组件路径" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="form.menuType !== 'M'">
          <el-input v-model="form.perms" placeholder="请输入权限标识" />
        </el-form-item>
        <el-form-item label="菜单图标" v-if="form.menuType !== 'F'">
          <el-input v-model="form.icon" placeholder="请输入菜单图标" />
        </el-form-item>
        <el-form-item label="是否外链" v-if="form.menuType !== 'F'">
          <el-radio-group v-model="form.isFrame">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否缓存" v-if="form.menuType === 'C'">
          <el-radio-group v-model="form.isCache">
            <el-radio :value="1">缓存</el-radio>
            <el-radio :value="0">不缓存</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否可见" v-if="form.menuType !== 'F'">
          <el-radio-group v-model="form.visible">
            <el-radio
              v-for="item in dictStore.getDict('sys_show_hide')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
            >{{ item.dictLabel }}</el-radio>
          </el-radio-group>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getMenuTree, addMenu, updateMenu, deleteMenu, exportMenuList, type Menu } from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportMenuList({
      menuName: queryParams.menuName || undefined,
      status: queryParams.status
    }),
    '菜单列表'
  )
}

const loading = ref(false)
const menuTree = ref<Menu[]>([])
const menuOptions = ref<Menu[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

const queryParams = reactive({
  menuName: '' as string,
  status: undefined as number | undefined
})

const defaultForm: Menu = {
  parentId: undefined,
  menuName: '',
  menuType: 'M',
  path: '',
  component: '',
  perms: '',
  icon: '',
  orderNum: 0,
  isFrame: 0,
  isCache: 1,
  visible: 1,
  status: 1
}

const form = reactive<Menu>({ ...defaultForm })

const rules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

const getMenuTypeLabel = (type: string) => {
  const map: Record<string, string> = { M: '目录', C: '菜单', F: '按钮' }
  return map[type] || type
}

const getMenuTypeTag = (type: string) => {
  const map: Record<string, string> = { M: '', C: 'success', F: 'warning' }
  return map[type] || ''
}

const filterTree = (nodes: Menu[], name: string, status: number | undefined): Menu[] => {
  return nodes.reduce((acc: Menu[], node) => {
    const children = node.children ? filterTree(node.children, name, status) : []
    const nameMatch = !name || node.menuName.includes(name)
    const statusMatch = status === undefined || node.status === status
    if (nameMatch && statusMatch || children.length > 0) {
      acc.push({ ...node, children: children.length > 0 ? children : node.children && nameMatch && statusMatch ? [] : children })
    }
    return acc
  }, [])
}

const filteredMenuTree = ref<Menu[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMenuTree()
    if (res.code === 200) {
      menuTree.value = res.data
      filteredMenuTree.value = res.data
      menuOptions.value = [{ id: 0, menuName: '主目录', children: res.data }] as Menu[]
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
  dialogTitle.value = '新增菜单'
  dialogVisible.value = true
}

const handleAddChild = (row: Menu) => {
  resetForm()
  form.parentId = row.id
  dialogTitle.value = '新增菜单'
  dialogVisible.value = true
}

const handleEdit = (row: Menu) => {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑菜单'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (form.id) {
    await updateMenu(form)
    ElMessage.success('修改成功')
  } else {
    await addMenu(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const handleDelete = (row: Menu) => {
  ElMessageBox.confirm('确认删除该菜单吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteMenu(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

const handleSearch = () => {
  if (!queryParams.menuName && queryParams.status === undefined) {
    filteredMenuTree.value = menuTree.value
  } else {
    filteredMenuTree.value = filterTree(menuTree.value, queryParams.menuName, queryParams.status)
  }
}

const handleReset = () => {
  queryParams.menuName = ''
  queryParams.status = undefined
  handleSearch()
}

onMounted(() => {
  dictStore.loadDicts('sys_normal_disable', 'sys_show_hide')
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
