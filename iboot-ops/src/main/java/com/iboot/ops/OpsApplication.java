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

package com.iboot.ops;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 运维控制台启动类。
 *
 * <p>独立于业务服务的可部署进程：通过注册中心发现实例，聚合各实例的 Actuator 信息，
 * 提供单实例巡检与运行期日志级别调整。本服务不连接业务数据库，也不依赖 {@code iboot-kernel}；
 * 访问控制由控制台自身的 Basic 认证承担，不复用门户的 JWT 安全链。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableAdminServer
public class OpsApplication {

    /**
     * 启动运维控制台。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OpsApplication.class, args);
    }
}
