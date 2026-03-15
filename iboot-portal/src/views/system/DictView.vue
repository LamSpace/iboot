<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" @click="handleAddType">新增字典类型</el-button>
        <el-button type="success" :loading="exportLoading" @click="handleExportType">导出类型</el-button>
        <el-button type="success" :loading="exportDataLoading" @click="handleExportData" :disabled="!currentType">导出数据</el-button>
      </div>
      <el-row :gutter="20">
        <!-- 左侧：字典类型列表 -->
        <el-col :span="12">
          <el-card>
            <template #header>字典类型</template>
            <el-table :data="typeList" style="width: 100%" v-loading="typeLoading" @row-click="handleTypeClick" highlight-current-row>
              <el-table-column prop="dictName" label="字典名称" />
              <el-table-column prop="dictType" label="字典类型" />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="dictStore.getDictListClass('sys_normal_disable', row.status)" size="small">
                    {{ dictStore.getDictLabel('sys_normal_disable', row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="300">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click.stop="handleEditType(row)">编辑</el-button>
                  <el-button type="danger" link size="small" @click.stop="handleDeleteType(row)">删除</el-button>
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
                <span>字典数据 {{ currentType ? `- ${currentType.dictName}` : '' }}</span>
                <el-button type="primary" size="small" @click="handleAddData" :disabled="!currentType">新增</el-button>
              </div>
            </template>
            <el-table :data="dataList" style="width: 100%" v-loading="dataLoading">
              <el-table-column prop="dictLabel" label="字典标签" />
              <el-table-column prop="dictValue" label="字典值" />
              <el-table-column prop="orderNum" label="排序" width="80" align="center" />
              <el-table-column prop="status" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="dictStore.getDictListClass('sys_normal_disable', row.status)" size="small">
                    {{ dictStore.getDictLabel('sys_normal_disable', row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="300">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="handleEditData(row)">编辑</el-button>
                  <el-button type="danger" link size="small" @click="handleDeleteData(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 字典类型对话框 -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="500px">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="100px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="请输入字典类型" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
            >{{ item.dictLabel }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitType">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据对话框 -->
    <el-dialog v-model="dataDialogVisible" :title="dataDialogTitle" width="500px">
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="100px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="请输入字典值" />
        </el-form-item>
        <el-form-item label="显示顺序">
          <el-input-number v-model="dataForm.orderNum" :min="0" />
        </el-form-item>
        <el-form-item label="样式属性">
          <el-input v-model="dataForm.cssClass" placeholder="请输入样式属性" />
        </el-form-item>
        <el-form-item label="回显样式">
          <el-select v-model="dataForm.listClass" placeholder="请选择">
            <el-option label="默认" value="" />
            <el-option label="primary" value="primary" />
            <el-option label="success" value="success" />
            <el-option label="info" value="info" />
            <el-option label="warning" value="warning" />
            <el-option label="danger" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否默认">
          <el-radio-group v-model="dataForm.isDefault">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio
              v-for="item in dictStore.getDict('sys_normal_disable')"
              :key="item.dictValue"
              :value="Number(item.dictValue)"
            >{{ item.dictLabel }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getDictTypeList, addDictType, updateDictType, deleteDictType,
  getDictDataByType, addDictData, updateDictData, deleteDictData,
  exportDictTypeList, exportDictDataList,
  type DictType, type DictData
} from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()
const { exportLoading: exportDataLoading, handleExport: performExportData } = useExport()

const handleExportType = () => {
  performExport(() => exportDictTypeList({}), '字典类型')
}

const handleExportData = () => {
  if (!currentType.value) return
  performExportData(
    () => exportDictDataList({ dictType: currentType.value!.dictType }),
    `字典数据_${currentType.value.dictType}`
  )
}

// 字典类型相关
const typeLoading = ref(false)
const typeList = ref<DictType[]>([])
const typeTotal = ref(0)
const typePageNum = ref(1)
const typePageSize = ref(10)
const typeDialogVisible = ref(false)
const typeDialogTitle = ref('')
const typeFormRef = ref<FormInstance>()
const currentType = ref<DictType | null>(null)

const defaultTypeForm: DictType = { dictName: '', dictType: '', status: 1, remark: '' }
const typeForm = reactive<DictType>({ ...defaultTypeForm })
const typeRules: FormRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}

// 字典数据相关
const dataLoading = ref(false)
const dataList = ref<DictData[]>([])
const dataDialogVisible = ref(false)
const dataDialogTitle = ref('')
const dataFormRef = ref<FormInstance>()

const defaultDataForm: DictData = { dictType: '', dictLabel: '', dictValue: '', orderNum: 0, cssClass: '', listClass: '', isDefault: 0, status: 1, remark: '' }
const dataForm = reactive<DictData>({ ...defaultDataForm })
const dataRules: FormRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }]
}

const loadTypeData = async () => {
  typeLoading.value = true
  try {
    const res = await getDictTypeList(typePageNum.value, typePageSize.value)
    if (res.code === 200) {
      typeList.value = res.data.data
      typeTotal.value = res.data.total
    }
  } finally {
    typeLoading.value = false
  }
}

const loadDataList = async () => {
  if (!currentType.value) return
  dataLoading.value = true
  try {
    const res = await getDictDataByType(currentType.value.dictType)
    if (res.code === 200) {
      dataList.value = res.data
    }
  } finally {
    dataLoading.value = false
  }
}

const handleTypeClick = (row: DictType) => {
  currentType.value = row
  loadDataList()
}

const handleAddType = () => {
  Object.assign(typeForm, defaultTypeForm)
  typeForm.id = undefined
  typeDialogTitle.value = '新增字典类型'
  typeDialogVisible.value = true
}

const handleEditType = (row: DictType) => {
  Object.assign(typeForm, row)
  typeDialogTitle.value = '编辑字典类型'
  typeDialogVisible.value = true
}

const handleSubmitType = async () => {
  await typeFormRef.value?.validate()
  if (typeForm.id) {
    await updateDictType(typeForm)
    ElMessage.success('修改成功')
  } else {
    await addDictType(typeForm)
    ElMessage.success('新增成功')
  }
  typeDialogVisible.value = false
  loadTypeData()
}

const handleDeleteType = (row: DictType) => {
  ElMessageBox.confirm('确认删除该字典类型吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteDictType(row.id!)
      ElMessage.success('删除成功')
      if (currentType.value?.id === row.id) {
        currentType.value = null
        dataList.value = []
      }
      loadTypeData()
    })
    .catch(() => {})
}

const handleAddData = () => {
  Object.assign(dataForm, defaultDataForm)
  dataForm.id = undefined
  dataForm.dictType = currentType.value!.dictType
  dataDialogTitle.value = '新增字典数据'
  dataDialogVisible.value = true
}

const handleEditData = (row: DictData) => {
  Object.assign(dataForm, row)
  dataDialogTitle.value = '编辑字典数据'
  dataDialogVisible.value = true
}

const handleSubmitData = async () => {
  await dataFormRef.value?.validate()
  if (dataForm.id) {
    await updateDictData(dataForm)
    ElMessage.success('修改成功')
  } else {
    await addDictData(dataForm)
    ElMessage.success('新增成功')
  }
  dataDialogVisible.value = false
  loadDataList()
}

const handleDeleteData = (row: DictData) => {
  ElMessageBox.confirm('确认删除该字典数据吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteDictData(row.id!)
      ElMessage.success('删除成功')
      loadDataList()
    })
    .catch(() => {})
}

onMounted(() => {
  dictStore.loadDicts('sys_normal_disable')
  loadTypeData()
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
