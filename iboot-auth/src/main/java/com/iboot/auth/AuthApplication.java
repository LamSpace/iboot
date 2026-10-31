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

package com.iboot.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证服务启动类。
 *
 * <p>承载 {@code /api/auth/login|logout|register}，负责校验用户名密码、签发令牌并把会话写入
 * 共享 Redis；不解析令牌，也不承担数据权限范围查询（{@code /api/auth/scope} 仍在管理端）。
 *
 * <p>过渡期直连 {@code iboot_admin} 库读写用户、角色、菜单、参数与登录日志，
 * 会话键与签名密钥与其余服务保持一致，因此网关改回路由即可回滚。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class AuthApplication {

    /**
     * 启动认证服务。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
