<template>
  <div class="register-container">
    <canvas id="three-bg" class="three-bg"></canvas>
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
          <router-link to="/login">{{ t("register.has_account") }}</router-link>
        </div>
      </el-form>
    </div>

    <!-- 滑块验证组件 -->
    <SliderCaptcha
      v-model="showCaptcha"
      @success="handleCaptchaSuccess"
      @cancel="handleCaptchaCancel"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import type { FormInstance, FormRules } from "element-plus";
import { useI18n } from "vue-i18n";
import * as THREE from "three";
import { register } from "@/api/auth";
import { useAppStore } from "@/stores/app";
import SliderCaptcha from "@/components/SliderCaptcha.vue";

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

// Three.js 3D 背景
let scene: THREE.Scene;
let camera: THREE.PerspectiveCamera;
let renderer: THREE.WebGLRenderer;
let particles: THREE.Points;
let animationId: number;

const initThree = () => {
  scene = new THREE.Scene();

  camera = new THREE.PerspectiveCamera(
    75,
    window.innerWidth / window.innerHeight,
    0.1,
    1000,
  );
  camera.position.z = 5;

  const canvas = document.getElementById("three-bg") as HTMLCanvasElement;
  renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(window.devicePixelRatio);

  const particleCount = 2000;
  const positions = new Float32Array(particleCount * 3);

  for (let i = 0; i < particleCount * 3; i += 3) {
    positions[i] = (Math.random() - 0.5) * 20;
    positions[i + 1] = (Math.random() - 0.5) * 20;
    positions[i + 2] = (Math.random() - 0.5) * 20;
  }

  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));

  const material = new THREE.PointsMaterial({
    color: 0x409eff,
    size: 0.05,
    transparent: true,
    opacity: 0.8,
  });

  particles = new THREE.Points(geometry, material);
  scene.add(particles);

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.4);
  directionalLight.position.set(1, 1, 1);
  scene.add(directionalLight);

  animate();
};

const animate = () => {
  animationId = requestAnimationFrame(animate);

  const positions = particles.geometry.attributes.position
    .array as Float32Array;
  for (let i = 0; i < positions.length; i += 3) {
    positions[i + 1] += 0.01;
    if (positions[i + 1] > 10) {
      positions[i + 1] = -10;
    }
  }
  particles.geometry.attributes.position.needsUpdate = true;

  particles.rotation.x += 0.001;
  particles.rotation.y += 0.001;

  renderer.render(scene, camera);
};

const handleResize = () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
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
    return;
  }
  initThree();
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  cancelAnimationFrame(animationId);
  window.removeEventListener("resize", handleResize);
  if (renderer) {
    renderer.dispose();
  }
});
</script>

<style scoped>
.register-container {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
}

.three-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.register-form {
  position: relative;
  z-index: 10;
  width: 400px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  color: #2c3e50;
}

.register-form h2 {
  text-align: center;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #2c3e50, #34495e, #7f8c8d);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-size: 24px;
}

.register-subtitle {
  text-align: center;
  margin-bottom: 24px;
  color: #606266;
  font-size: 14px;
}

.register-links {
  text-align: center;
}

.register-links a {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
}

.register-links a:hover {
  text-decoration: underline;
}

.el-input {
  margin-bottom: 20px;
}
</style>
