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

package com.iboot.admin.application.service;

import com.iboot.admin.application.service.notification.NotificationApplicationService;
import com.iboot.kernel.web.BusinessException;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.domain.workflow.model.LeaveApplication;
import com.iboot.admin.domain.workflow.model.LeaveStatus;
import com.iboot.admin.domain.workflow.repository.LeaveApplicationRepository;
import com.iboot.admin.domain.workflow.service.WorkflowService;
import com.iboot.admin.infrastructure.config.MyBatisConfig;
import com.iboot.admin.infrastructure.interceptor.DataScopeInterceptor;
import com.iboot.admin.infrastructure.interceptor.DataScopeSqlBuilder;
import com.iboot.admin.infrastructure.repository.LeaveApplicationRepositoryImpl;
import com.iboot.admin.infrastructure.workflow.FlowableWorkflowAdapter;
import org.flowable.engine.RuntimeService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * {@link ApprovalApplicationService} 的集成测试。
 *
 * <p>在真实 MySQL + Flowable 上验证发起、待办隔离、通过/驳回、详情可见性与事务回滚。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = ApprovalApplicationServiceIntegrationTest.IntegrationConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "flowable.database-schema-update=false",
                "flowable.async-executor-activate=false",
                "flowable.idm.enabled=false",
                "flowable.eventregistry.enabled=false",
                "flowable.check-process-definitions=true",
                "spring.flyway.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration,"
                        + "de.codecentric.boot.admin.server.config.AdminServerNotifierAutoConfiguration"
        })
@Testcontainers
class ApprovalApplicationServiceIntegrationTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_approval_service")
            .waitingFor(Wait.forLogMessage(".*mysqld.*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    private ApprovalApplicationService service;

    @Autowired
    private LeaveApplicationRepository repository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private RuntimeService runtimeService;

    /**
     * 验证发起、待办隔离、通过与详情历史。
     */
    @Test
    void submitTodoApproveAndDetail() {
        LeaveApplication application = submit(1L, 2L);

        assertThat(application.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(service.findTodo(2L, 1, 10).getData())
                .extracting(item -> item.getApplication().getId())
                .contains(application.getId());
        assertThat(service.findTodo(3L, 1, 10).getData())
                .extracting(item -> item.getApplication().getId())
                .doesNotContain(application.getId());

        service.approve(application.getId(), 2L, "bob", "同意");

        LeaveApplication updated = repository.findById(application.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(updated.getApproveComment()).isEqualTo("同意");
        assertThat(service.detail(application.getId(), 1L, false).getHistory()).hasSize(2);
        assertThat(service.findDone(2L, 1, 10).getData())
                .extracting(item -> item.getApplication().getId())
                .contains(application.getId());
    }

    /**
     * 验证驳回路径与驳回意见。
     */
    @Test
    void rejectPersistsComment() {
        LeaveApplication application = submit(11L, 12L);

        service.reject(application.getId(), 12L, "bob", "材料不全");

        LeaveApplication updated = repository.findById(application.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(updated.getApproveComment()).isEqualTo("材料不全");
    }

    /**
     * 验证非审批人操作被拒绝。
     */
    @Test
    void nonAssigneeCannotApprove() {
        LeaveApplication application = submit(21L, 22L);

        assertThatThrownBy(() -> service.approve(application.getId(), 23L, "eve", "同意"))
                .isInstanceOf(AccessDeniedException.class);
    }

    /**
     * 验证流程启动后发生异常时业务记录与流程实例一起回滚。
     */
    @Test
    void rollsBackBusinessAndProcessOnFailure() {
        long before = runtimeService.createProcessInstanceQuery().processDefinitionKey("leaveApproval").count();
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("boom");
        }).when(workflowService).startProcess(999L, 2L);

        assertThatThrownBy(() -> submit(999L, 2L)).isInstanceOf(IllegalStateException.class);

        assertThat(repository.countByApplicant(999L, null)).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("leaveApproval").count())
                .isEqualTo(before);
    }

    private LeaveApplication submit(Long applicantId, Long approverId) {
        return service.submit(applicantId, "user-" + applicantId, approverId, "ANNUAL",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), "测试请假");
    }

    /**
     * 集成测试上下文：真实 MySQL/Flowable/MyBatis，用户仓储使用 Mock。
     */
    @Configuration
    @EnableAutoConfiguration
    @Import({MyBatisConfig.class, DataScopeInterceptor.class, DataScopeSqlBuilder.class,
            UserDataScopeResolver.class, LeaveApplicationRepositoryImpl.class,
            FlowableWorkflowAdapter.class, ApprovalApplicationService.class})
    static class IntegrationConfig {

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
                    + "/iboot_approval_service?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
            dataSource.setUsername("root");
            dataSource.setPassword("root");
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            return dataSource;
        }

        /**
         * 提供通知编排服务的 Mock，避免集成测试触发真实外部渠道。
         *
         * @return 通知编排服务
         */
        @Bean
        NotificationApplicationService notificationApplicationService() {
            return mock(NotificationApplicationService.class);
        }

        /**
         * 提供所有用户均启用的仓储 Mock。
         *
         * @return 用户仓储
         */
        @Bean
        UserRepository userRepository() {
            UserRepository mockRepository = mock(UserRepository.class);
            when(mockRepository.findById(anyLong())).thenAnswer(invocation -> {
                User user = new User();
                user.setId(invocation.getArgument(0));
                user.setStatus(1);
                return Optional.of(user);
            });
            return mockRepository;
        }

        /**
         * 用 Spy 包装真实适配器，便于在回滚测试中制造异常。
         *
         * @param adapter Flowable 适配器
         * @return 工作流服务
         */
        @Bean
        @Primary
        WorkflowService workflowService(final FlowableWorkflowAdapter adapter) {
            return spy(adapter);
        }
    }
}
