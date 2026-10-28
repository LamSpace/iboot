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
 * 等待用户确认的写操作。
 *
 * <p>写工具在缺少授权时生成该对象，运行服务据此把运行置为待确认状态，
 * 并在确认后按这里的原始参数执行，避免重新解析模型输出。
 *
 * @param toolName      工具名
 * @param argumentsJson 原始参数 JSON
 * @param argsDigest    参数 SHA-256 摘要
 * @author iBoot Team
 * @since 1.0.0
 */
public record PendingAction(String toolName, String argumentsJson, String argsDigest) {
}
