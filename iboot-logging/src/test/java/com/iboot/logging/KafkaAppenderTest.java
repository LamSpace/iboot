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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import net.logstash.logback.appender.listener.AppenderListener;
import net.logstash.logback.encoder.LogstashEncoder;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link KafkaAppender} 的单元测试。
 *
 * <p>使用 kafka-clients 官方测试替身 {@link MockProducer}，不连接真实 broker：
 * 覆盖编码内容、发送失败不向调用方抛出、环形缓冲满时丢弃、关闭时释放生产者与关闭态不建连。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class KafkaAppenderTest {

    /**
     * 等待异步处理完成的超时时间。
     */
    private static final long AWAIT_TIMEOUT_MILLIS = 5000L;

    /**
     * 编码结果必须包含来源服务与追踪标识，保证 ES 中的检索契约不变。
     */
    @Test
    void encodesAppAndTraceId() throws Exception {
        MockProducer<String, byte[]> producer = newMockProducer();
        TestAppender appender = new TestAppender(producer);
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setCustomFields("{\"app\":\"iboot-test\"}");
        appender.setEncoder(encoder);
        Logger logger = startAppender(appender);

        MDC.put("traceId", "trace-1");
        try {
            logger.info("hello kafka");
        } finally {
            MDC.clear();
        }

        awaitHistory(producer, 1, appender);
        String payload = new String(producer.history().get(0).value(), StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"app\":\"iboot-test\""), payload);
        assertTrue(payload.contains("\"traceId\":\"trace-1\""), payload);
        appender.stop();
    }

    /**
     * 发送抛异常时不得向日志调用方传播，否则单次后端故障会变成业务异常。
     */
    @Test
    void sendFailureDoesNotPropagate() {
        Producer<String, byte[]> producer = new FailingProducer();
        TestAppender appender = new TestAppender(producer);
        Logger logger = startAppender(appender);

        try {
            logger.info("must not throw");
        } catch (RuntimeException e) {
            fail("日志调用方不应收到发送异常", e);
        }

        assertTrue(appender.isStarted(), "appender 应保持启动状态");
        appender.stop();
    }

    /**
     * 环形缓冲满时丢弃事件并通知监听器，而不是阻塞调用线程。
     */
    @Test
    void dropsWhenRingBufferIsFull() throws Exception {
        BlockingProducer producer = new BlockingProducer();
        TestAppender appender = new TestAppender(producer);
        appender.setRingBufferSize(1);
        AtomicInteger dropped = new AtomicInteger();
        appender.addListener(new AppenderListener<>() {
            @Override
            public void eventAppendFailed(ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender,
                                          ch.qos.logback.classic.spi.ILoggingEvent event, Throwable reason) {
                dropped.incrementAndGet();
            }
        });
        Logger logger = startAppender(appender);

        for (int i = 0; i < 100; i++) {
            logger.info("event {}", i);
        }

        assertTrue(dropped.get() > 0, "缓冲满时应产生丢弃");
        producer.release();
        appender.stop();
    }

    /**
     * 关闭 appender 时必须释放生产者，避免后台线程与连接泄漏。
     */
    @Test
    void closesProducerOnStop() throws Exception {
        MockProducer<String, byte[]> producer = newMockProducer();
        TestAppender appender = new TestAppender(producer);
        Logger logger = startAppender(appender);

        logger.info("stop me");
        awaitHistory(producer, 1, appender);
        appender.stop();

        assertTrue(producer.closed(), "停止后生产者应已关闭");
    }

    /**
     * 关闭态不创建生产者、不投递任何事件，宿主机调试与自动化测试因此不受影响。
     */
    @Test
    void disabledAppenderCreatesNoProducer() throws Exception {
        MockProducer<String, byte[]> producer = newMockProducer();
        TestAppender appender = new TestAppender(producer);
        appender.setEnabled(false);
        Logger logger = startAppender(appender);

        logger.info("dropped locally");
        Thread.sleep(100L);

        assertFalse(appender.producerCreated, "关闭态不应创建生产者");
        assertTrue(producer.history().isEmpty(), "关闭态不应投递事件");
        appender.stop();
    }

    /**
     * 构造一个自动完成发送的测试替身。
     *
     * @return MockProducer
     */
    private static MockProducer<String, byte[]> newMockProducer() {
        return new MockProducer<>(true, null, new StringSerializer(), new ByteArraySerializer());
    }

    /**
     * 把 appender 挂到一个新的 LoggerContext 并返回对应 logger。
     *
     * @param appender 待启动的 appender
     * @return 与该 context 绑定的 logger
     */
    private static Logger startAppender(TestAppender appender) {
        // 必须复用 SLF4J 已初始化的上下文：裸 LoggerContext 没有 MDC 适配器，
        // 事件在 prepareForDeferredProcessing 阶段会因 MDC 缺失而失败。
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender.setName("KAFKA-TEST");
        appender.setContext(context);
        appender.setTopic("iboot-logs");
        appender.start();
        Logger logger = context.getLogger("com.iboot.logging.test." + System.nanoTime());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(appender);
        return logger;
    }

    /**
     * 轮询等待生产者收到至少指定数量的记录。
     *
     * @param producer 生产者
     * @param expected 期望记录数
     * @param appender 用于失败时输出 logback 状态
     * @throws InterruptedException 等待被中断
     */
    private static void awaitHistory(MockProducer<String, byte[]> producer, int expected, KafkaAppender appender)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MILLIS;
        while (producer.history().size() < expected) {
            if (System.currentTimeMillis() > deadline) {
                fail("等待 Kafka 记录超时，期望 " + expected + " 条，实际 " + producer.history().size()
                        + " 条，状态：" + appender.getContext().getStatusManager().getCopyOfStatusList());
            }
            Thread.sleep(10L);
        }
    }

    /**
     * 测试用 appender：跳过真实生产者构造，记录是否被调用。
     */
    private static class TestAppender extends KafkaAppender {

        private final Producer<String, byte[]> testProducer;

        private volatile boolean producerCreated;

        TestAppender(Producer<String, byte[]> testProducer) {
            this.testProducer = testProducer;
            setEnabled(true);
        }

        @Override
        protected Producer<String, byte[]> createProducer(Properties properties) {
            producerCreated = true;
            return testProducer;
        }
    }

    /**
     * 发送总是抛异常的测试替身，用于验证异常不会传播到日志调用方。
     */
    private static class FailingProducer extends MockProducer<String, byte[]> {

        FailingProducer() {
            super(true, null, new StringSerializer(), new ByteArraySerializer());
        }

        @Override
        public synchronized Future<RecordMetadata> send(ProducerRecord<String, byte[]> record, Callback callback) {
            throw new RuntimeException("broker down");
        }
    }

    /**
     * 发送被闸门阻塞的测试替身，用于制造环形缓冲满的场景。
     */
    private static class BlockingProducer extends MockProducer<String, byte[]> {

        private final CountDownLatch gate = new CountDownLatch(1);

        BlockingProducer() {
            super(true, null, new StringSerializer(), new ByteArraySerializer());
        }

        @Override
        public synchronized Future<RecordMetadata> send(ProducerRecord<String, byte[]> record, Callback callback) {
            try {
                gate.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return super.send(record, callback);
        }

        void release() {
            gate.countDown();
        }
    }
}
