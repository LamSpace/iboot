<template>
  <div class="content-wrapper">
    <PageHeader
      :title="t('monitor.backup.title')"
      :description="summaryText"
    />

    <div class="content-body">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('monitor.backup.tabs.backups')" name="backups">
          <FilterBar>
            <el-form :inline="true" :model="backupQuery" class="search-form">
              <el-form-item :label="t('monitor.backup.columns.status')">
                <el-select
                  v-model="backupQuery.status"
                  :placeholder="t('common.select')"
                  clearable
                >
                  <el-option
                    v-for="status in backupStatuses"
                    :key="status"
                    :label="t(`monitor.backup.statuses.${status}`)"
                    :value="status"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleBackupSearch">
                  {{ t("common.search") }}
                </el-button>
                <el-button @click="handleBackupReset">
                  {{ t("common.reset") }}
                </el-button>
              </el-form-item>
            </el-form>
          </FilterBar>

          <TableToolbar>
            <el-button
              v-permission="'backup:record:create'"
              type="primary"
              :loading="creating"
              :disabled="!summary.enabled || hasRunningBackup"
              @click="handleCreate"
            >
              {{ t("monitor.backup.create") }}
            </el-button>
            <el-button :loading="loadingBackups" @click="loadAll">
              {{ t("common.reload") }}
            </el-button>
          </TableToolbar>

          <el-table
            :data="backups"
            style="width: 100%"
            v-loading="loadingBackups"
            row-key="id"
          >
            <el-table-column
              prop="id"
              :label="t('monitor.backup.columns.id')"
              width="80"
              align="center"
            />
            <el-table-column
              prop="backupNo"
              :label="t('monitor.backup.columns.backupNo')"
              min-width="210"
              show-overflow-tooltip
            />
            <el-table-column
              prop="triggerType"
              :label="t('monitor.backup.columns.triggerType')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                {{
                  t(
                    `monitor.backup.triggerTypes.${row.triggerType}`,
                    row.triggerType,
                  )
                }}
              </template>
            </el-table-column>
            <el-table-column
              prop="status"
              :label="t('monitor.backup.columns.status')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag size="small" :type="backupStatusType(row.status)">
                  {{ t(`monitor.backup.statuses.${row.status}`, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="ciphertextSize"
              :label="t('monitor.backup.columns.size')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                {{ formatSize(row.ciphertextSize) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="storageType"
              :label="t('monitor.backup.columns.storageType')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                {{
                  row.storageType
                    ? t(
                        `monitor.backup.storageTypes.${row.storageType}`,
                        row.storageType,
                      )
                    : "-"
                }}
              </template>
            </el-table-column>
            <el-table-column
              prop="checksumSha256"
              :label="t('monitor.backup.columns.checksum')"
              min-width="150"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                {{
                  row.checksumSha256
                    ? row.checksumSha256.slice(0, 16) + "..."
                    : "-"
                }}
              </template>
            </el-table-column>
            <el-table-column
              prop="durationMs"
              :label="t('monitor.backup.columns.duration')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                {{ formatDuration(row.durationMs) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="startedAt"
              :label="t('monitor.backup.columns.startedAt')"
              width="170"
            >
              <template #default="{ row }">
                {{ formatTime(row.startedAt) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="createdBy"
              :label="t('monitor.backup.columns.createdBy')"
              width="100"
            />
            <el-table-column
              :label="t('common.action')"
              width="120"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'SUCCESS'"
                  v-permission="'backup:record:restore'"
                  type="primary"
                  link
                  :disabled="hasRunningRestore"
                  @click="handleRestore(row)"
                >
                  {{ t("monitor.backup.restore") }}
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <EmptyState :title="t('common.no_data')" />
            </template>
          </el-table>

          <el-pagination
            class="pagination"
            v-model:current-page="backupQuery.pageNum"
            v-model:page-size="backupQuery.pageSize"
            :total="backupTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadBackups"
            @current-change="loadBackups"
          />
        </el-tab-pane>

        <el-tab-pane :label="t('monitor.backup.tabs.restores')" name="restores">
          <FilterBar>
            <el-form :inline="true" :model="restoreQuery" class="search-form">
              <el-form-item :label="t('monitor.backup.columns.backupId')">
                <el-input-number
                  v-model="restoreQuery.backupId"
                  :min="1"
                  :controls="false"
                  style="width: 160px"
                  @keyup.enter="handleRestoreSearch"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleRestoreSearch">
                  {{ t("common.search") }}
                </el-button>
                <el-button @click="handleRestoreReset">
                  {{ t("common.reset") }}
                </el-button>
              </el-form-item>
            </el-form>
          </FilterBar>

          <TableToolbar>
            <el-button :loading="loadingRestores" @click="loadAll">
              {{ t("common.reload") }}
            </el-button>
          </TableToolbar>

          <el-table
            :data="restores"
            style="width: 100%"
            v-loading="loadingRestores"
            row-key="id"
          >
            <el-table-column
              prop="id"
              :label="t('monitor.backup.columns.id')"
              width="80"
              align="center"
            />
            <el-table-column
              prop="backupId"
              :label="t('monitor.backup.columns.backupId')"
              width="100"
              align="center"
            />
            <el-table-column
              prop="targetSchema"
              :label="t('monitor.backup.columns.targetSchema')"
              min-width="160"
            />
            <el-table-column
              prop="status"
              :label="t('monitor.backup.columns.status')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag size="small" :type="restoreStatusType(row.status)">
                  {{ t(`monitor.backup.statuses.${row.status}`, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="tableCount"
              :label="t('monitor.backup.columns.tableCount')"
              width="100"
              align="center"
            />
            <el-table-column
              prop="durationMs"
              :label="t('monitor.backup.columns.duration')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                {{ formatDuration(row.durationMs) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="startedAt"
              :label="t('monitor.backup.columns.startedAt')"
              width="170"
            >
              <template #default="{ row }">
                {{ formatTime(row.startedAt) }}
              </template>
            </el-table-column>
            <el-table-column
              :label="t('common.action')"
              width="100"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button type="primary" link @click="openDetail(row)">
                  {{ t("common.view") }}
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <EmptyState :title="t('common.no_data')" />
            </template>
          </el-table>

          <el-pagination
            class="pagination"
            v-model:current-page="restoreQuery.pageNum"
            v-model:page-size="restoreQuery.pageSize"
            :total="restoreTotal"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadRestores"
            @current-change="loadRestores"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="detailVisible"
      :title="t('monitor.backup.detailTitle')"
      width="720px"
    >
      <template v-if="currentRestore">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('monitor.backup.columns.status')">
            <el-tag size="small" :type="restoreStatusType(currentRestore.status)">
              {{
                t(
                  `monitor.backup.statuses.${currentRestore.status}`,
                  currentRestore.status,
                )
              }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="t('monitor.backup.columns.targetSchema')">
            {{ currentRestore.targetSchema }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('monitor.backup.columns.tableCount')">
            {{ currentRestore.tableCount ?? "-" }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('monitor.backup.columns.duration')">
            {{ formatDuration(currentRestore.durationMs) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="currentRestore.errorMessage"
          class="detail-alert"
          type="error"
          :closable="false"
          :title="currentRestore.errorMessage"
        />

        <div class="detail-section-title">
          {{ t("monitor.backup.columns.mismatch") }}
        </div>
        <el-table
          v-if="currentRestore.mismatches.length"
          :data="currentRestore.mismatches"
          size="small"
          border
        >
          <el-table-column
            prop="tableName"
            :label="t('monitor.backup.mismatchTable.tableName')"
          />
          <el-table-column
            prop="expected"
            :label="t('monitor.backup.mismatchTable.expected')"
            width="140"
          />
          <el-table-column
            prop="actual"
            :label="t('monitor.backup.mismatchTable.actual')"
            width="140"
          />
        </el-table>
        <el-empty v-else :description="t('monitor.backup.noMismatch')" />
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createBackup,
  getBackupRecords,
  getBackupSummary,
  getRestoreRecords,
  startRestore,
  type BackupQuery,
  type BackupRecord,
  type BackupSummary,
  type DateTimeValue,
  type RestoreQuery,
  type RestoreRecord,
} from "@/api/backup";
import PageHeader from "@/components/PageHeader.vue";
import FilterBar from "@/components/FilterBar.vue";
import TableToolbar from "@/components/TableToolbar.vue";
import EmptyState from "@/components/EmptyState.vue";

const { t } = useI18n();

const backupStatuses = ["RUNNING", "SUCCESS", "FAILED"];

const activeTab = ref("backups");
const loadingBackups = ref(false);
const loadingRestores = ref(false);
const creating = ref(false);
const backups = ref<BackupRecord[]>([]);
const restores = ref<RestoreRecord[]>([]);
const backupTotal = ref(0);
const restoreTotal = ref(0);
const summary = ref<BackupSummary>({
  enabled: false,
  totalCount: 0,
  totalSize: 0,
  lastSuccessAt: null,
});
const detailVisible = ref(false);
const currentRestore = ref<RestoreRecord | null>(null);

const backupQuery = reactive<BackupQuery>({ pageNum: 1, pageSize: 10 });
const restoreQuery = reactive<RestoreQuery>({ pageNum: 1, pageSize: 10 });

let pollTimer: ReturnType<typeof setInterval> | null = null;

const hasRunningBackup = computed(() =>
  backups.value.some((row) => row.status === "RUNNING"),
);
const hasRunningRestore = computed(() =>
  restores.value.some((row) => row.status === "RUNNING"),
);

const summaryText = computed(() => {
  if (!summary.value.enabled) {
    return t("monitor.backup.disabledTip");
  }
  return t("monitor.backup.summary.overview", {
    count: summary.value.totalCount,
    size: formatSize(summary.value.totalSize),
    time: formatTime(summary.value.lastSuccessAt),
  });
});

const loadSummary = async () => {
  const res = await getBackupSummary();
  if (res.code === 200) {
    summary.value = res.data;
  }
};

const loadBackups = async () => {
  loadingBackups.value = true;
  try {
    const res = await getBackupRecords({ ...backupQuery });
    if (res.code === 200) {
      backups.value = res.data.data;
      backupTotal.value = res.data.total;
    }
  } finally {
    loadingBackups.value = false;
  }
};

const loadRestores = async () => {
  loadingRestores.value = true;
  try {
    const res = await getRestoreRecords({ ...restoreQuery });
    if (res.code === 200) {
      restores.value = res.data.data;
      restoreTotal.value = res.data.total;
    }
  } finally {
    loadingRestores.value = false;
  }
};

const loadAll = async () => {
  await Promise.all([loadSummary(), loadBackups(), loadRestores()]);
};

const handleBackupSearch = () => {
  backupQuery.pageNum = 1;
  loadBackups();
};

const handleBackupReset = () => {
  backupQuery.status = undefined;
  handleBackupSearch();
};

const handleRestoreSearch = () => {
  restoreQuery.pageNum = 1;
  loadRestores();
};

const handleRestoreReset = () => {
  restoreQuery.backupId = undefined;
  handleRestoreSearch();
};

const handleCreate = async () => {
  await ElMessageBox.confirm(
    t("monitor.backup.createConfirm"),
    t("monitor.backup.create"),
    {
      confirmButtonText: t("common.confirm"),
      cancelButtonText: t("common.cancel"),
      type: "warning",
    },
  );
  creating.value = true;
  try {
    const res = await createBackup();
    if (res.code === 200) {
      ElMessage.success(t("monitor.backup.createSuccess"));
      await loadAll();
    }
  } finally {
    creating.value = false;
  }
};

const handleRestore = async (row: BackupRecord) => {
  await ElMessageBox.confirm(
    t("monitor.backup.restoreConfirm", { backupNo: row.backupNo }),
    t("monitor.backup.restore"),
    {
      confirmButtonText: t("common.confirm"),
      cancelButtonText: t("common.cancel"),
      type: "warning",
    },
  );
  const res = await startRestore(row.id);
  if (res.code === 200) {
    ElMessage.success(t("monitor.backup.restoreSuccess"));
    activeTab.value = "restores";
    await loadAll();
  }
};

const openDetail = (row: RestoreRecord) => {
  currentRestore.value = row;
  detailVisible.value = true;
};

const backupStatusType = (status: string) => {
  if (status === "SUCCESS") return "success";
  if (status === "FAILED") return "danger";
  return "warning";
};

const restoreStatusType = (status: string) => {
  if (status === "SUCCESS") return "success";
  if (status === "MISMATCH") return "warning";
  if (status === "FAILED") return "danger";
  return "info";
};

const formatSize = (bytes?: number | null) => {
  if (!bytes || bytes <= 0) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
};

const formatDuration = (ms?: number | null) => {
  if (ms === null || ms === undefined) return "-";
  if (ms < 1000) return `${ms} ms`;
  return `${(ms / 1000).toFixed(1)} s`;
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

const startPolling = () => {
  pollTimer = setInterval(() => {
    if (hasRunningBackup.value || hasRunningRestore.value) {
      void loadAll();
    }
  }, 3000);
};

onMounted(async () => {
  await loadAll();
  startPolling();
});

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
  }
});
</script>

<style scoped>
.content-wrapper {
  padding: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.detail-section-title {
  margin: 16px 0 8px;
  font-weight: 600;
}

.detail-alert {
  margin-top: 12px;
}
</style>
