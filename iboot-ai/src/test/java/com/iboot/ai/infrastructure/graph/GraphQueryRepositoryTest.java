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
import com.iboot.ai.domain.graph.DeptPath;
import com.iboot.ai.domain.graph.GraphSnapshot;
import com.iboot.ai.domain.graph.MenuNode;
import com.iboot.ai.domain.graph.PermissionRef;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.domain.graph.RoleNode;
import com.iboot.ai.domain.graph.UserNode;
import com.iboot.ai.domain.graph.UserRef;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 图查询模板的集成测试。
 *
 * <p>覆盖同部门同事（2 跳）、部门成员含子部门、用户权限、权限持有人（3 跳）与跨部门最短路径，
 * 以及数据权限范围过滤后的空结果。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class GraphQueryRepositoryTest {

    private static final int BOLT_PORT = 7687;

    private static final QueryScope UNRESTRICTED = new QueryScope(true, List.of());

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
        repository = new Neo4jGraphRepository(driver, new GraphProperties());
        repository.replace(snapshot());
    }

    @Test
    void findsColleaguesInSameDepartment() {
        List<UserRef> colleagues = repository.colleagues(10L, UNRESTRICTED, 50);

        assertThat(colleagues).extracting(UserRef::username).containsExactly("bob");
    }

    @Test
    void findsDepartmentMembersIncludingChildDepartments() {
        List<UserRef> members = repository.deptMembers(2L, UNRESTRICTED, 50);

        assertThat(members).extracting(UserRef::username)
                .containsExactlyInAnyOrder("alice", "bob", "dave");
    }

    @Test
    void findsUserPermissionsThroughRoles() {
        List<PermissionRef> permissions = repository.userPermissions(11L, UNRESTRICTED, false, 50);

        assertThat(permissions).extracting(PermissionRef::permission)
                .containsExactlyInAnyOrder("user:list", "user:list", "role:list");
        assertThat(permissions).extracting(PermissionRef::roleCode)
                .containsOnly("admin", "viewer");
    }

    @Test
    void findsHoldersOfPermission() {
        List<UserRef> holders = repository.permHolders("role:list", UNRESTRICTED, 50);

        assertThat(holders).extracting(UserRef::username).containsExactly("bob");
    }

    @Test
    void findsShortestDepartmentPath() {
        Optional<DeptPath> path = repository.deptPath(3L, 4L, UNRESTRICTED);

        assertThat(path).isPresent();
        assertThat(path.get().departmentNames()).containsExactly("市场部", "总经办", "研发部", "后端组");
        assertThat(path.get().hops()).isEqualTo(3);
    }

    @Test
    void hidesDepartmentsOutsideVisibleScope() {
        QueryScope scope = new QueryScope(false, List.of(3L));

        assertThat(repository.colleagues(10L, scope, 50)).isEmpty();
        assertThat(repository.deptMembers(2L, scope, 50)).isEmpty();
        assertThat(repository.permHolders("role:list", scope, 50)).isEmpty();
        assertThat(repository.deptPath(3L, 4L, scope)).isEmpty();
    }

    @Test
    void allowsOwnPermissionsOutsideScope() {
        QueryScope scope = new QueryScope(false, List.of(3L));

        assertThat(repository.userPermissions(10L, scope, true, 50)).isNotEmpty();
        assertThat(repository.userPermissions(10L, scope, false, 50)).isEmpty();
    }

    private GraphSnapshot snapshot() {
        List<DeptNode> departments = List.of(
                new DeptNode(1L, "总经办", "HQ", null, 1),
                new DeptNode(2L, "研发部", "RD", 1L, 1),
                new DeptNode(3L, "市场部", "MKT", 1L, 1),
                new DeptNode(4L, "后端组", "RD-BE", 2L, 1));
        List<UserNode> users = List.of(
                new UserNode(10L, "alice", "爱丽丝", 2L, 1, List.of(5L)),
                new UserNode(11L, "bob", "鲍勃", 2L, 1, List.of(5L, 6L)),
                new UserNode(12L, "carol", "卡罗尔", 3L, 1, List.of()),
                new UserNode(13L, "dave", "戴夫", 4L, 1, List.of(5L)));
        List<RoleNode> roles = List.of(
                new RoleNode(5L, "admin", "管理员", 1, 1, List.of(100L), List.of()),
                new RoleNode(6L, "viewer", "只读", 4, 1, List.of(100L, 101L), List.of()));
        List<MenuNode> menus = List.of(
                new MenuNode(100L, "用户管理", "C", "user:list", null, 1),
                new MenuNode(101L, "角色管理", "C", "role:list", null, 1));
        return new GraphSnapshot("sync-query", departments, users, roles, menus);
    }
}
