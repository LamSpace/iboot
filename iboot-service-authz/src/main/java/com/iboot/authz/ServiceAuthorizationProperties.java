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

import java.time.Duration;
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
     * OPA 决策地址；为空时使用本地规则。
     */
    private String opaUrl = "";

    /**
     * 单次 OPA 决策超时。
     */
    private Duration opaTimeout = Duration.ofMillis(500);

    /**
     * OPA 传输错误重试次数；{@code 0} 表示不重试。
     */
    private int opaRetryAttempts = 1;

    /**
     * 明确 OPA 决策的本地缓存 TTL；默认禁用，避免 OPA 全部不可用时仍从缓存放行。
     */
    private Duration opaCacheTtl = Duration.ZERO;

    /**
     * 获取 OPA 决策地址。
     *
     * @return OPA 基础地址；为空表示使用本地规则
     */
    public String getOpaUrl() {
        return opaUrl;
    }

    /**
     * 设置 OPA 决策地址。
     *
     * @param opaUrl OPA 基础地址
     */
    public void setOpaUrl(final String opaUrl) {
        this.opaUrl = opaUrl == null ? "" : opaUrl;
    }

    /**
     * 获取 OPA 决策超时。
     *
     * @return 超时时间
     */
    public Duration getOpaTimeout() {
        return opaTimeout;
    }

    /**
     * 设置 OPA 决策超时。
     *
     * @param opaTimeout 超时时间
     */
    public void setOpaTimeout(final Duration opaTimeout) {
        this.opaTimeout = opaTimeout == null ? Duration.ofMillis(500) : opaTimeout;
    }

    /**
     * 获取 OPA 传输错误重试次数。
     *
     * @return 重试次数；{@code 0} 表示不重试
     */
    public int getOpaRetryAttempts() {
        return opaRetryAttempts;
    }

    /**
     * 设置 OPA 传输错误重试次数。
     *
     * @param opaRetryAttempts 重试次数；负数按 {@code 0} 处理
     */
    public void setOpaRetryAttempts(final int opaRetryAttempts) {
        this.opaRetryAttempts = Math.max(0, opaRetryAttempts);
    }

    /**
     * 获取 OPA 决策缓存 TTL。
     *
     * @return 缓存 TTL；{@code 0} 或负值表示禁用缓存
     */
    public Duration getOpaCacheTtl() {
        return opaCacheTtl;
    }

    /**
     * 设置 OPA 决策缓存 TTL。
     *
     * @param opaCacheTtl 缓存 TTL；{@code null} 表示禁用缓存
     */
    public void setOpaCacheTtl(final Duration opaCacheTtl) {
        this.opaCacheTtl = opaCacheTtl == null ? Duration.ZERO : opaCacheTtl;
    }

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
