/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.iboot.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import net.logstash.logback.encoder.LogstashEncoder;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link KafkaAppender} 的端到端集成测试。
 *
 * <p>用 Testcontainers 启动一个 KRaft 模式的真实 Kafka，验证「appender 编码 → 生产者发送 →
 * 消费者读取」的完整往返；镜像与本地编排保持同一版本，避免客户端与服务端漂移。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers
class KafkaAppenderIntegrationTest {

    /**
     * 测试专用 topic，避免与本地编排的 {@code iboot-logs} 混用。
     */
    private static final String TOPIC = "iboot-logs-it";

    /**
     * 消费等待上限。
     */
    private static final Duration CONSUME_TIMEOUT = Duration.ofSeconds(20);

    /**
     * 与本地编排一致的 Kafka 镜像。
     */
    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.1"));

    /**
     * 验证结构化日志经真实 broker 往返后仍携带来源服务与追踪标识。
     *
     * @throws Exception 建 topic 或等待消费超时
     */
    @Test
    void deliversStructuredLogToKafka() throws Exception {
        createTopic();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        KafkaAppender appender = new KafkaAppender();
        appender.setName("KAFKA-IT");
        appender.setContext(context);
        appender.setEnabled(true);
        appender.setBootstrapServers(KAFKA.getBootstrapServers());
        appender.setTopic(TOPIC);
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setCustomFields("{\"app\":\"iboot-it\"}");
        appender.setEncoder(encoder);
        appender.start();

        Logger logger = context.getLogger("com.iboot.logging.it." + System.nanoTime());
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(appender);

        MDC.put("traceId", "it-trace-1");
        try {
            logger.info("integration probe");
        } finally {
            MDC.clear();
        }

        String payload;
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(consumerProperties())) {
            consumer.subscribe(List.of(TOPIC));
            payload = poll(consumer);
        }

        assertNotNull(payload, "应消费到一条日志记录");
        assertTrue(payload.contains("\"app\":\"iboot-it\""), payload);
        assertTrue(payload.contains("\"traceId\":\"it-trace-1\""), payload);

        logger.detachAppender(appender);
        appender.stop();
    }

    /**
     * 显式创建单分区 topic，避免依赖 broker 的自动建 topic 配置。
     *
     * @throws Exception 建 topic 失败或超时
     */
    private static void createTopic() throws Exception {
        try (Admin admin = Admin.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get(30, TimeUnit.SECONDS);
        }
    }

    /**
     * 构造指向测试容器的消费者配置。
     *
     * @return 消费者配置
     */
    private static Map<String, Object> consumerProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "iboot-logging-it-" + UUID.randomUUID());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return properties;
    }

    /**
     * 轮询消费直到拿到第一条记录或超时。
     *
     * @param consumer 已订阅 topic 的消费者
     * @return 日志正文
     */
    private static String poll(KafkaConsumer<String, byte[]> consumer) {
        long deadline = System.currentTimeMillis() + CONSUME_TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, byte[]> record : consumer.poll(Duration.ofMillis(500))) {
                return new String(record.value(), StandardCharsets.UTF_8);
            }
        }
        fail("未在超时时间内消费到日志记录");
        return null;
    }
}
