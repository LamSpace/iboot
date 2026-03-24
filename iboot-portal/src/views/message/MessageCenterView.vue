<template>
  <div class="content-wrapper">
    <div class="content-body">
      <el-tabs v-model="activeTab">
        <el-tab-pane
          :label="t('message.messageCenter.tabs.message')"
          name="message"
        >
          <!-- 操作按钮 -->
          <div class="action-bar">
            <el-button
              type="primary"
              v-permission="'message:add'"
              @click="handleAdd"
              >{{ t("message.messageCenter.actions.add") }}</el-button
            >
            <el-button
              type="success"
              :loading="exportLoading"
              @click="handleExport"
              >{{ t("message.messageCenter.actions.export") }}</el-button
            >
          </div>

          <!-- 搜索表单 -->
          <el-form :inline="true" :model="queryParams" class="search-form">
            <el-form-item :label="t('message.messageCenter.search.title')">
              <el-input
                v-model="queryParams.title"
                :placeholder="t('message.messageCenter.search.placeholder')"
                clearable
              />
            </el-form-item>
            <el-form-item :label="t('message.messageCenter.search.type')">
              <el-select
                v-model="queryParams.messageType"
                :placeholder="t('message.messageCenter.search.placeholder')"
                clearable
              >
                <el-option
                  v-for="item in dictStore.getDict('sys_message_type')"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('message.messageCenter.search.status')">
              <el-select
                v-model="queryParams.status"
                :placeholder="t('message.messageCenter.search.placeholder')"
                clearable
              >
                <el-option
                  v-for="item in dictStore.getDict('sys_message_status')"
                  :key="item.dictValue"
                  :label="item.dictLabel"
                  :value="item.dictValue"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">{{
                t("message.messageCenter.actions.search")
              }}</el-button>
              <el-button @click="handleReset">{{
                t("message.messageCenter.actions.reset")
              }}</el-button>
            </el-form-item>
          </el-form>

          <!-- 数据表格 -->
          <el-table :data="messageList" style="width: 100%" v-loading="loading">
            <el-table-column
              prop="id"
              :label="t('message.messageCenter.table.id')"
              width="80"
              align="center"
            />
            <el-table-column
              prop="title"
              :label="t('message.messageCenter.table.title')"
              min-width="200"
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
                    dictStore.getDictListClass(
                      'sys_message_type',
                      row.messageType,
                    )
                  "
                >
                  {{
                    dictStore.getDictLabel("sys_message_type", row.messageType)
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="priority"
              :label="t('message.messageCenter.table.priority')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="
                    dictStore.getDictListClass(
                      'sys_message_priority',
                      row.priority,
                    )
                  "
                >
                  {{
                    dictStore.getDictLabel("sys_message_priority", row.priority)
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="receiverType"
              :label="t('message.messageCenter.table.receiver_type')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                {{
                  dictStore.getDictLabel(
                    "sys_message_receiver_type",
                    row.receiverType,
                  )
                }}
              </template>
            </el-table-column>
            <el-table-column
              prop="status"
              :label="t('message.messageCenter.table.status')"
              width="100"
              align="center"
            >
              <template #default="{ row }">
                <el-tag
                  :type="
                    dictStore.getDictListClass('sys_message_status', row.status)
                  "
                >
                  {{ dictStore.getDictLabel("sys_message_status", row.status) }}
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
              width="280"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  v-permission="'message:edit'"
                  type="primary"
                  link
                  @click="handleEdit(row)"
                  :disabled="row.status !== '0'"
                  >{{ t("message.messageCenter.actions.edit") }}</el-button
                >
                <el-button
                  v-permission="'message:send'"
                  type="success"
                  link
                  @click="handleSend(row)"
                  v-if="row.status === '0'"
                  >{{ t("message.messageCenter.actions.send") }}</el-button
                >
                <el-button
                  v-permission="'message:send'"
                  type="warning"
                  link
                  @click="handleRevoke(row)"
                  v-if="row.status === '1'"
                  >{{ t("message.messageCenter.actions.revoke") }}</el-button
                >
                <el-button
                  v-permission="'message:remove'"
                  type="danger"
                  link
                  @click="handleDelete(row)"
                  :disabled="row.status !== '0'"
                  >{{ t("message.messageCenter.actions.delete") }}</el-button
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
        </el-tab-pane>

        <el-tab-pane
          :label="t('message.messageCenter.tabs.template')"
          name="template"
        >
          <MessageTemplatePanel />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 新增/编辑消息对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item
          :label="t('message.messageCenter.dialog.title_label')"
          prop="title"
        >
          <el-input
            v-model="form.title"
            :placeholder="t('message.messageCenter.dialog.title_placeholder')"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.dialog.type_label')"
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
        <el-form-item
          :label="t('message.messageCenter.dialog.priority_label')"
          prop="priority"
        >
          <el-radio-group v-model="form.priority">
            <el-radio
              v-for="item in dictStore.getDict('sys_message_priority')"
              :key="item.dictValue"
              :value="item.dictValue"
              >{{ item.dictLabel }}</el-radio
            >
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.dialog.receiver_type_label')"
          prop="receiverType"
        >
          <el-select
            v-model="form.receiverType"
            :placeholder="
              t('message.messageCenter.dialog.receiver_type_placeholder')
            "
            @change="handleReceiverTypeChange"
          >
            <el-option
              v-for="item in dictStore.getDict('sys_message_receiver_type')"
              :key="item.dictValue"
              :label="item.dictLabel"
              :value="item.dictValue"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.dialog.target_user_label')"
          prop="targetUserIds"
          v-if="form.receiverType === '1'"
        >
          <el-select
            v-model="targetUserIds"
            multiple
            filterable
            remote
            :remote-method="searchUsers"
            :placeholder="
              t('message.messageCenter.dialog.target_user_placeholder')
            "
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="
                (user.nickname || user.username) + ' (' + user.username + ')'
              "
              :value="user.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.dialog.content_label')"
          prop="content"
        >
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            :placeholder="t('message.messageCenter.dialog.content_placeholder')"
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
          t("message.messageCenter.dialog.cancel")
        }}</el-button>
        <el-button @click="handleSubmit(false)">{{
          t("message.messageCenter.dialog.save_draft")
        }}</el-button>
        <el-button type="primary" @click="handleSubmit(true)">{{
          t("message.messageCenter.dialog.save_and_send")
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 发送消息对话框 -->
    <el-dialog
      v-model="sendDialogVisible"
      :title="t('message.messageCenter.send_dialog.title')"
      width="500px"
    >
      <el-form :model="sendForm" label-width="100px">
        <el-form-item :label="t('message.messageCenter.dialog.title_label')">
          <el-input :value="sendForm.title" disabled />
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.send_dialog.receiver_type_label')"
        >
          <el-input
            :value="
              dictStore.getDictLabel(
                'sys_message_receiver_type',
                sendForm.receiverType,
              )
            "
            disabled
          />
        </el-form-item>
        <el-form-item
          :label="t('message.messageCenter.send_dialog.target_user_label')"
          v-if="sendForm.receiverType === '1'"
        >
          <el-select
            v-model="sendForm.userIds"
            multiple
            filterable
            remote
            :remote-method="searchSendUsers"
            :placeholder="
              t('message.messageCenter.send_dialog.target_user_placeholder')
            "
            style="width: 100%"
          >
            <el-option
              v-for="user in sendUserOptions"
              :key="user.id"
              :label="
                (user.nickname || user.username) + ' (' + user.username + ')'
              "
              :value="user.id!"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialogVisible = false">{{
          t("message.messageCenter.send_dialog.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleConfirmSend">{{
          t("message.messageCenter.send_dialog.confirm")
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
  getMessageList,
  addMessage,
  updateMessage,
  deleteMessage,
  sendMessage,
  revokeMessage,
  getUserList,
  exportMessageList,
  type Message,
  type MessageQuery,
  type User,
} from "@/api/system";
import { useDictStore } from "@/stores/dict";
import { useExport } from "@/composables/useExport";
import MessageTemplatePanel from "@/components/MessageTemplatePanel.vue";

const { t } = useI18n();
const dictStore = useDictStore();
const { exportLoading, handleExport: performExport } = useExport();

const handleExport = () => {
  performExport(
    () =>
      exportMessageList({
        title: queryParams.title,
        messageType: queryParams.messageType,
        status: queryParams.status,
      }),
    t("message.messageCenter.title"),
  );
};

const activeTab = ref("message");
const loading = ref(false);
const messageList = ref<Message[]>([]);
const total = ref(0);
const dialogVisible = ref(false);
const dialogTitle = ref("");
const formRef = ref<FormInstance>();

const queryParams = reactive<MessageQuery>({
  pageNum: 1,
  pageSize: 10,
  title: undefined,
  messageType: undefined,
  status: undefined,
});

const defaultForm: Message = {
  title: "",
  content: "",
  messageType: "1",
  priority: "0",
  receiverType: "0",
  remark: "",
};

const form = reactive<Message>({ ...defaultForm });

// 新增消息时指定的接收用户
const targetUserIds = ref<number[]>([]);
const userOptions = ref<User[]>([]);

const rules: FormRules = {
  title: [
    {
      required: true,
      message: t("message.messageCenter.validation.title_required"),
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
  receiverType: [
    {
      required: true,
      message: t("message.messageCenter.validation.receiver_type_required"),
      trigger: "change",
    },
  ],
  content: [
    {
      required: true,
      message: t("message.messageCenter.validation.content_required"),
      trigger: "blur",
    },
  ],
};

// 发送对话框（仅用于列表中的"发送"按钮）
const sendDialogVisible = ref(false);
const sendForm = reactive({
  messageId: 0,
  title: "",
  receiverType: "",
  userIds: [] as number[],
});
const sendUserOptions = ref<User[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getMessageList(queryParams);
    if (res.code === 200) {
      messageList.value = res.data.data;
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
  queryParams.title = undefined;
  queryParams.messageType = undefined;
  queryParams.status = undefined;
  handleSearch();
};

const resetForm = () => {
  Object.assign(form, defaultForm);
  form.id = undefined;
  targetUserIds.value = [];
  userOptions.value = [];
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = t("message.messageCenter.dialog.add_title");
  dialogVisible.value = true;
};

const handleEdit = (row: Message) => {
  resetForm();
  Object.assign(form, row);
  dialogTitle.value = t("message.messageCenter.dialog.edit_title");
  dialogVisible.value = true;
};

const handleReceiverTypeChange = () => {
  targetUserIds.value = [];
};

const searchUsers = async (query: string) => {
  if (query.length < 1) return;
  try {
    const res = await getUserList({
      pageNum: 1,
      pageSize: 20,
      username: query,
    });
    if (res.code === 200) {
      userOptions.value = res.data.data;
    }
  } catch {
    // ignore
  }
};

const searchSendUsers = async (query: string) => {
  if (query.length < 1) return;
  try {
    const res = await getUserList({
      pageNum: 1,
      pageSize: 20,
      username: query,
    });
    if (res.code === 200) {
      sendUserOptions.value = res.data.data;
    }
  } catch {
    // ignore
  }
};

const handleSubmit = async (andSend: boolean) => {
  await formRef.value?.validate();
  if (
    andSend &&
    form.receiverType === "1" &&
    targetUserIds.value.length === 0
  ) {
    ElMessage.warning(t("message.messageCenter.messages.select_user_warning"));
    return;
  }
  let messageId: number;
  if (form.id) {
    await updateMessage(form);
    messageId = form.id;
    ElMessage.success(t("message.messageCenter.messages.update_success"));
  } else {
    const res = await addMessage(form);
    messageId = res.data.id!;
    ElMessage.success(t("message.messageCenter.messages.save_success"));
  }
  if (andSend) {
    try {
      await sendMessage({
        messageId,
        userIds: form.receiverType === "1" ? targetUserIds.value : undefined,
      });
      ElMessage.success(t("message.messageCenter.messages.send_success"));
    } catch {
      ElMessage.warning(t("message.messageCenter.messages.send_warning"));
    }
  }
  dialogVisible.value = false;
  loadData();
};

const handleDelete = (row: Message) => {
  ElMessageBox.confirm(
    t("message.messageCenter.messages.delete_confirm"),
    t("message.messageCenter.table.title"),
    { type: "warning" },
  )
    .then(async () => {
      await deleteMessage(row.id!);
      ElMessage.success(t("message.messageCenter.messages.delete_success"));
      loadData();
    })
    .catch(() => {});
};

const handleSend = (row: Message) => {
  sendForm.messageId = row.id!;
  sendForm.title = row.title;
  sendForm.receiverType = row.receiverType;
  sendForm.userIds = [];
  sendUserOptions.value = [];
  sendDialogVisible.value = true;
};

const handleConfirmSend = async () => {
  if (sendForm.receiverType === "1" && sendForm.userIds.length === 0) {
    ElMessage.warning(t("message.messageCenter.messages.select_user_warning"));
    return;
  }
  try {
    await sendMessage({
      messageId: sendForm.messageId,
      userIds: sendForm.receiverType === "1" ? sendForm.userIds : undefined,
    });
    ElMessage.success(t("message.messageCenter.messages.send_success"));
    sendDialogVisible.value = false;
    loadData();
  } catch {
    // error handled by interceptor
  }
};

const handleRevoke = (row: Message) => {
  ElMessageBox.confirm(
    t("message.messageCenter.messages.revoke_confirm"),
    t("message.messageCenter.table.title"),
    { type: "warning" },
  )
    .then(async () => {
      await revokeMessage(row.id!);
      ElMessage.success(t("message.messageCenter.messages.revoke_success"));
      loadData();
    })
    .catch(() => {});
};

onMounted(() => {
  dictStore.loadDicts(
    "sys_message_type",
    "sys_message_priority",
    "sys_message_status",
    "sys_message_sender_type",
    "sys_message_receiver_type",
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
