<template>
  <div class="content-wrapper">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><PieChart /></el-icon>
        <span class="toolbar-title">系统使用分析</span>
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
      <!-- 活跃度概览卡片 -->
      <el-row :gutter="16" class="stats-cards" v-if="usageData">
        <el-col :xs="12" :sm="8">
          <el-card shadow="hover" class="stat-card" style="border-top: 3px solid #409eff">
            <div class="stat-label">今日活跃用户</div>
            <div class="stat-value" style="color: #409eff">{{ usageData.activityStats.todayActiveUsers }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-card shadow="hover" class="stat-card" style="border-top: 3px solid #67c23a">
            <div class="stat-label">本周活跃用户</div>
            <div class="stat-value" style="color: #67c23a">{{ usageData.activityStats.weekActiveUsers }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8">
          <el-card shadow="hover" class="stat-card" style="border-top: 3px solid #e6a23c">
            <div class="stat-label">本月活跃用户</div>
            <div class="stat-value" style="color: #e6a23c">{{ usageData.activityStats.monthActiveUsers }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分析内容 -->
      <el-row :gutter="16" v-if="usageData">
        <!-- 左侧：活跃用户排行 -->
        <el-col :span="12">
          <el-card shadow="hover" class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">活跃用户排行</span>
                <el-tag type="info" size="small">TOP 10</el-tag>
              </div>
            </template>
            <el-table 
              :data="usageData.activityStats.topActiveUsers" 
              size="small" 
              stripe 
              max-height="320"
            >
              <el-table-column label="排名" width="60" align="center">
                <template #default="{ $index }">
                  <el-tag 
                    :type="$index < 3 ? 'warning' : 'info'" 
                    size="small"
                    effect="dark"
                  >
                    {{ $index + 1 }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="username" label="用户名" min-width="120" />
              <el-table-column prop="count" label="操作次数" width="100" align="right">
                <template #default="{ row }">
                  <span class="stat-num">{{ row.count }}</span>
                </template>
              </el-table-column>
              <el-table-column label="活跃度" width="150">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="getActivePercent(row.count)" 
                    :stroke-width="10"
                    :show-text="false"
                  />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="usageData.activityStats.topActiveUsers.length === 0" description="暂无数据" />
          </el-card>
        </el-col>

        <!-- 右侧：功能模块排行 -->
        <el-col :span="12">
          <el-card shadow="hover" class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">功能模块使用排行</span>
                <el-tag type="success" size="small">使用频率</el-tag>
              </div>
            </template>
            <el-table 
              :data="usageData.featureUsageStats.moduleRanking" 
              size="small" 
              stripe 
              max-height="320"
            >
              <el-table-column label="排名" width="60" align="center">
                <template #default="{ $index }">
                  <el-tag 
                    :type="$index < 3 ? 'success' : 'info'" 
                    size="small"
                    effect="dark"
                  >
                    {{ $index + 1 }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="模块名称" min-width="120" />
              <el-table-column prop="value" label="使用次数" width="100" align="right">
                <template #default="{ row }">
                  <span class="stat-num">{{ row.value }}</span>
                </template>
              </el-table-column>
              <el-table-column label="占比" width="150">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="getModulePercent(row.value)" 
                    :stroke-width="10"
                    status="success"
                    :show-text="false"
                  />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="usageData.featureUsageStats.moduleRanking.length === 0" description="暂无数据" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 时段分析 -->
      <el-row :gutter="16" style="margin-top: 16px" v-if="usageData">
        <el-col :span="24">
          <el-card shadow="hover" class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">24小时访问分布</span>
                <el-tag type="warning" size="small">
                  高峰时段: {{ usageData.timeDistributionStats.peakHours }}
                </el-tag>
              </div>
            </template>
            <div class="hour-chart">
              <div class="hour-bars">
                <div 
                  v-for="item in usageData.timeDistributionStats.hourlyDistribution" 
                  :key="item.hour"
                  class="hour-bar-wrapper"
                >
                  <div 
                    class="hour-bar"
                    :style="{ height: getHourBarHeight(item.value) + '%' }"
                    :class="{ 'peak': isPeakHour(item.hour) }"
                    :title="`${item.hour}:00 - ${item.value}次访问`"
                  ></div>
                </div>
              </div>
              <div class="hour-labels">
                <span v-for="h in 24" :key="h - 1">{{ h - 1 }}</span>
              </div>
              <div class="hour-axis-label">时段 (小时)</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 活跃趋势 -->
      <el-row :gutter="16" style="margin-top: 16px" v-if="usageData">
        <el-col :span="24">
          <el-card shadow="hover" class="analysis-card">
            <template #header>
              <span class="card-title">用户活跃趋势</span>
            </template>
            <div class="trend-container" v-if="usageData.activityStats.activeUserTrend.length > 0">
              <div class="trend-chart-large">
                <div 
                  v-for="item in usageData.activityStats.activeUserTrend" 
                  :key="item.date" 
                  class="trend-bar-large"
                  :style="{ height: getTrendBarHeight(item.value) + '%' }"
                  :title="`${item.date}: ${item.value}次登录`"
                >
                  <span class="trend-value">{{ item.value }}</span>
                </div>
              </div>
              <div class="trend-labels-large">
                <span v-for="item in usageData.activityStats.activeUserTrend" :key="item.date">
                  {{ formatDateLabel(item.date) }}
                </span>
              </div>
            </div>
            <el-empty v-else description="暂无趋势数据" />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { PieChart, Refresh } from '@element-plus/icons-vue'
import { getUsageAnalysis, type UsageResponse } from '@/api/system'

const loading = ref(false)
const usageData = ref<UsageResponse | null>(null)

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

// 最大活跃度
const maxActiveCount = computed(() => {
  if (!usageData.value) return 1
  const counts = usageData.value.activityStats.topActiveUsers.map(u => u.count)
  return Math.max(...counts, 1)
})

// 模块使用总数
const totalModuleUsage = computed(() => {
  if (!usageData.value) return 1
  return usageData.value.featureUsageStats.moduleRanking.reduce((sum, m) => sum + m.value, 0) || 1
})

// 小时分布最大值
const maxHourValue = computed(() => {
  if (!usageData.value) return 1
  const values = usageData.value.timeDistributionStats.hourlyDistribution.map(h => h.value)
  return Math.max(...values, 1)
})

// 活跃趋势最大值
const maxTrendValue = computed(() => {
  if (!usageData.value) return 1
  const values = usageData.value.activityStats.activeUserTrend.map(t => t.value)
  return Math.max(...values, 1)
})

// 高峰时段数组
const peakHours = computed(() => {
  if (!usageData.value) return []
  const peakStr = usageData.value.timeDistributionStats.peakHours
  return peakStr.split(',').map(s => parseInt(s.trim().split(':')[0]))
})

// 获取活跃度百分比
const getActivePercent = (count: number) => {
  return Math.round((count / maxActiveCount.value) * 100)
}

// 获取模块占比
const getModulePercent = (value: number) => {
  return Math.round((value / totalModuleUsage.value) * 100)
}

// 获取小时柱状图高度
const getHourBarHeight = (value: number) => {
  return Math.round((value / maxHourValue.value) * 100)
}

// 获取趋势柱状图高度
const getTrendBarHeight = (value: number) => {
  return Math.max(Math.round((value / maxTrendValue.value) * 100), 5)
}

// 判断是否高峰时段
const isPeakHour = (hour: number) => {
  return peakHours.value.includes(hour)
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
    const res = await getUsageAnalysis(params)
    if (res.code === 200) {
      usageData.value = res.data
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 默认最近30天
  const end = new Date()
  const start = new Date()
  start.setTime(start.getTime() - 29 * 24 * 60 * 60 * 1000)
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
  font-size: 28px;
  font-weight: 700;
}

.analysis-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: 600;
  font-size: 14px;
}

.stat-num {
  font-weight: 600;
  color: #303133;
}

/* 24小时分布图 */
.hour-chart {
  padding: 16px 0;
}

.hour-bars {
  display: flex;
  align-items: flex-end;
  height: 120px;
  gap: 2px;
  padding: 0 4px;
}

.hour-bar-wrapper {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: flex-end;
}

.hour-bar {
  width: 100%;
  background: linear-gradient(180deg, #79bbff, #409eff);
  border-radius: 3px 3px 0 0;
  min-height: 4px;
  transition: all 0.3s ease;
}

.hour-bar.peak {
  background: linear-gradient(180deg, #f89898, #f56c6c);
}

.hour-bar:hover {
  opacity: 0.8;
  transform: scaleY(1.05);
}

.hour-labels {
  display: flex;
  padding: 6px 4px 0;
  font-size: 10px;
  color: #909399;
}

.hour-labels span {
  flex: 1;
  text-align: center;
}

.hour-axis-label {
  text-align: center;
  font-size: 12px;
  color: #606266;
  margin-top: 8px;
}

/* 趋势图 */
.trend-container {
  padding: 16px 0;
}

.trend-chart-large {
  display: flex;
  align-items: flex-end;
  height: 150px;
  gap: 8px;
  padding: 0 8px;
}

.trend-bar-large {
  flex: 1;
  background: linear-gradient(180deg, #95d475, #67c23a);
  border-radius: 4px 4px 0 0;
  min-height: 20px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  transition: all 0.3s ease;
  position: relative;
}

.trend-bar-large:hover {
  opacity: 0.85;
}

.trend-value {
  position: absolute;
  top: -20px;
  font-size: 11px;
  font-weight: 600;
  color: #67c23a;
}

.trend-labels-large {
  display: flex;
  padding: 8px;
  font-size: 11px;
  color: #606266;
}

.trend-labels-large span {
  flex: 1;
  text-align: center;
}
</style>
