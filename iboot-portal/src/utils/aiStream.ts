/**
 * AI 对话流式客户端。
 *
 * 使用 fetch + ReadableStream 解析 SSE，支持 POST 请求体、Authorization 头与 AbortController；
 * 与业务推送的 EventSource 通道完全分离。
 */

export interface AiUsage {
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
}

export interface AiCitation {
  index: number;
  kbId: number;
  docId: number;
  docTitle: string;
  pageNo?: number;
  chunkIndex?: number;
  snippet: string;
}

export type AiStreamEvent =
  | { type: "meta"; conversationId: string; runId?: number; agentId?: number }
  | { type: "citations"; items: AiCitation[] }
  | { type: "step"; seq: number; tool: string }
  | { type: "tool_result"; seq: number; tool: string; state: string }
  | { type: "pending_approval"; runId: number; tool: string; argsDigest: string }
  | { type: "token"; delta: string }
  | {
      type: "done";
      conversationId?: string;
      model?: string;
      usage: AiUsage;
      degraded?: boolean;
      status?: string;
    }
  | { type: "error"; code: string; message: string; retryable: boolean };

export interface AiStreamOptions {
  content: string;
  conversationId?: string;
  knowledgeBaseId?: number;
  agentId?: number;
  endpoint?: string;
  token: string;
  signal?: AbortSignal;
  onEvent: (event: AiStreamEvent) => void;
}

const DEFAULT_ENDPOINT = "/iboot/ai/chat/stream";

/**
 * 发起一次流式对话。
 *
 * @param options 请求参数与事件回调
 * @throws Error 非 2xx 响应或浏览器不支持流式读取时抛出
 */
export async function streamChat(options: AiStreamOptions): Promise<void> {
  const response = await fetch(options.endpoint || DEFAULT_ENDPOINT, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${options.token}`,
    },
    body: JSON.stringify({
      content: options.content,
      conversationId: options.conversationId,
      knowledgeBaseId: options.knowledgeBaseId,
      ...(options.agentId ? { agentId: options.agentId } : {}),
    }),
    signal: options.signal,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new Error(body.message || `HTTP ${response.status}`);
  }
  if (!response.body) {
    throw new Error("当前浏览器不支持流式响应");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  let finished = false;
  while (!finished) {
    const { done, value } = await reader.read();
    if (done) {
      finished = true;
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split("\n\n");
    buffer = blocks.pop() ?? "";
    for (const block of blocks) {
      parseBlock(block, options.onEvent);
    }
  }
  if (buffer.trim()) {
    parseBlock(buffer, options.onEvent);
  }
}

function parseBlock(block: string, onEvent: (event: AiStreamEvent) => void): void {
  let eventName = "message";
  const dataLines: string[] = [];

  for (const rawLine of block.split("\n")) {
    const line = rawLine.replace(/\r$/, "");
    if (line.startsWith("event:")) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trim());
    }
  }
  if (dataLines.length === 0) {
    return;
  }

  let data: any;
  try {
    data = JSON.parse(dataLines.join("\n"));
  } catch {
    return;
  }

  switch (eventName) {
    case "meta":
      onEvent({
        type: "meta",
        conversationId: data.conversationId,
        runId: data.runId,
        agentId: data.agentId,
      });
      break;
    case "step":
      onEvent({ type: "step", seq: data.seq ?? 0, tool: data.tool ?? "" });
      break;
    case "tool_result":
      onEvent({
        type: "tool_result",
        seq: data.seq ?? 0,
        tool: data.tool ?? "",
        state: data.state ?? "UNKNOWN",
      });
      break;
    case "pending_approval":
      onEvent({
        type: "pending_approval",
        runId: data.runId,
        tool: data.tool ?? "",
        argsDigest: data.argsDigest ?? "",
      });
      break;
    case "citations":
      onEvent({
        type: "citations",
        items: Array.isArray(data) ? data : (data.items ?? []),
      });
      break;
    case "token":
      onEvent({ type: "token", delta: data.delta ?? "" });
      break;
    case "done":
      onEvent({
        type: "done",
        conversationId: data.conversationId,
        model: data.model,
        usage: data.usage ?? { promptTokens: 0, completionTokens: 0, totalTokens: 0 },
        degraded: Boolean(data.degraded),
        status: data.status,
      });
      break;
    case "error":
      onEvent({
        type: "error",
        code: data.code ?? "upstream_error",
        message: data.message ?? "模型暂时不可用",
        retryable: Boolean(data.retryable),
      });
      break;
    default:
      break;
  }
}
