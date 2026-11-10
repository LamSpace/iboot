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

package com.iboot.system.domain.system.repository;

import com.iboot.system.domain.system.model.LeaveQuotaTransfer;
import com.iboot.system.domain.system.model.LeaveQuotaTransferStatus;

import java.util.Optional;

/**
 * 额度划转 TCC 仓储。
 *
 * <p>所有读写都必须由调用方在 TCC 专用事务内执行，不得经过 Seata AT 代理的数据源。
 * 冻结与确认使用条件 SQL 并返回影响行数，调用方据此判断额度是否足够或数据是否异常。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface LeaveQuotaTransferRepository {

    /**
     * 按全局事务 ID 查询划转记录。
     *
     * @param xid 全局事务 ID
     * @return 划转记录；不存在时为空
     */
    Optional<LeaveQuotaTransfer> findByXid(String xid);

    /**
     * 按业务标识查询划转记录。
     *
     * @param transferId 划转业务标识
     * @return 划转记录；不存在时为空
     */
    Optional<LeaveQuotaTransfer> findByTransferId(String transferId);

    /**
     * 插入一条 {@code TRYING} 记录。
     *
     * @param transfer 划转记录
     * @return 是否插入成功；业务标识或 XID 已存在时返回 {@code false}
     */
    boolean insertTrying(LeaveQuotaTransfer transfer);

    /**
     * 插入一条 {@code CANCELLED} 墓碑记录。
     *
     * @param transfer 划转记录，状态应为 {@code CANCELLED}
     * @return 是否插入成功；业务标识或 XID 已存在时返回 {@code false}
     */
    boolean insertTombstone(LeaveQuotaTransfer transfer);

    /**
     * 将 {@code TRYING} 记录迁移到终态。
     *
     * @param xid    全局事务 ID
     * @param target 目标状态，只能是 {@code CONFIRMED} 或 {@code CANCELLED}
     * @return 受影响行数；记录不存在或已是终态时为 0
     */
    int markTerminal(String xid, LeaveQuotaTransferStatus target);

    /**
     * 确保额度行存在。
     *
     * @param userId     用户 ID
     * @param year       年度
     * @param totalCount 首次创建时使用的总额度
     */
    void ensureQuota(Long userId, int year, int totalCount);

    /**
     * 冻结额度。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @param count  冻结次数
     * @return 受影响行数；剩余额度不足时为 0
     */
    int freeze(Long userId, int year, int count);

    /**
     * 释放冻结额度。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @param count  释放次数
     * @return 受影响行数；冻结次数不足时为 0
     */
    int releaseFrozen(Long userId, int year, int count);

    /**
     * 确认划出：同时减少冻结与总额度。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @param count  确认次数
     * @return 受影响行数；冻结次数或总额度不足时为 0
     */
    int settleFrozen(Long userId, int year, int count);

    /**
     * 增加划入方总额度。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @param count  增加次数
     * @return 受影响行数
     */
    int increaseTotal(Long userId, int year, int count);
}
