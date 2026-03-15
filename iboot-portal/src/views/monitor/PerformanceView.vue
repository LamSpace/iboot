<template>
  <div class="content-wrapper">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><TrendCharts /></el-icon>
        <span class="toolbar-title">系统性能监控</span>
        <el-tag v-if="serverInfo" type="info" size="small" effect="plain">
          {{ serverInfo.os.hostName }}
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-button
          v-if="serverInfo?.sbaUrl"
          type="primary"
          link
          size="small"
          @click="openSba"
        >
          打开服务监控控制台
          <el-icon style="margin-left: 4px"><TopRight /></el-icon>
        </el-button>
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          inactive-text=""
          style="margin-right: 12px"
          @change="toggleAutoRefresh"
        />
        <el-tooltip content="刷新" placement="top">
          <el-button :icon="Refresh" circle size="small" @click="loadData" :loading="loading" />
        </el-tooltip>
      </div>
    </div>

    <div v-loading="loading && !serverInfo">
      <!-- 1. 概览卡片区 -->
      <el-row :gutter="16" class="stats-cards" v-if="serverInfo">
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">CPU 使用率</div>
            <div class="stat-value">
              {{ serverInfo.cpu.totalUsage.toFixed(1) }}%
              <el-tag
                :type="statusTagType(serverInfo.cpu.status)"
                size="small"
                effect="dark"
                style="margin-left: 6px"
              >
                {{ dictStore.getDictLabel('sys_monitor_status', serverInfo.cpu.status) }}
              </el-tag>
            </div>
            <el-progress
              :percentage="Math.min(Math.round(serverInfo.cpu.totalUsage), 100)"
              :status="progressStatus(serverInfo.cpu.status)"
              :stroke-width="6"
              style="margin-top: 6px"
            />
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">物理内存</div>
            <div class="stat-value">
              {{ serverInfo.memory.usageRate.toFixed(1) }}%
              <el-tag
                :type="statusTagType(serverInfo.memory.status)"
                size="small"
                effect="dark"
                style="margin-left: 6px"
              >
                {{ dictStore.getDictLabel('sys_monitor_status', serverInfo.memory.status) }}
              </el-tag>
            </div>
            <el-progress
              :percentage="Math.min(Math.round(serverInfo.memory.usageRate), 100)"
              :status="progressStatus(serverInfo.memory.status)"
              :stroke-width="6"
              style="margin-top: 6px"
            />
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">JVM 内存</div>
            <div class="stat-value">
              {{ serverInfo.jvm.usageRate.toFixed(1) }}%
              <el-tag
                :type="statusTagType(serverInfo.jvm.status)"
                size="small"
                effect="dark"
                style="margin-left: 6px"
              >
                {{ dictStore.getDictLabel('sys_monitor_status', serverInfo.jvm.status) }}
              </el-tag>
            </div>
            <el-progress
              :percentage="Math.min(Math.round(serverInfo.jvm.usageRate), 100)"
              :status="progressStatus(serverInfo.jvm.status)"
              :stroke-width="6"
              style="margin-top: 6px"
            />
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">磁盘 (最高)</div>
            <div class="stat-value" v-if="maxDisk">
              {{ maxDisk.usageRate.toFixed(1) }}%
              <el-tag
                :type="statusTagType(maxDisk.status)"
                size="small"
                effect="dark"
                style="margin-left: 6px"
              >
                {{ dictStore.getDictLabel('sys_monitor_status', maxDisk.status) }}
              </el-tag>
            </div>
            <div class="stat-value" v-else>--</div>
            <el-progress
              v-if="maxDisk"
              :percentage="Math.min(Math.round(maxDisk.usageRate), 100)"
              :status="progressStatus(maxDisk.status)"
              :stroke-width="6"
              style="margin-top: 6px"
            />
          </el-card>
        </el-col>
      </el-row>

      <!-- 2. 详细信息区 -->
      <el-row :gutter="16" class="section-row" v-if="serverInfo">
        <!-- CPU 详情 -->
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="primary" size="small" effect="plain" style="margin-right: 6px">CPU</el-tag>
                处理器信息
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="CPU 型号">{{ serverInfo.cpu.model }}</el-descriptions-item>
              <el-descriptions-item label="核心数">{{ serverInfo.cpu.coreCount }}</el-descriptions-item>
              <el-descriptions-item label="系统使用率">{{ serverInfo.cpu.systemUsage.toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="用户使用率">{{ serverInfo.cpu.userUsage.toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="总使用率">{{ serverInfo.cpu.totalUsage.toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="空闲率">{{ serverInfo.cpu.idle.toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusTagType(serverInfo.cpu.status)" size="small">
                  {{ dictStore.getDictLabel('sys_monitor_status', serverInfo.cpu.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <!-- 物理内存详情 -->
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="warning" size="small" effect="plain" style="margin-right: 6px">RAM</el-tag>
                物理内存
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="总内存">{{ serverInfo.memory.totalDisplay }}</el-descriptions-item>
              <el-descriptions-item label="已使用">{{ serverInfo.memory.usedDisplay }}</el-descriptions-item>
              <el-descriptions-item label="空闲">{{ serverInfo.memory.freeDisplay }}</el-descriptions-item>
              <el-descriptions-item label="使用率">{{ serverInfo.memory.usageRate.toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusTagType(serverInfo.memory.status)" size="small">
                  {{ dictStore.getDictLabel('sys_monitor_status', serverInfo.memory.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="section-row" v-if="serverInfo">
        <!-- JVM 详情 -->
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="success" size="small" effect="plain" style="margin-right: 6px">JVM</el-tag>
                Java 虚拟机
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="最大内存">{{ serverInfo.jvm.maxMemoryDisplay }}</el-descriptions-item>
              <el-descriptions-item label="已分配内存">{{ serverInfo.jvm.totalMemoryDisplay }}</el-descriptions-item>
              <el-descriptions-item label="已使用内存">{{ serverInfo.jvm.usedMemoryDisplay }}</el-descriptions-item>
              <el-descriptions-item label="空闲内存">{{ serverInfo.jvm.freeMemoryDisplay }}</el-descriptions-item>
              <el-descriptions-item label="使用率">{{ serverInfo.jvm.usageRate.toFixed(1) }}%</el-descriptions-item>
              <el-descriptions-item label="Java 版本">{{ serverInfo.jvm.javaVersion }}</el-descriptions-item>
              <el-descriptions-item label="JVM 名称">{{ serverInfo.jvm.jvmName }}</el-descriptions-item>
              <el-descriptions-item label="启动时间">{{ serverInfo.jvm.startTime }}</el-descriptions-item>
              <el-descriptions-item label="运行时长">{{ serverInfo.jvm.runTime }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusTagType(serverInfo.jvm.status)" size="small">
                  {{ dictStore.getDictLabel('sys_monitor_status', serverInfo.jvm.status) }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <!-- 操作系统信息 -->
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="info" size="small" effect="plain" style="margin-right: 6px">OS</el-tag>
                操作系统
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="操作系统">{{ serverInfo.os.name }}</el-descriptions-item>
              <el-descriptions-item label="系统架构">{{ serverInfo.os.arch }}</el-descriptions-item>
              <el-descriptions-item label="系统版本">{{ serverInfo.os.version }}</el-descriptions-item>
              <el-descriptions-item label="主机名">{{ serverInfo.os.hostName }}</el-descriptions-item>
              <el-descriptions-item label="运行时长">{{ serverInfo.os.uptime }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <!-- 3. 磁盘列表区 -->
      <el-card shadow="hover" class="section-row" v-if="serverInfo">
        <template #header>
          <span class="card-title">
            <el-tag type="danger" size="small" effect="plain" style="margin-right: 6px">DISK</el-tag>
            磁盘信息
          </span>
        </template>
        <el-table :data="serverInfo.disks" size="small" stripe>
          <el-table-column prop="mountPoint" label="挂载点" min-width="120" show-overflow-tooltip />
          <el-table-column prop="fsType" label="文件系统" width="100" />
          <el-table-column prop="totalDisplay" label="总大小" width="100" />
          <el-table-column prop="usedDisplay" label="已使用" width="100" />
          <el-table-column prop="freeDisplay" label="可用" width="100" />
          <el-table-column label="使用率" width="200">
            <template #default="{ row }">
              <el-progress
                :percentage="Math.min(Math.round(row.usageRate), 100)"
                :status="progressStatus(row.status)"
                :stroke-width="14"
                :text-inside="true"
              />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ dictStore.getDictLabel('sys_monitor_status', row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 4. 服务健康检查区 -->
      <el-card shadow="hover" class="section-row" v-if="serverInfo && serverInfo.serviceChecks.length > 0">
        <template #header>
          <span class="card-title">
            <el-tag type="success" size="small" effect="plain" style="margin-right: 6px">HEALTH</el-tag>
            服务健康检查
          </span>
        </template>
        <el-table :data="serverInfo.serviceChecks" size="small" stripe>
          <el-table-column prop="name" label="检查项" min-width="150" />
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">
                {{ row.statusLabel || dictStore.getDictLabel('sys_monitor_status', row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="响应时间" width="120" align="center">
            <template #default="{ row }">
              <span :class="{ 'slow-response': row.responseTime > 1000 }">
                {{ row.responseTime }} ms
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详细信息" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-card>

      <!-- 环境提示 -->
      <div class="env-hint" v-if="serverInfo">
        在容器 / Kubernetes 环境下，本页面展示的是当前应用实例可见的系统资源，不代表整个宿主机或整个集群。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { TrendCharts, Refresh, TopRight } from '@element-plus/icons-vue'
import {
  getServerMonitorInfo,
  getConfigByKey,
  type ServerInfoResponse
} from '@/api/system'
import { useDictStore } from '@/stores/dict'

const dictStore = useDictStore()

// ==================== 数据 ====================
const loading = ref(false)
const serverInfo = ref<ServerInfoResponse | null>(null)

// ==================== 自动刷新 ====================
const autoRefresh = ref(false)
const refreshInterval = ref(30)
let refreshTimer: ReturnType<typeof setInterval> | null = null

// ==================== 计算属性 ====================
const maxDisk = computed(() => {
  if (!serverInfo.value || !serverInfo.value.disks.length) return null
  return serverInfo.value.disks.reduce((max, disk) =>
    disk.usageRate > max.usageRate ? disk : max
  )
})

// ==================== 工具函数 ====================
const statusTagType = (status: string): string => {
  if (status === 'DOWN') return 'danger'
  if (status === 'WARN') return 'warning'
  return 'success'
}

const progressStatus = (status: string): string => {
  if (status === 'DOWN') return 'exception'
  if (status === 'WARN') return 'warning'
  return 'success'
}

const openSba = () => {
  if (serverInfo.value?.sbaUrl) {
    window.open(serverInfo.value.sbaUrl, '_blank')
  }
}

// ==================== 数据加载 ====================
const loadData = async () => {
  loading.value = true
  try {
    const res = await getServerMonitorInfo()
    if (res.code === 200) {
      serverInfo.value = res.data
    }
  } catch (e) {
    console.error('加载服务器监控数据失败', e)
  } finally {
    loading.value = false
  }
}

const loadRefreshConfig = async () => {
  try {
    const res = await getConfigByKey('monitor.performance.refreshInterval')
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

// ==================== 自动刷新 ====================
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

// ==================== 生命周期 ====================
onMounted(async () => {
  await dictStore.loadDicts('sys_monitor_status')
  await loadRefreshConfig()
  loadData()
})

onUnmounted(() => {
  stopAutoRefresh()
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
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: center;
}

.section-row {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
}

.slow-response {
  color: #e6a23c;
  font-weight: 600;
}

.env-hint {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  padding: 8px 0 4px;
}
</style>
