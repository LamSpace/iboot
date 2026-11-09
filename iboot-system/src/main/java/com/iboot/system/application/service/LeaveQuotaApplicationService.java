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
import com.iboot.system.domain.system.model.LeaveQuota;
import com.iboot.system.domain.system.repository.LeaveQuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 请假额度应用服务。
 *
 * <p>负责默认额度解析、首次扣减懒创建、按申请幂等与原子扣减。扣减方法在调用方事务内执行；
 * 当调用方处于 Seata 全局事务中时，本条 SQL 会登记为全局事务分支。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class LeaveQuotaApplicationService {

    /**
     * 默认额度配置键。
     */
    public static final String DEFAULT_COUNT_KEY = "leave.quota.default-count";

    private static final int FALLBACK_DEFAULT_COUNT = 10;

    private static final Logger log = LoggerFactory.getLogger(LeaveQuotaApplicationService.class);

    private final LeaveQuotaRepository leaveQuotaRepository;

    private final ConfigApplicationService configApplicationService;

    /**
     * 创建应用服务。
     *
     * @param leaveQuotaRepository     额度仓储
     * @param configApplicationService 系统配置服务
     */
    public LeaveQuotaApplicationService(final LeaveQuotaRepository leaveQuotaRepository,
                                        final ConfigApplicationService configApplicationService) {
        this.leaveQuotaRepository = leaveQuotaRepository;
        this.configApplicationService = configApplicationService;
    }

    /**
     * 查询用户年度额度；额度行不存在时按默认值返回，不写库。
     *
     * @param userId 用户 ID
     * @param year   年度
     * @return 额度视图
     */
    public LeaveQuota view(final Long userId, final int year) {
        return leaveQuotaRepository.findByUserIdAndYear(userId, year)
                .orElseGet(() -> new LeaveQuota(userId, year, defaultCount(), 0, 0));
    }

    /**
     * 按申请 ID 幂等扣减一次额度。
     *
     * <p>先写幂等日志，再懒创建额度行并执行条件更新；任何一步失败都会随调用方事务回滚。
     * 同一申请重复调用直接返回当前额度，不重复扣减。
     *
     * @param requestId 申请 ID，幂等键
     * @param userId    用户 ID
     * @param year      年度
     * @return 扣减后的额度视图
     * @throws BusinessException 额度不足时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public LeaveQuota deduct(final String requestId, final Long userId, final int year) {
        if (!leaveQuotaRepository.insertChange(requestId, userId, year, 1, "DEDUCT")) {
            return view(userId, year);
        }
        leaveQuotaRepository.ensureQuota(userId, year, defaultCount());
        if (leaveQuotaRepository.deductOne(userId, year) == 0) {
            throw new BusinessException(400, "请假额度不足");
        }
        return view(userId, year);
    }

    private int defaultCount() {
        String raw = configApplicationService.getConfigValue(DEFAULT_COUNT_KEY);
        if (raw == null || raw.isBlank()) {
            return FALLBACK_DEFAULT_COUNT;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // 落到下面的告警与兜底
        }
        log.warn("请假额度默认值非法，按 {} 处理：{}", FALLBACK_DEFAULT_COUNT, raw);
        return FALLBACK_DEFAULT_COUNT;
    }
}
