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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 配额服务的集成测试。
 *
 * <p>验证单用户、全局与跨日重置语义。
 */
@Testcontainers(disabledWithoutDocker = true)
class QuotaServiceTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.0.14"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;

    private StringRedisTemplate redisTemplate;

    private AiProperties properties;

    private MutableClock clock;

    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.getConnection().serverCommands().flushAll();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        properties = new AiProperties();
        clock = new MutableClock(Instant.parse("2026-10-25T00:00:00Z"));
        quotaService = new QuotaService(redisTemplate, properties,
                new AiMetrics(new SimpleMeterRegistry(), "test-model"), clock);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void rejectsWhenUserLimitReached() {
        properties.getQuota().setUserDailyTokens(10);

        quotaService.recordUsage(1L, 6, 5);

        assertThatThrownBy(() -> quotaService.assertWithinQuota(1L))
                .isInstanceOf(QuotaExceededException.class)
                .extracting("scope")
                .isEqualTo("user");
    }

    @Test
    void rejectsWhenGlobalLimitReached() {
        properties.getQuota().setUserDailyTokens(1000);
        properties.getQuota().setGlobalDailyTokens(10);

        quotaService.recordUsage(1L, 11, 0);

        assertThatThrownBy(() -> quotaService.assertWithinQuota(2L))
                .isInstanceOf(QuotaExceededException.class)
                .extracting("scope")
                .isEqualTo("global");
    }

    @Test
    void resetsOnNextDay() {
        properties.getQuota().setUserDailyTokens(10);
        quotaService.recordUsage(1L, 11, 0);

        assertThatThrownBy(() -> quotaService.assertWithinQuota(1L))
                .isInstanceOf(QuotaExceededException.class);

        clock.plusDays(1);

        quotaService.assertWithinQuota(1L);
        assertThat(quotaService.usage(1L).used()).isZero();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Shanghai");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void plusDays(long days) {
            instant = instant.plus(days, ChronoUnit.DAYS);
        }
    }
}
