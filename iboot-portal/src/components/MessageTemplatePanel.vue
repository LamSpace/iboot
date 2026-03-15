<template>
  <div>
    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" v-permission="'message:template:add'" @click="handleAdd">新增模板</el-button>
    </div>

    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item label="模板名称">
        <el-input v-model="queryParams.templateName" placeholder="请输入模板名称" clearable />
      </el-form-item>
      <el-form-item label="消息类型">
        <el-select v-model="queryParams.messageType" placeholder="请选择" clearable>
          <el-option
            v-for="item in dictStore.getDict('sys_message_type')"
            :key="item.dictValue"
            :label="item.dictLabel"
            :value="item.dictValue"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 数据表格 -->
    <el-table :data="templateList" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="templateCode" label="模板编码" width="160" show-overflow-tooltip />
      <el-table-column prop="templateName" label="模板名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="messageType" label="消息类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="dictStore.getDictListClass('sys_message_type', row.messageType)">
            {{ dictStore.getDictLabel('sys_message_type', row.messageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createBy" label="创建人" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'message:template:edit'"
            type="primary"
            link
            @click="handleEdit(row)"
          >编辑</el-button>
          <el-button
            v-permission="'message:template:remove'"
            type="danger"
            link
            @click="handleDelete(row)"
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="请输入模板编码" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="消息类型" prop="messageType">
          <el-select v-model="form.messageType" placeholder="请选择消息类型">
            <el-option
              v-for="item in dictStore.getDict('sys_message_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="模板内容" prop="templateContent">
          <el-input
            v-model="form.templateContent"
            type="textarea"
            :rows="6"
            placeholder="请输入模板内容，支持 ${变量名} 占位符"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" maxlength="500" show-word-limit />
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
  getMessageTemplateList,
  addMessageTemplate,
  updateMessageTemplate,
  deleteMessageTemplate,
  type MessageTemplate,
  type MessageTemplateQuery
} from '@/api/system'
import { useDictStore } from '@/stores/dict'

const dictStore = useDictStore()

const loading = ref(false)
const templateList = ref<MessageTemplate[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()

const queryParams = reactive<MessageTemplateQuery>({
  pageNum: 1,
  pageSize: 10,
  templateName: undefined,
  messageType: undefined,
  status: undefined
})

const defaultForm: MessageTemplate = {
  templateCode: '',
  templateName: '',
  templateContent: '',
  messageType: '1',
  status: 1,
  remark: ''
}

const form = reactive<MessageTemplate>({ ...defaultForm })

const rules: FormRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  messageType: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  templateContent: [{ required: true, message: '请输入模板内容', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMessageTemplateList(queryParams)
    if (res.code === 200) {
      templateList.value = res.data.data
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
  queryParams.templateName = undefined
  queryParams.messageType = undefined
  queryParams.status = undefined
  handleSearch()
}

const resetForm = () => {
  Object.assign(form, defaultForm)
  form.id = undefined
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增消息模板'
  dialogVisible.value = true
}

const handleEdit = (row: MessageTemplate) => {
  resetForm()
  Object.assign(form, row)
  dialogTitle.value = '编辑消息模板'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  if (form.id) {
    await updateMessageTemplate(form)
    ElMessage.success('修改成功')
  } else {
    await addMessageTemplate(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const handleDelete = (row: MessageTemplate) => {
  ElMessageBox.confirm('确认删除该消息模板吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteMessageTemplate(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  dictStore.loadDicts('sys_message_type')
  loadData()
})
</script>

<style scoped>
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
