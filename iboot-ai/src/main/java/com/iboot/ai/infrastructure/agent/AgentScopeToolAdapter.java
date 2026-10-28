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

package com.iboot.ai.infrastructure.agent;

import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.domain.agent.ToolOutcome;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 把内部 {@link AgentTool} 适配为 AgentScope 可调用的工具。
 *
 * <p>身份上下文从 {@code RuntimeContext} 中以类型键取出后显式传递，绝不使用线程局部变量。
 * 该适配器是 AgentScope 与业务工具之间唯一的耦合点。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AgentScopeToolAdapter implements io.agentscope.core.tool.AgentTool {

    private final AgentTool delegate;

    /**
     * 创建适配器。
     *
     * @param delegate 业务工具
     */
    AgentScopeToolAdapter(AgentTool delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.name();
    }

    @Override
    public String getDescription() {
        return delegate.description();
    }

    @Override
    public Map<String, Object> getParameters() {
        return delegate.parameterSchema();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.readOnly();
    }

    @Override
    public Mono<ToolResultBlock> callAsync(ToolCallParam param) {
        AgentContext context = param.getRuntimeContext() == null
                ? null
                : param.getRuntimeContext().get(AgentContext.class);
        if (context == null) {
            return Mono.just(ToolResultBlock.error("缺少运行上下文，已拒绝执行"));
        }
        ToolOutcome outcome;
        try {
            outcome = delegate.execute(context, param.getInput() == null ? Map.of() : param.getInput());
        } catch (RuntimeException e) {
            outcome = ToolOutcome.failed("工具执行异常");
        }
        if (outcome.status() == ToolOutcome.Status.OK
                || outcome.status() == ToolOutcome.Status.PENDING) {
            return Mono.just(ToolResultBlock.text(outcome.content()));
        }
        return Mono.just(ToolResultBlock.error(outcome.content()));
    }
}
