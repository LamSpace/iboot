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

package com.iboot.ai.infrastructure.security;

import java.util.Set;

/**
 * 无需认证的路径白名单。
 *
 * <p>AI 服务只暴露健康检查与指标端点；所有业务接口都必须携带有效登录态。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class SecurityWhitelistConfig {

    /**
     * 白名单路径。
     */
    private static final Set<String> WHITELIST = Set.of(
            "/actuator/health",
            "/actuator/prometheus");

    private SecurityWhitelistConfig() {
    }

    /**
     * 判断路径是否无需认证。
     *
     * @param path 请求路径
     * @return 是否白名单
     */
    public static boolean isWhitelisted(String path) {
        return WHITELIST.contains(path);
    }
}
