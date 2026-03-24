<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 统计信息 -->
      <div class="stats-bar">
        <el-tag type="success" size="large" effect="dark">
          {{ t("monitor.onlineUser.current_online_users") }}: {{ total }}
          {{ t("monitor.onlineUser.users_count") }}
        </el-tag>
        <div class="stats-actions">
          <el-button
            type="success"
            :loading="exportLoading"
            @click="handleExport"
            >{{ t("monitor.onlineUser.export") }}</el-button
          >
          <el-tooltip
            :content="t('monitor.onlineUser.refresh')"
            placement="top"
          >
            <el-button :icon="Refresh" circle @click="loadData" />
          </el-tooltip>
          <el-switch
            v-model="autoRefresh"
            :active-text="t('monitor.onlineUser.auto_refresh')"
            inactive-text=""
            style="margin-left: 12px"
            @change="toggleAutoRefresh"
          />
        </div>
      </div>

      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('monitor.onlineUser.username')">
          <el-input
            v-model="queryParams.username"
            :placeholder="t('monitor.onlineUser.placeholder.username')"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item :label="t('monitor.onlineUser.loginIp')">
          <el-input
            v-model="queryParams.loginIp"
            :placeholder="t('monitor.onlineUser.placeholder.loginIp')"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("monitor.onlineUser.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("monitor.onlineUser.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 在线用户表格 -->
      <el-table
        :data="userList"
        style="width: 100%"
        v-loading="loading"
        row-key="tokenId"
      >
        <el-table-column
          prop="tokenId"
          :label="t('monitor.onlineUser.tokenId')"
          width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="username"
          :label="t('monitor.onlineUser.username')"
          width="120"
        />
        <el-table-column
          prop="nickname"
          :label="t('monitor.onlineUser.nickname')"
          width="120"
        />
        <el-table-column
          prop="deptName"
          :label="t('monitor.onlineUser.dept')"
          width="120"
        />
        <el-table-column
          prop="loginIp"
          :label="t('monitor.onlineUser.loginIp')"
          width="140"
        />
        <el-table-column
          prop="browser"
          :label="t('monitor.onlineUser.browser')"
          width="120"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.browser"
              :type="
                dictStore.getDictListClass('sys_browser_type', row.browser) ||
                'info'
              "
              size="small"
            >
              {{ row.browser }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="os"
          :label="t('monitor.onlineUser.os')"
          width="120"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.os"
              :type="
                dictStore.getDictListClass('sys_os_type', row.os) || 'info'
              "
              size="small"
            >
              {{ row.os }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="loginTime"
          :label="t('monitor.onlineUser.loginTime')"
          width="180"
        >
          <template #default="{ row }">
            {{ formatTime(row.loginTime) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('monitor.onlineUser.action')"
          width="100"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-permission="'online:forceLogout'"
              type="danger"
              link
              size="small"
              @click="handleForceLogout(row)"
            >
              {{ t("monitor.onlineUser.force_logout") }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
import { ref, reactive, onMounted, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import { Refresh } from "@element-plus/icons-vue";
import {
  getOnlineUserList,
  forceLogout,
  getConfigByKey,
  exportOnlineUserList,
  type OnlineUser,
  type OnlineUserQuery,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportOnlineUserList({
        username: queryParams.username,
        loginIp: queryParams.loginIp,
      }),
    t("monitor.onlineUser.title"),
  );
};

const loading = ref(false);
const userList = ref<OnlineUser[]>([]);
const total = ref(0);
const autoRefresh = ref(false);
let refreshTimer: ReturnType<typeof setInterval> | null = null;
const refreshInterval = ref(30);

const queryParams = reactive<OnlineUserQuery>({
  pageNum: 1,
  pageSize: 10,
  username: "",
  loginIp: "",
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getOnlineUserList(queryParams);
    if (res.code === 200) {
      userList.value = res.data.data;
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
  queryParams.username = "";
  queryParams.loginIp = "";
  handleSearch();
};

const handleForceLogout = (row: OnlineUser) => {
  ElMessageBox.confirm(
    t("monitor.onlineUser.force_logout_confirm", { username: row.username }),
    t("monitor.onlineUser.force_logout_title"),
    { type: "warning" },
  )
    .then(async () => {
      if (!row.tokenId) return;
      await forceLogout(row.tokenId);
      ElMessage.success(t("monitor.onlineUser.force_logout_success"));
      loadData();
    })
    .catch(() => {});
};

const formatTime = (timeStr?: string) => {
  if (!timeStr) return "";
  // ISO format: 2024-01-01T12:00:00 -> 2024-01-01 12:00:00
  return timeStr.replace("T", " ").substring(0, 19);
};

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
      loadData();
    }, refreshInterval.value * 1000);
  }
};

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
};

const loadRefreshConfig = async () => {
  try {
    const res = await getConfigByKey("sys.online.refreshInterval");
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

onMounted(async () => {
  await dictStore.loadDicts("sys_browser_type", "sys_os_type");
  await loadRefreshConfig();
  loadData();
});

onUnmounted(() => {
  stopAutoRefresh();
});
</script>

<style scoped>
.content-wrapper {
  padding: 20px;
}

.content-body {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.stats-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stats-actions {
  display: flex;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
