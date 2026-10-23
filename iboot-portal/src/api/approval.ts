import request from "../utils/request";
import type { DateTimeValue } from "@/utils/datetime";
import type { ApiResponse } from "./auth";
import type { PageResult } from "./system";

/**
 * 审批列表项。
 */
export interface ApprovalItem {
  /** 申请 ID */
  id: number;
  /** 申请人用户 ID */
  applicantId: number;
  /** 申请人显示名 */
  applicantName?: string;
  /** 审批人用户 ID */
  approverId: number;
  /** 审批人显示名 */
  approverName?: string;
  /** 请假类型 */
  leaveType: string;
  /** 开始时间 */
  startTime: DateTimeValue;
  /** 结束时间 */
  endTime: DateTimeValue;
  /** 请假事由 */
  reason: string;
  /** 状态：PENDING / APPROVED / REJECTED */
  status: "PENDING" | "APPROVED" | "REJECTED";
  /** 审批意见或驳回原因 */
  approveComment?: string;
  /** 提交时间 */
  createTime: DateTimeValue;
  /** 当前任务 ID */
  taskId?: string;
  /** 当前任务名称 */
  taskName?: string;
  /** 任务创建时间 */
  taskCreateTime?: DateTimeValue;
  /** 任务完成时间 */
  completedTime?: DateTimeValue;
}

/**
 * 审批历史节点。
 */
export interface ApprovalHistoryItem {
  /** 节点名称 */
  activityName: string;
  /** 操作人用户 ID */
  actorId?: number;
  /** 操作人显示名 */
  actorName?: string;
  /** 审批结论：null-提交或待审批，true-通过，false-驳回 */
  approved?: boolean | null;
  /** 审批意见或驳回原因 */
  comment?: string;
  /** 节点发生时间 */
  time: DateTimeValue;
}

/**
 * 审批详情。
 */
export interface ApprovalDetail {
  application: ApprovalItem;
  history: ApprovalHistoryItem[];
}

/**
 * 发起请假参数。
 */
export interface LeaveApplyParams {
  approverId: number;
  leaveType: string;
  startTime: string;
  endTime: string;
  reason: string;
}

/**
 * 审批列表查询参数。
 */
export interface ApprovalQuery {
  pageNum?: number;
  pageSize?: number;
  status?: string;
}

/**
 * 发起请假。
 * @param data 请假参数
 */
export function applyLeave(
  data: LeaveApplyParams,
): Promise<ApiResponse<ApprovalItem>> {
  return request({ url: "/approval/leave", method: "post", data });
}

/**
 * 查询我的待办。
 * @param params 分页参数
 */
export function getTodoList(
  params: ApprovalQuery,
): Promise<ApiResponse<PageResult<ApprovalItem>>> {
  return request({ url: "/approval/todo", method: "get", params });
}

/**
 * 查询我的已办。
 * @param params 分页参数
 */
export function getDoneList(
  params: ApprovalQuery,
): Promise<ApiResponse<PageResult<ApprovalItem>>> {
  return request({ url: "/approval/done", method: "get", params });
}

/**
 * 查询我的申请。
 * @param params 分页与状态参数
 */
export function getMyApplications(
  params: ApprovalQuery,
): Promise<ApiResponse<PageResult<ApprovalItem>>> {
  return request({ url: "/approval/mine", method: "get", params });
}

/**
 * 查询审批详情。
 * @param id 申请 ID
 */
export function getApprovalDetail(
  id: number,
): Promise<ApiResponse<ApprovalDetail>> {
  return request({ url: `/approval/${id}`, method: "get" });
}

/**
 * 审批通过。
 * @param id 申请 ID
 * @param comment 审批意见
 */
export function approveApplication(
  id: number,
  comment?: string,
): Promise<ApiResponse<void>> {
  return request({
    url: `/approval/${id}/approve`,
    method: "post",
    data: { comment },
  });
}

/**
 * 审批驳回。
 * @param id 申请 ID
 * @param comment 驳回原因
 */
export function rejectApplication(
  id: number,
  comment: string,
): Promise<ApiResponse<void>> {
  return request({
    url: `/approval/${id}/reject`,
    method: "post",
    data: { comment },
  });
}
