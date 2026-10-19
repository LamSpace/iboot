<template>
  <el-dropdown trigger="click" @command="handleThemeChange">
    <button
      type="button"
      class="theme-trigger"
      :aria-label="t('common.theme.label')"
      :title="currentLabel"
    >
      <el-icon><component :is="currentIcon" /></el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in modes"
          :key="item.value"
          :command="item.value"
          :class="{ 'is-active': mode === item.value }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import { Monitor, Moon, Sunny } from "@element-plus/icons-vue";
import { useTheme, type ThemeMode } from "@/composables/useTheme";

const { t } = useI18n();
const { mode, resolvedTheme, setMode } = useTheme();

const modes = computed(() => [
  { label: t("common.theme.light"), value: "light" as ThemeMode, icon: Sunny },
  { label: t("common.theme.dark"), value: "dark" as ThemeMode, icon: Moon },
  { label: t("common.theme.system"), value: "system" as ThemeMode, icon: Monitor },
]);

const currentIcon = computed(() => {
  const value = mode.value === "system" ? resolvedTheme.value : mode.value;
  return value === "dark" ? Moon : Sunny;
});

const currentLabel = computed(
  () => modes.value.find((item) => item.value === mode.value)?.label ?? "",
);

const handleThemeChange = (value: ThemeMode) => {
  setMode(value);
};
</script>

<style scoped>
.theme-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--ib-radius-full);
  background: transparent;
  color: var(--ib-color-steel);
  cursor: pointer;
  transition:
    color var(--ib-duration-fast) var(--ib-ease),
    background var(--ib-duration-fast) var(--ib-ease),
    border-color var(--ib-duration-fast) var(--ib-ease);
}

.theme-trigger:hover {
  color: var(--ib-color-ink);
  background: var(--ib-color-surface);
}

.theme-trigger:focus-visible {
  outline: 2px solid var(--ib-color-brand-blue-deep);
  outline-offset: 2px;
}

.el-dropdown-menu__item.is-active {
  color: var(--ib-color-ink);
  font-weight: var(--ib-weight-semibold);
}
</style>
