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

package com.iboot.workflow.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 请假额度客户端配置。
 *
 * <p>绑定 {@code iboot.leave-quota.*}：系统服务地址按注册中心服务名寻址，内部令牌
 * 与系统服务共享；令牌缺失时调用失败关闭。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "iboot.leave-quota")
public class WorkflowLeaveQuotaProperties {

    /**
     * 系统服务地址。
     */
    private String baseUrl = "http://iboot-system";

    /**
     * 内部共享密钥。
     */
    private String internalToken = "";

    /**
     * 返回系统服务地址。
     *
     * @return 系统服务地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置系统服务地址。
     *
     * @param baseUrl 系统服务地址
     */
    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * 返回内部共享密钥。
     *
     * @return 内部共享密钥
     */
    public String getInternalToken() {
        return internalToken;
    }

    /**
     * 设置内部共享密钥。
     *
     * @param internalToken 内部共享密钥
     */
    public void setInternalToken(final String internalToken) {
        this.internalToken = internalToken;
    }
}
