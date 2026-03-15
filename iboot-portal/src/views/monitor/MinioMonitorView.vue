<template>
  <div class="minio-monitor-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><Box /></el-icon>
        <span class="toolbar-title">MinIO 对象存储</span>
        <el-tag :type="statusTagType" size="small" effect="plain">{{ statusText }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip content="在新窗口中打开MinIO控制台" placement="top">
          <el-button :icon="TopRight" circle size="small" @click="openConsole" />
        </el-tooltip>
        <el-tooltip content="刷新监控数据" placement="top">
          <el-button :icon="Refresh" circle size="small" @click="loadData" :loading="loading" />
        </el-tooltip>
      </div>
    </div>

    <!-- MinIO未启用状态 -->
    <el-card v-if="!minioStatus.minioEnabled" class="info-card">
      <el-result
        icon="info"
        title="MinIO 未启用"
        sub-title="当前系统使用本地文件存储，未启用MinIO对象存储"
      >
        <template #extra>
          <el-button type="primary" @click="openConsole">打开MinIO Console</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- 监控数据展示 -->
    <div v-else class="monitor-content">
      <!-- 服务状态 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-item">
              <el-icon :size="32" color="#00C853"><Box /></el-icon>
              <div class="stat-info">
                <div class="stat-value">{{ minioStatus.totalBuckets || 0 }}</div>
                <div class="stat-label">存储桶数量</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-item">
              <el-icon :size="32" color="#FF9800"><Cpu /></el-icon>
              <div class="stat-info">
                <div class="stat-value">{{ minioStatus.uptime || '-' }}</div>
                <div class="stat-label">运行时间</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-item">
              <el-icon :size="32" color="#2196F3"><Link /></el-icon>
              <div class="stat-info">
                <div class="stat-value">
                  <el-link type="primary" :href="minioStatus.consoleUrl" target="_blank">
                    打开控制台
                  </el-link>
                </div>
                <div class="stat-label">MinIO Console</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 服务器信息 -->
      <el-row :gutter="16" class="info-row">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>服务信息</span>
              </div>
            </template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="服务状态">
                <el-tag :type="minioStatus.status === 'online' ? 'success' : 'danger'" size="small">
                  {{ minioStatus.status === 'online' ? '在线' : '离线' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="API端点">{{ minioStatus.endpoint }}</el-descriptions-item>
              <el-descriptions-item label="控制台地址">
                <el-link type="primary" :href="minioStatus.consoleUrl" target="_blank">
                  {{ minioStatus.consoleUrl }}
                </el-link>
              </el-descriptions-item>
              <el-descriptions-item label="版本">{{ minioStatus.version || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>存储桶列表</span>
                <el-tag size="small">{{ minioStatus.buckets?.length || 0 }} 个</el-tag>
              </div>
            </template>
            <el-table :data="bucketList" size="small" max-height="200">
              <el-table-column prop="name" label="名称" />
              <el-table-column prop="objectCount" label="对象数" width="100" align="center">
                <template #default="{ row }">
                  {{ formatNumber(row.objectCount) }}
                </template>
              </el-table-column>
              <el-table-column prop="totalSize" label="大小" width="100" align="right">
                <template #default="{ row }">
                  {{ formatSize(row.totalSize) }}
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- 操作按钮 -->
      <el-card shadow="hover" class="action-card">
        <div class="action-buttons">
          <el-button type="primary" :icon="Link" @click="openConsole">
            在新窗口打开MinIO控制台
          </el-button>
          <el-button :icon="Setting" @click="openConfig">
            配置MinIO参数
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 错误提示 -->
    <el-alert
      v-if="minioStatus.status === 'error'"
      :title="minioStatus.message || '连接MinIO服务器失败'"
      type="error"
      show-icon
      closable
      style="margin-top: 16px"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Box, TopRight, Refresh, Files, Cpu, DataLine, Link, Setting } from '@element-plus/icons-vue'
import { getMinioStatus, type MinioStatus } from '@/api/system'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const minioStatus = ref<MinioStatus>({
  monitorEnabled: true,
  storageType: 'local',
  minioEnabled: false,
  consoleUrl: 'http://localhost:9001',
  endpoint: '',
  bucketName: '',
  status: 'offline'
})

const statusTagType = computed(() => {
  if (!minioStatus.value.minioEnabled) return 'info'
  return minioStatus.value.status === 'online' ? 'success' : 'danger'
})

const statusText = computed(() => {
  if (!minioStatus.value.minioEnabled) return '未启用'
  return minioStatus.value.status === 'online' ? '运行中' : '离线'
})

// 将buckets对象转换为数组
const bucketList = computed(() => {
  const buckets = minioStatus.value.buckets
  if (!buckets) return []
  return Object.entries(buckets).map(([name, stats]: [string, any]) => ({
    name,
    objectCount: stats?.objectCount || 0,
    totalSize: stats?.totalSize || 0
  }))
})

async function loadData() {
  loading.value = true
  try {
    const res = await getMinioStatus()
    if (res.code === 200 && res.data) {
      minioStatus.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取MinIO状态失败')
  } finally {
    loading.value = false
  }
}

function openConsole() {
  window.open(minioStatus.value.consoleUrl, '_blank')
}

function openConfig() {
  router.push({ path: '/dashboard/config' })
}

function formatNumber(num: number): string {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + units[i]
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.minio-monitor-wrapper {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  padding: 12px;
  box-sizing: border-box;
}

.monitor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 10px 16px;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
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
  gap: 6px;
}

.info-card {
  margin-top: 16px;
}

.monitor-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
  overflow-y: auto;
  min-height: 0;
}

.stats-row {
  flex-shrink: 0;
}

.stat-card {
  height: 100%;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.info-row {
  flex-shrink: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.action-card {
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  gap: 12px;
  padding: 8px 0;
}
</style>