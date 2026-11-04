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
        // 阶段 2：统一经网关转发；回滚时改回 http://localhost:8080
        target: 'http://localhost:8088',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/iboot\/api/, '/api')
      },
      '/iboot/ai': {
        // 阶段 2：统一经网关转发；回滚时改回 http://localhost:8081
        target: 'http://localhost:8088',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/iboot\/ai/, '/api/ai')
      },
      '/sba': {
        target: 'http://localhost:8089', // 运维控制台（Spring Boot Admin）
        // 必须保留原始 Host：SBA 用请求 Host 生成页面 base href。
        // 改成 8089 会让 SPA 的接口请求跨源，iframe 内表现为空白页。
        changeOrigin: false
      },
      '/actuator': {
        target: 'http://localhost:8089', // 运维控制台自身的 Actuator
        changeOrigin: true
      },
      '/instances': {
        target: 'http://localhost:8089', // 运维控制台实例端点
        changeOrigin: true
      },
      '/applications': {
        target: 'http://localhost:8089', // 运维控制台应用端点
        changeOrigin: true
      }
    }
  }
})