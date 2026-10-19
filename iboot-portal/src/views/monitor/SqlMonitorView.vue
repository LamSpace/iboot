<template>
  <div class="sql-monitor-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><DataAnalysis /></el-icon>
        <span class="toolbar-title">{{ t("monitor.sql.subtitle") }}</span>
        <el-tag type="primary" size="small" effect="plain">{{
          t("monitor.sql.druid_monitor")
        }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip :content="t('monitor.sql.open_in_new')" placement="top">
          <el-button
            :icon="TopRight"
            circle
            size="small"
            @click="openInNewWindow"
          />
        </el-tooltip>
        <el-tooltip :content="t('monitor.sql.refresh')" placement="top">
          <el-button
            :icon="Refresh"
            circle
            size="small"
            @click="iframePanel?.reload()"
          />
        </el-tooltip>
      </div>
    </div>
    <IframePanel
      ref="iframePanel"
      :src="druidUrl"
      :error-text="t('common.load_failed')"
      :retry-text="t('common.reload')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { DataAnalysis, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";
import IframePanel from "@/components/IframePanel.vue";

const { t } = useI18n();

/**
 * Druid 监控地址：
 * - 优先从系统配置中获取（配置键：sys.druid.monitor.url）
 * - 开发环境默认：前端 localhost:3000、后端 localhost:8080，iframe 必须直连后端
 * - 生产环境默认：前后端同源，使用相对路径即可
 */
const defaultDruidUrl = import.meta.env.DEV
  ? "http://localhost:8080/druid"
  : "/druid";
const druidUrl = ref(defaultDruidUrl);
const iframePanel = ref<InstanceType<typeof IframePanel>>();

/** 开发环境前后端端口不同，配置中的相对路径需指向后端 */
function resolveDruidUrl(url: string) {
  return import.meta.env.DEV && url.startsWith("/")
    ? `http://localhost:8080${url}`
    : url;
}

async function loadDruidUrl() {
  try {
    const res = await getConfigByKey("sys.druid.monitor.url");
    if (res.code === 200 && res.data) {
      druidUrl.value = resolveDruidUrl(res.data);
    }
  } catch {
    // 配置不存在时使用默认值
  }
}

function openInNewWindow() {
  window.open(druidUrl.value, "_blank");
}

onMounted(async () => {
  await loadDruidUrl();
});
</script>

<style scoped>
.sql-monitor-wrapper {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  padding: 12px;
  box-sizing: border-box;
}

.monitor-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--ib-color-canvas);
  padding: 10px 16px;
  border-radius: 8px 8px 0 0;
  box-shadow: var(--ib-shadow-1);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ib-color-ink);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
