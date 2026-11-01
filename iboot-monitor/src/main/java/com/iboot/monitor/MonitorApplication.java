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

package com.iboot.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 监控与日志服务启动类。
 *
 * <p>承载 {@code /api/log/**}、{@code /api/online/**} 与
 * {@code /api/monitor/{server,redis,minio}/**}：登录、操作、运行日志的查询与清理，
 * 在线会话的查看与强制下线，以及服务与中间件资源检查。鉴权、响应信封与操作日志
 * 复用 {@code iboot-kernel}。
 *
 * <p>审计表 {@code sys_operate_log} 与 {@code sys_login_log} 归本服务的
 * 共享库中的审计表，其他服务以同库写入的方式提交审计记录。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class MonitorApplication {

    /**
     * 启动监控与日志服务。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
