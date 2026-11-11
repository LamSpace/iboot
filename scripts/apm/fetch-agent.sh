#!/usr/bin/env bash
# 下载并解压 SkyWalking Java Agent 9.8.0 到 docker/skywalking/agent/。
#
# 9.8.0 是首个修复 Spring Framework 7 / Boot 4 入口 span 缺失问题的版本，
# 不要用更早的 9.x 替代。Agent 不是 Docker 镜像，解压目录已被 .gitignore 忽略。
#
# 9.8.0 把 Spring 6/7 服务端插件放在 optional-plugins，默认不加载；不装配它们
# 只能看到导出器自身的出站噪声，看不到任何业务入口链路。这里按项目技术栈启用：
#   - apm-springmvc-annotation-6.x      Spring MVC 服务（Boot 4 / Spring 7）
#   - apm-spring-webflux-6.x            WebFlux 服务
#   - apm-spring-cloud-gateway-4.x      网关（SCG 5.x 走 4.x 插件）
# 同时停用 apm-lettuce-5.x：当前 Lettuce/Reactor 已移除 Mono#subscriberContext，
# 插件 witness 不成立却仍拦截 TracedCommand，产生大量 NPE 噪声。
#
# 幂等：Agent 已解压时跳过下载，但仍会检查并修正插件装配。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VERSION="9.8.0"
DEST="$ROOT/docker/skywalking/agent"
TGZ="apache-skywalking-java-agent-${VERSION}.tgz"
# 优先国内 Apache 镜像；archive.apache.org 作为最后兜底（实测只有 ~10KB/s）。
MIRRORS=(
    "https://mirrors.ustc.edu.cn/apache/skywalking/java-agent/${VERSION}"
    "https://mirrors.aliyun.com/apache/skywalking/java-agent/${VERSION}"
    "https://mirrors.huaweicloud.com/apache/skywalking/java-agent/${VERSION}"
    "https://dlcdn.apache.org/skywalking/java-agent/${VERSION}"
    "https://archive.apache.org/dist/skywalking/java-agent/${VERSION}"
)

configure_plugins() {
    local plugins="$DEST/plugins"
    local optional="$DEST/optional-plugins"
    local expired="$DEST/expired-plugins"
    local jar

    for jar in \
        "apm-springmvc-annotation-6.x-plugin-${VERSION}.jar" \
        "apm-spring-webflux-6.x-plugin-${VERSION}.jar" \
        "apm-spring-cloud-gateway-4.x-plugin-${VERSION}.jar"; do
        if [ -f "$optional/$jar" ]; then
            mv "$optional/$jar" "$plugins/$jar"
            echo "  启用插件：$jar"
        fi
    done

    for jar in "$plugins"/apm-lettuce-*.jar; do
        [ -e "$jar" ] || continue
        case "$(basename "$jar")" in
            apm-lettuce-common-*) continue ;;
        esac
        mv "$jar" "$expired/"
        echo "  停用插件：$(basename "$jar")（与当前 Lettuce/Reactor 不兼容）"
    done
}

if [ -f "$DEST/skywalking-agent.jar" ]; then
    echo "SkyWalking Agent 已存在：$DEST"
    configure_plugins
    exit 0
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

echo "下载 $TGZ ..."
BASE_URL=""
for mirror in "${MIRRORS[@]}"; do
    echo "  尝试 $mirror"
    if curl -fSL --retry 2 --connect-timeout 10 -o "$TMP/$TGZ" "$mirror/$TGZ"; then
        BASE_URL="$mirror"
        break
    fi
done
[ -n "$BASE_URL" ] || {
    echo '所有镜像均下载失败' >&2
    exit 1
}

if curl -fSL --retry 2 --connect-timeout 10 -o "$TMP/$TGZ.sha512" "$BASE_URL/$TGZ.sha512"; then
    ( cd "$TMP" && sha512sum -c "$TGZ.sha512" )
else
    echo '[WARN] 未获取到 SHA-512 校验文件，跳过完整性校验' >&2
fi

mkdir -p "$TMP/extract"
tar -xzf "$TMP/$TGZ" -C "$TMP/extract"
[ -d "$TMP/extract/skywalking-agent" ] || {
    echo '压缩包结构不符合预期：缺少 skywalking-agent/ 目录' >&2
    exit 1
}

mkdir -p "$(dirname "$DEST")"
rm -rf "$DEST"
mv "$TMP/extract/skywalking-agent" "$DEST"
[ -f "$DEST/skywalking-agent.jar" ] || {
    echo '解压后缺少 skywalking-agent.jar' >&2
    exit 1
}

configure_plugins
echo "SkyWalking Agent ${VERSION} 已就绪：$DEST"
