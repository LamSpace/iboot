<template>
  <div class="login-container">
    <section class="login-brand">
      <span class="brand-mark">iB</span>
      <h1 class="brand-title">{{ systemName }}</h1>
      <p class="brand-copy">{{ t("home.system_title") }}</p>
    </section>

    <section class="login-panel">
      <LocalePicker class="login-locale" />
      <div class="login-form">
        <h2>{{ systemName }}</h2>
        <el-form
          :model="loginForm"
          :rules="loginRules"
          ref="loginFormRef"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              :placeholder="t('login.username_placeholder')"
              prefix-icon="User"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              :placeholder="t('login.password_placeholder')"
              prefix-icon="Lock"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="handleLogin"
              :loading="loading"
              style="width: 100%"
            >
              {{ t("login.submit") }}
            </el-button>
          </el-form-item>
          <div v-if="registerEnabled" class="login-links">
            <router-link to="/register">{{ t("login.register") }}</router-link>
          </div>
        </el-form>
      </div>
    </section>

    <!-- 滑块验证组件 -->
    <SliderCaptcha
      v-model="showCaptcha"
      @success="handleCaptchaSuccess"
      @cancel="handleCaptchaCancel"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import { useI18n } from "vue-i18n";
import { login } from "@/api/auth";
import { useUserStore } from "@/stores/user";
import { usePermissionStore } from "@/stores/permission";
import { useAppStore } from "@/stores/app";
import SliderCaptcha from "@/components/SliderCaptcha.vue";
import LocalePicker from "@/components/LocalePicker.vue";

interface LoginForm {
  username: string;
  password: string;
}

const router = useRouter();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const appStore = useAppStore();
const { t } = useI18n();
const loading = ref(false);
const loginFormRef = ref<FormInstance>();
const showCaptcha = ref(false);

const systemName = computed(() => appStore.systemName);
const registerEnabled = computed(() => appStore.registerEnabled);

const loginForm = reactive<LoginForm>({
  username: "",
  password: "",
});

const loginRules = {
  username: [
    {
      required: true,
      message: t("login.username_placeholder"),
      trigger: "blur",
    },
  ],
  password: [
    {
      required: true,
      message: t("login.password_placeholder"),
      trigger: "blur",
    },
    { min: 6, message: t("login.password_too_short"), trigger: "blur" },
  ],
};

const handleLogin = async () => {
  // 先验证表单
  if (!loginFormRef.value) return;

  const valid = await loginFormRef.value.validate().catch(() => false);
  if (!valid) return;

  // 显示人机验证
  showCaptcha.value = true;
};

const handleCaptchaSuccess = async () => {
  // 人机验证通过，执行登录
  loading.value = true;
  try {
    const res = await login({
      username: loginForm.username,
      password: loginForm.password,
    });

    // 保存用户信息和 token
    userStore.setToken(res.data.token);
    userStore.setUser({
      id: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      roles: res.data.roles,
      permissions: res.data.permissions,
    });

    // 同时保存到 localStorage 以便持久化
    localStorage.setItem("token", res.data.token);

    // 加载用户菜单和权限
    await permissionStore.loadUserPermissions();

    ElMessage.success(t("login.success"));
    // 跳转到管理页面
    router.push("/dashboard");
  } catch (error: any) {
    ElMessage.error(error.message || t("login.failed"));
  } finally {
    loading.value = false;
  }
};

const handleCaptchaCancel = () => {
  // 取消验证，不做任何操作
};

onMounted(() => {
  appStore.loadPublicConfig();
});
</script>

<style scoped>
.login-container {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  min-height: 100vh;
  background: var(--ib-color-canvas);
}

.login-brand {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: var(--ib-space-hero);
  background: var(--ib-color-surface);
  border-right: 1px solid var(--ib-color-hairline-soft);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: var(--ib-radius-lg);
  background: var(--ib-color-ink);
  color: var(--ib-color-on-primary);
  font-size: var(--ib-text-body-sm);
  font-weight: var(--ib-weight-bold);
}

.brand-title {
  max-width: 560px;
  margin: var(--ib-space-lg) 0;
  font-size: clamp(40px, 5vw, 64px);
  font-weight: var(--ib-weight-semibold);
  line-height: var(--ib-leading-tight);
  letter-spacing: -2px;
  color: var(--ib-color-ink-strong);
}

.brand-copy {
  max-width: 420px;
  margin: 0;
  font-size: var(--ib-text-subtitle);
  line-height: 1.6;
  color: var(--ib-color-steel);
}

.login-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ib-space-xxl);
}

.login-locale {
  position: absolute;
  top: var(--ib-space-xl);
  right: var(--ib-space-xl);
}

.login-form {
  width: 100%;
  max-width: 380px;
}

.login-form h2 {
  display: none;
  margin: 0 0 var(--ib-space-lg);
  font-size: var(--ib-text-heading-sm);
  font-weight: var(--ib-weight-semibold);
  text-align: center;
  color: var(--ib-color-ink);
}

.login-form :deep(.el-form-item) {
  margin-bottom: var(--ib-space-md);
}

.login-links {
  margin-top: var(--ib-space-xs);
  text-align: center;
}

.login-links a {
  color: var(--ib-color-ink);
  font-size: var(--ib-text-body-sm);
  text-decoration: none;
}

.login-links a:hover {
  text-decoration: underline;
}

@media (max-width: 900px) {
  .login-container {
    grid-template-columns: 1fr;
  }

  .login-brand {
    display: none;
  }

  .login-panel {
    padding: var(--ib-space-xl);
  }

  .login-form h2 {
    display: block;
  }
}
</style>
