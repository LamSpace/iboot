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

export default defineConfig(({ command }) => {
  // 服务间 mTLS：Vite 代理到网关/运维控制台时出示门户客户端证书。
  // 构建阶段不读取证书，避免镜像构建依赖本地证书目录。
  const proxyClientTls = command === 'serve' && internalTls
    ? {
        ca: fs.readFileSync(`${certDir}/ca/rootCA.pem`),
        cert: fs.readFileSync(`${certDir}/portal.pem`),
        key: fs.readFileSync(`${certDir}/portal-key.pem`),
      }
    : {}
  // http-proxy 从 target 对象读取客户端证书，而不是代理项的顶层选项。
  const gatewayTarget = new URL(gateway)
  const opsTarget = new URL(ops)
  Object.assign(gatewayTarget, proxyClientTls)
  Object.assign(opsTarget, proxyClientTls)

  return {
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
        target: gatewayTarget,
        changeOrigin: true,
      },
      '/oauth2': {
        target: gatewayTarget,
        changeOrigin: false,
        xfwd: true,
      },
      '/.well-known': {
        target: gatewayTarget,
        changeOrigin: false,
        xfwd: true,
      },
      '/login': {
        target: gatewayTarget,
        changeOrigin: false,
        xfwd: true,
      },
      '/connect': {
        target: gatewayTarget,
        changeOrigin: false,
        xfwd: true,
      },
      '/iboot/api': {
        // 阶段 2：统一经网关转发；回滚时改回 http://localhost:8080
        target: gatewayTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/iboot\/api/, '/api'),
      },
      '/iboot/ai': {
        // 阶段 2：统一经网关转发；回滚时改回 http://localhost:8081
        target: gatewayTarget,
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
        target: opsTarget, // 运维控制台（Spring Boot Admin）
        // 必须保留原始 Host：SBA 用请求 Host 生成页面 base href。
        // 改成 8089 会让 SPA 的接口请求跨源，iframe 内表现为空白页。
        changeOrigin: false,
      },
      '/actuator': {
        target: opsTarget, // 运维控制台自身的 Actuator
        changeOrigin: true,
      },
      '/instances': {
        target: opsTarget, // 运维控制台实例端点
        changeOrigin: true,
      },
      '/applications': {
        target: opsTarget, // 运维控制台应用端点
        changeOrigin: true,
      },
    },
  },
  }
})
