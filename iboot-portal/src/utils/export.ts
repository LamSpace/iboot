/**
 * Excel导出工具函数
 */

/**
 * 下载文件（处理Blob响应）
 * @param blob 文件数据
 * @param fileName 文件名
 */
export function downloadFile(blob: Blob, fileName: string): void {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 生成带时间戳的文件名
 * @param prefix 文件名前缀
 * @param extension 文件扩展名（默认xlsx）
 * @returns 带时间戳的文件名
 */
export function generateExportFileName(prefix: string, extension: string = 'xlsx'): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  const timestamp = `${year}${month}${day}${hours}${minutes}${seconds}`
  return `${prefix}_${timestamp}.${extension}`
}
