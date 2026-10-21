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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;
import com.iboot.admin.domain.workflow.repository.LeaveApplicationRepository;
import com.iboot.admin.infrastructure.config.MyBatisConfig;
import com.iboot.admin.infrastructure.interceptor.DataScopeInterceptor;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 请假申请仓储的集成测试。
 *
 * <p>使用 Testcontainers MySQL 与 Flyway 全量迁移，验证保存、查询、状态筛选、
 * 分页与流程实例 ID 唯一约束。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringJUnitConfig(LeaveApplicationRepositoryTest.RepositoryTestConfig.class)
@Testcontainers
class LeaveApplicationRepositoryTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_repository")
            .waitingFor(Wait.forLogMessage(".*mysqld.*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    private LeaveApplicationRepository repository;

    /**
     * 验证保存后可按 ID 与流程实例 ID 查询。
     */
    @Test
    void savesAndFindsByIdAndProcessInstanceId() {
        LeaveApplication saved = repository.save(application(1L, 2L, "instance-" + UUID.randomUUID()));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
        assertThat(repository.findByProcessInstanceId(saved.getProcessInstanceId()))
                .get()
                .extracting(LeaveApplication::getApplicantId)
                .isEqualTo(1L);
    }

    /**
     * 验证按申请人查询、状态筛选与分页。
     */
    @Test
    void findsByApplicantWithStatusAndPagination() {
        LeaveApplication pending = repository.save(application(10L, 20L, "instance-" + UUID.randomUUID()));
        LeaveApplication approved = application(10L, 21L, "instance-" + UUID.randomUUID());
        approved.setStatus(LeaveStatus.APPROVED);
        repository.save(approved);

        List<LeaveApplication> all = repository.findByApplicant(10L, null, 0, 10);
        List<LeaveApplication> pendingOnly = repository.findByApplicant(10L, LeaveStatus.PENDING, 0, 10);

        assertThat(all).hasSize(2);
        assertThat(pendingOnly).hasSize(1);
        assertThat(pendingOnly.get(0).getId()).isEqualTo(pending.getId());
        assertThat(repository.countByApplicant(10L, null)).isEqualTo(2);
        assertThat(repository.countByApplicant(10L, LeaveStatus.PENDING)).isEqualTo(1);
        assertThat(repository.findByApplicant(10L, null, 1, 1)).hasSize(1);
    }

    /**
     * 验证流程实例 ID 唯一约束。
     */
    @Test
    void rejectsDuplicateProcessInstanceId() {
        String processInstanceId = "instance-" + UUID.randomUUID();
        repository.save(application(30L, 31L, processInstanceId));

        assertThatThrownBy(() -> repository.save(application(32L, 33L, processInstanceId)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    private LeaveApplication application(Long applicantId, Long approverId, String processInstanceId) {
        LeaveApplication application = new LeaveApplication();
        application.setApplicantId(applicantId);
        application.setApproverId(approverId);
        application.setLeaveType("ANNUAL");
        application.setStartTime(LocalDateTime.of(2026, 11, 2, 9, 0));
        application.setEndTime(LocalDateTime.of(2026, 11, 3, 18, 0));
        application.setReason("测试请假");
        application.setStatus(LeaveStatus.PENDING);
        application.setProcessInstanceId(processInstanceId);
        application.setCreateBy("tester");
        application.setCreateTime(LocalDateTime.now());
        return application;
    }

    /**
     * 仓储测试上下文：数据源、Flyway 与 MyBatis。
     */
    @Configuration
    @Import({MyBatisConfig.class, DataScopeInterceptor.class, LeaveApplicationRepositoryImpl.class})
    static class RepositoryTestConfig {

        /**
         * 执行 Flyway 迁移并返回数据源。
         *
         * @return 数据源
         */
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                    + "/iboot_repository?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            dataSource.setUsername("root");
            dataSource.setPassword("root");
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            return dataSource;
        }
    }
}
