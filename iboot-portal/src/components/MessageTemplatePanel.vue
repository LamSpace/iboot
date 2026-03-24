<template>
  <div>
    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button
        type="primary"
        v-permission="'message:template:add'"
        @click="handleAdd"
        >{{ t("message.messageCenter.template.add") }}</el-button
      >
    </div>

    <!-- 搜索表单 -->
    <el-form :inline="true" :model="queryParams" class="search-form">
      <el-form-item :label="t('message.messageCenter.template.template_code')">
        <el-input
          v-model="queryParams.templateCode"
          :placeholder="
            t('message.messageCenter.template.placeholder.template_code')
          "
          clearable
        />
      </el-form-item>
      <el-form-item :label="t('message.messageCenter.template.template_name')">
        <el-input
          v-model="queryParams.templateName"
          :placeholder="
            t('message.messageCenter.template.placeholder.template_name')
          "
          clearable
        />
      </el-form-item>
      <el-form-item :label="t('message.messageCenter.template.status')">
        <el-select
          v-model="queryParams.status"
          :placeholder="t('common.please_select')"
          clearable
        >
          <el-option
            :label="t('message.messageCenter.template.enabled')"
            :value="1"
          />
          <el-option
            :label="t('message.messageCenter.template.disabled')"
            :value="0"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">{{
          t("message.messageCenter.template.search")
        }}</el-button>
        <el-button @click="handleReset">{{
          t("message.messageCenter.template.reset")
        }}</el-button>
      </el-form-item>
    </el-form>

    <!-- 数据表格 -->
    <el-table :data="templateList" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column
        prop="templateCode"
        :label="t('message.messageCenter.template.template_code')"
        width="160"
        show-overflow-tooltip
      />
      <el-table-column
        prop="templateName"
        :label="t('message.messageCenter.template.template_name')"
        min-width="180"
        show-overflow-tooltip
      />
      <el-table-column
        prop="messageType"
        :label="t('message.messageCenter.table.type')"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            :type="
              dictStore.getDictListClass('sys_message_type', row.messageType)
            "
          >
            {{ dictStore.getDictLabel("sys_message_type", row.messageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="status"
        :label="t('message.messageCenter.template.status')"
        width="80"
        align="center"
      >
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">
            {{
              row.status === 1
                ? t("message.messageCenter.template.enabled")
                : t("message.messageCenter.template.disabled")
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="createBy"
        :label="t('message.messageCenter.table.create_by')"
        width="120"
      />
      <el-table-column
        prop="createTime"
        :label="t('message.messageCenter.table.create_time')"
        width="180"
      />
      <el-table-column
        :label="t('message.messageCenter.table.actions')"
        width="200"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            v-permission="'message:template:edit'"
            type="primary"
            link
            @click="handleEdit(row)"
            >{{ t("message.messageCenter.template.edit") }}</el-button
          >
          <el-button
            v-permission="'message:template:remove'"
            type="danger"
            link
            @click="handleDelete(row)"
            >{{ t("message.messageCenter.template.delete") }}</el-button
          >
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item
          :label="t('message.messageCenter.template.template_code')"
          prop="templateCode"
        >
          <el-input
            v-model="form.templateCode"
            :placeholder="
              t('message.messageCenter.template.placeholder.template_code')
            "
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.template.template_name')"
          prop="templateName"
        >
          <el-input
            v-model="form.templateName"
            :placeholder="
              t('message.messageCenter.template.placeholder.template_name')
            "
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.table.type')"
          prop="messageType"
        >
          <el-select
            v-model="form.messageType"
            :placeholder="t('message.messageCenter.dialog.type_placeholder')"
          >
            <el-option
              v-for="item in dictStore.getDict('sys_message_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('message.messageCenter.template.status')">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.template.template_content')"
          prop="templateContent"
        >
          <el-input
            v-model="form.templateContent"
            type="textarea"
            :rows="6"
            :placeholder="
              t('message.messageCenter.template.placeholder.template_content')
            "
          />
        </el-form-item>
        <el-form-item :label="t('message.messageCenter.dialog.remark_label')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('message.messageCenter.dialog.remark_placeholder')"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{
          t("message.messageCenter.template.dialog.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit">{{
          t("message.messageCenter.template.dialog.confirm")
        }}</el-button>
      </template>
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
import {
  getMessageTemplateList,
  addMessageTemplate,
  updateMessageTemplate,
  deleteMessageTemplate,
  type MessageTemplate,
  type MessageTemplateQuery,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";

const { t } = useI18n();
const dictStore = useDictStore();

const loading = ref(false);
const templateList = ref<MessageTemplate[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

const queryParams = reactive<MessageTemplateQuery>({
  pageNum: 1,
  pageSize: 10,
  templateCode: undefined,
  templateName: undefined,
  messageType: undefined,
  status: undefined,
});

const defaultForm: MessageTemplate = {
  templateCode: "",
  templateName: "",
  templateContent: "",
  messageType: "1",
  status: 1,
  remark: "",
};

const form = reactive<MessageTemplate>({ ...defaultForm });

const rules: FormRules = {
  templateCode: [
    {
      required: true,
      message: t(
        "message.messageCenter.template.validation.template_code_required",
      ),
      trigger: "blur",
    },
  ],
  templateName: [
    {
      required: true,
      message: t(
        "message.messageCenter.template.validation.template_name_required",
      ),
      trigger: "blur",
    },
  ],
  messageType: [
    {
      required: true,
      message: t("message.messageCenter.validation.type_required"),
      trigger: "change",
    },
  ],
  templateContent: [
    {
      required: true,
      message: t(
        "message.messageCenter.template.validation.template_content_required",
      ),
      trigger: "blur",
    },
  ],
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getMessageTemplateList(queryParams);
    if (res.code === 200) {
      templateList.value = res.data.data;
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
  queryParams.templateCode = undefined;
  queryParams.templateName = undefined;
  queryParams.messageType = undefined;
  queryParams.status = undefined;
  handleSearch();
};

const resetForm = () => {
  Object.assign(form, defaultForm);
  form.id = undefined;
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = t("message.messageCenter.template.dialog.add_title");
  dialogVisible.value = true;
};

const handleEdit = (row: MessageTemplate) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("message.messageCenter.template.dialog.edit_title");
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  await formRef.value?.validate();
  if (form.id) {
    await updateMessageTemplate(form);
    ElMessage.success(
      t("message.messageCenter.template.messages.update_success"),
    );
  } else {
    await addMessageTemplate(form);
    ElMessage.success(
      t("message.messageCenter.template.messages.save_success"),
    );
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: MessageTemplate) => {
  ElMessageBox.confirm(
    t("message.messageCenter.template.messages.delete_confirm"),
    t("common.confirm"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteMessageTemplate(row.id!);
      ElMessage.success(
        t("message.messageCenter.template.messages.delete_success"),
      );
      loadData();
    })
    .catch(() => {});
};

onMounted(() => {
  dictStore.loadDicts("sys_message_type");
  loadData();
});
</script>

<style scoped>
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
