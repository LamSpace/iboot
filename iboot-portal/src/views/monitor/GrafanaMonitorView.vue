<template>
  <div class="grafana-monitor-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><Histogram /></el-icon>
        <span class="toolbar-title">{{ t("monitor.grafana.title") }}</span>
        <el-tag type="success" size="small" effect="plain">{{
          t("monitor.grafana.grafana")
        }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip :content="t('monitor.grafana.open_in_new')" placement="top">
          <el-button
            :icon="TopRight"
            circle
            size="small"
            @click="openInNewWindow"
          />
        </el-tooltip>
        <el-tooltip :content="t('monitor.grafana.refresh')" placement="top">
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
      :src="grafanaUrl"
      :error-text="t('common.load_failed')"
      :retry-text="t('common.reload')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { Histogram, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";
import IframePanel from "@/components/IframePanel.vue";

const { t } = useI18n();

const defaultUrl = "http://localhost:3000";
const grafanaUrl = ref(defaultUrl);
const iframePanel = ref<InstanceType<typeof IframePanel>>();

async function loadUrl() {
  try {
    const res = await getConfigByKey("sys.grafana.monitor.url");
    if (res.code === 200 && res.data) {
      grafanaUrl.value = res.data;
    }
  } catch {
    // 配置不存在时使用默认值
  }
}

function openInNewWindow() {
  window.open(grafanaUrl.value, "_blank");
}

onMounted(async () => {
  await loadUrl();
});
</script>

<style scoped>
.grafana-monitor-wrapper {
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
