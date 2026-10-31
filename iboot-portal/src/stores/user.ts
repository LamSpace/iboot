import { defineStore } from "pinia";
import { ref } from "vue";
import { usePermissionStore } from "./permission";
import { TOKENS_UPDATED_EVENT, clearTokenSet, readCurrentUser, readTokenSet } from "@/utils/oidc";

interface User {
  id?: number;
  username: string;
  nickname?: string;
  email?: string;
  avatar?: string;
  roles?: string[];
  permissions?: string[];
}

export const useUserStore = defineStore("user", () => {
  const userInfo = ref<User | null>(null);
  const token = ref<string>(readTokenSet()?.accessToken || "");
  const isAuthenticated = ref<boolean>(!!token.value);

  function setUser(user: User) {
    userInfo.value = user;
    isAuthenticated.value = true;
  }

  function clearUser() {
    userInfo.value = null;
    token.value = "";
    isAuthenticated.value = false;
    clearTokenSet();
    // 联动清空权限和菜单数据
    const permissionStore = usePermissionStore();
    permissionStore.clearPermissions();
  }

  /**
   * 与本地令牌同步，并刷新内存中的用户信息。
   *
   * 令牌续期后由 {@link TOKENS_UPDATED_EVENT} 触发，保证 SSE、下载等直接读取令牌的调用点
   * 拿到的是最新访问令牌。
   */
  function syncFromStorage() {
    const tokens = readTokenSet();
    token.value = tokens?.accessToken || "";
    isAuthenticated.value = !!token.value;
    if (token.value) {
      const current = readCurrentUser();
      if (current) {
        setUser({
          id: current.id,
          username: current.username,
          nickname: current.nickname,
          roles: current.roles,
          permissions: current.permissions,
        });
      }
    }
  }

  if (typeof window !== "undefined") {
    window.addEventListener(TOKENS_UPDATED_EVENT, syncFromStorage);
    window.addEventListener("storage", (event) => {
      if (event.key === null || event.key === "token") {
        syncFromStorage();
      }
    });
  }

  return {
    userInfo,
    token,
    isAuthenticated,
    setUser,
    clearUser,
    syncFromStorage,
  };
});
