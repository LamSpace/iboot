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

package com.iboot.ai.infrastructure.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Neo4j 驱动配置。
 *
 * <p>仅在 {@code iboot.ai.graph.enabled=true} 时创建驱动，关闭时应用不建立任何图数据库连接，
 * 容器关闭时由 {@code close()} 释放连接池。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class Neo4jConfig {

    /**
     * 创建图数据库驱动。
     *
     * @param properties 知识图谱配置
     * @return 驱动
     */
    @Bean(destroyMethod = "close")
    public Driver graphDriver(GraphProperties properties) {
        return GraphDatabase.driver(properties.getUri(),
                AuthTokens.basic(properties.getUsername(), properties.getPassword()));
    }
}
