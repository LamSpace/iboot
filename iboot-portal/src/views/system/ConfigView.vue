<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">新增配置</el-button>
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
        <el-button type="warning" @click="handleRefreshCache">刷新缓存</el-button>
      </div>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="配置名称">
          <el-input v-model="queryParams.configName" placeholder="请输入配置名称" clearable />
        </el-form-item>
        <el-form-item label="配置键">
          <el-input v-model="queryParams.configKey" placeholder="请输入配置键" clearable />
        </el-form-item>
        <el-form-item label="配置类型">
          <el-select v-model="queryParams.configType" placeholder="请选择" clearable>
            <el-option
              v-for="item in dictStore.getDict('sys_config_type')"
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

      <el-table :data="configList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="configName" label="配置名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="configKey" label="配置键" min-width="180" show-overflow-tooltip />
        <el-table-column prop="configValue" label="配置值" min-width="150" show-overflow-tooltip />
        <el-table-column prop="configType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_config_type', row.configType)">
              {{ dictStore.getDictLabel('sys_config_type', row.configType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)" :disabled="row.configType === 1">删除</el-button>
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
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="配置键" prop="configKey">
          <el-input v-model="form.configKey" placeholder="请输入配置键" />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="form.configValue" type="textarea" :rows="3" placeholder="请输入配置值" />
        </el-form-item>
        <el-form-item label="配置类型">
          <el-radio-group v-model="form.configType">
            <el-radio
              v-for="item in dictStore.getDict('sys_config_type')"
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getConfigList, addConfig, updateConfig, deleteConfig, refreshConfigCache, exportConfigList, type Config } from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportConfigList({
      configName: queryParams.configName,
      configKey: queryParams.configKey,
      configType: queryParams.configType
    }),
    '配置列表'
  )
}

const loading = ref(false)
const configList = ref<Config[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  configName: undefined as string | undefined,
  configKey: undefined as string | undefined,
  configType: undefined as number | undefined
})

const defaultForm: Config = {
  configName: '',
  configKey: '',
  configValue: '',
  configType: 0,
  remark: ''
}

const form = reactive<Config>({ ...defaultForm })

const rules: FormRules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getConfigList(queryParams)
    if (res.code === 200) {
      configList.value = res.data.data
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  loadData()
}

const handleReset = () => {
  queryParams.configName = undefined
  queryParams.configKey = undefined
  queryParams.configType = undefined
  handleSearch()
}

const resetForm = () => {
  Object.assign(form, defaultForm)
  form.id = undefined
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增配置'
  dialogVisible.value = true
}

const handleEdit = (row: Config) => {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑配置'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (form.id) {
    await updateConfig(form)
    ElMessage.success('修改成功')
  } else {
    await addConfig(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const handleDelete = (row: Config) => {
  ElMessageBox.confirm('确认删除该配置吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteConfig(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

const handleRefreshCache = async () => {
  await refreshConfigCache()
  ElMessage.success('缓存刷新成功')
}

onMounted(() => {
  dictStore.loadDicts('sys_config_type')
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
