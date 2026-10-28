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

import com.iboot.ai.domain.graph.DeptNode;
import com.iboot.ai.domain.graph.GraphSnapshot;
import com.iboot.ai.domain.graph.GraphStatus;
import com.iboot.ai.domain.graph.GraphSyncSummary;
import com.iboot.ai.domain.graph.MenuNode;
import com.iboot.ai.domain.graph.RoleNode;
import com.iboot.ai.domain.graph.UserNode;
import com.iboot.ai.infrastructure.config.GraphProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
 * {@link Neo4jGraphRepository} 写入路径的集成测试。
 *
 * <p>覆盖快照写入的节点与关系数量、同快照重复写入的幂等性，以及快照中移除实体后的清理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class Neo4jGraphRepositoryTest {

    private static final int BOLT_PORT = 7687;

    @Container
    static final GenericContainer<?> NEO4J = new GenericContainer<>("neo4j:5.26.29-community")
            .withExposedPorts(BOLT_PORT)
            .withEnv("NEO4J_AUTH", "neo4j/test-password")
            .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private static Driver driver;

    private Neo4jGraphRepository repository;

    @BeforeAll
    static void setUpDriver() {
        driver = GraphDatabase.driver("bolt://" + NEO4J.getHost() + ":" + NEO4J.getMappedPort(BOLT_PORT),
                AuthTokens.basic("neo4j", "test-password"));
    }

    @AfterAll
    static void closeDriver() {
        driver.close();
    }

    @BeforeEach
    void setUp() {
        new GraphSchemaInitializer(driver).initialize();
        try (Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
        GraphProperties properties = new GraphProperties();
        repository = new Neo4jGraphRepository(driver, properties);
    }

    @Test
    void writesSnapshotAndIsIdempotent() {
        GraphSyncSummary first = repository.replace(snapshotV1("sync-1"));
        assertThat(first.departments()).isEqualTo(3);
        assertThat(first.users()).isEqualTo(3);
        assertThat(first.roles()).isEqualTo(2);
        assertThat(first.menus()).isEqualTo(2);
        assertThat(first.relations()).isEqualTo(12);
        assertThat(first.removed()).isZero();
        assertThat(countNodes("Dept")).isEqualTo(3);
        assertThat(countNodes("User")).isEqualTo(3);
        assertThat(countEdges("BELONGS_TO")).isEqualTo(3);

        GraphSyncSummary second = repository.replace(snapshotV1("sync-1"));
        assertThat(second.removed()).isZero();
        assertThat(countNodes("User")).isEqualTo(3);
        assertThat(countEdges("GRANTS")).isEqualTo(3);
    }

    @Test
    void removesEntitiesMissingFromNewSnapshot() {
        repository.replace(snapshotV1("sync-1"));

        GraphSyncSummary summary = repository.replace(snapshotV2("sync-2"));

        assertThat(summary.removed()).isEqualTo(3);
        assertThat(countNodes("Dept")).isEqualTo(3);
        assertThat(countNodes("User")).isEqualTo(2);
        assertThat(countNodes("Role")).isEqualTo(1);
        assertThat(countNodes("Menu")).isEqualTo(1);
        assertThat(countEdges("PARENT_OF")).isEqualTo(2);
        assertThat(countEdges("BELONGS_TO")).isEqualTo(2);
        assertThat(countEdges("HAS_ROLE")).isEqualTo(2);
        assertThat(countEdges("GRANTS")).isEqualTo(1);
        assertThat(countEdges("SCOPE_DEPT")).isZero();
    }

    @Test
    void reportsStatusAfterSync() {
        assertThat(repository.loadStatus()).isEqualTo(GraphStatus.empty());

        repository.replace(snapshotV1("sync-1"));

        GraphStatus status = repository.loadStatus();
        assertThat(status.synced()).isTrue();
        assertThat(status.syncedAt()).isNotNull();
        assertThat(status.departments()).isEqualTo(3);
        assertThat(status.users()).isEqualTo(3);
        assertThat(status.relations()).isEqualTo(12);
    }

    private long countNodes(String label) {
        try (Session session = driver.session()) {
            return session.run("MATCH (n:" + label + ") RETURN count(n) AS total")
                    .single().get("total").asLong();
        }
    }

    private long countEdges(String type) {
        try (Session session = driver.session()) {
            return session.run("MATCH ()-[r:" + type + "]->() RETURN count(r) AS total")
                    .single().get("total").asLong();
        }
    }

    private GraphSnapshot snapshotV1(String syncId) {
        List<DeptNode> departments = List.of(
                new DeptNode(1L, "总经办", "HQ", null, 1),
                new DeptNode(2L, "研发部", "RD", 1L, 1),
                new DeptNode(3L, "市场部", "MKT", 1L, 1));
        List<UserNode> users = List.of(
                new UserNode(10L, "alice", "爱丽丝", 2L, 1, List.of(5L)),
                new UserNode(11L, "bob", "鲍勃", 2L, 1, List.of(5L, 6L)),
                new UserNode(12L, "carol", "卡罗尔", 3L, 1, List.of()));
        List<RoleNode> roles = List.of(
                new RoleNode(5L, "admin", "管理员", 1, 1, List.of(100L), List.of()),
                new RoleNode(6L, "viewer", "只读", 4, 1, List.of(100L, 101L), List.of(2L)));
        List<MenuNode> menus = List.of(
                new MenuNode(100L, "用户管理", "C", "user:list", null, 1),
                new MenuNode(101L, "角色管理", "C", "role:list", null, 1));
        return new GraphSnapshot(syncId, departments, users, roles, menus);
    }

    private GraphSnapshot snapshotV2(String syncId) {
        List<DeptNode> departments = List.of(
                new DeptNode(1L, "总经办", "HQ", null, 1),
                new DeptNode(2L, "研发部", "RD", 1L, 1),
                new DeptNode(3L, "市场部", "MKT", 1L, 1));
        List<UserNode> users = List.of(
                new UserNode(10L, "alice", "爱丽丝", 2L, 1, List.of(5L)),
                new UserNode(11L, "bob", "鲍勃", 2L, 1, List.of(5L)));
        List<RoleNode> roles = List.of(
                new RoleNode(5L, "admin", "管理员", 1, 1, List.of(100L), List.of()));
        List<MenuNode> menus = List.of(
                new MenuNode(100L, "用户管理", "C", "user:list", null, 1));
        return new GraphSnapshot(syncId, departments, users, roles, menus);
    }
}
