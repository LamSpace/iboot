import { type Component } from 'vue'
import {
  SetUp,
  User,
  UserFilled,
  List,
  Notebook,
  Operation,
  OfficeBuilding,
  Position,
  Tickets,
  MessageBox,
  ChatLineSquare,
  ChatLineRound,
  ChatDotRound,
  Finished,
  EditPen,
  Document,
  FolderOpened,
  Files,
  Connection,
  DataLine,
  TrendCharts,
  Clock,
  PieChart,
  Lock,
  Monitor,
  Histogram,
  DataAnalysis,
  Upload,
  Bell,
  CopyDocument,
  Coin,
  Warning,
  WarningFilled,
  Box
} from '@element-plus/icons-vue'

/**
 * 后端数据库图标名称到 Element Plus 图标组件的映射表
 */
const iconMap: Record<string, Component> = {
  // 系统管理
  'setting': SetUp,
  'system': SetUp,
  // 用户
  'user': User,
  'peoples': UserFilled,
  // 菜单/列表
  'tree-table': List,
  'list': List,
  // 组织架构
  'tree': OfficeBuilding,
  'dept': OfficeBuilding,
  'post': Position,
  'org-chart': Tickets,
  'ticket': Tickets,
  // 消息
  'message': ChatLineRound,
  'message-box': MessageBox,
  'chat': ChatLineSquare,
  'chat-dot-round': ChatDotRound,
  'notice': MessageBox,
  'notification': Bell,
  // 安全/日志
  'log': Lock,
  'lock': Lock,
  'logininfor': Finished,
  'finished': Finished,
  'form': EditPen,
  'edit': EditPen,
  'document': Document,
  'bug': Warning,
  // 文件
  'file': FolderOpened,
  'folder': FolderOpened,
  'upload': Upload,
  'files': Files,
  // 数据
  'database': Coin,
  // 字典
  'dict': Notebook,
  'notebook': Notebook,
  // 配置
  'config': Operation,
  'operation': Operation,
  'template': CopyDocument,
  // 监控
  'monitor': Monitor,
  'server': Monitor,
  'redis': Connection,
  'connection': Connection,
  'sql': DataLine,
  'data-line': DataLine,
  'performance': TrendCharts,
  'trend': TrendCharts,
  'online': User,
  'swagger': Document,
  'api': Document,
  // 定时任务
  'job': Clock,
  'clock': Clock,
  // 统计分析
  'chart': Histogram,
  'histogram': Histogram,
  'pie-chart': PieChart,
  'data-analysis': DataAnalysis,
  'report': TrendCharts,
  'usage': TrendCharts,
  'dashboard': PieChart,
  // 对象存储
  'box': Box,
  'minio': Box,
  // 可观测性
  'prometheus': TrendCharts,
  'grafana': Histogram,
  'kibana': DataAnalysis,
  'alertmanager': WarningFilled,
  'warning-filled': WarningFilled,
}

/**
 * 根据图标名称获取 Element Plus 图标组件
 * @param iconName 后端返回的图标名称字符串
 * @returns 对应的 Element Plus 图标组件，未找到时返回 undefined
 */
export function getIconComponent(iconName?: string): Component | undefined {
  if (!iconName) return undefined
  return iconMap[iconName] ?? iconMap[iconName.toLowerCase()] ?? undefined
}
