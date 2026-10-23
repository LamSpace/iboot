<template>
  <div class="content-wrapper">
    <PageHeader :title="t('monitor.notificationDelivery.title')" />

    <FilterBar>
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('monitor.notificationDelivery.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('common.select')"
            clearable
          >
            <el-option
              v-for="status in statuses"
              :key="status"
              :label="t(`monitor.notificationDelivery.statusValues.${status}`)"
              :value="status"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('monitor.notificationDelivery.channel')">
          <el-select
            v-model="queryParams.channel"
            :placeholder="t('common.select')"
            clearable
          >
            <el-option
              v-for="channel in channels"
              :key="channel"
              :label="
                t(`monitor.notificationDelivery.channelValues.${channel}`)
              "
              :value="channel"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('monitor.notificationDelivery.recipient')">
          <el-input
            v-model="queryParams.recipient"
            :placeholder="t('common.input')"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            {{ t("common.search") }}
          </el-button>
          <el-button @click="handleReset">{{ t("common.reset") }}</el-button>
        </el-form-item>
      </el-form>
    </FilterBar>

    <div class="content-body">
      <el-table :data="deliveries" v-loading="loading" row-key="id">
        <el-table-column
          prop="id"
          :label="t('monitor.notificationDelivery.id')"
          width="80"
        />
        <el-table-column
          prop="eventId"
          :label="t('monitor.notificationDelivery.eventId')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="eventType"
          :label="t('monitor.notificationDelivery.eventType')"
          width="140"
        >
          <template #default="{ row }">
            {{
              row.eventType
                ? t(
                    `monitor.notificationDelivery.eventTypeValues.${row.eventType}`,
                    row.eventType,
                  )
                : "-"
            }}
          </template>
        </el-table-column>
        <el-table-column
          prop="channel"
          :label="t('monitor.notificationDelivery.channel')"
          width="100"
        >
          <template #default="{ row }">
            <el-tag size="small" type="info">
              {{
                row.channel
                  ? t(
                      `monitor.notificationDelivery.channelValues.${row.channel}`,
                      row.channel,
                    )
                  : "-"
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="recipient"
          :label="t('monitor.notificationDelivery.recipient')"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="title"
          :label="t('monitor.notificationDelivery.messageTitle')"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          :label="t('monitor.notificationDelivery.status')"
          width="110"
        >
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">
              {{
                t(
                  `monitor.notificationDelivery.statusValues.${row.status}`,
                  row.status,
                )
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="t('monitor.notificationDelivery.attempts')"
          width="110"
        >
          <template #default="{ row }">
            {{ row.attempts }} / {{ row.maxAttempts }}
          </template>
        </el-table-column>
        <el-table-column
          prop="nextRetryTime"
          :label="t('monitor.notificationDelivery.nextRetryTime')"
          width="170"
        >
          <template #default="{ row }">
            {{ formatTime(row.nextRetryTime) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="lastError"
          :label="t('monitor.notificationDelivery.lastError')"
          min-width="220"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.lastError || "-" }}
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          :label="t('monitor.notificationDelivery.createTime')"
          width="170"
        >
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('common.action')"
          width="100"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'FAILED' || row.status === 'DEAD'"
              v-permission="'notification:delivery:retry'"
              type="primary"
              link
              size="small"
              @click="handleRetry(row)"
            >
              {{ t("monitor.notificationDelivery.retry") }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState :title="t('common.no_data')" />
        </template>
      </el-table>

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
import { onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getNotificationDeliveries,
  retryNotificationDelivery,
  type DateTimeValue,
  type NotificationDelivery,
  type NotificationDeliveryQuery,
} from "@/api/notification";
import PageHeader from "@/components/PageHeader.vue";
import FilterBar from "@/components/FilterBar.vue";
import EmptyState from "@/components/EmptyState.vue";

const { t } = useI18n();

const statuses = ["PENDING", "SENDING", "SUCCESS", "FAILED", "DEAD", "SKIPPED"];
const channels = ["IN_APP", "EMAIL", "DINGTALK", "SMS"];

const loading = ref(false);
const deliveries = ref<NotificationDelivery[]>([]);
const total = ref(0);

const queryParams = reactive<NotificationDeliveryQuery>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  channel: undefined,
  recipient: "",
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getNotificationDeliveries({
      ...queryParams,
      recipient: queryParams.recipient || undefined,
    });
    if (res.code === 200) {
      deliveries.value = res.data.data;
      total.value = res.data.total;
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  queryParams.pageNum = 1;
  loadData();
};

const handleReset = () => {
  queryParams.status = undefined;
  queryParams.channel = undefined;
  queryParams.recipient = "";
  handleSearch();
};

const handleRetry = async (row: NotificationDelivery) => {
  await ElMessageBox.confirm(
    t("monitor.notificationDelivery.retryConfirm"),
    t("monitor.notificationDelivery.retry"),
    {
      confirmButtonText: t("common.confirm"),
      cancelButtonText: t("common.cancel"),
      type: "warning",
    },
  );
  const res = await retryNotificationDelivery(row.id);
  if (res.code === 200) {
    ElMessage.success(t("monitor.notificationDelivery.retrySuccess"));
    await loadData();
  }
};

const statusType = (status: string) => {
  if (status === "SUCCESS") return "success";
  if (status === "FAILED" || status === "DEAD") return "danger";
  if (status === "SENDING") return "warning";
  return "info";
};

const formatTime = (value?: DateTimeValue | null) => {
  if (!value) return "-";
  const date = Array.isArray(value)
    ? new Date(
        value[0] ?? 1970,
        (value[1] ?? 1) - 1,
        value[2] ?? 1,
        value[3] ?? 0,
        value[4] ?? 0,
        value[5] ?? 0,
      )
    : new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
};

onMounted(loadData);
</script>

<style scoped>
.content-wrapper {
  padding: 16px;
}

.content-body {
  margin-top: 12px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
