import { ref } from "vue";
import { ElMessage } from "element-plus";
import { downloadFile, generateExportFileName } from "@/utils/export";

/**
 * Excel导出组合式函数
 */
export function useExport() {
  const exportLoading = ref(false);

  /**
   * 执行导出操作
   * @param exportFn 导出API函数（返回Blob）
   * @param fileName 文件名前缀
   */
  const handleExport = async (
    exportFn: () => Promise<Blob>,
    fileName: string,
  ): Promise<void> => {
    exportLoading.value = true;
    try {
      const blob = await exportFn();
      const fullFileName = generateExportFileName(fileName);
      downloadFile(blob, fullFileName);
      ElMessage.success("导出成功");
    } catch (error) {
      console.error("导出失败:", error);
      ElMessage.error("导出失败，请稍后重试");
    } finally {
      exportLoading.value = false;
    }
  };

  return {
    exportLoading,
    handleExport,
  };
}
