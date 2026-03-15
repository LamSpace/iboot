<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
        <el-button type="danger" @click="handleClean">清空日志</el-button>
      </div>
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="模块名称">
          <el-input v-model="queryParams.title" placeholder="请输入模块名称" clearable />
        </el-form-item>
        <el-form-item label="操作人员">
          <el-input v-model="queryParams.operatorName" placeholder="请输入操作人员" clearable />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="queryParams.businessType" placeholder="请选择" clearable>
            <el-option
              v-for="item in dictStore.getDict('sys_business_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="Number(item.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option
              v-for="item in dictStore.getDict('sys_common_status')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="Number(item.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
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
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="title" label="模块" min-width="120" show-overflow-tooltip />
        <el-table-column prop="businessType" label="业务类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_business_type', row.businessType)">
              {{ dictStore.getDictLabel('sys_business_type', row.businessType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="请求方式" width="100" />
        <el-table-column prop="operatorName" label="操作人员" width="120" />
        <el-table-column prop="operIp" label="操作IP" width="130" />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_common_status', row.status)">
              {{ dictStore.getDictLabel('sys_common_status', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costTime" label="耗时(ms)" width="100" />
        <el-table-column prop="operTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
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
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模块">{{ currentLog?.title }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ dictStore.getDictLabel('sys_business_type', currentLog?.businessType) }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ currentLog?.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作人员">{{ currentLog?.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ currentLog?.operIp }}</el-descriptions-item>
        <el-descriptions-item label="操作地点">{{ currentLog?.operLocation }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ currentLog?.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="方法名称" :span="2">{{ currentLog?.method }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <div class="code-block">{{ currentLog?.operParam }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2">
          <div class="code-block">{{ currentLog?.jsonResult }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="dictStore.getDictListClass('sys_common_status', currentLog?.status)">
            {{ dictStore.getDictLabel('sys_common_status', currentLog?.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog?.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ currentLog?.operTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentLog?.errorMsg" label="错误信息" :span="2">
          <div class="error-msg">{{ currentLog?.errorMsg }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOperateLogList, getOperateLogById, cleanOperateLog, exportOperateLogList, type OperateLog, type OperateLogQuery } from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportOperateLogList({
      title: queryParams.title,
      operatorName: queryParams.operatorName,
      businessType: queryParams.businessType,
      status: queryParams.status,
      beginTime: queryParams.beginTime,
      endTime: queryParams.endTime
    }),
    '操作日志'
  )
}

const loading = ref(false)
const logList = ref<OperateLog[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])
const detailVisible = ref(false)
const currentLog = ref<OperateLog | null>(null)

const queryParams = reactive<OperateLogQuery>({
  pageNum: 1,
  pageSize: 10,
  title: '',
  operatorName: '',
  businessType: undefined,
  status: undefined,
  beginTime: '',
  endTime: ''
})

watch(dateRange, (val) => {
  queryParams.beginTime = val?.[0] || ''
  queryParams.endTime = val?.[1] || ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOperateLogList(queryParams)
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
  queryParams.title = ''
  queryParams.operatorName = ''
  queryParams.businessType = undefined
  queryParams.status = undefined
  dateRange.value = []
  handleSearch()
}

const handleDetail = async (row: OperateLog) => {
  const res = await getOperateLogById(row.id!)
  if (res.code === 200) {
    currentLog.value = res.data
    detailVisible.value = true
  }
}

const handleClean = () => {
  ElMessageBox.confirm('确认清空所有操作日志吗？此操作不可恢复！', '警告', { type: 'warning' })
    .then(async () => {
      await cleanOperateLog()
      ElMessage.success('清空成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  dictStore.loadDicts('sys_common_status', 'sys_business_type')
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

.code-block {
  max-height: 200px;
  overflow: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.error-msg {
  color: #f56c6c;
}
</style>
