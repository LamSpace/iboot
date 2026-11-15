<template>
  <div class="sentinel-console-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><WarningFilled /></el-icon>
        <span class="toolbar-title">{{ t("monitor.sentinel.title") }}</span>
        <el-tag type="warning" size="small" effect="plain">{{
          t("monitor.sentinel.sentinel")
        }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip
          :content="t('monitor.sentinel.open_in_new')"
          placement="top"
        >
          <el-button
            :icon="TopRight"
            circle
            size="small"
            @click="openInNewWindow"
          />
        </el-tooltip>
        <el-tooltip :content="t('monitor.sentinel.refresh')" placement="top">
          <el-button
            :icon="Refresh"
            circle
            size="small"
            :loading="loading"
            @click="refresh"
          />
        </el-tooltip>
      </div>
    </div>

    <IframePanel
      v-if="embed"
      ref="iframePanel"
      :src="consoleUrl"
      :error-text="t('common.load_failed')"
      :retry-text="t('common.reload')"
    />

    <el-card v-else class="info-card" shadow="never">
      <el-result
        icon="info"
        :title="t('monitor.sentinel.embed_disabled')"
        :sub-title="t('monitor.sentinel.embed_disabled_subtitle')"
      >
        <template #extra>
          <el-button type="primary" @click="openInNewWindow">{{
            t("monitor.sentinel.open_console")
          }}</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { WarningFilled, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";
import IframePanel from "@/components/IframePanel.vue";

const { t } = useI18n();

const defaultUrl = "http://localhost:8858";
const consoleUrl = ref(defaultUrl);
const embed = ref(true);
const loading = ref(false);
const iframePanel = ref<InstanceType<typeof IframePanel>>();

async function loadConfig() {
  loading.value = true;
  try {
    const [urlRes, embedRes] = await Promise.all([
      getConfigByKey("sys.sentinel.dashboard.url"),
      getConfigByKey("sys.sentinel.dashboard.embed"),
    ]);
    if (urlRes.code === 200 && urlRes.data) {
      consoleUrl.value = urlRes.data;
    }
    if (embedRes.code === 200 && embedRes.data != null) {
      embed.value = embedRes.data !== "false";
    }
  } catch {
    // 配置不存在时使用默认值
  } finally {
    loading.value = false;
  }
}

function openInNewWindow() {
  window.open(consoleUrl.value, "_blank");
}

async function refresh() {
  await loadConfig();
  if (embed.value) {
    iframePanel.value?.reload();
  }
}

onMounted(loadConfig);
</script>

<style scoped>
.sentinel-console-wrapper {
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

.info-card {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0 0 8px 8px;
}
</style>
