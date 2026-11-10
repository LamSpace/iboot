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

package com.iboot.workflow.application.service.quota;

import com.iboot.kernel.web.BusinessException;

/**
 * 额度划转 TCC 端口。
 *
 * <p>调用方 MUST 处于全局事务中；实现只发起 Try，Confirm/Cancel 由 Seata 协调器
 * 回调额度服务。实现失败时抛出异常，使全局事务回滚。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface LeaveQuotaTransferPort {

    /**
     * 发起额度划转的 Try。
     *
     * @param transferId 划转业务标识，幂等键
     * @param fromUserId 划出方用户 ID
     * @param toUserId   划入方用户 ID
     * @param year       年度
     * @param count      划转次数
     * @throws BusinessException 额度不足、划转已取消、下游不可用或未处于全局事务时抛出
     */
    void transfer(String transferId, Long fromUserId, Long toUserId, int year, int count);
}
