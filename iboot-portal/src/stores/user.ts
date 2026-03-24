import { defineStore } from "pinia";
import { ref } from "vue";
import { usePermissionStore } from "./permission";

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
  const token = ref<string>(localStorage.getItem("token") || "");
  const isAuthenticated = ref<boolean>(!!localStorage.getItem("token"));

  function setUser(user: User) {
    userInfo.value = user;
    isAuthenticated.value = true;
  }

  function clearUser() {
    userInfo.value = null;
    token.value = "";
    isAuthenticated.value = false;
    localStorage.removeItem("token");
    // 联动清空权限和菜单数据
    const permissionStore = usePermissionStore();
    permissionStore.clearPermissions();
  }

  function setToken(authToken: string) {
    token.value = authToken;
    localStorage.setItem("token", authToken);
  }

  return {
    userInfo,
    token,
    isAuthenticated,
    setUser,
    clearUser,
    setToken,
  };
});
