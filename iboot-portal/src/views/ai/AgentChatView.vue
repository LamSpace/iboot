<template>
  <div class="agent-chat">
    <aside class="agent-chat__side">
      <el-select v-model="agentId" :placeholder="t('ai.agent.select')" :disabled="streaming">
        <el-option v-for="agent in agents" :key="agent.id" :label="agent.name" :value="agent.id" />
      </el-select>
      <el-button class="agent-chat__new" @click="startNew">{{ t("ai.agent.newRun") }}</el-button>
      <p class="agent-chat__hint">{{ t("ai.agent.hint") }}</p>
    </aside>

    <section class="agent-chat__main">
      <header class="agent-chat__header">
        <span>{{ t("ai.agent.title") }}</span>
        <span v-if="activeTool" class="agent-chat__tool">
          {{ t("ai.agent.runningTool", { tool: activeTool }) }}
        </span>
      </header>

      <div ref="messagesEl" class="agent-chat__messages">
        <p v-if="messages.length === 0" class="agent-chat__empty">{{ t("ai.agent.empty") }}</p>
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="agent-chat__message"
          :class="`is-${message.role.toLowerCase()}`"
        >
          <div class="agent-chat__bubble" :class="{ 'is-markdown': message.role === 'ASSISTANT' }">
            <MarkdownContent v-if="message.role === 'ASSISTANT'" :content="message.text" />
            <template v-else>{{ message.text }}</template>
          </div>
          <div v-if="message.steps?.length" class="agent-chat__steps">
            <el-tag v-for="(step, stepIndex) in message.steps" :key="stepIndex" size="small" type="info">
              {{ step }}
            </el-tag>
          </div>
        </div>

        <el-card v-if="pending" class="agent-chat__pending" shadow="never">
          <p>{{ t("ai.agent.pending", { tool: pending.tool }) }}</p>
          <div class="agent-chat__pending-actions">
            <el-button type="primary" size="small" :loading="confirming" @click="decide(true)">
              {{ t("ai.agent.approve") }}
            </el-button>
            <el-button size="small" :loading="confirming" @click="decide(false)">
              {{ t("ai.agent.reject") }}
            </el-button>
          </div>
        </el-card>
      </div>

      <footer class="agent-chat__input">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          :placeholder="t('ai.agent.placeholder')"
          :disabled="streaming"
          @keydown.enter.exact.prevent="send"
        />
        <el-button v-if="!streaming" type="primary" @click="send">{{ t("ai.agent.send") }}</el-button>
        <el-button v-else @click="stop">{{ t("ai.agent.stop") }}</el-button>
      </footer>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import { useUserStore } from "../../stores/user";
import { confirmAgentRun, listAgents, type AgentConfig } from "../../api/agent";
import MarkdownContent from "../../components/MarkdownContent.vue";
import { streamChat, type AiStreamEvent } from "../../utils/aiStream";

interface ChatMessage {
  role: "USER" | "ASSISTANT";
  text: string;
  steps?: string[];
}

interface PendingAction {
  runId: number;
  tool: string;
}

const { t } = useI18n();
const userStore = useUserStore();

const agents = ref<AgentConfig[]>([]);
const agentId = ref<number>();
const messages = ref<ChatMessage[]>([]);
const conversationId = ref<string>();
const pending = ref<PendingAction>();
const activeTool = ref("");
const draft = ref("");
const streaming = ref(false);
const confirming = ref(false);
const messagesEl = ref<HTMLElement>();

let controller: AbortController | null = null;

const selectedAgent = computed(() => agents.value.find((agent) => agent.id === agentId.value));

onMounted(async () => {
  try {
    agents.value = await listAgents();
    agentId.value = agents.value[0]?.id;
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.agent.loadError"));
  }
});

function startNew(): void {
  messages.value = [];
  conversationId.value = undefined;
  pending.value = undefined;
  activeTool.value = "";
}

async function send(): Promise<void> {
  const content = draft.value.trim();
  if (!content || streaming.value) {
    return;
  }
  if (!selectedAgent.value) {
    ElMessage.warning(t("ai.agent.select"));
    return;
  }
  draft.value = "";
  pending.value = undefined;
  messages.value.push({ role: "USER", text: content });
  const assistant: ChatMessage = { role: "ASSISTANT", text: "", steps: [] };
  messages.value.push(assistant);
  streaming.value = true;
  controller = new AbortController();

  try {
    await streamChat({
      content,
      conversationId: conversationId.value,
      agentId: selectedAgent.value.id,
      endpoint: "/iboot/ai/agent/chat/stream",
      token: userStore.token,
      signal: controller.signal,
      onEvent: (event) => handleEvent(event, assistant),
    });
  } catch (error: any) {
    if (error?.name !== "AbortError") {
      ElMessage.error(error?.message || t("ai.agent.error"));
      dropEmptyAssistantMessage();
    }
  } finally {
    streaming.value = false;
    controller = null;
    activeTool.value = "";
  }
}

function handleEvent(event: AiStreamEvent, assistant: ChatMessage): void {
  if (event.type === "meta") {
    conversationId.value = event.conversationId;
    if (event.runId) {
      assistant.steps?.push(t("ai.agent.start"));
    }
  } else if (event.type === "step") {
    activeTool.value = event.tool;
    assistant.steps?.push(t("ai.agent.toolStep", { tool: event.tool }));
    scrollToBottom();
  } else if (event.type === "tool_result") {
    activeTool.value = "";
    assistant.steps?.push(t("ai.agent.toolDone", { tool: event.tool, state: event.state }));
    scrollToBottom();
  } else if (event.type === "pending_approval") {
    pending.value = { runId: event.runId, tool: event.tool };
    scrollToBottom();
  } else if (event.type === "token") {
    assistant.text += event.delta;
    scrollToBottom();
  } else if (event.type === "done") {
    activeTool.value = "";
  } else if (event.type === "error") {
    ElMessage.error(event.message);
    dropEmptyAssistantMessage();
  }
}

async function decide(approve: boolean): Promise<void> {
  if (!pending.value) {
    return;
  }
  confirming.value = true;
  try {
    const result = await confirmAgentRun(pending.value.runId, approve);
    messages.value.push({
      role: "ASSISTANT",
      text: formatConfirmResult(result.status, result.content),
      steps: [t("ai.agent.confirmResult", { status: result.status })],
    });
    pending.value = undefined;
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.agent.error"));
  } finally {
    confirming.value = false;
  }
}

function formatConfirmResult(status: string, content: string): string {
  const title = status === "COMPLETED"
    ? "✅ " + t("ai.agent.executed")
    : status === "REJECTED"
      ? t("ai.agent.rejected")
      : "⚠️ " + t("ai.agent.notCompleted");
  try {
    const parsed = JSON.parse(content);
    return `**${title}**\n\n\`\`\`json\n${JSON.stringify(parsed, null, 2)}\n\`\`\``;
  } catch {
    return `**${title}**\n\n${content}`;
  }
}

function stop(): void {
  controller?.abort();
  streaming.value = false;
}

function dropEmptyAssistantMessage(): void {
  const last = messages.value[messages.value.length - 1];
  if (last && last.role === "ASSISTANT" && !last.text) {
    messages.value.pop();
  }
}

function scrollToBottom(): void {
  nextTick(() => {
    messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight });
  });
}
</script>

<style scoped>
.agent-chat {
  display: flex;
  height: calc(100vh - 140px);
  min-height: 480px;
  overflow: hidden;
}

.agent-chat__side {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 220px;
  padding: 16px;
  border-right: 1px solid var(--el-border-color-lighter);
}

.agent-chat__hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.agent-chat__main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.agent-chat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.agent-chat__tool {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.agent-chat__messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.agent-chat__empty {
  color: var(--el-text-color-secondary);
}

.agent-chat__message {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}

.agent-chat__message.is-user {
  align-items: flex-end;
}

.agent-chat__message.is-assistant {
  align-items: flex-start;
}

.agent-chat__bubble {
  max-width: 80%;
  padding: 8px 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.agent-chat__bubble.is-markdown {
  white-space: normal;
}

.agent-chat__message.is-user .agent-chat__bubble {
  background: var(--el-color-primary-light-9);
}

.agent-chat__steps {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.agent-chat__pending {
  max-width: 420px;
  border-color: var(--el-color-warning-light-5);
}

.agent-chat__pending-actions {
  display: flex;
  gap: 8px;
}

.agent-chat__input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  padding: 12px 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
