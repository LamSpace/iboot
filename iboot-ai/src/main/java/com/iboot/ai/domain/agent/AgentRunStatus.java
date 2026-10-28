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
 * 智能体运行状态。
 *
 * <p>合法迁移：RUNNING → PENDING_APPROVAL → EXECUTING → COMPLETED / FAILED；
 * RUNNING 可直接进入 COMPLETED / FAILED / REJECTED；PENDING_APPROVAL 可进入 REJECTED / EXPIRED。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum AgentRunStatus {

    /** 正在执行。 */
    RUNNING,

    /** 写操作等待确认。 */
    PENDING_APPROVAL,

    /** 已确认，正在执行写操作。 */
    EXECUTING,

    /** 正常完成。 */
    COMPLETED,

    /** 用户拒绝。 */
    REJECTED,

    /** 确认超时。 */
    EXPIRED,

    /** 执行失败。 */
    FAILED
}
