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
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.tool.AdminApiClient;
import com.iboot.ai.infrastructure.tool.ToolResultTrimmer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link QueryUsersTool} 的单元测试。
 *
 * <p>验证身份只取自运行上下文，模型伪造的身份参数被忽略；权限不足时不发起下游请求。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class QueryUsersToolTest {

    private final AdminApiClient client = mock(AdminApiClient.class);

    private final QueryUsersTool tool = new QueryUsersTool(client,
            new ToolResultTrimmer(new AiProperties()));

    /**
     * 伪造的 userId / token 参数不进入下游调用。
     */
    @Test
    void ignoresForgedIdentityArguments() {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("username", "ali");
        arguments.put("status", 1);
        arguments.put("pageSize", 5);
        arguments.put("userId", 999L);
        arguments.put("token", "forged-token");
        arguments.put("deptId", 88L);
        when(client.queryUsers("tk-1", "ali", 1, 1, 5)).thenReturn("{\"code\":200,\"data\":[]}");

        ToolOutcome outcome = tool.execute(new AgentContext(1L, "tk-1", Set.of("user:list"), null, null,
                action -> { }), arguments);

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.OK);
        verify(client).queryUsers("tk-1", "ali", 1, 1, 5);
    }

    /**
     * 分页大小被限制在允许的最大行数内。
     */
    @Test
    void clampsPageSize() {
        AiProperties properties = new AiProperties();
        properties.getAgent().setMaxResultRows(5);
        QueryUsersTool limited = new QueryUsersTool(client, new ToolResultTrimmer(properties));
        when(client.queryUsers("tk-1", null, null, 1, 5)).thenReturn("{}");

        limited.execute(context(), Map.of("pageSize", 100));

        verify(client).queryUsers("tk-1", null, null, 1, 5);
    }

    /**
     * 缺少权限：直接拒绝，不调用下游。
     */
    @Test
    void deniesWithoutPermission() {
        ToolOutcome outcome = tool.execute(new AgentContext(1L, "tk-1", Set.of(), null, null,
                action -> { }), Map.of());

        assertThat(outcome.status()).isEqualTo(ToolOutcome.Status.DENIED);
        verifyNoInteractions(client);
    }

    private AgentContext context() {
        return new AgentContext(1L, "tk-1", Set.of("user:list"), null, null, action -> { });
    }
}
