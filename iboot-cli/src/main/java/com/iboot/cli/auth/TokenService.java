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

package com.iboot.cli.auth;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.config.Context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌续期与发现文档缓存。
 *
 * <p>续期在凭据文件锁内完成：锁内重新读取最新令牌，若已被其他进程轮换则直接使用新值，
 * 否则用 refresh token 换取新令牌并原子写回。这样多个 CLI 进程不会因轮换互相作废。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class TokenService {

    private final CredentialStore credentialStore;

    private final OidcClient oidcClient;

    private final Map<String, OidcClient.Discovery> discoveries = new ConcurrentHashMap<>();

    /**
     * 创建令牌服务。
     *
     * @param credentialStore 凭据存储
     * @param oidcClient OIDC 客户端
     */
    public TokenService(CredentialStore credentialStore, OidcClient oidcClient) {
        this.credentialStore = credentialStore;
        this.oidcClient = oidcClient;
    }

    /**
     * 获取发现文档，按签发者缓存。
     *
     * @param context 当前上下文
     * @return 发现文档端点
     */
    public OidcClient.Discovery discovery(Context context) {
        return discoveries.computeIfAbsent(context.issuer(), ignored -> oidcClient.discover(context));
    }

    /**
     * 续期访问令牌。
     *
     * @param context 当前上下文
     * @param stale 触发续期的旧令牌
     * @return 最新令牌
     * @throws CliException 无刷新令牌或刷新失败时抛出
     */
    public TokenSet refresh(Context context, TokenSet stale) {
        return credentialStore.withLock(() -> {
            TokenSet latest = credentialStore.load(context.name());
            if (latest != null && stale != null
                    && latest.accessToken() != null
                    && !latest.accessToken().equals(stale.accessToken())) {
                return latest;
            }
            TokenSet current = latest != null ? latest : stale;
            if (current == null || !current.hasRefreshToken()) {
                throw new CliException(ExitCode.AUTH, "登录已失效，请重新执行 iboot login");
            }
            TokenSet refreshed = oidcClient.refresh(discovery(context), current.refreshToken(),
                    context.clientId());
            credentialStore.save(context.name(), refreshed);
            return refreshed;
        });
    }
}
