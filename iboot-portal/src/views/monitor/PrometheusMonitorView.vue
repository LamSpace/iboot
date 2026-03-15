<template>
  <div class="prometheus-monitor-wrapper">
    <div class="monitor-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><TrendCharts /></el-icon>
        <span class="toolbar-title">Prometh监控</span>
        <el-tag type="warning" size="small" effect="plain">Prometheus</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip content="在新窗口中打开" placement="top">
          <el-button :icon="TopRight" circle size="small" @click="openInNewWindow" />
        </el-tooltip>
        <el-tooltip content="刷新监控页面" placement="top">
          <el-button :icon="Refresh" circle size="small" @click="refreshIframe" />
        </el-tooltip>
      </div>
    </div>
    <div class="monitor-iframe-container" v-loading="loading" element-loading-text="加载监控页面中...">
      <iframe
        ref="prometheusIframe"
        :src="prometheusUrl"
        class="monitor-iframe"
        frameborder="0"
        @load="onIframeLoad"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { TrendCharts, TopRight, Refresh } from '@element-plus/icons-vue'
import { getConfigByKey } from '@/api/system'

const defaultUrl = 'http://localhost:9090'
const prometheusUrl = ref(defaultUrl)
const loading = ref(true)
const prometheusIframe = ref<HTMLIFrameElement>()

async function loadUrl() {
  try {
    const res = await getConfigByKey('sys.prometheus.monitor.url')
    if (res.code === 200 && res.data) {
      prometheusUrl.value = res.data
    }
  } catch {
    // 配置不存在时使用默认值
  }
}

function onIframeLoad() {
  loading.value = false
}

function openInNewWindow() {
  window.open(prometheusUrl.value, '_blank')
}

function refreshIframe() {
  loading.value = true
  if (prometheusIframe.value) {
    prometheusIframe.value.src = prometheusUrl.value
  }
}

onMounted(async () => {
  loading.value = true
  await loadUrl()
})
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
