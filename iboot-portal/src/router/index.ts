import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import { useUserStore } from "../stores/user";
import { usePermissionStore } from "../stores/permission";
import { ElMessage } from "element-plus";

// 不需要认证的白名单路由
const whiteList = ["/", "/login", "/register"];

// 定义路由
const routes: Array<RouteRecordRaw> = [
  {
    path: "/",
    name: "Home",
    component: () => import("../views/HomeView.vue"),
  },
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/LoginView.vue"),
  },
  {
    path: "/register",
    name: "Register",
    component: () => import("../views/RegisterView.vue"),
  },
  {
    path: "/dashboard",
    name: "Dashboard",
    component: () => import("../views/DashboardView.vue"),
    meta: { requiresAuth: true },
    children: [
      {
        path: "",
        name: "DashboardHome",
        component: () => import("../views/DashboardHomeView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "user",
        name: "User",
        component: () => import("../views/system/UserView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "role",
        name: "Role",
        component: () => import("../views/system/RoleView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "menu",
        name: "Menu",
        component: () => import("../views/system/MenuView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "dict",
        name: "Dict",
        component: () => import("../views/system/DictView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "config",
        name: "Config",
        component: () => import("../views/system/ConfigView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "dept",
        name: "Dept",
        component: () => import("../views/organization/DeptView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "post",
        name: "Post",
        component: () => import("../views/organization/PostView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "org-chart",
        name: "OrgChart",
        component: () => import("../views/organization/OrgChartView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "notice",
        name: "Notice",
        component: () => import("../views/message/NoticeView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "message-center",
        name: "MessageCenter",
        component: () => import("../views/message/MessageCenterView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "login-log",
        name: "LoginLog",
        component: () => import("../views/security/LoginLogView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "operate-log",
        name: "OperateLog",
        component: () => import("../views/security/OperateLogView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "audit-log",
        name: "AuditLog",
        component: () => import("../views/security/AuditLogView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "file-manage",
        name: "FileManager",
        component: () => import("../views/file/FileManageView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "online-user",
        name: "OnlineUser",
        component: () => import("../views/monitor/OnlineUserView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "server-monitor",
        name: "ServerMonitor",
        component: () => import("../views/monitor/ServerMonitorView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "redis-monitor",
        name: "RedisMonitor",
        component: () => import("../views/monitor/RedisMonitorView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "sql-monitor",
        name: "SqlMonitor",
        component: () => import("../views/monitor/SqlMonitorView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "minio-monitor",
        name: "MinioMonitor",
        component: () => import("../views/monitor/MinioMonitorView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "swagger",
        name: "Swagger",
        component: () => import("../views/monitor/SwaggerView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "performance",
        name: "Performance",
        component: () => import("../views/monitor/PerformanceView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "job",
        name: "Job",
        component: () => import("../views/monitor/JobView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "report",
        name: "Report",
        component: () => import("../views/analysis/ReportView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "usage",
        name: "Usage",
        component: () => import("../views/analysis/UsageView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "prometheus-monitor",
        name: "PrometheusMonitor",
        component: () => import("../views/monitor/PrometheusMonitorView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "grafana-monitor",
        name: "GrafanaMonitor",
        component: () => import("../views/monitor/GrafanaMonitorView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "kibana-monitor",
        name: "KibanaMonitor",
        component: () => import("../views/monitor/KibanaMonitorView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "alertmanager",
        name: "Alertmanager",
        component: () => import("../views/monitor/AlertmanagerView.vue"),
        meta: { requiresAuth: true },
      },
      {
        path: "thanos-query",
        name: "ThanosQuery",
        component: () => import("../views/monitor/ThanosQueryView.vue"),
        meta: { requiresAuth: true },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由导航守卫
router.beforeEach(async (to, _from, next) => {
  // 白名单路由直接放行
  if (whiteList.includes(to.path)) {
    next();
    return;
  }

  const userStore = useUserStore();
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);

  // 需要认证但无 token，跳转登录
  if (requiresAuth && !userStore.token) {
    next("/login");
    return;
  }

  // 有 token，确保菜单权限已加载（处理页面刷新场景）
  if (userStore.token) {
    const permissionStore = usePermissionStore();
    if (!permissionStore.isMenuLoaded) {
      try {
        await permissionStore.loadUserPermissions();
      } catch {
        // 权限加载失败（如 token 过期），清空状态跳转登录
        userStore.clearUser();
        ElMessage.error("登录信息已过期，请重新登录");
        next("/login");
        return;
      }
    }

    // 检查 /dashboard 子路由的访问权限
    if (to.path.startsWith("/dashboard/")) {
      const childPath = to.path.replace("/dashboard/", "");
      if (!permissionStore.hasRouteAccess(childPath)) {
        const firstPath = permissionStore.getFirstAccessiblePath();
        if (firstPath) {
          ElMessage.warning("无权限访问该页面");
          next(`/dashboard/${firstPath}`);
        } else {
          // 用户无任何菜单权限，留在 dashboard
          next("/dashboard");
        }
        return;
      }
    }
  }

  next();
});

export default router;
