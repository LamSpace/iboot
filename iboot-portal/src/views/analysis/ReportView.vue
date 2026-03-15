<template>
  <div class="content-wrapper">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><TrendCharts /></el-icon>
        <span class="toolbar-title">数据统计报表</span>
      </div>
      <div class="toolbar-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          style="width: 260px"
          @change="loadData"
        />
        <el-button :icon="Refresh" circle size="small" @click="loadData" :loading="loading" style="margin-left: 12px" />
      </div>
    </div>

    <div v-loading="loading">
      <!-- 系统概览卡片 -->
      <el-row :gutter="16" class="stats-cards" v-if="reportData">
        <el-col :xs="12" :sm="8" :md="4" v-for="item in overviewCards" :key="item.label">
          <el-card shadow="hover" class="stat-card" :style="{ borderTop: `3px solid ${item.color}` }">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 报表类型切换 -->
      <el-tabs v-model="activeTab" class="report-tabs">
        <!-- 用户统计 -->
        <el-tab-pane label="用户统计" name="user">
          <el-row :gutter="16" v-if="reportData">
            <el-col :span="8">
              <el-card shadow="hover">
                <template #header><span class="card-title">用户概览</span></template>
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="用户总数">
                    <span class="stat-num">{{ reportData.userStats.totalUsers }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="今日活跃用户">
                    <span class="stat-num highlight">{{ reportData.userStats.activeUsersToday }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="今日新增用户">
                    <span class="stat-num">{{ reportData.userStats.newUsersToday }}</span>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
            <el-col :span="16">
              <el-card shadow="hover">
                <template #header><span class="card-title">用户活跃度说明</span></template>
                <el-alert type="info" :closable="false" show-icon>
                  <p>活跃用户：指在选定时间范围内成功登录系统的独立用户数。</p>
                  <p>部门分布：展示各部门的用户数量占比（暂未启用）。</p>
                </el-alert>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 日志统计 -->
        <el-tab-pane label="日志统计" name="log">
          <el-row :gutter="16" v-if="reportData">
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header><span class="card-title">登录日志统计</span></template>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="总登录次数">
                    <span class="stat-num">{{ reportData.logStats.loginLogCount }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="成功率">
                    <el-tag type="success" size="small">
                      {{ loginSuccessRate }}%
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="成功次数">
                    <span class="stat-num success">{{ reportData.logStats.loginSuccessCount }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="失败次数">
                    <span class="stat-num danger">{{ reportData.logStats.loginFailCount }}</span>
                  </el-descriptions-item>
                </el-descriptions>
                <div class="chart-placeholder" v-if="reportData.logStats.loginTrend.length > 0">
                  <div class="trend-title">登录趋势 (近期)</div>
                  <div class="trend-chart">
                    <div 
                      v-for="item in reportData.logStats.loginTrend" 
                      :key="item.date" 
                      class="trend-bar"
                      :style="{ height: getTrendBarHeight(item.value, maxLoginTrend) + '%' }"
                      :title="`${item.date}: ${item.value}次`"
                    ></div>
                  </div>
                  <div class="trend-labels">
                    <span v-for="item in reportData.logStats.loginTrend" :key="item.date">
                      {{ formatDateLabel(item.date) }}
                    </span>
                  </div>
                </div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover">
                <template #header><span class="card-title">操作日志统计</span></template>
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="总操作次数">
                    <span class="stat-num">{{ reportData.logStats.operateLogCount }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="成功率">
                    <el-tag type="success" size="small">
                      {{ operateSuccessRate }}%
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="成功次数">
                    <span class="stat-num success">{{ reportData.logStats.operateSuccessCount }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="失败次数">
                    <span class="stat-num danger">{{ reportData.logStats.operateFailCount }}</span>
                  </el-descriptions-item>
                </el-descriptions>
                <div class="chart-placeholder" v-if="reportData.logStats.operateTrend.length > 0">
                  <div class="trend-title">操作趋势 (近期)</div>
                  <div class="trend-chart">
                    <div 
                      v-for="item in reportData.logStats.operateTrend" 
                      :key="item.date" 
                      class="trend-bar operate"
                      :style="{ height: getTrendBarHeight(item.value, maxOperateTrend) + '%' }"
                      :title="`${item.date}: ${item.value}次`"
                    ></div>
                  </div>
                  <div class="trend-labels">
                    <span v-for="item in reportData.logStats.operateTrend" :key="item.date">
                      {{ formatDateLabel(item.date) }}
                    </span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-row :gutter="16" style="margin-top: 16px" v-if="reportData">
            <el-col :span="24">
              <el-card shadow="hover">
                <template #header><span class="card-title">模块操作统计</span></template>
                <el-table :data="reportData.logStats.moduleStats" size="small" stripe max-height="300">
                  <el-table-column prop="name" label="模块名称" min-width="150" />
                  <el-table-column prop="value" label="操作次数" width="120" align="right" />
                  <el-table-column label="占比" width="200">
                    <template #default="{ row }">
                      <el-progress 
                        :percentage="getModulePercent(row.value)" 
                        :stroke-width="12"
                        :text-inside="true"
                      />
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>

        <!-- 系统概览 -->
        <el-tab-pane label="系统概览" name="overview">
          <el-row :gutter="16" v-if="reportData">
            <el-col :span="24">
              <el-card shadow="hover">
                <template #header><span class="card-title">系统资源统计</span></template>
                <el-table :data="overviewTableData" size="small" stripe>
                  <el-table-column prop="category" label="资源类别" width="150" />
                  <el-table-column prop="count" label="数量" width="120" align="right">
                    <template #default="{ row }">
                      <span class="stat-num">{{ row.count }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="说明" />
                </el-table>
              </el-card>
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { TrendCharts, Refresh } from '@element-plus/icons-vue'
import { getStatisticsReport, type ReportResponse } from '@/api/system'

const loading = ref(false)
const reportData = ref<ReportResponse | null>(null)
const activeTab = ref('log')

// 日期范围
const dateRange = ref<[string, string] | null>(null)

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 6 * 24 * 60 * 60 * 1000)
      return [start, end]
    }
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 29 * 24 * 60 * 60 * 1000)
      return [start, end]
    }
  },
  {
    text: '本月',
    value: () => {
      const end = new Date()
      const start = new Date(end.getFullYear(), end.getMonth(), 1)
      return [start, end]
    }
  }
]

// 概览卡片
const overviewCards = computed(() => {
  if (!reportData.value) return []
  const { overviewStats } = reportData.value
  return [
    { label: '用户数', value: overviewStats.userCount, color: '#409eff' },
    { label: '角色数', value: overviewStats.roleCount, color: '#67c23a' },
    { label: '部门数', value: overviewStats.deptCount, color: '#e6a23c' },
    { label: '岗位数', value: overviewStats.postCount, color: '#f56c6c' },
    { label: '菜单数', value: overviewStats.menuCount, color: '#909399' },
    { label: '配置数', value: overviewStats.configCount, color: '#8e44ad' }
  ]
})

// 概览表格数据
const overviewTableData = computed(() => {
  if (!reportData.value) return []
  const { overviewStats } = reportData.value
  return [
    { category: '用户管理', count: overviewStats.userCount, description: '系统注册用户总数' },
    { category: '角色管理', count: overviewStats.roleCount, description: '权限角色总数' },
    { category: '部门管理', count: overviewStats.deptCount, description: '组织架构部门总数' },
    { category: '岗位管理', count: overviewStats.postCount, description: '岗位设置总数' },
    { category: '菜单管理', count: overviewStats.menuCount, description: '系统菜单和权限总数' },
    { category: '系统配置', count: overviewStats.configCount, description: '系统参数配置总数' }
  ]
})

// 登录成功率
const loginSuccessRate = computed(() => {
  if (!reportData.value) return 0
  const { loginSuccessCount, loginFailCount } = reportData.value.logStats
  const total = loginSuccessCount + loginFailCount
  if (total === 0) return 0
  return ((loginSuccessCount / total) * 100).toFixed(1)
})

// 操作成功率
const operateSuccessRate = computed(() => {
  if (!reportData.value) return 0
  const { operateSuccessCount, operateFailCount } = reportData.value.logStats
  const total = operateSuccessCount + operateFailCount
  if (total === 0) return 0
  return ((operateSuccessCount / total) * 100).toFixed(1)
})

// 登录趋势最大值
const maxLoginTrend = computed(() => {
  if (!reportData.value) return 1
  const values = reportData.value.logStats.loginTrend.map(t => t.value)
  return Math.max(...values, 1)
})

// 操作趋势最大值
const maxOperateTrend = computed(() => {
  if (!reportData.value) return 1
  const values = reportData.value.logStats.operateTrend.map(t => t.value)
  return Math.max(...values, 1)
})

// 模块操作总数
const totalModuleOps = computed(() => {
  if (!reportData.value) return 1
  return reportData.value.logStats.moduleStats.reduce((sum, m) => sum + m.value, 0) || 1
})

// 获取趋势柱状图高度
const getTrendBarHeight = (value: number, max: number) => {
  return Math.round((value / max) * 100)
}

// 获取模块占比
const getModulePercent = (value: number) => {
  return Math.round((value / totalModuleOps.value) * 100)
}

// 格式化日期标签
const formatDateLabel = (date: string) => {
  return date.substring(5) // MM-DD
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const params: { startDate?: string; endDate?: string } = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getStatisticsReport(params)
    if (res.code === 200) {
      reportData.value = res.data
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 默认最近7天
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 6 * 24 * 60 * 60 * 1000)
  const formatDate = (d: Date) => d.toISOString().split('T')[0]
  dateRange.value = [formatDate(start), formatDate(end)]
  loadData()
})
</script>

<style scoped>
.content-wrapper {
  padding: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 10px 16px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.stats-cards {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-card :deep(.el-card__body) {
  padding: 16px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.report-tabs {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.card-title {
  font-weight: 600;
  font-size: 14px;
}

.stat-num {
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}

.stat-num.highlight {
  color: #409eff;
}

.stat-num.success {
  color: #67c23a;
}

.stat-num.danger {
  color: #f56c6c;
}

.chart-placeholder {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.trend-title {
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  font-weight: 500;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  height: 80px;
  gap: 4px;
}

.trend-bar {
  flex: 1;
  background: linear-gradient(180deg, #409eff, #79bbff);
  border-radius: 3px 3px 0 0;
  min-height: 4px;
  transition: height 0.3s ease;
}

.trend-bar.operate {
  background: linear-gradient(180deg, #67c23a, #95d475);
}

.trend-bar:hover {
  opacity: 0.8;
}

.trend-labels {
  display: flex;
  margin-top: 6px;
  font-size: 10px;
  color: #909399;
}

.trend-labels span {
  flex: 1;
  text-align: center;
}
</style>
