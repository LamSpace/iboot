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

package com.iboot.ai.application.service.agent;

import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentDefinition;
import com.iboot.ai.domain.agent.AgentRuntimeEvent;
import reactor.core.publisher.Flux;

/**
 * 智能体运行时的抽象。
 *
 * <p>应用层只依赖本接口，具体编排框架（AgentScope 或自管循环）由基础设施实现。
 * 实现必须把身份上下文显式传递给工具，并把框架事件映射为 {@link AgentRuntimeEvent}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface AgentRuntime {

    /**
     * 以流式方式执行一次智能体运行。
     *
     * @param context 运行上下文
     * @param agent   智能体定义快照
     * @param message 用户消息
     * @return 标准化事件流
     */
    Flux<AgentRuntimeEvent> stream(AgentContext context, AgentDefinition agent, String message);
}
