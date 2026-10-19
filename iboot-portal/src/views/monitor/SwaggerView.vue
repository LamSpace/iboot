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
            @click="iframePanel?.reload()"
          />
        </el-tooltip>
      </div>
    </div>
    <IframePanel
      ref="iframePanel"
      :src="swaggerUrl"
      :error-text="t('common.load_failed')"
      :retry-text="t('common.reload')"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { Document, TopRight, Refresh } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";
import IframePanel from "@/components/IframePanel.vue";

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
const iframePanel = ref<InstanceType<typeof IframePanel>>();

/** 开发环境前后端端口不同，配置中的相对路径需指向后端 */
function resolveSwaggerUrl(url: string) {
  return import.meta.env.DEV && url.startsWith("/")
    ? `http://localhost:8080${url}`
    : url;
}

async function loadSwaggerUrl() {
  try {
    const res = await getConfigByKey("sys.swagger.ui.url");
    if (res.code === 200 && res.data) {
      swaggerUrl.value = resolveSwaggerUrl(res.data);
    }
  } catch {
    // 配置不存在时使用默认值
  }
}

function openInNewWindow() {
  window.open(swaggerUrl.value, "_blank");
}

onMounted(async () => {
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
