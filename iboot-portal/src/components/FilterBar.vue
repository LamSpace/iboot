<template>
  <div class="filter-bar">
    <slot />
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.filter-bar {
  padding: var(--ib-space-md);
  border: 1px solid var(--ib-color-hairline);
  border-radius: var(--ib-radius-xl);
  background: var(--ib-color-canvas);
  margin-bottom: var(--ib-space-md);
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-bar :deep(.el-form--inline) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--ib-space-sm) 0;
}
</style>
