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

package com.iboot.system.application.service;

import com.iboot.kernel.web.BusinessException;
import com.iboot.system.domain.system.model.LeaveQuotaTransfer;
import com.iboot.system.domain.system.model.LeaveQuotaTransferStatus;
import com.iboot.system.domain.system.repository.LeaveQuotaTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * 额度划转 TCC 应用服务。
 *
 * <p>编排 Try/Confirm/Cancel 三段的状态迁移与额度调整。每个动作都在
 * {@code tccTransactionManager} 管理的本地事务内执行；该事务不使用 Seata AT
 * 代理的数据源，因此不会产生 AT 分支。
 *
 * <p>幂等与悬挂由状态机与唯一键共同保证：Try 只接受一次冻结，Confirm/Cancel
 * 只从 {@code TRYING} 进入终态，Cancel 先到时会留下 {@code CANCELLED} 墓碑。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.system.infrastructure.config.TccDataSourceConfig
 */
@Service
public class LeaveQuotaTransferApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LeaveQuotaTransferApplicationService.class);

    private final LeaveQuotaTransferRepository leaveQuotaTransferRepository;

    private final TransactionTemplate tccTransactionTemplate;

    private final LeaveQuotaApplicationService leaveQuotaApplicationService;

    /**
     * 创建应用服务。
     *
     * @param leaveQuotaTransferRepository 划转仓储
     * @param tccTransactionTemplate       TCC 专用事务模板
     * @param leaveQuotaApplicationService 额度服务，用于解析默认额度
     */
    public LeaveQuotaTransferApplicationService(
            final LeaveQuotaTransferRepository leaveQuotaTransferRepository,
            @Qualifier("tccTransactionTemplate") final TransactionTemplate tccTransactionTemplate,
            final LeaveQuotaApplicationService leaveQuotaApplicationService) {
        this.leaveQuotaTransferRepository = leaveQuotaTransferRepository;
        this.tccTransactionTemplate = tccTransactionTemplate;
        this.leaveQuotaApplicationService = leaveQuotaApplicationService;
    }

    /**
     * 执行 Try：冻结划出方额度并登记划转。
     *
     * <p>同一 XID 或同一划转标识已存在时按幂等成功返回；存在取消墓碑时拒绝执行，
     * 避免已回滚的分支重新占用额度。
     *
     * @param xid        全局事务 ID
     * @param transferId 划转业务标识
     * @param fromUserId 划出方用户 ID
     * @param toUserId   划入方用户 ID
     * @param year       年度
     * @param count      划转次数
     * @return 是否执行成功
     * @throws BusinessException 额度不足或命中取消墓碑时抛出
     */
    public boolean tryTransfer(final String xid, final String transferId, final Long fromUserId,
                               final Long toUserId, final int year, final int count) {
        return Boolean.TRUE.equals(tccTransactionTemplate.execute(status ->
                doTry(xid, transferId, fromUserId, toUserId, year, count)));
    }

    /**
     * 执行 Confirm：把冻结额度结算为划转。
     *
     * <p>记录不存在或已是终态时按幂等成功处理，不重复调整额度。
     *
     * @param xid 全局事务 ID
     * @return 是否执行成功
     * @throws IllegalStateException 冻结次数与记录不一致时抛出，使分支重试
     */
    public boolean confirm(final String xid) {
        return Boolean.TRUE.equals(tccTransactionTemplate.execute(status -> doConfirm(xid)));
    }

    /**
     * 执行 Cancel：释放冻结额度；无记录时写入取消墓碑。
     *
     * <p>重复取消、确认后的迟到取消都按幂等成功处理。
     *
     * @param xid        全局事务 ID
     * @param transferId 划转业务标识
     * @param fromUserId 划出方用户 ID
     * @param toUserId   划入方用户 ID
     * @param year       年度
     * @param count      划转次数
     * @return 是否执行成功
     */
    public boolean cancel(final String xid, final String transferId, final Long fromUserId,
                          final Long toUserId, final int year, final int count) {
        return Boolean.TRUE.equals(tccTransactionTemplate.execute(status ->
                doCancel(xid, transferId, fromUserId, toUserId, year, count)));
    }

    private boolean doTry(final String xid, final String transferId, final Long fromUserId,
                          final Long toUserId, final int year, final int count) {
        Optional<LeaveQuotaTransfer> byXid = leaveQuotaTransferRepository.findByXid(xid);
        if (byXid.isPresent()) {
            return acceptExisting(byXid.get());
        }
        Optional<LeaveQuotaTransfer> byTransferId = leaveQuotaTransferRepository.findByTransferId(transferId);
        if (byTransferId.isPresent()) {
            return acceptExisting(byTransferId.get());
        }

        LeaveQuotaTransfer transfer = new LeaveQuotaTransfer(transferId, xid, fromUserId, toUserId,
                year, count, LeaveQuotaTransferStatus.TRYING);
        if (!leaveQuotaTransferRepository.insertTrying(transfer)) {
            // 并发重复 Try：重新读取后按幂等处理
            return leaveQuotaTransferRepository.findByXid(xid)
                    .or(() -> leaveQuotaTransferRepository.findByTransferId(transferId))
                    .map(this::acceptExisting)
                    .orElse(false);
        }
        int defaultCount = leaveQuotaApplicationService.defaultCount();
        leaveQuotaTransferRepository.ensureQuota(fromUserId, year, defaultCount);
        leaveQuotaTransferRepository.ensureQuota(toUserId, year, defaultCount);
        if (leaveQuotaTransferRepository.freeze(fromUserId, year, count) == 0) {
            throw new BusinessException(400, "请假额度不足");
        }
        log.info("TCC Try 完成：xid={}, transferId={}, from={}, to={}, year={}, count={}",
                xid, transferId, fromUserId, toUserId, year, count);
        return true;
    }

    private boolean acceptExisting(final LeaveQuotaTransfer transfer) {
        if (transfer.status() == LeaveQuotaTransferStatus.CANCELLED) {
            throw new BusinessException(409, "划转已取消");
        }
        return true;
    }

    private boolean doConfirm(final String xid) {
        Optional<LeaveQuotaTransfer> found = leaveQuotaTransferRepository.findByXid(xid);
        if (found.isEmpty()) {
            log.warn("TCC Confirm 未找到划转记录，按幂等成功处理：xid={}", xid);
            return true;
        }
        LeaveQuotaTransfer transfer = found.get();
        if (transfer.status() == LeaveQuotaTransferStatus.CONFIRMED) {
            return true;
        }
        if (transfer.status() == LeaveQuotaTransferStatus.CANCELLED) {
            log.warn("TCC Confirm 命中已取消的划转，忽略：xid={}", xid);
            return true;
        }
        if (leaveQuotaTransferRepository.settleFrozen(transfer.fromUserId(), transfer.year(), transfer.count()) == 0) {
            throw new IllegalStateException("额度划转确认失败，冻结次数不足：xid=" + xid);
        }
        leaveQuotaTransferRepository.ensureQuota(transfer.toUserId(), transfer.year(),
                leaveQuotaApplicationService.defaultCount());
        if (leaveQuotaTransferRepository.increaseTotal(transfer.toUserId(), transfer.year(),
                transfer.count()) == 0) {
            throw new IllegalStateException("额度划转确认失败，划入方额度行不存在：xid=" + xid);
        }
        leaveQuotaTransferRepository.markTerminal(xid, LeaveQuotaTransferStatus.CONFIRMED);
        log.info("TCC Confirm 完成：xid={}, transferId={}, from={}, to={}, count={}",
                xid, transfer.transferId(), transfer.fromUserId(), transfer.toUserId(), transfer.count());
        return true;
    }

    private boolean doCancel(final String xid, final String transferId, final Long fromUserId,
                             final Long toUserId, final int year, final int count) {
        Optional<LeaveQuotaTransfer> found = leaveQuotaTransferRepository.findByXid(xid);
        if (found.isEmpty()) {
            LeaveQuotaTransfer tombstone = new LeaveQuotaTransfer(transferId, xid, fromUserId, toUserId,
                    year, count, LeaveQuotaTransferStatus.CANCELLED);
            leaveQuotaTransferRepository.insertTombstone(tombstone);
            log.info("TCC Cancel 空回滚，写入取消墓碑：xid={}, transferId={}", xid, transferId);
            return true;
        }
        LeaveQuotaTransfer transfer = found.get();
        if (transfer.status() == LeaveQuotaTransferStatus.CANCELLED) {
            return true;
        }
        if (transfer.status() == LeaveQuotaTransferStatus.CONFIRMED) {
            log.warn("TCC Cancel 命中已确认的划转，忽略：xid={}", xid);
            return true;
        }
        if (leaveQuotaTransferRepository.releaseFrozen(transfer.fromUserId(), transfer.year(),
                transfer.count()) == 0) {
            log.warn("TCC Cancel 释放冻结未命中，可能已被处理：xid={}", xid);
        }
        leaveQuotaTransferRepository.markTerminal(xid, LeaveQuotaTransferStatus.CANCELLED);
        log.info("TCC Cancel 完成：xid={}, transferId={}, from={}, count={}",
                xid, transfer.transferId(), transfer.fromUserId(), transfer.count());
        return true;
    }
}
