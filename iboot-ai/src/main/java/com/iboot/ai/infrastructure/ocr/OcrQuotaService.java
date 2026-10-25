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

package com.iboot.ai.infrastructure.ocr;

import com.iboot.ai.infrastructure.config.RagProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;

/**
 * OCR 日页数配额。
 *
 * <p>使用 Redis 按自然日计数，调用前预留、超限回滚；Redis 不可用时失败关闭。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class OcrQuotaService {

    private static final String KEY_PREFIX = "iboot:ai:quota:ocr:pages:";

    private final StringRedisTemplate redis;

    private final RagProperties properties;

    private final Clock clock;

    /**
     * 创建配额服务。
     *
     * @param redis      Redis 模板
     * @param properties 知识库配置
     */
    @Autowired
    public OcrQuotaService(StringRedisTemplate redis, RagProperties properties) {
        this(redis, properties, Clock.systemDefaultZone());
    }

    OcrQuotaService(StringRedisTemplate redis, RagProperties properties, Clock clock) {
        this.redis = redis;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 预留指定页数的 OCR 配额。
     *
     * @param pages 页数
     * @return 配额足够时返回 {@code true}
     */
    public boolean tryAcquire(int pages) {
        if (pages <= 0) {
            return true;
        }
        int limit = properties.getOcr().getDailyPageLimit();
        if (limit <= 0) {
            return false;
        }
        String key = KEY_PREFIX + LocalDate.now(clock);
        try {
            Long value = redis.opsForValue().increment(key, pages);
            if (value == null) {
                return false;
            }
            if (value == pages) {
                redis.expire(key, Duration.ofHours(48));
            }
            if (value > limit) {
                redis.opsForValue().decrement(key, pages);
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
