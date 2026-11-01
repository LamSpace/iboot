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

package com.iboot.system.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 时间源配置。
 *
 * <p>提供可替换的 {@link Clock}，让备份、恢复演练等按时间推进的逻辑可在测试中控制。
 * 通知投递已迁出本服务，其时钟由 {@code iboot-notify} 自行提供。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class NotificationConfig {

    /**
     * 系统默认时钟。
     *
     * @return 系统时区时钟
     */
    @Bean
    public Clock notificationClock() {
        return Clock.systemDefaultZone();
    }
}
