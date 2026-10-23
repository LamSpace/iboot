import request from "../utils/request";
import type { ApiResponse } from "./auth";
import type { PageResult } from "./system";

/**
 * 后端 LocalDateTime 在 CloudEvents 响应中的两种序列化形态：
 * ISO 字符串，或 `[year, month, day, hour, minute, second]` 数组。
 */
export type DateTimeValue = string | number[];

/**
 * 通知投递记录。
 */
export interface NotificationDelivery {
  /** 投递记录 ID */
  id: number;
  /** 业务事件 ID */
  eventId: string;
  /** 业务事件类型 */
  eventType: string;
  /** 渠道：IN_APP / EMAIL / DINGTALK / SMS */
  channel: string;
  /** 脱敏后的接收人 */
  recipient: string;
  /** 模板编码 */
  templateCode: string;
  /** 消息标题 */
  title: string;
  /** 状态 */
  status: string;
  /** 已尝试次数 */
  attempts: number;
  /** 最大尝试次数 */
  maxAttempts: number;
  /** 下次重试时间 */
  nextRetryTime?: DateTimeValue | null;
  /** 最后错误（已脱敏） */
  lastError?: string | null;
  /** 发送成功时间 */
  sentTime?: DateTimeValue | null;
  /** 创建时间 */
  createTime: DateTimeValue;
}

/**
 * 通知投递查询参数。
 */
export interface NotificationDeliveryQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
  channel?: string;
  eventType?: string;
  recipient?: string;
  startTime?: string;
  endTime?: string;
}

/**
 * 分页查询通知投递记录。
 * @param params 查询参数
 */
export function getNotificationDeliveries(
  params: NotificationDeliveryQuery,
): Promise<ApiResponse<PageResult<NotificationDelivery>>> {
  return request({ url: "/notification/delivery", method: "get", params });
}

/**
 * 人工重试一条投递记录。
 * @param id 投递记录 ID
 */
export function retryNotificationDelivery(
  id: number,
): Promise<ApiResponse<void>> {
  return request({
    url: `/notification/delivery/${id}/retry`,
    method: "post",
  });
}
