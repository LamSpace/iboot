<template>
  <div class="tempo-monitor-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><Connection /></el-icon>
        <span class="toolbar-title">{{ t("monitor.tempo.title") }}</span>
        <el-tag type="success" size="small" effect="plain">{{
          t("monitor.tempo.tempo")
        }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip :content="t('monitor.tempo.open_in_new')" placement="top">
          <el-button
            :icon="TopRight"
            circle
            size="small"
            @click="openInNewWindow"
          />
        </el-tooltip>
        <el-tooltip :content="t('monitor.tempo.refresh')" placement="top">
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
      :src="tempoUrl"
      :error-text="t('common.load_failed')"
      :retry-text="t('common.reload')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { Connection, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";
import IframePanel from "@/components/IframePanel.vue";

const { t } = useI18n();

const defaultUrl = "https://localhost:3000";
const grafanaUrl = ref(defaultUrl);
const iframePanel = ref<InstanceType<typeof IframePanel>>();

const tempoDatasource = { type: "tempo", uid: "tempo" };

const tempoUrl = computed(
  () => `${grafanaUrl.value.replace(/\/+$/, "")}/explore?${exploreQuery()}`,
);

function exploreQuery(): string {
  const panes = {
    A: {
      datasource: "tempo",
      queries: [
        {
          refId: "A",
          datasource: tempoDatasource,
          queryType: "traceqlSearch",
          limit: 20,
          tableType: "traces",
        },
      ],
      range: { from: "now-1h", to: "now" },
    },
  };
  return `orgId=1&kiosk&schemaVersion=1&panes=${encodeURIComponent(
    JSON.stringify(panes),
  )}`;
}

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
  window.open(tempoUrl.value, "_blank");
}

onMounted(async () => {
  await loadUrl();
});
</script>

<style scoped>
.tempo-monitor-wrapper {
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
