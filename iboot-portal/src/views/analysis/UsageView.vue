<template>
  <div class="content-wrapper">
    <PageHeader :title="t('analysis.usage.title')">
      <template #actions>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          :range-separator="t('analysis.usage.date_range.separator')"
          :start-placeholder="t('analysis.usage.date_range.start')"
          :end-placeholder="t('analysis.usage.date_range.end')"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          style="width: 260px"
          @change="loadData"
        />
        <el-button
          :icon="Refresh"
          circle
          size="small"
          @click="loadData"
          :loading="loading"
        />
      </template>
    </PageHeader>

    <div v-loading="loading">
      <!-- 活跃度概览卡片 -->
      <div class="metrics-grid" v-if="usageData">
        <MetricCard
          :label="t('analysis.usage.overview.today_active')"
          :value="usageData.activityStats.todayActiveUsers"
          accent="blue"
        />
        <MetricCard
          :label="t('analysis.usage.overview.week_active')"
          :value="usageData.activityStats.weekActiveUsers"
          accent="magenta"
        />
        <MetricCard
          :label="t('analysis.usage.overview.month_active')"
          :value="usageData.activityStats.monthActiveUsers"
          accent="cyan"
        />
      </div>

      <!-- 分析内容 -->
      <el-row :gutter="16" v-if="usageData">
        <!-- 左侧：活跃用户排行 -->
        <el-col :xs="24" :md="12">
          <el-card class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">{{
                  t("analysis.usage.active_user_rank.title")
                }}</span>
                <el-tag type="info" size="small"
                  >{{ t("analysis.usage.active_user_rank.top") }} 10</el-tag
                >
              </div>
            </template>
            <el-table
              :data="usageData.activityStats.topActiveUsers"
              size="small"
              stripe
              max-height="320"
            >
              <el-table-column
                :label="t('analysis.usage.active_user_rank.rank')"
                width="60"
                align="center"
              >
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
              <el-table-column
                prop="username"
                :label="t('analysis.usage.active_user_rank.username')"
                min-width="120"
              />
              <el-table-column
                prop="count"
                :label="t('analysis.usage.active_user_rank.operation_count')"
                width="100"
                align="right"
              >
                <template #default="{ row }">
                  <span class="stat-num">{{ row.count }}</span>
                </template>
              </el-table-column>
              <el-table-column
                :label="t('analysis.usage.active_user_rank.activity')"
                width="150"
              >
                <template #default="{ row }">
                  <el-progress
                    :percentage="getActivePercent(row.count)"
                    :stroke-width="10"
                    :show-text="false"
                  />
                </template>
              </el-table-column>
            </el-table>
            <EmptyState
              v-if="usageData.activityStats.topActiveUsers.length === 0"
              :title="t('analysis.usage.empty.no_data')"
            />
          </el-card>
        </el-col>

        <!-- 右侧：功能模块排行 -->
        <el-col :xs="24" :md="12">
          <el-card class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">{{
                  t("analysis.usage.feature_module.title")
                }}</span>
                <el-tag type="success" size="small">{{
                  t("analysis.usage.feature_module.frequency")
                }}</el-tag>
              </div>
            </template>
            <el-table
              :data="usageData.featureUsageStats.moduleRanking"
              size="small"
              stripe
              max-height="320"
            >
              <el-table-column
                :label="t('analysis.usage.feature_module.rank')"
                width="60"
                align="center"
              >
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
              <el-table-column
                prop="name"
                :label="t('analysis.usage.feature_module.module_name')"
                min-width="120"
              />
              <el-table-column
                prop="value"
                :label="t('analysis.usage.feature_module.usage_count')"
                width="100"
                align="right"
              >
                <template #default="{ row }">
                  <span class="stat-num">{{ row.value }}</span>
                </template>
              </el-table-column>
              <el-table-column
                :label="t('analysis.usage.feature_module.percentage')"
                width="150"
              >
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
            <EmptyState
              v-if="usageData.featureUsageStats.moduleRanking.length === 0"
              :title="t('analysis.usage.empty.no_data')"
            />
          </el-card>
        </el-col>
      </el-row>

      <!-- 时段分析 -->
      <el-row :gutter="16" style="margin-top: 16px" v-if="usageData">
        <el-col :span="24">
          <el-card class="analysis-card">
            <template #header>
              <div class="card-header">
                <span class="card-title">{{
                  t("analysis.usage.time_distribution.title")
                }}</span>
                <el-tag type="warning" size="small">
                  {{ t("analysis.usage.time_distribution.peak_hours") }}:
                  {{ usageData.timeDistributionStats.peakHours }}
                </el-tag>
              </div>
            </template>
            <div class="hour-chart">
              <div class="hour-bars">
                <div
                  v-for="item in usageData.timeDistributionStats
                    .hourlyDistribution"
                  :key="item.hour"
                  class="hour-bar-wrapper"
                >
                  <div
                    class="hour-bar"
                    :style="{ height: getHourBarHeight(item.value) + '%' }"
                    :class="{ peak: isPeakHour(item.hour) }"
                    :title="`${item.hour}:00 - ${item.value}${t('analysis.usage.active_user_rank.operation_count')}`"
                  ></div>
                </div>
              </div>
              <div class="hour-labels">
                <span v-for="h in 24" :key="h - 1">{{ h - 1 }}</span>
              </div>
              <div class="hour-axis-label">
                {{ t("analysis.usage.time_distribution.hour_label") }}
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 活跃趋势 -->
      <el-row :gutter="16" style="margin-top: 16px" v-if="usageData">
        <el-col :span="24">
          <el-card class="analysis-card">
            <template #header>
              <span class="card-title">{{
                t("analysis.usage.activity_trend.title")
              }}</span>
            </template>
            <div
              class="trend-container"
              v-if="usageData.activityStats.activeUserTrend.length > 0"
            >
              <div class="trend-chart-large">
                <div
                  v-for="item in usageData.activityStats.activeUserTrend"
                  :key="item.date"
                  class="trend-bar-large"
                  :style="{ height: getTrendBarHeight(item.value) + '%' }"
                  :title="`${item.date}: ${item.value}${t('analysis.usage.active_user_rank.operation_count')}`"
                >
                  <span class="trend-value">{{ item.value }}</span>
                </div>
              </div>
              <div class="trend-labels-large">
                <span
                  v-for="item in usageData.activityStats.activeUserTrend"
                  :key="item.date"
                >
                  {{ formatDateLabel(item.date) }}
                </span>
              </div>
            </div>
            <EmptyState
              v-else
              :title="t('analysis.usage.empty.no_trend_data')"
            />
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { Refresh } from "@element-plus/icons-vue";
import { getUsageAnalysis, type UsageResponse } from "@/api/system";
import PageHeader from "@/components/PageHeader.vue";
import MetricCard from "@/components/MetricCard.vue";
import EmptyState from "@/components/EmptyState.vue";

const { t } = useI18n();
const loading = ref(false);
const usageData = ref<UsageResponse | null>(null);

// 日期范围
const dateRange = ref<[string, string] | null>(null);

// 日期快捷选项
const dateShortcuts = [
  {
    text: t("analysis.usage.date_range.shortcuts.last_7_days"),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 6 * 24 * 60 * 60 * 1000);
      return [start, end];
    },
  },
  {
    text: t("analysis.usage.date_range.shortcuts.last_30_days"),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 29 * 24 * 60 * 60 * 1000);
      return [start, end];
    },
  },
  {
    text: t("analysis.usage.date_range.shortcuts.this_month"),
    value: () => {
      const end = new Date();
      const start = new Date(end.getFullYear(), end.getMonth(), 1);
      return [start, end];
    },
  },
];

// 最大活跃度
const maxActiveCount = computed(() => {
  if (!usageData.value) return 1;
  const counts = usageData.value.activityStats.topActiveUsers.map(
    (u) => u.count,
  );
  return Math.max(...counts, 1);
});

// 模块使用总数
const totalModuleUsage = computed(() => {
  if (!usageData.value) return 1;
  return (
    usageData.value.featureUsageStats.moduleRanking.reduce(
      (sum, m) => sum + m.value,
      0,
    ) || 1
  );
});

// 小时分布最大值
const maxHourValue = computed(() => {
  if (!usageData.value) return 1;
  const values = usageData.value.timeDistributionStats.hourlyDistribution.map(
    (h) => h.value,
  );
  return Math.max(...values, 1);
});

// 活跃趋势最大值
const maxTrendValue = computed(() => {
  if (!usageData.value) return 1;
  const values = usageData.value.activityStats.activeUserTrend.map(
    (t) => t.value,
  );
  return Math.max(...values, 1);
});

// 高峰时段数组
const peakHours = computed(() => {
  if (!usageData.value) return [];
  const peakStr = usageData.value.timeDistributionStats.peakHours;
  return peakStr.split(",").map((s) => parseInt(s.trim().split(":")[0]));
});

// 获取活跃度百分比
const getActivePercent = (count: number) => {
  return Math.round((count / maxActiveCount.value) * 100);
};

// 获取模块占比
const getModulePercent = (value: number) => {
  return Math.round((value / totalModuleUsage.value) * 100);
};

// 获取小时柱状图高度
const getHourBarHeight = (value: number) => {
  return Math.round((value / maxHourValue.value) * 100);
};

// 获取趋势柱状图高度
const getTrendBarHeight = (value: number) => {
  return Math.max(Math.round((value / maxTrendValue.value) * 100), 5);
};

// 判断是否高峰时段
const isPeakHour = (hour: number) => {
  return peakHours.value.includes(hour);
};

// 格式化日期标签
const formatDateLabel = (date: string) => {
  return date.substring(5); // MM-DD
};

// 加载数据
const loadData = async () => {
  loading.value = true;
  try {
    const params: { startDate?: string; endDate?: string } = {};
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const res = await getUsageAnalysis(params);
    if (res.code === 200) {
      usageData.value = res.data;
    }
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  // 默认最近 30 天
  const end = new Date();
  const start = new Date();
  start.setTime(start.getTime() - 29 * 24 * 60 * 60 * 1000);
  const formatDate = (d: Date) => d.toISOString().split("T")[0];
  dateRange.value = [formatDate(start), formatDate(end)];
  loadData();
});
</script>

<style scoped>
.content-wrapper {
  padding: var(--ib-content-gutter);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--ib-space-md);
  margin-bottom: var(--ib-space-md);
}

.analysis-card {
  margin-bottom: var(--ib-space-md);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-weight: var(--ib-weight-semibold);
  font-size: var(--ib-text-body-sm);
  color: var(--ib-color-ink);
}

.stat-num {
  font-weight: var(--ib-weight-semibold);
  color: var(--ib-color-ink);
}

/* 24 小时分布图 */
.hour-chart {
  padding: var(--ib-space-md) 0;
}

.hour-bars {
  display: flex;
  align-items: flex-end;
  height: 120px;
  gap: 2px;
  padding: 0 var(--ib-space-xxs);
}

.hour-bar-wrapper {
  flex: 1;
  height: 100%;
  display: flex;
  align-items: flex-end;
}

.hour-bar {
  width: 100%;
  min-height: 4px;
  border-radius: var(--ib-radius-xs) var(--ib-radius-xs) 0 0;
  background: var(--ib-color-brand-blue-mid);
  transition: height var(--ib-duration-base) var(--ib-ease);
}

.hour-bar.peak {
  background: var(--ib-color-brand-coral);
}

.hour-bar:hover {
  opacity: 0.8;
  transform: scaleY(1.05);
}

.hour-labels {
  display: flex;
  padding: 6px var(--ib-space-xxs) 0;
  font-size: var(--ib-text-micro);
  color: var(--ib-color-steel);
}

.hour-labels span {
  flex: 1;
  text-align: center;
}

.hour-axis-label {
  text-align: center;
  font-size: var(--ib-text-micro);
  color: var(--ib-color-steel);
  margin-top: var(--ib-space-xs);
}

/* 趋势图 */
.trend-container {
  padding: var(--ib-space-md) 0;
}

.trend-chart-large {
  display: flex;
  align-items: flex-end;
  height: 150px;
  gap: var(--ib-space-xs);
  padding: 0 var(--ib-space-xs);
}

.trend-bar-large {
  flex: 1;
  min-height: 20px;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  position: relative;
  border-radius: var(--ib-radius-xs) var(--ib-radius-xs) 0 0;
  background: var(--ib-color-success-text);
  transition: height var(--ib-duration-base) var(--ib-ease);
}

.trend-bar-large:hover {
  opacity: 0.85;
}

.trend-value {
  position: absolute;
  top: -20px;
  font-size: var(--ib-text-micro);
  font-weight: var(--ib-weight-semibold);
  color: var(--ib-color-success-text);
}

.trend-labels-large {
  display: flex;
  padding: var(--ib-space-xs);
  font-size: var(--ib-text-micro);
  color: var(--ib-color-steel);
}

.trend-labels-large span {
  flex: 1;
  text-align: center;
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
