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

package com.iboot.ai.infrastructure.tool;

import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.domain.agent.ToolOutcome;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * admin 代理工具的公共基类。
 *
 * <p>统一完成本地权限前置校验、异常映射与结果裁剪。子类只负责组装参数与调用
 * {@link AdminApiClient}，不得自行访问下游。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public abstract class AdminToolSupport implements AgentTool {

    /**
     * 分页默认大小。
     */
    protected static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 下游客户端。
     */
    protected final AdminApiClient client;

    /**
     * 结果裁剪器。
     */
    protected final ToolResultTrimmer trimmer;

    /**
     * 创建工具。
     *
     * @param client  下游客户端
     * @param trimmer 结果裁剪器
     */
    protected AdminToolSupport(AdminApiClient client, ToolResultTrimmer trimmer) {
        this.client = client;
        this.trimmer = trimmer;
    }

    /**
     * 执行一次只读代理调用。
     *
     * <p>先校验本地权限，权限不足时直接返回且不发起下游请求；下游异常映射为可辨识的失败结果。
     *
     * @param context    运行上下文
     * @param permission 所需权限码
     * @param request    下游请求
     * @return 工具结果
     */
    protected ToolOutcome readOnlyCall(AgentContext context, String permission, Supplier<String> request) {
        if (!context.hasPermission(permission)) {
            return ToolOutcome.denied("当前用户没有执行该操作所需的权限：" + permission);
        }
        try {
            return ToolOutcome.ok(trimmer.trim(request.get()));
        } catch (AdminApiException e) {
            return ToolOutcome.failed(e.getMessage());
        }
    }

    /**
     * 计算分页大小，限制在 1 到允许的最大行数之间。
     *
     * @param arguments 参数表
     * @return 分页大小
     */
    protected int pageSize(Map<String, Object> arguments) {
        Integer requested = ToolArguments.integer(arguments, "pageSize", DEFAULT_PAGE_SIZE);
        int size = requested == null ? DEFAULT_PAGE_SIZE : requested;
        return Math.max(1, Math.min(size, trimmer.maxRows()));
    }

    /**
     * 计算页码，最小为 1。
     *
     * @param arguments 参数表
     * @return 页码
     */
    protected int pageNum(Map<String, Object> arguments) {
        Integer requested = ToolArguments.integer(arguments, "pageNum", 1);
        return requested == null ? 1 : Math.max(1, requested);
    }

    /**
     * 构造对象类型的参数 Schema。
     *
     * @param properties 属性定义
     * @param required   必填字段
     * @return Schema
     */
    protected static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    /**
     * 构造单个属性定义。
     *
     * @param type         JSON Schema 类型
     * @param description  说明
     * @return 属性定义
     */
    protected static Map<String, Object> property(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
