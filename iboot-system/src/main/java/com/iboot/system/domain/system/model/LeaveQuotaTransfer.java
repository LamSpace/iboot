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
 * 一次额度划转的 TCC 记录。
 *
 * <p>记录划出方、划入方、年度、次数与状态，并以 {@code transferId} 与 {@code xid}
 * 作为幂等键。Confirm 与 Cancel 只按 XID 定位记录，避免依赖参数传递。
 *
 * @param transferId 划转业务标识，幂等键
 * @param xid        全局事务 ID
 * @param fromUserId 划出方用户 ID
 * @param toUserId   划入方用户 ID
 * @param year       年度
 * @param count      划转次数
 * @param status     当前状态
 * @author iBoot Team
 * @since 1.0.0
 */
public record LeaveQuotaTransfer(String transferId, String xid, Long fromUserId, Long toUserId,
                                 int year, int count, LeaveQuotaTransferStatus status) {
}
