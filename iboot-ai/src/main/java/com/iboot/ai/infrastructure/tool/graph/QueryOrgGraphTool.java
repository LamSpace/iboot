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

package com.iboot.ai.infrastructure.tool.graph;

import com.iboot.ai.application.service.graph.GraphAccessDeniedException;
import com.iboot.ai.application.service.graph.GraphQueryService;
import com.iboot.ai.application.service.graph.GraphRows;
import com.iboot.ai.application.service.graph.GraphScopeUnavailableException;
import com.iboot.ai.application.service.graph.GraphUnavailableException;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.domain.graph.GraphQueryTemplate;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.infrastructure.graph.GraphScopeResolver;
import com.iboot.ai.infrastructure.tool.ToolArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织图谱只读查询工具。
 *
 * <p>模型只能选择模板与填写目标主键，调用者身份与数据范围始终取自运行上下文；
 * 模板不支持自由 Cypher。结果行数受 {@code iboot.ai.graph.query.max-rows} 限制，
 * 超出时截断并在返回值中标记 {@code truncated}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class QueryOrgGraphTool implements AgentTool {

    /**
     * 工具名。
     */
    public static final String NAME = "query_org_graph";

    private static final String PERMISSION = "ai:graph:use";

    private static final List<String> TEMPLATES = List.of("colleagues", "dept_members", "user_permissions",
            "perm_holders");

    private final GraphQueryService queryService;

    private final GraphScopeResolver scopeResolver;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * 创建工具。
     *
     * @param queryService  图查询服务
     * @param scopeResolver 数据权限范围解析器
     */
    public QueryOrgGraphTool(GraphQueryService queryService, GraphScopeResolver scopeResolver) {
        this.queryService = queryService;
        this.scopeResolver = scopeResolver;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "按模板查询组织架构与 RBAC 关系：同部门同事、部门成员、用户角色菜单权限、权限持有人。"
                + "只返回调用者可见范围内的数据。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("template", Map.of("type", "string", "enum", TEMPLATES,
                "description", "查询模板：colleagues、dept_members、user_permissions、perm_holders"));
        properties.put("userId", Map.of("type", "integer", "description", "目标用户 ID，可省略表示本人"));
        properties.put("deptId", Map.of("type", "integer", "description", "目标部门 ID，dept_members 必填"));
        properties.put("permission", Map.of("type", "string", "description", "权限标识，perm_holders 必填"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("template"));
        return schema;
    }

    @Override
    public boolean readOnly() {
        return true;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public ToolOutcome execute(AgentContext context, Map<String, Object> arguments) {
        if (!context.hasPermission(PERMISSION)) {
            return ToolOutcome.denied("当前用户没有执行该操作所需的权限：" + PERMISSION);
        }
        try {
            GraphQueryTemplate template = GraphQueryTemplate.fromWire(ToolArguments.string(arguments, "template"));
            if (template == GraphQueryTemplate.DEPT_PATH) {
                return ToolOutcome.failed("dept_path 请使用 query_dept_path 工具");
            }
            QueryScope scope = scopeResolver.resolve(context.userId(), context.bearerToken());
            return switch (template) {
                case COLLEAGUES -> render(queryService.colleagues(
                        ToolArguments.longValue(arguments, "userId"), context.userId(), scope));
                case DEPT_MEMBERS -> render(queryService.deptMembers(
                        ToolArguments.longValue(arguments, "deptId"), scope));
                case USER_PERMISSIONS -> render(queryService.userPermissions(
                        ToolArguments.longValue(arguments, "userId"), context.userId(), scope,
                        context.permissions()));
                case PERM_HOLDERS -> render(queryService.permHolders(
                        ToolArguments.string(arguments, "permission"), scope));
                case DEPT_PATH -> ToolOutcome.failed("dept_path 请使用 query_dept_path 工具");
            };
        } catch (GraphAccessDeniedException e) {
            return ToolOutcome.denied(e.getMessage());
        } catch (GraphScopeUnavailableException | GraphUnavailableException e) {
            return ToolOutcome.failed(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolOutcome.failed(e.getMessage());
        } catch (RuntimeException e) {
            return ToolOutcome.failed("图查询失败");
        }
    }

    private ToolOutcome render(GraphRows<?> rows) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rows", rows.rows());
        payload.put("truncated", rows.truncated());
        return ToolOutcome.ok(jsonMapper.writeValueAsString(payload));
    }
}
