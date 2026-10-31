<template>
  <div class="callback-page">
    <div class="callback-card">
      <span class="spinner"></span>
      <p>正在完成登录…</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { completeLogin, consumeReturnTo } from "@/utils/oidc";
import { useUserStore } from "@/stores/user";
import { usePermissionStore } from "@/stores/permission";

const router = useRouter();

onMounted(async () => {
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");
  const state = params.get("state");
  const error = params.get("error");

  if (error || !code || !state) {
    await router.replace("/login");
    return;
  }

  try {
    await completeLogin(code, state);
    const userStore = useUserStore();
    userStore.syncFromStorage();
    await usePermissionStore().loadUserPermissions();
    const returnTo = consumeReturnTo();
    await router.replace(returnTo && returnTo !== "/login" ? returnTo : "/dashboard");
  } catch (e: any) {
    ElMessage.error(e?.message || "登录失败，请重试");
    await router.replace("/login");
  }
});
</script>

<style scoped>
.callback-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--ib-color-canvas);
}

.callback-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
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
