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

import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Quartz 配置类 确保 Quartz 使用 Spring 管理的 DataSource 和事务管理器
 *
 * @author iBoot
 */
@Configuration
public class QuartzConfig {

    /**
     * 自定义 SchedulerFactoryBean 配置 将 Spring 管理的 DataSource 和事务管理器注入到 Quartz
     */
    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer(DataSource dataSource,
                                                                         PlatformTransactionManager transactionManager) {
        return schedulerFactoryBean -> {
            schedulerFactoryBean.setDataSource(dataSource);
            schedulerFactoryBean.setTransactionManager(transactionManager);
        };
    }

}
