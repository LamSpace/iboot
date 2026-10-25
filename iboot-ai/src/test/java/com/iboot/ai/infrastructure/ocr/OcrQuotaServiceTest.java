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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OcrQuotaService} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class OcrQuotaServiceTest {

    private static final String KEY = "iboot:ai:quota:ocr:pages:2026-10-25";

    private StringRedisTemplate redis;

    private ValueOperations<String, String> operations;

    private OcrQuotaService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        RagProperties properties = new RagProperties();
        properties.getOcr().setDailyPageLimit(2);
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new OcrQuotaService(redis, properties, clock);
    }

    /**
     * 超过日页数上限时拒绝并回滚计数。
     */
    @Test
    void rejectsWhenDailyLimitExceeded() {
        when(operations.increment(KEY, 1L)).thenReturn(1L, 2L, 3L);

        assertThat(service.tryAcquire(1)).isTrue();
        assertThat(service.tryAcquire(1)).isTrue();
        assertThat(service.tryAcquire(1)).isFalse();

        verify(operations).decrement(KEY, 1L);
    }
}
