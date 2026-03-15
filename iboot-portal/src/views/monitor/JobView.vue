<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="任务名称">
          <el-input v-model="queryParams.jobName" placeholder="请输入任务名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="任务组名">
          <el-select v-model="queryParams.jobGroup" placeholder="请选择" clearable>
            <el-option
              v-for="dict in dictStore.getDict('sys_job_group')"
              :key="dict.dictValue"
              :label="dict.dictLabel"
              :value="dict.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option
              v-for="dict in dictStore.getDict('sys_job_status')"
              :key="dict.dictValue"
              :label="dict.dictLabel"
              :value="Number(dict.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button v-permission="'job:add'" type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
        <el-button v-permission="'job:log'" type="info" :icon="Document" @click="showLogDialog = true">执行日志</el-button>
      </div>

      <!-- 任务列表 -->
      <el-table :data="jobList" style="width: 100%" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="任务ID" width="80" />
        <el-table-column prop="jobName" label="任务名称" width="160" show-overflow-tooltip />
        <el-table-column prop="jobGroup" label="任务组名" width="100">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_job_group', row.jobGroup) || 'info'" size="small">
              {{ dictStore.getDictLabel('sys_job_group', row.jobGroup) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invokeTarget" label="调用目标" min-width="200" show-overflow-tooltip />
        <el-table-column prop="cronExpression" label="cron表达式" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
              v-permission="'job:changeStatus'"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'job:edit'" type="primary" link size="small" @click="handleEdit(row)">
              修改
            </el-button>
            <el-button v-permission="'job:run'" type="success" link size="small" @click="handleRun(row)">
              执行
            </el-button>
            <el-button v-permission="'job:log'" type="info" link size="small" @click="viewJobLog(row)">
              日志
            </el-button>
            <el-button v-permission="'job:remove'" type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
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
    <el-dialog v-model="showDialog" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务组名" prop="jobGroup">
          <el-select v-model="form.jobGroup" placeholder="请选择任务组名">
            <el-option
              v-for="dict in dictStore.getDict('sys_job_group')"
              :key="dict.dictValue"
              :label="dict.dictLabel"
              :value="dict.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调用目标" prop="invokeTarget">
          <el-input v-model="form.invokeTarget" placeholder="如: sampleTask.noParams" />
          <div class="form-tip">Bean名称.方法名(参数)，例: sampleTask.withParams('hello')</div>
        </el-form-item>
        <el-form-item label="cron表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="如: 0/10 * * * * ?" />
          <div class="form-tip">
            秒 分 时 日 月 周 [年]，
            <el-link type="primary" href="https://cron.qqe2.com/" target="_blank">在线生成</el-link>
          </div>
        </el-form-item>
        <el-form-item label="错过策略" prop="misfirePolicy">
          <el-radio-group v-model="form.misfirePolicy">
            <el-radio
              v-for="dict in dictStore.getDict('sys_job_misfire')"
              :key="dict.dictValue"
              :value="Number(dict.dictValue)"
            >
              {{ dict.dictLabel }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否并发" prop="concurrent">
          <el-radio-group v-model="form.concurrent">
            <el-radio
              v-for="dict in dictStore.getDict('sys_job_concurrent')"
              :key="dict.dictValue"
              :value="Number(dict.dictValue)"
            >
              {{ dict.dictLabel }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dictStore.getDict('sys_job_status')"
              :key="dict.dictValue"
              :value="Number(dict.dictValue)"
            >
              {{ dict.dictLabel }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 执行日志对话框 -->
    <el-dialog v-model="showLogDialog" title="执行日志" width="900px" destroy-on-close>
      <el-form :inline="true" :model="logQueryParams" class="search-form">
        <el-form-item label="任务名称">
          <el-input v-model="logQueryParams.jobName" placeholder="请输入" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="执行状态">
          <el-select v-model="logQueryParams.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogData">搜索</el-button>
          <el-button @click="handleLogReset">重置</el-button>
          <el-button v-permission="'job:log'" type="danger" @click="handleCleanLog">清空日志</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logList" style="width: 100%" v-loading="logLoading" max-height="400">
        <el-table-column prop="id" label="日志ID" width="80" />
        <el-table-column prop="jobName" label="任务名称" width="140" show-overflow-tooltip />
        <el-table-column prop="invokeTarget" label="调用目标" min-width="180" show-overflow-tooltip />
        <el-table-column prop="jobMessage" label="日志信息" width="100" />
        <el-table-column prop="status" label="执行状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="90" align="center" />
        <el-table-column prop="createTime" label="执行时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewLogDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="logQueryParams.pageNum"
        v-model:page-size="logQueryParams.pageSize"
        :total="logTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadLogData"
        @current-change="loadLogData"
      />
    </el-dialog>

    <!-- 日志详情对话框 -->
    <el-dialog v-model="showLogDetailDialog" title="日志详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ currentLog?.jobId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentLog?.jobName }}</el-descriptions-item>
        <el-descriptions-item label="任务组名">{{ currentLog?.jobGroup }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag :type="currentLog?.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog?.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatTime(currentLog?.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatTime(currentLog?.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentLog?.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="日志信息">{{ currentLog?.jobMessage }}</el-descriptions-item>
        <el-descriptions-item label="调用目标" :span="2">{{ currentLog?.invokeTarget }}</el-descriptions-item>
        <el-descriptions-item v-if="currentLog?.exceptionInfo" label="异常信息" :span="2">
          <el-input type="textarea" :value="currentLog?.exceptionInfo" :rows="5" readonly />
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Document } from '@element-plus/icons-vue'
import {
  getJobList,
  getJobById,
  addJob,
  updateJob,
  deleteJob,
  changeJobStatus,
  runJob,
  getJobLogList,
  cleanJobLog,
  exportJobList,
  type Job,
  type JobLog,
  type JobQuery,
  type JobLogQuery
} from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportJobList({
      jobName: queryParams.jobName || undefined,
      jobGroup: queryParams.jobGroup || undefined,
      status: queryParams.status
    }),
    '定时任务'
  )
}

const loading = ref(false)
const jobList = ref<Job[]>([])
const total = ref(0)
const showDialog = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const queryParams = reactive<JobQuery>({
  pageNum: 1,
  pageSize: 10,
  jobName: '',
  jobGroup: '',
  status: undefined
})

const defaultForm: Job = {
  jobName: '',
  jobGroup: 'DEFAULT',
  invokeTarget: '',
  cronExpression: '',
  misfirePolicy: 3,
  concurrent: 0,
  status: 1,
  remark: ''
}

const form = reactive<Job>({ ...defaultForm })

const rules: FormRules = {
  jobName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  jobGroup: [{ required: true, message: '请选择任务组名', trigger: 'change' }],
  invokeTarget: [{ required: true, message: '请输入调用目标', trigger: 'blur' }],
  cronExpression: [{ required: true, message: '请输入cron表达式', trigger: 'blur' }]
}

// 日志相关
const showLogDialog = ref(false)
const logLoading = ref(false)
const logList = ref<JobLog[]>([])
const logTotal = ref(0)
const showLogDetailDialog = ref(false)
const currentLog = ref<JobLog | null>(null)

const logQueryParams = reactive<JobLogQuery>({
  pageNum: 1,
  pageSize: 10,
  jobId: undefined,
  jobName: '',
  status: undefined
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getJobList(queryParams)
    if (res.code === 200) {
      jobList.value = res.data.data
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
  queryParams.jobName = ''
  queryParams.jobGroup = ''
  queryParams.status = undefined
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增定时任务'
  Object.assign(form, defaultForm)
  form.id = undefined
  showDialog.value = true
}

const handleEdit = async (row: Job) => {
  dialogTitle.value = '编辑定时任务'
  const res = await getJobById(row.id!)
  if (res.code === 200) {
    Object.assign(form, res.data)
    showDialog.value = true
  }
}

const handleDelete = (row: Job) => {
  ElMessageBox.confirm(`确认要删除任务 "${row.jobName}" 吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteJob(row.id!)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

const handleStatusChange = async (row: Job) => {
  const statusText = row.status === 1 ? '启用' : '暂停'
  try {
    await changeJobStatus(row.id!, row.status!)
    ElMessage.success(`${statusText}成功`)
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleRun = (row: Job) => {
  ElMessageBox.confirm(`确认要立即执行任务 "${row.jobName}" 吗？`, '执行确认', { type: 'warning' })
    .then(async () => {
      await runJob(row.id!)
      ElMessage.success('执行命令已发送')
    })
    .catch(() => {})
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (form.id) {
      await updateJob(form)
      ElMessage.success('修改成功')
    } else {
      await addJob(form)
      ElMessage.success('新增成功')
    }
    showDialog.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

// 日志相关方法
const loadLogData = async () => {
  logLoading.value = true
  try {
    const res = await getJobLogList(logQueryParams)
    if (res.code === 200) {
      logList.value = res.data.data
      logTotal.value = res.data.total
    }
  } finally {
    logLoading.value = false
  }
}

const handleLogReset = () => {
  logQueryParams.jobId = undefined
  logQueryParams.jobName = ''
  logQueryParams.status = undefined
  logQueryParams.pageNum = 1
  loadLogData()
}

const viewJobLog = (row: Job) => {
  logQueryParams.jobId = row.id
  logQueryParams.jobName = row.jobName
  logQueryParams.pageNum = 1
  showLogDialog.value = true
  loadLogData()
}

const viewLogDetail = (row: JobLog) => {
  currentLog.value = row
  showLogDetailDialog.value = true
}

const handleCleanLog = () => {
  ElMessageBox.confirm('确认要清空30天前的执行日志吗？', '清空确认', { type: 'warning' })
    .then(async () => {
      const res = await cleanJobLog({ days: 30 })
      ElMessage.success(`清理了 ${res.data} 条日志`)
      loadLogData()
    })
    .catch(() => {})
}

const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 19)
}

onMounted(async () => {
  await dictStore.loadDicts('sys_job_group', 'sys_job_status', 'sys_job_misfire', 'sys_job_concurrent')
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

.search-form {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
