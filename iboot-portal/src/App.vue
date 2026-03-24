<script lang="ts" setup>
import { RouterView } from "vue-router";
import { ElConfigProvider } from "element-plus";
import { useLocale } from "@/composables/useLocale";
import zhCn from "element-plus/es/locale/lang/zh-cn";

const { elementLocale } = useLocale();
</script>

<template>
  <ElConfigProvider :locale="elementLocale">
    <RouterView />
  </ElConfigProvider>
</template>

<style scoped>
/* 全局样式 */
</style>
