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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 共享内核的操作日志自动配置。
 *
 * <p>注册切面、异步记录器与日志专用线程池。宿主只需加依赖并使用 {@code @Log}，
 * 不需要再配置 AOP 或异步执行器。
 *
 * <p>日志写入是旁路行为：线程池独立于业务线程池，写失败只记告警，不影响请求结果。
 * 数据源按需获取，测试切片没有数据源时不会阻断上下文启动。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
public class KernelLoggingAutoConfiguration {

    /**
     * 操作日志专用线程池。
     *
     * <p>单线程即可：日志写入是低价值旁路操作，串行执行既能保证顺序，也不会与业务线程
     * 竞争数据库连接。队列满时由调用线程执行，不丢弃日志。
     *
     * @return 日志专用执行器
     */
    @Bean(name = "operateLogTaskExecutor")
    @ConditionalOnMissingBean(name = "operateLogTaskExecutor")
    public ThreadPoolTaskExecutor operateLogTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("operate-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 操作日志记录器。
     *
     * @param dataSourceProvider 数据源，可缺省
     * @param executor 日志专用线程池
     * @return 异步记录器
     */
    @Bean
    @ConditionalOnMissingBean
    public OperateLogRecorder operateLogRecorder(ObjectProvider<DataSource> dataSourceProvider,
                                                 @Qualifier("operateLogTaskExecutor") Executor executor) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        return new OperateLogRecorder(dataSource == null ? null : new JdbcTemplate(dataSource), executor);
    }

    /**
     * 操作日志切面。
     *
     * @param recorder 操作日志记录器
     * @return 切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    public LogAspect logAspect(OperateLogRecorder recorder) {
        return new LogAspect(recorder);
    }
}
