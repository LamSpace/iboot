<template>
  <div class="content-wrapper">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="item in statsCards" :key="item.label">
        <el-card shadow="hover" class="stat-card" :style="{ borderTop: `3px solid ${item.color}` }">
          <div class="stat-card-body">
            <div class="stat-info">
              <div class="stat-label">{{ item.label }}</div>
              <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
            </div>
            <el-icon class="stat-icon" :style="{ color: item.color }" :size="36">
              <component :is="item.icon" />
            </el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统状态 + 登录趋势 -->
    <el-row :gutter="16" class="charts-row">
      <!-- 系统健康状态 -->
      <el-col :span="8">
        <el-card shadow="hover" class="health-card">
          <template #header>
            <div class="card-header">
              <span>系统健康状态</span>
              <el-tag :type="systemHealthType" size="small" effect="dark">
                {{ systemHealthLabel }}
              </el-tag>
            </div>
          </template>
          <div class="health-items" v-if="serverInfo">
            <div class="health-item">
              <div class="health-label">
                <el-icon><Cpu /></el-icon>
                <span>CPU</span>
              </div>
              <el-progress 
                :percentage="Math.round(serverInfo.cpu.totalUsage)" 
                :status="getProgressStatus(serverInfo.cpu.status)"
                :stroke-width="12"
              />
            </div>
            <div class="health-item">
              <div class="health-label">
                <el-icon><Coin /></el-icon>
                <span>内存</span>
              </div>
              <el-progress 
                :percentage="Math.round(serverInfo.memory.usageRate)" 
                :status="getProgressStatus(serverInfo.memory.status)"
                :stroke-width="12"
              />
            </div>
            <div class="health-item">
              <div class="health-label">
                <el-icon><Box /></el-icon>
                <span>JVM</span>
              </div>
              <el-progress 
                :percentage="Math.round(serverInfo.jvm.usageRate)" 
                :status="getProgressStatus(serverInfo.jvm.status)"
                :stroke-width="12"
              />
            </div>
          </div>
          <div v-else class="health-loading">
            <el-skeleton :rows="3" animated />
          </div>
        </el-card>
      </el-col>

      <!-- 登录趋势图 -->
      <el-col :span="16">
        <el-card shadow="hover" class="trend-card">
          <template #header>
            <div class="card-header">
              <span>最近7天登录趋势</span>
              <el-tag type="info" size="small">
                累计 {{ totalLoginCount }} 次
              </el-tag>
            </div>
          </template>
          <div class="trend-chart" v-if="loginTrend.length > 0">
            <div class="trend-bars">
              <div 
                v-for="item in loginTrend" 
                :key="item.date" 
                class="trend-bar-wrapper"
              >
                <div class="trend-value">{{ item.value }}</div>
                <div 
                  class="trend-bar"
                  :style="{ height: getTrendBarHeight(item.value) + '%' }"
                ></div>
              </div>
            </div>
            <div class="trend-labels">
              <span v-for="item in loginTrend" :key="item.date">
                {{ formatDateLabel(item.date) }}
              </span>
            </div>
          </div>
          <div v-else class="trend-empty">
            <el-empty description="暂无趋势数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近日志 -->
    <el-row :gutter="16" class="logs-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近登录日志</span>
              <el-button link type="primary" @click="router.push('/dashboard/login-log')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="summary?.recentLoginLogs ?? []" style="width: 100%" v-loading="loading" size="small">
            <el-table-column prop="username" label="用户名" min-width="100" />
            <el-table-column prop="ipAddress" label="登录IP" min-width="120" />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="loginTime" label="时间" min-width="160" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>最近操作日志</span>
              <el-button link type="primary" @click="router.push('/dashboard/operate-log')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="summary?.recentOperateLogs ?? []" style="width: 100%" v-loading="loading" size="small">
            <el-table-column prop="operatorName" label="操作人" min-width="100" />
            <el-table-column prop="title" label="模块" min-width="100" />
            <el-table-column prop="requestMethod" label="方式" width="70" align="center" />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operTime" label="时间" min-width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷导航 -->
    <el-card shadow="hover" class="shortcuts-card">
      <template #header>
        <span>快捷导航</span>
      </template>
      <el-row :gutter="16">
        <el-col :span="4" v-for="item in shortcuts" :key="item.path">
          <div class="shortcut-item" @click="router.push(`/dashboard/${item.path}`)">
            <el-icon :size="28" :style="{ color: item.color }">
              <component :is="item.icon" />
            </el-icon>
            <span class="shortcut-label">{{ item.label }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, type Component } from 'vue'
import { useRouter } from 'vue-router'
import { 
  User, Avatar, OfficeBuilding, Briefcase, Tickets, Operation, 
  Setting, Document, UserFilled, Monitor, Cpu, Coin, Box 
} from '@element-plus/icons-vue'
import { 
  getDashboardSummary, 
  getOnlineUserCount,
  getServerMonitorInfo,
  getStatisticsReport,
  type DashboardSummary,
  type ServerInfoResponse,
  type TrendData
} from '@/api/system'

const router = useRouter()
const loading = ref(false)
const summary = ref<DashboardSummary | null>(null)
const onlineCount = ref(0)
const serverInfo = ref<ServerInfoResponse | null>(null)
const loginTrend = ref<TrendData[]>([])
const todayActiveUsers = ref(0)

// 自动刷新定时器
let refreshTimer: ReturnType<typeof setInterval> | null = null

interface StatCard {
  label: string
  value: number
  icon: Component
  color: string
}

const statsCards = computed<StatCard[]>(() => [
  { label: '用户总数', value: summary.value?.userCount ?? 0, icon: User, color: '#409eff' },
  { label: '今日活跃', value: todayActiveUsers.value, icon: UserFilled, color: '#67c23a' },
  { label: '在线用户', value: onlineCount.value, icon: Monitor, color: '#e6a23c' },
  { label: '部门数', value: summary.value?.deptCount ?? 0, icon: OfficeBuilding, color: '#f56c6c' },
  { label: '登录日志', value: summary.value?.loginLogCount ?? 0, icon: Tickets, color: '#909399' },
  { label: '操作日志', value: summary.value?.operateLogCount ?? 0, icon: Operation, color: '#8e44ad' },
])

const shortcuts = [
  { label: '用户管理', path: 'user', icon: User, color: '#409eff' },
  { label: '角色管理', path: 'role', icon: Avatar, color: '#67c23a' },
  { label: '部门管理', path: 'dept', icon: OfficeBuilding, color: '#e6a23c' },
  { label: '岗位管理', path: 'post', icon: Briefcase, color: '#f56c6c' },
  { label: '菜单管理', path: 'menu', icon: Setting, color: '#909399' },
  { label: '操作日志', path: 'operate-log', icon: Document, color: '#8e44ad' },
]

// 系统健康状态类型
const systemHealthType = computed(() => {
  if (!serverInfo.value) return 'info'
  const statuses = [
    serverInfo.value.cpu.status,
    serverInfo.value.memory.status,
    serverInfo.value.jvm.status
  ]
  if (statuses.includes('DOWN')) return 'danger'
  if (statuses.includes('WARN')) return 'warning'
  return 'success'
})

// 系统健康状态标签
const systemHealthLabel = computed(() => {
  if (!serverInfo.value) return '加载中'
  const type = systemHealthType.value
  if (type === 'danger') return '异常'
  if (type === 'warning') return '警告'
  return '正常'
})

// 登录趋势总数
const totalLoginCount = computed(() => {
  return loginTrend.value.reduce((sum, item) => sum + item.value, 0)
})

// 登录趋势最大值
const maxTrendValue = computed(() => {
  if (loginTrend.value.length === 0) return 1
  return Math.max(...loginTrend.value.map(t => t.value), 1)
})

// 获取进度条状态
const getProgressStatus = (status: string) => {
  if (status === 'DOWN') return 'exception'
  if (status === 'WARN') return 'warning'
  return 'success'
}

// 获取趋势柱状图高度
const getTrendBarHeight = (value: number) => {
  return Math.max(Math.round((value / maxTrendValue.value) * 100), 5)
}

// 格式化日期标签
const formatDateLabel = (date: string) => {
  return date.substring(5) // MM-DD
}

// 加载基础数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getDashboardSummary()
    if (res.code === 200) {
      summary.value = res.data
    }
  } finally {
    loading.value = false
  }
}

// 加载在线用户数
const loadOnlineCount = async () => {
  try {
    const res = await getOnlineUserCount()
    if (res.code === 200) {
      onlineCount.value = res.data
    }
  } catch {
    // 忽略错误
  }
}

// 加载服务器状态
const loadServerInfo = async () => {
  try {
    const res = await getServerMonitorInfo()
    if (res.code === 200) {
      serverInfo.value = res.data
    }
  } catch {
    // 忽略错误
  }
}

// 加载统计数据（趋势 + 活跃用户）
const loadStatistics = async () => {
  try {
    // 最近7天
    const end = new Date()
    const start = new Date()
    start.setTime(start.getTime() - 6 * 24 * 60 * 60 * 1000)
    const formatDate = (d: Date) => d.toISOString().split('T')[0]
    
    const res = await getStatisticsReport({
      startDate: formatDate(start),
      endDate: formatDate(end)
    })
    if (res.code === 200) {
      loginTrend.value = res.data.logStats.loginTrend
      todayActiveUsers.value = res.data.userStats.activeUsersToday
    }
  } catch {
    // 忽略错误
  }
}

// 加载所有数据
const loadAllData = async () => {
  await Promise.all([
    loadData(),
    loadOnlineCount(),
    loadServerInfo(),
    loadStatistics()
  ])
}

// 启动定时刷新
const startAutoRefresh = () => {
  // 每30秒刷新在线用户和服务器状态
  refreshTimer = setInterval(() => {
    loadOnlineCount()
    loadServerInfo()
  }, 30000)
}

// 停止定时刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadAllData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.content-wrapper {
  padding: 16px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 8px;
}

.stat-card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.stat-icon {
  opacity: 0.8;
}

.charts-row {
  margin-bottom: 16px;
}

.health-card,
.trend-card {
  height: 280px;
  border-radius: 8px;
  overflow: hidden;
}

.health-card :deep(.el-card__body) {
  padding: 16px;
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}

.health-card :deep(.el-card__body::-webkit-scrollbar) {
  display: none; /* Chrome Safari */
}

.health-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.health-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.health-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.health-loading {
  padding: 16px 0;
}

.trend-card :deep(.el-card__body) {
  padding: 16px;
  height: calc(100% - 56px);
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE 10+ */
}

.trend-card :deep(.el-card__body::-webkit-scrollbar) {
  display: none; /* Chrome Safari */
}

.trend-chart {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.trend-bars {
  flex: 1;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 0 8px;
}

.trend-bar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.trend-value {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}

.trend-bar {
  width: 100%;
  max-width: 40px;
  background: linear-gradient(180deg, #409eff, #79bbff);
  border-radius: 4px 4px 0 0;
  min-height: 4px;
  transition: height 0.3s ease;
}

.trend-bar:hover {
  opacity: 0.85;
}

.trend-labels {
  display: flex;
  padding: 8px 8px 0;
  gap: 12px;
}

.trend-labels span {
  flex: 1;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.trend-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logs-row {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.shortcuts-card {
  border-radius: 8px;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.shortcut-item:hover {
  background-color: #f5f7fa;
}

.shortcut-label {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
}
</style>
