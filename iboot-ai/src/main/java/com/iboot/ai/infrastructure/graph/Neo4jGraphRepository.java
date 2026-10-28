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
import com.iboot.ai.domain.graph.GraphStatus;
import com.iboot.ai.domain.graph.GraphSyncSummary;
import com.iboot.ai.domain.graph.MenuNode;
import com.iboot.ai.domain.graph.PermissionRef;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.domain.graph.RoleNode;
import com.iboot.ai.domain.graph.UserNode;
import com.iboot.ai.domain.graph.UserRef;
import com.iboot.ai.infrastructure.config.GraphProperties;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于官方驱动的图仓储。
 *
 * <p>负责把一次完整快照写入图：节点与关系统一 {@code MERGE} 并盖上 {@code syncId}，
 * 事务末尾清理旧快照遗留的节点与关系，因此重复执行幂等、失败时旧图保持完整。
 * 只在 {@code iboot.ai.graph.enabled=true} 时装配。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class Neo4jGraphRepository {

    private static final String MERGE_DEPTS = """
            UNWIND $rows AS row
            MERGE (n:Dept {id: row.id})
            SET n.name = row.name, n.code = row.code, n.parentId = row.parentId,
                n.status = row.status, n.syncId = $syncId
            """;

    private static final String MERGE_USERS = """
            UNWIND $rows AS row
            MERGE (n:User {id: row.id})
            SET n.username = row.username, n.nickname = row.nickname, n.deptId = row.deptId,
                n.status = row.status, n.syncId = $syncId
            """;

    private static final String MERGE_ROLES = """
            UNWIND $rows AS row
            MERGE (n:Role {id: row.id})
            SET n.code = row.code, n.name = row.name, n.dataScope = row.dataScope,
                n.status = row.status, n.syncId = $syncId
            """;

    private static final String MERGE_MENUS = """
            UNWIND $rows AS row
            MERGE (n:Menu {id: row.id})
            SET n.name = row.name, n.type = row.type, n.permission = row.permission,
                n.parentId = row.parentId, n.status = row.status, n.syncId = $syncId
            """;

    private static final String MERGE_PARENT_OF = """
            UNWIND $rows AS row
            MATCH (parent:Dept {id: row.parentId})
            MATCH (child:Dept {id: row.childId})
            MERGE (parent)-[r:PARENT_OF]->(child)
            SET r.syncId = $syncId
            """;

    private static final String MERGE_BELONGS_TO = """
            UNWIND $rows AS row
            MATCH (user:User {id: row.userId})
            MATCH (dept:Dept {id: row.deptId})
            MERGE (user)-[r:BELONGS_TO]->(dept)
            SET r.syncId = $syncId
            """;

    private static final String MERGE_HAS_ROLE = """
            UNWIND $rows AS row
            MATCH (user:User {id: row.userId})
            MATCH (role:Role {id: row.roleId})
            MERGE (user)-[r:HAS_ROLE]->(role)
            SET r.syncId = $syncId
            """;

    private static final String MERGE_GRANTS = """
            UNWIND $rows AS row
            MATCH (role:Role {id: row.roleId})
            MATCH (menu:Menu {id: row.menuId})
            MERGE (role)-[r:GRANTS]->(menu)
            SET r.syncId = $syncId
            """;

    private static final String MERGE_SCOPE_DEPT = """
            UNWIND $rows AS row
            MATCH (role:Role {id: row.roleId})
            MATCH (dept:Dept {id: row.deptId})
            MERGE (role)-[r:SCOPE_DEPT]->(dept)
            SET r.syncId = $syncId
            """;

    private static final String DELETE_STALE_EDGES = """
            MATCH ()-[r:PARENT_OF|BELONGS_TO|HAS_ROLE|GRANTS|SCOPE_DEPT]->()
            WHERE r.syncId IS NULL OR r.syncId <> $syncId
            DELETE r
            """;

    private static final String DELETE_STALE_NODES = """
            MATCH (n)
            WHERE (n:Dept OR n:User OR n:Role OR n:Menu)
              AND (n.syncId IS NULL OR n.syncId <> $syncId)
            WITH collect(n) AS stale
            FOREACH (node IN stale | DETACH DELETE node)
            RETURN size(stale) AS removed
            """;

    private static final String WRITE_SYNC_STATE = """
            MERGE (s:GraphSyncState {id: 'current'})
            SET s.syncId = $syncId, s.syncedAtEpochMilli = $syncedAt, s.departments = $departments,
                s.users = $users, s.roles = $roles, s.menus = $menus, s.relations = $relations
            """;

    private static final String READ_SYNC_STATE = """
            MATCH (s:GraphSyncState {id: 'current'})
            RETURN s.syncedAtEpochMilli AS syncedAt, s.departments AS departments, s.users AS users,
                   s.roles AS roles, s.menus AS menus, s.relations AS relations
            """;

    private static final String QUERY_COLLEAGUES = """
            MATCH (target:User {id: $userId})-[:BELONGS_TO]->(dept:Dept)
            WHERE $unrestricted OR dept.id IN $visibleDeptIds
            MATCH (peer:User)-[:BELONGS_TO]->(dept)
            WHERE peer.id <> $userId
            RETURN DISTINCT peer.id AS id, peer.username AS username, peer.nickname AS nickname,
                   dept.id AS deptId, dept.name AS deptName
            ORDER BY username
            LIMIT $limit
            """;

    private static final String QUERY_DEPT_MEMBERS = """
            MATCH (root:Dept {id: $deptId})
            WHERE $unrestricted OR root.id IN $visibleDeptIds
            MATCH (root)-[:PARENT_OF*0..10]->(sub:Dept)
            WHERE $unrestricted OR sub.id IN $visibleDeptIds
            MATCH (member:User)-[:BELONGS_TO]->(sub)
            RETURN DISTINCT member.id AS id, member.username AS username, member.nickname AS nickname,
                   sub.id AS deptId, sub.name AS deptName
            ORDER BY username
            LIMIT $limit
            """;

    private static final String QUERY_USER_PERMISSIONS = """
            MATCH (user:User {id: $userId})
            WHERE $self OR $unrestricted
               OR EXISTS { MATCH (user)-[:BELONGS_TO]->(dept:Dept) WHERE dept.id IN $visibleDeptIds }
            MATCH (user)-[:HAS_ROLE]->(role:Role)-[:GRANTS]->(menu:Menu)
            RETURN DISTINCT role.code AS roleCode, role.name AS roleName,
                   menu.name AS menuName, menu.permission AS permission
            ORDER BY roleCode, permission
            LIMIT $limit
            """;

    private static final String QUERY_PERM_HOLDERS = """
            MATCH (holder:User)-[:BELONGS_TO]->(dept:Dept)
            WHERE $unrestricted OR dept.id IN $visibleDeptIds
            MATCH (holder)-[:HAS_ROLE]->(:Role)-[:GRANTS]->(menu:Menu {permission: $permission})
            RETURN DISTINCT holder.id AS id, holder.username AS username, holder.nickname AS nickname,
                   dept.id AS deptId, dept.name AS deptName
            ORDER BY username
            LIMIT $limit
            """;

    private static final String QUERY_DEPT_PATH = """
            MATCH (from:Dept {id: $fromId}), (to:Dept {id: $toId})
            WHERE ($unrestricted OR from.id IN $visibleDeptIds)
              AND ($unrestricted OR to.id IN $visibleDeptIds)
            MATCH path = shortestPath((from)-[:PARENT_OF*0..10]-(to))
            WHERE all(node IN nodes(path) WHERE $unrestricted OR node.id IN $visibleDeptIds)
            RETURN [node IN nodes(path) | node.name] AS names, length(path) AS hops
            LIMIT 1
            """;

    private final Driver driver;

    private final SessionConfig sessionConfig;

    /**
     * 创建仓储。
     *
     * @param driver     图数据库驱动
     * @param properties 知识图谱配置
     */
    public Neo4jGraphRepository(Driver driver, GraphProperties properties) {
        this.driver = driver;
        this.sessionConfig = SessionConfig.forDatabase(properties.getDatabase());
    }

    /**
     * 用新快照整体替换图内容。
     *
     * @param snapshot 完整组织快照
     * @return 同步摘要
     */
    public GraphSyncSummary replace(GraphSnapshot snapshot) {
        Instant syncedAt = Instant.now();
        int relations = countRelations(snapshot);
        try (Session session = driver.session(sessionConfig)) {
            int removed = session.executeWrite(transaction -> {
                writeNodes(transaction, snapshot);
                writeRelations(transaction, snapshot);
                transaction.run(DELETE_STALE_EDGES, Map.of("syncId", snapshot.syncId())).consume();
                Record record = transaction.run(DELETE_STALE_NODES, Map.of("syncId", snapshot.syncId())).single();
                transaction.run(WRITE_SYNC_STATE, Map.of(
                        "syncId", snapshot.syncId(),
                        "syncedAt", syncedAt.toEpochMilli(),
                        "departments", snapshot.departments().size(),
                        "users", snapshot.users().size(),
                        "roles", snapshot.roles().size(),
                        "menus", snapshot.menus().size(),
                        "relations", relations)).consume();
                return record.get("removed").asInt();
            });
            return new GraphSyncSummary(snapshot.syncId(), syncedAt, snapshot.departments().size(),
                    snapshot.users().size(), snapshot.roles().size(), snapshot.menus().size(), relations, removed);
        }
    }

    /**
     * 读取上次同步状态。
     *
     * @return 状态；从未同步时返回 {@link GraphStatus#empty()}
     */
    public GraphStatus loadStatus() {
        try (Session session = driver.session(sessionConfig)) {
            List<Record> records = session.executeRead(transaction -> transaction.run(READ_SYNC_STATE).list());
            if (records.isEmpty()) {
                return GraphStatus.empty();
            }
            Record record = records.getFirst();
            return new GraphStatus(true, Instant.ofEpochMilli(record.get("syncedAt").asLong()),
                    record.get("departments").asInt(), record.get("users").asInt(),
                    record.get("roles").asInt(), record.get("menus").asInt(),
                    record.get("relations").asInt());
        }
    }

    /**
     * 查询同部门同事。
     *
     * @param userId 目标用户
     * @param scope  调用者数据权限范围
     * @param limit  返回行数上限
     * @return 同部门的其他用户
     */
    public List<UserRef> colleagues(long userId, QueryScope scope, int limit) {
        return read(QUERY_COLLEAGUES, Map.of("userId", userId, "limit", limit), scope, this::userRef);
    }

    /**
     * 查询部门成员，包含全部下级部门。
     *
     * @param deptId 目标部门
     * @param scope  调用者数据权限范围
     * @param limit  返回行数上限
     * @return 部门及其下级部门的用户
     */
    public List<UserRef> deptMembers(long deptId, QueryScope scope, int limit) {
        return read(QUERY_DEPT_MEMBERS, Map.of("deptId", deptId, "limit", limit), scope, this::userRef);
    }

    /**
     * 查询用户的角色与菜单权限。
     *
     * @param userId 目标用户
     * @param scope  调用者数据权限范围
     * @param self   目标是否为调用者本人
     * @param limit  返回行数上限
     * @return 权限记录
     */
    public List<PermissionRef> userPermissions(long userId, QueryScope scope, boolean self, int limit) {
        Map<String, Object> params = withScope(Map.of("userId", userId, "self", self, "limit", limit), scope);
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(transaction -> transaction.run(QUERY_USER_PERMISSIONS, params).list(record ->
                    new PermissionRef(record.get("roleCode").asString(), record.get("roleName").asString(),
                            record.get("menuName").asString(), record.get("permission").asString())));
        }
    }

    /**
     * 查询持有指定权限的用户。
     *
     * @param permission 权限标识
     * @param scope      调用者数据权限范围
     * @param limit      返回行数上限
     * @return 权限持有人
     */
    public List<UserRef> permHolders(String permission, QueryScope scope, int limit) {
        return read(QUERY_PERM_HOLDERS, Map.of("permission", permission, "limit", limit), scope, this::userRef);
    }

    /**
     * 查询两个部门之间的最短组织路径。
     *
     * @param fromId 起点部门
     * @param toId   终点部门
     * @param scope  调用者数据权限范围
     * @return 路径；不连通或越出可见范围时为空
     */
    public Optional<DeptPath> deptPath(long fromId, long toId, QueryScope scope) {
        Map<String, Object> params = withScope(Map.of("fromId", fromId, "toId", toId), scope);
        try (Session session = driver.session(sessionConfig)) {
            List<DeptPath> paths = session.executeRead(transaction ->
                    transaction.run(QUERY_DEPT_PATH, params).list(record -> new DeptPath(
                            record.get("names").asList(value -> value.asString()),
                            record.get("hops").asInt())));
            return paths.stream().findFirst();
        }
    }

    private <T> List<T> read(String statement, Map<String, Object> params, QueryScope scope,
                             java.util.function.Function<Record, T> mapper) {
        Map<String, Object> scoped = withScope(params, scope);
        try (Session session = driver.session(sessionConfig)) {
            return session.executeRead(transaction -> transaction.run(statement, scoped).list(mapper));
        }
    }

    private Map<String, Object> withScope(Map<String, Object> params, QueryScope scope) {
        Map<String, Object> scoped = new LinkedHashMap<>(params);
        scoped.put("unrestricted", scope.unrestricted());
        scoped.put("visibleDeptIds", scope.visibleDeptIds());
        return scoped;
    }

    private UserRef userRef(Record record) {
        return new UserRef(record.get("id").asLong(), record.get("username").asString(),
                record.get("nickname").asString(), record.get("deptId").asLong(),
                record.get("deptName").asString());
    }

    private void writeNodes(TransactionContext transaction, GraphSnapshot snapshot) {
        List<Map<String, Object>> depts = new ArrayList<>();
        for (DeptNode dept : snapshot.departments()) {
            depts.add(row("id", dept.id(), "name", dept.name(), "code", dept.code(),
                    "parentId", dept.parentId(), "status", dept.status()));
        }
        run(transaction, MERGE_DEPTS, depts, snapshot.syncId());

        List<Map<String, Object>> users = new ArrayList<>();
        for (UserNode user : snapshot.users()) {
            users.add(row("id", user.id(), "username", user.username(), "nickname", user.nickname(),
                    "deptId", user.deptId(), "status", user.status()));
        }
        run(transaction, MERGE_USERS, users, snapshot.syncId());

        List<Map<String, Object>> roles = new ArrayList<>();
        for (RoleNode role : snapshot.roles()) {
            roles.add(row("id", role.id(), "code", role.code(), "name", role.name(),
                    "dataScope", role.dataScope(), "status", role.status()));
        }
        run(transaction, MERGE_ROLES, roles, snapshot.syncId());

        List<Map<String, Object>> menus = new ArrayList<>();
        for (MenuNode menu : snapshot.menus()) {
            menus.add(row("id", menu.id(), "name", menu.name(), "type", menu.type(),
                    "permission", menu.permission(), "parentId", menu.parentId(), "status", menu.status()));
        }
        run(transaction, MERGE_MENUS, menus, snapshot.syncId());
    }

    private void writeRelations(TransactionContext transaction, GraphSnapshot snapshot) {
        List<Map<String, Object>> parentOf = new ArrayList<>();
        for (DeptNode dept : snapshot.departments()) {
            if (dept.parentId() != null) {
                parentOf.add(row("parentId", dept.parentId(), "childId", dept.id()));
            }
        }
        run(transaction, MERGE_PARENT_OF, parentOf, snapshot.syncId());

        List<Map<String, Object>> belongsTo = new ArrayList<>();
        List<Map<String, Object>> hasRole = new ArrayList<>();
        for (UserNode user : snapshot.users()) {
            if (user.deptId() != null) {
                belongsTo.add(row("userId", user.id(), "deptId", user.deptId()));
            }
            for (Long roleId : user.roleIds()) {
                hasRole.add(row("userId", user.id(), "roleId", roleId));
            }
        }
        run(transaction, MERGE_BELONGS_TO, belongsTo, snapshot.syncId());
        run(transaction, MERGE_HAS_ROLE, hasRole, snapshot.syncId());

        List<Map<String, Object>> grants = new ArrayList<>();
        List<Map<String, Object>> scopeDept = new ArrayList<>();
        for (RoleNode role : snapshot.roles()) {
            for (Long menuId : role.menuIds()) {
                grants.add(row("roleId", role.id(), "menuId", menuId));
            }
            for (Long deptId : role.deptIds()) {
                scopeDept.add(row("roleId", role.id(), "deptId", deptId));
            }
        }
        run(transaction, MERGE_GRANTS, grants, snapshot.syncId());
        run(transaction, MERGE_SCOPE_DEPT, scopeDept, snapshot.syncId());
    }

    private void run(TransactionContext transaction, String statement, List<Map<String, Object>> rows, String syncId) {
        transaction.run(statement, Map.of("rows", rows, "syncId", syncId)).consume();
    }

    private int countRelations(GraphSnapshot snapshot) {
        int total = 0;
        for (DeptNode dept : snapshot.departments()) {
            total += dept.parentId() == null ? 0 : 1;
        }
        for (UserNode user : snapshot.users()) {
            total += user.deptId() == null ? 0 : 1;
            total += user.roleIds().size();
        }
        for (RoleNode role : snapshot.roles()) {
            total += role.menuIds().size() + role.deptIds().size();
        }
        return total;
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }
}
