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

package com.iboot.ai.interfaces.dto;

import com.iboot.ai.domain.agent.AgentTool;

/**
 * 可用工具描述。
 *
 * @param name               工具名
 * @param description        说明
 * @param readOnly           是否只读
 * @param requiredPermission 所需权限
 * @author iBoot Team
 * @since 1.0.0
 */
public record AgentToolResponse(String name, String description, boolean readOnly, String requiredPermission) {

    /**
     * 由工具构造响应。
     *
     * @param tool 工具
     * @return 响应
     */
    public static AgentToolResponse from(AgentTool tool) {
        return new AgentToolResponse(tool.name(), tool.description(), tool.readOnly(),
                tool.requiredPermission());
    }
}
