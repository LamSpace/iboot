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
 * 审批服务 RocketMQ 客户端配置。
 *
 * <p>绑定 {@code iboot.rocketmq.*}，供 outbox 中继创建生产者。本地明文 Proxy 必须
 * 关闭 SSL；容器内应用应通过 {@code IBOOT_ROCKETMQ_ENDPOINTS} 覆盖端点。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "iboot.rocketmq")
public class RocketMqProperties {

    /**
     * Proxy 的 gRPC 端点，宿主机默认为映射后的 18081。
     */
    private String endpoints = "localhost:18081";

    /**
     * 是否启用 TLS；本地明文 Proxy 必须为 {@code false}。
     */
    private boolean sslEnabled = false;

    /**
     * 通知事件主题。
     */
    private String topic = "iboot-notification";

    /**
     * Proxy 的 gRPC 端点。
     *
     * @return 端点地址
     */
    public String getEndpoints() {
        return endpoints;
    }

    /**
     * 设置 Proxy 的 gRPC 端点。
     *
     * @param endpoints 端点地址
     */
    public void setEndpoints(final String endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * 是否启用 TLS。
     *
     * @return 启用 TLS 返回 {@code true}
     */
    public boolean isSslEnabled() {
        return sslEnabled;
    }

    /**
     * 设置是否启用 TLS。
     *
     * @param sslEnabled 是否启用
     */
    public void setSslEnabled(final boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    /**
     * 通知事件主题。
     *
     * @return 主题名
     */
    public String getTopic() {
        return topic;
    }

    /**
     * 设置通知事件主题。
     *
     * @param topic 主题名
     */
    public void setTopic(final String topic) {
        this.topic = topic;
    }
}
