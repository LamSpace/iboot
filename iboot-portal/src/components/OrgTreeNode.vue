<template>
  <li>
    <!-- 节点卡片 -->
    <div
      class="org-card"
      :class="{
        'is-root': level === 0,
        'is-disabled': node.status !== 1,
        'is-matched': isMatched,
      }"
      :style="{ borderTopColor: levelColor }"
      @click="$emit('select', node)"
    >
      <div class="card-header">
        <span class="dept-name">{{ node.deptName }}</span>
        <div class="header-actions">
          <span v-if="node.status !== 1" class="status-badge">停用</span>
          <!-- 展开/折叠按钮 -->
          <el-icon
            v-if="hasChildren"
            class="toggle-btn"
            @click.stop="$emit('toggle', node.id)"
          >
            <ArrowRight v-if="isCollapsed" />
            <ArrowDown v-else />
          </el-icon>
        </div>
      </div>
      <div class="card-body">
        <div v-if="node.leader" class="leader-info">
          <el-icon class="icon"><User /></el-icon>
          <span>{{ node.leader }}</span>
        </div>
        <div class="member-count" :style="{ color: levelColor }">
          <span class="count">{{ node.memberCount }}</span>
          <span class="unit">人</span>
        </div>
      </div>
    </div>
    <!-- 递归渲染子节点（根据折叠状态） -->
    <ul v-if="hasChildren && !isCollapsed">
      <OrgTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
        :matched-ids="matchedIds"
        :collapsed-ids="collapsedIds"
        @select="(n) => $emit('select', n)"
        @toggle="(id) => $emit('toggle', id)"
      />
    </ul>
  </li>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { User, ArrowDown, ArrowRight } from "@element-plus/icons-vue";
import type { OrgChartNode } from "@/api/system";

const props = defineProps<{
  node: OrgChartNode;
  level: number;
  matchedIds: Set<number>;
  collapsedIds: Set<number>;
}>();

defineEmits<{
  select: [node: OrgChartNode];
  toggle: [nodeId: number];
}>();

const levelColors = ["#409EFF", "#67C23A", "#E6A23C", "#F56C6C", "#909399"];
const levelColor = computed(
  () => levelColors[props.level % levelColors.length],
);

// 搜索高亮
const isMatched = computed(() => props.matchedIds.has(props.node.id));

// 展开/折叠状态
const isCollapsed = computed(() => props.collapsedIds.has(props.node.id));
const hasChildren = computed(
  () => props.node.children && props.node.children.length > 0,
);
</script>

<style scoped>
/* 卡片样式 */
.org-card {
  display: inline-block;
  min-width: 160px;
  max-width: 200px;
  background: white;
  border-radius: 8px;
  border-top: 4px solid #409eff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.org-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.org-card.is-root {
  min-width: 200px;
  max-width: 240px;
}

.org-card.is-disabled {
  opacity: 0.6;
  background: #f5f5f5;
}

/* 搜索匹配高亮 */
.org-card.is-matched {
  box-shadow:
    0 0 0 3px #409eff,
    0 4px 12px rgba(64, 158, 255, 0.3);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    box-shadow:
      0 0 0 3px #409eff,
      0 4px 12px rgba(64, 158, 255, 0.3);
  }
  50% {
    box-shadow:
      0 0 0 5px #409eff,
      0 4px 16px rgba(64, 158, 255, 0.5);
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 8px;
  border-bottom: 1px solid #f0f0f0;
}

.dept-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.org-card.is-root .dept-name {
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.status-badge {
  font-size: 10px;
  padding: 2px 5px;
  background: #fef0f0;
  color: #f56c6c;
  border-radius: 3px;
  flex-shrink: 0;
}

.card-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 12px;
}

.leader-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}

.leader-info .icon {
  font-size: 12px;
  color: #909399;
}

.member-count {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.member-count .count {
  font-size: 20px;
  font-weight: 700;
}

.member-count .unit {
  font-size: 11px;
  color: #909399;
}

/* 展开/折叠按钮 */
.toggle-btn {
  cursor: pointer;
  color: #909399;
  font-size: 14px;
  transition:
    color 0.2s,
    transform 0.2s;
}

.toggle-btn:hover {
  color: #409eff;
  transform: scale(1.2);
}
</style>
