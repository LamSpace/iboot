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

package com.iboot.system.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * TCC 数据源配置。
 *
 * <p>TCC 动作的本地写入必须走一个未被 Seata AT 自动代理的连接池，否则 Try 中的 SQL
 * 会额外登记 AT 分支，回滚时与 Cancel 重复补偿。该数据源通过
 * {@code seata.excludes-for-auto-proxying} 按 Bean 名排除，配套
 * {@link DataSourceTransactionManager} 只服务 TCC 的三段操作。
 *
 * <p>连接池刻意保持小规格（默认 1~5 个连接），仅承载额度划转。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see PrimaryDataSourceConfig
 */
@Configuration
public class TccDataSourceConfig {

    /**
     * 创建 TCC 专用数据源。
     *
     * @return 未被 Seata 代理的 Druid 数据源
     */
    @Bean("tccDataSource")
    @ConfigurationProperties("iboot.tcc.datasource")
    public TccDruidDataSource tccDataSource() {
        return new TccDruidDataSource();
    }

    /**
     * 创建 TCC 专用事务管理器。
     *
     * @param tccDataSource TCC 数据源
     * @return 只管理 TCC 数据源本地事务的事务管理器
     */
    @Bean("tccTransactionManager")
    public DataSourceTransactionManager tccTransactionManager(
            @Qualifier("tccDataSource") final DataSource tccDataSource) {
        return new DataSourceTransactionManager(tccDataSource);
    }

    /**
     * 创建 TCC 专用 JDBC 模板。
     *
     * @param tccDataSource TCC 数据源
     * @return 绑定 TCC 数据源的模板
     */
    @Bean("tccJdbcTemplate")
    public JdbcTemplate tccJdbcTemplate(@Qualifier("tccDataSource") final DataSource tccDataSource) {
        return new JdbcTemplate(tccDataSource);
    }

    /**
     * 创建 TCC 专用事务模板。
     *
     * @param tccTransactionManager TCC 事务管理器
     * @return 绑定 TCC 事务管理器的事务模板
     */
    @Bean("tccTransactionTemplate")
    public TransactionTemplate tccTransactionTemplate(
            @Qualifier("tccTransactionManager") final DataSourceTransactionManager tccTransactionManager) {
        return new TransactionTemplate(tccTransactionManager);
    }
}
