<template>
  <div class="content-wrapper">
    <div class="content-body">
      <!-- 搜索表单 -->
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item :label="t('monitor.job.jobName')">
          <el-input
            v-model="queryParams.jobName"
            :placeholder="t('monitor.job.placeholder.jobName')"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item :label="t('monitor.job.jobGroup')">
          <el-select
            v-model="queryParams.jobGroup"
            :placeholder="t('monitor.job.please_select')"
            clearable
          >
            <el-option
              v-for="dict in dictStore.getDict('sys_job_group')"
              :key="dict.dictValue"
              :label="dict.dictLabel"
              :value="dict.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('monitor.job.status')">
          <el-select
            v-model="queryParams.status"
            :placeholder="t('monitor.job.please_select')"
            clearable
          >
            <el-option
              v-for="dict in dictStore.getDict('sys_job_status')"
              :key="dict.dictValue"
              :label="dict.dictLabel"
              :value="Number(dict.dictValue)"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">{{
            t("monitor.job.search")
          }}</el-button>
          <el-button @click="handleReset">{{
            t("monitor.job.reset")
          }}</el-button>
        </el-form-item>
      </el-form>

      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button
          v-permission="'job:add'"
          type="primary"
          :icon="Plus"
          @click="handleAdd"
          >{{ t("monitor.job.add") }}</el-button
        >
        <el-button
          type="success"
          :loading="exportLoading"
          @click="handleExport"
          >{{ t("monitor.job.export") }}</el-button
        >
        <el-button
          v-permission="'job:log'"
          type="info"
          :icon="Document"
          @click="showLogDialog = true"
          >{{ t("monitor.job.log") }}</el-button
        >
      </div>

      <!-- 任务列表 -->
      <el-table
        :data="jobList"
        style="width: 100%"
        v-loading="loading"
        row-key="id"
      >
        <el-table-column prop="id" :label="t('monitor.job.jobId')" width="80" />
        <el-table-column
          prop="jobName"
          :label="t('monitor.job.jobName')"
          width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="jobGroup"
          :label="t('monitor.job.jobGroup')"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="
                dictStore.getDictListClass('sys_job_group', row.jobGroup) ||
                'info'
              "
              size="small"
            >
              {{ dictStore.getDictLabel("sys_job_group", row.jobGroup) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="invokeTarget"
          :label="t('monitor.job.invokeTarget')"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="cronExpression"
          :label="t('monitor.job.cronExpression')"
          width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          :label="t('monitor.job.status')"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
              v-permission="'job:changeStatus'"
            />
          </template>
        </el-table-column>
        <el-table-column
          :label="t('monitor.job.action')"
          width="240"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-permission="'job:edit'"
              type="primary"
              link
              size="small"
              @click="handleEdit(row)"
            >
              {{ t("monitor.job.edit") }}
            </el-button>
            <el-button
              v-permission="'job:run'"
              type="success"
              link
              size="small"
              @click="handleRun(row)"
            >
              {{ t("monitor.job.run") }}
            </el-button>
            <el-button
              v-permission="'job:log'"
              type="info"
              link
              size="small"
              @click="viewJobLog(row)"
            >
              {{ t("monitor.job.view_log") }}
            </el-button>
            <el-button
              v-permission="'job:remove'"
              type="danger"
              link
              size="small"
              @click="handleDelete(row)"
            >
              {{ t("monitor.job.delete") }}
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('monitor.job.jobName')" prop="jobName">
          <el-input
            v-model="form.jobName"
            :placeholder="t('monitor.job.placeholder.jobName')"
          />
        </el-form-item>
        <el-form-item :label="t('monitor.job.jobGroup')" prop="jobGroup">
          <el-select
            v-model="form.jobGroup"
            :placeholder="t('monitor.job.placeholder.jobGroup')"
          >
            <el-option
              v-for="dict in dictStore.getDict('sys_job_group')"
              :key="dict.dictValue"
              :label="dict.dictLabel"
              :value="dict.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="t('monitor.job.invokeTarget')"
          prop="invokeTarget"
        >
          <el-input
            v-model="form.invokeTarget"
            :placeholder="t('monitor.job.target_placeholder')"
          />
          <div class="form-tip">{{ t("monitor.job.target_tip") }}</div>
        </el-form-item>
        <el-form-item
          :label="t('monitor.job.cronExpression')"
          prop="cronExpression"
        >
          <el-input
            v-model="form.cronExpression"
            :placeholder="t('monitor.job.cron_placeholder')"
          />
          <div class="form-tip">
            {{ t("monitor.job.cron_tip") }}
            <el-link
              type="primary"
              href="https://cron.qqe2.com/"
              target="_blank"
              >{{ t("monitor.job.online_generator") }}</el-link
            >
          </div>
        </el-form-item>
        <el-form-item
          :label="t('monitor.job.misfirePolicy')"
          prop="misfirePolicy"
        >
          <el-radio-group v-model="form.misfirePolicy">
            <el-radio
              v-for="dict in dictStore.getDict('sys_job_misfire')"
              :key="dict.dictValue"
              :value="Number(dict.dictValue)"
            >
              {{ dict.dictLabel }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('monitor.job.concurrent')" prop="concurrent">
          <el-radio-group v-model="form.concurrent">
            <el-radio
              v-for="dict in dictStore.getDict('sys_job_concurrent')"
              :key="dict.dictValue"
              :value="Number(dict.dictValue)"
            >
              {{ dict.dictLabel }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('monitor.job.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dictStore.getDict('sys_job_status')"
              :key="dict.dictValue"
              :value="Number(dict.dictValue)"
            >
              {{ dict.dictLabel }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('monitor.job.remark')" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('monitor.job.placeholder.remark')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">{{
          t("monitor.job.cancel")
        }}</el-button>
        <el-button
          type="primary"
          @click="submitForm"
          :loading="submitLoading"
          >{{ t("monitor.job.confirm") }}</el-button
        >
      </template>
    </el-dialog>

    <!-- 执行日志对话框 -->
    <el-dialog
      v-model="showLogDialog"
      :title="t('monitor.job.log_title')"
      width="900px"
      destroy-on-close
    >
      <el-form :inline="true" :model="logQueryParams" class="search-form">
        <el-form-item :label="t('monitor.job.jobName')">
          <el-input
            v-model="logQueryParams.jobName"
            :placeholder="t('monitor.job.please_input')"
            clearable
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item :label="t('monitor.job.status')">
          <el-select
            v-model="logQueryParams.status"
            :placeholder="t('monitor.job.please_select')"
            clearable
            style="width: 120px"
          >
            <el-option :label="t('monitor.job.success')" :value="1" />
            <el-option :label="t('monitor.job.fail')" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadLogData">{{
            t("monitor.job.search")
          }}</el-button>
          <el-button @click="handleLogReset">{{
            t("monitor.job.reset")
          }}</el-button>
          <el-button
            v-permission="'job:log'"
            type="danger"
            @click="handleCleanLog"
            >{{ t("monitor.job.log") }}</el-button
          >
        </el-form-item>
      </el-form>

      <el-table
        :data="logList"
        style="width: 100%"
        v-loading="logLoading"
        max-height="400"
      >
        <el-table-column prop="id" :label="t('monitor.job.jobId')" width="80" />
        <el-table-column
          prop="jobName"
          :label="t('monitor.job.jobName')"
          width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="invokeTarget"
          :label="t('monitor.job.invokeTarget')"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="jobMessage"
          :label="t('monitor.job.jobMessage')"
          width="100"
        />
        <el-table-column
          prop="status"
          :label="t('monitor.job.status')"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{
                row.status === 1
                  ? t("monitor.job.success")
                  : t("monitor.job.fail")
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="costTime"
          :label="t('monitor.job.costTime')"
          width="90"
          align="center"
        />
        <el-table-column
          prop="createTime"
          :label="t('monitor.job.createTime')"
          width="160"
        >
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('monitor.job.action')"
          width="80"
          align="center"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="viewLogDetail(row)"
              >{{ t("monitor.job.log_detail") }}</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="logQueryParams.pageNum"
        v-model:page-size="logQueryParams.pageSize"
        :total="logTotal"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadLogData"
        @current-change="loadLogData"
      />
    </el-dialog>

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="showLogDetailDialog"
      :title="t('monitor.job.log_detail_title')"
      width="700px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('monitor.job.jobId')">{{
          currentLog?.jobId
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.job.jobName')">{{
          currentLog?.jobName
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.job.jobGroup')">{{
          currentLog?.jobGroup
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.job.status')">
          <el-tag
            :type="currentLog?.status === 1 ? 'success' : 'danger'"
            size="small"
          >
            {{
              currentLog?.status === 1
                ? t("monitor.job.success")
                : t("monitor.job.fail")
            }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('monitor.job.startTime')">{{
          formatTime(currentLog?.startTime)
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.job.endTime')">{{
          formatTime(currentLog?.endTime)
        }}</el-descriptions-item>
        <el-descriptions-item :label="t('monitor.job.costTime')"
          >{{ currentLog?.costTime }} ms</el-descriptions-item
        >
        <el-descriptions-item :label="t('monitor.job.jobMessage')">{{
          currentLog?.jobMessage
        }}</el-descriptions-item>
        <el-descriptions-item
          :label="t('monitor.job.invokeTarget')"
          :span="2"
          >{{ currentLog?.invokeTarget }}</el-descriptions-item
        >
        <el-descriptions-item
          v-if="currentLog?.exceptionInfo"
          :label="t('monitor.job.exceptionInfo')"
          :span="2"
        >
          <el-input
            type="textarea"
            :value="currentLog?.exceptionInfo"
            :rows="5"
            readonly
          />
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from "element-plus";
import { Plus, Document } from "@element-plus/icons-vue";
import {
  getJobList,
  getJobById,
  addJob,
  updateJob,
  deleteJob,
  changeJobStatus,
  runJob,
  getJobLogList,
  cleanJobLog,
  exportJobList,
  type Job,
  type JobLog,
  type JobQuery,
  type JobLogQuery,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportJobList({
        jobName: queryParams.jobName || undefined,
        jobGroup: queryParams.jobGroup || undefined,
        status: queryParams.status,
      }),
    t("monitor.job.title"),
  );
};

const loading = ref(false);
const jobList = ref<Job[]>([]);
const total = ref(0);
const showDialog = ref(false);
const dialogTitle = ref("");
const submitLoading = ref(false);
const formRef = ref<FormInstance>();

const queryParams = reactive<JobQuery>({
  pageNum: 1,
  pageSize: 10,
  jobName: "",
  jobGroup: "",
  status: undefined,
});

const defaultForm: Job = {
  jobName: "",
  jobGroup: "DEFAULT",
  invokeTarget: "",
  cronExpression: "",
  misfirePolicy: 3,
  concurrent: 0,
  status: 1,
  remark: "",
};

const form = reactive<Job>({ ...defaultForm });

const rules: FormRules = {
  jobName: [
    {
      required: true,
      message: t("monitor.job.validation.jobName_required"),
      trigger: "blur",
    },
  ],
  jobGroup: [
    {
      required: true,
      message: t("monitor.job.validation.jobGroup_required"),
      trigger: "change",
    },
  ],
  invokeTarget: [
    {
      required: true,
      message: t("monitor.job.validation.invokeTarget_required"),
      trigger: "blur",
    },
  ],
  cronExpression: [
    {
      required: true,
      message: t("monitor.job.validation.cronExpression_required"),
      trigger: "blur",
    },
  ],
};

// 日志相关
const showLogDialog = ref(false);
const logLoading = ref(false);
const logList = ref<JobLog[]>([]);
const logTotal = ref(0);
const showLogDetailDialog = ref(false);
const currentLog = ref<JobLog | null>(null);

const logQueryParams = reactive<JobLogQuery>({
  pageNum: 1,
  pageSize: 10,
  jobId: undefined,
  jobName: "",
  status: undefined,
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getJobList(queryParams);
    if (res.code === 200) {
      jobList.value = res.data.data;
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
  queryParams.jobName = "";
  queryParams.jobGroup = "";
  queryParams.status = undefined;
  handleSearch();
};

const handleAdd = () => {
  dialogTitle.value = t("monitor.job.add_title");
  Object.assign(form, defaultForm);
  form.id = undefined;
  showDialog.value = true;
};

const handleEdit = async (row: Job) => {
  dialogTitle.value = t("monitor.job.edit_title");
  const res = await getJobById(row.id!);
  if (res.code === 200) {
    Object.assign(form, res.data);
    showDialog.value = true;
  }
};

const handleDelete = (row: Job) => {
  ElMessageBox.confirm(
    t("monitor.job.delete_confirm", { jobName: row.jobName }),
    t("monitor.job.delete_title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteJob(row.id!);
      ElMessage.success(t("monitor.job.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handleStatusChange = async (row: Job) => {
  const statusText =
    row.status === 1
      ? t("monitor.job.enable_success")
      : t("monitor.job.pause_success");
  try {
    await changeJobStatus(row.id!, row.status!);
    ElMessage.success(statusText);
  } catch {
    row.status = row.status === 1 ? 0 : 1;
  }
};

const handleRun = (row: Job) => {
  ElMessageBox.confirm(
    t("monitor.job.run_confirm", { jobName: row.jobName }),
    t("monitor.job.run_title"),
    { type: "warning" },
  )
    .then(async () => {
      await runJob(row.id!);
      ElMessage.success(t("monitor.job.run_success"));
    })
    .catch(() => {});
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (form.id) {
      await updateJob(form);
      ElMessage.success(t("monitor.job.edit_success"));
    } else {
      await addJob(form);
      ElMessage.success(t("monitor.job.add_success"));
    }
    showDialog.value = false;
    loadData();
  } finally {
    submitLoading.value = false;
  }
};

// 日志相关方法
const loadLogData = async () => {
  logLoading.value = true;
  try {
    const res = await getJobLogList(logQueryParams);
    if (res.code === 200) {
      logList.value = res.data.data;
      logTotal.value = res.data.total;
    }
  } finally {
    logLoading.value = false;
  }
};

const handleLogReset = () => {
  logQueryParams.jobId = undefined;
  logQueryParams.jobName = "";
  logQueryParams.status = undefined;
  logQueryParams.pageNum = 1;
  loadLogData();
};

const viewJobLog = (row: Job) => {
  logQueryParams.jobId = row.id;
  logQueryParams.jobName = row.jobName;
  logQueryParams.pageNum = 1;
  showLogDialog.value = true;
  loadLogData();
};

const viewLogDetail = (row: JobLog) => {
  currentLog.value = row;
  showLogDetailDialog.value = true;
};

const handleCleanLog = () => {
  ElMessageBox.confirm(
    t("monitor.job.clean_confirm"),
    t("monitor.job.clean_title"),
    { type: "warning" },
  )
    .then(async () => {
      const res = await cleanJobLog({ days: 30 });
      ElMessage.success(t("monitor.job.clean_success", { count: res.data }));
      loadLogData();
    })
    .catch(() => {});
};

const formatTime = (timeStr?: string) => {
  if (!timeStr) return "";
  return timeStr.replace("T", " ").substring(0, 19);
};

onMounted(async () => {
  await dictStore.loadDicts(
    "sys_job_group",
    "sys_job_status",
    "sys_job_misfire",
    "sys_job_concurrent",
  );
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

.search-form {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
