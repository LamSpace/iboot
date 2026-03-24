<template>
  <div class="content-wrapper">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><Connection /></el-icon>
        <span class="toolbar-title">{{ t("monitor.redis.title") }}</span>
        <el-tag v-if="redisInfo" type="success" size="small" effect="plain">
          Redis {{ redisInfo.serverInfo.version }}
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-switch
          v-model="autoRefresh"
          :active-text="t('monitor.redis.auto_refresh')"
          inactive-text=""
          style="margin-right: 12px"
          @change="toggleAutoRefresh"
        />
        <el-tooltip :content="t('monitor.redis.refresh')" placement="top">
          <el-button
            :icon="Refresh"
            circle
            size="small"
            @click="loadAll"
            :loading="loading"
          />
        </el-tooltip>
      </div>
    </div>

    <div v-loading="loading && !redisInfo">
      <!-- 概览卡片 -->
      <el-row :gutter="16" class="stats-cards" v-if="redisInfo">
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">{{ t("monitor.redis.redis_version") }}</div>
            <div class="stat-value">{{ redisInfo.serverInfo.version }}</div>
            <div class="stat-desc">{{ redisInfo.serverInfo.mode }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">{{ t("monitor.redis.uptime") }}</div>
            <div class="stat-value">
              {{ redisInfo.serverInfo.uptimeDisplay }}
            </div>
            <div class="stat-desc">
              PID: {{ redisInfo.serverInfo.processId }}
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">
              {{ t("monitor.redis.connected_clients") }}
            </div>
            <div class="stat-value">
              {{ redisInfo.serverInfo.connectedClients }}
            </div>
            <div class="stat-desc">
              {{ t("monitor.redis.tcp_port") }}:
              {{ redisInfo.serverInfo.tcpPort }}
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">{{ t("monitor.redis.memory_usage") }}</div>
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
                <el-tag
                  type="primary"
                  size="small"
                  effect="plain"
                  style="margin-right: 6px"
                >
                  {{ t("monitor.redis.server_info") }}
                </el-tag>
                {{ t("monitor.redis.basic_info") }}
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item :label="t('monitor.redis.redis_version')">{{
                redisInfo.serverInfo.version
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.mode')">{{
                redisInfo.serverInfo.mode
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.os')">{{
                redisInfo.serverInfo.os
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.tcp_port')">{{
                redisInfo.serverInfo.tcpPort
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.process_id')">{{
                redisInfo.serverInfo.processId
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.uptime')">{{
                redisInfo.serverInfo.uptimeDisplay
              }}</el-descriptions-item>
              <el-descriptions-item
                :label="t('monitor.redis.connected_clients_label')"
                >{{
                  redisInfo.serverInfo.connectedClients
                }}</el-descriptions-item
              >
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag
                  type="warning"
                  size="small"
                  effect="plain"
                  style="margin-right: 6px"
                >
                  {{ t("monitor.redis.memory_info") }}
                </el-tag>
                {{ t("monitor.redis.memory_detail") }}
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item :label="t('monitor.redis.used_memory')">{{
                redisInfo.memoryInfo.usedMemoryHuman
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.max_memory')">{{
                redisInfo.memoryInfo.maxMemoryHuman
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.usage_rate')">
                {{
                  redisInfo.memoryInfo.maxMemory > 0
                    ? redisInfo.memoryInfo.memoryUsageRate + "%"
                    : t("monitor.redis.unlimited")
                }}
              </el-descriptions-item>
              <el-descriptions-item
                :label="t('monitor.redis.fragmentation_ratio')"
                >{{
                  redisInfo.memoryInfo.fragmentationRatio
                }}</el-descriptions-item
              >
              <el-descriptions-item :label="t('monitor.redis.status')">
                <el-tag :type="memoryTagType" size="small">{{
                  redisInfo.memoryInfo.statusLabel
                }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.warn_threshold')"
                >{{ redisInfo.memoryInfo.warnThreshold }}%</el-descriptions-item
              >
              <el-descriptions-item :label="t('monitor.redis.error_threshold')"
                >{{
                  redisInfo.memoryInfo.errorThreshold
                }}%</el-descriptions-item
              >
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
                <el-tag
                  type="info"
                  size="small"
                  effect="plain"
                  style="margin-right: 6px"
                >
                  {{ t("monitor.redis.stats_info") }}
                </el-tag>
                {{ t("monitor.redis.run_stats") }}
              </span>
            </template>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item :label="t('monitor.redis.qps')">
                <span class="highlight-value">{{
                  redisInfo.statsInfo.instantaneousOpsPerSec
                }}</span>
              </el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.hit_rate')">
                <span class="highlight-value"
                  >{{ redisInfo.statsInfo.hitRate }}%</span
                >
                <span class="stat-detail">
                  ({{ t("monitor.redis.keyspace_hits") }}
                  {{ redisInfo.statsInfo.keyspaceHits }} /
                  {{ t("monitor.redis.keyspace_misses") }}
                  {{ redisInfo.statsInfo.keyspaceMisses }})
                </span>
              </el-descriptions-item>
              <el-descriptions-item
                :label="t('monitor.redis.total_commands')"
                >{{
                  formatNumber(redisInfo.statsInfo.totalCommandsProcessed)
                }}</el-descriptions-item
              >
              <el-descriptions-item
                :label="t('monitor.redis.total_connections')"
                >{{
                  formatNumber(redisInfo.statsInfo.totalConnectionsReceived)
                }}</el-descriptions-item
              >
              <el-descriptions-item :label="t('monitor.redis.expired_keys')">{{
                formatNumber(redisInfo.statsInfo.expiredKeys)
              }}</el-descriptions-item>
              <el-descriptions-item :label="t('monitor.redis.evicted_keys')">{{
                formatNumber(redisInfo.statsInfo.evictedKeys)
              }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">
                <el-tag
                  type="danger"
                  size="small"
                  effect="plain"
                  style="margin-right: 6px"
                >
                  {{ t("monitor.redis.keyspace_info") }}
                </el-tag>
                {{ t("monitor.redis.keyspace_info") }}
              </span>
            </template>
            <el-table
              :data="redisInfo.keyspaceInfo"
              size="small"
              v-if="redisInfo.keyspaceInfo.length > 0"
            >
              <el-table-column
                prop="dbIndex"
                :label="t('monitor.redis.db_index')"
                width="80"
              >
                <template #default="{ row }">db{{ row.dbIndex }}</template>
              </el-table-column>
              <el-table-column
                prop="keys"
                :label="t('monitor.redis.keys_count')"
              />
              <el-table-column
                prop="expires"
                :label="t('monitor.redis.expires_count')"
              />
              <el-table-column
                prop="avgTtl"
                :label="t('monitor.redis.avg_ttl')"
              />
            </el-table>
            <el-empty
              v-else
              :description="t('monitor.redis.no_keyspace_data')"
              :image-size="60"
            />
          </el-card>
        </el-col>
      </el-row>

      <!-- 命令统计 Top10 -->
      <el-card
        shadow="hover"
        class="section-row"
        v-if="redisInfo && redisInfo.commandStats.length > 0"
      >
        <template #header>
          <span class="card-title">{{
            t("monitor.redis.command_stats_top10")
          }}</span>
        </template>
        <el-table :data="redisInfo.commandStats" size="small">
          <el-table-column
            prop="command"
            :label="t('monitor.redis.command')"
            width="180"
          >
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.command }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="calls" :label="t('monitor.redis.calls')">
            <template #default="{ row }">{{
              formatNumber(row.calls)
            }}</template>
          </el-table-column>
          <el-table-column
            prop="usecPerCall"
            :label="t('monitor.redis.avg_time')"
          >
            <template #default="{ row }"
              >{{ row.usecPerCall.toFixed(2) }} &micro;s</template
            >
          </el-table-column>
          <el-table-column prop="usec" :label="t('monitor.redis.total_time')">
            <template #default="{ row }"
              >{{ formatNumber(row.usec) }} &micro;s</template
            >
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 缓存键管理 -->
      <el-card shadow="hover" class="section-row">
        <template #header>
          <span class="card-title">{{
            t("monitor.redis.cache_key_management")
          }}</span>
        </template>

        <!-- 搜索栏 -->
        <el-form :inline="true" class="search-form">
          <el-form-item :label="t('monitor.redis.key_pattern')">
            <el-input
              v-model="keyPattern"
              :placeholder="t('monitor.redis.placeholder.key_pattern')"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearchKeys"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearchKeys">{{
              t("monitor.redis.search")
            }}</el-button>
            <el-button @click="handleResetSearch">{{
              t("monitor.redis.reset")
            }}</el-button>
            <el-button
              v-permission="'redis:clear'"
              type="danger"
              @click="handleClearAll"
            >
              {{ t("monitor.redis.clear_all") }}
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 缓存键表格 -->
        <el-table
          :data="cacheKeys"
          size="small"
          v-loading="keysLoading"
          style="width: 100%"
        >
          <el-table-column
            prop="key"
            :label="t('monitor.redis.key_name')"
            min-width="300"
            show-overflow-tooltip
          />
          <el-table-column
            prop="type"
            :label="t('monitor.redis.key_type')"
            width="100"
          >
            <template #default="{ row }">
              <el-tag :type="keyTypeTag(row.type)" size="small">{{
                row.type
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="ttlDisplay"
            :label="t('monitor.redis.expiration')"
            width="150"
          />
          <el-table-column
            :label="t('monitor.redis.action')"
            width="150"
            align="center"
          >
            <template #default="{ row }">
              <el-button
                v-permission="'redis:query'"
                type="primary"
                link
                size="small"
                @click="handleViewKey(row.key)"
              >
                {{ t("monitor.redis.view") }}
              </el-button>
              <el-button
                v-permission="'redis:delete'"
                type="danger"
                link
                size="small"
                @click="handleDeleteKey(row.key)"
              >
                {{ t("monitor.redis.delete") }}
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
    <el-dialog
      v-model="keyDetailVisible"
      :title="t('monitor.redis.key_detail_title')"
      width="650px"
      destroy-on-close
    >
      <el-descriptions v-if="keyDetail" :column="2" border size="small">
        <el-descriptions-item :label="t('monitor.redis.key_name')" :span="2">{{
          keyDetail.key
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.redis.key_type')">
          <el-tag :type="keyTypeTag(keyDetail.type)" size="small">{{
            keyDetail.type
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('monitor.redis.expiration')">
          {{
            keyDetail.ttl === -1
              ? t("monitor.redis.permanent")
              : keyDetail.ttl === -2
                ? t("monitor.redis.expired")
                : keyDetail.ttl + t("monitor.redis.seconds")
          }}
        </el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">{{
        t("monitor.redis.key_value_content")
      }}</el-divider>
      <div class="key-value-content">
        <pre>{{ formatKeyValue(keyDetail?.value) }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { Connection, Refresh } from "@element-plus/icons-vue";
import {
  getRedisMonitorInfo,
  getCacheKeys,
  getCacheKeyValue,
  deleteCacheKey,
  clearAllCacheKeys,
  getConfigByKey,
  type RedisMonitorInfo,
  type CacheKeyInfo,
  type CacheKeyDetail,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";

const { t } = useI18n();
const dictStore = useDictStore();

// ==================== 监控数据 ====================
const loading = ref(false);
const redisInfo = ref<RedisMonitorInfo | null>(null);

// ==================== 缓存键管理 ====================
const keysLoading = ref(false);
const cacheKeys = ref<CacheKeyInfo[]>([]);
const keysTotal = ref(0);
const keysPageNum = ref(1);
const keysPageSize = ref(20);
const keyPattern = ref("*");

// ==================== 键值详情 ====================
const keyDetailVisible = ref(false);
const keyDetail = ref<CacheKeyDetail | null>(null);

// ==================== 自动刷新 ====================
const autoRefresh = ref(false);
const refreshInterval = ref(30);
let refreshTimer: ReturnType<typeof setInterval> | null = null;

// ==================== 计算属性 ====================
const memoryPercentage = computed(() => {
  if (!redisInfo.value || redisInfo.value.memoryInfo.maxMemory <= 0) return 0;
  return Math.min(Math.round(redisInfo.value.memoryInfo.memoryUsageRate), 100);
});

const progressStatus = computed(() => {
  if (!redisInfo.value) return "";
  const status = redisInfo.value.memoryInfo.status;
  if (status === "DOWN") return "exception";
  if (status === "WARN") return "warning";
  return "success";
});

const memoryTagType = computed(() => {
  if (!redisInfo.value) return "info";
  const status = redisInfo.value.memoryInfo.status;
  if (status === "DOWN") return "danger";
  if (status === "WARN") return "warning";
  return "success";
});

// ==================== 数据加载 ====================
const loadRedisInfo = async () => {
  loading.value = true;
  try {
    const res = await getRedisMonitorInfo();
    if (res.code === 200) {
      redisInfo.value = res.data;
    }
  } catch (e) {
    console.error("加载 Redis 监控数据失败", e);
  } finally {
    loading.value = false;
  }
};

const loadCacheKeys = async () => {
  keysLoading.value = true;
  try {
    const res = await getCacheKeys({
      pattern: keyPattern.value || "*",
      pageNum: keysPageNum.value,
      pageSize: keysPageSize.value,
    });
    if (res.code === 200) {
      cacheKeys.value = res.data.data;
      keysTotal.value = res.data.total;
    }
  } catch (e) {
    console.error("加载缓存键失败", e);
  } finally {
    keysLoading.value = false;
  }
};

const loadAll = () => {
  loadRedisInfo();
  loadCacheKeys();
};

const loadRefreshConfig = async () => {
  try {
    const res = await getConfigByKey("monitor.redis.refreshInterval");
    if (res.code === 200 && res.data) {
      const val = parseInt(res.data as unknown as string, 10);
      if (!isNaN(val) && val > 0) {
        refreshInterval.value = val;
      }
    }
  } catch {
    // use default
  }
};

// ==================== 缓存键操作 ====================
const handleSearchKeys = () => {
  keysPageNum.value = 1;
  loadCacheKeys();
};

const handleResetSearch = () => {
  keyPattern.value = "*";
  handleSearchKeys();
};

const handleViewKey = async (key: string) => {
  try {
    const res = await getCacheKeyValue(key);
    if (res.code === 200) {
      keyDetail.value = res.data;
      keyDetailVisible.value = true;
    } else {
      ElMessage.error(res.message || t("monitor.redis.get_key_error"));
    }
  } catch {
    ElMessage.error(t("monitor.redis.get_key_error"));
  }
};

const handleDeleteKey = (key: string) => {
  ElMessageBox.confirm(
    t("monitor.redis.confirm_delete_key", { key }),
    t("monitor.redis.delete_confirm_title"),
    { type: "warning" },
  )
    .then(async () => {
      const res = await deleteCacheKey(key);
      if (res.code === 200) {
        ElMessage.success(t("monitor.redis.delete_success"));
        loadCacheKeys();
      } else {
        ElMessage.error(res.message || t("monitor.redis.delete_error"));
      }
    })
    .catch(() => {});
};

const handleClearAll = () => {
  ElMessageBox.confirm(
    t("monitor.redis.confirm_clear_all"),
    t("monitor.redis.danger_operation"),
    {
      type: "error",
      confirmButtonText: t("monitor.redis.confirm"),
      cancelButtonText: t("monitor.redis.cancel"),
    },
  )
    .then(async () => {
      const res = await clearAllCacheKeys();
      if (res.code === 200) {
        ElMessage.success(t("monitor.redis.clear_success"));
        loadAll();
      } else {
        ElMessage.error(res.message || t("monitor.redis.clear_error"));
      }
    })
    .catch(() => {});
};

// ==================== 自动刷新 ====================
const toggleAutoRefresh = (val: boolean | string | number) => {
  if (val) {
    startAutoRefresh();
  } else {
    stopAutoRefresh();
  }
};

const startAutoRefresh = () => {
  stopAutoRefresh();
  if (refreshInterval.value > 0) {
    refreshTimer = setInterval(() => {
      loadAll();
    }, refreshInterval.value * 1000);
  }
};

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
};

// ==================== 工具函数 ====================
const formatNumber = (num: number) => {
  if (num == null) return "0";
  return num.toLocaleString();
};

const formatKeyValue = (value: unknown): string => {
  if (value === null || value === undefined) return "";
  if (typeof value === "string") return value;
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
};

const keyTypeTag = (type: string): string => {
  const map: Record<string, string> = {
    string: "success",
    hash: "warning",
    list: "primary",
    set: "info",
    zset: "danger",
  };
  return map[type] || "info";
};

// ==================== 生命周期 ====================
onMounted(async () => {
  await dictStore.loadDicts("sys_redis_info_category", "sys_monitor_status");
  await loadRefreshConfig();
  loadAll();
});

onUnmounted(() => {
  stopAutoRefresh();
});
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
  font-family: "Courier New", Courier, monospace;
}
</style>
