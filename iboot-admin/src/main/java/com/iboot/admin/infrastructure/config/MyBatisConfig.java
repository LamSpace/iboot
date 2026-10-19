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

package com.iboot.admin.infrastructure.config;

import com.iboot.admin.infrastructure.interceptor.DataScopeInterceptor;
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
 * <p>注册 {@link org.apache.ibatis.session.SqlSessionFactory} 并挂载数据权限拦截器，
 * 同时扫描 {@code com.iboot.admin.infrastructure.persistence.mapper} 下的 Mapper 接口。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.admin.infrastructure.interceptor.DataScopeInterceptor
 */
@Configuration
@MapperScan("com.iboot.admin.infrastructure.persistence.mapper")
public class MyBatisConfig {

    /**
     * 创建 MyBatis {@link SqlSessionFactory}。
     *
     * <p>注册数据权限拦截器，开启驼峰映射、二级缓存和延迟加载，并加载
     * {@code mapper} 目录下的 XML 映射文件。
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

        // 注册数据权限拦截器
        sessionFactory.setPlugins(dataScopeInterceptor);

        // 设置 MyBatis 配置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        // 驼峰命名转换
        configuration.setMapUnderscoreToCamelCase(true);
        // 开启二级缓存
        configuration.setCacheEnabled(true);
        // 延迟加载
        configuration.setLazyLoadingEnabled(true);
        // 按需加载
        configuration.setAggressiveLazyLoading(false);

        sessionFactory.setConfiguration(configuration);

        // 设置 mapper xml 文件位置
        sessionFactory
                .setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));

        return sessionFactory.getObject();
    }

}
