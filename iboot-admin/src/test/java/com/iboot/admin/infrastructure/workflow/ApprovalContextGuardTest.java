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

package com.iboot.admin.infrastructure.workflow;

import com.iboot.admin.infrastructure.config.AsyncConfig;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flowable 与项目异步执行器共存性的上下文守卫测试。
 *
 * <p>只装配 {@link AsyncConfig}、Flowable 流程引擎自动配置与测试数据源，验证
 * {@code taskExecutor} 不再与 Flowable 的同名 Bean 冲突。该测试是 Spike 报告中
 * "适配被回退则应用无法启动" 风险的回归门禁。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = ApprovalContextGuardTest.GuardConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "flowable.database-schema-update=true",
                "flowable.async-executor-activate=false",
                "flowable.idm.enabled=false",
                "flowable.eventregistry.enabled=false",
                "flowable.check-process-definitions=false",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration"
        })
@Testcontainers
class ApprovalContextGuardTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_guard")
            .waitingFor(Wait.forLogMessage(".*mysqld.*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    private ApplicationContext context;

    /**
     * 验证异步执行器与 Flowable 自动配置可以同时注册。
     */
    @Test
    void asyncExecutorAndFlowableCoexist() {
        assertThat(context.getBeansOfType(ProcessEngine.class)).hasSize(1);
        assertThat(context.getBean("taskExecutor")).isInstanceOf(AsyncTaskExecutor.class);
    }

    /**
     * 最小上下文配置：启用自动配置，并提供指向测试容器的数据源。
     */
    @Configuration
    @EnableAutoConfiguration
    @Import(AsyncConfig.class)
    static class GuardConfig {

        /**
         * 创建指向 Testcontainers MySQL 的数据源。
         *
         * @return 数据源
         */
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                    + "/iboot_guard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            dataSource.setUsername("root");
            dataSource.setPassword("root");
            return dataSource;
        }
    }
}
