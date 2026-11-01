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

package com.iboot.monitor.infrastructure.config;

import com.iboot.monitor.infrastructure.interceptor.DataScopeInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis 配置。
 *
 * <p>只扫描本服务的 Mapper 包，注册 {@link SqlSessionFactory} 加载 {@code mapper} 目录下的
 * XML 映射文件，并挂载 {@link DataScopeInterceptor}：登录日志与操作日志的列表和导出查询
 * 带 {@code @DataScope} 标记，需要按当前用户的数据范围过滤。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
@MapperScan("com.iboot.monitor.infrastructure.persistence.mapper")
public class MyBatisConfig {

    /**
     * 创建 MyBatis {@link SqlSessionFactory}。
     *
     * @param dataSource 数据源
     * @param dataScopeInterceptor 数据权限拦截器
     * @return SqlSessionFactory 实例
     * @throws Exception 初始化失败时抛出
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, DataScopeInterceptor dataScopeInterceptor)
            throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        sessionFactory.setConfiguration(configuration);

        sessionFactory.setPlugins(new Interceptor[]{dataScopeInterceptor});
        sessionFactory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
        return sessionFactory.getObject();
    }
}
