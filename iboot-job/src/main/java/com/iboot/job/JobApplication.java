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

package com.iboot.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 调度服务启动类。
 *
 * <p>承载 {@code /api/job/**}（定时任务管理与执行）与 {@code /api/monitor/backup/**}
 * （数据备份与恢复演练），通过网关对外提供，鉴权与操作日志复用 {@code iboot-kernel}。
 *
 * <p>本服务是项目里第一个时间驱动的部署单元：Quartz 以集群模式运行，任务只调度本进程内的
 * Bean；管理端与文件、通知服务不在此进程内。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class JobApplication {

    /**
     * 启动调度服务。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(JobApplication.class, args);
    }
}
