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
import com.iboot.ai.domain.agent.PendingAction;
import com.iboot.ai.domain.agent.ToolOutcome;
import com.iboot.ai.infrastructure.tool.AdminApiClient;
import com.iboot.ai.infrastructure.tool.AdminApiException;
import com.iboot.ai.infrastructure.tool.AdminToolSupport;
import com.iboot.ai.infrastructure.tool.ToolArguments;
import com.iboot.ai.infrastructure.tool.ToolResultTrimmer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 触发备份的可写工具。
 *
 * <p>没有一次性授权时只登记待确认动作并返回待确认结果，绝不发起下游请求；
 * 只有确认流程签发的、与工具名和参数摘要完全匹配的授权才能放行。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class TriggerBackupTool extends AdminToolSupport {

    /**
     * 工具名。
     */
    public static final String NAME = "trigger_backup";

    private static final String PERMISSION = "backup:record:create";

    /**
     * 创建工具。
     *
     * @param client  下游客户端
     * @param trimmer 结果裁剪器
     */
    public TriggerBackupTool(AdminApiClient client, ToolResultTrimmer trimmer) {
        super(client, trimmer);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "立即触发一次全量数据备份。该操作会写入备份产物，必须先获得用户确认。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return objectSchema(Map.of(), List.of());
    }

    @Override
    public boolean readOnly() {
        return false;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public ToolOutcome execute(AgentContext context, Map<String, Object> arguments) {
        if (!context.hasPermission(PERMISSION)) {
            return ToolOutcome.denied("当前用户没有执行该操作所需的权限：" + PERMISSION);
        }
        Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
        String digest = ToolArguments.digest(safeArguments);
        if (context.grant() == null || !context.grant().matches(NAME, digest)) {
            if (context.pendingSink() != null) {
                context.pendingSink().submit(new PendingAction(NAME,
                        ToolArguments.canonicalJson(safeArguments), digest));
            }
            return ToolOutcome.pending("已准备触发备份，等待用户确认；确认前不会产生任何数据变更。");
        }
        try {
            return ToolOutcome.ok(trimmer.trim(client.triggerBackup(context.bearerToken())));
        } catch (AdminApiException e) {
            return ToolOutcome.failed(e.getMessage());
        }
    }
}
