<template>
  <div class="metric-card" :class="`accent-${accent}`">
    <div class="metric-card-head">
      <span class="metric-card-label">{{ label }}</span>
      <el-icon v-if="icon" class="metric-card-icon">
        <component :is="icon" />
      </el-icon>
    </div>
    <div class="metric-card-value">{{ value }}</div>
    <div v-if="trend" class="metric-card-trend">{{ trend }}</div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from "vue";

withDefaults(
  defineProps<{
    label: string;
    value: string | number;
    trend?: string;
    accent?: "ink" | "coral" | "magenta" | "blue" | "purple" | "cyan";
    icon?: Component;
  }>(),
  {
    trend: "",
    accent: "ink",
    icon: undefined,
  },
);
</script>

<style scoped>
.metric-card {
  padding: var(--ib-space-lg);
  border: 1px solid var(--ib-color-hairline);
  border-radius: var(--ib-radius-xl);
  background: var(--ib-color-canvas);
  border-top: 2px solid var(--ib-color-ink);
}

.metric-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ib-space-sm);
}

.metric-card-label {
  font-size: var(--ib-text-caption);
  color: var(--ib-color-steel);
}

.metric-card-icon {
  font-size: 18px;
  color: var(--ib-color-ink);
}

.metric-card-value {
  margin-top: var(--ib-space-sm);
  font-size: var(--ib-text-heading-sm);
  font-weight: var(--ib-weight-semibold);
  line-height: 1.2;
  color: var(--ib-color-ink);
}

.metric-card-trend {
  margin-top: var(--ib-space-xxs);
  font-size: var(--ib-text-micro);
  color: var(--ib-color-steel);
}

.accent-coral {
  border-top-color: var(--ib-color-brand-coral);
}

.accent-coral .metric-card-icon {
  color: var(--ib-color-brand-coral);
}

.accent-magenta {
  border-top-color: var(--ib-color-brand-magenta);
}

.accent-magenta .metric-card-icon {
  color: var(--ib-color-brand-magenta);
}

.accent-blue {
  border-top-color: var(--ib-color-brand-blue);
}

.accent-blue .metric-card-icon {
  color: var(--ib-color-brand-blue);
}

.accent-purple {
  border-top-color: var(--ib-color-brand-purple);
}

.accent-purple .metric-card-icon {
  color: var(--ib-color-brand-purple);
}

.accent-cyan {
  border-top-color: var(--ib-color-brand-cyan);
}

.accent-cyan .metric-card-icon {
  color: var(--ib-color-brand-cyan);
}
</style>
