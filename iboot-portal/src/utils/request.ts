import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import router from '../router'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/iboot/api', // API 的 base_url
  timeout: 50000, // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    // 添加 API 版本头
    config.headers['Accept-Version'] = '1'
    return config
  },
  (error: any) => {
    console.error(error) // for debug
    return Promise.reject(error)
  }
)

/**
 * 判断是否为 CloudEvent 响应
 */
function isCloudEvent(data: any): boolean {
  return (
    data &&
    typeof data === 'object' &&
    data.specversion === '1.0' &&
    data.id &&
    data.type &&
    data.source &&
    data.datacontenttype === 'application/json' &&
    data.data !== undefined
  )
}

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // 如果是 Blob 数据（二进制文件下载等），直接返回
    if (res instanceof Blob) {
      return res
    }

    // 检测是否为 CloudEvents 格式响应
    if (isCloudEvent(res)) {
      // CloudEvents 格式：解包获取内部的 data（ApiResponse）
      const cloudEvent = res as any
      const innerData = cloudEvent.data

      // 如果内部数据是 ApiResponse 格式
      if (innerData && innerData.code !== undefined) {
        // 如果是错误响应，显示错误消息
        if (innerData.code !== 200) {
          ElMessage({
            message: innerData.message || 'Error',
            type: 'error',
            duration: 5 * 1000
          })
          return Promise.reject(new Error(innerData.message || 'Error'))
        }
        // 成功响应，返回内部数据
        return innerData
      }
      // 如果内部数据格式不对，返回原始 CloudEvent 数据
      return cloudEvent
    }

    // 非 CloudEvents 格式：按原有逻辑处理
    // 如果自定义状态码不是 200，则判断为错误
    if (res.code && res.code !== 200) {
      ElMessage({
        message: res.message || 'Error',
        type: 'error',
        duration: 5 * 1000
      })

      // 50008: Illegal token; 50012: Other clients logged in; 50014: Token expired;
      if (res.code === 50008 || res.code === 50012 || res.code === 50014) {
        // TODO: 重新登录处理
      }
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res
    }
  },
  (error: any) => {
    console.error(error) // for debug

    // 处理 401 未认证错误
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.clearUser()
      ElMessage({
        message: '登录已过期，请重新登录',
        type: 'warning',
        duration: 3 * 1000
      })
      router.push('/login')
      return Promise.reject(error)
    }

    ElMessage({
      message: error.response?.data?.message || error.message || '请求失败',
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
