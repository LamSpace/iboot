<template>
  <div class="content-wrapper">
    <PageHeader :title="t('approval.title')" />
    <div class="content-body">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane
          v-if="canApprove"
          :label="t('approval.tabs.todo')"
          name="todo"
        />
        <el-tab-pane
          v-if="canApprove"
          :label="t('approval.tabs.done')"
          name="done"
        />
        <el-tab-pane
          v-if="canApply"
          :label="t('approval.tabs.mine')"
          name="mine"
        />
      </el-tabs>

      <TableToolbar>
        <el-button v-if="canApply" type="primary" @click="applyVisible = true">
          {{ t("approval.actions.apply") }}
        </el-button>
        <el-button @click="loadCurrent">{{ t("approval.actions.refresh") }}</el-button>
        <el-select
          v-if="activeTab === 'mine'"
          v-model="mineStatus"
          clearable
          style="width: 160px; margin-left: 8px"
          @change="reloadMine"
        >
          <el-option :label="t('approval.status.PENDING')" value="PENDING" />
          <el-option :label="t('approval.status.APPROVED')" value="APPROVED" />
          <el-option :label="t('approval.status.REJECTED')" value="REJECTED" />
        </el-select>
      </TableToolbar>

      <el-table v-loading="loading" :data="currentItems" border stripe>
        <el-table-column
          :label="activeTab === 'mine' ? t('approval.columns.approver') : t('approval.columns.applicant')"
          min-width="120"
        >
          <template #default="{ row }">
            {{
              activeTab === "mine"
                ? row.approverName || row.approverId
                : row.applicantName || row.applicantId
            }}
          </template>
        </el-table-column>
        <el-table-column :label="t('approval.columns.leaveType')" width="110">
          <template #default="{ row }">
            {{ t(`approval.leaveType.${row.leaveType}`) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('approval.columns.period')" min-width="220">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('approval.columns.reason')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>
        <el-table-column :label="t('approval.columns.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">
              {{ t(`approval.status.${row.status}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          :label="activeTab === 'done' ? t('approval.columns.completedAt') : t('approval.columns.submittedAt')"
          width="180"
        >
          <template #default="{ row }">
            {{ formatTime(activeTab === "done" ? row.completedTime : row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('approval.columns.actions')" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">
              {{ t("approval.actions.detail") }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('approval.empty')" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="currentTotal"
          layout="total, prev, pager, next"
          @current-change="loadCurrent"
        />
      </div>
    </div>

    <LeaveApplyDialog v-model="applyVisible" @success="loadCurrent" />
    <ApprovalDetailDrawer
      v-model="detailVisible"
      :application-id="detailId"
      @decided="loadCurrent"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import PageHeader from "@/components/PageHeader.vue";
import TableToolbar from "@/components/TableToolbar.vue";
import { usePermissionStore } from "@/stores/permission";
import {
  getDoneList,
  getMyApplications,
  getTodoList,
  type ApprovalItem,
} from "@/api/approval";
import LeaveApplyDialog from "./LeaveApplyDialog.vue";
import ApprovalDetailDrawer from "./ApprovalDetailDrawer.vue";

const { t } = useI18n();
const permissionStore = usePermissionStore();

const canApply = computed(() => permissionStore.hasPermission("approval:apply"));
const canApprove = computed(() =>
  permissionStore.hasPermission("approval:approve"),
);

const activeTab = ref(canApprove.value ? "todo" : "mine");
const applyVisible = ref(false);
const detailVisible = ref(false);
const detailId = ref<number | null>(null);

const loading = ref(false);
const todoItems = ref<ApprovalItem[]>([]);
const doneItems = ref<ApprovalItem[]>([]);
const mineItems = ref<ApprovalItem[]>([]);
const todoTotal = ref(0);
const doneTotal = ref(0);
const mineTotal = ref(0);
const pageSize = ref(10);
const todoPage = ref(1);
const donePage = ref(1);
const minePage = ref(1);
const mineStatus = ref<string>("");

const currentItems = computed(() => {
  if (activeTab.value === "todo") return todoItems.value;
  if (activeTab.value === "done") return doneItems.value;
  return mineItems.value;
});

const currentTotal = computed(() => {
  if (activeTab.value === "todo") return todoTotal.value;
  if (activeTab.value === "done") return doneTotal.value;
  return mineTotal.value;
});

const currentPage = computed({
  get: () => {
    if (activeTab.value === "todo") return todoPage.value;
    if (activeTab.value === "done") return donePage.value;
    return minePage.value;
  },
  set: (value: number) => {
    if (activeTab.value === "todo") todoPage.value = value;
    else if (activeTab.value === "done") donePage.value = value;
    else minePage.value = value;
  },
});

const loadCurrent = async () => {
  loading.value = true;
  try {
    if (activeTab.value === "todo") {
      const res = await getTodoList({
        pageNum: todoPage.value,
        pageSize: pageSize.value,
      });
      if (res.code === 200) {
        todoItems.value = res.data.data ?? [];
        todoTotal.value = res.data.total;
      }
    } else if (activeTab.value === "done") {
      const res = await getDoneList({
        pageNum: donePage.value,
        pageSize: pageSize.value,
      });
      if (res.code === 200) {
        doneItems.value = res.data.data ?? [];
        doneTotal.value = res.data.total;
      }
    } else {
      const res = await getMyApplications({
        pageNum: minePage.value,
        pageSize: pageSize.value,
        status: mineStatus.value || undefined,
      });
      if (res.code === 200) {
        mineItems.value = res.data.data ?? [];
        mineTotal.value = res.data.total;
      }
    }
  } finally {
    loading.value = false;
  }
};

const handleTabChange = () => {
  loadCurrent();
};

const reloadMine = () => {
  minePage.value = 1;
  loadCurrent();
};

const openDetail = (id: number) => {
  detailId.value = id;
  detailVisible.value = true;
};

const formatTime = (value?: string) =>
  value ? new Date(value).toLocaleString() : "-";

const statusType = (status: string): "success" | "warning" | "danger" => {
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "warning";
};

onMounted(() => {
  if (permissionStore.menuTree.length === 0) {
    permissionStore.loadUserPermissions();
  }
  loadCurrent();
});
</script>

<style scoped>
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
