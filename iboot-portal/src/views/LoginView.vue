<template>
  <div class="login-container">
    <canvas id="three-bg" class="three-bg"></canvas>
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
import type { FormInstance } from "element-plus";
import { useI18n } from "vue-i18n";
import * as THREE from "three";
import { login } from "@/api/auth";
import { useUserStore } from "@/stores/user";
import { usePermissionStore } from "@/stores/permission";
import { useAppStore } from "@/stores/app";
import SliderCaptcha from "@/components/SliderCaptcha.vue";

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

// Three.js 3D 背景
let scene: THREE.Scene;
let camera: THREE.PerspectiveCamera;
let renderer: THREE.WebGLRenderer;
let particles: THREE.Points;
let animationId: number;

const initThree = () => {
  // 初始化场景
  scene = new THREE.Scene();

  // 初始化相机
  camera = new THREE.PerspectiveCamera(
    75,
    window.innerWidth / window.innerHeight,
    0.1,
    1000,
  );
  camera.position.z = 5;

  // 初始化渲染器
  const canvas = document.getElementById("three-bg") as HTMLCanvasElement;
  renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(window.devicePixelRatio);

  // 创建粒子系统
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

  // 添加光源
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
  scene.add(ambientLight);

  const directionalLight = new THREE.DirectionalLight(0xffffff, 0.4);
  directionalLight.position.set(1, 1, 1);
  scene.add(directionalLight);

  // 开始动画循环
  animate();
};

const animate = () => {
  animationId = requestAnimationFrame(animate);

  // 更新粒子位置
  const positions = particles.geometry.attributes.position
    .array as Float32Array;
  for (let i = 0; i < positions.length; i += 3) {
    positions[i + 1] += 0.01; // 让粒子缓慢移动
    if (positions[i + 1] > 10) {
      positions[i + 1] = -10;
    }
  }
  particles.geometry.attributes.position.needsUpdate = true;

  // 添加旋转效果
  particles.rotation.x += 0.001;
  particles.rotation.y += 0.001;

  renderer.render(scene, camera);
};

const handleResize = () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
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
  initThree();
  window.addEventListener("resize", handleResize);
});

onUnmounted(() => {
  cancelAnimationFrame(animationId);
  window.removeEventListener("resize", handleResize);
  renderer.dispose();
});
</script>

<style scoped>
.login-container {
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

.login-form {
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

.login-form h2 {
  text-align: center;
  margin-bottom: 30px;
  background: linear-gradient(135deg, #2c3e50, #34495e, #7f8c8d);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  font-size: 24px;
}

.el-input {
  margin-bottom: 20px;
}

.login-links {
  text-align: center;
}

.login-links a {
  color: #409eff;
  text-decoration: none;
  font-size: 14px;
}

.login-links a:hover {
  text-decoration: underline;
}
</style>
