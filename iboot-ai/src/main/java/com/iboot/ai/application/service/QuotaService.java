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

package com.iboot.ai.application.service;

import com.iboot.ai.infrastructure.config.AiProperties;
import com.iboot.ai.infrastructure.metrics.AiMetrics;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * 每日 token 配额。
 *
 * <p>按北京时间自然日统计全局与单用户用量。调用前只做“当前值是否已超限”的检查，调用后再按
 * 实际 usage 累加；并发下允许小幅超额，由调用方预留余量。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class QuotaService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final String GLOBAL_PREFIX = "iboot:ai:quota:global:";

    private static final String USER_PREFIX = "iboot:ai:quota:user:";

    private static final long TTL_HOURS = 48L;

    private final StringRedisTemplate redisTemplate;

    private final AiProperties aiProperties;

    private final AiMetrics aiMetrics;

    private final Clock clock;

    /**
     * 创建配额服务。
     *
     * @param redisTemplate 字符串 Redis 模板
     * @param aiProperties  配额配置
     * @param aiMetrics     指标
     */
    @org.springframework.beans.factory.annotation.Autowired
    public QuotaService(StringRedisTemplate redisTemplate, AiProperties aiProperties, AiMetrics aiMetrics) {
        this(redisTemplate, aiProperties, aiMetrics, Clock.system(ZONE));
    }

    QuotaService(StringRedisTemplate redisTemplate, AiProperties aiProperties, AiMetrics aiMetrics, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.aiProperties = aiProperties;
        this.aiMetrics = aiMetrics;
        this.clock = clock;
    }

    /**
     * 检查配额，超限时抛出异常。
     *
     * @param userId 用户 ID
     */
    public void assertWithinQuota(Long userId) {
        long globalUsed = read(globalKey());
        if (globalUsed >= aiProperties.getQuota().getGlobalDailyTokens()) {
            aiMetrics.quotaRejected("global");
            throw new QuotaExceededException("global", "今日全局 AI 配额已用尽");
        }
        long userUsed = read(userKey(userId));
        if (userUsed >= aiProperties.getQuota().getUserDailyTokens()) {
            aiMetrics.quotaRejected("user");
            throw new QuotaExceededException("user", "今日个人 AI 配额已用尽");
        }
    }

    /**
     * 记录实际用量。
     *
     * @param userId           用户 ID
     * @param promptTokens     输入 token
     * @param completionTokens 输出 token
     */
    public void recordUsage(Long userId, int promptTokens, int completionTokens) {
        long total = (long) promptTokens + completionTokens;
        increment(globalKey(), total);
        increment(userKey(userId), total);
    }

    /**
     * 查询用户当天用量与剩余额度。
     *
     * @param userId 用户 ID
     * @return 用量快照
     */
    public UsageSnapshot usage(Long userId) {
        long used = read(userKey(userId));
        long limit = aiProperties.getQuota().getUserDailyTokens();
        return new UsageSnapshot(today().toString(), used, Math.max(0L, limit - used), limit);
    }

    /**
     * 用量快照。
     *
     * @param date      统计日期
     * @param used      已用 token
     * @param remaining 剩余 token
     * @param limit     单用户上限
     */
    public record UsageSnapshot(String date, long used, long remaining, long limit) {
    }

    private long read(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private void increment(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        if (value != null && value == delta) {
            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        }
    }

    private String globalKey() {
        return GLOBAL_PREFIX + today();
    }

    private String userKey(Long userId) {
        return USER_PREFIX + userId + ":" + today();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
