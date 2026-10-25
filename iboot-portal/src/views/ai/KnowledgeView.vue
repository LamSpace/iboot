<template>
  <div class="knowledge">
    <el-card shadow="never" class="knowledge__card">
      <template #header>
        <div class="knowledge__header">
          <span>{{ t("ai.knowledge.title") }}</span>
          <el-button v-if="canManage" type="primary" @click="openCreate">
            {{ t("ai.knowledge.create") }}
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loadingBases"
        :data="knowledgeBases"
        highlight-current-row
        @current-change="selectKnowledgeBase"
      >
        <el-table-column prop="name" :label="t('ai.knowledge.name')" min-width="180" />
        <el-table-column :label="t('ai.knowledge.visibility')" width="150">
          <template #default="{ row }">{{ visibilityLabel(row.visibility) }}</template>
        </el-table-column>
        <el-table-column prop="ownerUserId" :label="t('ai.knowledge.owner')" width="120" />
        <el-table-column :label="t('ai.knowledge.actions')" width="120">
          <template #default="{ row }">
            <el-button v-if="canManage" link type="danger" @click.stop="removeKnowledgeBase(row)">
              {{ t("ai.knowledge.delete") }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="selectedBase" shadow="never" class="knowledge__card">
      <template #header>
        <div class="knowledge__header">
          <span>{{ selectedBase.name }} · {{ t("ai.knowledge.documents") }}</span>
          <el-upload
            v-if="canManage"
            :show-file-list="false"
            :http-request="handleUpload"
            accept=".pdf,.docx,.txt,.md"
          >
            <el-button type="primary" :loading="uploading">{{ t("ai.knowledge.upload") }}</el-button>
          </el-upload>
        </div>
      </template>

      <el-table v-loading="loadingDocuments" :data="documents">
        <el-table-column prop="fileName" :label="t('ai.knowledge.fileName')" min-width="220" />
        <el-table-column :label="t('ai.knowledge.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" :label="t('ai.knowledge.chunks')" width="100" />
        <el-table-column :label="t('ai.knowledge.error')" min-width="200">
          <template #default="{ row }">{{ row.errorMessage || "-" }}</template>
        </el-table-column>
        <el-table-column :label="t('ai.knowledge.actions')" width="240">
          <template #default="{ row }">
            <el-button
              v-if="canManage && row.status === 'FAILED'"
              link
              type="primary"
              @click="retry(row)"
            >
              {{ t("ai.knowledge.retry") }}
            </el-button>
            <el-button
              v-if="canManage && row.status === 'READY'"
              link
              type="primary"
              @click="reindex(row)"
            >
              {{ t("ai.knowledge.reindex") }}
            </el-button>
            <el-button v-if="canManage" link type="danger" @click="removeDocument(row)">
              {{ t("ai.knowledge.delete") }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="t('ai.knowledge.create')" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item :label="t('ai.knowledge.name')">
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('ai.knowledge.description')">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="512" />
        </el-form-item>
        <el-form-item :label="t('ai.knowledge.visibility')">
          <el-select v-model="form.visibility">
            <el-option
              v-for="option in visibilityOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.visibility === 2" :label="t('ai.knowledge.sharedDepts')">
          <el-input
            v-model="form.sharedDeptText"
            :placeholder="t('ai.knowledge.sharedDeptsHint')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t("ai.knowledge.cancel") }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ t("ai.knowledge.save") }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox, type UploadRequestOptions } from "element-plus";
import { usePermissionStore } from "@/stores/permission";
import {
  createKnowledgeBase,
  deleteDocument,
  deleteKnowledgeBase,
  listDocuments,
  listKnowledgeBases,
  reindexDocument,
  retryDocument,
  uploadDocument,
  type DocumentStatus,
  type KnowledgeBase,
  type KnowledgeDocument,
} from "@/api/knowledge";

const { t } = useI18n();
const permissionStore = usePermissionStore();
const canManage = computed(() => permissionStore.hasPermission("ai:kb:manage"));

const knowledgeBases = ref<KnowledgeBase[]>([]);
const selectedBase = ref<KnowledgeBase | null>(null);
const documents = ref<KnowledgeDocument[]>([]);
const loadingBases = ref(false);
const loadingDocuments = ref(false);
const uploading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const form = ref({ name: "", description: "", visibility: 5, sharedDeptText: "" });
let pollTimer: number | undefined;

const visibilityOptions = computed(() =>
  [1, 2, 3, 4, 5].map((value) => ({ value, label: visibilityLabel(value) })),
);

function visibilityLabel(value: number): string {
  return t(`ai.knowledge.visibilityValues.${value}`);
}

function statusLabel(status: DocumentStatus): string {
  return t(`ai.knowledge.statusValues.${status}`);
}

function statusType(status: DocumentStatus): "success" | "warning" | "danger" | "info" {
  if (status === "READY") return "success";
  if (status === "FAILED") return "danger";
  if (status === "PROCESSING") return "warning";
  return "info";
}

async function loadKnowledgeBases(): Promise<void> {
  loadingBases.value = true;
  try {
    knowledgeBases.value = await listKnowledgeBases();
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.knowledge.error"));
  } finally {
    loadingBases.value = false;
  }
}

function selectKnowledgeBase(row: KnowledgeBase | null): void {
  selectedBase.value = row;
  if (row) {
    void loadDocuments(row.id);
  } else {
    documents.value = [];
    stopPolling();
  }
}

async function loadDocuments(kbId: number): Promise<void> {
  loadingDocuments.value = true;
  try {
    documents.value = await listDocuments(kbId);
    startPollingIfNeeded();
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.knowledge.error"));
  } finally {
    loadingDocuments.value = false;
  }
}

function startPollingIfNeeded(): void {
  stopPolling();
  if (documents.value.some((doc) => doc.status === "PENDING" || doc.status === "PROCESSING")) {
    pollTimer = window.setInterval(() => {
      if (selectedBase.value) {
        void loadDocuments(selectedBase.value.id);
      }
    }, 3000);
  }
}

function stopPolling(): void {
  if (pollTimer) {
    window.clearInterval(pollTimer);
    pollTimer = undefined;
  }
}

function openCreate(): void {
  form.value = { name: "", description: "", visibility: 5, sharedDeptText: "" };
  dialogVisible.value = true;
}

async function save(): Promise<void> {
  if (!form.value.name.trim()) {
    ElMessage.warning(t("ai.knowledge.nameRequired"));
    return;
  }
  saving.value = true;
  try {
    const sharedDeptIds =
      form.value.visibility === 2
        ? form.value.sharedDeptText
            .split(",")
            .map((item) => Number(item.trim()))
            .filter((item) => Number.isFinite(item) && item > 0)
        : [];
    await createKnowledgeBase({
      name: form.value.name.trim(),
      description: form.value.description,
      visibility: form.value.visibility,
      sharedDeptIds,
    });
    dialogVisible.value = false;
    await loadKnowledgeBases();
    ElMessage.success(t("ai.knowledge.created"));
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.knowledge.error"));
  } finally {
    saving.value = false;
  }
}

async function removeKnowledgeBase(row: KnowledgeBase): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t("ai.knowledge.deleteConfirm", { name: row.name }),
      t("ai.knowledge.confirm"),
      { type: "warning" },
    );
    await deleteKnowledgeBase(row.id);
    if (selectedBase.value?.id === row.id) {
      selectedBase.value = null;
      documents.value = [];
      stopPolling();
    }
    await loadKnowledgeBases();
  } catch (error: any) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || t("ai.knowledge.error"));
    }
  }
}

async function handleUpload(options: UploadRequestOptions): Promise<void> {
  const base = selectedBase.value;
  if (!base) {
    return;
  }
  uploading.value = true;
  try {
    const result = await uploadDocument(base.id, options.file);
    if (result.duplicate) {
      ElMessage.info(t("ai.knowledge.duplicate"));
    } else {
      ElMessage.success(t("ai.knowledge.uploaded"));
    }
    await loadDocuments(base.id);
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.knowledge.error"));
  } finally {
    uploading.value = false;
  }
}

async function retry(row: KnowledgeDocument): Promise<void> {
  try {
    await retryDocument(row.id);
    await loadDocuments(row.kbId);
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.knowledge.error"));
  }
}

async function reindex(row: KnowledgeDocument): Promise<void> {
  try {
    await reindexDocument(row.id);
    await loadDocuments(row.kbId);
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.knowledge.error"));
  }
}

async function removeDocument(row: KnowledgeDocument): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t("ai.knowledge.deleteDocumentConfirm", { name: row.fileName }),
      t("ai.knowledge.confirm"),
      { type: "warning" },
    );
    await deleteDocument(row.id);
    await loadDocuments(row.kbId);
  } catch (error: any) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || t("ai.knowledge.error"));
    }
  }
}

onMounted(loadKnowledgeBases);
onBeforeUnmount(stopPolling);
</script>

<style scoped>
.knowledge {
  display: flex;
  flex-direction: column;
  gap: var(--ib-space-md);
}

.knowledge__card {
  border-radius: var(--ib-radius-lg);
}

.knowledge__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ib-space-sm);
}
</style>
