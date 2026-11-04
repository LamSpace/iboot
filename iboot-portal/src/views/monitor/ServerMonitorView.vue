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
      <div v-if="sbaEnabled" class="toolbar-right">
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
            @click="iframePanel?.reload()"
          />
        </el-tooltip>
      </div>
    </div>
    <div v-if="!sbaEnabled" class="monitor-disabled">
      <el-icon :size="32"><Lock /></el-icon>
      <p class="monitor-disabled-title">
        {{ t("monitor.server.internal_only") }}
      </p>
      <p class="monitor-disabled-hint">
        {{ t("monitor.server.internal_only_hint") }}
      </p>
    </div>
    <IframePanel
      v-else
      ref="iframePanel"
      :src="sbaUrl"
      :error-text="t('common.load_failed')"
      :retry-text="t('common.reload')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { Monitor, TopRight, Refresh, Lock } from "@element-plus/icons-vue";
import { getConfigByKey } from "@/api/system";
import IframePanel from "@/components/IframePanel.vue";

const { t } = useI18n();

/**
 * 运维控制台地址与开关：
 * - 地址优先从系统配置中获取（配置键：monitor.sba.url），默认走同源 /sba，
 *   由 Vite 或生产反向代理转发到 iboot-ops；
 * - 开关来自配置键 monitor.sba.enabled；生产环境默认关闭，只允许内网访问。
 */
const sbaUrl = ref("/sba");
const sbaEnabled = ref(import.meta.env.DEV);
const iframePanel = ref<InstanceType<typeof IframePanel>>();

/**
 * 新窗口打开时使用的绝对地址。
 *
 * <p>iframe 必须使用同源路径 /sba，才能通过控制台的 SAMEORIGIN 头；新窗口不受此限制，
 * 开发环境直接指向 iboot-ops 的 8089 端口，避免打开门户自身的 5173 地址。
 */
const externalSbaUrl = computed(() => {
  if (import.meta.env.DEV && sbaUrl.value.startsWith("/")) {
    return `http://localhost:8089${sbaUrl.value}`;
  }
  return sbaUrl.value;
});

async function loadSbaConfig() {
  try {
    const urlRes = await getConfigByKey("monitor.sba.url");
    if (urlRes.code === 200 && urlRes.data) {
      sbaUrl.value = urlRes.data;
    }
  } catch {
    // 地址配置不存在时保留默认值
  }

  try {
    const enabledRes = await getConfigByKey("monitor.sba.enabled");
    if (enabledRes.code === 200 && enabledRes.data !== undefined) {
      sbaEnabled.value = String(enabledRes.data) !== "false";
    }
  } catch {
    // 开关配置不存在时保留环境默认值
  }
}

function openInNewWindow() {
  window.open(externalSbaUrl.value, "_blank");
}

onMounted(async () => {
  await loadSbaConfig();
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

.monitor-disabled {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 480px;
  border: 1px solid var(--ib-color-hairline);
  border-radius: 0 0 8px 8px;
  background: var(--ib-color-canvas);
  color: var(--ib-color-ink);
}

.monitor-disabled-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--ib-color-ink);
}

.monitor-disabled-hint {
  margin: 0;
  font-size: 13px;
}
</style>
