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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * 操作日志异步记录器。
 *
 * <p>在独立线程池中把操作日志插入配置的审计表，避免日志落库拖慢业务请求；
 * 目标表由 {@code iboot.logging.operate-log-table} 指定，拆分服务可以指向共享审计表。
 * 写入失败只记错误日志并累加指标，不影响业务结果——与迁移前 {@code @Async} 的语义一致。
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
     * 未配置写入目标时使用的默认审计表。
     */
    private static final String DEFAULT_TABLE_NAME = "sys_operate_log";

    /**
     * 允许的写入目标格式：{@code table} 或 {@code schema.table}。
     *
     * <p>表名会拼进 SQL，不能作为绑定参数，因此必须在启动阶段校验。
     */
    private static final Pattern TABLE_NAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)?$");

    private final JdbcTemplate jdbcTemplate;

    private final Executor executor;

    private final String tableName;

    private final String insertSql;

    /**
     * 写入失败计数器；宿主没有注册表时为空，例如非 Web 的测试切片。
     */
    private final Counter failedCounter;

    private volatile boolean missingDataSourceWarned;

    /**
     * 保证数据库持续不可用时错误日志只打一次，避免刷屏。
     */
    private final AtomicBoolean writeFailureLogged = new AtomicBoolean();

    /**
     * 创建记录器。
     *
     * @param jdbcTemplate    共享库访问模板；为 {@code null} 时跳过写入
     * @param executor        日志专用线程池
     * @param tableName       审计表名，允许 {@code schema.table}；空白时使用
     *                        {@code sys_operate_log}
     * @param meterRegistry   指标注册表；为 {@code null} 时只记录日志
     * @param applicationName 应用名，作为失败计数的标签
     * @throws IllegalArgumentException 表名不符合 {@code table} 或 {@code schema.table} 格式时抛出
     */
    public OperateLogRecorder(JdbcTemplate jdbcTemplate, Executor executor, String tableName,
                              MeterRegistry meterRegistry, String applicationName) {
        this.jdbcTemplate = jdbcTemplate;
        this.executor = executor;
        this.tableName = requireValidTableName(tableName);
        this.insertSql = buildInsertSql(this.tableName);
        this.failedCounter = meterRegistry == null ? null
                : Counter.builder("operate_log_write_failed")
                        .description("操作日志写入失败的次数")
                        .tag("application", applicationName)
                        .register(meterRegistry);
    }

    /**
     * 校验并规范化审计表名。
     *
     * @param tableName 配置的表名，可为空白
     * @return 合法的 {@code table} 或 {@code schema.table}；空白时返回默认表名
     * @throws IllegalArgumentException 表名不符合白名单格式时抛出
     */
    static String requireValidTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return DEFAULT_TABLE_NAME;
        }
        String candidate = tableName.trim();
        if (!TABLE_NAME_PATTERN.matcher(candidate).matches()) {
            throw new IllegalArgumentException(
                    "iboot.logging.operate-log-table 配置非法：" + tableName);
        }
        return candidate;
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
                jdbcTemplate.update(insertSql,
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
                if (failedCounter != null) {
                    failedCounter.increment();
                }
                if (writeFailureLogged.compareAndSet(false, true)) {
                    log.error("记录操作日志失败（目标表 {}）：{}", tableName, e.getMessage());
                } else {
                    log.debug("记录操作日志失败（目标表 {}）：{}", tableName, e.getMessage());
                }
            }
        });
    }

    /**
     * 启动后验证审计表可达性。
     *
     * <p>只做一次轻量查询；失败时记录错误日志，但不阻止服务启动——审计不可用不应让业务服务起不来。
     * 没有数据源时直接跳过。
     */
    public void verifyTarget() {
        if (jdbcTemplate == null) {
            return;
        }
        try {
            jdbcTemplate.queryForList("SELECT 1 FROM " + tableName + " LIMIT 1");
        } catch (Exception e) {
            log.error("操作日志目标表 {} 不可达：{}", tableName, e.getMessage());
        }
    }

    private static String buildInsertSql(String tableName) {
        return "INSERT INTO " + tableName + " (title, business_type, method, request_method, operator_type,"
                + " operator_name, dept_name, url, ip, location, param, result,"
                + " status, error_msg, operate_time, cost_time)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
}
