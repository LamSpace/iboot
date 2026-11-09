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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.encoder.com.lmax.disruptor.EventHandler;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 把结构化日志经 Kafka 投递到集中日志系统的 Logback appender。
 *
 * <p>继承自 {@link LoggingEventAsyncDisruptorAppender}，复用其环形缓冲、非阻塞丢弃与停机排空语义：
 * 业务线程只负责把日志事件放入缓冲，JSON 编码与 Kafka 发送都发生在独立的处理线程上，
 * 因此 broker 不可用时不会阻塞请求线程。缓冲耗尽时事件被丢弃并计入丢弃计数，
 * 不影响业务请求；broker 恢复后下一次发送自动成功，无需重启服务。
 *
 * <p>投放语义为至少一次：发送超时后会重试，可能产生重复记录；不提供去重。
 * 本类不做任何 Spring 集成——Logback 在 Spring 上下文之前初始化，只能直接持有 Kafka 客户端。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class KafkaAppender extends LoggingEventAsyncDisruptorAppender {

    /**
     * 关闭生产者时等待未完成发送的最长时间，避免拖住进程停机。
     */
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(2);

    /**
     * 发送失败告警的间隔：首次失败必告警，之后每累计这么多次再告警一次，防止日志后端故障时刷屏。
     */
    private static final long FAILURE_WARN_INTERVAL = 1000L;

    /**
     * 是否启用投递。默认关闭，由 {@code iboot.logging.kafka.enabled} 控制；
     * 容器化部署显式开启，宿主机调试与自动化测试保持关闭。
     */
    private boolean enabled;

    /**
     * Kafka 引导地址。容器内为 {@code kafka:9092}，宿主机进程为 {@code localhost:29092}。
     */
    private String bootstrapServers = "localhost:29092";

    /**
     * 日志 topic，与集中日志侧的消费者共同约定。
     */
    private String topic = "iboot-logs";

    /**
     * 生产者客户端标识，默认取服务应用名，便于在 broker 侧按服务观察连接。
     */
    private String clientId = "iboot-logging";

    /**
     * 事件编码器，默认输出与既有链路一致的 JSON（含 {@code app}、{@code traceId}）。
     */
    private Encoder<ILoggingEvent> encoder = new LogstashEncoder();

    /**
     * 真实 Kafka 生产者；关闭或初始化失败时为 {@code null}，此时日志只落控制台与文件。
     */
    private Producer<String, byte[]> producer;

    /**
     * 累计发送失败次数，用于节流告警。
     */
    private final AtomicLong failureCount = new AtomicLong();

    /**
     * 启动 appender。
     *
     * <p>先按配置创建生产者，再启动基类的环形缓冲与处理线程。创建失败只记录 error 状态并降级为
     * 仅本地输出，不抛出异常，保证日志后端问题不会阻止服务启动。
     */
    @Override
    public void start() {
        if (enabled) {
            try {
                encoder.setContext(getContext());
                encoder.start();
                producer = createProducer(buildProducerProperties());
            } catch (RuntimeException e) {
                producer = null;
                addError("Kafka 日志投递初始化失败，将只保留控制台与文件输出", e);
            }
        }
        super.start();
    }

    /**
     * 停止 appender：先排空环形缓冲，再关闭生产者。
     *
     * <p>排空由基类完成，最多等待其配置的宽限期；随后关闭生产者，等待时间不超过
     * {@value #CLOSE_TIMEOUT} 秒，未确认的批次允许丢失。
     */
    @Override
    public void stop() {
        super.stop();
        Producer<String, byte[]> current = producer;
        producer = null;
        if (current != null) {
            try {
                current.close(CLOSE_TIMEOUT);
            } catch (RuntimeException e) {
                addWarn("关闭 Kafka 生产者时出现异常", e);
            }
        }
        if (encoder.isStarted()) {
            encoder.stop();
        }
    }

    /**
     * 投递入口：未启用或生产者不可用时直接返回，不进入环形缓冲。
     *
     * <p>该方法在日志调用线程上执行，必须保持常量时间、不发起网络 I/O。
     *
     * @param event 日志事件
     */
    @Override
    protected void append(ILoggingEvent event) {
        if (producer == null) {
            return;
        }
        super.append(event);
    }

    /**
     * 创建处理线程的事件处理器：在 disruptor 线程上编码并发送到 Kafka。
     *
     * @return 事件处理器
     */
    @Override
    protected EventHandler<LogEvent<ILoggingEvent>> createEventHandler() {
        return (logEvent, sequence, endOfBatch) -> send(logEvent.event);
    }

    /**
     * 创建 Kafka 生产者；测试通过覆写该方法注入 {@code MockProducer}。
     *
     * @param properties 生产者配置
     * @return 生产者实例
     */
    protected Producer<String, byte[]> createProducer(Properties properties) {
        return new KafkaProducer<>(properties);
    }

    /**
     * 编码并异步发送一条日志。
     *
     * <p>发送结果通过回调处理，任何异常都只计入失败次数与状态告警，不向调用方抛出。
     *
     * @param event 日志事件
     */
    private void send(ILoggingEvent event) {
        try {
            byte[] payload = encoder.encode(event);
            if (payload == null || payload.length == 0) {
                return;
            }
            producer.send(new ProducerRecord<>(topic, payload), (metadata, exception) -> {
                if (exception != null) {
                    reportFailure(exception);
                }
            });
        } catch (RuntimeException e) {
            reportFailure(e);
        }
    }

    /**
     * 记录一次发送失败，并按固定间隔输出状态告警。
     *
     * @param failure 失败原因
     */
    private void reportFailure(Throwable failure) {
        long count = failureCount.incrementAndGet();
        if (count == 1 || count % FAILURE_WARN_INTERVAL == 0) {
            addWarn("Kafka 日志投递失败 " + count + " 次，事件被丢弃（topic=" + topic + "）", failure);
        }
    }

    /**
     * 构造生产者配置。
     *
     * <p>关键取舍：{@code max.block.ms} 限制 broker 不可达时处理线程的阻塞上限；
     * {@code delivery.timeout.ms} 界定恢复后自动续投的窗口；缓冲与压缩参数使停机期间堆占用有界。
     *
     * @return 生产者配置
     */
    private Properties buildProducerProperties() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ProducerConfig.ACKS_CONFIG, "1");
        properties.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        return properties;
    }

    /**
     * 是否启用 Kafka 投递。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 Kafka 投递。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 Kafka 引导地址。
     *
     * @return 引导地址
     */
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    /**
     * 设置 Kafka 引导地址。
     *
     * @param bootstrapServers 引导地址
     */
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    /**
     * 获取日志 topic。
     *
     * @return topic 名称
     */
    public String getTopic() {
        return topic;
    }

    /**
     * 设置日志 topic。
     *
     * @param topic topic 名称
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * 获取生产者客户端标识。
     *
     * @return 客户端标识
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * 设置生产者客户端标识。
     *
     * @param clientId 客户端标识
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * 获取事件编码器。
     *
     * @return 编码器
     */
    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    /**
     * 设置事件编码器。
     *
     * @param encoder 编码器
     */
    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }
}
