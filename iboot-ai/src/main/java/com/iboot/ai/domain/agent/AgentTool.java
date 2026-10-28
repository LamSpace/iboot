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

package com.iboot.ai.domain.agent;

import java.util.Map;

/**
 * 智能体可调用工具的统一契约。
 *
 * <p>每个工具声明名称、描述、JSON Schema、只读属性与所需权限，并以调用者身份执行。
 * 实现只允许调用带显式权限校验的下游接口；不得直接访问数据库，也不得接受模型提供的身份参数。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface AgentTool {

    /**
     * 工具名，需与模型可见的名称一致。
     *
     * @return 工具名
     */
    String name();

    /**
     * 工具用途描述，供模型选择工具。
     *
     * @return 描述文本
     */
    String description();

    /**
     * 参数 JSON Schema。
     *
     * @return Schema 对象
     */
    Map<String, Object> parameterSchema();

    /**
     * 是否为只读工具。
     *
     * @return 只读返回 {@code true}
     */
    boolean readOnly();

    /**
     * 调用该工具所需的权限码。
     *
     * @return 权限码
     */
    String requiredPermission();

    /**
     * 以调用者身份执行工具。
     *
     * @param context   运行上下文，提供身份、令牌与授权
     * @param arguments 模型提供的参数，实现必须忽略其中的身份字段
     * @return 执行结果
     */
    ToolOutcome execute(AgentContext context, Map<String, Object> arguments);
}
