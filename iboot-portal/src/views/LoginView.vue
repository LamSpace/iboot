<template>
  <div class="login-redirect">
    <span class="spinner"></span>
    <p>正在跳转到登录页…</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { beginLogin } from "@/utils/oidc";

/**
 * 登录页已迁移到认证服务。
 *
 * 本视图只负责把浏览器送往认证服务的登录页，账号密码不再经过门户。
 */
onMounted(() => {
  void beginLogin();
});
</script>

<style scoped>
.login-redirect {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  min-height: 100vh;
  background: var(--ib-color-canvas);
  color: var(--ib-color-steel);
  font-size: var(--ib-text-body-sm);
}

.spinner {
  width: 28px;
  height: 28px;
  border: 2px solid var(--ib-color-hairline-soft);
  border-top-color: var(--ib-color-ink);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
