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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GraphSchemaInitializer} 的集成测试。
 *
 * <p>使用 Testcontainers Neo4j 验证四类节点的唯一约束可以重复初始化且不产生重复约束。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class GraphSchemaInitializerTest {

    private static final int BOLT_PORT = 7687;

    @Container
    static final GenericContainer<?> NEO4J = new GenericContainer<>("neo4j:5.26.29-community")
            .withExposedPorts(BOLT_PORT)
            .withEnv("NEO4J_AUTH", "neo4j/test-password")
            .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private static Driver driver;

    @BeforeAll
    static void setUpDriver() {
        driver = GraphDatabase.driver("bolt://" + NEO4J.getHost() + ":" + NEO4J.getMappedPort(BOLT_PORT),
                AuthTokens.basic("neo4j", "test-password"));
    }

    @AfterAll
    static void closeDriver() {
        driver.close();
    }

    @Test
    void createsUniqueConstraintsIdempotently() {
        GraphSchemaInitializer initializer = new GraphSchemaInitializer(driver);

        initializer.initialize();
        initializer.initialize();

        List<String> names = constraintNames();
        assertThat(names).contains("dept_id_unique", "user_id_unique", "role_id_unique", "menu_id_unique");
        assertThat(names.stream().filter(name -> name.endsWith("_id_unique")).count()).isEqualTo(4);
    }

    private List<String> constraintNames() {
        try (Session session = driver.session()) {
            return session.run("SHOW CONSTRAINTS YIELD name RETURN name").list(record -> record.get("name").asString());
        }
    }
}
