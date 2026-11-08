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

import java.time.Duration;

/**
 * 通知 outbox 中继配置。
 *
 * <p>绑定 {@code iboot.workflow.outbox.*}。{@code enabled=false} 用于滚动升级或回滚时
 * 暂停中继并排空事件；其余参数控制批量、轮询间隔、SENDING 租约与重试上限。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "iboot.workflow.outbox")
public class OutboxProperties {

    /**
     * 是否启用后台中继。
     */
    private boolean enabled = true;

    /**
     * 每轮抢占的最大事件数。
     */
    private int batchSize = 20;

    /**
     * 两轮之间的固定间隔。
     */
    private Duration pollInterval = Duration.ofSeconds(1);

    /**
     * SENDING 记录的最大租约；超时视为中继崩溃并重置为待发。
     */
    private Duration lease = Duration.ofMinutes(5);

    /**
     * 单条事件的最大投递尝试次数，达到后标记为死信。
     */
    private int maxAttempts = 10;

    /**
     * 首次重试的退避时间，之后逐次翻倍。
     */
    private Duration initialBackoff = Duration.ofSeconds(5);

    /**
     * SENT 记录的保留期，过期由清理任务删除。
     */
    private Duration retention = Duration.ofDays(7);

    /**
     * 是否启用后台中继。
     *
     * @return 启用返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用后台中继。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 每轮抢占的最大事件数。
     *
     * @return 批量大小
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 设置每轮抢占的最大事件数。
     *
     * @param batchSize 批量大小
     */
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * 两轮之间的固定间隔。
     *
     * @return 轮询间隔
     */
    public Duration getPollInterval() {
        return pollInterval;
    }

    /**
     * 设置两轮之间的固定间隔。
     *
     * @param pollInterval 轮询间隔
     */
    public void setPollInterval(final Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    /**
     * SENDING 记录的最大租约。
     *
     * @return 租约时长
     */
    public Duration getLease() {
        return lease;
    }

    /**
     * 设置 SENDING 记录的最大租约。
     *
     * @param lease 租约时长
     */
    public void setLease(final Duration lease) {
        this.lease = lease;
    }

    /**
     * 单条事件的最大投递尝试次数。
     *
     * @return 最大尝试次数
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 设置单条事件的最大投递尝试次数。
     *
     * @param maxAttempts 最大尝试次数
     */
    public void setMaxAttempts(final int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * 首次重试的退避时间。
     *
     * @return 初始退避
     */
    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    /**
     * 设置首次重试的退避时间。
     *
     * @param initialBackoff 初始退避
     */
    public void setInitialBackoff(final Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    /**
     * SENT 记录的保留期。
     *
     * @return 保留时长
     */
    public Duration getRetention() {
        return retention;
    }

    /**
     * 设置 SENT 记录的保留期。
     *
     * @param retention 保留时长
     */
    public void setRetention(final Duration retention) {
        this.retention = retention;
    }
}
