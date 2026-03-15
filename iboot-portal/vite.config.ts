import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/iboot/api': {
        target: 'http://localhost:8080', // 后端服务地址
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/iboot\/api/, '/api')
      },
      '/sba': {
        target: 'http://localhost:8080', // Spring Boot Admin 监控页面
        changeOrigin: true
      },
      '/actuator': {
        target: 'http://localhost:8080', // Actuator 端点（SBA 需要）
        changeOrigin: true
      },
      '/instances': {
        target: 'http://localhost:8080', // SBA 实例注册端点
        changeOrigin: true
      },
      '/applications': {
        target: 'http://localhost:8080', // SBA 应用端点
        changeOrigin: true
      }
    }
  }
})