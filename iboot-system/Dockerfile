#
#    Copyright 2023 the original author, Lam Tong
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#

# 统一运行期镜像模板：差异仅限应用名、端口与 jar 文件名
FROM openjdk:25-jdk-slim

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# 通用启动脚本（九个模块内容一致）
COPY script/run-java.sh /app/run-java.sh
RUN chmod +x /app/run-java.sh

# 预构建 jar；构建请在宿主机执行 mvn package 或 scripts/build.sh
ARG JAR_FILE=target/iboot-system-1.0.0.jar
COPY ${JAR_FILE} /app/app.jar

# 非 root 运行，并准备可写日志目录
RUN groupadd -r app && useradd -r -g app app \
 && mkdir -p /app/logs && chown -R app:app /app
USER app

EXPOSE 8080

ENV JAVA_APP_JAR=/app/app.jar \
    JAVA_APP_NAME=iboot-system \
    JAVA_OPTIONS="--enable-native-access=ALL-UNNAMED"

ENTRYPOINT ["/app/run-java.sh"]
