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

import java.util.Set;

/**
 * 一次智能体运行的身份与授权上下文。
 *
 * <p>由请求线程在入口处构造，随后显式传入运行时并挂到 {@code RuntimeContext}，
 * 工具从调用参数中取回。用户标识、令牌与权限只能来自当前登录态，不得采用模型输出中的任何身份字段。
 *
 * @param userId      发起用户 ID
 * @param bearerToken 当前登录令牌，用于代理调用下游 admin
 * @param permissions 当前会话持有的权限码
 * @param runId       运行标识；首段运行中途可能尚未落库，此时为 {@code null}
 * @param grant       一次性写操作授权；无授权时为 {@code null}
 * @param pendingSink 待确认动作收集器，写工具在缺少授权时向此提交
 * @author iBoot Team
 * @since 1.0.0
 */
public record AgentContext(Long userId,
                           String bearerToken,
                           Set<String> permissions,
                           Long runId,
                           ApprovalGrant grant,
                           PendingActionSink pendingSink) {

    /**
     * 创建不带授权的首段上下文。
     *
     * @param userId      用户 ID
     * @param bearerToken 登录令牌
     * @param permissions 权限码集合
     * @param pendingSink 待确认动作收集器
     * @return 运行上下文
     */
    public static AgentContext start(Long userId, String bearerToken, Set<String> permissions,
                                     PendingActionSink pendingSink) {
        return new AgentContext(userId, bearerToken, permissions, null, null, pendingSink);
    }

    /**
     * 判断当前用户是否持有指定权限。
     *
     * @param permission 权限码
     * @return 持有返回 {@code true}
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
