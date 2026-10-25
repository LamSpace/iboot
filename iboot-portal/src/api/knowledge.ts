import { useUserStore } from "../stores/user";

/**
 * 知识库与文档接口。
 *
 * AI 服务返回原生 JSON，不经过 admin 的 CloudEvents 包装，因此这里使用 fetch。
 */

const BASE = "/iboot/ai";

export type DocumentStatus = "PENDING" | "PROCESSING" | "READY" | "FAILED";

export interface KnowledgeBase {
  id: number;
  name: string;
  description?: string;
  visibility: number;
  ownerUserId: number;
  ownerDeptId?: number;
  sharedDeptIds: number[];
  createTime?: string;
}

export interface KnowledgeBasePayload {
  name: string;
  description?: string;
  visibility: number;
  sharedDeptIds?: number[];
}

export interface KnowledgeDocument {
  id: number;
  kbId: number;
  fileName: string;
  fileExt?: string;
  fileSize?: number;
  status: DocumentStatus;
  errorCode?: string;
  errorMessage?: string;
  chunkCount?: number;
  createTime?: string;
}

export interface KnowledgeChunk {
  chunkIndex: number;
  pageNo?: number;
  headingPath?: string;
  content: string;
}

export interface KnowledgeSubmitResponse {
  documentId: number;
  duplicate: boolean;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const userStore = useUserStore();
  const headers = new Headers(options.headers);
  headers.set("Authorization", `Bearer ${userStore.token}`);
  if (!(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
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
 * 查询当前用户可见的知识库。
 */
export function listKnowledgeBases(): Promise<KnowledgeBase[]> {
  return request<KnowledgeBase[]>("/knowledge-bases");
}

/**
 * 创建知识库。
 * @param payload 创建参数
 */
export function createKnowledgeBase(payload: KnowledgeBasePayload): Promise<KnowledgeBase> {
  return request<KnowledgeBase>("/knowledge-bases", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

/**
 * 更新知识库。
 * @param id 知识库 ID
 * @param payload 更新参数
 */
export function updateKnowledgeBase(
  id: number,
  payload: KnowledgeBasePayload,
): Promise<KnowledgeBase> {
  return request<KnowledgeBase>(`/knowledge-bases/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

/**
 * 删除知识库。
 * @param id 知识库 ID
 */
export function deleteKnowledgeBase(id: number): Promise<void> {
  return request<void>(`/knowledge-bases/${id}`, { method: "DELETE" });
}

/**
 * 查询知识库文档。
 * @param kbId 知识库 ID
 */
export function listDocuments(kbId: number): Promise<KnowledgeDocument[]> {
  return request<KnowledgeDocument[]>(`/knowledge-bases/${kbId}/documents`);
}

/**
 * 查询文档。
 * @param id 文档 ID
 */
export function getDocument(id: number): Promise<KnowledgeDocument> {
  return request<KnowledgeDocument>(`/knowledge-documents/${id}`);
}

/**
 * 上传文档。
 * @param kbId 知识库 ID
 * @param file 文件
 */
export function uploadDocument(kbId: number, file: File): Promise<KnowledgeSubmitResponse> {
  const body = new FormData();
  body.append("file", file);
  return request<KnowledgeSubmitResponse>(`/knowledge-bases/${kbId}/documents`, {
    method: "POST",
    body,
  });
}

/**
 * 重试文档入库。
 * @param id 文档 ID
 */
export function retryDocument(id: number): Promise<void> {
  return request<void>(`/knowledge-documents/${id}/retry`, { method: "POST" });
}

/**
 * 重建文档索引。
 * @param id 文档 ID
 */
export function reindexDocument(id: number): Promise<void> {
  return request<void>(`/knowledge-documents/${id}/reindex`, { method: "POST" });
}

/**
 * 删除文档。
 * @param id 文档 ID
 */
export function deleteDocument(id: number): Promise<void> {
  return request<void>(`/knowledge-documents/${id}`, { method: "DELETE" });
}

/**
 * 查询文档分块。
 * @param id 文档 ID
 */
export function getDocumentChunks(id: number): Promise<KnowledgeChunk[]> {
  return request<KnowledgeChunk[]>(`/knowledge-documents/${id}/chunks`);
}

/**
 * 获取文档下载地址。
 * @param id 文档 ID
 */
export async function getDownloadUrl(id: number): Promise<string> {
  const result = await request<{ url: string }>(`/knowledge-documents/${id}/download`);
  return result.url;
}
