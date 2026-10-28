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

package com.iboot.ai.infrastructure.tool.admin;

import com.iboot.ai.domain.agent.AgentContext;
import com.iboot.ai.domain.agent.ApprovalGrant;
import com.iboot.ai.domain.agent.PendingAction;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.tool.AdminApiClient;
import com.iboot.ai.infrastructure.tool.ToolArguments;
import com.iboot.ai.infrastructure.tool.ToolResultTrimmer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link TriggerBackupTool} 的单元测试。
 *
 * <p>验证无授权时只登记待确认动作且不发起下游请求，授权匹配后才真正执行。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class TriggerBackupToolTest {

    private static final String PERMISSION = "backup:record:create";

    private final AdminApiClient client = mock(AdminApiClient.class);

    private final TriggerBackupTool tool = new TriggerBackupTool(client,
            new ToolResultTrimmer(new AiProperties()));

    private final List<PendingAction> pending = new ArrayList<>();

    /**
     * 无授权：返回待确认并登记动作，下游零请求。
     */
    @Test
    void registersPendingWithoutGrant() {
        ToolOutcome outcome = tool.execute(context(null), Map.of());

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().toolName()).isEqualTo("trigger_backup");
        assertThat(pending.getFirst().argsDigest()).isEqualTo(ToolArguments.digest(Map.of()));
        verifyNoInteractions(client);
    }

    /**
     * 授权匹配：真正调用下游。
     */
    @Test
    void executesWithMatchingGrant() {
        Map<String, Object> arguments = Map.of();
        when(client.triggerBackup("tk-1")).thenReturn("{\"code\":200,\"data\":{\"id\":7,\"status\":\"RUNNING\"}}");

        ToolOutcome outcome = tool.execute(context(new ApprovalGrant("trigger_backup",
                ToolArguments.digest(arguments))), arguments);

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.OK);
        assertThat(outcome.content()).contains("RUNNING");
        assertThat(pending).isEmpty();
        verify(client).triggerBackup("tk-1");
    }

    /**
     * 参数摘要不一致：视为未授权，不调用下游。
     */
    @Test
    void rejectsGrantWithDifferentDigest() {
        Map<String, Object> arguments = Map.of("reason", "手动");
        ToolOutcome outcome = tool.execute(context(new ApprovalGrant("trigger_backup",
                ToolArguments.digest(Map.of()))), arguments);

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.PENDING);
        assertThat(pending).hasSize(1);
        verifyNoInteractions(client);
    }

    /**
     * 缺少权限：直接拒绝，既不登记也不调用下游。
     */
    @Test
    void deniesWithoutPermission() {
        ToolOutcome outcome = tool.execute(new AgentContext(1L, "tk-1", Set.of(), null, null, pending::add),
                Map.of());

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.DENIED);
        assertThat(pending).isEmpty();
        verifyNoInteractions(client);
    }

    private AgentContext context(ApprovalGrant grant) {
        return new AgentContext(1L, "tk-1", Set.of(PERMISSION), null, grant, pending::add);
    }
}
