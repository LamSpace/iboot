<template>
  <div class="empty-state">
    <el-icon class="empty-state-icon">
      <component :is="icon" />
    </el-icon>
    <p v-if="title" class="empty-state-title">{{ title }}</p>
    <p v-if="description" class="empty-state-description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-state-action">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from "vue";
import { Document } from "@element-plus/icons-vue";

withDefaults(
  defineProps<{
    title?: string;
    description?: string;
    icon?: Component;
  }>(),
  {
    title: "",
    description: "",
    icon: () => Document,
  },
);
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--ib-space-xs);
  padding: var(--ib-space-xxl) var(--ib-space-md);
  color: var(--ib-color-steel);
  text-align: center;
}

.empty-state-icon {
  font-size: 32px;
  color: var(--ib-color-muted);
}

.empty-state-title {
  margin: 0;
  font-size: var(--ib-text-body-sm);
  font-weight: var(--ib-weight-medium);
  color: var(--ib-color-charcoal);
}

.empty-state-description {
  margin: 0;
  font-size: var(--ib-text-caption);
  color: var(--ib-color-steel);
}

.empty-state-action {
  margin-top: var(--ib-space-sm);
}
</style>
