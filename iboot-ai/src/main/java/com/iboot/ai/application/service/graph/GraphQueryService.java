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

package com.iboot.ai.application.service.graph;

import com.iboot.ai.domain.graph.DeptPath;
import com.iboot.ai.domain.graph.PermissionRef;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.domain.graph.UserRef;
import com.iboot.ai.infrastructure.config.GraphProperties;
import com.iboot.ai.infrastructure.graph.Neo4jGraphRepository;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * 图查询应用服务。
 *
 * <p>校验模板参数、收敛返回行数，并执行「查询他人权限需要 {@code user:list}」的本地拒绝。
 * 数据权限范围由调用方解析后传入，查询本身不再做范围判断。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class GraphQueryService {

    private final Neo4jGraphRepository repository;

    private final GraphProperties properties;

    private final AiMetrics metrics;

    /**
     * 创建查询服务。
     *
     * @param repository 图仓储
     * @param properties 知识图谱配置
     * @param metrics    指标记录
     */
    public GraphQueryService(Neo4jGraphRepository repository, GraphProperties properties, AiMetrics metrics) {
        this.repository = repository;
        this.properties = properties;
        this.metrics = metrics;
    }

    /**
     * 查询同部门同事。
     *
     * @param targetUserId 目标用户；为空表示调用者本人
     * @param callerId     调用者
     * @param scope        数据权限范围
     * @return 同部门的其他用户
     */
    public GraphRows<UserRef> colleagues(Long targetUserId, long callerId, QueryScope scope) {
        long target = targetUserId == null ? callerId : targetUserId;
        return measured("colleagues", () -> GraphRows.of(repository.colleagues(target, scope, limit() + 1), limit()));
    }

    /**
     * 查询部门成员。
     *
     * @param deptId 目标部门
     * @param scope  数据权限范围
     * @return 部门及其下级部门的用户
     * @throws IllegalArgumentException 缺少部门标识时抛出
     */
    public GraphRows<UserRef> deptMembers(Long deptId, QueryScope scope) {
        if (deptId == null) {
            throw new IllegalArgumentException("缺少部门标识");
        }
        return measured("dept_members",
                () -> GraphRows.of(repository.deptMembers(deptId, scope, limit() + 1), limit()));
    }

    /**
     * 查询用户权限。
     *
     * @param targetUserId 目标用户；为空表示调用者本人
     * @param callerId     调用者
     * @param scope        数据权限范围
     * @param permissions  调用者权限码集合
     * @return 权限记录
     * @throws GraphAccessDeniedException 查询他人权限且缺少 {@code user:list} 时抛出
     */
    public GraphRows<PermissionRef> userPermissions(Long targetUserId, long callerId, QueryScope scope,
                                                    Set<String> permissions) {
        long target = targetUserId == null ? callerId : targetUserId;
        boolean self = target == callerId;
        if (!self && !permissions.contains("user:list")) {
            throw new GraphAccessDeniedException("查询他人权限需要 user:list 权限");
        }
        return measured("user_permissions",
                () -> GraphRows.of(repository.userPermissions(target, scope, self, limit() + 1), limit()));
    }

    /**
     * 查询权限持有人。
     *
     * @param permission 权限标识
     * @param scope      数据权限范围
     * @return 权限持有人
     * @throws IllegalArgumentException 缺少权限标识时抛出
     */
    public GraphRows<UserRef> permHolders(String permission, QueryScope scope) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("缺少权限标识");
        }
        return measured("perm_holders",
                () -> GraphRows.of(repository.permHolders(permission, scope, limit() + 1), limit()));
    }

    /**
     * 查询跨部门最短路径。
     *
     * @param fromDeptId 起点部门
     * @param toDeptId   终点部门
     * @param scope      数据权限范围
     * @return 路径；不连通或越出可见范围时为空
     * @throws IllegalArgumentException 缺少部门标识时抛出
     */
    public Optional<DeptPath> deptPath(Long fromDeptId, Long toDeptId, QueryScope scope) {
        if (fromDeptId == null || toDeptId == null) {
            throw new IllegalArgumentException("缺少部门标识");
        }
        return measured("dept_path", () -> repository.deptPath(fromDeptId, toDeptId, scope));
    }

    private <T> T measured(String template, java.util.function.Supplier<T> query) {
        long start = System.nanoTime();
        try {
            T result = query.get();
            metrics.graphQuery(template, "success", java.time.Duration.ofNanos(System.nanoTime() - start));
            return result;
        } catch (RuntimeException e) {
            metrics.graphQuery(template, "error", java.time.Duration.ofNanos(System.nanoTime() - start));
            throw e;
        }
    }

    private int limit() {
        return Math.max(1, properties.getQuery().getMaxRows());
    }
}
