import request from "../utils/request";
import type { ApiResponse } from "./auth";

/**
 * 修改用户语言偏好
 * @param locale 语言代码，如 zh-CN、en-US
 */
export function changeLocale(
  locale: string,
): Promise<ApiResponse<{ locale: string }>> {
  return request({
    url: "/i18n/locale",
    method: "post",
    params: { locale },
  });
}

/**
 * 获取当前语言
 */
export function getCurrentLocale(): Promise<ApiResponse<string>> {
  return request({
    url: "/i18n/locale",
    method: "get",
  });
}
