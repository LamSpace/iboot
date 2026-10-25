import { useUserStore } from "../stores/user";

/**
 * AI 对话接口。
 *
 * AI 服务返回原生 JSON，不经过 admin 的 CloudEvents 包装，因此这里使用 fetch 而不是共享的 axios 实例。
 */

const BASE = "/iboot/ai";

export interface ConversationSummary {
  conversationId: string;
  title: string;
  updatedAt: number;
}

export interface AiMessage {
  role: string;
  text: string;
}

export interface UsageSnapshot {
  date: string;
  used: number;
  remaining: number;
  limit: number;
}

async function getJson<T>(path: string): Promise<T> {
  const userStore = useUserStore();
  const response = await fetch(BASE + path, {
    headers: {
      Authorization: `Bearer ${userStore.token}`,
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `HTTP ${response.status}`);
  }
  return response.json() as Promise<T>;
}

/**
 * 查询当前用户的会话列表。
 */
export function getConversations(): Promise<ConversationSummary[]> {
  return getJson<ConversationSummary[]>("/conversations");
}

/**
 * 查询指定会话的消息。
 * @param conversationId 会话 ID
 */
export function getMessages(conversationId: string): Promise<AiMessage[]> {
  return getJson<AiMessage[]>(`/conversations/${encodeURIComponent(conversationId)}/messages`);
}

/**
 * 查询当天用量与剩余配额。
 */
export function getUsage(): Promise<UsageSnapshot> {
  return getJson<UsageSnapshot>("/usage");
}
