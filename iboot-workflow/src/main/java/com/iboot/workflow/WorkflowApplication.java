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

package com.iboot.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 审批工作流服务启动类。
 *
 * <p>承载 {@code /api/approval/**} 的请假发起、待办、已办、详情与通过/驳回接口，
 * 通过网关对外提供；鉴权、响应信封与操作日志复用 {@code iboot-kernel}。
 *
 * <p>Flowable 引擎与审批业务表共用 {@code iboot_workflow} 数据源，流程状态与业务记录
 * 在同一本地事务内更新；用户校验跨库只读管理端的 {@code sys_user}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class WorkflowApplication {

    /**
     * 启动审批工作流服务。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(WorkflowApplication.class, args);
    }
}
