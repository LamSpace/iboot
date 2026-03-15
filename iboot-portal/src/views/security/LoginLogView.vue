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
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="登录IP">
          <el-input v-model="queryParams.ipAddress" placeholder="请输入登录IP" clearable />
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
        <el-form-item label="登录时间">
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
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ipAddress" label="登录IP" width="150" />
        <el-table-column prop="loginLocation" label="登录地点" width="150" />
        <el-table-column prop="browser" label="浏览器" width="150" />
        <el-table-column prop="os" label="操作系统" width="150" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="dictStore.getDictListClass('sys_common_status', row.status)">
              {{ dictStore.getDictLabel('sys_common_status', row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="消息" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="180" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLoginLogList, cleanLoginLog, exportLoginLogList, type LoginLog, type LoginLogQuery } from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportLoginLogList({
      username: queryParams.username,
      ipAddress: queryParams.ipAddress,
      status: queryParams.status,
      beginTime: queryParams.beginTime,
      endTime: queryParams.endTime
    }),
    '登录日志'
  )
}

const loading = ref(false)
const logList = ref<LoginLog[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])

const queryParams = reactive<LoginLogQuery>({
  pageNum: 1,
  pageSize: 10,
  username: '',
  ipAddress: '',
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
    const res = await getLoginLogList(queryParams)
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
  queryParams.username = ''
  queryParams.ipAddress = ''
  queryParams.status = undefined
  dateRange.value = []
  handleSearch()
}

const handleClean = () => {
  ElMessageBox.confirm('确认清空所有登录日志吗？此操作不可恢复！', '警告', { type: 'warning' })
    .then(async () => {
      await cleanLoginLog()
      ElMessage.success('清空成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  dictStore.loadDicts('sys_common_status')
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
