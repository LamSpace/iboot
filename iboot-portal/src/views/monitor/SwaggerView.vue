<template>
  <div class="swagger-wrapper">
    <div class="swagger-toolbar">
      <div class="toolbar-left">
        <el-icon :size="18"><Document /></el-icon>
        <span class="toolbar-title">{{ t("monitor.swagger.subtitle") }}</span>
        <el-tag type="success" size="small" effect="plain">{{
          t("monitor.swagger.openapi_doc")
        }}</el-tag>
      </div>
      <div class="toolbar-right">
        <el-tooltip :content="t('monitor.swagger.open_in_new')" placement="top">
          <el-button
            :icon="TopRight"
            circle
            size="small"
            @click="openInNewWindow"
          />
        </el-tooltip>
        <el-tooltip :content="t('monitor.swagger.refresh')" placement="top">
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
      class="swagger-iframe-container"
      v-loading="loading"
      :element-loading-text="t('monitor.swagger.loading_text')"
    >
      <iframe
        ref="swaggerIframe"
        :src="swaggerUrl"
        class="swagger-iframe"
        frameborder="0"
        @load="onIframeLoad"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { Document, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";

const { t } = useI18n();

/**
 * Swagger UI 访问地址：
 * - 优先从系统配置中获取（配置键：sys.swagger.ui.url）
 * - 开发环境默认：前端 localhost:3000、后端 localhost:8080，iframe 必须直连后端
 * - 生产环境默认：前后端同源，使用相对路径即可
 */
const defaultSwaggerUrl = import.meta.env.DEV
  ? "http://localhost:8080/swagger-ui.html"
  : "/swagger-ui.html";
const swaggerUrl = ref(defaultSwaggerUrl);
const loading = ref(true);
const swaggerIframe = ref<HTMLIFrameElement>();

async function loadSwaggerUrl() {
  try {
    const res = await getConfigByKey("sys.swagger.ui.url");
    if (res.code === 200 && res.data) {
      swaggerUrl.value = res.data;
    }
  } catch {
    // 配置不存在时使用默认值
  }
}

function onIframeLoad() {
  loading.value = false;
}

function openInNewWindow() {
  window.open(swaggerUrl.value, "_blank");
}

function refreshIframe() {
  loading.value = true;
  if (swaggerIframe.value) {
    swaggerIframe.value.src = swaggerUrl.value;
  }
}

onMounted(async () => {
  loading.value = true;
  await loadSwaggerUrl();
});
</script>

<style scoped>
.swagger-wrapper {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  padding: 12px;
  box-sizing: border-box;
}

.swagger-toolbar {
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

.swagger-iframe-container {
  flex: 1;
  background: #fff;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  min-height: 0;
}

.swagger-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
</style>
