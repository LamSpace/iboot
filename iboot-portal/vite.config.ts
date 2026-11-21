import fs from 'node:fs'
import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 本地 TLS：默认启用；证书由 scripts/tls-init.sh 生成。
// 关闭 TLS 仅用于排障，回归与验收不得在该模式下判定通过。
const tlsEnabled = (process.env.IBOOT_TLS_ENABLED ?? 'true') !== 'false'
const certDir = process.env.IBOOT_TLS_CERT_DIR
  || fileURLToPath(new URL('../docker/certs', import.meta.url))
// 门户是唯一公开入口：OIDC 与 /api 都由 Vite 反代到网关。
// OIDC 必须保留原始 Host，否则认证服务会按 localhost:8088 生成回调与页面地址。
// 内部 TLS 默认开启；关闭时仅用于宿主机排障（Docker 不提供该回退）。
const internalTls = (process.env.IBOOT_INTERNAL_TLS_ENABLED ?? 'true') !== 'false'
const gateway = `${internalTls ? 'https' : 'http'}://localhost:8088`
const ops = `${internalTls ? 'https' : 'http'}://localhost:8089`

export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    // 构建阶段不启动开发服务器，也不读取证书；只有 vite serve 才需要 HTTPS。
    https: command === 'serve' && tlsEnabled
      ? {
          cert: fs.readFileSync(`${certDir}/portal.pem`),
          key: fs.readFileSync(`${certDir}/portal-key.pem`),
        }
      : undefined,
    proxy: {
      // CLI 直接使用 /api；门户自身仍使用 /iboot/api
      '/api': {
        target: gateway,
        changeOrigin: true,
      },
      '/oauth2': {
        target: gateway,
        changeOrigin: false,
        xfwd: true,
      },
      '/.well-known': {
        target: gateway,
        changeOrigin: false,
        xfwd: true,
      },
      '/login': {
        target: gateway,
        changeOrigin: false,
        xfwd: true,
      },
      '/connect': {
        target: gateway,
        changeOrigin: false,
        xfwd: true,
      },
      '/iboot/api': {
        // 阶段 2：统一经网关转发；回滚时改回 http://localhost:8080
        target: gateway,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/iboot\/api/, '/api'),
      },
      '/iboot/ai': {
        // 阶段 2：统一经网关转发；回滚时改回 http://localhost:8081
        target: gateway,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/iboot\/ai/, '/api/ai'),
      },
      // Nacos 控制台只在本地开发内嵌：代理到控制台 SPA 与 /v3 API，
      // 并删除 X-Frame-Options（Nacos 的 GET 文档响应带 DENY）。
      '/nacos-console': {
        target: 'https://localhost:8849',
        changeOrigin: true,
        ws: true,
        rewrite: (path) =>
          path
            .replace(/^\/nacos-console\/v3/, '/v3')
            .replace(/^\/nacos-console/, '/next'),
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            delete proxyRes.headers['x-frame-options']
          })
        },
      },
      '/sba': {
        target: ops, // 运维控制台（Spring Boot Admin）
        // 必须保留原始 Host：SBA 用请求 Host 生成页面 base href。
        // 改成 8089 会让 SPA 的接口请求跨源，iframe 内表现为空白页。
        changeOrigin: false,
      },
      '/actuator': {
        target: ops, // 运维控制台自身的 Actuator
        changeOrigin: true,
      },
      '/instances': {
        target: ops, // 运维控制台实例端点
        changeOrigin: true,
      },
      '/applications': {
        target: ops, // 运维控制台应用端点
        changeOrigin: true,
      },
    },
  },
}))
