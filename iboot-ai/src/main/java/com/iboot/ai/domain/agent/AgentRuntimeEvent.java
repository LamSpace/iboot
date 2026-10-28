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

package com.iboot.ai.domain.agent;

/**
 * 运行时的标准化事件。
 *
 * <p>把具体编排框架的事件收敛为应用层可消费的类型，使运行服务、审计与回测不依赖框架类型。
 *
 * @param type         事件类型
 * @param text         文本增量或结果文本
 * @param toolName     工具名
 * @param toolState    工具结果状态
 * @param inputTokens  本次模型调用的输入 token
 * @param outputTokens 本次模型调用的输出 token
 * @param errorCode    失败原因码
 * @author iBoot Team
 * @since 1.0.0
 */
public record AgentRuntimeEvent(Type type,
                                String text,
                                String toolName,
                                String toolState,
                                int inputTokens,
                                int outputTokens,
                                String errorCode) {

    /**
     * 事件类型。
     */
    public enum Type {

        /** 模型调用开始，用于调用前检查配额。 */
        MODEL_START,

        /** 文本增量。 */
        TEXT,

        /** 模型请求调用工具。 */
        TOOL_CALL,

        /** 工具执行结束。 */
        TOOL_RESULT,

        /** 一次模型调用的用量。 */
        USAGE,

        /** 步数达到上限。 */
        STEP_LIMIT,

        /** 运行正常结束。 */
        COMPLETED,

        /** 运行失败。 */
        ERROR
    }

    /**
     * 构造模型调用开始事件。
     *
     * @return 事件
     */
    public static AgentRuntimeEvent modelStart() {
        return new AgentRuntimeEvent(Type.MODEL_START, null, null, null, 0, 0, null);
    }

    /**
     * 构造文本增量事件。
     *
     * @param delta 文本增量
     * @return 事件
     */
    public static AgentRuntimeEvent text(String delta) {
        return new AgentRuntimeEvent(Type.TEXT, delta, null, null, 0, 0, null);
    }

    /**
     * 构造工具调用事件。
     *
     * @param toolName 工具名
     * @return 事件
     */
    public static AgentRuntimeEvent toolCall(String toolName) {
        return new AgentRuntimeEvent(Type.TOOL_CALL, null, toolName, null, 0, 0, null);
    }

    /**
     * 构造工具结果事件。
     *
     * @param toolName 工具名
     * @param state    结果状态
     * @return 事件
     */
    public static AgentRuntimeEvent toolResult(String toolName, String state) {
        return new AgentRuntimeEvent(Type.TOOL_RESULT, null, toolName, state, 0, 0, null);
    }

    /**
     * 构造用量事件。
     *
     * @param inputTokens  输入 token
     * @param outputTokens 输出 token
     * @return 事件
     */
    public static AgentRuntimeEvent usage(int inputTokens, int outputTokens) {
        return new AgentRuntimeEvent(Type.USAGE, null, null, null, inputTokens, outputTokens, null);
    }

    /**
     * 构造步数超限事件。
     *
     * @return 事件
     */
    public static AgentRuntimeEvent stepLimit() {
        return new AgentRuntimeEvent(Type.STEP_LIMIT, null, null, null, 0, 0, "step_limit");
    }

    /**
     * 构造完成事件。
     *
     * @return 事件
     */
    public static AgentRuntimeEvent completed() {
        return new AgentRuntimeEvent(Type.COMPLETED, null, null, null, 0, 0, null);
    }

    /**
     * 构造失败事件。
     *
     * @param errorCode 原因码
     * @return 事件
     */
    public static AgentRuntimeEvent error(String errorCode) {
        return new AgentRuntimeEvent(Type.ERROR, null, null, null, 0, 0, errorCode);
    }
}
