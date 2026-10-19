<template>
  <div class="register-container">
    <section class="register-brand">
      <span class="brand-mark">iB</span>
      <h1 class="brand-title">{{ systemName }}</h1>
      <p class="brand-copy">{{ t("home.system_title") }}</p>
    </section>

    <section class="register-panel">
      <div class="register-actions">
        <ThemePicker />
        <LocalePicker class="register-locale" />
      </div>
      <div class="register-form">
        <h2>{{ systemName }}</h2>
        <p class="register-subtitle">{{ t("register.title") }}</p>
        <el-form
          :model="registerForm"
          :rules="registerRules"
          ref="registerFormRef"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              :placeholder="t('register.username_placeholder')"
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              :placeholder="t('register.password_placeholder')"
              prefix-icon="Lock"
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              :placeholder="t('register.confirm_password_placeholder')"
              prefix-icon="Lock"
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <el-form-item prop="nickname">
            <el-input
              v-model="registerForm.nickname"
              :placeholder="t('register.nickname_placeholder')"
              prefix-icon="UserFilled"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="handleRegister"
              :loading="loading"
              style="width: 100%"
            >
              {{ t("register.submit") }}
            </el-button>
          </el-form-item>
          <div class="register-links">
            <router-link to="/login">{{
              t("register.has_account")
            }}</router-link>
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
import type { FormInstance, FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import { register } from "@/api/auth";
import { useAppStore } from "@/stores/app";
import SliderCaptcha from "@/components/SliderCaptcha.vue";
import LocalePicker from "@/components/LocalePicker.vue";
import ThemePicker from "@/components/ThemePicker.vue";

interface RegisterForm {
  username: string;
  password: string;
  confirmPassword: string;
  nickname: string;
}

const router = useRouter();
const appStore = useAppStore();
const { t } = useI18n();
const loading = ref(false);
const registerFormRef = ref<FormInstance>();
const showCaptcha = ref(false);

const systemName = computed(() => appStore.systemName);

const registerForm = reactive<RegisterForm>({
  username: "",
  password: "",
  confirmPassword: "",
  nickname: "",
});

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error(t("register.password_not_match")));
  } else {
    callback();
  }
};

const registerRules: FormRules = {
  username: [
    {
      required: true,
      message: t("register.username_placeholder"),
      trigger: "blur",
    },
    {
      min: 2,
      max: 30,
      message: t("register.username_length"),
      trigger: "blur",
    },
  ],
  password: [
    {
      required: true,
      message: t("register.password_placeholder"),
      trigger: "blur",
    },
    {
      min: 6,
      max: 20,
      message: t("register.password_length"),
      trigger: "blur",
    },
  ],
  confirmPassword: [
    {
      required: true,
      message: t("register.confirm_password_placeholder"),
      trigger: "blur",
    },
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
  nickname: [
    { max: 30, message: t("register.nickname_length"), trigger: "blur" },
  ],
};

const handleRegister = async () => {
  if (!registerFormRef.value) return;

  const valid = await registerFormRef.value.validate().catch(() => false);
  if (!valid) return;

  showCaptcha.value = true;
};

const handleCaptchaSuccess = async () => {
  loading.value = true;
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      nickname: registerForm.nickname || undefined,
    });

    ElMessage.success(t("register.success"));
    router.push("/login");
  } catch (error: any) {
    ElMessage.error(error.message || t("register.failed"));
  } finally {
    loading.value = false;
  }
};

const handleCaptchaCancel = () => {
  // 取消验证，不做任何操作
};

onMounted(() => {
  appStore.loadPublicConfig();
  // 如果注册未开启，跳转回登录页
  if (appStore.configLoaded && !appStore.registerEnabled) {
    ElMessage.warning(t("register.not_enabled"));
    router.push("/login");
  }
});
</script>

<style scoped>
.register-container {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  min-height: 100vh;
  background: var(--ib-color-canvas);
}

.register-brand {
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

.register-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ib-space-xxl);
}

.register-actions {
  position: absolute;
  top: var(--ib-space-xl);
  right: var(--ib-space-xl);
  display: flex;
  align-items: center;
  gap: var(--ib-space-sm);
}

.register-form {
  width: 100%;
  max-width: 400px;
}

.register-form h2 {
  display: none;
  margin: 0;
  font-size: var(--ib-text-heading-sm);
  font-weight: var(--ib-weight-semibold);
  text-align: center;
  color: var(--ib-color-ink);
}

.register-subtitle {
  margin: 0 0 var(--ib-space-lg);
  font-size: var(--ib-text-body-sm);
  color: var(--ib-color-steel);
  text-align: center;
}

.register-form :deep(.el-form-item) {
  margin-bottom: var(--ib-space-md);
}

.register-links {
  margin-top: var(--ib-space-xs);
  text-align: center;
}

.register-links a {
  color: var(--ib-color-ink);
  font-size: var(--ib-text-body-sm);
  text-decoration: none;
}

.register-links a:hover {
  text-decoration: underline;
}

@media (max-width: 900px) {
  .register-container {
    grid-template-columns: 1fr;
  }

  .register-brand {
    display: none;
  }

  .register-panel {
    padding: var(--ib-space-xl);
  }

  .register-form h2 {
    display: block;
    margin-bottom: var(--ib-space-xs);
  }
}
</style>
