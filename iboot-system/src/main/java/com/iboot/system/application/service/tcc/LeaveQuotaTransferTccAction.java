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

package com.iboot.system.application.service.tcc;

import com.iboot.kernel.web.BusinessException;
import com.iboot.system.application.service.LeaveQuotaTransferApplicationService;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.LocalTCC;
import org.apache.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.core.context.RootContext;
import org.springframework.stereotype.Service;

/**
 * 额度划转 TCC 动作。
 *
 * <p>Try 由审批服务经内部 HTTP 端点调用，Confirm/Cancel 由 Seata 协调器回调本 Bean。
 * 约定：Try 参数全部通过 {@link BusinessActionContextParameter} 登记，Confirm/Cancel
 * 只依赖 {@link BusinessActionContext}，不依赖 HTTP 请求。
 *
 * <p>Try 要求存在全局事务 XID；缺失时失败关闭，避免在普通本地事务里冻结额度。
 *
 * <p>{@link LocalTCC} 是 Seata 识别 TCC 资源的标记：没有它，Try 不会登记 TCC 分支，
 * 全局事务会以零分支提交。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@LocalTCC
@Service
public class LeaveQuotaTransferTccAction {

    /**
     * TCC 动作名，协调器据此路由 Confirm/Cancel。
     */
    public static final String ACTION_NAME = "leaveQuotaTransfer";

    private final LeaveQuotaTransferApplicationService leaveQuotaTransferApplicationService;

    /**
     * 创建 TCC 动作。
     *
     * @param leaveQuotaTransferApplicationService 划转应用服务
     */
    public LeaveQuotaTransferTccAction(
            final LeaveQuotaTransferApplicationService leaveQuotaTransferApplicationService) {
        this.leaveQuotaTransferApplicationService = leaveQuotaTransferApplicationService;
    }

    /**
     * 执行 Try。
     *
     * @param transferId 划转业务标识
     * @param fromUserId 划出方用户 ID
     * @param toUserId   划入方用户 ID
     * @param year       年度
     * @param count      划转次数
     * @return 是否执行成功
     * @throws BusinessException 缺少全局事务或额度不足时抛出
     */
    @TwoPhaseBusinessAction(name = ACTION_NAME, commitMethod = "confirm", rollbackMethod = "cancel")
    public boolean tryTransfer(
            @BusinessActionContextParameter(paramName = "transferId") final String transferId,
            @BusinessActionContextParameter(paramName = "fromUserId") final Long fromUserId,
            @BusinessActionContextParameter(paramName = "toUserId") final Long toUserId,
            @BusinessActionContextParameter(paramName = "year") final int year,
            @BusinessActionContextParameter(paramName = "count") final int count) {
        String xid = RootContext.getXID();
        if (xid == null || xid.isBlank()) {
            throw new BusinessException(503, "缺少全局事务标识，额度划转未执行");
        }
        return leaveQuotaTransferApplicationService.tryTransfer(xid, transferId, fromUserId,
                toUserId, year, count);
    }

    /**
     * 执行 Confirm。
     *
     * @param context 分支事务上下文
     * @return 是否执行成功
     */
    public boolean confirm(final BusinessActionContext context) {
        return leaveQuotaTransferApplicationService.confirm(context.getXid());
    }

    /**
     * 执行 Cancel。
     *
     * @param context 分支事务上下文
     * @return 是否执行成功
     */
    public boolean cancel(final BusinessActionContext context) {
        return leaveQuotaTransferApplicationService.cancel(
                context.getXid(),
                stringValue(context.getActionContext("transferId")),
                longValue(context.getActionContext("fromUserId")),
                longValue(context.getActionContext("toUserId")),
                intValue(context.getActionContext("year")),
                intValue(context.getActionContext("count")));
    }

    private String stringValue(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(final Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private int intValue(final Object value) {
        return value == null ? 0 : Integer.parseInt(String.valueOf(value));
    }
}
