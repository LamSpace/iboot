/**
 * 后端 LocalDateTime 在 CloudEvents 响应中的两种序列化形态：
 * ISO 字符串，或 `[year, month, day, hour, minute, second]` 数组。
 */
export type DateTimeValue = string | number[];

/**
 * 将后端日期时间格式化为统一的本地时间字符串。
 *
 * <p>统一使用 {@code YYYY-MM-DD HH:mm:ss}，避免不同页面混用 ISO 字符串、
 * 数组原值和 {@code toLocaleString()} 造成风格不一致。
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
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const pad = (part: number) => String(part).padStart(2, "0");
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  );
}

/**
 * Element Plus 表格列的时间格式化器。
 *
 * <p>直接绑定到 {@code el-table-column} 的 {@code formatter}，让所有表格的
 * 时间列走同一套格式化逻辑。
 *
 * @param _row 行数据
 * @param _column 列定义
 * @param value 单元格原值
 * @returns 统一格式的时间字符串
 */
export function dateTimeColumnFormatter(
  _row: unknown,
  _column: unknown,
  value?: DateTimeValue | null,
): string {
  return formatDateTime(value);
}
