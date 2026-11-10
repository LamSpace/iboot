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

import com.alibaba.druid.spring.boot4.autoconfigure.DruidDataSourceWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 主数据源配置。
 *
 * <p>本服务存在主数据源与 TCC 数据源两个连接池，Druid starter 的
 * {@code @ConditionalOnMissingBean} 会因任一 {@code DataSource} 存在而退让，
 * 因此在这里显式声明主数据源，保持它仍为 {@code @Primary} 并继续被 Seata AT 自动代理。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see TccDataSourceConfig
 */
@Configuration
public class PrimaryDataSourceConfig {

    /**
     * 声明主数据源。
     *
     * <p>绑定 {@code spring.datasource.druid} 前缀，行为与 Druid starter 默认创建的一致，
     * 包括初始化连接池与注册过滤器。
     *
     * @return Druid 主数据源
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.druid")
    public DruidDataSourceWrapper dataSource() {
        return new DruidDataSourceWrapper();
    }

    /**
     * 声明主 JDBC 模板。
     *
     * <p>本服务还存在 {@code tccJdbcTemplate}，Spring Boot 的
     * {@code JdbcTemplateAutoConfiguration} 会因已有 {@code JdbcOperations} 而整体退让。
     * 这里显式提供主模板并标记 {@code @Primary}，保证业务仓储仍使用被 Seata AT 代理的数据源。
     *
     * @param dataSource 主数据源
     * @return 主 JDBC 模板
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("dataSource") final DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
