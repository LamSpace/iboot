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

package com.iboot.auth.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 个人访问令牌策略配置。
 *
 * <p>绑定 {@code iboot.auth.pat.*}，约束签发有效期与每用户活动令牌数量。默认值面向
 * 无浏览器 CI：有效期足以覆盖常见的定时任务周期，上限用于限制长期凭据的规模。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "iboot.auth.pat")
public class PersonalAccessTokenProperties {

    /**
     * 未显式指定有效期时的默认天数。
     */
    private long defaultDays = 30L;

    /**
     * 允许签发的最大有效期天数。
     */
    private long maxDays = 90L;

    /**
     * 每用户允许同时存在的活动令牌数量。
     */
    private int maxPerUser = 10;

    /**
     * 获取默认有效期。
     *
     * @return 默认天数
     */
    public long getDefaultDays() {
        return defaultDays;
    }

    /**
     * 设置默认有效期。
     *
     * @param defaultDays 默认天数
     */
    public void setDefaultDays(long defaultDays) {
        this.defaultDays = defaultDays;
    }

    /**
     * 获取最大有效期。
     *
     * @return 最大天数
     */
    public long getMaxDays() {
        return maxDays;
    }

    /**
     * 设置最大有效期。
     *
     * @param maxDays 最大天数
     */
    public void setMaxDays(long maxDays) {
        this.maxDays = maxDays;
    }

    /**
     * 获取每用户活动令牌上限。
     *
     * @return 数量上限
     */
    public int getMaxPerUser() {
        return maxPerUser;
    }

    /**
     * 设置每用户活动令牌上限。
     *
     * @param maxPerUser 数量上限
     */
    public void setMaxPerUser(int maxPerUser) {
        this.maxPerUser = maxPerUser;
    }
}
