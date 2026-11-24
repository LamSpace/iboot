# 服务 SVID 取证 sidecar：复用 SPIRE Agent 镜像里的 CLI 与本地 nginx 基础镜像的 shell。
# 取回证书后把私钥权限放宽到 644，供同一卷挂载的应用容器（app 用户）读取。
FROM ghcr.io/spiffe/spire-agent:1.12.4 AS spire

FROM nginx:1.30.6-alpine3.24

COPY --from=spire /opt/spire/bin/spire-agent /opt/spire/bin/spire-agent
RUN chmod 755 /opt/spire/bin/spire-agent

ENTRYPOINT ["/bin/sh", "-c"]
