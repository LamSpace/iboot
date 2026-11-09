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
 * 请假额度端口。
 *
 * <p>业务主流程只依赖该端口，不感知额度数据由哪个服务拥有。扣减 MUST 在调用方
 * 全局事务内执行：失败时抛出异常，使审批的本地写入一起回滚。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface LeaveQuotaPort {

    /**
     * 按申请 ID 幂等扣减一次额度。
     *
     * @param requestId 申请 ID，幂等键
     * @param userId    申请人用户 ID
     * @param year      请假开始时间所在年度
     * @return 扣减后的额度快照
     * @throws BusinessException 额度不足、下游不可用或未处于全局事务时抛出
     */
    LeaveQuotaSnapshot deduct(String requestId, Long userId, int year);

    /**
     * 查询指定用户的年度额度。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @return 额度快照
     * @throws BusinessException 下游不可用时抛出
     */
    LeaveQuotaSnapshot view(Long userId, int year);
}
