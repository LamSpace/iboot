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

/**
 * Elasticsearch 配置类
 * <p>
 * 使用 Spring Boot 自动配置的 Elasticsearch 客户端
 * 配置项位于 application.yml 的 spring.elasticsearch 节点
 * </p>
 *
 * @author iBoot
 */
@Configuration
public class ElasticsearchConfig {
    // 无需自定义 Bean，Spring Boot 已自动配置 ElasticsearchClient 和 ElasticsearchTemplate
    // 超时配置通过 application.yml 中的 spring.elasticsearch 属性控制
}
