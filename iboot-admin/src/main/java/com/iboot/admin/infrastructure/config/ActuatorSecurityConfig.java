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

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Actuator 端点安全配置。
 *
 * <p>Actuator 的访问控制由 {@link SecurityWhitelistProperties} 和 {@code SecurityConfig}
 * 统一处理：{@code /actuator/**} 在白名单中允许匿名访问，便于 Spring Boot Admin
 * 注册和采集数据。生产环境应通过网络层（防火墙或 Nginx）限制外部访问。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see SecurityWhitelistProperties
 */
@Configuration
@Order(1)
public class ActuatorSecurityConfig {

    // Actuator 端点访问控制由 SecurityWhitelistProperties 和 SecurityConfig 统一处理
    // 不再定义独立的 SecurityFilterChain，避免与主安全链冲突导致 StackOverflowError

}
