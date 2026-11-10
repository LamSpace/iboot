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

package com.iboot.workflow.infrastructure.demo;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seata TCC 演练故障切面。
 *
 * <p>仅在 {@code seata-tcc-demo} profile 注册。在额度划转 Try 成功返回后检查故障标记；
 * 标记存在时抛出异常，使全局事务回滚并驱动协调器调用 Cancel。默认 profile 不注册，
 * 不参与生产行为。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Aspect
@Component
@Profile("seata-tcc-demo")
public class SeataTccDemoFaultAspect {

    /**
     * TCC Try 成功返回后触发故障。
     */
    @AfterReturning("execution(* com.iboot.workflow.application.service.quota.LeaveQuotaTransferPort.transfer(..))")
    public void failAfterTccTry() {
        if (SeataDemoFaultContext.isRequested(SeataDemoFaultContext.AFTER_TCC_TRY)) {
            SeataDemoFaultContext.clear();
            throw new IllegalStateException("seata tcc demo fault after try");
        }
    }
}
