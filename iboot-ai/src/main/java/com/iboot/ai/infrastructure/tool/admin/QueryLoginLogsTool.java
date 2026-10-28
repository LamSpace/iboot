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

package com.iboot.ai.infrastructure.tool.admin;

import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.infrastructure.tool.AdminApiClient;
import com.iboot.ai.infrastructure.tool.AdminToolSupport;
import com.iboot.ai.infrastructure.tool.ToolArguments;
import com.iboot.ai.infrastructure.tool.ToolResultTrimmer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询登录日志只读工具。
 *
 * <p>以调用者身份代理访问 admin 的登录日志接口；所需权限为 {@code loginlog:list}。
 * 数据范围由 admin 侧拦截器处理，本工具不追加也不放宽任何过滤条件。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class QueryLoginLogsTool extends AdminToolSupport {

    /**
     * 工具名。
     */
    public static final String NAME = "query_login_logs";

    private static final String PERMISSION = "loginlog:list";

    /**
     * 创建工具。
     *
     * @param client  下游客户端
     * @param trimmer 结果裁剪器
     */
    public QueryLoginLogsTool(AdminApiClient client, ToolResultTrimmer trimmer) {
        super(client, trimmer);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "按用户名、IP、状态或时间范围分页查询登录日志，返回登录时间、IP、浏览器与结果。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("username", property("string", "用户名关键字，模糊匹配"));
        properties.put("ipAddress", property("string", "登录 IP 关键字"));
        properties.put("status", property("integer", "状态：0-失败，1-成功"));
        properties.put("beginTime", property("string", "开始时间，格式 yyyy-MM-dd HH:mm:ss"));
        properties.put("endTime", property("string", "结束时间，格式 yyyy-MM-dd HH:mm:ss"));
        properties.put("pageNum", property("integer", "页码，从 1 开始"));
        properties.put("pageSize", property("integer", "每页数量，最多 " + DEFAULT_PAGE_SIZE + " 条"));
        return objectSchema(properties, List.of());
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
        return readOnlyCall(context, PERMISSION, () -> client.queryLoginLogs(
                context.bearerToken(),
                ToolArguments.string(arguments, "username"),
                ToolArguments.string(arguments, "ipAddress"),
                ToolArguments.integer(arguments, "status", null),
                ToolArguments.string(arguments, "beginTime"),
                ToolArguments.string(arguments, "endTime"),
                pageNum(arguments),
                pageSize(arguments)));
    }
}
