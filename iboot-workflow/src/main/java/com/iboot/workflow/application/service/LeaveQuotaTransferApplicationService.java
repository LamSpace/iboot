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

package com.iboot.workflow.application.service;

import com.iboot.kernel.web.BusinessException;
import com.iboot.workflow.application.service.quota.LeaveQuotaTransferPort;
import com.iboot.workflow.infrastructure.directory.WorkflowUser;
import com.iboot.workflow.infrastructure.directory.WorkflowUserReader;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 额度划转应用服务。
 *
 * <p>本服务只做参数校验、目标用户校验与全局事务编排，不写本地库；全局事务中只有
 * 额度服务的 TCC 分支。划转标识在这里生成，作为下游的幂等键。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see LeaveQuotaTransferPort
 */
@Service
public class LeaveQuotaTransferApplicationService {

    private static final int MIN_YEAR = 2000;

    private static final int MAX_YEAR = 2100;

    private final LeaveQuotaTransferPort leaveQuotaTransferPort;

    private final WorkflowUserReader userReader;

    /**
     * 创建应用服务。
     *
     * @param leaveQuotaTransferPort 额度划转端口
     * @param userReader             系统用户只读适配器
     */
    public LeaveQuotaTransferApplicationService(final LeaveQuotaTransferPort leaveQuotaTransferPort,
                                                final WorkflowUserReader userReader) {
        this.leaveQuotaTransferPort = leaveQuotaTransferPort;
        this.userReader = userReader;
    }

    /**
     * 发起一次额度划转。
     *
     * <p>在全局事务内调用额度服务的 Try；Try 失败或后续步骤失败时由 Seata 驱动
     * Cancel 释放冻结额度。本方法不产生本地写入。
     *
     * @param fromUserId 划出方用户 ID
     * @param toUserId   划入方用户 ID
     * @param year       年度
     * @param count      划转次数
     * @return 划转业务标识
     * @throws BusinessException 参数非法、划入方不存在或停用、额度不足、下游不可用时抛出
     */
    @GlobalTransactional(name = "transfer-leave-quota", rollbackFor = Exception.class)
    public String transfer(final Long fromUserId, final Long toUserId, final int year, final int count) {
        validate(fromUserId, toUserId, year, count);
        String transferId = UUID.randomUUID().toString();
        leaveQuotaTransferPort.transfer(transferId, fromUserId, toUserId, year, count);
        return transferId;
    }

    private void validate(final Long fromUserId, final Long toUserId, final int year, final int count) {
        if (fromUserId == null || toUserId == null) {
            throw new BusinessException(400, "划出方与划入方不能为空");
        }
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(400, "划出方与划入方不能相同");
        }
        if (count <= 0) {
            throw new BusinessException(400, "划转次数必须大于 0");
        }
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new BusinessException(400, "年度不合法");
        }
        WorkflowUser target = userReader.findById(toUserId)
                .orElseThrow(() -> new BusinessException(400, "划入方不存在"));
        if (!Integer.valueOf(1).equals(target.status())) {
            throw new BusinessException(400, "划入方已停用");
        }
    }
}
