<template>
  <div class="agent-config">
    <header class="agent-config__header">
      <span>{{ t("ai.agentConfig.title") }}</span>
      <el-button type="primary" @click="openCreate">{{ t("ai.agentConfig.create") }}</el-button>
    </header>

    <el-table v-loading="loading" :data="agents" border>
      <el-table-column prop="name" :label="t('ai.agentConfig.name')" width="160" />
      <el-table-column prop="description" :label="t('ai.agentConfig.description')" />
      <el-table-column :label="t('ai.agentConfig.tools')" width="260">
        <template #default="{ row }">
          <el-tag v-for="tool in row.toolAllowlist" :key="tool" size="small" class="agent-config__tag">
            {{ tool }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ai.agentConfig.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? t("ai.agentConfig.enabled") : t("ai.agentConfig.disabled") }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('ai.agentConfig.actions')" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">
            {{ t("ai.agentConfig.edit") }}
          </el-button>
          <el-button link type="danger" @click="remove(row)">
            {{ t("ai.agentConfig.delete") }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? t('ai.agentConfig.edit') : t('ai.agentConfig.create')"
               width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item :label="t('ai.agentConfig.name')" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('ai.agentConfig.description')">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item :label="t('ai.agentConfig.systemPrompt')" required>
          <el-input v-model="form.systemPrompt" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('ai.agentConfig.model')">
          <el-input v-model="form.model" :placeholder="t('ai.agentConfig.modelHint')" />
        </el-form-item>
        <el-form-item :label="t('ai.agentConfig.tools')">
          <el-checkbox-group v-model="form.toolAllowlist">
            <el-checkbox v-for="tool in toolOptions" :key="tool.name" :value="tool.name">
              {{ tool.name }}
              <el-tag v-if="!tool.readOnly" size="small" type="warning">
                {{ t("ai.agentConfig.write") }}
              </el-tag>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item :label="t('ai.agentConfig.enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t("ai.agentConfig.cancel") }}</el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ t("ai.agentConfig.save") }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createAgent,
  deleteAgent,
  listAgentTools,
  listAgents,
  updateAgent,
  type AgentConfig,
  type AgentTool,
} from "../../api/agent";

const { t } = useI18n();

const agents = ref<AgentConfig[]>([]);
const toolOptions = ref<AgentTool[]>([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);

const form = reactive({
  id: 0,
  name: "",
  description: "",
  systemPrompt: "",
  model: "",
  toolAllowlist: [] as string[],
  enabled: true,
});

onMounted(load);

async function load(): Promise<void> {
  loading.value = true;
  try {
    [agents.value, toolOptions.value] = await Promise.all([listAgents(true), listAgentTools()]);
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.agentConfig.error"));
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  Object.assign(form, {
    id: 0,
    name: "",
    description: "",
    systemPrompt: "",
    model: "",
    toolAllowlist: [],
    enabled: true,
  });
  dialogVisible.value = true;
}

function openEdit(row: AgentConfig): void {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    description: row.description ?? "",
    systemPrompt: row.systemPrompt,
    model: row.model ?? "",
    toolAllowlist: [...row.toolAllowlist],
    enabled: row.enabled,
  });
  dialogVisible.value = true;
}

async function save(): Promise<void> {
  saving.value = true;
  try {
    const payload = {
      name: form.name,
      description: form.description,
      systemPrompt: form.systemPrompt,
      model: form.model || undefined,
      toolAllowlist: form.toolAllowlist,
      enabled: form.enabled,
    };
    if (form.id) {
      await updateAgent(form.id, payload);
    } else {
      await createAgent(payload);
    }
    dialogVisible.value = false;
    await load();
    ElMessage.success(t("ai.agentConfig.saved"));
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.agentConfig.error"));
  } finally {
    saving.value = false;
  }
}

async function remove(row: AgentConfig): Promise<void> {
  try {
    await ElMessageBox.confirm(t("ai.agentConfig.deleteConfirm", { name: row.name }),
      t("ai.agentConfig.title"), { type: "warning" });
  } catch {
    return;
  }
  try {
    await deleteAgent(row.id);
    await load();
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.agentConfig.error"));
  }
}
</script>

<style scoped>
.agent-config {
  padding: 16px;
}

.agent-config__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-weight: 600;
}

.agent-config__tag {
  margin-right: 4px;
}
</style>
