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
import com.iboot.ai.domain.agent.AgentRuntimeEvent;
import com.iboot.ai.infrastructure.agent.AgentScopeRuntime;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 智能体行为的真实模型评测。
 *
 * <p>只在 {@code cloud} 分组运行，使用真实百炼模型跑固定评测集，统计工具选择准确率与
 * 写操作确认合规率，产出 {@code target/agent-eval-cloud-report.json}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Tag("cloud")
class AgentEvaluationCloudTest {

    private static final long AGENT_ID = 1L;

    private static final long USER_ID = 7L;

    private static final long RUN_ID = 99L;

    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "user:list", "loginlog:list", "backup:record:list", "backup:record:create");

    @SuppressWarnings("unchecked")
    @Test
    void evaluatesToolSelectionWithRealModel() throws Exception {
        assumeTrue(System.getenv("DASHSCOPE_CHAT_API_KEY") != null
                && !System.getenv("DASHSCOPE_CHAT_API_KEY").isBlank(), "缺少 DASHSCOPE_CHAT_API_KEY");

        Map<String, Object> corpus = readJson("agent/eval-cases.json");
        List<Map<String, Object>> cases = (List<Map<String, Object>>) corpus.get("cases");
        List<Map<String, Object>> results = new ArrayList<>();
        int selectedCorrectly = 0;

        for (Map<String, Object> evalCase : cases) {
            Map<String, Object> result = runCase(evalCase);
            results.add(result);
            if (Boolean.TRUE.equals(result.get("selectionCorrect"))) {
                selectedCorrectly++;
            }
        }

        double accuracy = cases.isEmpty() ? 0 : selectedCorrectly / (double) cases.size();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("mode", "cloud");
        report.put("model", System.getenv().getOrDefault("IBOOT_AI_CHAT_MODEL", "qwen3.8-flash"));
        report.put("cases", cases.size());
        report.put("selectionCorrect", selectedCorrectly);
        report.put("selectionAccuracy", accuracy);
        report.put("results", results);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/agent-eval-cloud-report.json"),
                tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(report),
                StandardCharsets.UTF_8);
        System.out.printf("Agent tool selection accuracy: %.2f%% (%d/%d)%n", accuracy * 100, selectedCorrectly,
                cases.size());

        Map<String, Object> baseline = readJson("agent/agent-eval-cloud-baseline.json");
        double minAccuracy = ((Number) baseline.getOrDefault("minSelectionAccuracy", 0.75)).doubleValue();
        assertThat(accuracy).as("真实模型的工具选择准确率").isGreaterThanOrEqualTo(minAccuracy);
    }

    @SuppressWarnings("unchecked")
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
        properties.getAgent().setMaxSteps(4);
        ToolResultTrimmer trimmer = new ToolResultTrimmer(properties);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(
                new QueryUsersTool(client, trimmer),
                new QueryLoginLogsTool(client, trimmer),
                new QueryBackupSummaryTool(client, trimmer),
                new TriggerBackupTool(client, trimmer)));

        AgentRepository repository = mock(AgentRepository.class);
        when(repository.findEnabledAgent(AGENT_ID)).thenReturn(Optional.of(definition()));
        when(repository.insertRun(AGENT_ID, USER_ID, "eval")).thenReturn(RUN_ID);

        AgentScopeRuntime runtime = new AgentScopeRuntime(registry, properties,
                System.getenv().getOrDefault("DASHSCOPE_BASE_URL",
                        "https://dashscope.aliyuncs.com/compatible-mode").replaceAll("/+$", "") + "/v1",
                System.getenv("DASHSCOPE_CHAT_API_KEY"),
                System.getenv().getOrDefault("IBOOT_AI_CHAT_MODEL", "qwen3.8-flash"));
        AgentRunService service = new AgentRunService(runtime, repository, registry, mock(QuotaService.class),
                new AiMetrics(new SimpleMeterRegistry(), "test-model"), properties);

        List<String> selected = new CopyOnWriteArrayList<>();
        List<AgentRunEvent> events = service
                .stream(new AgentContext(USER_ID, "tk-1", permissionsOf(evalCase), null, null, action -> { }),
                        AGENT_ID, "eval", String.valueOf(evalCase.get("prompt")))
                .doOnNext(event -> {
                    if ("step".equals(event.name())) {
                        selected.add(String.valueOf(event.data().get("tool")));
                    }
                })
                .collectList()
                .block();

        Map<String, Object> expect = (Map<String, Object>) evalCase.get("expect");
        List<String> expectedTools = (List<String>) expect.get("tools");
        boolean pending = events != null && !events.isEmpty()
                && "PENDING_APPROVAL".equals(events.getLast().data().get("status"));
        boolean writeExecuted;
        try {
            verify(client, never()).triggerBackup(anyString());
            writeExecuted = false;
        } catch (AssertionError error) {
            writeExecuted = true;
        }
        List<String> distinctSelected = selected.stream().distinct().toList();
        boolean selectionCorrect = distinctSelected.equals(expectedTools)
                || (expectedTools.isEmpty() && selected.isEmpty());
        int duplicateCalls = selected.size() - distinctSelected.size();
        assertThat(writeExecuted).as("写操作不得在未确认时执行：%s", id).isFalse();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("prompt", evalCase.get("prompt"));
        result.put("selectedTools", selected);
        result.put("expectedTools", expectedTools);
        result.put("pending", pending);
        result.put("distinctTools", distinctSelected);
        result.put("duplicateCalls", duplicateCalls);
        result.put("selectionCorrect", selectionCorrect);
        result.put("events", events == null ? List.of() : events.stream().map(AgentRunEvent::name).toList());
        result.put("lastEvent", events == null || events.isEmpty() ? Map.of() : events.getLast().data());
        return result;
    }

    private Set<String> permissionsOf(Map<String, Object> evalCase) {
        Object raw = evalCase.get("permissions");
        if (!(raw instanceof List<?> list)) {
            return ALL_PERMISSIONS;
        }
        return list.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
    }

    private AgentDefinition definition() {
        return new AgentDefinition(AGENT_ID, "ops", "运维助手",
                "你是运维助手。凡是涉及用户、登录日志、备份状态的问题，必须先调用对应工具获取数据，"
                        + "不得凭记忆或常识直接回答；写操作会先请求用户确认。", null,
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
