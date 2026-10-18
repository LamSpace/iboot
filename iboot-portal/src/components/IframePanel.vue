<template>
  <div class="iframe-panel">
    <div v-if="failed" class="iframe-panel-state">
      <p class="iframe-panel-message">{{ errorText }}</p>
      <el-button type="primary" @click="reload">{{ retryText }}</el-button>
    </div>
    <div v-else class="iframe-panel-frame" v-loading="loading">
      <iframe
        :key="reloadKey"
        :src="src"
        :title="title"
        class="iframe-panel-content"
        frameborder="0"
        @load="handleLoad"
        @error="handleError"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    src: string;
    title?: string;
    errorText?: string;
    retryText?: string;
    timeout?: number;
  }>(),
  {
    title: "",
    errorText: "The page could not be loaded.",
    retryText: "Reload",
    timeout: 20000,
  },
);

const loading = ref(true);
const failed = ref(false);
const reloadKey = ref(0);
let timer: ReturnType<typeof setTimeout> | undefined;

function startTimer() {
  clearTimer();
  timer = setTimeout(() => {
    if (loading.value) failed.value = true;
  }, props.timeout);
}

function clearTimer() {
  if (timer) {
    clearTimeout(timer);
    timer = undefined;
  }
}

function handleLoad() {
  loading.value = false;
  failed.value = false;
  clearTimer();
}

function handleError() {
  loading.value = false;
  failed.value = true;
  clearTimer();
}

async function checkAvailability() {
  try {
    await fetch(props.src, { mode: "no-cors", cache: "no-store" });
  } catch {
    loading.value = false;
    failed.value = true;
    clearTimer();
  }
}

function reload() {
  failed.value = false;
  loading.value = true;
  reloadKey.value += 1;
  startTimer();
  void checkAvailability();
}

defineExpose({ reload });

watch(
  () => props.src,
  () => {
    failed.value = false;
    loading.value = true;
    startTimer();
    void checkAvailability();
  },
  { immediate: true },
);

onBeforeUnmount(clearTimer);
</script>

<style scoped>
.iframe-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}

.iframe-panel-frame {
  flex: 1;
  min-height: 480px;
  border: 1px solid var(--ib-color-hairline);
  border-radius: var(--ib-radius-xl);
  overflow: hidden;
  background: var(--ib-color-canvas);
}

.iframe-panel-content {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 480px;
}

.iframe-panel-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ib-space-md);
  min-height: 320px;
  border: 1px solid var(--ib-color-hairline);
  border-radius: var(--ib-radius-xl);
  background: var(--ib-color-surface);
}

.iframe-panel-message {
  margin: 0;
  color: var(--ib-color-steel);
}
</style>
