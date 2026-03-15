import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserMenuTree, getUserPermissions, type Menu } from '@/api/system'

export const usePermissionStore = defineStore('permission', () => {
  const menuTree = ref<Menu[]>([])
  const permissions = ref<string[]>([])
  const menuPaths = ref<Set<string>>(new Set())
  const isMenuLoaded = ref(false)

  // 用于防止并发重复请求
  let loadingPromise: Promise<void> | null = null

  /**
   * 从菜单树中递归提取所有菜单项（type C）的路由路径最后一段
   */
  function extractMenuPaths(menus: Menu[]): Set<string> {
    const paths = new Set<string>()
    function walk(list: Menu[]) {
      for (const menu of list) {
        if (menu.menuType === 'C' && menu.path) {
          // 提取路径最后一段: /system/user → user
          const segments = menu.path.split('/').filter(Boolean)
          if (segments.length > 0) {
            paths.add(segments[segments.length - 1])
          }
        }
        if (menu.children && menu.children.length > 0) {
          walk(menu.children)
        }
      }
    }
    walk(menus)
    return paths
  }

  /**
   * 加载当前用户的菜单和权限数据
   */
  async function loadUserPermissions(): Promise<void> {
    if (isMenuLoaded.value) return
    if (loadingPromise) return loadingPromise

    loadingPromise = (async () => {
      try {
        const [menuRes, permsRes] = await Promise.all([
          getUserMenuTree(),
          getUserPermissions()
        ])
        menuTree.value = menuRes.data ?? []
        permissions.value = permsRes.data ?? []
        menuPaths.value = extractMenuPaths(menuTree.value)
        isMenuLoaded.value = true
      } finally {
        loadingPromise = null
      }
    })()

    return loadingPromise
  }

  /**
   * 检查是否拥有指定的按钮权限（OR 逻辑）
   */
  function hasPermission(perm: string | string[]): boolean {
    if (!permissions.value || permissions.value.length === 0) return false
    const perms = Array.isArray(perm) ? perm : [perm]
    return perms.some(p => permissions.value.includes(p))
  }

  /**
   * 检查是否有权访问某个路由子路径
   * @param childPath 路由子路径，如 'user'、'role'
   */
  function hasRouteAccess(childPath: string): boolean {
    return menuPaths.value.has(childPath)
  }

  /**
   * 获取用户第一个可访问的菜单路径
   */
  function getFirstAccessiblePath(): string | null {
    function findFirst(menus: Menu[]): string | null {
      for (const menu of menus) {
        if (menu.menuType === 'C' && menu.path) {
          const segments = menu.path.split('/').filter(Boolean)
          if (segments.length > 0) {
            return segments[segments.length - 1]
          }
        }
        if (menu.children && menu.children.length > 0) {
          const result = findFirst(menu.children)
          if (result) return result
        }
      }
      return null
    }
    return findFirst(menuTree.value)
  }

  /**
   * 清空所有权限和菜单数据
   */
  function clearPermissions() {
    menuTree.value = []
    permissions.value = []
    menuPaths.value = new Set()
    isMenuLoaded.value = false
    loadingPromise = null
  }

  return {
    menuTree,
    permissions,
    menuPaths,
    isMenuLoaded,
    loadUserPermissions,
    hasPermission,
    hasRouteAccess,
    getFirstAccessiblePath,
    clearPermissions
  }
})
