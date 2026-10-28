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

/**
 * 确认操作的结果。
 *
 * @param runId   运行 ID
 * @param status  运行终态
 * @param content 工具执行结果摘要，拒绝时为说明文本
 * @author iBoot Team
 * @since 1.0.0
 */
public record AgentConfirmResult(long runId, String status, String content) {
}
