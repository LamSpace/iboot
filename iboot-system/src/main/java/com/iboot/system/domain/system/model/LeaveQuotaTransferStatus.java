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

package com.iboot.system.domain.system.model;

/**
 * 额度划转状态。
 *
 * <p>TCC 三段操作对应三种状态：Try 成功后为 {@link #TRYING}，Confirm 成功后为
 * {@link #CONFIRMED}，Cancel 成功后为 {@link #CANCELLED}。只有 {@code TRYING}
 * 可以进入终态，终态之间不可互相迁移。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum LeaveQuotaTransferStatus {

    /**
     * 已冻结，等待确认或取消。
     */
    TRYING,

    /**
     * 已确认，额度完成划转。
     */
    CONFIRMED,

    /**
     * 已取消；也可能是空回滚留下的墓碑。
     */
    CANCELLED
}
