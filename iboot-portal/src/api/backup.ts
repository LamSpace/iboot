import request from "../utils/request";
import type { ApiResponse } from "./auth";
import type { PageResult } from "./system";

/**
 * 后端 LocalDateTime 的两种序列化形态：ISO 字符串或数组。
 */
export type DateTimeValue = string | number[];

/**
 * 备份记录。
 */
export interface BackupRecord {
  /** 记录 ID */
  id: number;
  /** 备份编号 */
  backupNo: string;
  /** 触发方式：SCHEDULED / MANUAL */
  triggerType: string;
  /** 状态：RUNNING / SUCCESS / FAILED */
  status: string;
  /** 源数据库名 */
  databaseName: string;
  /** 存储类型 */
  storageType?: string | null;
  /** 加密对象路径 */
  storagePath?: string | null;
  /** 密文大小 */
  ciphertextSize?: number | null;
  /** 明文大小 */
  plaintextSize?: number | null;
  /** 明文 SHA-256 */
  checksumSha256?: string | null;
  /** 加密算法 */
  encryption?: string | null;
  /** 导出工具 */
  dumpTool?: string | null;
  /** 表数量 */
  tableCount?: number | null;
  /** 逐表行数快照 */
  tableStats: Record<string, number>;
  /** 开始时间 */
  startedAt: DateTimeValue;
  /** 结束时间 */
  finishedAt?: DateTimeValue | null;
  /** 耗时（毫秒） */
  durationMs?: number | null;
  /** 脱敏后的错误 */
  errorMessage?: string | null;
  /** 触发人 */
  createdBy?: string | null;
  /** 创建时间 */
  createTime: DateTimeValue;
}

/**
 * 单表行数差异。
 */
export interface TableCountMismatch {
  /** 表名 */
  tableName: string;
  /** 备份时行数 */
  expected: number;
  /** 演练后行数 */
  actual: number;
}

/**
 * 恢复演练记录。
 */
export interface RestoreRecord {
  /** 记录 ID */
  id: number;
  /** 备份记录 ID */
  backupId: number;
  /** 演练库名 */
  targetSchema: string;
  /** 状态：RUNNING / SUCCESS / MISMATCH / FAILED */
  status: string;
  /** 比对表数量 */
  tableCount?: number | null;
  /** 还原后逐表行数 */
  tableStats: Record<string, number>;
  /** 行数差异明细 */
  mismatches: TableCountMismatch[];
  /** 开始时间 */
  startedAt: DateTimeValue;
  /** 结束时间 */
  finishedAt?: DateTimeValue | null;
  /** 耗时（毫秒） */
  durationMs?: number | null;
  /** 脱敏后的错误 */
  errorMessage?: string | null;
  /** 触发人 */
  createdBy?: string | null;
  /** 创建时间 */
  createTime: DateTimeValue;
}

/**
 * 备份概览。
 */
export interface BackupSummary {
  /** 功能是否启用 */
  enabled: boolean;
  /** 备份记录总数 */
  totalCount: number;
  /** 成功备份密文总大小 */
  totalSize: number;
  /** 最近一次成功备份时间 */
  lastSuccessAt?: DateTimeValue | null;
}

/**
 * 备份记录查询参数。
 */
export interface BackupQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
}

/**
 * 演练记录查询参数。
 */
export interface RestoreQuery {
  pageNum?: number;
  pageSize?: number;
  backupId?: number;
}

/**
 * 分页查询备份记录。
 * @param params 查询参数
 */
export function getBackupRecords(
  params: BackupQuery,
): Promise<ApiResponse<PageResult<BackupRecord>>> {
  return request({ url: "/monitor/backup/records", method: "get", params });
}

/**
 * 手动触发一次备份。
 */
export function createBackup(): Promise<ApiResponse<BackupRecord>> {
  return request({ url: "/monitor/backup/records", method: "post" });
}

/**
 * 分页查询恢复演练记录。
 * @param params 查询参数
 */
export function getRestoreRecords(
  params: RestoreQuery,
): Promise<ApiResponse<PageResult<RestoreRecord>>> {
  return request({ url: "/monitor/backup/restores", method: "get", params });
}

/**
 * 对指定备份发起恢复演练。
 * @param id 备份记录 ID
 */
export function startRestore(id: number): Promise<ApiResponse<RestoreRecord>> {
  return request({
    url: `/monitor/backup/records/${id}/restore`,
    method: "post",
  });
}

/**
 * 查询备份概览。
 */
export function getBackupSummary(): Promise<ApiResponse<BackupSummary>> {
  return request({ url: "/monitor/backup/summary", method: "get" });
}
