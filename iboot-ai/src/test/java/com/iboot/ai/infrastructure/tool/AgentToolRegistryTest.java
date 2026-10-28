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
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.tool.admin.QueryBackupSummaryTool;
import com.iboot.ai.infrastructure.tool.admin.QueryLoginLogsTool;
import com.iboot.ai.infrastructure.tool.admin.QueryUsersTool;
import com.iboot.ai.infrastructure.tool.admin.TriggerBackupTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * {@link AgentToolRegistry} 的单元测试。
 *
 * <p>验证未登记工具不可解析、白名单收窄与重名保护。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class AgentToolRegistryTest {

    private final AdminApiClient client = mock(AdminApiClient.class);

    private final ToolResultTrimmer trimmer = new ToolResultTrimmer(new AiProperties());

    /**
     * 四个内置工具均登记，且只读标记正确。
     */
    @Test
    void registersBuiltInToolsWithReadOnlyFlags() {
        AgentToolRegistry registry = new AgentToolRegistry(List.of(
                new QueryUsersTool(client, trimmer),
                new QueryLoginLogsTool(client, trimmer),
                new QueryBackupSummaryTool(client, trimmer),
                new TriggerBackupTool(client, trimmer)));

        assertThat(registry.names()).containsExactlyInAnyOrder(
                "query_users", "query_login_logs", "query_backup_summary", "trigger_backup");
        assertThat(registry.find("query_users").orElseThrow().readOnly()).isTrue();
        assertThat(registry.find("trigger_backup").orElseThrow().readOnly()).isFalse();
        assertThat(registry.find("trigger_backup").orElseThrow().requiredPermission())
                .isEqualTo("backup:record:create");
    }

    /**
     * 白名单中的未知工具被忽略。
     */
    @Test
    void ignoresUnknownToolsInAllowlist() {
        AgentToolRegistry registry = new AgentToolRegistry(List.of(new QueryUsersTool(client, trimmer)));

        List<AgentTool> allowed = registry.allowed(List.of("query_users", "delete_user"));

        assertThat(allowed).extracting(AgentTool::name).containsExactly("query_users");
        assertThat(registry.find("delete_user")).isEmpty();
    }

    /**
     * 重名工具在启动期直接失败。
     */
    @Test
    void rejectsDuplicateNames() {
        assertThatThrownBy(() -> new AgentToolRegistry(List.of(
                new StaticTool("dup", true), new StaticTool("dup", false))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("工具名重复");
    }

    /**
     * 测试用工具。
     */
    private record StaticTool(String name, boolean readOnly) implements AgentTool {

        @Override
        public String description() {
            return name;
        }

        @Override
        public Map<String, Object> parameterSchema() {
            return Map.of("type", "object");
        }

        @Override
        public String requiredPermission() {
            return "test:read";
        }

        @Override
        public ToolOutcome execute(AgentContext context, Map<String, Object> arguments) {
            return ToolOutcome.ok("{}");
        }
    }
}
