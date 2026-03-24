<template>
  <el-dropdown @command="handleLocaleChange">
    <span class="locale-trigger">
      <el-icon><Switch /></el-icon>
      {{ currentLabel }}
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in locales"
          :key="item.value"
          :command="item.value"
        >
          {{ item.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { Switch } from "@element-plus/icons-vue";
import { useLocale } from "@/composables/useLocale";

const { t } = useI18n();
const { currentLocale, changeLocale } = useLocale();

const locales = computed(() => [
  { label: t("common.locale.zh-CN"), value: "zh-CN" },
  { label: t("common.locale.en-US"), value: "en-US" },
]);

const currentLabel = computed(() => {
  return (
    locales.value.find((l) => l.value === currentLocale.value)?.label ||
    t("common.locale.zh-CN")
  );
});

const handleLocaleChange = (locale: string) => {
  changeLocale(locale);
};
</script>

<style scoped>
.locale-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}

.locale-trigger:hover {
  color: var(--el-color-primary);
}
</style>
