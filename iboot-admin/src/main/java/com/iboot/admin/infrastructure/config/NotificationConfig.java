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

package com.iboot.admin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 通知基础设施配置。
 *
 * <p>提供可替换的 {@link Clock}，让投递退避与测试时间可控。
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
