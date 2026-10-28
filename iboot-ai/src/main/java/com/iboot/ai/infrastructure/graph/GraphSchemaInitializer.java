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

package com.iboot.ai.infrastructure.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图约束初始化器。
 *
 * <p>启动时为四类节点幂等创建 {@code id} 唯一约束；重复执行不会产生重复约束。
 * 仅在 {@code iboot.ai.graph.enabled=true} 时装配。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class GraphSchemaInitializer implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(GraphSchemaInitializer.class);

    private static final List<String> STATEMENTS = List.of(
            "CREATE CONSTRAINT dept_id_unique IF NOT EXISTS FOR (n:Dept) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT user_id_unique IF NOT EXISTS FOR (n:User) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT role_id_unique IF NOT EXISTS FOR (n:Role) REQUIRE n.id IS UNIQUE",
            "CREATE CONSTRAINT menu_id_unique IF NOT EXISTS FOR (n:Menu) REQUIRE n.id IS UNIQUE");

    private final Driver driver;

    /**
     * 创建初始化器。
     *
     * @param driver 图数据库驱动
     */
    public GraphSchemaInitializer(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void afterPropertiesSet() {
        initialize();
    }

    /**
     * 幂等创建唯一约束。
     */
    public void initialize() {
        try (Session session = driver.session()) {
            for (String statement : STATEMENTS) {
                session.run(statement).consume();
            }
        }
        log.info("知识图谱唯一约束已就绪");
    }
}
