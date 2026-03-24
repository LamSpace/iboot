<template>
  <div class="home">
    <!-- 背景动画 -->
    <canvas id="three-home-bg" class="three-home-bg"></canvas>

    <!-- 导航栏 -->
    <header class="navbar">
      <div class="navbar-brand">
        <span class="brand-text">{{ systemName }}</span>
      </div>
      <nav class="navbar-links">
        <router-link v-if="registerEnabled" to="/register" class="nav-link">{{
          t("home.register")
        }}</router-link>
        <router-link to="/login" class="nav-link nav-link-primary">{{
          t("home.login")
        }}</router-link>
        <!-- 语言切换 -->
        <LocalePicker class="locale-picker" />
      </nav>
    </header>

    <!-- 主要内容区 -->
    <main class="main-content">
      <!-- Hero 区域 -->
      <section class="hero">
        <h1 class="hero-title">
          <span class="gradient-text">{{ systemName }}</span>
        </h1>
        <p class="hero-subtitle">{{ t("home.system_title") }}</p>
        <p class="hero-description" v-html="t('home.system_description')"></p>
        <div class="hero-actions">
          <router-link to="/login" class="btn btn-primary">
            <span>{{ t("home.start_using") }}</span>
            <svg
              class="btn-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M13 7l5 5m0 0l-5 5m5-5H6"
              />
            </svg>
          </router-link>
          <a href="#features" class="btn btn-secondary">{{
            t("home.learn_more")
          }}</a>
        </div>
      </section>

      <!-- 特性展示 -->
      <section id="features" class="features">
        <h2 class="section-title">{{ t("home.features_title") }}</h2>
        <div class="features-grid">
          <div
            class="feature-card"
            v-for="(feature, index) in features"
            :key="index"
          >
            <div class="feature-icon" :style="{ background: feature.gradient }">
              <component :is="feature.icon" />
            </div>
            <h3 class="feature-title">{{ feature.title }}</h3>
            <p class="feature-desc">{{ feature.description }}</p>
          </div>
        </div>
      </section>

      <!-- 技术栈展示 -->
      <section class="tech-stack">
        <h2 class="section-title">{{ t("home.tech_stack_title") }}</h2>
        <div class="tech-grid">
          <div
            class="tech-item"
            v-for="(tech, index) in techStack"
            :key="index"
          >
            <span class="tech-name">{{ tech }}</span>
          </div>
        </div>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <p>© {{ currentYear }} {{ systemName }}. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, computed, h } from "vue";
import { useI18n } from "vue-i18n";
import * as THREE from "three";
import { useAppStore } from "@/stores/app";
import LocalePicker from "@/components/LocalePicker.vue";

const { t } = useI18n();
const appStore = useAppStore();
const systemName = computed(() => appStore.systemName);
const registerEnabled = computed(() => appStore.registerEnabled);
const currentYear = new Date().getFullYear();

// 特性数据
const features = computed(() => [
  {
    title: t("home.feature_permission_title"),
    description: t("home.feature_permission_desc"),
    gradient: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
    icon: h(
      "svg",
      {
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        "stroke-width": 2,
      },
      [
        h("path", {
          "stroke-linecap": "round",
          "stroke-linejoin": "round",
          d: "M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z",
        }),
      ],
    ),
  },
  {
    title: t("home.feature_architecture_title"),
    description: t("home.feature_architecture_desc"),
    gradient: "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
    icon: h(
      "svg",
      {
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        "stroke-width": 2,
      },
      [
        h("path", {
          "stroke-linecap": "round",
          "stroke-linejoin": "round",
          d: "M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10",
        }),
      ],
    ),
  },
  {
    title: t("home.feature_security_title"),
    description: t("home.feature_security_desc"),
    gradient: "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
    icon: h(
      "svg",
      {
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        "stroke-width": 2,
      },
      [
        h("path", {
          "stroke-linecap": "round",
          "stroke-linejoin": "round",
          d: "M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z",
        }),
      ],
    ),
  },
  {
    title: t("home.feature_efficiency_title"),
    description: t("home.feature_efficiency_desc"),
    gradient: "linear-gradient(135deg, #fa709a 0%, #fee140 100%)",
    icon: h(
      "svg",
      {
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        "stroke-width": 2,
      },
      [
        h("path", {
          "stroke-linecap": "round",
          "stroke-linejoin": "round",
          d: "M13 10V3L4 14h7v7l9-11h-7z",
        }),
      ],
    ),
  },
]);

// 技术栈数据
const techStack = [
  "Vue 3",
  "TypeScript",
  "Vite",
  "Element Plus",
  "Spring Boot",
  "MyBatis-Plus",
  "Redis",
  "MySQL",
];

// Three.js 背景
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
  camera.position.z = 50;

  const canvas = document.getElementById("three-home-bg") as HTMLCanvasElement;
  renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

  // 创建粒子系统
  const particleCount = 1500;
  const positions = new Float32Array(particleCount * 3);
  const colors = new Float32Array(particleCount * 3);

  const colorPalette = [
    new THREE.Color(0x667eea),
    new THREE.Color(0x764ba2),
    new THREE.Color(0xf093fb),
    new THREE.Color(0xf5576c),
    new THREE.Color(0x4facfe),
    new THREE.Color(0x00f2fe),
    new THREE.Color(0xfa709a),
    new THREE.Color(0xfee140),
  ];

  for (let i = 0; i < particleCount; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 200;
    positions[i * 3 + 1] = (Math.random() - 0.5) * 200;
    positions[i * 3 + 2] = (Math.random() - 0.5) * 200;

    const color = colorPalette[Math.floor(Math.random() * colorPalette.length)];
    colors[i * 3] = color.r;
    colors[i * 3 + 1] = color.g;
    colors[i * 3 + 2] = color.b;
  }

  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
  geometry.setAttribute("color", new THREE.BufferAttribute(colors, 3));

  const material = new THREE.PointsMaterial({
    size: 0.5,
    vertexColors: true,
    transparent: true,
    opacity: 0.8,
    sizeAttenuation: true,
  });

  particles = new THREE.Points(geometry, material);
  scene.add(particles);

  animate();
};

const animate = () => {
  animationId = requestAnimationFrame(animate);

  particles.rotation.x += 0.0003;
  particles.rotation.y += 0.0005;

  renderer.render(scene, camera);
};

const handleResize = () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
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
.home {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(180deg, #0d0d0d 0%, #1a1a2e 50%, #16213e 100%);
  color: #ffffff;
  overflow-x: hidden;
}

.three-home-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

/* 导航栏 */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 3rem;
  background: rgba(13, 13, 13, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.brand-text {
  font-size: 1.5rem;
  font-weight: 700;
  background: linear-gradient(90deg, #667eea, #f093fb, #4facfe);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.locale-picker {
  margin-left: 0.5rem;
}

.nav-link {
  text-decoration: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.95rem;
  font-weight: 500;
  transition: color 0.3s ease;
}

.nav-link:hover {
  color: #ffffff;
}

.nav-link-primary {
  padding: 0.5rem 1.25rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 6px;
  color: #ffffff;
}

.nav-link-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* 主内容 */
.main-content {
  position: relative;
  z-index: 10;
  padding-top: 80px;
}

/* Hero 区域 */
.hero {
  min-height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 4rem 2rem;
}

.hero-title {
  font-size: clamp(3rem, 8vw, 5rem);
  font-weight: 800;
  margin-bottom: 1rem;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: linear-gradient(
    90deg,
    #667eea,
    #f093fb,
    #f5576c,
    #4facfe,
    #fee140
  );
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: gradient-flow 4s ease infinite;
}

@keyframes gradient-flow {
  0%,
  100% {
    background-position: 0% center;
  }
  50% {
    background-position: 100% center;
  }
}

.hero-subtitle {
  font-size: clamp(1.5rem, 3vw, 2rem);
  font-weight: 300;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 1.5rem;
}

.hero-description {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.8;
  max-width: 600px;
  margin-bottom: 3rem;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 2rem;
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  border: none;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: transparent;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.btn-icon {
  width: 20px;
  height: 20px;
}

/* 特性区域 */
.features {
  padding: 6rem 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  text-align: center;
  margin-bottom: 4rem;
  background: linear-gradient(90deg, #ffffff 0%, rgba(255, 255, 255, 0.7) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.feature-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 2rem;
  transition: all 0.3s ease;
}

.feature-card:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.15);
  transform: translateY(-5px);
}

.feature-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.feature-icon svg {
  width: 28px;
  height: 28px;
  color: #ffffff;
}

.feature-title {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 0.75rem;
  color: #ffffff;
}

.feature-desc {
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
}

/* 技术栈区域 */
.tech-stack {
  padding: 4rem 2rem 6rem;
  max-width: 1200px;
  margin: 0 auto;
}

.tech-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 1rem;
}

.tech-item {
  padding: 0.75rem 1.5rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.tech-item:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

/* 页脚 */
.footer {
  position: relative;
  z-index: 10;
  padding: 2rem;
  text-align: center;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.footer p {
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.875rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navbar {
    padding: 1rem 1.5rem;
  }

  .hero {
    padding: 2rem 1rem;
  }

  .features,
  .tech-stack {
    padding: 3rem 1rem;
  }

  .section-title {
    font-size: 2rem;
    margin-bottom: 2rem;
  }
}
</style>
