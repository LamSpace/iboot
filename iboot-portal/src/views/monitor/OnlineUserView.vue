<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 统计信息 -->
      <div class="stats-bar">
        <el-tag type="success" size="large" effect="dark">
          当前在线用户: {{ total }} 人
        </el-tag>
        <div class="stats-actions">
          <el-button type="success" :loading="exportLoading" @click="handleExport">导出Excel</el-button>
          <el-tooltip content="刷新" placement="top">
            <el-button :icon="Refresh" circle @click="loadData" />
          </el-tooltip>
          <el-switch
            v-model="autoRefresh"
            active-text="自动刷新"
            inactive-text=""
            style="margin-left: 12px"
            @change="toggleAutoRefresh"
          />
        </div>
      </div>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="登录IP">
          <el-input v-model="queryParams.loginIp" placeholder="请输入登录IP" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 在线用户表格 -->
      <el-table :data="userList" style="width: 100%" v-loading="loading" row-key="tokenId">
        <el-table-column prop="tokenId" label="会话ID" width="200" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="deptName" label="部门" width="120" />
        <el-table-column prop="loginIp" label="登录IP" width="140" />
        <el-table-column prop="browser" label="浏览器" width="120">
          <template #default="{ row }">
            <el-tag
              v-if="row.browser"
              :type="dictStore.getDictListClass('sys_browser_type', row.browser) || 'info'"
              size="small"
            >
              {{ row.browser }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="os" label="操作系统" width="120">
          <template #default="{ row }">
            <el-tag
              v-if="row.os"
              :type="dictStore.getDictListClass('sys_os_type', row.os) || 'info'"
              size="small"
            >
              {{ row.os }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginTime" label="登录时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'online:forceLogout'"
              type="danger"
              link
              size="small"
              @click="handleForceLogout(row)"
            >
              强退
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  getOnlineUserList,
  forceLogout,
  getConfigByKey,
  exportOnlineUserList,
  type OnlineUser,
  type OnlineUserQuery
} from '@/api/system'
import { useDictStore } from '@/stores/dict'
import { useExport } from '@/composables/useExport'

const dictStore = useDictStore()
const { exportLoading, handleExport: performExport } = useExport()

const handleExport = () => {
  performExport(
    () => exportOnlineUserList({
      username: queryParams.username,
      loginIp: queryParams.loginIp
    }),
    '在线用户'
  )
}

const loading = ref(false)
const userList = ref<OnlineUser[]>([])
const total = ref(0)
const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
const refreshInterval = ref(30)

const queryParams = reactive<OnlineUserQuery>({
  pageNum: 1,
  pageSize: 10,
  username: '',
  loginIp: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOnlineUserList(queryParams)
    if (res.code === 200) {
      userList.value = res.data.data
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
  queryParams.loginIp = ''
  handleSearch()
}

const handleForceLogout = (row: OnlineUser) => {
  ElMessageBox.confirm(
    `确认要强制退出用户 "${row.username}" 吗？`,
    '强制退出确认',
    { type: 'warning' }
  )
    .then(async () => {
      if (!row.tokenId) return
      await forceLogout(row.tokenId)
      ElMessage.success('强退成功')
      loadData()
    })
    .catch(() => {})
}

const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  // ISO format: 2024-01-01T12:00:00 -> 2024-01-01 12:00:00
  return timeStr.replace('T', ' ').substring(0, 19)
}

const toggleAutoRefresh = (val: boolean | string | number) => {
  if (val) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  stopAutoRefresh()
  if (refreshInterval.value > 0) {
    refreshTimer = setInterval(() => {
      loadData()
    }, refreshInterval.value * 1000)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

const loadRefreshConfig = async () => {
  try {
    const res = await getConfigByKey('sys.online.refreshInterval')
    if (res.code === 200 && res.data) {
      const val = parseInt(res.data as unknown as string, 10)
      if (!isNaN(val) && val > 0) {
        refreshInterval.value = val
      }
    }
  } catch {
    // use default
  }
}

onMounted(async () => {
  await dictStore.loadDicts('sys_browser_type', 'sys_os_type')
  await loadRefreshConfig()
  loadData()
})

onUnmounted(() => {
  stopAutoRefresh()
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

.stats-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stats-actions {
  display: flex;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
