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
import com.iboot.ai.infrastructure.tool.AdminApiClient;
import com.iboot.ai.infrastructure.tool.AdminToolSupport;
import com.iboot.ai.infrastructure.tool.ToolResultTrimmer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 查询备份概览只读工具。
 *
 * <p>返回备份功能开关、总记录数、总占用与最近一次成功时间；所需权限为 {@code backup:record:list}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class QueryBackupSummaryTool extends AdminToolSupport {

    /**
     * 工具名。
     */
    public static final String NAME = "query_backup_summary";

    private static final String PERMISSION = "backup:record:list";

    /**
     * 创建工具。
     *
     * @param client  下游客户端
     * @param trimmer 结果裁剪器
     */
    public QueryBackupSummaryTool(AdminApiClient client, ToolResultTrimmer trimmer) {
        super(client, trimmer);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "查询数据备份概览：功能是否开启、备份记录总数、总占用空间与最近一次成功备份时间。";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return objectSchema(Map.of(), List.of());
    }

    @Override
    public boolean readOnly() {
        return true;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public ToolOutcome execute(AgentContext context, Map<String, Object> arguments) {
        return readOnlyCall(context, PERMISSION, () -> client.backupSummary(context.bearerToken()));
    }
}
