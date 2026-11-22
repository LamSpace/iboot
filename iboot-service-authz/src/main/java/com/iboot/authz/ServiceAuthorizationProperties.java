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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务间授权配置。
 *
 * <p>绑定 {@code iboot.security.service-authorization}。公共规则放在三份
 * {@code iboot-tls.yml} 的 {@code rules}；宿主服务在自己的 {@code application.yml}
 * 用 {@code extra-rules} 声明专属规则。匹配时 {@code extra-rules} 优先。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.security.service-authorization")
public class ServiceAuthorizationProperties {

    /**
     * 是否启用服务间授权；默认关闭以保护单元测试切片。
     */
    private boolean enabled;

    /**
     * 公共规则，通常来自 {@code iboot-tls.yml}。
     */
    private List<ServiceAuthorizationRule> rules = new ArrayList<>();

    /**
     * 宿主专属规则，优先于公共规则匹配。
     */
    private List<ServiceAuthorizationRule> extraRules = new ArrayList<>();

    /**
     * 是否启用。
     *
     * @return 启用返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取公共规则。
     *
     * @return 公共规则列表
     */
    public List<ServiceAuthorizationRule> getRules() {
        return rules;
    }

    /**
     * 设置公共规则。
     *
     * @param rules 公共规则列表
     */
    public void setRules(final List<ServiceAuthorizationRule> rules) {
        this.rules = rules == null ? new ArrayList<>() : rules;
    }

    /**
     * 获取宿主专属规则。
     *
     * @return 专属规则列表
     */
    public List<ServiceAuthorizationRule> getExtraRules() {
        return extraRules;
    }

    /**
     * 设置宿主专属规则。
     *
     * @param extraRules 专属规则列表
     */
    public void setExtraRules(final List<ServiceAuthorizationRule> extraRules) {
        this.extraRules = extraRules == null ? new ArrayList<>() : extraRules;
    }
}
