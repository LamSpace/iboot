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

import com.iboot.ai.application.service.agent.AgentRuntime;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentDefinition;
import com.iboot.ai.domain.agent.AgentRuntimeEvent;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.tool.AgentToolRegistry;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.ExceedMaxItersEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 基于 AgentScope Java 2.0.1 的运行时实现。
 *
 * <p>只使用 {@code AgentTool} 注册工具（不经反射生成 schema），不启用结构化输出重载，
 * 以规避 victools 双版本冲突（见 {@code doc/spike/ai-1.3-spike-report.md}）。
 * 模型配置复用 {@code spring.ai.openai.chat.*}，与 1.1/1.2 指向同一端点。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.agent", name = "enabled", havingValue = "true")
public class AgentScopeRuntime implements AgentRuntime {

    private static final String DEFAULT_ENDPOINT_BASE = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private final AgentToolRegistry registry;

    private final AiProperties properties;

    private final String baseUrl;

    private final String apiKey;

    private final String defaultModel;

    /**
     * 创建运行时。
     *
     * @param registry     工具注册表
     * @param properties   AI 配置
     * @param baseUrl      对话模型端点
     * @param apiKey       对话模型密钥
     * @param defaultModel 默认模型标识
     */
    public AgentScopeRuntime(AgentToolRegistry registry,
                             AiProperties properties,
                             @Value("${spring.ai.openai.chat.base-url:" + DEFAULT_ENDPOINT_BASE + "}") String baseUrl,
                             @Value("${spring.ai.openai.chat.api-key:}") String apiKey,
                             @Value("${spring.ai.openai.chat.options.model:qwen3.8-flash}") String defaultModel) {
        this.registry = registry;
        this.properties = properties;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
    }

    @Override
    public Flux<AgentRuntimeEvent> stream(AgentContext context, AgentDefinition agent, String message) {
        Toolkit toolkit = new Toolkit();
        for (AgentTool tool : registry.allowed(agent.toolAllowlist())) {
            toolkit.registerAgentTool(new AgentScopeToolAdapter(tool));
        }
        ReActAgent reactAgent = ReActAgent.builder()
                .name(agent.name())
                .sysPrompt(agent.systemPrompt())
                .model(model(agent.model()))
                .toolkit(toolkit)
                .maxIters(Math.max(1, properties.getAgent().getMaxSteps()))
                .build();
        RuntimeContext runtimeContext = RuntimeContext.empty();
        runtimeContext.put(AgentContext.class, context);

        Msg userMessage = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .textContent(message)
                .build();
        return reactAgent.streamEvents(List.of(userMessage), runtimeContext)
                .handle((event, sink) -> {
                    AgentRuntimeEvent mapped = map(event);
                    if (mapped != null) {
                        sink.next(mapped);
                    }
                });
    }

    private Model model(String override) {
        String modelName = override == null || override.isBlank() ? defaultModel : override;
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .stream(true)
                .generateOptions(GenerateOptions.builder()
                        .temperature(0.2)
                        .additionalBodyParam("enable_thinking", false)
                        .build())
                .build();
    }

    private AgentRuntimeEvent map(AgentEvent event) {
        AgentEventType type = event.getType();
        return switch (type) {
            case MODEL_CALL_START -> AgentRuntimeEvent.modelStart();
            case TEXT_BLOCK_DELTA -> AgentRuntimeEvent.text(((TextBlockDeltaEvent) event).getDelta());
            case TOOL_CALL_END -> AgentRuntimeEvent.toolCall(((ToolCallEndEvent) event).getToolCallName());
            case TOOL_RESULT_END -> AgentRuntimeEvent.toolResult(((ToolResultEndEvent) event).getToolCallName(),
                    ((ToolResultEndEvent) event).getState() == null
                            ? "UNKNOWN" : ((ToolResultEndEvent) event).getState().name());
            case MODEL_CALL_END -> {
                ModelCallEndEvent modelEvent = (ModelCallEndEvent) event;
                yield modelEvent.getUsage() == null
                        ? null
                        : AgentRuntimeEvent.usage(modelEvent.getUsage().getInputTokens(),
                        modelEvent.getUsage().getOutputTokens());
            }
            case EXCEED_MAX_ITERS -> AgentRuntimeEvent.stepLimit();
            case AGENT_END -> AgentRuntimeEvent.completed();
            default -> null;
        };
    }
}
