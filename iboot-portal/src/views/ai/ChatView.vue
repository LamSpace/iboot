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
      <header class="ai-chat__header">{{ t("ai.chat.title") }}</header>

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
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useI18n } from "vue-i18n";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import {
  getConversations,
  getMessages,
  getUsage,
  type AiMessage,
  type ConversationSummary,
  type UsageSnapshot,
} from "@/api/ai";
import { streamChat, type AiStreamEvent } from "@/utils/aiStream";

const { t } = useI18n();
const userStore = useUserStore();

const conversations = ref<ConversationSummary[]>([]);
const messages = ref<AiMessage[]>([]);
const conversationId = ref("");
const input = ref("");
const streaming = ref(false);
const usage = ref<UsageSnapshot | null>(null);
const messagesEl = ref<HTMLElement>();
let controller: AbortController | null = null;

const usedPercent = computed(() => {
  if (!usage.value || usage.value.limit <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((usage.value.used / usage.value.limit) * 100));
});

onMounted(async () => {
  await Promise.all([loadConversations(), loadUsage()]);
});

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
}

async function send(): Promise<void> {
  const content = input.value.trim();
  if (!content || streaming.value) {
    return;
  }
  input.value = "";
  messages.value.push({ role: "USER", text: content });
  messages.value.push({ role: "ASSISTANT", text: "" });
  streaming.value = true;
  controller = new AbortController();
  scrollToBottom();

  try {
    await streamChat({
      content,
      conversationId: conversationId.value || undefined,
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
  } else if (event.type === "token") {
    const last = messages.value[messages.value.length - 1];
    if (last && last.role === "ASSISTANT") {
      last.text += event.delta;
      scrollToBottom();
    }
  } else if (event.type === "error") {
    ElMessage.error(event.message);
    dropEmptyAssistantMessage();
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
  padding: var(--ib-space-sm) var(--ib-space-lg);
  border-bottom: 1px solid var(--ib-color-hairline);
  color: var(--ib-color-ink-strong);
  font-weight: 600;
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
  margin-bottom: var(--ib-space-md);
}

.ai-chat__message.is-user {
  justify-content: flex-end;
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
