#
#    Copyright 2023 the original author, Lam Tong
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#          http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#


# 构建阶段：使用国内 npm 镜像，不依赖宿主机 cnpm。
# 上下文中的 .env 会被 Vite 读取，因此必须保留（见 .dockerignore）。
FROM node:22.23.5-bookworm-slim AS build

WORKDIR /app

COPY package.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm install --registry=https://registry.npmmirror.com

COPY . .
RUN npm run build

# 生产运行阶段
FROM nginx:1.30.6-alpine3.24 AS production

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制构建产物到 nginx html 目录
COPY --from=build /app/dist/ /usr/share/nginx/html/

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 暴露端口
EXPOSE 80

# 启动 nginx
CMD ["nginx", "-g", "daemon off;"]
