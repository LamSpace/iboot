# iBoot Portal - 前端管理系统

iBoot Portal是基于Vue 3的前端管理系统，提供现代化的用户界面和丰富的交互体验。

## 技术栈

- **Vue 3**: 前端框架，使用Composition API
- **TypeScript**: 类型安全
- **Vite**: 构建工具，快速热更新
- **Element Plus**: UI组件库
- **Pinia**: 状态管理
- **Vue Router**: 路由管理
- **Axios**: HTTP客户端
- **Three.js**: 3D图形渲染
- **SCSS**: CSS预处理器

## 项目结构

```
src/
├── assets/               # 静态资源
├── components/           # 公共组件
├── views/                # 页面组件
│   ├── LoginView.vue     # 登录页面
│   ├── HomeView.vue      # 首页
│   └── DashboardView.vue # 仪表板页面
├── router/               # 路由配置
├── stores/               # Pinia状态管理
├── utils/                # 工具函数
├── styles/               # 全局样式
├── api/                  # API接口封装
└── main.ts               # 入口文件
```

## 核心功能

### 1. 用户认证
- 登录/登出功能
- JWT身份验证
- 自动登录状态保持
- 权限验证

### 2. 动态菜单
- 基于角色的动态菜单生成
- 多级菜单支持
- 图标个性化定制
- 面包屑导航

### 3. 3D可视化
- 酷炫的3D登录背景
- 首页3D视觉效果
- 流畅的动画体验

### 4. 响应式设计
- 适配不同屏幕尺寸
- 移动端友好
- 优雅的UI交互

## 安装和运行

### 环境要求
- Node.js >= 16.0.0
- npm 或 yarn

### 安装依赖
```bash
npm install
```

### 开发模式运行
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

### 预览生产版本
```bash
npm run preview
```

## 主要特性

### 1. 登录页面
- 3D粒子背景动画
- 深色渐变字体效果
- 回车键登录支持
- 表单验证

### 2. 仪表板页面
- 响应式布局
- 动态侧边栏菜单
- 嵌套路由支持
- 面包屑导航

### 3. 菜单系统
- 动态菜单加载
- 多级菜单支持
- 个性化图标
- 权限控制

### 4. 组件复用
- 可复用的UI组件
- 统一的样式规范
- 模块化的功能组件

## API集成

前端通过Axios与后端API通信，已封装常用请求方法：

```typescript
// 示例：获取用户信息
import { getUserInfo } from '@/api/user'

const userInfo = await getUserInfo()
```

## 状态管理

使用Pinia进行状态管理：

```typescript
// 示例：用户状态管理
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
userStore.setUser(userInfo)
```

## 样式规范

- 使用Element Plus组件库
- 统一的色彩方案
- 响应式断点规范
- CSS变量管理

## 自定义配置

### 环境变量
在`.env`文件中配置：

```
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=iBoot管理系统
```

### 构建配置
在`vite.config.ts`中进行构建配置：

```typescript
export default defineConfig({
  base: './',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  }
})
```

## 部署

### 静态文件部署
构建完成后，将`dist`目录下的文件部署到Web服务器即可。

### Nginx配置示例
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }
    
    # API代理
    location /api/ {
        proxy_pass http://backend-server:8080/;
    }
}
```

## 开发规范

- 组件命名使用PascalCase
- 使用TypeScript进行类型定义
- 遵循Vue 3最佳实践
- 使用Composition API
- 统一的错误处理机制

## 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 发起Pull Request

## 浏览器兼容性

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 87

## 许可证

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

See the [LICENSE](../LICENSE) file for additional licensing information.