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

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link OperateLogRecorder} 的写入测试。
 *
 * <p>记录器是操作日志的唯一写入点；这里断言它确实把字段按顺序交给 JDBC，并且在没有数据源时
 * 不会让调用方失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class OperateLogRecorderTest {

    /**
     * 记录器应按列顺序把日志字段传给 JDBC。
     */
    @Test
    void insertsAllColumns() {
        AtomicReference<Object[]> captured = new AtomicReference<>();
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                captured.set(args);
                return 1;
            }
        };

        new OperateLogRecorder(jdbcTemplate, Runnable::run).record(sample());

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
        assertThatCode(() -> new OperateLogRecorder(null, Runnable::run).record(sample()))
                .doesNotThrowAnyException();
    }

    private OperateLog sample() {
        OperateLog log = OperateLog.builder()
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
        return log;
    }
}
