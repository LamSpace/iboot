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

import com.iboot.system.domain.system.model.LeaveQuota;

import java.util.Optional;

/**
 * 请假额度仓储。
 *
 * <p>提供额度读取、懒创建、原子扣减与幂等日志写入。仓储不控制事务边界，
 * 由应用服务在调用方事务内组合这些操作。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface LeaveQuotaRepository {

    /**
     * 按用户与年度查询额度。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @return 额度；不存在时为空
     */
    Optional<LeaveQuota> findByUserIdAndYear(Long userId, int year);

    /**
     * 确保额度行存在；已存在时不修改既有值。
     *
     * @param userId     用户 ID
     * @param year       年度
     * @param totalCount 首次创建时使用的总额度
     */
    void ensureQuota(Long userId, int year, int totalCount);

    /**
     * 原子扣减一次额度。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @return 受影响的记录数；额度不足时为 0
     */
    int deductOne(Long userId, int year);

    /**
     * 写入一次额度变更日志。
     *
     * @param requestId 申请 ID，幂等键
     * @param userId    用户 ID
     * @param year      年度
     * @param delta     次数变化
     * @param type      变更类型
     * @return 是否为新写入；幂等键已存在时返回 {@code false}
     */
    boolean insertChange(String requestId, Long userId, int year, int delta, String type);
}
