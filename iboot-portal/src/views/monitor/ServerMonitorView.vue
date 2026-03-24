<template>
  <div class="server-monitor-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><Monitor /></el-icon>
        <span class="toolbar-title">{{ t("monitor.server.subtitle") }}</span>
        <el-tag type="success" size="small" effect="plain">{{
          t("monitor.server.spring_boot_admin")
        }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip :content="t('monitor.server.open_in_new')" placement="top">
          <el-button
            :icon="TopRight"
            circle
            size="small"
            @click="openInNewWindow"
          />
        </el-tooltip>
        <el-tooltip :content="t('monitor.server.refresh')" placement="top">
          <el-button
            :icon="Refresh"
            circle
            size="small"
            @click="refreshIframe"
          />
        </el-tooltip>
      </div>
    </div>
    <div
      class="monitor-iframe-container"
      v-loading="loading"
      :element-loading-text="t('monitor.server.loading_text')"
    >
      <iframe
        ref="sbaIframe"
        :src="sbaUrl"
        class="monitor-iframe"
        frameborder="0"
        @load="onIframeLoad"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { Monitor, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";

const { t } = useI18n();

/**
 * SBA 监控地址：
 * - 优先从系统配置中获取（配置键：monitor.sba.url）
 * - 开发环境默认：前端 localhost:3000、后端 localhost:8080，iframe 必须直连后端
 * - 生产环境默认：前后端同源，使用相对路径即可
 */
const defaultSbaUrl = import.meta.env.DEV
  ? "http://localhost:8080/sba"
  : "/sba";
const sbaUrl = ref(defaultSbaUrl);
const loading = ref(true);
const sbaIframe = ref<HTMLIFrameElement>();

async function loadSbaUrl() {
  try {
    const res = await getConfigByKey("monitor.sba.url");
    if (res.code === 200 && res.data) {
      sbaUrl.value = res.data;
    }
  } catch {
    // 配置不存在时使用默认值
  }
}

function onIframeLoad() {
  loading.value = false;
}

function openInNewWindow() {
  window.open(sbaUrl.value, "_blank");
}

function refreshIframe() {
  loading.value = true;
  if (sbaIframe.value) {
    sbaIframe.value.src = sbaUrl.value;
  }
}

onMounted(async () => {
  loading.value = true;
  await loadSbaUrl();
});
</script>

<style scoped>
.server-monitor-wrapper {
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
  color: #303133;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.monitor-iframe-container {
  flex: 1;
  background: #fff;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  min-height: 0;
}

.monitor-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
</style>
