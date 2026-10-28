import { useUserStore } from "../stores/user";

/**
 * 智能体配置与确认接口。
 *
 * AI 服务返回原生 JSON，不经过 admin 的 CloudEvents 包装，因此这里使用 fetch。
 */

const BASE = "/iboot/ai";

export interface AgentConfig {
  id: number;
  name: string;
  description?: string;
  systemPrompt: string;
  model?: string;
  toolAllowlist: string[];
  enabled: boolean;
}

export interface AgentConfigPayload {
  name: string;
  description?: string;
  systemPrompt: string;
  model?: string;
  toolAllowlist: string[];
  enabled: boolean;
}

export interface AgentTool {
  name: string;
  description: string;
  readOnly: boolean;
  requiredPermission: string;
}

export interface AgentConfirmResult {
  runId: number;
  status: string;
  content: string;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const userStore = useUserStore();
  const headers = new Headers(options.headers);
  headers.set("Authorization", `Bearer ${userStore.token}`);
  headers.set("Content-Type", "application/json");
  const response = await fetch(BASE + path, { ...options, headers });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `HTTP ${response.status}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

/**
 * 查询智能体列表。
 *
 * @param all 是否包含停用项（需要 ai:agent:manage）
 */
export function listAgents(all = false): Promise<AgentConfig[]> {
  return request<AgentConfig[]>(`/agent/agents${all ? "?all=true" : ""}`);
}

/**
 * 查询可配置的工具清单。
 */
export function listAgentTools(): Promise<AgentTool[]> {
  return request<AgentTool[]>("/agent/tools");
}

/**
 * 查询单个智能体配置。
 * @param id 智能体 ID
 */
export function getAgent(id: number): Promise<AgentConfig> {
  return request<AgentConfig>(`/agent/agents/${id}`);
}

/**
 * 新建智能体。
 * @param payload 配置
 */
export function createAgent(payload: AgentConfigPayload): Promise<AgentConfig> {
  return request<AgentConfig>("/agent/agents", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * 更新智能体。
 * @param id 智能体 ID
 * @param payload 配置
 */
export function updateAgent(id: number, payload: AgentConfigPayload): Promise<AgentConfig> {
  return request<AgentConfig>(`/agent/agents/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/**
 * 删除智能体。
 * @param id 智能体 ID
 */
export function deleteAgent(id: number): Promise<void> {
  return request<void>(`/agent/agents/${id}`, { method: "DELETE" });
}

/**
 * 确认或拒绝待确认的写操作。
 * @param runId 运行 ID
 * @param approve 是否批准
 */
export function confirmAgentRun(runId: number, approve: boolean): Promise<AgentConfirmResult> {
  return request<AgentConfirmResult>(`/agent/runs/${runId}/confirm`, {
    method: "POST",
    body: JSON.stringify({ approve }),
  });
}
