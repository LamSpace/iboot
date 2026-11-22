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

package com.iboot.authz;

import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务间授权策略。
 *
 * <p>按“专属规则优先、公共规则其次、首个命中生效”的顺序求值；未命中任何规则时
 * 放行，由既有 JWT/白名单语义继续处理。调用方为空且命中规则时按拒绝处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ServiceAuthorizationPolicy {

    private final List<ServiceAuthorizationRule> rules;

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * 创建策略。
     *
     * @param properties 授权配置
     */
    public ServiceAuthorizationPolicy(final ServiceAuthorizationProperties properties) {
        List<ServiceAuthorizationRule> ordered = new ArrayList<>();
        ordered.addAll(properties.getExtraRules());
        ordered.addAll(properties.getRules());
        this.rules = List.copyOf(ordered);
    }

    /**
     * 判断请求是否应被拒绝。
     *
     * @param path   请求路径
     * @param caller 调用方 CN，可为 {@code null}
     * @return 命中规则且调用方不在白名单时返回 {@code true}
     */
    public boolean isDenied(final String path, final String caller) {
        for (ServiceAuthorizationRule rule : rules) {
            if (rule.matches(path, matcher)) {
                return !rule.allows(caller);
            }
        }
        return false;
    }
}
