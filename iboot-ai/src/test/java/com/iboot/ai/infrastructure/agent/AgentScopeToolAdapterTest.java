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
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.ToolCallParam;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AgentScopeToolAdapter} 的单元测试。
 *
 * <p>验证身份上下文从 {@code RuntimeContext} 取回并传给业务工具，缺少上下文时拒绝执行。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AgentScopeToolAdapterTest {

    private final AgentTool delegate = mock(AgentTool.class);

    private final AgentScopeToolAdapter adapter = new AgentScopeToolAdapter(delegate);

    /**
     * 上下文存在时把参数与上下文原样传给业务工具。
     */
    @Test
    void delegatesWithRuntimeContext() {
        AgentContext context = new AgentContext(7L, "tk-1", Set.of("user:list"), 99L, null, action -> { });
        RuntimeContext runtime = RuntimeContext.empty();
        runtime.put(AgentContext.class, context);
        ToolCallParam param = mock(ToolCallParam.class);
        when(param.getRuntimeContext()).thenReturn(runtime);
        when(param.getInput()).thenReturn(Map.of("username", "ali"));
        when(delegate.execute(context, Map.of("username", "ali"))).thenReturn(ToolOutcome.ok("结果"));

        ToolResultBlock block = adapter.callAsync(param).block();

        assertThat(text(block)).isEqualTo("结果");
        verify(delegate).execute(context, Map.of("username", "ali"));
    }

    /**
     * 缺少上下文时拒绝执行，不调用业务工具。
     */
    @Test
    void rejectsWithoutContext() {
        ToolCallParam param = mock(ToolCallParam.class);
        when(param.getRuntimeContext()).thenReturn(null);

        ToolResultBlock block = adapter.callAsync(param).block();

        assertThat(text(block)).contains("缺少运行上下文");
    }

    /**
     * 无权限结果以错误文本返回，供模型区分。
     */
    @Test
    void mapsDeniedToErrorBlock() {
        AgentContext context = new AgentContext(7L, "tk-1", Set.of(), null, null, action -> { });
        RuntimeContext runtime = RuntimeContext.empty();
        runtime.put(AgentContext.class, context);
        ToolCallParam param = mock(ToolCallParam.class);
        when(param.getRuntimeContext()).thenReturn(runtime);
        when(param.getInput()).thenReturn(Map.of());
        when(delegate.execute(context, Map.of())).thenReturn(ToolOutcome.denied("没有权限"));

        ToolResultBlock block = adapter.callAsync(param).block();

        assertThat(text(block)).contains("没有权限");
    }

    private String text(ToolResultBlock block) {
        List<ContentBlock> output = block.getOutput();
        assertThat(output).isNotEmpty();
        assertThat(output.getFirst()).isInstanceOf(TextBlock.class);
        return ((TextBlock) output.getFirst()).getText();
    }
}
