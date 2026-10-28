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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 面向接口层的运行事件。
 *
 * <p>控制器按 {@code name} 映射为 SSE 事件名，{@code data} 直接作为事件数据序列化。
 *
 * @param name 事件名
 * @param data 事件数据
 * @author iBoot Team
 * @since 1.0.0
 */
public record AgentRunEvent(String name, Map<String, Object> data) {

    /**
     * 开始事件。
     *
     * @param runId          运行 ID
     * @param conversationId 会话 ID
     * @param agentId        智能体 ID
     * @return 事件
     */
    public static AgentRunEvent meta(long runId, String conversationId, long agentId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("runId", runId);
        data.put("conversationId", conversationId);
        data.put("agentId", agentId);
        return new AgentRunEvent("meta", data);
    }

    /**
     * 文本增量事件。
     *
     * @param delta 增量文本
     * @return 事件
     */
    public static AgentRunEvent token(String delta) {
        return new AgentRunEvent("token", Map.of("delta", delta));
    }

    /**
     * 工具调用事件。
     *
     * @param seq  步骤序号
     * @param tool 工具名
     * @return 事件
     */
    public static AgentRunEvent step(int seq, String tool) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seq", seq);
        data.put("tool", tool);
        return new AgentRunEvent("step", data);
    }

    /**
     * 工具结果事件。
     *
     * @param seq   步骤序号
     * @param tool  工具名
     * @param state 结果状态
     * @return 事件
     */
    public static AgentRunEvent toolResult(int seq, String tool, String state) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("seq", seq);
        data.put("tool", tool);
        data.put("state", state);
        return new AgentRunEvent("tool_result", data);
    }

    /**
     * 待确认事件。
     *
     * @param runId     运行 ID
     * @param tool      工具名
     * @param argsDigest 参数摘要
     * @return 事件
     */
    public static AgentRunEvent pendingApproval(long runId, String tool, String argsDigest) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("runId", runId);
        data.put("tool", tool);
        data.put("argsDigest", argsDigest);
        return new AgentRunEvent("pending_approval", data);
    }

    /**
     * 结束事件。
     *
     * @param status       运行状态
     * @param inputTokens  输入 token
     * @param outputTokens 输出 token
     * @return 事件
     */
    public static AgentRunEvent done(String status, long inputTokens, long outputTokens) {
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("promptTokens", inputTokens);
        usage.put("completionTokens", outputTokens);
        usage.put("totalTokens", inputTokens + outputTokens);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("usage", usage);
        return new AgentRunEvent("done", data);
    }

    /**
     * 错误事件。
     *
     * @param code      原因码
     * @param message   说明
     * @param retryable 是否可重试
     * @return 事件
     */
    public static AgentRunEvent error(String code, String message, boolean retryable) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("message", message);
        data.put("retryable", retryable);
        return new AgentRunEvent("error", data);
    }
}
