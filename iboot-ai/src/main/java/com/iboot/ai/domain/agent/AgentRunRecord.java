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

import java.time.LocalDateTime;

/**
 * 运行记录快照。
 *
 * <p>用于状态机判断与确认校验；步骤轨迹只保存摘要，不保存工具返回的原始内容。
 *
 * @param id                运行 ID
 * @param conversationId    会话 ID
 * @param agentId           智能体 ID
 * @param userId            发起用户 ID
 * @param status            当前状态
 * @param pendingTool       待确认工具名
 * @param pendingArgsJson   待确认参数 JSON
 * @param pendingArgsDigest 待确认参数摘要
 * @param pendingExpiresAt  待确认过期时间
 * @param stepCount         模型调用步数
 * @param stepsJson         步骤轨迹 JSON
 * @param inputTokens       输入 token 累计
 * @param outputTokens      输出 token 累计
 * @param errorCode         失败原因码
 * @param createTime        创建时间
 * @param finishedAt        结束时间
 * @author iBoot Team
 * @since 1.0.0
 */
public record AgentRunRecord(Long id,
                             String conversationId,
                             Long agentId,
                             Long userId,
                             AgentRunStatus status,
                             String pendingTool,
                             String pendingArgsJson,
                             String pendingArgsDigest,
                             LocalDateTime pendingExpiresAt,
                             Integer stepCount,
                             String stepsJson,
                             Long inputTokens,
                             Long outputTokens,
                             String errorCode,
                             LocalDateTime createTime,
                             LocalDateTime finishedAt) {
}
