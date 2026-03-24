import type { Directive } from "vue";
import { usePermissionStore } from "@/stores/permission";

/**
 * v-permission 按钮级权限控制指令
 *
 * 用法:
 *   v-permission="'user:add'"          - 单个权限
 *   v-permission="['user:add']"        - 数组形式（OR 逻辑，满足任一即可）
 *   v-permission="['user:add','user:edit']" - 多个权限，满足任一即可
 */
export const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const { value } = binding;
    if (!value) return;

    const permissionStore = usePermissionStore();
    if (!permissionStore.hasPermission(value)) {
      el.parentNode?.removeChild(el);
    }
  },
};
