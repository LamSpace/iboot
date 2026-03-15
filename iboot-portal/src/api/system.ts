import request from '../utils/request'
import type { ApiResponse } from './auth'

// ==================== 类型定义 ====================

export interface PageResult<T> {
  data: T[]
  total: number
  pageNum: number
  pageSize: number
}

// 部门
export interface Dept {
  id?: number
  parentId?: number
  deptCode: string
  deptName: string
  orderNum?: number
  leader?: string
  phone?: string
  email?: string
  status?: number
  createTime?: string
  children?: Dept[]
}

// 组织架构图节点
export interface OrgChartNode {
  id: number
  deptName: string
  deptCode: string
  leader?: string
  phone?: string
  email?: string
  status: number
  memberCount: number
  children?: OrgChartNode[]
}

// 岗位
export interface Post {
  id?: number
  postCode: string
  postName: string
  orderNum?: number
  status?: number
  remark?: string
  createTime?: string
}

// 角色
export interface Role {
  id?: number
  roleCode: string
  roleName: string
  orderNum?: number
  dataScope?: number
  status?: number
  remark?: string
  createTime?: string
  menuIds?: number[]
  deptIds?: number[]
}

// 菜单
export interface Menu {
  id?: number
  parentId?: number
  menuName: string
  menuType: string
  path?: string
  component?: string
  perms?: string
  icon?: string
  orderNum?: number
  isFrame?: number
  isCache?: number
  visible?: number
  status?: number
  remark?: string
  createTime?: string
  children?: Menu[]
}

// 字典类型
export interface DictType {
  id?: number
  dictType: string
  dictName: string
  status?: number
  remark?: string
  createTime?: string
}

// 字典数据
export interface DictData {
  id?: number
  dictType: string
  dictLabel: string
  dictValue: string
  orderNum?: number
  cssClass?: string
  listClass?: string
  isDefault?: number
  status?: number
  remark?: string
  createTime?: string
}

// 配置
export interface Config {
  id?: number
  configName: string
  configKey: string
  configValue: string
  configType?: number
  remark?: string
  createTime?: string
}

// 登录日志
export interface LoginLog {
  id?: number
  username?: string
  ipAddress?: string
  loginLocation?: string
  browser?: string
  os?: string
  status?: number
  msg?: string
  loginTime?: string
}

// 操作日志
export interface OperateLog {
  id?: number
  title?: string
  businessType?: number
  method?: string
  requestMethod?: string
  operatorName?: string
  deptName?: string
  operUrl?: string
  operIp?: string
  operLocation?: string
  operParam?: string
  jsonResult?: string
  status?: number
  errorMsg?: string
  costTime?: number
  operTime?: string
}

// ==================== 部门管理 ====================

export function getDeptTree(): Promise<ApiResponse<Dept[]>> {
  return request({ url: '/dept/tree', method: 'get' })
}

export function getDeptList(): Promise<ApiResponse<Dept[]>> {
  return request({ url: '/dept/list', method: 'get' })
}

export function getDeptById(id: number): Promise<ApiResponse<Dept>> {
  return request({ url: `/dept/${id}`, method: 'get' })
}

export function addDept(data: Dept): Promise<ApiResponse<Dept>> {
  return request({ url: '/dept', method: 'post', data })
}

export function updateDept(data: Dept): Promise<ApiResponse<void>> {
  return request({ url: '/dept', method: 'put', data })
}

export function deleteDept(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/dept/${id}`, method: 'delete' })
}

export function getOrgChart(): Promise<ApiResponse<OrgChartNode[]>> {
  return request({ url: '/dept/orgChart', method: 'get' })
}

// ==================== 岗位管理 ====================

export interface PostQuery {
  pageNum?: number
  pageSize?: number
  postName?: string
  postCode?: string
  status?: number
}

export function getPostList(params: PostQuery): Promise<ApiResponse<PageResult<Post>>> {
  return request({ url: '/post/list', method: 'get', params })
}

export function getAllPosts(): Promise<ApiResponse<Post[]>> {
  return request({ url: '/post/all', method: 'get' })
}

export function getPostById(id: number): Promise<ApiResponse<Post>> {
  return request({ url: `/post/${id}`, method: 'get' })
}

export function addPost(data: Post): Promise<ApiResponse<Post>> {
  return request({ url: '/post', method: 'post', data })
}

export function updatePost(data: Post): Promise<ApiResponse<void>> {
  return request({ url: '/post', method: 'put', data })
}

export function deletePost(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/post/${id}`, method: 'delete' })
}

export function changePostStatus(id: number, status: number): Promise<ApiResponse<void>> {
  return request({ url: '/post/changeStatus', method: 'put', params: { id, status } })
}

// ==================== 角色管理 ====================

export interface RoleQuery {
  pageNum?: number
  pageSize?: number
  roleName?: string
  roleCode?: string
  status?: number
}

export function getRoleList(params: RoleQuery): Promise<ApiResponse<PageResult<Role>>> {
  return request({ url: '/role/list', method: 'get', params })
}

export function getAllRoles(): Promise<ApiResponse<Role[]>> {
  return request({ url: '/role/all', method: 'get' })
}

export function getRoleById(id: number): Promise<ApiResponse<Role>> {
  return request({ url: `/role/${id}`, method: 'get' })
}

export function addRole(data: Role): Promise<ApiResponse<Role>> {
  return request({ url: '/role', method: 'post', data })
}

export function updateRole(data: Role): Promise<ApiResponse<void>> {
  return request({ url: '/role', method: 'put', data })
}

export function deleteRole(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/role/${id}`, method: 'delete' })
}

export function changeRoleStatus(id: number, status: number): Promise<ApiResponse<void>> {
  return request({ url: '/role/changeStatus', method: 'put', params: { id, status } })
}

export function setRoleDataScope(data: Role): Promise<ApiResponse<void>> {
  return request({ url: '/role/dataScope', method: 'put', data })
}

// ==================== 菜单管理 ====================

export function getMenuTree(): Promise<ApiResponse<Menu[]>> {
  return request({ url: '/menu/tree', method: 'get' })
}

export function getMenuList(): Promise<ApiResponse<Menu[]>> {
  return request({ url: '/menu/list', method: 'get' })
}

export function getMenuById(id: number): Promise<ApiResponse<Menu>> {
  return request({ url: `/menu/${id}`, method: 'get' })
}

export function getUserMenuTree(): Promise<ApiResponse<Menu[]>> {
  return request({ url: '/menu/user/tree', method: 'get' })
}

export function getUserPermissions(): Promise<ApiResponse<string[]>> {
  return request({ url: '/menu/user/perms', method: 'get' })
}

export function addMenu(data: Menu): Promise<ApiResponse<Menu>> {
  return request({ url: '/menu', method: 'post', data })
}

export function updateMenu(data: Menu): Promise<ApiResponse<void>> {
  return request({ url: '/menu', method: 'put', data })
}

export function deleteMenu(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/menu/${id}`, method: 'delete' })
}

// ==================== 字典管理 ====================

export function getDictTypeList(pageNum = 1, pageSize = 10): Promise<ApiResponse<PageResult<DictType>>> {
  return request({ url: '/dict/type/list', method: 'get', params: { pageNum, pageSize } })
}

export function getAllDictTypes(): Promise<ApiResponse<DictType[]>> {
  return request({ url: '/dict/type/all', method: 'get' })
}

export function getDictTypeById(id: number): Promise<ApiResponse<DictType>> {
  return request({ url: `/dict/type/${id}`, method: 'get' })
}

export function addDictType(data: DictType): Promise<ApiResponse<DictType>> {
  return request({ url: '/dict/type', method: 'post', data })
}

export function updateDictType(data: DictType): Promise<ApiResponse<void>> {
  return request({ url: '/dict/type', method: 'put', data })
}

export function deleteDictType(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/dict/type/${id}`, method: 'delete' })
}

export function getDictDataByType(dictType: string): Promise<ApiResponse<DictData[]>> {
  return request({ url: `/dict/data/type/${dictType}`, method: 'get' })
}

export function getDictDataById(id: number): Promise<ApiResponse<DictData>> {
  return request({ url: `/dict/data/${id}`, method: 'get' })
}

export function addDictData(data: DictData): Promise<ApiResponse<DictData>> {
  return request({ url: '/dict/data', method: 'post', data })
}

export function updateDictData(data: DictData): Promise<ApiResponse<void>> {
  return request({ url: '/dict/data', method: 'put', data })
}

export function deleteDictData(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/dict/data/${id}`, method: 'delete' })
}

export function getDictLabel(dictType: string, dictValue: string): Promise<ApiResponse<string>> {
  return request({ url: '/dict/label', method: 'get', params: { dictType, dictValue } })
}

// ==================== 配置管理 ====================

export interface ConfigQuery {
  pageNum?: number
  pageSize?: number
  configName?: string
  configKey?: string
  configType?: number
}

export function getConfigList(params: ConfigQuery): Promise<ApiResponse<PageResult<Config>>> {
  return request({ url: '/config/list', method: 'get', params })
}

export function getAllConfigs(): Promise<ApiResponse<Config[]>> {
  return request({ url: '/config/all', method: 'get' })
}

export function getConfigById(id: number): Promise<ApiResponse<Config>> {
  return request({ url: `/config/${id}`, method: 'get' })
}

export function getConfigByKey(configKey: string): Promise<ApiResponse<string>> {
  return request({ url: `/config/key/${configKey}`, method: 'get' })
}

export function addConfig(data: Config): Promise<ApiResponse<Config>> {
  return request({ url: '/config', method: 'post', data })
}

export function updateConfig(data: Config): Promise<ApiResponse<void>> {
  return request({ url: '/config', method: 'put', data })
}

export function deleteConfig(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/config/${id}`, method: 'delete' })
}

export function refreshConfigCache(): Promise<ApiResponse<void>> {
  return request({ url: '/config/refreshCache', method: 'delete' })
}

// ==================== 日志管理 ====================

export interface LoginLogQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  ipAddress?: string
  status?: number
  beginTime?: string
  endTime?: string
}

export interface OperateLogQuery {
  pageNum?: number
  pageSize?: number
  title?: string
  operatorName?: string
  businessType?: number
  status?: number
  beginTime?: string
  endTime?: string
}

export function getLoginLogList(params: LoginLogQuery): Promise<ApiResponse<PageResult<LoginLog>>> {
  return request({ url: '/log/login/list', method: 'get', params })
}

export function cleanLoginLog(): Promise<ApiResponse<void>> {
  return request({ url: '/log/login/clean', method: 'delete' })
}

export function getOperateLogList(params: OperateLogQuery): Promise<ApiResponse<PageResult<OperateLog>>> {
  return request({ url: '/log/operate/list', method: 'get', params })
}

export function getOperateLogById(id: number): Promise<ApiResponse<OperateLog>> {
  return request({ url: `/log/operate/${id}`, method: 'get' })
}

export function cleanOperateLog(): Promise<ApiResponse<void>> {
  return request({ url: '/log/operate/clean', method: 'delete' })
}

// 运行日志
export interface RunLog {
  id?: string
  timestamp?: string
  level?: string
  thread?: string
  logger?: string
  message?: string
}

export interface RunLogQuery {
  pageNum?: number
  pageSize?: number
  level?: string
  keyword?: string
  logger?: string
  thread?: string
  beginTime?: string
  endTime?: string
}

export function getRunLogList(params: RunLogQuery): Promise<ApiResponse<PageResult<RunLog>>> {
  return request({ url: '/log/run/list', method: 'get', params })
}

export function getRunLogById(id: string): Promise<ApiResponse<RunLog>> {
  return request({ url: `/log/run/${id}`, method: 'get' })
}

// ==================== 用户管理 ====================

export interface User {
  id?: number
  username: string
  password?: string
  nickname?: string
  email?: string
  phone?: string
  gender?: number
  avatar?: string
  userType?: number
  deptId?: number
  postIds?: number[]
  roleIds?: number[]
  status?: number
  remark?: string
  loginIp?: string
  loginTime?: string
  createTime?: string
}

export interface UserQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  phone?: string
  status?: number
}

export function getUserList(params: UserQuery): Promise<ApiResponse<PageResult<User>>> {
  return request({ url: '/user/list', method: 'get', params })
}

export function getUserById(id: number): Promise<ApiResponse<User>> {
  return request({ url: `/user/${id}`, method: 'get' })
}

export function addUser(data: User): Promise<ApiResponse<User>> {
  return request({ url: '/user', method: 'post', data })
}

export function updateUser(data: User): Promise<ApiResponse<void>> {
  return request({ url: '/user', method: 'put', data })
}

export function deleteUser(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/user/${id}`, method: 'delete' })
}

export function changeUserStatus(id: number, status: number): Promise<ApiResponse<void>> {
  return request({ url: '/user/changeStatus', method: 'put', params: { id, status } })
}

export function resetUserPassword(id: number, newPassword: string): Promise<ApiResponse<void>> {
  return request({ url: '/user/resetPassword', method: 'put', params: { id, newPassword } })
}

// ==================== 个人中心 ====================

export interface UserProfile {
  id?: number
  username?: string
  nickname?: string
  email?: string
  phone?: string
  gender?: number
  avatar?: string
  deptName?: string
  postNames?: string[]
  roleNames?: string[]
  createTime?: string
}

export interface UpdatePasswordParams {
  oldPassword: string
  newPassword: string
}

export function getUserProfile(): Promise<ApiResponse<UserProfile>> {
  return request({ url: '/user/profile', method: 'get' })
}

export function updateUserProfile(data: UserProfile): Promise<ApiResponse<void>> {
  return request({ url: '/user/profile', method: 'put', data })
}

export function updateUserPassword(data: UpdatePasswordParams): Promise<ApiResponse<void>> {
  return request({ url: '/user/profile/password', method: 'put', data })
}

// ==================== Dashboard ====================

export interface DashboardSummary {
  userCount: number
  roleCount: number
  deptCount: number
  postCount: number
  loginLogCount: number
  operateLogCount: number
  recentLoginLogs: LoginLog[]
  recentOperateLogs: OperateLog[]
}

export function getDashboardSummary(): Promise<ApiResponse<DashboardSummary>> {
  return request({ url: '/dashboard/summary', method: 'get' })
}

// ==================== 公开配置 ====================

export interface PublicConfig {
  systemName: string
  registerEnabled: boolean
}

export function getPublicConfig(): Promise<ApiResponse<PublicConfig>> {
  return request({ url: '/config/public', method: 'get' })
}

// ==================== 文件管理 ====================

export interface FileInfo {
  id?: number
  fileName: string
  fileSize?: number
  readableSize?: string
  fileType?: string
  fileExt?: string
  fileCategory?: string
  uploadBy?: string
  uploadTime?: string
  remark?: string
  createTime?: string
}

export interface FileQuery {
  pageNum?: number
  pageSize?: number
  fileName?: string
  fileCategory?: string
  fileExt?: string
}

export interface FileUpdateParams {
  id: number
  fileName?: string
  fileCategory?: string
  remark?: string
}

export function getFileList(params: FileQuery): Promise<ApiResponse<PageResult<FileInfo>>> {
  return request({ url: '/file/list', method: 'get', params })
}

export function getFileById(id: number): Promise<ApiResponse<FileInfo>> {
  return request({ url: `/file/${id}`, method: 'get' })
}

export function uploadFile(data: FormData): Promise<ApiResponse<FileInfo>> {
  return request({
    url: '/file/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function updateFileInfo(data: FileUpdateParams): Promise<ApiResponse<void>> {
  return request({ url: '/file', method: 'put', data })
}

export function deleteFile(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/file/${id}`, method: 'delete' })
}

export function getFileDownloadUrl(id: number): string {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/iboot/api'
  return `${baseURL}/file/download/${id}`
}

// ==================== 系统公告 ====================

export interface Notice {
  id?: number
  noticeTitle: string
  noticeType: string
  noticeContent: string
  status?: string
  isTop?: number
  createBy?: string
  remark?: string
  createTime?: string
}

export interface NoticeQuery {
  pageNum?: number
  pageSize?: number
  noticeTitle?: string
  noticeType?: string
  status?: string
}

export interface PublishedNotice extends Notice {
  isRead?: boolean
}

export function getNoticeList(params: NoticeQuery): Promise<ApiResponse<PageResult<Notice>>> {
  return request({ url: '/notice/list', method: 'get', params })
}

export function getNoticeById(id: number): Promise<ApiResponse<Notice>> {
  return request({ url: `/notice/${id}`, method: 'get' })
}

export function addNotice(data: Notice): Promise<ApiResponse<Notice>> {
  return request({ url: '/notice', method: 'post', data })
}

export function updateNotice(data: Notice): Promise<ApiResponse<void>> {
  return request({ url: '/notice', method: 'put', data })
}

export function deleteNotice(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/notice/${id}`, method: 'delete' })
}

export function publishNotice(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/notice/${id}/publish`, method: 'put' })
}

export function revokeNotice(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/notice/${id}/revoke`, method: 'put' })
}

export function getPublishedNotices(): Promise<ApiResponse<PublishedNotice[]>> {
  return request({ url: '/notice/published', method: 'get' })
}

export function getUnreadNoticeCount(): Promise<ApiResponse<number>> {
  return request({ url: '/notice/unread/count', method: 'get' })
}

export function markNoticeAsRead(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/notice/${id}/read`, method: 'put' })
}

// ==================== 消息中心 ====================

export interface Message {
  id?: number
  title: string
  content: string
  messageType: string
  priority?: string
  senderType?: string
  senderId?: number
  receiverType: string
  status?: string
  createBy?: string
  createTime?: string
  remark?: string
}

export interface MessageQuery {
  pageNum?: number
  pageSize?: number
  title?: string
  messageType?: string
  status?: string
}

export interface UserMessage {
  id?: number
  title?: string
  content?: string
  messageType?: string
  priority?: string
  senderType?: string
  senderId?: number
  createBy?: string
  createTime?: string
  isRead?: number
  readTime?: string
}

export interface UserMessageQuery {
  pageNum?: number
  pageSize?: number
  messageType?: string
  isRead?: number
}

export interface MessageSendRequest {
  messageId: number
  userIds?: number[]
}

export function getMessageList(params: MessageQuery): Promise<ApiResponse<PageResult<Message>>> {
  return request({ url: '/message/list', method: 'get', params })
}

export function getMessageById(id: number): Promise<ApiResponse<Message>> {
  return request({ url: `/message/${id}`, method: 'get' })
}

export function addMessage(data: Message): Promise<ApiResponse<Message>> {
  return request({ url: '/message', method: 'post', data })
}

export function updateMessage(data: Message): Promise<ApiResponse<void>> {
  return request({ url: '/message', method: 'put', data })
}

export function deleteMessage(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/message/${id}`, method: 'delete' })
}

export function sendMessage(data: MessageSendRequest): Promise<ApiResponse<void>> {
  return request({ url: '/message/send', method: 'post', data })
}

export function revokeMessage(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/message/${id}/revoke`, method: 'put' })
}

export function getUserInbox(params: UserMessageQuery): Promise<ApiResponse<PageResult<UserMessage>>> {
  return request({ url: '/message/inbox', method: 'get', params })
}

export function getUnreadMessageCount(): Promise<ApiResponse<number>> {
  return request({ url: '/message/unread/count', method: 'get' })
}

export function markMessageAsRead(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/message/${id}/read`, method: 'put' })
}

export function markAllMessagesAsRead(): Promise<ApiResponse<void>> {
  return request({ url: '/message/read/all', method: 'put' })
}

export function deleteUserMessage(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/message/inbox/${id}`, method: 'delete' })
}

// ==================== 消息模板 ====================

export interface MessageTemplate {
  id?: number
  templateCode: string
  templateName: string
  templateContent: string
  messageType: string
  status?: number
  createBy?: string
  createTime?: string
  remark?: string
}

export interface MessageTemplateQuery {
  pageNum?: number
  pageSize?: number
  templateName?: string
  messageType?: string
  status?: number
}

export function getMessageTemplateList(params: MessageTemplateQuery): Promise<ApiResponse<PageResult<MessageTemplate>>> {
  return request({ url: '/message/template/list', method: 'get', params })
}

export function getMessageTemplateById(id: number): Promise<ApiResponse<MessageTemplate>> {
  return request({ url: `/message/template/${id}`, method: 'get' })
}

export function getAllEnabledTemplates(): Promise<ApiResponse<MessageTemplate[]>> {
  return request({ url: '/message/template/all', method: 'get' })
}

export function addMessageTemplate(data: MessageTemplate): Promise<ApiResponse<MessageTemplate>> {
  return request({ url: '/message/template', method: 'post', data })
}

export function updateMessageTemplate(data: MessageTemplate): Promise<ApiResponse<void>> {
  return request({ url: '/message/template', method: 'put', data })
}

export function deleteMessageTemplate(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/message/template/${id}`, method: 'delete' })
}

// ==================== 在线用户监控 ====================

export interface OnlineUser {
  tokenId?: string
  userId?: number
  username?: string
  nickname?: string
  deptId?: number
  deptName?: string
  loginIp?: string
  browser?: string
  os?: string
  loginTime?: string
  roles?: string[]
}

export interface OnlineUserQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  loginIp?: string
}

export function getOnlineUserList(params: OnlineUserQuery): Promise<ApiResponse<PageResult<OnlineUser>>> {
  return request({ url: '/online/list', method: 'get', params })
}

export function getOnlineUserCount(): Promise<ApiResponse<number>> {
  return request({ url: '/online/count', method: 'get' })
}

export function forceLogout(tokenId: string): Promise<ApiResponse<void>> {
  return request({ url: `/online/${tokenId}`, method: 'delete' })
}

// ==================== Redis 缓存监控 ====================

export interface RedisServerInfo {
  version: string
  mode: string
  os: string
  tcpPort: number
  uptimeInSeconds: number
  uptimeDisplay: string
  connectedClients: number
  processId: number
}

export interface RedisMemoryInfo {
  usedMemory: number
  usedMemoryHuman: string
  maxMemory: number
  maxMemoryHuman: string
  memoryUsageRate: number
  status: string
  statusLabel: string
  fragmentationRatio: number
  warnThreshold: number
  errorThreshold: number
}

export interface RedisStatsInfo {
  totalConnectionsReceived: number
  totalCommandsProcessed: number
  instantaneousOpsPerSec: number
  totalNetInputBytes: number
  totalNetOutputBytes: number
  keyspaceHits: number
  keyspaceMisses: number
  hitRate: number
  expiredKeys: number
  evictedKeys: number
}

export interface RedisKeyspaceInfo {
  dbIndex: number
  keys: number
  expires: number
  avgTtl: number
}

export interface RedisCommandStat {
  command: string
  calls: number
  usec: number
  usecPerCall: number
}

export interface RedisMonitorInfo {
  serverInfo: RedisServerInfo
  memoryInfo: RedisMemoryInfo
  statsInfo: RedisStatsInfo
  keyspaceInfo: RedisKeyspaceInfo[]
  commandStats: RedisCommandStat[]
}

export interface CacheKeyInfo {
  key: string
  type: string
  ttl: number
  ttlDisplay: string
}

export interface CacheKeyDetail {
  key: string
  type: string
  ttl: number
  value: unknown
}

export function getRedisMonitorInfo(): Promise<ApiResponse<RedisMonitorInfo>> {
  return request({ url: '/monitor/redis', method: 'get' })
}

export function getCacheKeys(params: {
  pattern?: string
  pageNum?: number
  pageSize?: number
}): Promise<ApiResponse<PageResult<CacheKeyInfo>>> {
  return request({ url: '/monitor/redis/keys', method: 'get', params })
}

export function getCacheKeyValue(key: string): Promise<ApiResponse<CacheKeyDetail>> {
  return request({ url: `/monitor/redis/key/${encodeURIComponent(key)}`, method: 'get' })
}

export function deleteCacheKey(key: string): Promise<ApiResponse<void>> {
  return request({ url: `/monitor/redis/key/${encodeURIComponent(key)}`, method: 'delete' })
}

export function clearAllCacheKeys(): Promise<ApiResponse<void>> {
  return request({ url: '/monitor/redis/keys', method: 'delete' })
}

// ==================== 服务器监控 ====================

export interface CpuInfo {
  coreCount: number
  model: string
  systemUsage: number
  userUsage: number
  totalUsage: number
  idle: number
  status: string
}

export interface MemoryInfo {
  total: number
  used: number
  free: number
  usageRate: number
  totalDisplay: string
  usedDisplay: string
  freeDisplay: string
  status: string
}

export interface JvmInfo {
  maxMemory: number
  totalMemory: number
  usedMemory: number
  freeMemory: number
  usageRate: number
  maxMemoryDisplay: string
  totalMemoryDisplay: string
  usedMemoryDisplay: string
  freeMemoryDisplay: string
  javaVersion: string
  jvmName: string
  startTime: string
  runTime: string
  status: string
}

export interface DiskInfo {
  mountPoint: string
  fsType: string
  total: number
  used: number
  free: number
  usageRate: number
  totalDisplay: string
  usedDisplay: string
  freeDisplay: string
  status: string
}

export interface OsInfo {
  name: string
  arch: string
  version: string
  hostName: string
  uptime: string
}

export interface ServiceCheckInfo {
  name: string
  type: string
  status: string
  statusLabel: string
  responseTime: number
  detail: string
}

export interface ServerInfoResponse {
  cpu: CpuInfo
  memory: MemoryInfo
  jvm: JvmInfo
  disks: DiskInfo[]
  os: OsInfo
  serviceChecks: ServiceCheckInfo[]
  sbaUrl: string
}

export function getServerMonitorInfo(): Promise<ApiResponse<ServerInfoResponse>> {
  return request({ url: '/monitor/server', method: 'get' })
}

// ==================== 定时任务管理 ====================

export interface Job {
  id?: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy?: number
  concurrent?: number
  status?: number
  remark?: string
  createBy?: string
  createTime?: string
  updateBy?: string
  updateTime?: string
}

export interface JobLog {
  id?: number
  jobId?: number
  jobName?: string
  jobGroup?: string
  invokeTarget?: string
  jobMessage?: string
  status?: number
  exceptionInfo?: string
  startTime?: string
  endTime?: string
  costTime?: number
  createTime?: string
}

export interface JobQuery {
  pageNum?: number
  pageSize?: number
  jobName?: string
  jobGroup?: string
  status?: number
}

export interface JobLogQuery {
  pageNum?: number
  pageSize?: number
  jobId?: number
  jobName?: string
  jobGroup?: string
  status?: number
  beginTime?: string
  endTime?: string
}

export function getJobList(params: JobQuery): Promise<ApiResponse<PageResult<Job>>> {
  return request({ url: '/job/list', method: 'get', params })
}

export function getJobById(id: number): Promise<ApiResponse<Job>> {
  return request({ url: `/job/${id}`, method: 'get' })
}

export function addJob(data: Job): Promise<ApiResponse<Job>> {
  return request({ url: '/job', method: 'post', data })
}

export function updateJob(data: Job): Promise<ApiResponse<void>> {
  return request({ url: '/job', method: 'put', data })
}

export function deleteJob(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/job/${id}`, method: 'delete' })
}

export function changeJobStatus(id: number, status: number): Promise<ApiResponse<void>> {
  return request({ url: `/job/${id}/status`, method: 'put', params: { status } })
}

export function runJob(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/job/${id}/run`, method: 'post' })
}

export function checkCronExpression(cronExpression: string): Promise<ApiResponse<boolean>> {
  return request({ url: '/job/checkCron', method: 'get', params: { cronExpression } })
}

export function getJobLogList(params: JobLogQuery): Promise<ApiResponse<PageResult<JobLog>>> {
  return request({ url: '/job/log/list', method: 'get', params })
}

export function getJobLogById(id: number): Promise<ApiResponse<JobLog>> {
  return request({ url: `/job/log/${id}`, method: 'get' })
}

export function cleanJobLog(params: { jobId?: number; days?: number }): Promise<ApiResponse<number>> {
  return request({ url: '/job/log/clean', method: 'delete', params })
}

// ==================== 统计分析 ====================

export interface ChartData {
  name: string
  value: number
}

export interface TrendData {
  date: string
  value: number
}

export interface RankData {
  username: string
  count: number
}

export interface HourData {
  hour: number
  value: number
}

export interface UserStats {
  totalUsers: number
  newUsersToday: number
  activeUsersToday: number
  deptDistribution: ChartData[]
}

export interface LogStats {
  loginLogCount: number
  operateLogCount: number
  loginSuccessCount: number
  loginFailCount: number
  operateSuccessCount: number
  operateFailCount: number
  loginTrend: TrendData[]
  operateTrend: TrendData[]
  moduleStats: ChartData[]
}

export interface OverviewStats {
  userCount: number
  roleCount: number
  deptCount: number
  postCount: number
  menuCount: number
  configCount: number
}

export interface ReportResponse {
  userStats: UserStats
  logStats: LogStats
  overviewStats: OverviewStats
}

export interface ActivityStats {
  todayActiveUsers: number
  weekActiveUsers: number
  monthActiveUsers: number
  activeUserTrend: TrendData[]
  topActiveUsers: RankData[]
}

export interface FeatureUsageStats {
  moduleRanking: ChartData[]
  apiCallStats: ChartData[]
}

export interface TimeDistributionStats {
  hourlyDistribution: HourData[]
  peakHours: string
}

export interface UsageResponse {
  activityStats: ActivityStats
  featureUsageStats: FeatureUsageStats
  timeDistributionStats: TimeDistributionStats
}

export function getStatisticsReport(params?: {
  startDate?: string
  endDate?: string
}): Promise<ApiResponse<ReportResponse>> {
  return request({ url: '/statistics/report', method: 'get', params })
}

export function getUsageAnalysis(params?: {
  startDate?: string
  endDate?: string
}): Promise<ApiResponse<UsageResponse>> {
  return request({ url: '/statistics/usage', method: 'get', params })
}

// ==================== Excel导出接口 ====================

export function exportUserList(params?: {
  username?: string
  phone?: string
  status?: number
}): Promise<Blob> {
  return request({
    url: '/user/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportRoleList(params?: {
  roleName?: string
  roleCode?: string
  status?: number
}): Promise<Blob> {
  return request({
    url: '/role/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportLoginLogList(params?: {
  username?: string
  ipAddress?: string
  status?: number
  beginTime?: string
  endTime?: string
}): Promise<Blob> {
  return request({
    url: '/log/login/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportOperateLogList(params?: {
  title?: string
  operatorName?: string
  businessType?: number
  status?: number
  beginTime?: string
  endTime?: string
}): Promise<Blob> {
  return request({
    url: '/log/operate/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportOnlineUserList(params?: {
  username?: string
  loginIp?: string
}): Promise<Blob> {
  return request({
    url: '/online/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportDeptList(params?: {
  deptName?: string
  status?: number
}): Promise<Blob> {
  return request({
    url: '/dept/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportPostList(params?: {
  postName?: string
  postCode?: string
  status?: number
}): Promise<Blob> {
  return request({
    url: '/post/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportMenuList(params?: {
  menuName?: string
  status?: number
}): Promise<Blob> {
  return request({
    url: '/menu/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportConfigList(params?: {
  configName?: string
  configKey?: string
  configType?: number
}): Promise<Blob> {
  return request({
    url: '/config/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportDictTypeList(params?: {
  dictName?: string
  dictType?: string
  status?: number
}): Promise<Blob> {
  return request({
    url: '/dict/type/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportDictDataList(params: {
  dictType: string
}): Promise<Blob> {
  return request({
    url: '/dict/data/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportNoticeList(params?: {
  noticeTitle?: string
  noticeType?: string
  status?: string
}): Promise<Blob> {
  return request({
    url: '/notice/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportMessageList(params?: {
  title?: string
  messageType?: string
  status?: string
}): Promise<Blob> {
  return request({
    url: '/message/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportJobList(params?: {
  jobName?: string
  jobGroup?: string
  status?: number
}): Promise<Blob> {
  return request({
    url: '/job/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

export function exportFileList(params?: {
  fileName?: string
  fileCategory?: string
  fileExt?: string
}): Promise<Blob> {
  return request({
    url: '/file/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// ==================== MinIO监控 ====================

export interface MinioBucketStats {
  objectCount: number
  totalSize: number
}

export interface MinioStatus {
  monitorEnabled: boolean
  storageType: string
  minioEnabled: boolean
  consoleUrl: string
  endpoint: string
  bucketName: string
  status: string
  message?: string
  uptime?: string
  version?: string
  platform?: string
  memory?: any
  buckets?: Record<string, MinioBucketStats>
  totalBuckets?: number
  totalSize?: number
  totalObjects?: number
}

export function getMinioStatus(): Promise<ApiResponse<MinioStatus>> {
  return request({ url: '/monitor/minio', method: 'get' })
}

export function pingMinio(): Promise<ApiResponse<boolean>> {
  return request({ url: '/monitor/minio/ping', method: 'get' })
}

// ==================== 消息推送 ====================

export interface PushStatus {
  online: boolean
  totalOnline: number
}

export function getPushStatus(): Promise<ApiResponse<PushStatus>> {
  return request({ url: '/push/status', method: 'get' })
}
