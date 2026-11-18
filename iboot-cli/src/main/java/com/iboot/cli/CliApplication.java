/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.cli;

import com.iboot.cli.auth.CredentialStore;
import com.iboot.cli.auth.LoopbackServer;
import com.iboot.cli.auth.OidcClient;
import com.iboot.cli.auth.Pkce;
import com.iboot.cli.auth.TokenService;
import com.iboot.cli.auth.TokenSet;
import com.iboot.cli.config.CliConfig;
import com.iboot.cli.config.ConfigStore;
import com.iboot.cli.config.Context;
import com.iboot.cli.http.ApiResponse;
import com.iboot.cli.http.GatewayClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * CLI 的应用服务。
 *
 * <p>组合 context 配置、凭据存储、OIDC 登录与网关请求；命令层只负责参数解析与输出，
 * 不直接读写文件或拼接 OAuth2 请求。所有需要业务接口的请求在 401 时尝试续期一次。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class CliApplication {

    private final ConfigStore configStore;

    private final CredentialStore credentialStore;

    private final OidcClient oidcClient;

    private final TokenService tokenService;

    /**
     * 是否已经读取过标准输入，保证多个命令路径只消费一次。
     */
    private final AtomicBoolean tokenStdinRead = new AtomicBoolean();

    /**
     * 从标准输入注入的令牌，优先级高于环境变量与本地凭据。
     */
    private volatile String tokenOverride;

    /**
     * 使用默认配置路径创建应用。
     */
    public CliApplication() {
        this(new ConfigStore(), new CredentialStore());
    }

    CliApplication(ConfigStore configStore, CredentialStore credentialStore) {
        this.configStore = configStore;
        this.credentialStore = credentialStore;
        this.oidcClient = new OidcClient();
        this.tokenService = new TokenService(credentialStore, oidcClient);
    }

    /**
     * 解析当前上下文。
     *
     * @param override 命令行指定的 context 名称，可为 {@code null}
     * @return 上下文
     */
    public Context context(String override) {
        CliConfig config = configStore.load();
        if (override == null || override.isBlank()) {
            return config.current();
        }
        return config.find(override).orElseThrow(() ->
                new CliException(ExitCode.USAGE, "context 不存在：" + override));
    }

    /**
     * 列出上下文。
     *
     * @return 上下文列表
     */
    public List<Context> contexts() {
        return configStore.contexts();
    }

    /**
     * 切换当前上下文。
     *
     * @param name 上下文名称
     * @return 切换后的上下文
     */
    public Context useContext(String name) {
        return configStore.use(name);
    }

    /**
     * 读取令牌；{@code IBOOT_TOKEN} 存在时优先用于 CI。
     *
     * @param contextName context 名称
     * @return 令牌；不存在时返回 {@code null}
     */
    public TokenSet tokens(String contextName) {
        if (tokenOverride != null && !tokenOverride.isBlank()) {
            return new TokenSet(tokenOverride, null, null, Long.MAX_VALUE, "Bearer");
        }
        String environmentToken = System.getenv("IBOOT_TOKEN");
        if (environmentToken != null && !environmentToken.isBlank()) {
            return new TokenSet(environmentToken, null, null, Long.MAX_VALUE, "Bearer");
        }
        return credentialStore.load(contextName);
    }

    /**
     * 判断当前调用是否已从标准输入注入令牌。
     *
     * @return 存在标准输入令牌时返回 {@code true}
     */
    public boolean hasTokenOverride() {
        return tokenOverride != null && !tokenOverride.isBlank();
    }

    /**
     * 从标准输入读取访问令牌，仅第一次调用生效。
     *
     * <p>用于 CI 注入 PAT：令牌不进入命令行参数、不写入凭据文件，也不回显。
     *
     * @throws CliException 标准输入为空或读取失败时抛出
     */
    public void useTokenFromStdin() {
        if (!tokenStdinRead.compareAndSet(false, true)) {
            return;
        }
        try {
            String value = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).trim();
            if (value.isEmpty()) {
                throw new CliException(ExitCode.USAGE, "标准输入没有令牌");
            }
            this.tokenOverride = value;
        } catch (IOException e) {
            throw new CliException(ExitCode.USAGE, "读取标准输入失败", e);
        }
    }

    /**
     * 读取令牌，未登录时抛出认证异常。
     *
     * @param contextName context 名称
     * @return 令牌
     */
    public TokenSet requireTokens(String contextName) {
        TokenSet tokens = tokens(contextName);
        if (tokens == null || tokens.accessToken() == null || tokens.accessToken().isBlank()) {
            throw new CliException(ExitCode.AUTH, "未登录，请先执行 iboot login");
        }
        return tokens;
    }

    /**
     * 发送业务请求；401 时续期一次并重放。
     *
     * @param context 当前上下文
     * @param method HTTP 方法
     * @param path 接口路径
     * @param body 请求体，可为 {@code null}
     * @param query 查询参数
     * @param raw 是否跳过解包
     * @return 已解包响应
     */
    public ApiResponse request(Context context, String method, String path, String body,
                               Map<String, String> query, boolean raw) {
        GatewayClient client = gateway(context);
        try {
            return client.request(method, path, body, query, raw);
        } catch (CliException e) {
            if (e.exitCode() != ExitCode.AUTH) {
                throw e;
            }
            TokenSet stale = requireTokens(context.name());
            tokenService.refresh(context, stale);
            return client.request(method, path, body, query, raw);
        }
    }

    /**
     * 执行浏览器授权码登录。
     *
     * @param context 当前上下文
     * @param openBrowser 是否自动打开浏览器
     * @param timeout 等待回调的超时时间
     * @return 登录后的令牌
     */
    public TokenSet login(Context context, boolean openBrowser, Duration timeout) {
        OidcClient.Discovery discovery = tokenService.discovery(context);
        Pkce pkce = Pkce.generate();
        int port = loopbackPort(context);
        try (LoopbackServer server = new LoopbackServer(port, pkce.state())) {
            String url = authorizeUrl(context, discovery, pkce);
            if (openBrowser) {
                com.iboot.cli.auth.BrowserLauncher.open(url);
            } else {
                System.out.println("请在浏览器打开以下地址完成登录：");
                System.out.println(url);
                System.out.flush();
            }
            String code = server.await(timeout);
            TokenSet tokens = oidcClient.exchange(discovery, code, pkce.verifier(),
                    context.redirectUri(), context.clientId());
            credentialStore.save(context.name(), tokens);
            return tokens;
        } catch (IOException e) {
            throw new CliException(ExitCode.USAGE,
                    "无法监听 127.0.0.1:" + port + "，端口可能被占用", e);
        }
    }

    /**
     * 登出：撤销服务端会话并删除本地凭据。
     *
     * @param context 当前上下文
     * @return 服务端会话是否已成功撤销
     */
    public boolean logout(Context context) {
        TokenSet current = tokens(context.name());
        boolean revoked = true;
        if (current != null && current.accessToken() != null) {
            try {
                gateway(context).request("POST", "/api/auth/logout", null, Map.of(), false);
            } catch (CliException e) {
                if (e.exitCode() != ExitCode.AUTH) {
                    revoked = false;
                }
            }
        }
        credentialStore.delete(context.name());
        return revoked;
    }

    /**
     * 创建网关客户端。
     *
     * @param context 当前上下文
     * @return 网关客户端
     */
    public GatewayClient gateway(Context context) {
        return new GatewayClient(context,
                () -> requireTokens(context.name()).accessToken());
    }

    /**
     * 获取令牌服务。
     *
     * @return 令牌服务
     */
    public TokenService tokenService() {
        return tokenService;
    }

    /**
     * 获取凭据存储。
     *
     * @return 凭据存储
     */
    public CredentialStore credentialStore() {
        return credentialStore;
    }

    /**
     * 获取配置存储。
     *
     * @return 配置存储
     */
    public ConfigStore configStore() {
        return configStore;
    }

    private int loopbackPort(Context context) {
        try {
            URI uri = URI.create(context.redirectUri());
            if (uri.getPort() <= 0) {
                throw new CliException(ExitCode.USAGE,
                        "loopback 回调地址必须包含端口：" + context.redirectUri());
            }
            return uri.getPort();
        } catch (IllegalArgumentException e) {
            throw new CliException(ExitCode.USAGE,
                    "loopback 回调地址非法：" + context.redirectUri(), e);
        }
    }

    private String authorizeUrl(Context context, OidcClient.Discovery discovery, Pkce pkce) {
        Map<String, String> params = Map.of(
                "response_type", "code",
                "client_id", context.clientId(),
                "scope", "openid profile api",
                "state", pkce.state(),
                "redirect_uri", context.redirectUri(),
                "code_challenge", pkce.challenge(),
                "code_challenge_method", "S256");
        String query = params.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return discovery.authorizationEndpoint() + "?" + query;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
