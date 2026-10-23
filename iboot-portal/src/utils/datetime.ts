/**
 * 后端 LocalDateTime 在 CloudEvents 响应中的两种序列化形态：
 * ISO 字符串，或 `[year, month, day, hour, minute, second]` 数组。
 */
export type DateTimeValue = string | number[];

/**
 * 将后端日期时间格式化为本地时间字符串。
 *
 * @param value ISO 字符串、时间数组或空值
 * @returns 本地时间字符串；空值返回 "-"，无法解析时返回原始值
 */
export function formatDateTime(value?: DateTimeValue | null): string {
  if (!value) {
    return "-";
  }
  const date = Array.isArray(value)
    ? new Date(
        value[0] ?? 1970,
        (value[1] ?? 1) - 1,
        value[2] ?? 1,
        value[3] ?? 0,
        value[4] ?? 0,
        value[5] ?? 0,
      )
    : new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}
