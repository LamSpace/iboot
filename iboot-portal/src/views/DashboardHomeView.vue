<template>
  <div class="content-wrapper">
    <PageHeader :title="t('dashboard.home')" />

    <!-- 统计卡片 -->
    <div class="metrics-grid">
      <MetricCard
        v-for="item in statsCards"
        :key="item.label"
        :label="item.label"
        :value="item.value"
        :icon="item.icon"
        :accent="item.accent"
      />
    </div>

    <!-- 系统状态 + 登录趋势 -->
    <div class="dashboard-grid">
      <!-- 系统健康状态 -->
      <el-card class="health-card">
        <template #header>
          <div class="card-header">
            <span>{{ t("dashboard.system_health") }}</span>
            <StatusTag :type="systemHealthType" :label="systemHealthLabel" />
          </div>
        </template>
        <div class="health-items" v-if="serverInfo">
          <div class="health-item">
            <div class="health-label">
              <el-icon><Cpu /></el-icon>
              <span>{{ t("dashboard.cpu") }}</span>
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
              <span>{{ t("dashboard.memory") }}</span>
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
              <span>{{ t("dashboard.jvm") }}</span>
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

      <!-- 登录趋势图 -->
      <el-card class="trend-card">
        <template #header>
          <div class="card-header">
            <span>{{ t("dashboard.login_trend") }}</span>
            <StatusTag
              type="info"
              :label="`${t('dashboard.login_trend_total')} ${totalLoginCount} ${t('common.times')}`"
            />
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
          <EmptyState :title="t('dashboard.login_trend_no_data')" />
        </div>
      </el-card>
    </div>

    <!-- 最近日志 -->
    <div class="dashboard-grid logs-grid">
      <el-card>
        <template #header>
          <div class="card-header">
            <span>{{ t("dashboard.recent_login_logs") }}</span>
            <el-button
              link
              type="primary"
              @click="router.push('/dashboard/login-log')"
              >{{ t("dashboard.view_all") }}</el-button
            >
          </div>
        </template>
        <el-table
          :data="summary?.recentLoginLogs ?? []"
          style="width: 100%"
          v-loading="loading"
          size="small"
        >
          <el-table-column
            prop="username"
            :label="t('dashboard.username')"
            min-width="100"
          />
          <el-table-column
            prop="ipAddress"
            :label="t('dashboard.login_ip')"
            min-width="120"
          />
          <el-table-column
            prop="status"
            :label="t('dashboard.status')"
            width="80"
            align="center"
          >
            <template #default="{ row }">
              <StatusTag
                :type="row.status === 1 ? 'success' : 'danger'"
                :label="
                  row.status === 1
                    ? t('dashboard.success')
                    : t('dashboard.failure')
                "
              />
            </template>
          </el-table-column>
          <el-table-column
            prop="loginTime"
            :label="t('dashboard.time')"
            min-width="160"
          />
          <template #empty>
            <EmptyState :title="t('common.no_data')" />
          </template>
        </el-table>
      </el-card>

      <el-card>
        <template #header>
          <div class="card-header">
            <span>{{ t("dashboard.recent_operate_logs") }}</span>
            <el-button
              link
              type="primary"
              @click="router.push('/dashboard/operate-log')"
              >{{ t("dashboard.view_all") }}</el-button
            >
          </div>
        </template>
        <el-table
          :data="summary?.recentOperateLogs ?? []"
          style="width: 100%"
          v-loading="loading"
          size="small"
        >
          <el-table-column
            prop="operatorName"
            :label="t('dashboard.operator')"
            min-width="100"
          />
          <el-table-column
            prop="title"
            :label="t('dashboard.module')"
            min-width="100"
          />
          <el-table-column
            prop="requestMethod"
            :label="t('dashboard.method')"
            width="70"
            align="center"
          />
          <el-table-column
            prop="status"
            :label="t('dashboard.status')"
            width="80"
            align="center"
          >
            <template #default="{ row }">
              <StatusTag
                :type="row.status === 1 ? 'success' : 'danger'"
                :label="
                  row.status === 1
                    ? t('dashboard.success')
                    : t('dashboard.failure')
                "
              />
            </template>
          </el-table-column>
          <el-table-column
            prop="operTime"
            :label="t('dashboard.time')"
            min-width="160"
          />
          <template #empty>
            <EmptyState :title="t('common.no_data')" />
          </template>
        </el-table>
      </el-card>
    </div>

    <!-- 快捷导航 -->
    <el-card class="shortcuts-card">
      <template #header>
        <span>{{ t("dashboard.shortcuts") }}</span>
      </template>
      <div class="shortcuts-grid">
        <div
          v-for="item in shortcuts"
          :key="item.path"
          class="shortcut-item"
          @click="router.push(`/dashboard/${item.path}`)"
        >
          <el-icon :size="24" :style="{ color: item.color }">
            <component :is="item.icon" />
          </el-icon>
          <span class="shortcut-label">{{ item.label }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, type Component } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import {
  User,
  Avatar,
  OfficeBuilding,
  Briefcase,
  Tickets,
  Operation,
  Setting,
  Document,
  UserFilled,
  Monitor,
  Cpu,
  Coin,
  Box,
} from "@element-plus/icons-vue";
import {
  getDashboardSummary,
  getOnlineUserCount,
  getServerMonitorInfo,
  getStatisticsReport,
  type DashboardSummary,
  type ServerInfoResponse,
  type TrendData,
} from "@/api/system";
import PageHeader from "@/components/PageHeader.vue";
import MetricCard from "@/components/MetricCard.vue";
import StatusTag from "@/components/StatusTag.vue";
import EmptyState from "@/components/EmptyState.vue";

const router = useRouter();
const { t } = useI18n();
const loading = ref(false);
const summary = ref<DashboardSummary | null>(null);
const onlineCount = ref(0);
const serverInfo = ref<ServerInfoResponse | null>(null);
const loginTrend = ref<TrendData[]>([]);
const todayActiveUsers = ref(0);

// 自动刷新定时器
let refreshTimer: ReturnType<typeof setInterval> | null = null;

interface StatCard {
  label: string;
  value: number;
  icon: Component;
  accent: "ink" | "coral" | "magenta" | "blue" | "purple" | "cyan";
}

const statsCards = computed<StatCard[]>(() => [
  {
    label: t("dashboard.user_count"),
    value: summary.value?.userCount ?? 0,
    icon: User,
    accent: "ink",
  },
  {
    label: t("dashboard.today_active"),
    value: todayActiveUsers.value,
    icon: UserFilled,
    accent: "blue",
  },
  {
    label: t("dashboard.online_users"),
    value: onlineCount.value,
    icon: Monitor,
    accent: "cyan",
  },
  {
    label: t("dashboard.dept_count"),
    value: summary.value?.deptCount ?? 0,
    icon: OfficeBuilding,
    accent: "purple",
  },
  {
    label: t("dashboard.login_log_count"),
    value: summary.value?.loginLogCount ?? 0,
    icon: Tickets,
    accent: "magenta",
  },
  {
    label: t("dashboard.operate_log_count"),
    value: summary.value?.operateLogCount ?? 0,
    icon: Operation,
    accent: "coral",
  },
]);

const shortcuts = computed(() => [
  {
    label: t("dashboard.user_management"),
    path: "user",
    icon: User,
    color: "var(--ib-color-brand-blue)",
  },
  {
    label: t("dashboard.role_management"),
    path: "role",
    icon: Avatar,
    color: "var(--ib-color-brand-magenta)",
  },
  {
    label: t("dashboard.dept_management"),
    path: "dept",
    icon: OfficeBuilding,
    color: "var(--ib-color-brand-purple)",
  },
  {
    label: t("dashboard.post_management"),
    path: "post",
    icon: Briefcase,
    color: "var(--ib-color-brand-coral)",
  },
  {
    label: t("dashboard.menu_management"),
    path: "menu",
    icon: Setting,
    color: "var(--ib-color-steel)",
  },
  {
    label: t("dashboard.operate_log"),
    path: "operate-log",
    icon: Document,
    color: "var(--ib-color-brand-cyan)",
  },
]);

// 系统健康状态类型
const systemHealthType = computed(() => {
  if (!serverInfo.value) return "info";
  const statuses = [
    serverInfo.value.cpu.status,
    serverInfo.value.memory.status,
    serverInfo.value.jvm.status,
  ];
  if (statuses.includes("DOWN")) return "danger";
  if (statuses.includes("WARN")) return "warning";
  return "success";
});

// 系统健康状态标签
const systemHealthLabel = computed(() => {
  if (!serverInfo.value) return t("dashboard.system_health_loading");
  const type = systemHealthType.value;
  if (type === "danger") return t("dashboard.system_health_error");
  if (type === "warning") return t("dashboard.system_health_warning");
  return t("dashboard.system_health_normal");
});

// 登录趋势总数
const totalLoginCount = computed(() => {
  return loginTrend.value.reduce((sum, item) => sum + item.value, 0);
});

// 登录趋势最大值
const maxTrendValue = computed(() => {
  if (loginTrend.value.length === 0) return 1;
  return Math.max(...loginTrend.value.map((t) => t.value), 1);
});

// 获取进度条状态
const getProgressStatus = (status: string) => {
  if (status === "DOWN") return "exception";
  if (status === "WARN") return "warning";
  return "success";
};

// 获取趋势柱状图高度
const getTrendBarHeight = (value: number) => {
  return Math.max(Math.round((value / maxTrendValue.value) * 100), 5);
};

// 格式化日期标签
const formatDateLabel = (date: string) => {
  return date.substring(5); // MM-DD
};

// 加载基础数据
const loadData = async () => {
  loading.value = true;
  try {
    const res = await getDashboardSummary();
    if (res.code === 200) {
      summary.value = res.data;
    }
  } finally {
    loading.value = false;
  }
};

// 加载在线用户数
const loadOnlineCount = async () => {
  try {
    const res = await getOnlineUserCount();
    if (res.code === 200) {
      onlineCount.value = res.data;
    }
  } catch {
    // 忽略错误
  }
};

// 加载服务器状态
const loadServerInfo = async () => {
  try {
    const res = await getServerMonitorInfo();
    if (res.code === 200) {
      serverInfo.value = res.data;
    }
  } catch {
    // 忽略错误
  }
};

// 加载统计数据（趋势 + 活跃用户）
const loadStatistics = async () => {
  try {
    // 最近7天
    const end = new Date();
    const start = new Date();
    start.setTime(start.getTime() - 6 * 24 * 60 * 60 * 1000);
    const formatDate = (d: Date) => d.toISOString().split("T")[0];

    const res = await getStatisticsReport({
      startDate: formatDate(start),
      endDate: formatDate(end),
    });
    if (res.code === 200) {
      loginTrend.value = res.data.logStats.loginTrend;
      todayActiveUsers.value = res.data.userStats.activeUsersToday;
    }
  } catch {
    // 忽略错误
  }
};

// 加载所有数据
const loadAllData = async () => {
  await Promise.all([
    loadData(),
    loadOnlineCount(),
    loadServerInfo(),
    loadStatistics(),
  ]);
};

// 启动定时刷新
const startAutoRefresh = () => {
  // 每30秒刷新在线用户和服务器状态
  refreshTimer = setInterval(() => {
    loadOnlineCount();
    loadServerInfo();
  }, 30000);
};

// 停止定时刷新
const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
};

onMounted(() => {
  loadAllData();
  startAutoRefresh();
});

onUnmounted(() => {
  stopAutoRefresh();
});
</script>

<style scoped>
.content-wrapper {
  padding: var(--ib-content-gutter);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--ib-space-md);
  margin-bottom: var(--ib-space-md);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 2fr);
  gap: var(--ib-space-md);
  margin-bottom: var(--ib-space-md);
}

.logs-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.health-card :deep(.el-card__body) {
  height: 220px;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.health-card :deep(.el-card__body::-webkit-scrollbar) {
  display: none;
}

.health-items {
  display: flex;
  flex-direction: column;
  gap: var(--ib-space-lg);
}

.health-item {
  display: flex;
  flex-direction: column;
  gap: var(--ib-space-xs);
}

.health-label {
  display: flex;
  align-items: center;
  gap: var(--ib-space-xs);
  font-size: var(--ib-text-caption);
  color: var(--ib-color-steel);
}

.health-loading {
  padding: var(--ib-space-md) 0;
}

.trend-card :deep(.el-card__body) {
  height: 220px;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.trend-card :deep(.el-card__body::-webkit-scrollbar) {
  display: none;
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
  gap: var(--ib-space-sm);
  padding: 0 var(--ib-space-xs);
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
  margin-bottom: var(--ib-space-xxs);
  font-size: var(--ib-text-micro);
  font-weight: var(--ib-weight-semibold);
  color: var(--ib-color-brand-blue);
}

.trend-bar {
  width: 100%;
  max-width: 40px;
  min-height: 4px;
  border-radius: var(--ib-radius-xs) var(--ib-radius-xs) 0 0;
  background: var(--ib-color-brand-blue);
  transition: height var(--ib-duration-base) var(--ib-ease);
}

.trend-bar:hover {
  opacity: 0.85;
}

.trend-labels {
  display: flex;
  gap: var(--ib-space-sm);
  padding: var(--ib-space-xs) var(--ib-space-xs) 0;
}

.trend-labels span {
  flex: 1;
  text-align: center;
  font-size: var(--ib-text-micro);
  color: var(--ib-color-steel);
}

.trend-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: var(--ib-weight-semibold);
}

.shortcuts-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--ib-space-xs);
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--ib-space-xs);
  padding: var(--ib-space-md) 0;
  border-radius: var(--ib-radius-md);
  cursor: pointer;
  transition: background var(--ib-duration-fast) var(--ib-ease);
}

.shortcut-item:hover {
  background: var(--ib-color-surface);
}

.shortcut-label {
  font-size: var(--ib-text-caption);
  color: var(--ib-color-charcoal);
  text-align: center;
}

@media (max-width: 1280px) {
  .metrics-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1024px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .shortcuts-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .logs-grid {
    grid-template-columns: 1fr;
  }

  .shortcuts-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
