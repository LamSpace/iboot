/**
 * SSE 推送客户端
 *
 * 封装 EventSource，提供自动重连、心跳保持等功能
 */

export interface PushEvent {
  id: string
  type: string
  source: string
  time: string
  dataContentType: string
  data: any
  targetUserId?: number
}

export type PushEventCallback = (event: PushEvent) => void

export class PushClient {
  private eventSource: EventSource | null = null
  private callbacks: PushEventCallback[] = []
  private reconnectTimer: number | null = null
  private connected = false
  private baseUrl: string

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || '/iboot/api/push/connect'
  }

  /**
   * 连接到 SSE 推送服务
   */
  connect(token: string): void {
    if (this.eventSource) {
      this.disconnect()
    }

    const url = `${this.baseUrl}${this.baseUrl.includes('?') ? '&' : '?'}t=${token}`

    console.log('[PushClient] 正在连接 SSE:', url)
    this.eventSource = new EventSource(url)

    // 连接打开
    this.eventSource.onopen = () => {
      console.log('[PushClient] SSE 连接已建立')
      this.connected = true
      if (this.reconnectTimer) {
        window.clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
    }

    // 接收消息
    this.eventSource.onmessage = (event) => {
      try {
        const pushEvent: PushEvent = JSON.parse(event.data)
        console.log('[PushClient] 收到推送事件:', pushEvent.type, pushEvent)
        this.notifyCallbacks(pushEvent)
      } catch (error) {
        console.error('[PushClient] 解析推送消息失败:', error)
      }
    }

    // 监听 custom event types
    this.eventSource.addEventListener('heartbeat', (event) => {
      console.log('[PushClient] 收到心跳:', event.data)
    })

    this.eventSource.addEventListener('com.iboot.push.new.message', (event) => {
      try {
        const pushEvent: PushEvent = JSON.parse(event.data)
        console.log('[PushClient] 收到新消息推送:', pushEvent)
        this.notifyCallbacks(pushEvent)
      } catch (error) {
        console.error('[PushClient] 解析新消息推送失败:', error)
      }
    })

    this.eventSource.addEventListener('com.iboot.push.message.read', (event) => {
      try {
        const pushEvent: PushEvent = JSON.parse(event.data)
        console.log('[PushClient] 收到消息已读推送:', pushEvent)
        this.notifyCallbacks(pushEvent)
      } catch (error) {
        console.error('[PushClient] 解析消息已读推送失败:', error)
      }
    })

    // 连接错误
    this.eventSource.onerror = (error) => {
      console.error('[PushClient] SSE 连接错误:', error)
      this.connected = false

      if (this.eventSource) {
        this.eventSource.close()
        this.eventSource = null
      }

      // 3 秒后重连
      if (!this.reconnectTimer) {
        console.log('[PushClient] 准备重连...')
        this.reconnectTimer = window.setTimeout(() => {
          this.reconnectTimer = null
          this.connect(token)
        }, 3000)
      }
    }
  }

  /**
   * 断开连接
   */
  disconnect(): void {
    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.eventSource) {
      console.log('[PushClient] 断开 SSE 连接')
      this.eventSource.close()
      this.eventSource = null
    }

    this.connected = false
    this.callbacks = []
  }

  /**
   * 注册消息回调
   */
  onMessage(callback: PushEventCallback): void {
    this.callbacks.push(callback)
  }

  /**
   * 移除消息回调
   */
  offMessage(callback: PushEventCallback): void {
    const index = this.callbacks.indexOf(callback)
    if (index > -1) {
      this.callbacks.splice(index, 1)
    }
  }

  /**
   * 获取连接状态
   */
  isConnected(): boolean {
    return this.connected
  }

  /**
   * 通知所有回调
   */
  private notifyCallbacks(event: PushEvent): void {
    this.callbacks.forEach((callback) => {
      try {
        callback(event)
      } catch (error) {
        console.error('[PushClient] 回调执行失败:', error)
      }
    })
  }
}

// 导出单例
export const pushClient = new PushClient()
