<template>
  <div class="org-chart-page">
    <!-- 顶部工具栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon><OfficeBuilding /></el-icon>
          组织架构图
        </h2>
      </div>
      <div class="header-right">
        <!-- 搜索框 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索部门"
          clearable
          style="width: 160px"
          @keyup.enter="handleSearch"
          @clear="clearSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="handleSearch" type="primary">
          <el-icon><Search /></el-icon>
        </el-button>
        
        <!-- 展开/折叠按钮 -->
        <el-button-group>
          <el-button @click="expandAll" title="展开全部">
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <el-button @click="collapseAll" title="折叠全部">
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </el-button-group>
        
        <!-- 缩放控制 -->
        <el-button-group class="zoom-controls">
          <el-button @click="zoomOut" :disabled="scale <= 0.3">
            <el-icon><Minus /></el-icon>
          </el-button>
          <el-button class="zoom-value">{{ Math.round(scale * 100) }}%</el-button>
          <el-button @click="zoomIn" :disabled="scale >= 1.5">
            <el-icon><Plus /></el-icon>
          </el-button>
        </el-button-group>
        <el-button @click="resetView">
          <el-icon><FullScreen /></el-icon>
          重置
        </el-button>
        <el-button type="primary" @click="loadData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 组织架构图主体 -->
    <div class="chart-container">
      <div class="chart-scroll" :style="{ transform: `scale(${scale})`, transformOrigin: 'top center' }">
        <div class="org-chart" v-loading="loading">
          <ul v-if="orgData.length > 0" class="org-tree">
            <OrgTreeNode
              v-for="node in orgData"
              :key="node.id"
              :node="node"
              :level="0"
              :matched-ids="matchedNodeIds"
              :collapsed-ids="collapsedNodeIds"
              @select="handleSelectNode"
              @toggle="toggleNode"
            />
          </ul>
          <el-empty v-else-if="!loading" description="暂无组织架构数据">
            <el-button type="primary" @click="loadData">重新加载</el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <!-- 部门详情面板 -->
    <el-drawer v-model="drawerVisible" title="部门详情" direction="rtl" :size="380">
      <template v-if="selectedNode">
        <div class="drawer-content">
          <div class="dept-header">
            <div class="dept-avatar">
              <el-icon :size="28"><OfficeBuilding /></el-icon>
            </div>
            <div class="dept-info">
              <h3>{{ selectedNode.deptName }}</h3>
              <span class="dept-code">{{ selectedNode.deptCode }}</span>
            </div>
            <el-tag :type="selectedNode.status === 1 ? 'success' : 'danger'" effect="dark" size="small">
              {{ selectedNode.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </div>

          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-value">{{ selectedNode.memberCount }}</div>
              <div class="stat-label">成员数量</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ selectedNode.children?.length || 0 }}</div>
              <div class="stat-label">下级部门</div>
            </div>
          </div>

          <el-divider content-position="left">联系信息</el-divider>
          <div class="info-list">
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span class="label">负责人</span>
              <span class="value">{{ selectedNode.leader || '未设置' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Phone /></el-icon>
              <span class="label">电话</span>
              <span class="value">{{ selectedNode.phone || '未设置' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Message /></el-icon>
              <span class="label">邮箱</span>
              <span class="value">{{ selectedNode.email || '未设置' }}</span>
            </div>
          </div>

          <template v-if="selectedNode.children && selectedNode.children.length > 0">
            <el-divider content-position="left">下级部门</el-divider>
            <div class="sub-dept-list">
              <div
                v-for="child in selectedNode.children"
                :key="child.id"
                class="sub-dept-item"
                @click="handleSelectNode(child)"
              >
                <el-icon><FolderOpened /></el-icon>
                <span>{{ child.deptName }}</span>
                <el-tag size="small" type="info">{{ child.memberCount }}人</el-tag>
              </div>
            </div>
          </template>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  OfficeBuilding, User, Phone, Message, FolderOpened,
  Refresh, Plus, Minus, FullScreen, Search, ArrowDown, ArrowRight
} from '@element-plus/icons-vue'
import { getOrgChart, type OrgChartNode } from '@/api/system'
import OrgTreeNode from '@/components/OrgTreeNode.vue'

const loading = ref(false)
const orgData = ref<OrgChartNode[]>([])
const scale = ref(0.75)
const drawerVisible = ref(false)
const selectedNode = ref<OrgChartNode | null>(null)

// 搜索相关状态
const searchKeyword = ref('')
const matchedNodeIds = ref<Set<number>>(new Set())

// 展开/折叠相关状态
const collapsedNodeIds = ref<Set<number>>(new Set())

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOrgChart()
    if (res.code === 200) {
      orgData.value = res.data
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch {
    ElMessage.error('加载组织架构数据失败')
  } finally {
    loading.value = false
  }
}

const handleSelectNode = (node: OrgChartNode) => {
  selectedNode.value = node
  drawerVisible.value = true
}

const zoomIn = () => {
  scale.value = Math.min(scale.value + 0.1, 1.5)
}

const zoomOut = () => {
  scale.value = Math.max(scale.value - 0.1, 0.3)
}

const resetView = () => {
  scale.value = 0.75
}

// ==================== 搜索功能 ====================

// 递归搜索匹配节点
const findMatchingNodes = (nodes: OrgChartNode[], keyword: string): number[] => {
  const result: number[] = []
  const search = (node: OrgChartNode) => {
    if (node.deptName.includes(keyword)) {
      result.push(node.id)
    }
    node.children?.forEach(search)
  }
  nodes.forEach(search)
  return result
}

// 展开到匹配节点的路径
const expandToMatchedNodes = () => {
  const ancestorIds = new Set<number>()
  const findAncestors = (nodes: OrgChartNode[], parentIds: number[] = []) => {
    nodes.forEach(node => {
      const currentPath = [...parentIds]
      if (matchedNodeIds.value.has(node.id)) {
        parentIds.forEach(id => ancestorIds.add(id))
      }
      if (node.children?.length) {
        findAncestors(node.children, [...currentPath, node.id])
      }
    })
  }
  findAncestors(orgData.value)
  
  // 从 collapsedNodeIds 中移除祖先节点
  ancestorIds.forEach(id => collapsedNodeIds.value.delete(id))
  collapsedNodeIds.value = new Set(collapsedNodeIds.value)
}

// 执行搜索
const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    matchedNodeIds.value = new Set()
    return
  }
  const ids = findMatchingNodes(orgData.value, searchKeyword.value.trim())
  matchedNodeIds.value = new Set(ids)
  
  if (ids.length === 0) {
    ElMessage.warning('未找到匹配的部门')
    return
  }
  
  // 自动展开包含匹配节点的路径
  expandToMatchedNodes()
  
  // 滚动到第一个匹配节点
  nextTick(() => {
    const firstMatch = document.querySelector('.org-card.is-matched')
    firstMatch?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

// 清除搜索
const clearSearch = () => {
  searchKeyword.value = ''
  matchedNodeIds.value = new Set()
}

// ==================== 展开/折叠功能 ====================

// 切换节点展开状态
const toggleNode = (nodeId: number) => {
  if (collapsedNodeIds.value.has(nodeId)) {
    collapsedNodeIds.value.delete(nodeId)
  } else {
    collapsedNodeIds.value.add(nodeId)
  }
  collapsedNodeIds.value = new Set(collapsedNodeIds.value)
}

// 展开全部
const expandAll = () => {
  collapsedNodeIds.value = new Set()
}

// 折叠全部
const collapseAll = () => {
  const allIds = new Set<number>()
  const collectIds = (nodes: OrgChartNode[]) => {
    nodes.forEach(node => {
      if (node.children?.length) {
        allIds.add(node.id)
        collectIds(node.children)
      }
    })
  }
  collectIds(orgData.value)
  collapsedNodeIds.value = allIds
}

onMounted(() => {
  loadData()
})
</script>

<style>
/* 树形结构全局样式 - 必须是全局样式才能作用于递归组件 */
.org-tree,
.org-tree ul {
  margin: 0;
  padding: 0;
  list-style: none;
  position: relative;
}

.org-tree {
  display: flex;
  justify-content: center;
}

.org-tree ul {
  display: flex;
  justify-content: center;
  padding-top: 30px;
  position: relative;
}

/* 父节点到子节点列表的垂直连接线 */
.org-tree ul::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  width: 2px;
  height: 30px;
  background: #c0c4cc;
}

.org-tree > li {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.org-tree li li {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 15px;
  position: relative;
}

/* 子节点上方的垂直连接线 */
.org-tree li li::before {
  content: '';
  position: absolute;
  top: -30px;
  left: 50%;
  width: 2px;
  height: 30px;
  background: #c0c4cc;
}

/* 子节点之间的水平连接线 */
.org-tree li li::after {
  content: '';
  position: absolute;
  top: -30px;
  width: 100%;
  height: 2px;
  background: #c0c4cc;
}

/* 第一个子节点：水平线从中间开始 */
.org-tree li li:first-child::after {
  left: 50%;
  width: 50%;
}

/* 最后一个子节点：水平线到中间结束 */
.org-tree li li:last-child::after {
  right: 50%;
  width: 50%;
  left: auto;
}

/* 唯一子节点：不显示水平线 */
.org-tree li li:only-child::after {
  display: none;
}

/* 第一个且最后一个（唯一子节点）的处理 */
.org-tree li li:first-child:last-child::after {
  display: none;
}
</style>

<style scoped>
.org-chart-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8eaed 100%);
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.zoom-controls .zoom-value {
  min-width: 55px;
  cursor: default;
  font-weight: 500;
}

/* 图表容器 */
.chart-container {
  flex: 1;
  overflow: auto;
  padding: 30px 20px 50px;
}

.chart-scroll {
  transition: transform 0.2s ease;
}

.org-chart {
  display: flex;
  justify-content: center;
  min-width: fit-content;
  min-height: 200px;
}

/* 抽屉样式 */
.drawer-content {
  padding: 0 8px;
}

.dept-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 16px;
}

.dept-avatar {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.dept-info {
  flex: 1;
  min-width: 0;
}

.dept-info h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #303133;
}

.dept-code {
  font-size: 12px;
  color: #909399;
}

.stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #409EFF;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: #fafafa;
  border-radius: 6px;
}

.info-item .el-icon {
  color: #909399;
}

.info-item .label {
  color: #909399;
  font-size: 12px;
  min-width: 45px;
}

.info-item .value {
  color: #303133;
  font-size: 13px;
}

.sub-dept-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-dept-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: #fafafa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.sub-dept-item:hover {
  background: #ecf5ff;
}

.sub-dept-item .el-icon {
  color: #409EFF;
}

.sub-dept-item span {
  flex: 1;
  color: #303133;
  font-size: 13px;
}
</style>
