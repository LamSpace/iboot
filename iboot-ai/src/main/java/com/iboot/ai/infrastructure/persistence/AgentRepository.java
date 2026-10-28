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

package com.iboot.ai.infrastructure.persistence;

import com.iboot.ai.domain.agent.AgentDefinition;
import com.iboot.ai.domain.agent.AgentRunRecord;
import com.iboot.ai.domain.agent.AgentRunStatus;
import com.iboot.ai.domain.agent.AgentSummary;
import com.iboot.ai.domain.agent.PendingAction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 智能体运行与定义仓储。
 *
 * <p>使用 Spring JDBC 访问 {@code iboot_ai} 下的 {@code ai_agent} 与 {@code ai_agent_run}。
 * 运行状态迁移通过带条件的 UPDATE 完成，避免并发确认时重复执行写操作。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
@ConditionalOnProperty(prefix = "iboot.ai.agent", name = "enabled", havingValue = "true")
public class AgentRepository {

    private static final String RUN_COLUMNS = "id, conversation_id, agent_id, user_id, status, pending_tool, "
            + "pending_args_json, pending_args_digest, pending_expires_at, step_count, steps_json, input_tokens, "
            + "output_tokens, error_code, create_time, finished_at";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final JdbcTemplate jdbc;

    /**
     * 创建仓储。
     *
     * @param jdbc JDBC 模板
     */
    public AgentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 查询启用状态的智能体定义。
     *
     * @param id 智能体 ID
     * @return 定义，未启用或不存在时为空
     */
    public Optional<AgentDefinition> findEnabledAgent(long id) {
        List<AgentDefinition> found = jdbc.query("""
                SELECT id, name, description, system_prompt, model, tool_allowlist_json
                FROM ai_agent WHERE id = ? AND status = 1 AND deleted = 0
                """, (rs, rowNum) -> new AgentDefinition(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("system_prompt"),
                rs.getString("model"),
                parseAllowlist(rs.getString("tool_allowlist_json"))), id);
        return found.stream().findFirst();
    }

    /**
     * 新建一次运行。
     *
     * @param agentId        智能体 ID
     * @param userId         发起用户 ID
     * @param conversationId 会话 ID
     * @return 运行 ID
     */
    public long insertRun(long agentId, long userId, String conversationId) {
        jdbc.update("""
                INSERT INTO ai_agent_run (conversation_id, agent_id, user_id, status)
                VALUES (?, ?, ?, 'RUNNING')
                """, conversationId, agentId, userId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0L : id;
    }

    /**
     * 查询运行记录。
     *
     * @param id 运行 ID
     * @return 运行记录
     */
    public Optional<AgentRunRecord> findRun(long id) {
        List<AgentRunRecord> found = jdbc.query("SELECT " + RUN_COLUMNS + " FROM ai_agent_run WHERE id = ?",
                RUN_MAPPER, id);
        return found.stream().findFirst();
    }

    /**
     * 更新运行进度。
     *
     * @param id          运行 ID
     * @param stepCount   步数
     * @param stepsJson   轨迹 JSON
     * @param inputTokens 输入 token
     * @param outputTokens 输出 token
     */
    public void updateProgress(long id, int stepCount, String stepsJson, long inputTokens, long outputTokens) {
        jdbc.update("""
                UPDATE ai_agent_run SET step_count = ?, steps_json = ?, input_tokens = ?, output_tokens = ?
                WHERE id = ?
                """, stepCount, stepsJson, inputTokens, outputTokens, id);
    }

    /**
     * 把运行置为待确认。
     *
     * @param id              运行 ID
     * @param action          待确认动作
     * @param expiresAt       过期时间
     * @param stepCount       步数
     * @param stepsJson       轨迹 JSON
     * @param inputTokens     输入 token
     * @param outputTokens    输出 token
     */
    public void markPending(long id, PendingAction action, LocalDateTime expiresAt, int stepCount,
                            String stepsJson, long inputTokens, long outputTokens) {
        jdbc.update("""
                UPDATE ai_agent_run SET status = 'PENDING_APPROVAL', pending_tool = ?, pending_args_json = ?,
                       pending_args_digest = ?, pending_expires_at = ?, step_count = ?, steps_json = ?,
                       input_tokens = ?, output_tokens = ?
                WHERE id = ? AND status = 'RUNNING'
                """, action.toolName(), action.argumentsJson(), action.argsDigest(), Timestamp.valueOf(expiresAt),
                stepCount, stepsJson, inputTokens, outputTokens, id);
    }

    /**
     * 原子认领待确认运行，置为执行中。
     *
     * @param id     运行 ID
     * @param userId 发起用户 ID
     * @return 影响行数；0 表示状态或归属不满足
     */
    public int claimForApproval(long id, long userId) {
        return jdbc.update("""
                UPDATE ai_agent_run SET status = 'EXECUTING', approval_granted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND status = 'PENDING_APPROVAL'
                """, id, userId);
    }

    /**
     * 结束运行并清理待确认字段。
     *
     * @param id           运行 ID
     * @param status       终态
     * @param stepsJson    轨迹 JSON
     * @param stepCount    步数
     * @param inputTokens  输入 token
     * @param outputTokens 输出 token
     * @param errorCode    失败原因码，可为 {@code null}
     */
    public void markFinished(long id, AgentRunStatus status, String stepsJson, int stepCount,
                             long inputTokens, long outputTokens, String errorCode) {
        jdbc.update("""
                UPDATE ai_agent_run SET status = ?, steps_json = ?, step_count = ?, input_tokens = ?,
                       output_tokens = ?, error_code = ?, finished_at = CURRENT_TIMESTAMP,
                       pending_tool = NULL, pending_args_json = NULL, pending_args_digest = NULL,
                       pending_expires_at = NULL
                WHERE id = ?
                """, status.name(), stepsJson, stepCount, inputTokens, outputTokens, errorCode, id);
    }

    private List<String> parseAllowlist(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        JsonNode node = MAPPER.readTree(json);
        List<String> names = new ArrayList<>();
        node.forEach(item -> names.add(item.asString()));
        return names;
    }


    /**
     * 查询智能体列表。
     *
     * @param onlyEnabled 是否只返回启用的智能体
     * @return 配置视图列表
     */
    public List<AgentSummary> listAgents(boolean onlyEnabled) {
        String sql = "SELECT id, name, description, system_prompt, model, tool_allowlist_json, status "
                + "FROM ai_agent WHERE deleted = 0"
                + (onlyEnabled ? " AND status = 1" : "")
                + " ORDER BY id";
        return jdbc.query(sql, (rs, rowNum) -> new AgentSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("system_prompt"),
                rs.getString("model"),
                parseAllowlist(rs.getString("tool_allowlist_json")),
                rs.getInt("status") == 1));
    }

    /**
     * 查询单个智能体配置。
     *
     * @param id 智能体 ID
     * @return 配置视图
     */
    public Optional<AgentSummary> findAgentSummary(long id) {
        List<AgentSummary> found = jdbc.query("""
                SELECT id, name, description, system_prompt, model, tool_allowlist_json, status
                FROM ai_agent WHERE id = ? AND deleted = 0
                """, (rs, rowNum) -> new AgentSummary(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("system_prompt"),
                rs.getString("model"),
                parseAllowlist(rs.getString("tool_allowlist_json")),
                rs.getInt("status") == 1), id);
        return found.stream().findFirst();
    }

    /**
     * 新建智能体。
     *
     * @param name          名称
     * @param description   描述
     * @param systemPrompt  系统提示词
     * @param model         模型标识，可为 {@code null}
     * @param toolAllowlist 工具白名单
     * @param enabled       是否启用
     * @param createdBy     创建人
     * @return 新记录 ID
     */
    public long insertAgent(String name, String description, String systemPrompt, String model,
                            List<String> toolAllowlist, boolean enabled, String createdBy) {
        jdbc.update("""
                INSERT INTO ai_agent (name, description, system_prompt, model, tool_allowlist_json, status,
                                      created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, name, description, systemPrompt, model, MAPPER.writeValueAsString(toolAllowlist),
                enabled ? 1 : 0, createdBy);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0L : id;
    }

    /**
     * 更新智能体。
     *
     * @param id            智能体 ID
     * @param name          名称
     * @param description   描述
     * @param systemPrompt  系统提示词
     * @param model         模型标识
     * @param toolAllowlist 工具白名单
     * @param enabled       是否启用
     * @return 影响行数
     */
    public int updateAgent(long id, String name, String description, String systemPrompt, String model,
                           List<String> toolAllowlist, boolean enabled) {
        return jdbc.update("""
                UPDATE ai_agent SET name = ?, description = ?, system_prompt = ?, model = ?,
                       tool_allowlist_json = ?, status = ?
                WHERE id = ? AND deleted = 0
                """, name, description, systemPrompt, model, MAPPER.writeValueAsString(toolAllowlist),
                enabled ? 1 : 0, id);
    }

    /**
     * 逻辑删除智能体。
     *
     * @param id 智能体 ID
     * @return 影响行数
     */
    public int deleteAgent(long id) {
        return jdbc.update("UPDATE ai_agent SET deleted = id WHERE id = ? AND deleted = 0", id);
    }

    private static final RowMapper<AgentRunRecord> RUN_MAPPER = (ResultSet rs, int rowNum) -> {
        try {
            return new AgentRunRecord(
                    rs.getLong("id"),
                    rs.getString("conversation_id"),
                    rs.getLong("agent_id"),
                    rs.getLong("user_id"),
                    AgentRunStatus.valueOf(rs.getString("status")),
                    rs.getString("pending_tool"),
                    rs.getString("pending_args_json"),
                    rs.getString("pending_args_digest"),
                    toLocalDateTime(rs.getTimestamp("pending_expires_at")),
                    rs.getInt("step_count"),
                    rs.getString("steps_json"),
                    rs.getLong("input_tokens"),
                    rs.getLong("output_tokens"),
                    rs.getString("error_code"),
                    toLocalDateTime(rs.getTimestamp("create_time")),
                    toLocalDateTime(rs.getTimestamp("finished_at")));
        } catch (SQLException e) {
            throw new IllegalStateException("读取运行记录失败", e);
        }
    };

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
