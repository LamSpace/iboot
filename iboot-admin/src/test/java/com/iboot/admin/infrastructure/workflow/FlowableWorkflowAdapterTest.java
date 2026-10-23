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

import com.iboot.admin.domain.workflow.model.WorkflowHistoryItem;
import com.iboot.admin.domain.workflow.model.WorkflowTask;
import com.iboot.admin.domain.workflow.service.WorkflowService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FlowableWorkflowAdapter} 的集成测试。
 *
 * <p>在 Flyway 迁移后的空库上启动 Flowable 引擎，验证流程部署、任务分派、
 * 通过/驳回路径与历史查询。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = FlowableWorkflowAdapterTest.AdapterTestConfig.class,
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
class FlowableWorkflowAdapterTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_workflow")
            .waitingFor(Wait.forLogMessage(".*mysqld.*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    @Autowired
    private WorkflowService workflowService;

    /**
     * 验证启动流程后生成指派给审批人的任务。
     */
    @Test
    void startsProcessAndAssignsTask() {
        String instanceId = workflowService.startProcess(100L, 200L);

        WorkflowTask task = workflowService.findActiveTask(instanceId);
        assertThat(task).isNotNull();
        assertThat(task.getAssigneeId()).isEqualTo(200L);
        assertThat(workflowService.countActiveTasks(200L)).isGreaterThanOrEqualTo(1);
        assertThat(workflowService.findActiveTasks(200L, 0, 10))
                .extracting(WorkflowTask::getProcessInstanceId)
                .contains(instanceId);
    }

    /**
     * 验证通过路径结束流程并记录历史。
     */
    @Test
    void approveCompletesProcess() {
        String instanceId = workflowService.startProcess(101L, 201L);
        WorkflowTask task = workflowService.findActiveTask(instanceId);

        workflowService.completeTask(task.getTaskId(), true, "同意");

        assertThat(workflowService.findActiveTask(instanceId)).isNull();
        assertThat(workflowService.countCompletedTasks(201L)).isGreaterThanOrEqualTo(1);
        List<WorkflowHistoryItem> history = workflowService.findHistory(instanceId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getActorId()).isEqualTo(201L);
        assertThat(history.get(0).getTime()).isNotNull();
    }

    /**
     * 验证驳回路径结束流程。
     */
    @Test
    void rejectCompletesProcess() {
        String instanceId = workflowService.startProcess(102L, 202L);
        WorkflowTask task = workflowService.findActiveTask(instanceId);

        workflowService.completeTask(task.getTaskId(), false, "材料不全");

        assertThat(workflowService.findActiveTask(instanceId)).isNull();
        assertThat(workflowService.findCompletedTasks(202L, 0, 10))
                .extracting(WorkflowHistoryItem::getProcessInstanceId)
                .contains(instanceId);
    }

    /**
     * 测试上下文：数据源、Flyway 与工作流适配器。
     */
    @Configuration
    @EnableAutoConfiguration
    @Import(FlowableWorkflowAdapter.class)
    static class AdapterTestConfig {

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
                    + "/iboot_workflow?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
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
