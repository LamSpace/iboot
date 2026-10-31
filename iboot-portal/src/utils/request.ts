import axios, {
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from "axios";
import { ElMessage } from "element-plus";
import { useUserStore } from "../stores/user";
import router from "../router";
import { beginLogin, ensureFreshToken, readTokenSet } from "./oidc";

/** 已重试过的请求不会再续期，避免 401 循环。 */
interface RetriableConfig extends InternalAxiosRequestConfig {
  __retried?: boolean;
}

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/iboot/api", // API 的 base_url
  timeout: 50000, // 请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const tokens = readTokenSet();
    if (tokens?.accessToken) {
      config.headers.Authorization = `Bearer ${tokens.accessToken}`;
    }
    // 添加 API 版本头
    config.headers["Accept-Version"] = "1";
    return config;
  },
  (error: any) => {
    console.error(error); // for debug
    return Promise.reject(error);
  },
);

/**
 * 判断业务状态码是否表示认证失效。
 *
 * <p>{@code 40101} 表示令牌版本已停用（双栈窗口关闭后的旧令牌），与普通 401 同等处理：
 * 先续期，续期失败再重新登录。
 *
 * @param code 业务状态码
 * @returns 认证失效时返回 true
 */
function isAuthFailure(code: number): boolean {
  return code === 401 || code === 40101;
}

/**
 * 尝试续期并重放一次请求。
 *
 * @param config 触发 401 的请求配置
 * @returns 续期成功时返回重放的响应；否则返回 null
 */
async function retryAfterRefresh(config?: RetriableConfig) {
  if (!config || config.__retried) {
    return null;
  }
  const tokens = await ensureFreshToken(0);
  if (!tokens) {
    return null;
  }
  config.__retried = true;
  config.headers.Authorization = `Bearer ${tokens.accessToken}`;
  return service(config);
}

/**
 * 续期失败后结束本地会话并重新登录。
 */
async function requireLogin(): Promise<void> {
  const userStore = useUserStore();
  userStore.clearUser();
  ElMessage({ message: "登录已过期，请重新登录", type: "warning", duration: 3 * 1000 });
  await beginLogin(router.currentRoute.value.fullPath);
}

/**
 * 判断是否为 CloudEvent 响应
 */
function isCloudEvent(data: any): boolean {
  return (
    data &&
    typeof data === "object" &&
    data.specversion === "1.0" &&
    data.id &&
    data.type &&
    data.source &&
    data.datacontenttype === "application/json" &&
    data.data !== undefined
  );
}

// 响应拦截器
service.interceptors.response.use(
  async (response: AxiosResponse) => {
    const res = response.data;

    // 如果是 Blob 数据（二进制文件下载等），直接返回
    if (res instanceof Blob) {
      return res;
    }

    // 检测是否为 CloudEvents 格式响应
    if (isCloudEvent(res)) {
      // CloudEvents 格式：解包获取内部的 data（ApiResponse）
      const cloudEvent = res as any;
      const innerData = cloudEvent.data;

      // 如果内部数据是 ApiResponse 格式
      if (innerData && innerData.code !== undefined) {
        // 如果是错误响应，显示错误消息
        if (innerData.code !== 200) {
          if (isAuthFailure(innerData.code)) {
            const retried = await retryAfterRefresh(response.config as RetriableConfig);
            if (retried) {
              return retried;
            }
            await requireLogin();
          }
          ElMessage({
            message: innerData.message || "Error",
            type: "error",
            duration: 5 * 1000,
          });
          return Promise.reject(new Error(innerData.message || "Error"));
        }
        // 成功响应，返回内部数据
        return innerData;
      }
      // 如果内部数据格式不对，返回原始 CloudEvent 数据
      return cloudEvent;
    }

    // 非 CloudEvents 格式：按原有逻辑处理
    // 如果自定义状态码不是 200，则判断为错误
    if (res.code && res.code !== 200) {
      if (isAuthFailure(res.code)) {
        const retried = await retryAfterRefresh(response.config as RetriableConfig);
        if (retried) {
          return retried;
        }
        await requireLogin();
      }
      ElMessage({
        message: res.message || "Error",
        type: "error",
        duration: 5 * 1000,
      });

      // 50008: Illegal token; 50012: Other clients logged in; 50014: Token expired;
      if (res.code === 50008 || res.code === 50012 || res.code === 50014) {
        // TODO: 重新登录处理
      }
      return Promise.reject(new Error(res.message || "Error"));
    } else {
      return res;
    }
  },
  async (error: any) => {
    console.error(error); // for debug

    // 处理 401 未认证错误：先尝试续期并重放一次，失败再走重新登录
    if (error.response?.status === 401) {
      const retried = await retryAfterRefresh(error.config as RetriableConfig);
      if (retried) {
        return retried;
      }
      await requireLogin();
      return Promise.reject(error);
    }

    ElMessage({
      message: error.response?.data?.message || error.message || "请求失败",
      type: "error",
      duration: 5 * 1000,
    });
    return Promise.reject(error);
  },
);

export default service;
