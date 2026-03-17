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
 * Actuator 安全配置
 * <p>
 * Actuator 端点的访问控制已由 {@link SecurityWhitelistConfig} 统一管理： - /actuator/** 路径在白名单中，允许匿名访问 - Spring Boot Admin 需要无障碍访问
 * Actuator 端点完成注册和数据采集 - 生产环境建议通过网络层（防火墙/Nginx）限制 Actuator 端点的外部访问
 *
 * @author iBoot
 */
@Configuration
@Order(1)
public class ActuatorSecurityConfig {

    // Actuator 端点访问控制由 SecurityWhitelistConfig 和 SecurityConfig 统一处理
    // 不再定义独立的 SecurityFilterChain，避免与主安全链冲突导致 StackOverflowError

}
