<template>
  <div class="dashboard-layout">
    <el-container style="margin: 0; padding: 0">
      <!-- 侧边栏 -->
      <el-aside width="200px" class="sidebar" style="margin: 0; padding: 0">
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
                <el-icon v-if="item.icon"
                  ><component :is="item.icon"
                /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <template v-for="subItem in item.children" :key="subItem.index">
                <el-menu-item :index="subItem.index">
                  <el-icon v-if="subItem.icon"
                    ><component :is="subItem.icon"
                  /></el-icon>
                  <span>{{ subItem.title }}</span>
                </el-menu-item>
              </template>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>

      <!-- 主内容区域 -->
      <el-container style="margin: 0; padding: 0">
        <el-header class="header" style="margin: 0; padding: 0">
          <el-row :gutter="20" class="header-row" style="margin: 0; padding: 0">
            <el-col :span="12">
              <el-breadcrumb separator="/" class="breadcrumb">
                <el-breadcrumb-item
                  :to="{ path: '/dashboard' }"
                  style="margin-left: 20px"
                  >{{ t("dashboard.home") }}</el-breadcrumb-item
                >
                <el-breadcrumb-item style="margin-right: 20px">{{
                  currentBreadcrumb
                }}</el-breadcrumb-item>
              </el-breadcrumb>
            </el-col>
            <el-col :span="12" class="user-info">
              <NotificationBell class="notification-bell" />
              <el-badge
                :value="unreadNoticeCount"
                :hidden="unreadNoticeCount === 0"
                :max="99"
                class="notice-badge"
              >
                <el-icon class="notice-bell" @click="noticePopupVisible = true"
                  ><Bell
                /></el-icon>
              </el-badge>
              <el-dropdown>
                <span class="el-dropdown-link">
                  {{ currentUsername }} <el-icon><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleProfile">{{
                      t("dashboard.profile")
                    }}</el-dropdown-item>
                    <el-dropdown-item @click="handleChangePassword">{{
                      t("dashboard.change_password")
                    }}</el-dropdown-item>
                    <el-dropdown-item divided @click="handleLogout">{{
                      t("dashboard.logout")
                    }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <!-- 语言切换组件 -->
              <LocalePicker class="locale-picker" />
            </el-col>
          </el-row>
        </el-header>

        <el-main class="main-content" style="margin: 0; padding: 0">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <!-- 个人中心对话框 -->
    <el-dialog
      v-model="profileDialogVisible"
      :title="t('dashboard.profile')"
      width="500px"
    >
      <el-form
        ref="profileFormRef"
        :model="profileForm"
        :rules="profileRules"
        label-width="80px"
      >
        <el-form-item :label="t('user.username')">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item :label="t('user.nickname')" prop="nickname">
          <el-input
            v-model="profileForm.nickname"
            :placeholder="t('user.placeholder.nickname')"
          />
        </el-form-item>
        <el-form-item :label="t('user.email')" prop="email">
          <el-input
            v-model="profileForm.email"
            :placeholder="t('user.placeholder.email')"
          />
        </el-form-item>
        <el-form-item :label="t('user.phone')" prop="phone">
          <el-input
            v-model="profileForm.phone"
            :placeholder="t('user.placeholder.phone')"
          />
        </el-form-item>
        <el-form-item :label="t('user.gender')">
          <el-radio-group v-model="profileForm.gender">
            <el-radio :value="1">{{ t("user.male") }}</el-radio>
            <el-radio :value="2">{{ t("user.female") }}</el-radio>
            <el-radio :value="0">{{ t("user.unknown") }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('user.dept')">
          <el-input v-model="profileForm.deptName" disabled />
        </el-form-item>
        <el-form-item :label="t('user.post')">
          <el-input :value="profileForm.postNames?.join(', ')" disabled />
        </el-form-item>
        <el-form-item :label="t('user.role')">
          <el-input :value="profileForm.roleNames?.join(', ')" disabled />
        </el-form-item>
        <el-form-item :label="t('common.create_time')">
          <el-input v-model="profileForm.createTime" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileDialogVisible = false">{{
          t("common.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmitProfile">{{
          t("common.save")
        }}</el-button>
      </template>
    </el-dialog>

    <!-- 系统公告弹窗 -->
    <NoticePopup v-model="noticePopupVisible" @read-change="loadUnreadCount" />

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      :title="t('dashboard.change_password')"
      width="400px"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="80px"
      >
        <el-form-item :label="t('dashboard.old_password')" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            :placeholder="t('dashboard.old_password_required')"
            show-password
          />
        </el-form-item>
        <el-form-item :label="t('dashboard.new_password')" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            :placeholder="t('dashboard.new_password_required')"
            show-password
          />
        </el-form-item>
        <el-form-item
          :label="t('dashboard.confirm_password')"
          prop="confirmPassword"
        >
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            :placeholder="t('dashboard.confirm_password_required')"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">{{
          t("common.cancel")
        }}</el-button>
        <el-button type="primary" @click="handleSubmitPassword">{{
          t("common.confirm")
        }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, type Component } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  ElMessage,
  ElRow,
  ElCol,
  type FormInstance,
  type FormRules,
} from "element-plus";
import { useI18n } from "vue-i18n";
import { ArrowDown, Bell } from "@element-plus/icons-vue";
import {
  getUserProfile,
  updateUserProfile,
  updateUserPassword,
  getUnreadNoticeCount,
  type UserProfile,
  type Menu,
} from "@/api/system";
import { useUserStore } from "@/stores/user";
import { usePermissionStore } from "@/stores/permission";
import { useAppStore } from "@/stores/app";
import { getIconComponent } from "@/utils/icon-map";
import NoticePopup from "@/components/NoticePopup.vue";
import NotificationBell from "@/components/Notification/NotificationBell.vue";
import LocalePicker from "@/components/LocalePicker.vue";

interface MenuItem {
  index: string;
  title: string;
  icon?: Component;
  children?: MenuItem[];
}

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const appStore = useAppStore();
const { t } = useI18n();
const isCollapse = ref(false);
const currentUsername = ref("admin");

const systemName = computed(() => appStore.systemName);

// ==================== 系统公告 ====================
const noticePopupVisible = ref(false);
const unreadNoticeCount = ref(0);

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadNoticeCount();
    if (res.code === 200) {
      unreadNoticeCount.value = res.data ?? 0;
    }
  } catch {
    // ignore
  }
};

// ==================== 动态菜单 ====================

function getLastPathSegment(path: string): string {
  const segments = path.split("/").filter(Boolean);
  return segments.length > 0 ? segments[segments.length - 1] : path;
}

function convertMenuTree(menus: Menu[]): MenuItem[] {
  const result: MenuItem[] = [];
  for (const menu of menus) {
    if (menu.visible === 0 || menu.status === 0) continue;

    if (menu.menuType === "M") {
      const children = menu.children ? convertMenuTree(menu.children) : [];
      if (children.length > 0) {
        result.push({
          index: `dir-${menu.id}`,
          title: menu.menuName,
          icon: getIconComponent(menu.icon),
          children,
        });
      }
    } else if (menu.menuType === "C" && menu.path) {
      const routePath = getLastPathSegment(menu.path);
      result.push({
        index: "/" + routePath,
        title: menu.menuName,
        icon: getIconComponent(menu.icon),
      });
    }
  }
  return result;
}

const menuList = computed(() => convertMenuTree(permissionStore.menuTree));

const activeIndex = computed(() => {
  const path = route.path;
  if (path.startsWith("/dashboard/")) {
    return "/" + path.replace("/dashboard/", "");
  }
  return "";
});

const currentBreadcrumb = computed(() => {
  const item = findMenuItemByIndex(activeIndex.value);
  return item?.title ?? "";
});

// ==================== 个人中心 ====================

const profileDialogVisible = ref(false);
const profileFormRef = ref<FormInstance>();
const profileForm = reactive<UserProfile>({
  username: "",
  nickname: "",
  email: "",
  phone: "",
  gender: 0,
  deptName: "",
  postNames: [],
  roleNames: [],
  createTime: "",
});
const profileRules: FormRules = {
  email: [{ type: "email", message: t("user.email_invalid"), trigger: "blur" }],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: t("user.phone_invalid"),
      trigger: "blur",
    },
  ],
};

// ==================== 修改密码 ====================

const passwordDialogVisible = ref(false);
const passwordFormRef = ref<FormInstance>();
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});
const validateConfirmPassword = (
  _rule: unknown,
  value: string,
  callback: (error?: Error) => void,
) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error(t("dashboard.password_not_match")));
  } else {
    callback();
  }
};
const passwordRules: FormRules = {
  oldPassword: [
    {
      required: true,
      message: t("dashboard.old_password_required"),
      trigger: "blur",
    },
  ],
  newPassword: [
    {
      required: true,
      message: t("dashboard.new_password_required"),
      trigger: "blur",
    },
    {
      min: 6,
      max: 20,
      message: t("dashboard.new_password_length"),
      trigger: "blur",
    },
  ],
  confirmPassword: [
    {
      required: true,
      message: t("dashboard.confirm_password_required"),
      trigger: "blur",
    },
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
};

// ==================== 方法 ====================

const loadUserProfile = async () => {
  try {
    const res = await getUserProfile();
    if (res.code === 200) {
      Object.assign(profileForm, res.data);
      currentUsername.value = res.data.nickname || res.data.username || "admin";
    }
  } catch {
    // 如果获取失败，使用默认值
  }
};

const handleProfile = async () => {
  await loadUserProfile();
  profileDialogVisible.value = true;
};

const handleSubmitProfile = async () => {
  await profileFormRef.value?.validate();
  try {
    await updateUserProfile({
      nickname: profileForm.nickname,
      email: profileForm.email,
      phone: profileForm.phone,
      gender: profileForm.gender,
    });
    ElMessage.success(t("dashboard.profile_update_success"));
    profileDialogVisible.value = false;
    currentUsername.value =
      profileForm.nickname || profileForm.username || "admin";
  } catch {
    ElMessage.error(t("dashboard.profile_update_failed"));
  }
};

const handleChangePassword = () => {
  passwordForm.oldPassword = "";
  passwordForm.newPassword = "";
  passwordForm.confirmPassword = "";
  passwordDialogVisible.value = true;
};

const handleSubmitPassword = async () => {
  await passwordFormRef.value?.validate();
  try {
    await updateUserPassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });
    ElMessage.success(t("dashboard.password_update_success"));
    passwordDialogVisible.value = false;
    userStore.clearUser();
    router.push("/login");
  } catch {
    ElMessage.error(t("dashboard.password_update_failed"));
  }
};

const handleLogout = () => {
  userStore.clearUser();
  router.push("/login");
  ElMessage.success(t("dashboard.logout_success"));
};

const handleMenuSelect = (index: string) => {
  if (index.startsWith("/")) {
    router.push(`/dashboard${index}`);
  } else if (index.startsWith("dir-")) {
    const dirItem = menuList.value.find((item) => item.index === index);
    if (dirItem?.children && dirItem.children.length > 0) {
      const firstChild = dirItem.children[0];
      router.push(`/dashboard${firstChild.index}`);
    }
  }
};

const findMenuItemByIndex = (index: string): MenuItem | null => {
  for (const item of menuList.value) {
    if (item.index === index) return item;
    if (item.children) {
      const child = item.children.find((c) => c.index === index);
      if (child) return child;
    }
  }
  return null;
};

onMounted(async () => {
  loadUserProfile();
  const noticeRes = await getUnreadNoticeCount().catch(() => null);
  if (noticeRes && noticeRes.code === 200) {
    unreadNoticeCount.value = noticeRes.data ?? 0;
    if (unreadNoticeCount.value > 0) {
      noticePopupVisible.value = true;
    }
  }
});
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

.locale-picker {
  margin-left: 8px;
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
</style>
