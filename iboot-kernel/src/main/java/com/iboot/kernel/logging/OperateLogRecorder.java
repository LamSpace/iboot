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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.Executor;

/**
 * 操作日志异步记录器。
 *
 * <p>在独立线程池中把操作日志插入共享的 {@code sys_operate_log} 表，避免日志落库拖慢业务请求；
 * 写入失败只记告警日志，不影响业务结果——与迁移前 {@code @Async} 的语义一致。
 *
 * <p>使用 {@link JdbcTemplate} 而非 MyBatis：写路径只有一条 insert，不涉及数据权限，用最简单的
 * 方式落库可以让内核不依赖宿主是否配置了 MyBatis。没有事实数据源时（例如非 Web 的测试切片）
 * 只记录一次告警并跳过写入。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class OperateLogRecorder {

    private static final Logger log = LoggerFactory.getLogger(OperateLogRecorder.class);

    /**
     * 插入语句，列与 {@code sys_operate_log} 的迁移前定义保持一致。
     */
    private static final String INSERT_SQL = """
            INSERT INTO sys_operate_log (title, business_type, method, request_method, operator_type,
                                         operator_name, dept_name, url, ip, location, param, result,
                                         status, error_msg, operate_time, cost_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    private final Executor executor;

    private volatile boolean missingDataSourceWarned;

    /**
     * 创建记录器。
     *
     * @param jdbcTemplate 共享库访问模板；为 {@code null} 时跳过写入
     * @param executor 日志专用线程池
     */
    public OperateLogRecorder(JdbcTemplate jdbcTemplate, Executor executor) {
        this.jdbcTemplate = jdbcTemplate;
        this.executor = executor;
    }

    /**
     * 异步记录一条操作日志。
     *
     * @param operateLog 操作日志
     */
    public void record(OperateLog operateLog) {
        if (jdbcTemplate == null) {
            if (!missingDataSourceWarned) {
                missingDataSourceWarned = true;
                log.warn("未配置数据源，操作日志不会落库");
            }
            return;
        }
        executor.execute(() -> {
            try {
                jdbcTemplate.update(INSERT_SQL,
                        operateLog.getTitle(),
                        operateLog.getBusinessType(),
                        operateLog.getMethod(),
                        operateLog.getRequestMethod(),
                        operateLog.getOperatorType(),
                        operateLog.getOperatorName(),
                        operateLog.getDeptName(),
                        operateLog.getUrl(),
                        operateLog.getIp(),
                        operateLog.getLocation(),
                        operateLog.getParam(),
                        operateLog.getResult(),
                        operateLog.getStatus(),
                        operateLog.getErrorMsg(),
                        operateLog.getOperateTime(),
                        operateLog.getCostTime());
            } catch (Exception e) {
                log.warn("记录操作日志失败：{}", e.getMessage());
            }
        });
    }
}
