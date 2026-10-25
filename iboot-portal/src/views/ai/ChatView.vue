<template>
  <div class="ai-chat">
    <aside class="ai-chat__side">
      <el-button class="ai-chat__new" type="primary" @click="startNew">
        {{ t("ai.chat.newConversation") }}
      </el-button>

      <div class="ai-chat__list">
        <button
          v-for="item in conversations"
          :key="item.conversationId"
          class="ai-chat__item"
          :class="{ 'is-active': item.conversationId === conversationId }"
          @click="openConversation(item.conversationId)"
        >
          {{ item.title || t("ai.chat.untitled") }}
        </button>
        <p v-if="conversations.length === 0" class="ai-chat__empty">
          {{ t("ai.chat.noConversations") }}
        </p>
      </div>

      <div class="ai-chat__usage">
        <div class="ai-chat__usage-row">
          <span>{{ t("ai.usage.today") }}</span>
          <strong>{{ usage?.used ?? 0 }} / {{ usage?.limit ?? 0 }}</strong>
        </div>
        <el-progress :percentage="usedPercent" :show-text="false" :stroke-width="6" />
      </div>
    </aside>

    <section class="ai-chat__main">
      <header class="ai-chat__header">
        <span>{{ t("ai.chat.title") }}</span>
        <div v-if="canUseKnowledge" class="ai-chat__toolbar">
          <el-switch v-model="ragMode" :active-text="t('ai.chat.ragMode')" :disabled="streaming" />
          <el-select
            v-if="ragMode"
            v-model="knowledgeBaseId"
            clearable
            :disabled="streaming"
            :placeholder="t('ai.chat.allKnowledgeBases')"
            class="ai-chat__kb-select"
          >
            <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
        </div>
      </header>

      <div ref="messagesEl" class="ai-chat__messages">
        <p v-if="messages.length === 0" class="ai-chat__hint">
          {{ t("ai.chat.empty") }}
        </p>
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="ai-chat__message"
          :class="`is-${message.role.toLowerCase()}`"
        >
          <div class="ai-chat__bubble">{{ message.text }}</div>
          <div
            v-if="index === messages.length - 1 && message.role === 'ASSISTANT' && citations.length > 0"
            class="ai-chat__citations"
          >
            <el-tag
              v-for="item in citations"
              :key="item.index"
              class="ai-chat__citation"
              @click="openCitation(item)"
            >
              [{{ item.index }}] {{ item.docTitle }}
            </el-tag>
          </div>
          <el-tag
            v-if="index === messages.length - 1 && message.role === 'ASSISTANT' && degraded"
            type="warning"
            size="small"
          >
            {{ t("ai.chat.degraded") }}
          </el-tag>
        </div>
      </div>

      <footer class="ai-chat__composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          :placeholder="t('ai.chat.placeholder')"
          @keydown.enter.exact.prevent="send"
        />
        <el-button
          v-if="!streaming"
          type="primary"
          :disabled="!input.trim()"
          @click="send"
        >
          {{ t("ai.chat.send") }}
        </el-button>
        <el-button v-else type="danger" plain @click="stop">
          {{ t("ai.chat.stop") }}
        </el-button>
      </footer>
    </section>

    <el-drawer
      v-model="citationDrawer"
      :title="selectedCitation?.docTitle || t('ai.chat.citation')"
      size="420px"
    >
      <p class="ai-chat__citation-meta">
        <span v-if="selectedCitation?.pageNo">
          {{ t("ai.chat.page", { page: selectedCitation.pageNo }) }}
        </span>
        <el-button link type="primary" @click="downloadCitation">
          {{ t("ai.chat.download") }}
        </el-button>
      </p>
      <p class="ai-chat__citation-snippet">{{ selectedCitation?.snippet }}</p>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { usePermissionStore } from "@/stores/permission";
import {
  getConversations,
  getMessages,
  getUsage,
  type AiMessage,
  type ConversationSummary,
  type UsageSnapshot,
} from "@/api/ai";
import { getDownloadUrl, listKnowledgeBases, type KnowledgeBase } from "@/api/knowledge";
import { streamChat, type AiCitation, type AiStreamEvent } from "@/utils/aiStream";

const { t } = useI18n();
const userStore = useUserStore();
const permissionStore = usePermissionStore();

const conversations = ref<ConversationSummary[]>([]);
const messages = ref<AiMessage[]>([]);
const conversationId = ref("");
const input = ref("");
const streaming = ref(false);
const usage = ref<UsageSnapshot | null>(null);
const messagesEl = ref<HTMLElement>();
let controller: AbortController | null = null;

const knowledgeBases = ref<KnowledgeBase[]>([]);
const ragMode = ref(false);
const knowledgeBaseId = ref<number | undefined>();
const citations = ref<AiCitation[]>([]);
const degraded = ref(false);
const citationDrawer = ref(false);
const selectedCitation = ref<AiCitation | null>(null);
const canUseKnowledge = computed(() => permissionStore.hasPermission("ai:kb:query"));

const usedPercent = computed(() => {
  if (!usage.value || usage.value.limit <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((usage.value.used / usage.value.limit) * 100));
});

onMounted(async () => {
  await Promise.all([loadConversations(), loadUsage()]);
  if (canUseKnowledge.value) {
    await loadKnowledgeBases();
  }
});

async function loadKnowledgeBases(): Promise<void> {
  try {
    knowledgeBases.value = await listKnowledgeBases();
  } catch {
    // 未开通知识库或服务不可用时不影响普通对话
  }
}

async function loadConversations(): Promise<void> {
  try {
    conversations.value = await getConversations();
  } catch {
    ElMessage.error(t("ai.chat.error"));
  }
}

async function loadUsage(): Promise<void> {
  try {
    usage.value = await getUsage();
  } catch {
    // 用量面板失败不阻塞对话
  }
}

async function openConversation(id: string): Promise<void> {
  if (streaming.value) {
    return;
  }
  conversationId.value = id;
  citations.value = [];
  degraded.value = false;
  try {
    messages.value = await getMessages(id);
    scrollToBottom();
  } catch {
    ElMessage.error(t("ai.chat.error"));
  }
}

function startNew(): void {
  if (streaming.value) {
    return;
  }
  conversationId.value = "";
  messages.value = [];
  input.value = "";
  citations.value = [];
  degraded.value = false;
}

async function send(): Promise<void> {
  const content = input.value.trim();
  if (!content || streaming.value) {
    return;
  }
  input.value = "";
  citations.value = [];
  degraded.value = false;
  messages.value.push({ role: "USER", text: content });
  messages.value.push({ role: "ASSISTANT", text: "" });
  streaming.value = true;
  controller = new AbortController();
  scrollToBottom();

  try {
    await streamChat({
      content,
      conversationId: conversationId.value || undefined,
      knowledgeBaseId: ragMode.value ? knowledgeBaseId.value : undefined,
      endpoint: ragMode.value ? "/iboot/ai/rag/chat/stream" : undefined,
      token: userStore.token,
      signal: controller.signal,
      onEvent: handleEvent,
    });
  } catch (error: any) {
    if (error?.name !== "AbortError") {
      ElMessage.error(error?.message || t("ai.chat.error"));
      dropEmptyAssistantMessage();
    }
  } finally {
    streaming.value = false;
    controller = null;
    await Promise.all([loadUsage(), loadConversations()]);
  }
}

function handleEvent(event: AiStreamEvent): void {
  if (event.type === "meta") {
    conversationId.value = event.conversationId;
  } else if (event.type === "citations") {
    citations.value = event.items;
  } else if (event.type === "token") {
    const last = messages.value[messages.value.length - 1];
    if (last && last.role === "ASSISTANT") {
      last.text += event.delta;
      scrollToBottom();
    }
  } else if (event.type === "done") {
    degraded.value = Boolean(event.degraded);
  } else if (event.type === "error") {
    ElMessage.error(event.message);
    dropEmptyAssistantMessage();
  }
}

function openCitation(citation: AiCitation): void {
  selectedCitation.value = citation;
  citationDrawer.value = true;
}

async function downloadCitation(): Promise<void> {
  if (!selectedCitation.value) {
    return;
  }
  try {
    const url = await getDownloadUrl(selectedCitation.value.docId);
    window.open(url, "_blank");
  } catch (error: any) {
    ElMessage.error(error?.message || t("ai.chat.downloadError"));
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
.ai-chat {
  display: flex;
  height: calc(100vh - 140px);
  min-height: 480px;
  overflow: hidden;
  border: 1px solid var(--ib-color-hairline);
  border-radius: var(--ib-radius-lg);
  background: var(--ib-color-surface);
}

.ai-chat__side {
  display: flex;
  width: 260px;
  flex-direction: column;
  gap: var(--ib-space-sm);
  padding: var(--ib-space-md);
  border-right: 1px solid var(--ib-color-hairline);
  background: var(--ib-color-canvas);
}

.ai-chat__new {
  width: 100%;
}

.ai-chat__list {
  flex: 1;
  overflow-y: auto;
}

.ai-chat__item {
  display: block;
  width: 100%;
  margin-bottom: var(--ib-space-xxs);
  padding: var(--ib-space-xs) var(--ib-space-sm);
  overflow: hidden;
  border: 0;
  border-radius: var(--ib-radius-sm);
  background: transparent;
  color: var(--ib-color-ink);
  font-family: var(--ib-font-sans);
  font-size: 13px;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.ai-chat__item:hover,
.ai-chat__item.is-active {
  background: var(--ib-color-surface-soft);
}

.ai-chat__empty {
  color: var(--ib-color-muted);
  font-size: 12px;
  text-align: center;
}

.ai-chat__usage {
  padding-top: var(--ib-space-sm);
  border-top: 1px solid var(--ib-color-hairline);
}

.ai-chat__usage-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--ib-space-xxs);
  color: var(--ib-color-muted);
  font-size: 12px;
}

.ai-chat__main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.ai-chat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ib-space-md);
  padding: var(--ib-space-sm) var(--ib-space-lg);
  border-bottom: 1px solid var(--ib-color-hairline);
  color: var(--ib-color-ink-strong);
  font-weight: 600;
}

.ai-chat__toolbar {
  display: flex;
  align-items: center;
  gap: var(--ib-space-sm);
  font-weight: 400;
}

.ai-chat__kb-select {
  width: 220px;
}

.ai-chat__messages {
  flex: 1;
  padding: var(--ib-space-lg);
  overflow-y: auto;
}

.ai-chat__hint {
  margin-top: 20%;
  color: var(--ib-color-muted);
  text-align: center;
}

.ai-chat__message {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  margin-bottom: var(--ib-space-md);
}

.ai-chat__message.is-user {
  align-items: flex-end;
}

.ai-chat__citations {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ib-space-xxs);
  margin-top: var(--ib-space-xs);
}

.ai-chat__citation {
  cursor: pointer;
}

.ai-chat__citation-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--ib-color-muted);
  font-size: 13px;
}

.ai-chat__citation-snippet {
  color: var(--ib-color-ink);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-chat__bubble {
  max-width: 78%;
  padding: var(--ib-space-sm) var(--ib-space-md);
  border-radius: var(--ib-radius-md);
  background: var(--ib-color-surface-soft);
  color: var(--ib-color-ink);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-chat__message.is-user .ai-chat__bubble {
  background: var(--ib-color-primary);
  color: var(--ib-color-on-primary);
}

.ai-chat__composer {
  display: flex;
  align-items: flex-end;
  gap: var(--ib-space-sm);
  padding: var(--ib-space-md) var(--ib-space-lg);
  border-top: 1px solid var(--ib-color-hairline);
}

.ai-chat__composer :deep(.el-textarea) {
  flex: 1;
}
</style>
