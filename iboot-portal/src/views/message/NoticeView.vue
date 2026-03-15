<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="primary" v-permission="'notice:add'" @click="handleAdd">新增公告</el-button>
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
      </div>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="公告标题">
          <el-input v-model="queryParams.noticeTitle" placeholder="请输入公告标题" clearable />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-select v-model="queryParams.noticeType" placeholder="请选择" clearable>
            <el-option
              v-for="item in dictStore.getDict('sys_notice_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="公告状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option
              v-for="item in dictStore.getDict('sys_notice_status')"
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

      <!-- 数据表格 -->
      <el-table :data="noticeList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="noticeTitle" label="公告标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="noticeType" label="公告类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_notice_type', row.noticeType)">
              {{ dictStore.getDictLabel('sys_notice_type', row.noticeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_notice_status', row.status)">
              {{ dictStore.getDictLabel('sys_notice_status', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isTop" label="置顶" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isTop === 1" type="danger" size="small">置顶</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createBy" label="创建人" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'notice:edit'"
              type="primary"
              link
              @click="handleEdit(row)"
              :disabled="row.status === '1'"
            >编辑</el-button>
            <el-button
              v-permission="'notice:publish'"
              type="success"
              link
              @click="handlePublish(row)"
              v-if="row.status !== '1'"
            >发布</el-button>
            <el-button
              v-permission="'notice:publish'"
              type="warning"
              link
              @click="handleRevoke(row)"
              v-if="row.status === '1'"
            >撤回</el-button>
            <el-button
              v-permission="'notice:remove'"
              type="danger"
              link
              @click="handleDelete(row)"
              :disabled="row.status === '1'"
            >删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="公告标题" prop="noticeTitle">
          <el-input v-model="form.noticeTitle" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="公告类型" prop="noticeType">
          <el-radio-group v-model="form.noticeType">
            <el-radio
              v-for="item in dictStore.getDict('sys_notice_type')"
              :key="item.dictValue"
              :value="item.dictValue"
            >{{ item.dictLabel }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公告内容" prop="noticeContent">
          <el-input
            v-model="form.noticeContent"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
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
import {
  getNoticeList,
  addNotice,
  updateNotice,
  deleteNotice,
  publishNotice,
  revokeNotice,
  exportNoticeList,
  type Notice
} from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportNoticeList({
      noticeTitle: queryParams.noticeTitle,
      noticeType: queryParams.noticeType,
      status: queryParams.status
    }),
    '公告列表'
  )
}

const loading = ref(false)
const noticeList = ref<Notice[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  noticeTitle: undefined as string | undefined,
  noticeType: undefined as string | undefined,
  status: undefined as string | undefined
})

const defaultForm: Notice = {
  noticeTitle: '',
  noticeType: '1',
  noticeContent: '',
  isTop: 0,
  remark: ''
}

const form = reactive<Notice>({ ...defaultForm })

const rules: FormRules = {
  noticeTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
  noticeContent: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getNoticeList(queryParams)
    if (res.code === 200) {
      noticeList.value = res.data.data
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
  queryParams.noticeTitle = undefined
  queryParams.noticeType = undefined
  queryParams.status = undefined
  handleSearch()
}

const resetForm = () => {
  Object.assign(form, defaultForm)
  form.id = undefined
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增公告'
  dialogVisible.value = true
}

const handleEdit = (row: Notice) => {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑公告'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (form.id) {
    await updateNotice(form)
    ElMessage.success('修改成功')
  } else {
    await addNotice(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const handleDelete = (row: Notice) => {
  ElMessageBox.confirm('确认删除该公告吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteNotice(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

const handlePublish = (row: Notice) => {
  ElMessageBox.confirm('确认发布该公告吗？发布后所有用户可见。', '提示', { type: 'info' })
    .then(async () => {
      await publishNotice(row.id!)
      ElMessage.success('发布成功')
      loadData()
    })
    .catch(() => {})
}

const handleRevoke = (row: Notice) => {
  ElMessageBox.confirm('确认撤回该公告吗？撤回后用户将不可见。', '提示', { type: 'warning' })
    .then(async () => {
      await revokeNotice(row.id!)
      ElMessage.success('撤回成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  dictStore.loadDicts('sys_notice_type', 'sys_notice_status')
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
