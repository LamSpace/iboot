<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-upload
          ref="uploadRef"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :data="uploadData"
          :before-upload="beforeUpload"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :show-file-list="false"
          multiple
        >
          <el-button type="primary"><el-icon><Upload /></el-icon> 上传文件</el-button>
        </el-upload>
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
      </div>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="文件名称">
          <el-input v-model="queryParams.fileName" placeholder="请输入文件名称" clearable />
        </el-form-item>
        <el-form-item label="文件分类">
          <el-select v-model="queryParams.fileCategory" placeholder="请选择" clearable>
            <el-option
              v-for="item in dictStore.getDict('sys_file_category')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 文件列表 -->
      <el-table :data="fileList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="fileName" label="文件名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-icon v-if="row.fileCategory === 'image'" color="#67C23A"><Picture /></el-icon>
            <el-icon v-else-if="row.fileCategory === 'video'" color="#E6A23C"><VideoCamera /></el-icon>
            <el-icon v-else-if="row.fileCategory === 'audio'" color="#409EFF"><Headset /></el-icon>
            <el-icon v-else-if="row.fileCategory === 'archive'" color="#F56C6C"><Box /></el-icon>
            <el-icon v-else color="#909399"><Document /></el-icon>
            <span style="margin-left: 6px">{{ row.fileName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="readableSize" label="文件大小" width="120" align="center" />
        <el-table-column prop="fileExt" label="扩展名" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" v-if="row.fileExt">.{{ row.fileExt }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileCategory" label="文件分类" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_file_category', row.fileCategory)">
              {{ dictStore.getDictLabel('sys_file_category', row.fileCategory) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadBy" label="上传人" width="120" align="center" />
        <el-table-column prop="uploadTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">下载</el-button>
            <el-button type="warning" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="'编辑文件信息'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="文件名称" prop="fileName">
          <el-input v-model="form.fileName" placeholder="请输入文件名称" />
        </el-form-item>
        <el-form-item label="文件分类">
          <el-select v-model="form.fileCategory" placeholder="请选择文件分类" clearable style="width: 100%">
            <el-option
              v-for="item in dictStore.getDict('sys_file_category')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRawFile } from 'element-plus'
import { Upload, Picture, VideoCamera, Headset, Box, Document } from '@element-plus/icons-vue'
import {
  getFileList,
  uploadFile,
  updateFileInfo,
  deleteFile,
  getFileDownloadUrl,
  exportFileList,
  type FileInfo,
  type FileUpdateParams
} from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const userStore = useUserStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportFileList({
      fileName: queryParams.fileName,
      fileCategory: queryParams.fileCategory
    }),
    '文件列表'
  )
}

const loading = ref(false)
const fileList = ref<FileInfo[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  fileName: undefined as string | undefined,
  fileCategory: undefined as string | undefined
})

const form = reactive<FileUpdateParams>({
  id: 0,
  fileName: '',
  fileCategory: '',
  remark: ''
})

const rules: FormRules = {
  fileName: [{ required: true, message: '请输入文件名称', trigger: 'blur' }]
}

// 上传配置
const uploadUrl = computed(() => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/iboot/api'
  return `${baseURL}/file/upload`
})

const uploadHeaders = computed(() => ({
  Authorization: userStore.token ? `Bearer ${userStore.token}` : ''
}))

const uploadData = ref<Record<string, string>>({})

const beforeUpload = (file: UploadRawFile) => {
  const maxSize = 50 * 1024 * 1024 // 50MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response: any) => {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    loadData()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFileList(queryParams)
    if (res.code === 200) {
      fileList.value = res.data.data
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
  queryParams.fileName = undefined
  queryParams.fileCategory = undefined
  handleSearch()
}

const handleEdit = (row: FileInfo) => {
  form.id = row.id!
  form.fileName = row.fileName
  form.fileCategory = row.fileCategory || ''
  form.remark = row.remark || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  await updateFileInfo(form)
  ElMessage.success('修改成功')
  dialogVisible.value = false
  loadData()
}

const handleDownload = (row: FileInfo) => {
  const url = getFileDownloadUrl(row.id!)
  const link = document.createElement('a')
  link.href = url
  // 附加 token 用于认证
  const token = userStore.token
  if (token) {
    link.href = url + '?token=' + encodeURIComponent(token)
  }
  link.target = '_blank'
  link.click()
}

const handleDelete = (row: FileInfo) => {
  ElMessageBox.confirm('确认删除该文件吗？删除后文件将无法恢复。', '提示', { type: 'warning' })
    .then(async () => {
      await deleteFile(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  dictStore.loadDicts('sys_file_category')
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
