import request from "@/utils/request";

export interface RegisterParams {
  username: string;
  password: string;
  confirmPassword: string;
  nickname?: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * CloudEvents 规范响应格式
 * https://github.com/cloudevents/spec
 */
export interface CloudEvent<T> {
  /**
   * CloudEvents 规范版本
   */
  specversion: string;
  /**
   * 事件 ID，确保唯一性
   */
  id: string;
  /**
   * 事件类型，格式如 com.iboot.user.created
   */
  type: string;
  /**
   * 事件源，通常是请求路径
   */
  source: string;
  /**
   * 事件发生时间，ISO 8601 格式
   */
  time: string;
  /**
   * 数据内容类型
   */
  datacontenttype: string;
  /**
   * 实际的业务数据（原有的 ApiResponse 响应）
   */
  data: ApiResponse<T>;
}

/**
 * 用户注册
 */
export function register(data: RegisterParams): Promise<ApiResponse<void>> {
  return request({
    url: "/auth/register",
    method: "post",
    data,
  });
}
