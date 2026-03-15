<template>
  <div class="dashboard-layout">
    <el-container style="margin: 0; padding: 0;">
      <!-- 侧边栏 -->
      <el-aside width="200px" class="sidebar" style="margin: 0; padding: 0;">
        <div class="logo">
          <h3>{{ systemName }}</h3>
        </div>
        <el-menu
          :default-active="activeIndex"
          class="menu"
          :router="false"
          :collapse="isCollapse"
          :unique-opened="true"
          background-color="#545c64"
          text-color="#fff"
          active-text-color="#ffd04b"
          @select="handleMenuSelect"
        >
          <template v-for="item in menuList" :key="item.index">
            <el-menu-item v-if="!item.children" :index="item.index">
              <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
            <el-sub-menu v-else :index="item.index">
              <template #title>
                <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <template v-for="subItem in item.children" :key="subItem.index">
                <el-menu-item :index="subItem.index">
                  <el-icon v-if="subItem.icon"><component :is="subItem.icon" /></el-icon>
                  <span>{{ subItem.title }}</span>
                </el-menu-item>
              </template>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区域 -->
      <el-container style="margin: 0; padding: 0;">
        <el-header class="header" style="margin: 0; padding: 0;">
          <el-row :gutter="20" class="header-row" style="margin: 0; padding: 0;">
            <el-col :span="12">
              <el-breadcrumb separator="/" class="breadcrumb">
                <el-breadcrumb-item :to="{ path: '/dashboard' }" style="margin-left: 20px;">首页</el-breadcrumb-item>
                <el-breadcrumb-item style="margin-right: 20px;">{{ currentBreadcrumb }}</el-breadcrumb-item>
              </el-breadcrumb>
            </el-col>
            <el-col :span="12" class="user-info">
              <NotificationBell class="notification-bell" />
              <el-badge :value="unreadNoticeCount" :hidden="unreadNoticeCount === 0" :max="99" class="notice-badge">
                <el-icon class="notice-bell" @click="noticePopupVisible = true"><Bell /></el-icon>
              </el-badge>
              <el-dropdown>
                <span class="el-dropdown-link">
                  {{ currentUsername }} <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleProfile">个人中心</el-dropdown-item>
                    <el-dropdown-item @click="handleChangePassword">修改密码</el-dropdown-item>
                    <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-col>
          </el-row>
        </el-header>
        
        <el-main class="main-content" style="margin: 0; padding: 0;">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <!-- 个人中心对话框 -->
    <el-dialog v-model="profileDialogVisible" title="个人中心" width="500px">
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profileForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="profileForm.deptName" disabled />
        </el-form-item>
        <el-form-item label="岗位">
          <el-input :value="profileForm.postNames?.join(', ')" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :value="profileForm.roleNames?.join(', ')" disabled />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-input v-model="profileForm.createTime" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 系统公告弹窗 -->
    <NoticePopup v-model="noticePopupVisible" @read-change="loadUnreadCount" />

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入旧密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, type Component } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElRow, ElCol, type FormInstance, type FormRules } from 'element-plus'
import { ArrowDown, Bell } from '@element-plus/icons-vue'
import { getUserProfile, updateUserProfile, updateUserPassword, getUnreadNoticeCount, type UserProfile, type Menu } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { useAppStore } from '@/stores/app'
import { getIconComponent } from '@/utils/icon-map'
import NoticePopup from '@/components/NoticePopup.vue'
import NotificationBell from '@/components/Notification/NotificationBell.vue'

interface MenuItem {
  index: string
  title: string
  icon?: Component
  children?: MenuItem[]
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const appStore = useAppStore()
const isCollapse = ref(false)
const currentUsername = ref('admin')

const systemName = computed(() => appStore.systemName)

// ==================== 系统公告 ====================
const noticePopupVisible = ref(false)
const unreadNoticeCount = ref(0)

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadNoticeCount()
    if (res.code === 200) {
      unreadNoticeCount.value = res.data ?? 0
    }
  } catch {
    // ignore
  }
}

// ==================== 动态菜单 ====================

/**
 * 提取菜单路径的最后一段，用于路由导航
 * 例如: /system/user → user, /organization/dept → dept
 */
function getLastPathSegment(path: string): string {
  const segments = path.split('/').filter(Boolean)
  return segments.length > 0 ? segments[segments.length - 1] : path
}

/**
 * 将后端菜单树转换为 el-menu 所需格式
 */
function convertMenuTree(menus: Menu[]): MenuItem[] {
  const result: MenuItem[] = []
  for (const menu of menus) {
    if (menu.visible === 0 || menu.status === 0) continue

    if (menu.menuType === 'M') {
      // 目录类型：有子菜单
      const children = menu.children ? convertMenuTree(menu.children) : []
      if (children.length > 0) {
        result.push({
          index: `dir-${menu.id}`,
          title: menu.menuName,
          icon: getIconComponent(menu.icon),
          children
        })
      }
    } else if (menu.menuType === 'C' && menu.path) {
      // 菜单类型：叶子节点
      const routePath = getLastPathSegment(menu.path)
      result.push({
        index: '/' + routePath,
        title: menu.menuName,
        icon: getIconComponent(menu.icon)
      })
    }
  }
  return result
}

const menuList = computed(() => convertMenuTree(permissionStore.menuTree))

/**
 * 根据当前路由路径计算激活的菜单索引
 */
const activeIndex = computed(() => {
  const path = route.path
  if (path.startsWith('/dashboard/')) {
    return '/' + path.replace('/dashboard/', '')
  }
  return ''
})

/**
 * 根据当前路由路径计算面包屑文字
 */
const currentBreadcrumb = computed(() => {
  const item = findMenuItemByIndex(activeIndex.value)
  return item?.title ?? ''
})

// ==================== 个人中心 ====================

const profileDialogVisible = ref(false)
const profileFormRef = ref<FormInstance>()
const profileForm = reactive<UserProfile>({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  deptName: '',
  postNames: [],
  roleNames: [],
  createTime: ''
})
const profileRules: FormRules = {
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

// ==================== 修改密码 ====================

const passwordDialogVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}
const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// ==================== 方法 ====================

const loadUserProfile = async () => {
  try {
    const res = await getUserProfile()
    if (res.code === 200) {
      Object.assign(profileForm, res.data)
      currentUsername.value = res.data.nickname || res.data.username || 'admin'
    }
  } catch {
    // 如果获取失败，使用默认值
  }
}

const handleProfile = async () => {
  await loadUserProfile()
  profileDialogVisible.value = true
}

const handleSubmitProfile = async () => {
  await profileFormRef.value?.validate()
  try {
    await updateUserProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone,
      gender: profileForm.gender
    })
    ElMessage.success('个人信息修改成功')
    profileDialogVisible.value = false
    currentUsername.value = profileForm.nickname || profileForm.username || 'admin'
  } catch {
    ElMessage.error('修改失败')
  }
}

const handleChangePassword = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const handleSubmitPassword = async () => {
  await passwordFormRef.value?.validate()
  try {
    await updateUserPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    userStore.clearUser()
    router.push('/login')
  } catch {
    ElMessage.error('密码修改失败')
  }
}

const handleLogout = () => {
  userStore.clearUser()
  router.push('/login')
  ElMessage.success('已退出登录')
}

const handleMenuSelect = (index: string) => {
  if (index.startsWith('/')) {
    // 菜单项（type C）：导航到对应路由
    router.push(`/dashboard${index}`)
  } else if (index.startsWith('dir-')) {
    // 目录项（type M）：导航到第一个子菜单
    const dirItem = menuList.value.find(item => item.index === index)
    if (dirItem?.children && dirItem.children.length > 0) {
      const firstChild = dirItem.children[0]
      router.push(`/dashboard${firstChild.index}`)
    }
  }
}

const findMenuItemByIndex = (index: string): MenuItem | null => {
  for (const item of menuList.value) {
    if (item.index === index) return item
    if (item.children) {
      const child = item.children.find(c => c.index === index)
      if (child) return child
    }
  }
  return null
}

onMounted(async () => {
  loadUserProfile()
  const noticeRes = await getUnreadNoticeCount().catch(() => null)
  if (noticeRes && noticeRes.code === 200) {
    unreadNoticeCount.value = noticeRes.data ?? 0
    if (unreadNoticeCount.value > 0) {
      noticePopupVisible.value = true
    }
  }
})
</script>

<style scoped>
.dashboard-layout {
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

.dashboard-layout :deep(> .el-container) {
  height: 100%;
}

.dashboard-layout :deep(.el-container.is-vertical) {
  min-height: 0;
}

.sidebar {
  background-color: #545c64;
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  border-right: none;
  top: 0;
  left: 0;
}

.sidebar::-webkit-scrollbar {
  display: none;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  border-bottom: 1px solid #444;
  margin: 0;
  padding: 0;
}

.menu {
  border-right: none;
  border: none;
  margin: 0;
  padding-left: 0;
  padding-right: 0;
}

.menu:not(.el-menu--collapse) {
  width: 200px;
  min-height: 400px;
  margin: 0;
  padding: 0;
}

.header {
  background-color: white;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  margin: 0;
  padding: 0;
}

.header-row {
  display: flex;
  align-items: center;
  height: 100%;
  margin: 0;
  padding: 0;
}

.breadcrumb {
  line-height: 60px;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  padding-right: 20px;
}

.notification-bell {
  display: flex;
  align-items: center;
}

.notice-bell {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.notice-bell:hover {
  color: #409eff;
}

.notice-badge {
  line-height: normal;
}

.main-content {
  background-color: #f5f7fa;
  margin: 0;
  padding: 0;
  overflow-y: auto;
}

.welcome-card {
  background: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  text-align: center;
}

.welcome-card h2 {
  margin-bottom: 15px;
  color: #409eff;
}
</style>