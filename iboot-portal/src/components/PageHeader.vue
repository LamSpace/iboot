<template>
  <div class="page-header">
    <div class="page-header-text">
      <h1 v-if="title" class="page-header-title">{{ title }}</h1>
      <p v-if="description" class="page-header-description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string;
  description?: string;
}>();
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--ib-space-md);
  margin-bottom: var(--ib-space-lg);
}

.page-header-text {
  min-width: 0;
}

.page-header-title {
  margin: 0;
  font-size: var(--ib-text-heading-sm);
  font-weight: var(--ib-weight-semibold);
  line-height: var(--ib-leading-heading);
  color: var(--ib-color-ink);
}

.page-header-description {
  margin: var(--ib-space-xxs) 0 0;
  font-size: var(--ib-text-body-sm);
  color: var(--ib-color-steel);
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: var(--ib-space-xs);
  flex-shrink: 0;
}
</style>
