<template>
  <el-tag :type="type" :effect="effect" size="small" class="status-tag">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
    type?: "primary" | "success" | "warning" | "danger" | "info";
    effect?: "light" | "dark" | "plain";
  }>(),
  {
    type: "info",
    effect: "light",
  },
);
</script>

<style scoped>
.status-tag {
  border-radius: var(--ib-radius-full);
  font-weight: var(--ib-weight-semibold);
}
</style>
