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
import com.iboot.ai.application.service.graph.GraphScopeUnavailableException;
import com.iboot.ai.application.service.graph.GraphUnavailableException;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.domain.graph.DeptPath;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.infrastructure.graph.GraphScopeResolver;
import com.iboot.ai.infrastructure.tool.ToolArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 跨部门组织路径只读查询工具。
 *
 * <p>返回两个部门之间经过共同上级的最短路径；路径上任一部门不在调用者可见范围内时返回空结果。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class QueryDeptPathTool implements AgentTool {

    /**
     * 工具名。
     */
    public static final String NAME = "query_dept_path";

    private static final String PERMISSION = "ai:graph:use";

    private final GraphQueryService queryService;

    private final GraphScopeResolver scopeResolver;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * 创建工具。
     *
     * @param queryService  图查询服务
     * @param scopeResolver 数据权限范围解析器
     */
    public QueryDeptPathTool(GraphQueryService queryService, GraphScopeResolver scopeResolver) {
        this.queryService = queryService;
        this.scopeResolver = scopeResolver;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "查询两个部门之间的组织路径，用于回答跨部门协作、汇报关系等问题。只返回调用者可见范围内的路径。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("fromDeptId", Map.of("type", "integer", "description", "起点部门 ID"));
        properties.put("toDeptId", Map.of("type", "integer", "description", "终点部门 ID"));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("fromDeptId", "toDeptId"));
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
            QueryScope scope = scopeResolver.resolve(context.userId(), context.bearerToken());
            Optional<DeptPath> path = queryService.deptPath(ToolArguments.longValue(arguments, "fromDeptId"),
                    ToolArguments.longValue(arguments, "toDeptId"), scope);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("path", path.orElse(null));
            return ToolOutcome.ok(jsonMapper.writeValueAsString(payload));
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
}
