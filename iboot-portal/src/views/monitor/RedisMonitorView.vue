<template>
  <div class="content-wrapper">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><Connection /></el-icon>
        <span class="toolbar-title">Redis 缓存监控</span>
        <el-tag v-if="redisInfo" type="success" size="small" effect="plain">
          Redis {{ redisInfo.serverInfo.version }}
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          inactive-text=""
          style="margin-right: 12px"
          @change="toggleAutoRefresh"
        />
        <el-tooltip content="刷新" placement="top">
          <el-button :icon="Refresh" circle size="small" @click="loadAll" :loading="loading" />
        </el-tooltip>
      </div>
    </div>

    <div v-loading="loading && !redisInfo">
      <!-- 概览卡片 -->
      <el-row :gutter="16" class="stats-cards" v-if="redisInfo">
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">Redis 版本</div>
            <div class="stat-value">{{ redisInfo.serverInfo.version }}</div>
            <div class="stat-desc">{{ redisInfo.serverInfo.mode }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">运行时长</div>
            <div class="stat-value">{{ redisInfo.serverInfo.uptimeDisplay }}</div>
            <div class="stat-desc">PID: {{ redisInfo.serverInfo.processId }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">连接客户端</div>
            <div class="stat-value">{{ redisInfo.serverInfo.connectedClients }}</div>
            <div class="stat-desc">端口: {{ redisInfo.serverInfo.tcpPort }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">内存使用</div>
            <div class="stat-value memory-value">
              {{ redisInfo.memoryInfo.usedMemoryHuman }}
              <el-tag
                :type="memoryTagType"
                size="small"
                effect="dark"
                style="margin-left: 6px"
              >
                {{ redisInfo.memoryInfo.statusLabel }}
              </el-tag>
            </div>
            <el-progress
              :percentage="memoryPercentage"
              :status="progressStatus"
              :stroke-width="6"
              style="margin-top: 6px"
            />
          </el-card>
        </el-col>
      </el-row>

      <!-- 基本信息 + 内存详情 -->
      <el-row :gutter="16" class="section-row" v-if="redisInfo">
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="primary" size="small" effect="plain" style="margin-right: 6px">
                  {{ dictStore.getDictLabel('sys_redis_info_category', 'server') || '服务器信息' }}
                </el-tag>
                基本信息
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="Redis 版本">{{ redisInfo.serverInfo.version }}</el-descriptions-item>
              <el-descriptions-item label="运行模式">{{ redisInfo.serverInfo.mode }}</el-descriptions-item>
              <el-descriptions-item label="操作系统">{{ redisInfo.serverInfo.os }}</el-descriptions-item>
              <el-descriptions-item label="TCP 端口">{{ redisInfo.serverInfo.tcpPort }}</el-descriptions-item>
              <el-descriptions-item label="进程 ID">{{ redisInfo.serverInfo.processId }}</el-descriptions-item>
              <el-descriptions-item label="运行时长">{{ redisInfo.serverInfo.uptimeDisplay }}</el-descriptions-item>
              <el-descriptions-item label="连接客户端">{{ redisInfo.serverInfo.connectedClients }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="warning" size="small" effect="plain" style="margin-right: 6px">
                  {{ dictStore.getDictLabel('sys_redis_info_category', 'memory') || '内存信息' }}
                </el-tag>
                内存详情
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="已用内存">{{ redisInfo.memoryInfo.usedMemoryHuman }}</el-descriptions-item>
              <el-descriptions-item label="最大内存">{{ redisInfo.memoryInfo.maxMemoryHuman }}</el-descriptions-item>
              <el-descriptions-item label="使用率">
                {{ redisInfo.memoryInfo.maxMemory > 0 ? redisInfo.memoryInfo.memoryUsageRate + '%' : '无限制' }}
              </el-descriptions-item>
              <el-descriptions-item label="内存碎片率">{{ redisInfo.memoryInfo.fragmentationRatio }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="memoryTagType" size="small">{{ redisInfo.memoryInfo.statusLabel }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="警告阈值">{{ redisInfo.memoryInfo.warnThreshold }}%</el-descriptions-item>
              <el-descriptions-item label="异常阈值">{{ redisInfo.memoryInfo.errorThreshold }}%</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <!-- 统计信息 + 键空间 -->
      <el-row :gutter="16" class="section-row" v-if="redisInfo">
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="info" size="small" effect="plain" style="margin-right: 6px">
                  {{ dictStore.getDictLabel('sys_redis_info_category', 'stats') || '统计信息' }}
                </el-tag>
                运行统计
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="每秒操作数 (QPS)">
                <span class="highlight-value">{{ redisInfo.statsInfo.instantaneousOpsPerSec }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="命中率">
                <span class="highlight-value">{{ redisInfo.statsInfo.hitRate }}%</span>
                <span class="stat-detail">
                  (命中 {{ redisInfo.statsInfo.keyspaceHits }} / 未命中 {{ redisInfo.statsInfo.keyspaceMisses }})
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="总命令处理数">{{ formatNumber(redisInfo.statsInfo.totalCommandsProcessed) }}</el-descriptions-item>
              <el-descriptions-item label="总连接数">{{ formatNumber(redisInfo.statsInfo.totalConnectionsReceived) }}</el-descriptions-item>
              <el-descriptions-item label="已过期键数">{{ formatNumber(redisInfo.statsInfo.expiredKeys) }}</el-descriptions-item>
              <el-descriptions-item label="已驱逐键数">{{ formatNumber(redisInfo.statsInfo.evictedKeys) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag type="danger" size="small" effect="plain" style="margin-right: 6px">
                  {{ dictStore.getDictLabel('sys_redis_info_category', 'keyspace') || '键空间信息' }}
                </el-tag>
                键空间信息
              </span>
            </template>
            <el-table :data="redisInfo.keyspaceInfo" size="small" v-if="redisInfo.keyspaceInfo.length > 0">
              <el-table-column prop="dbIndex" label="数据库" width="80">
                <template #default="{ row }">db{{ row.dbIndex }}</template>
              </el-table-column>
              <el-table-column prop="keys" label="键总数" />
              <el-table-column prop="expires" label="设置过期" />
              <el-table-column prop="avgTtl" label="平均TTL(ms)" />
            </el-table>
            <el-empty v-else description="当前无键空间数据" :image-size="60" />
          </el-card>
        </el-col>
      </el-row>

      <!-- 命令统计 Top10 -->
      <el-card shadow="hover" class="section-row" v-if="redisInfo && redisInfo.commandStats.length > 0">
        <template #header>
          <span class="card-title">命令统计 Top10</span>
        </template>
        <el-table :data="redisInfo.commandStats" size="small">
          <el-table-column prop="command" label="命令" width="180">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.command }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="calls" label="调用次数">
            <template #default="{ row }">{{ formatNumber(row.calls) }}</template>
          </el-table-column>
          <el-table-column prop="usecPerCall" label="平均耗时">
            <template #default="{ row }">{{ row.usecPerCall.toFixed(2) }} &micro;s</template>
          </el-table-column>
          <el-table-column prop="usec" label="总耗时">
            <template #default="{ row }">{{ formatNumber(row.usec) }} &micro;s</template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 缓存键管理 -->
      <el-card shadow="hover" class="section-row">
        <template #header>
          <span class="card-title">缓存键管理</span>
        </template>

        <!-- 搜索栏 -->
        <el-form :inline="true" class="search-form">
          <el-form-item label="键名匹配">
            <el-input
              v-model="keyPattern"
              placeholder="输入匹配模式，如 sys_config:*"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearchKeys"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearchKeys">搜索</el-button>
            <el-button @click="handleResetSearch">重置</el-button>
            <el-button
              v-permission="'redis:clear'"
              type="danger"
              @click="handleClearAll"
            >
              清空所有缓存
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 缓存键表格 -->
        <el-table :data="cacheKeys" size="small" v-loading="keysLoading" style="width: 100%">
          <el-table-column prop="key" label="键名" min-width="300" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="keyTypeTag(row.type)" size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ttlDisplay" label="过期时间" width="150" />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button
                v-permission="'redis:query'"
                type="primary"
                link
                size="small"
                @click="handleViewKey(row.key)"
              >
                查看
              </el-button>
              <el-button
                v-permission="'redis:delete'"
                type="danger"
                link
                size="small"
                @click="handleDeleteKey(row.key)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
          class="pagination"
          v-model:current-page="keysPageNum"
          v-model:page-size="keysPageSize"
          :total="keysTotal"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadCacheKeys"
          @current-change="loadCacheKeys"
        />
      </el-card>
    </div>

    <!-- 键值详情对话框 -->
    <el-dialog v-model="keyDetailVisible" title="缓存键详情" width="650px" destroy-on-close>
      <el-descriptions v-if="keyDetail" :column="2" border size="small">
        <el-descriptions-item label="键名" :span="2">{{ keyDetail.key }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="keyTypeTag(keyDetail.type)" size="small">{{ keyDetail.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="TTL">
          {{ keyDetail.ttl === -1 ? '永久' : keyDetail.ttl === -2 ? '已过期' : keyDetail.ttl + '秒' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">键值内容</el-divider>
      <div class="key-value-content">
        <pre>{{ formatKeyValue(keyDetail?.value) }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Refresh } from '@element-plus/icons-vue'
import {
  getRedisMonitorInfo,
  getCacheKeys,
  getCacheKeyValue,
  deleteCacheKey,
  clearAllCacheKeys,
  getConfigByKey,
  type RedisMonitorInfo,
  type CacheKeyInfo,
  type CacheKeyDetail
} from '@/api/system'
import { useDictStore } from '@/stores/dict'

const dictStore = useDictStore()

// ==================== 监控数据 ====================
const loading = ref(false)
const redisInfo = ref<RedisMonitorInfo | null>(null)

// ==================== 缓存键管理 ====================
const keysLoading = ref(false)
const cacheKeys = ref<CacheKeyInfo[]>([])
const keysTotal = ref(0)
const keysPageNum = ref(1)
const keysPageSize = ref(20)
const keyPattern = ref('*')

// ==================== 键值详情 ====================
const keyDetailVisible = ref(false)
const keyDetail = ref<CacheKeyDetail | null>(null)

// ==================== 自动刷新 ====================
const autoRefresh = ref(false)
const refreshInterval = ref(30)
let refreshTimer: ReturnType<typeof setInterval> | null = null

// ==================== 计算属性 ====================
const memoryPercentage = computed(() => {
  if (!redisInfo.value || redisInfo.value.memoryInfo.maxMemory <= 0) return 0
  return Math.min(Math.round(redisInfo.value.memoryInfo.memoryUsageRate), 100)
})

const progressStatus = computed(() => {
  if (!redisInfo.value) return ''
  const status = redisInfo.value.memoryInfo.status
  if (status === 'DOWN') return 'exception'
  if (status === 'WARN') return 'warning'
  return 'success'
})

const memoryTagType = computed(() => {
  if (!redisInfo.value) return 'info'
  const status = redisInfo.value.memoryInfo.status
  if (status === 'DOWN') return 'danger'
  if (status === 'WARN') return 'warning'
  return 'success'
})

// ==================== 数据加载 ====================
const loadRedisInfo = async () => {
  loading.value = true
  try {
    const res = await getRedisMonitorInfo()
    if (res.code === 200) {
      redisInfo.value = res.data
    }
  } catch (e) {
    console.error('加载Redis监控数据失败', e)
  } finally {
    loading.value = false
  }
}

const loadCacheKeys = async () => {
  keysLoading.value = true
  try {
    const res = await getCacheKeys({
      pattern: keyPattern.value || '*',
      pageNum: keysPageNum.value,
      pageSize: keysPageSize.value
    })
    if (res.code === 200) {
      cacheKeys.value = res.data.data
      keysTotal.value = res.data.total
    }
  } catch (e) {
    console.error('加载缓存键失败', e)
  } finally {
    keysLoading.value = false
  }
}

const loadAll = () => {
  loadRedisInfo()
  loadCacheKeys()
}

const loadRefreshConfig = async () => {
  try {
    const res = await getConfigByKey('monitor.redis.refreshInterval')
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

// ==================== 缓存键操作 ====================
const handleSearchKeys = () => {
  keysPageNum.value = 1
  loadCacheKeys()
}

const handleResetSearch = () => {
  keyPattern.value = '*'
  handleSearchKeys()
}

const handleViewKey = async (key: string) => {
  try {
    const res = await getCacheKeyValue(key)
    if (res.code === 200) {
      keyDetail.value = res.data
      keyDetailVisible.value = true
    } else {
      ElMessage.error(res.message || '获取键值失败')
    }
  } catch {
    ElMessage.error('获取键值失败')
  }
}

const handleDeleteKey = (key: string) => {
  ElMessageBox.confirm(
    `确认要删除缓存键 "${key}" 吗？`,
    '删除确认',
    { type: 'warning' }
  )
    .then(async () => {
      const res = await deleteCacheKey(key)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadCacheKeys()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    })
    .catch(() => {})
}

const handleClearAll = () => {
  ElMessageBox.confirm(
    '此操作将清空当前数据库中所有缓存数据，该操作不可恢复！确认继续？',
    '危险操作',
    { type: 'error', confirmButtonText: '确认清空', cancelButtonText: '取消' }
  )
    .then(async () => {
      const res = await clearAllCacheKeys()
      if (res.code === 200) {
        ElMessage.success('缓存已清空')
        loadAll()
      } else {
        ElMessage.error(res.message || '清空失败')
      }
    })
    .catch(() => {})
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
      loadAll()
    }, refreshInterval.value * 1000)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// ==================== 工具函数 ====================
const formatNumber = (num: number) => {
  if (num == null) return '0'
  return num.toLocaleString()
}

const formatKeyValue = (value: unknown): string => {
  if (value === null || value === undefined) return ''
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

const keyTypeTag = (type: string): string => {
  const map: Record<string, string> = {
    string: 'success',
    hash: 'warning',
    list: 'primary',
    set: 'info',
    zset: 'danger'
  }
  return map[type] || 'info'
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await dictStore.loadDicts('sys_redis_info_category', 'sys_monitor_status')
  await loadRefreshConfig()
  loadAll()
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
}

.memory-value {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.stat-desc {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
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

.highlight-value {
  font-weight: 700;
  color: #409eff;
  font-size: 15px;
}

.stat-detail {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}

.search-form {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.key-value-content {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  max-height: 400px;
  overflow: auto;
}

.key-value-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  color: #606266;
  font-family: 'Courier New', Courier, monospace;
}
</style>
