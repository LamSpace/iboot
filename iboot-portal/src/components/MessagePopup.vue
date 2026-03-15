<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="我的消息"
    width="700px"
    :close-on-click-modal="false"
  >
    <!-- 筛选栏 -->
    <div class="inbox-toolbar">
      <el-radio-group v-model="filter" size="small" @change="loadMessages">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="unread">未读</el-radio-button>
        <el-radio-button value="read">已读</el-radio-button>
      </el-radio-group>
      <el-button size="small" @click="handleMarkAllRead" :disabled="messages.length === 0">全部已读</el-button>
    </div>

    <div v-if="loading" v-loading="true" style="min-height: 200px;"></div>
    <div v-else-if="messages.length === 0" class="empty-message">
      <el-empty description="暂无消息" :image-size="80" />
    </div>
    <div v-else class="message-list">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="message-item"
        :class="{ expanded: expandedId === msg.id, unread: msg.isRead === 0 }"
      >
        <div class="message-header" @click="toggleExpand(msg)">
          <div class="message-title-row">
            <span v-if="msg.isRead === 0" class="unread-dot"></span>
            <el-tag
              :type="dictStore.getDictListClass('sys_message_type', msg.messageType)"
              size="small"
            >{{ dictStore.getDictLabel('sys_message_type', msg.messageType) }}</el-tag>
            <el-tag
              v-if="msg.priority === '2'"
              type="danger"
              size="small"
            >紧急</el-tag>
            <el-tag
              v-else-if="msg.priority === '1'"
              type="warning"
              size="small"
            >重要</el-tag>
            <span class="message-title">{{ msg.title }}</span>
          </div>
          <div class="message-meta">
            <span class="message-time">{{ msg.createTime }}</span>
          </div>
        </div>
        <div v-if="expandedId === msg.id" class="message-content">
          <div class="message-text">{{ msg.content }}</div>
          <div class="message-actions">
            <el-button type="danger" link size="small" @click.stop="handleDeleteMsg(msg)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-if="totalMsg > pageSize"
      class="pagination"
      v-model:current-page="pageNum"
      :page-size="pageSize"
      :total="totalMsg"
      layout="prev, pager, next"
      small
      @current-change="loadMessages"
    />

    <template #footer>
      <el-button type="primary" @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUserInbox,
  markMessageAsRead,
  markAllMessagesAsRead,
  deleteUserMessage,
  type UserMessage
} from '@/api/system'
import { useDictStore } from '@/stores/dict'

const dictStore = useDictStore()

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'read-change': []
}>()

const loading = ref(false)
const messages = ref<UserMessage[]>([])
const expandedId = ref<number | undefined>()
const filter = ref('all')
const pageNum = ref(1)
const pageSize = 10
const totalMsg = ref(0)

const loadMessages = async () => {
  loading.value = true
  try {
    await dictStore.loadDicts('sys_message_type')
    const isRead = filter.value === 'unread' ? 0 : filter.value === 'read' ? 1 : undefined
    const res = await getUserInbox({
      pageNum: pageNum.value,
      pageSize,
      isRead
    })
    if (res.code === 200) {
      messages.value = res.data.data || []
      totalMsg.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const toggleExpand = async (msg: UserMessage) => {
  if (expandedId.value === msg.id) {
    expandedId.value = undefined
    return
  }
  expandedId.value = msg.id
  if (msg.isRead === 0 && msg.id) {
    try {
      await markMessageAsRead(msg.id)
      msg.isRead = 1
      emit('read-change')
    } catch {
      // ignore
    }
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllMessagesAsRead()
    messages.value.forEach(m => { m.isRead = 1 })
    ElMessage.success('已全部标记为已读')
    emit('read-change')
  } catch {
    // ignore
  }
}

const handleDeleteMsg = (msg: UserMessage) => {
  ElMessageBox.confirm('确认删除该消息吗？', '提示', { type: 'warning' })
    .then(async () => {
      await deleteUserMessage(msg.id!)
      ElMessage.success('删除成功')
      loadMessages()
      emit('read-change')
    })
    .catch(() => {})
}

watch(() => props.modelValue, (val) => {
  if (val) {
    expandedId.value = undefined
    filter.value = 'all'
    pageNum.value = 1
    loadMessages()
  }
})
</script>

<style scoped>
.inbox-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.message-list {
  max-height: 420px;
  overflow-y: auto;
}

.message-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 10px;
  transition: box-shadow 0.2s;
}

.message-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.message-item.expanded {
  border-color: #409eff;
}

.message-item.unread {
  background-color: #f0f9ff;
}

.message-header {
  padding: 12px 16px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
  flex-shrink: 0;
}

.message-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.message-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  margin-left: 16px;
}

.message-content {
  padding: 0 16px 12px;
  border-top: 1px solid #f0f0f0;
}

.message-text {
  padding-top: 12px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.empty-message {
  padding: 20px 0;
}

.pagination {
  margin-top: 12px;
  justify-content: center;
}
</style>
