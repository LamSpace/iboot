<template>
  <div class="markdown-body" v-html="html"></div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { renderMarkdown } from "../utils/markdown";

const props = defineProps<{ content: string }>();

const html = computed(() => renderMarkdown(props.content));
</script>

<style scoped>
.markdown-body {
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
  word-break: break-word;
}

.markdown-body :deep(> *:first-child) {
  margin-top: 0;
}

.markdown-body :deep(> *:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 16px 0 8px;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-body :deep(h1) {
  font-size: 20px;
}

.markdown-body :deep(h2) {
  font-size: 18px;
}

.markdown-body :deep(h3) {
  font-size: 16px;
}

.markdown-body :deep(h4) {
  font-size: 14px;
}

.markdown-body :deep(p) {
  margin: 8px 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 22px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(li > p) {
  margin: 0;
}

.markdown-body :deep(strong) {
  font-weight: 600;
}

.markdown-body :deep(a) {
  color: var(--el-color-primary);
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(code) {
  padding: 2px 6px;
  font-family: "JetBrains Mono", "Fira Code", Consolas, monospace;
  font-size: 12.5px;
  background: var(--el-fill-color);
  border-radius: 4px;
}

.markdown-body :deep(pre) {
  margin: 10px 0;
  padding: 12px 14px;
  overflow-x: auto;
  background: var(--el-fill-color-dark);
  border-radius: 8px;
}

.markdown-body :deep(pre code) {
  padding: 0;
  font-size: 12.5px;
  background: transparent;
}

.markdown-body :deep(blockquote) {
  margin: 10px 0;
  padding: 6px 12px;
  color: var(--el-text-color-secondary);
  border-left: 3px solid var(--el-border-color);
  background: var(--el-fill-color-lighter);
  border-radius: 0 6px 6px 0;
}

.markdown-body :deep(table) {
  width: 100%;
  margin: 10px 0;
  font-size: 13px;
  border-collapse: collapse;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 6px 10px;
  text-align: left;
  border: 1px solid var(--el-border-color-lighter);
}

.markdown-body :deep(th) {
  font-weight: 600;
  background: var(--el-fill-color-light);
}

.markdown-body :deep(tr:nth-child(even) td) {
  background: var(--el-fill-color-lighter);
}

.markdown-body :deep(hr) {
  margin: 16px 0;
  border: none;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
