<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="日志级别">
          <el-select v-model="queryParams.level" placeholder="请选择" clearable style="width: 120px">
            <el-option label="ERROR" value="ERROR" />
            <el-option label="WARN" value="WARN" />
            <el-option label="INFO" value="INFO" />
            <el-option label="DEBUG" value="DEBUG" />
            <el-option label="TRACE" value="TRACE" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="queryParams.keyword" placeholder="搜索消息内容" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="Logger">
          <el-input v-model="queryParams.logger" placeholder="如 com.iboot.admin" clearable />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logList" style="width: 100%" v-loading="loading">
        <el-table-column prop="timestamp" label="时间" width="180" />
        <el-table-column prop="level" label="级别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.level)" size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="thread" label="线程" width="160" show-overflow-tooltip />
        <el-table-column prop="logger" label="Logger" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ getShortLogger(row.logger) }}
          </template>
        </el-table-column>
        <el-table-column prop="message" label="消息" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="运行日志详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="时间">{{ currentLog?.timestamp }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag :type="getLevelTagType(currentLog?.level)" size="small">{{ currentLog?.level }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="线程" :span="2">{{ currentLog?.thread }}</el-descriptions-item>
        <el-descriptions-item label="Logger" :span="2">{{ currentLog?.logger }}</el-descriptions-item>
        <el-descriptions-item label="消息" :span="2">
          <pre class="code-block">{{ currentLog?.message }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { getRunLogList, type RunLog, type RunLogQuery } from '@/api/system'

const loading = ref(false)
const logList = ref<RunLog[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const detailVisible = ref(false)
const currentLog = ref<RunLog | null>(null)

const queryParams = reactive<RunLogQuery>({
  pageNum: 1,
  pageSize: 20,
  level: undefined,
  keyword: '',
  logger: '',
  thread: '',
  beginTime: '',
  endTime: ''
})

watch(dateRange, (val) => {
  queryParams.beginTime = val?.[0] || ''
  queryParams.endTime = val?.[1] || ''
})

const getLevelTagType = (level?: string) => {
  switch (level) {
    case 'ERROR': return 'danger'
    case 'WARN': return 'warning'
    case 'INFO': return 'success'
    case 'DEBUG': return 'info'
    default: return ''
  }
}

const getShortLogger = (logger?: string) => {
  if (!logger) return ''
  const parts = logger.split('.')
  return parts.length > 1 ? parts[parts.length - 1] : logger
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRunLogList(queryParams)
    if (res.code === 200) {
      logList.value = res.data.data
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
  queryParams.level = undefined
  queryParams.keyword = ''
  queryParams.logger = ''
  queryParams.thread = ''
  dateRange.value = []
  handleSearch()
}

const handleDetail = (row: RunLog) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
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
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.code-block {
  max-height: 400px;
  overflow: auto;
  padding: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
