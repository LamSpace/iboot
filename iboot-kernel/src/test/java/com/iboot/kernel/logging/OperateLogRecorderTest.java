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

package com.iboot.kernel.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OperateLogRecorder} 的写入测试。
 *
 * <p>记录器是操作日志的唯一写入点；这里断言它把字段按顺序交给 JDBC、使用配置的目标表、
 * 失败时计数且只告警一次，并且没有数据源时不会让调用方失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class OperateLogRecorderTest {

    /**
     * 记录器应按列顺序把日志字段传给 JDBC，并使用配置的限定表名。
     */
    @Test
    void insertsAllColumnsIntoConfiguredTable() {
        AtomicReference<String> capturedSql = new AtomicReference<>();
        AtomicReference<Object[]> captured = new AtomicReference<>();
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                capturedSql.set(sql);
                captured.set(args);
                return 1;
            }
        };

        recorder(jdbcTemplate, new SimpleMeterRegistry()).record(sample());

        assertThat(capturedSql.get()).contains("INSERT INTO iboot_admin.sys_operate_log");
        assertThat(captured.get()).hasSize(16);
        assertThat(captured.get()[0]).isEqualTo("登录");
        assertThat(captured.get()[5]).isEqualTo("tester");
        assertThat(captured.get()[12]).isEqualTo(1);
        assertThat(captured.get()[14]).isInstanceOf(LocalDateTime.class);
    }

    /**
     * 没有数据源时只跳过写入，不向调用方抛异常。
     */
    @Test
    void skipsWhenDataSourceMissing() {
        assertThatCode(() -> new OperateLogRecorder(
                null, Runnable::run, "sys_operate_log", new SimpleMeterRegistry(), "itest")
                .record(sample()))
                .doesNotThrowAnyException();
    }

    /**
     * 写入失败时计数每次增加，但错误日志只打一次。
     */
    @Test
    void countsFailuresAndLogsOnce() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JdbcTemplate failing = new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                throw new DataAccessResourceFailureException("database down");
            }
        };
        OperateLogRecorder recorder = recorder(failing, registry);

        Logger logger = (Logger) LoggerFactory.getLogger(OperateLogRecorder.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            recorder.record(sample());
            recorder.record(sample());
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(registry.get("operate_log_write_failed")
                .tag("application", "itest")
                .counter()
                .count()).isEqualTo(2.0);
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    /**
     * 目标表不可达时只记录一次错误，不向启动流程抛异常。
     */
    @Test
    void verifyTargetLogsWhenTableMissing() {
        JdbcTemplate missingTable = new JdbcTemplate() {
            @Override
            public List<Map<String, Object>> queryForList(String sql) {
                throw new DataAccessResourceFailureException("table not found");
            }
        };
        OperateLogRecorder recorder = recorder(missingTable, new SimpleMeterRegistry());

        Logger logger = (Logger) LoggerFactory.getLogger(OperateLogRecorder.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatCode(recorder::verifyTarget).doesNotThrowAnyException();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    /**
     * 空白配置回落到默认表名，合法的限定名原样保留。
     */
    @Test
    void acceptsDefaultAndQualifiedTableNames() {
        assertThat(OperateLogRecorder.requireValidTableName(null)).isEqualTo("sys_operate_log");
        assertThat(OperateLogRecorder.requireValidTableName(" ")).isEqualTo("sys_operate_log");
        assertThat(OperateLogRecorder.requireValidTableName(" iboot_admin.sys_operate_log "))
                .isEqualTo("iboot_admin.sys_operate_log");
    }

    /**
     * 疑似注入或不合法的表名必须被拒绝。
     */
    @Test
    void rejectsInvalidTableNames() {
        assertThatThrownBy(() -> OperateLogRecorder.requireValidTableName("sys_operate_log;drop"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OperateLogRecorder.requireValidTableName("a.b.c"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OperateLogRecorder.requireValidTableName("bad-name"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private OperateLogRecorder recorder(JdbcTemplate jdbcTemplate, SimpleMeterRegistry registry) {
        return new OperateLogRecorder(
                jdbcTemplate, Runnable::run, "iboot_admin.sys_operate_log", registry, "itest");
    }

    private OperateLog sample() {
        return OperateLog.builder()
                .title("登录")
                .businessType(0)
                .method("com.example.Controller.login()")
                .requestMethod("POST")
                .operatorName("tester")
                .deptName("研发部")
                .url("/api/auth/login")
                .ip("127.0.0.1")
                .location("")
                .status(1)
                .operateTime(LocalDateTime.now())
                .costTime(12L)
                .build();
    }
}
