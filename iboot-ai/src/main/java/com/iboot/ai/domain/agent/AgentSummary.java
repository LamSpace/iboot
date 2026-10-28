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

import java.util.List;

/**
 * 智能体配置视图。
 *
 * @param id            智能体 ID
 * @param name          名称
 * @param description   描述
 * @param systemPrompt  系统提示词
 * @param model         模型标识
 * @param toolAllowlist 工具白名单
 * @param enabled       是否启用
 * @author iBoot Team
 * @since 1.0.0
 */
public record AgentSummary(Long id,
                           String name,
                           String description,
                           String systemPrompt,
                           String model,
                           List<String> toolAllowlist,
                           boolean enabled) {
}
