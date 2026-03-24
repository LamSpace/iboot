<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 操作按钮 -->
      <div class="action-bar">
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("security.operateLog.export") }}</el-button
        >
        <el-button type="danger" @click="handleClean">{{
          t("security.operateLog.clean")
        }}</el-button>
      </div>
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('security.operateLog.module')">
          <el-input
            v-model="queryParams.title"
            :placeholder="t('security.operateLog.placeholder.title')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('security.operateLog.operatorName')">
          <el-input
            v-model="queryParams.operatorName"
            :placeholder="t('security.operateLog.placeholder.operatorName')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('security.operateLog.businessType')">
          <el-select
            v-model="queryParams.businessType"
            :placeholder="t('security.operateLog.please_select')"
            clearable
          >
            <el-option
              v-for="item in dictStore.getDict('sys_business_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="Number(item.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('security.operateLog.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('security.operateLog.please_select')"
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
        <el-form-item :label="t('security.operateLog.operTime')">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            :range-separator="t('security.operateLog.date_range_separator')"
            :start-placeholder="t('security.operateLog.start_time')"
            :end-placeholder="t('security.operateLog.end_time')"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("security.operateLog.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("security.operateLog.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column
          prop="title"
          :label="t('security.operateLog.module')"
          min-width="120"
          show-overflow-tooltip
        />
        <el-table-column
          prop="businessType"
          :label="t('security.operateLog.businessType')"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass(
                  'sys_business_type',
                  row.businessType,
                )
              "
            >
              {{
                dictStore.getDictLabel("sys_business_type", row.businessType)
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="requestMethod"
          :label="t('security.operateLog.requestMethod')"
          width="100"
        />
        <el-table-column
          prop="operatorName"
          :label="t('security.operateLog.operatorName')"
          width="120"
        />
        <el-table-column
          prop="operIp"
          :label="t('security.operateLog.operIp')"
          width="130"
        />
        <el-table-column
          prop="status"
          :label="t('security.operateLog.status')"
          width="80"
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
          prop="costTime"
          :label="t('security.operateLog.costTime')"
          width="100"
        />
        <el-table-column
          prop="operTime"
          :label="t('security.operateLog.operTime')"
          width="180"
        />
        <el-table-column
          :label="t('security.operateLog.action')"
          width="100"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">{{
              t("security.operateLog.detail")
            }}</el-button>
          </template>
        </el-table-column>
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      :title="t('security.operateLog.detail_title')"
      width="700px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('security.operateLog.module')">{{
          currentLog?.title
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('security.operateLog.businessType')">{{
          dictStore.getDictLabel("sys_business_type", currentLog?.businessType)
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('security.operateLog.requestMethod')">{{
          currentLog?.requestMethod
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('security.operateLog.operatorName')">{{
          currentLog?.operatorName
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('security.operateLog.operIp')">{{
          currentLog?.operIp
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('security.operateLog.operLocation')">{{
          currentLog?.operLocation
        }}</el-descriptions-item>
        <el-descriptions-item
          :label="t('security.operateLog.operUrl')"
          :span="2"
          >{{ currentLog?.operUrl }}</el-descriptions-item
        >
        <el-descriptions-item
          :label="t('security.operateLog.method')"
          :span="2"
          >{{ currentLog?.method }}</el-descriptions-item
        >
        <el-descriptions-item
          :label="t('security.operateLog.operParam')"
          :span="2"
        >
          <div class="code-block">{{ currentLog?.operParam }}</div>
        </el-descriptions-item>
        <el-descriptions-item
          :label="t('security.operateLog.jsonResult')"
          :span="2"
        >
          <div class="code-block">{{ currentLog?.jsonResult }}</div>
        </el-descriptions-item>
        <el-descriptions-item :label="t('security.operateLog.status')">
          <el-tag
            :type="
              dictStore.getDictListClass(
                'sys_common_status',
                currentLog?.status,
              )
            "
          >
            {{
              dictStore.getDictLabel("sys_common_status", currentLog?.status)
            }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('security.operateLog.costTime')"
          >{{ currentLog?.costTime }} ms</el-descriptions-item
        >
        <el-descriptions-item
          :label="t('security.operateLog.operTime')"
          :span="2"
          >{{ currentLog?.operTime }}</el-descriptions-item
        >
        <el-descriptions-item
          v-if="currentLog?.errorMsg"
          :label="t('security.operateLog.errorMsg')"
          :span="2"
        >
          <div class="error-msg">{{ currentLog?.errorMsg }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getOperateLogList,
  getOperateLogById,
  cleanOperateLog,
  exportOperateLogList,
  type OperateLog,
  type OperateLogQuery,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportOperateLogList({
        title: queryParams.title,
        operatorName: queryParams.operatorName,
        businessType: queryParams.businessType,
        status: queryParams.status,
        beginTime: queryParams.beginTime,
        endTime: queryParams.endTime,
      }),
    t("security.operateLog.title"),
  );
};

const loading = ref(false);
const logList = ref<OperateLog[]>([]);
const total = ref(0);
const dateRange = ref<string[]>([]);
const detailVisible = ref(false);
const currentLog = ref<OperateLog | null>(null);

const queryParams = reactive<OperateLogQuery>({
  pageNum: 1,
  pageSize: 10,
  title: "",
  operatorName: "",
  businessType: undefined,
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
    const res = await getOperateLogList(queryParams);
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
  queryParams.title = "";
  queryParams.operatorName = "";
  queryParams.businessType = undefined;
  queryParams.status = undefined;
  dateRange.value = [];
  handleSearch();
};

const handleDetail = async (row: OperateLog) => {
  const res = await getOperateLogById(row.id!);
  if (res.code === 200) {
    currentLog.value = res.data;
    detailVisible.value = true;
  }
};

const handleClean = () => {
  ElMessageBox.confirm(
    t("security.operateLog.confirm_clean"),
    t("security.operateLog.confirm_clean_title"),
    { type: "warning" },
  )
    .then(async () => {
      await cleanOperateLog();
      ElMessage.success(t("security.operateLog.clean_success"));
      loadData();
    })
    .catch(() => {});
};

onMounted(() => {
  dictStore.loadDicts("sys_common_status", "sys_business_type");
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

.code-block {
  max-height: 200px;
  overflow: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}

.error-msg {
  color: #f56c6c;
}
</style>
