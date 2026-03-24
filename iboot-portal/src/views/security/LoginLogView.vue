<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("security.loginLog.export") }}</el-button
        >
        <el-button type="danger" @click="handleClean">{{
          t("security.loginLog.clean")
        }}</el-button>
      </div>
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('security.loginLog.username')">
          <el-input
            v-model="queryParams.username"
            :placeholder="t('security.loginLog.placeholder.username')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('security.loginLog.ipAddress')">
          <el-input
            v-model="queryParams.ipAddress"
            :placeholder="t('security.loginLog.placeholder.ipAddress')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('security.loginLog.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('security.loginLog.please_select')"
            clearable
          >
            <el-option
              v-for="item in dictStore.getDict('sys_common_status')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="Number(item.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('security.loginLog.loginTime')">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            :range-separator="t('security.loginLog.date_range_separator')"
            :start-placeholder="t('security.loginLog.start_time')"
            :end-placeholder="t('security.loginLog.end_time')"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("security.loginLog.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("security.loginLog.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column
          prop="username"
          :label="t('security.loginLog.username')"
          width="120"
        />
        <el-table-column
          prop="ipAddress"
          :label="t('security.loginLog.ipAddress')"
          width="150"
        />
        <el-table-column
          prop="loginLocation"
          :label="t('security.loginLog.loginLocation')"
          width="150"
        />
        <el-table-column
          prop="browser"
          :label="t('security.loginLog.browser')"
          width="150"
        />
        <el-table-column
          prop="os"
          :label="t('security.loginLog.os')"
          width="150"
        />
        <el-table-column
          prop="status"
          :label="t('security.loginLog.status')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass('sys_common_status', row.status)
              "
            >
              {{ dictStore.getDictLabel("sys_common_status", row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="msg"
          :label="t('security.loginLog.msg')"
          show-overflow-tooltip
        />
        <el-table-column
          prop="loginTime"
          :label="t('security.loginLog.loginTime')"
          width="180"
        />
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
import { ref, reactive, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getLoginLogList,
  cleanLoginLog,
  exportLoginLogList,
  type LoginLog,
  type LoginLogQuery,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportLoginLogList({
        username: queryParams.username,
        ipAddress: queryParams.ipAddress,
        status: queryParams.status,
        beginTime: queryParams.beginTime,
        endTime: queryParams.endTime,
      }),
    t("security.loginLog.title"),
  );
};

const loading = ref(false);
const logList = ref<LoginLog[]>([]);
const total = ref(0);
const dateRange = ref<string[]>([]);

const queryParams = reactive<LoginLogQuery>({
  pageNum: 1,
  pageSize: 10,
  username: "",
  ipAddress: "",
  status: undefined,
  beginTime: "",
  endTime: "",
});

watch(dateRange, (val) => {
  queryParams.beginTime = val?.[0] || "";
  queryParams.endTime = val?.[1] || "";
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getLoginLogList(queryParams);
    if (res.code === 200) {
      logList.value = res.data.data;
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
  queryParams.ipAddress = "";
  queryParams.status = undefined;
  dateRange.value = [];
  handleSearch();
};

const handleClean = () => {
  ElMessageBox.confirm(
    t("security.loginLog.confirm_clean"),
    t("security.loginLog.confirm_clean_title"),
    { type: "warning" },
  )
    .then(async () => {
      await cleanLoginLog();
      ElMessage.success(t("security.loginLog.clean_success"));
      loadData();
    })
    .catch(() => {});
};

onMounted(() => {
  dictStore.loadDicts("sys_common_status");
  loadData();
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

.action-bar {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
