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

# 使用 OpenJDK 25 作为基础镜像
FROM openjdk:25-jdk-slim

# 设置工作目录
WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制 run-java.sh 脚本
COPY script/run-java.sh /app/run-java.sh
RUN chmod +x /app/run-java.sh

# 复制 JAR 文件
COPY target/iboot-admin-1.0.0.jar /app/app.jar

# 暴露端口
EXPOSE 8080

# 环境变量配置 (基于容器资源自动优化 JVM)
# 最大堆内存比例 (默认 50%，容器内存小于 300MB 时为 25%)
ENV JAVA_MAX_MEM_RATIO=50
# 初始堆内存比例 (默认不设置，如需设置取消注释)
# ENV JAVA_INIT_MEM_RATIO=25
# 启用 GC 诊断 (可选)
# ENV JAVA_DIAGNOSTICS=true
# 启用远程调试 (可选)
# ENV JAVA_DEBUG=true
# ENV JAVA_DEBUG_PORT=5005
# 应用名称 (用于进程识别)
ENV JAVA_APP_JAR=/app/app.jar
ENV JAVA_APP_NAME=iboot-admin

# 使用 run-java.sh 启动应用 (自动根据容器资源优化 JVM 参数)
ENTRYPOINT ["/app/run-java.sh"]
