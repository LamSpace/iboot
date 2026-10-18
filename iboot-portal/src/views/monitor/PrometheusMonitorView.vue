<template>
  <div class="prometheus-monitor-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><TrendCharts /></el-icon>
        <span class="toolbar-title">{{ t("monitor.prometheus.title") }}</span>
        <el-tag type="warning" size="small" effect="plain">{{
          t("monitor.prometheus.prometheus")
        }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip
          :content="t('monitor.prometheus.open_in_new')"
          placement="top"
        >
          <el-button
            :icon="TopRight"
            circle
            size="small"
            @click="openInNewWindow"
          />
        </el-tooltip>
        <el-tooltip :content="t('monitor.prometheus.refresh')" placement="top">
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
      :src="prometheusUrl"
      :error-text="t('common.load_failed')"
      :retry-text="t('common.reload')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { TrendCharts, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";
import IframePanel from "@/components/IframePanel.vue";

const { t } = useI18n();

const defaultUrl = "http://localhost:9090";
const prometheusUrl = ref(defaultUrl);
const iframePanel = ref<InstanceType<typeof IframePanel>>();

async function loadUrl() {
  try {
    const res = await getConfigByKey("sys.prometheus.monitor.url");
    if (res.code === 200 && res.data) {
      prometheusUrl.value = res.data;
    }
  } catch {
    // 配置不存在时使用默认值
  }
}

function openInNewWindow() {
  window.open(prometheusUrl.value, "_blank");
}

onMounted(async () => {
  await loadUrl();
});
</script>

<style scoped>
.prometheus-monitor-wrapper {
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
  background: #fff;
  padding: 10px 16px;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
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
