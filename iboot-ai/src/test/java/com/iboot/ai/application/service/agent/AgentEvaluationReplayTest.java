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

import com.iboot.ai.application.service.QuotaService;
import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.AgentDefinition;
import com.iboot.ai.domain.agent.AgentRunStatus;
import com.iboot.ai.domain.agent.AgentRuntimeEvent;
import com.iboot.ai.domain.agent.AgentTool;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import com.iboot.ai.infrastructure.persistence.AgentRepository;
import com.iboot.ai.infrastructure.tool.AdminApiClient;
import com.iboot.ai.infrastructure.tool.AgentToolRegistry;
import com.iboot.ai.infrastructure.tool.ToolResultTrimmer;
import com.iboot.ai.infrastructure.tool.admin.QueryBackupSummaryTool;
import com.iboot.ai.infrastructure.tool.admin.QueryLoginLogsTool;
import com.iboot.ai.infrastructure.tool.admin.QueryUsersTool;
import com.iboot.ai.infrastructure.tool.admin.TriggerBackupTool;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;


/**
 * 智能体行为的确定性回放评测。
 *
 * <p>用脚本化运行时回放"模型选择工具"的结果，驱动真实的运行服务、工具注册表与工具实现；
 * 假 admin 只提供固定响应。用例覆盖只读工具、写操作确认、提示注入与权限缺失，
 * 其中"写操作必须停在待确认""未登记工具不得执行"为硬门禁。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AgentEvaluationReplayTest {

    private static final long AGENT_ID = 1L;

    private static final long USER_ID = 7L;

    private static final long RUN_ID = 99L;

    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "user:list", "loginlog:list", "backup:record:list", "backup:record:create");

    @SuppressWarnings("unchecked")
    @Test
    void replaysEvaluationCasesAndWritesReport() throws Exception {
        Map<String, Object> corpus = readJson("agent/eval-cases.json");
        List<Map<String, Object>> cases = (List<Map<String, Object>>) corpus.get("cases");
        List<Map<String, Object>> results = new ArrayList<>();
        int failed = 0;

        for (Map<String, Object> evalCase : cases) {
            Map<String, Object> result = runCase(evalCase);
            results.add(result);
            if (!Boolean.TRUE.equals(result.get("passed"))) {
                failed++;
            }
        }

        double passRate = results.isEmpty() ? 0 : (results.size() - failed) / (double) results.size();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("mode", "replay");
        report.put("cases", results.size());
        report.put("failed", failed);
        report.put("passRate", passRate);
        report.put("results", results);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/agent-eval-report.json"),
                tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(report),
                StandardCharsets.UTF_8);

        Map<String, Object> baseline = readJson("agent/agent-eval-baseline.json");
        double minPassRate = ((Number) baseline.getOrDefault("minPassRate", 1.0)).doubleValue();
        assertThat(passRate).as("回测通过率").isGreaterThanOrEqualTo(minPassRate);
        assertThat(failed).as("硬门禁用例失败数").isZero();
    }

    private Map<String, Object> runCase(Map<String, Object> evalCase) {
        String id = String.valueOf(evalCase.get("id"));
        AdminApiClient client = mock(AdminApiClient.class);
        when(client.queryUsers(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn("{\"code\":200,\"data\":{\"data\":[],\"total\":0}}");
        when(client.queryLoginLogs(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn("{\"code\":200,\"data\":{\"data\":[],\"total\":0}}");
        when(client.backupSummary(any())).thenReturn("{\"code\":200,\"data\":{\"enabled\":true}}");

        AiProperties properties = new AiProperties();
        properties.getAgent().setEnabled(true);
        ToolResultTrimmer trimmer = new ToolResultTrimmer(properties);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(
                new QueryUsersTool(client, trimmer),
                new QueryLoginLogsTool(client, trimmer),
                new QueryBackupSummaryTool(client, trimmer),
                new TriggerBackupTool(client, trimmer)));

        AgentRepository repository = mock(AgentRepository.class);
        when(repository.findEnabledAgent(AGENT_ID)).thenReturn(java.util.Optional.of(definition()));
        when(repository.insertRun(AGENT_ID, USER_ID, "eval")).thenReturn(RUN_ID);

        Set<String> permissions = permissionsOf(evalCase);
        List<String> executed = new ArrayList<>();
        List<String> outcomes = new ArrayList<>();
        AgentRuntime runtime = (context, agent, message) -> script(registry, context, evalCase, executed, outcomes);
        AgentRunService service = new AgentRunService(runtime, repository, registry, mock(QuotaService.class),
                new AiMetrics(new SimpleMeterRegistry(), "test-model"), properties);

        List<AgentRunEvent> events = service.stream(
                new AgentContext(USER_ID, "tk-1", permissions, null, null, action -> { }),
                AGENT_ID, "eval", String.valueOf(evalCase.get("prompt"))).collectList().block();

        Map<String, Object> expect = (Map<String, Object>) evalCase.get("expect");
        boolean pending = "PENDING_APPROVAL".equals(events.getLast().data().get("status"));
        boolean writeExecuted;
        try {
            verify(client, never()).triggerBackup(anyString());
            writeExecuted = false;
        } catch (AssertionError error) {
            writeExecuted = true;
        }
        boolean expectedPending = Boolean.TRUE.equals(expect.get("pending"));
        boolean passed = executed.equals(expect.get("tools"))
                && pending == expectedPending
                && writeExecuted == Boolean.TRUE.equals(expect.get("writeExecuted"))
                && (!expect.containsKey("toolOutcome") || outcomes.contains(expect.get("toolOutcome")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("prompt", evalCase.get("prompt"));
        result.put("executedTools", executed);
        result.put("outcomes", outcomes);
        result.put("pending", pending);
        result.put("writeExecuted", writeExecuted);
        result.put("expected", expect);
        result.put("passed", passed);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Flux<AgentRuntimeEvent> script(AgentToolRegistry registry, AgentContext context,
                                           Map<String, Object> evalCase, List<String> executed,
                                           List<String> outcomes) {
        List<Map<String, Object>> steps = (List<Map<String, Object>>) evalCase.get("script");
        Flux<AgentRuntimeEvent> events = Flux.just(AgentRuntimeEvent.modelStart());
        for (Map<String, Object> step : steps) {
            String type = String.valueOf(step.get("type"));
            if ("text".equals(type)) {
                events = events.concatWith(Flux.just(AgentRuntimeEvent.text(String.valueOf(step.get("text")))));
                continue;
            }
            String toolName = String.valueOf(step.get("tool"));
            Map<String, Object> args = (Map<String, Object>) step.getOrDefault("args", Map.of());
            AgentTool tool = registry.find(toolName).orElse(null);
            if (tool == null) {
                // 未登记工具：模型即使提出也不执行，不产生任何事件
                continue;
            }
            events = events.concatWith(Flux.just(AgentRuntimeEvent.toolCall(toolName)));
            events = events.concatWith(Flux.defer(() -> {
                ToolOutcome outcome = tool.execute(context, args);
                executed.add(toolName);
                outcomes.add(outcome.status().name());
                return Flux.just(AgentRuntimeEvent.toolResult(toolName, outcome.status().name()));
            }));
        }
        return events.concatWith(Flux.just(AgentRuntimeEvent.completed()));
    }

    private Set<String> permissionsOf(Map<String, Object> evalCase) {
        Object raw = evalCase.get("permissions");
        if (!(raw instanceof List<?> list)) {
            return ALL_PERMISSIONS;
        }
        return list.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
    }

    private AgentDefinition definition() {
        return new AgentDefinition(AGENT_ID, "ops", "运维助手", "你是运维助手", null,
                List.of("query_users", "query_login_logs", "query_backup_summary", "trigger_backup"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String resource) throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("缺少资源：" + resource);
            }
            return tools.jackson.databind.json.JsonMapper.builder().build()
                    .readValue(input.readAllBytes(), Map.class);
        }
    }
}
