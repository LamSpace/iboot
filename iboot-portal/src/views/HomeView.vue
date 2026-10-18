<template>
  <div class="home">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="navbar-brand">
        <span class="brand-mark">iB</span>
        <span class="brand-text">{{ systemName }}</span>
      </div>
      <nav class="navbar-links">
        <router-link v-if="registerEnabled" to="/register" class="nav-link">{{
          t("home.register")
        }}</router-link>
        <router-link to="/login" class="btn btn-primary nav-cta">{{
          t("home.login")
        }}</router-link>
        <LocalePicker class="locale-picker" />
      </nav>
    </header>

    <!-- 主要内容区 -->
    <main class="main-content">
      <!-- Hero 区域 -->
      <section class="hero">
        <p class="hero-eyebrow">{{ t("home.system_title") }}</p>
        <h1 class="hero-title">{{ systemName }}</h1>
        <p
          class="hero-description"
          v-html="t('home.system_description')"
        ></p>
        <div class="hero-actions">
          <router-link to="/login" class="btn btn-primary">{{
            t("home.start_using")
          }}</router-link>
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
            <div class="feature-icon" :class="`accent-${feature.accent}`">
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
          <span
            class="tech-item"
            v-for="tech in techStack"
            :key="tech"
            >{{ tech }}</span
          >
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
import { onMounted, computed, h } from "vue";
import { useI18n } from "vue-i18n";
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
    accent: "blue",
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
    accent: "magenta",
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
    accent: "purple",
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
    accent: "coral",
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

onMounted(() => {
  appStore.loadPublicConfig();
});
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: var(--ib-color-canvas);
  color: var(--ib-color-ink);
  overflow-x: hidden;
}

/* 导航栏 */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  padding: 0 var(--ib-space-xxl);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--ib-color-hairline-soft);
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: var(--ib-space-sm);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--ib-radius-md);
  background: var(--ib-color-ink);
  color: var(--ib-color-on-primary);
  font-size: var(--ib-text-caption);
  font-weight: var(--ib-weight-bold);
}

.brand-text {
  font-size: var(--ib-text-body);
  font-weight: var(--ib-weight-semibold);
}

.navbar-links {
  display: flex;
  align-items: center;
  gap: var(--ib-space-lg);
}

.nav-link {
  color: var(--ib-color-steel);
  font-size: var(--ib-text-body-sm);
  text-decoration: none;
  transition: color var(--ib-duration-fast) var(--ib-ease);
}

.nav-link:hover {
  color: var(--ib-color-ink);
}

.nav-cta {
  height: 36px;
  padding: 0 var(--ib-space-lg);
  font-size: var(--ib-text-caption);
}

.locale-picker {
  margin-left: var(--ib-space-xxs);
}

/* 主内容 */
.main-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 var(--ib-space-xxl);
}

/* Hero 区域 */
.hero {
  min-height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: var(--ib-space-hero) 0;
}

.hero-eyebrow {
  margin: 0 0 var(--ib-space-md);
  font-size: var(--ib-text-body-sm);
  color: var(--ib-color-steel);
}

.hero-title {
  max-width: 900px;
  margin: 0 0 var(--ib-space-lg);
  font-size: clamp(40px, 7vw, 80px);
  font-weight: var(--ib-weight-semibold);
  line-height: var(--ib-leading-tight);
  letter-spacing: -2px;
  color: var(--ib-color-ink-strong);
}

.hero-description {
  max-width: 640px;
  margin: 0 0 var(--ib-space-xxl);
  font-size: var(--ib-text-subtitle);
  line-height: 1.6;
  color: var(--ib-color-steel);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ib-space-sm);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  padding: 0 var(--ib-space-xl);
  border-radius: var(--ib-radius-full);
  font-size: var(--ib-text-body-sm);
  font-weight: var(--ib-weight-semibold);
  text-decoration: none;
  transition:
    background var(--ib-duration-fast) var(--ib-ease),
    color var(--ib-duration-fast) var(--ib-ease);
}

.btn-primary {
  background: var(--ib-color-ink);
  color: var(--ib-color-on-primary);
}

.btn-primary:hover {
  background: var(--ib-color-charcoal);
}

.btn-secondary {
  border: 1px solid var(--ib-color-ink);
  color: var(--ib-color-ink);
}

.btn-secondary:hover {
  background: var(--ib-color-surface);
}

/* 区块 */
.features,
.tech-stack {
  padding: var(--ib-space-section-lg) 0;
  border-top: 1px solid var(--ib-color-hairline-soft);
}

.section-title {
  margin: 0 0 var(--ib-space-xxl);
  font-size: var(--ib-text-heading-md);
  font-weight: var(--ib-weight-semibold);
  letter-spacing: -0.5px;
  color: var(--ib-color-ink);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--ib-space-md);
}

.feature-card {
  padding: var(--ib-space-xl);
  border: 1px solid var(--ib-color-hairline);
  border-radius: var(--ib-radius-xl);
  background: var(--ib-color-canvas);
}

.feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  margin-bottom: var(--ib-space-lg);
  border-radius: var(--ib-radius-lg);
  background: var(--ib-color-surface);
}

.feature-icon :deep(svg) {
  width: 22px;
  height: 22px;
}

.accent-blue {
  color: var(--ib-color-brand-blue);
}

.accent-magenta {
  color: var(--ib-color-brand-magenta);
}

.accent-purple {
  color: var(--ib-color-brand-purple);
}

.accent-coral {
  color: var(--ib-color-brand-coral);
}

.feature-title {
  margin: 0 0 var(--ib-space-xs);
  font-size: var(--ib-text-card-title);
  font-weight: var(--ib-weight-semibold);
  color: var(--ib-color-ink);
}

.feature-desc {
  margin: 0;
  font-size: var(--ib-text-body-sm);
  line-height: 1.6;
  color: var(--ib-color-steel);
}

.tech-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--ib-space-xs);
}

.tech-item {
  padding: 8px 14px;
  border: 1px solid var(--ib-color-hairline);
  border-radius: var(--ib-radius-full);
  background: var(--ib-color-canvas);
  font-size: var(--ib-text-caption);
  color: var(--ib-color-charcoal);
}

.footer {
  padding: var(--ib-space-xl) var(--ib-space-xxl);
  border-top: 1px solid var(--ib-color-hairline-soft);
  color: var(--ib-color-steel);
  font-size: var(--ib-text-caption);
  text-align: center;
}

@media (max-width: 1024px) {
  .main-content {
    padding: 0 var(--ib-space-xl);
  }

  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 0 var(--ib-space-lg);
  }

  .navbar-links .nav-link {
    display: none;
  }

  .hero-title {
    letter-spacing: -1px;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }
}
</style>
