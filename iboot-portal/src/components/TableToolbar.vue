<template>
  <div class="table-toolbar">
    <div class="table-toolbar-main">
      <slot />
    </div>
    <div v-if="$slots.actions" class="table-toolbar-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--ib-space-md);
  margin-bottom: var(--ib-space-md);
}

.table-toolbar-main,
.table-toolbar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--ib-space-xs);
}
</style>
