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

package com.iboot.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 消息与通知服务启动类。
 *
 * <p>阶段 2 的业务拆分单元：承载消息中心、公告、消息模板、通知投递编排、
 * SSE 实时推送与内容检索，通过网关对外提供 {@code /api/message/**}、
 * {@code /api/notice/**}、{@code /api/notification/**}、{@code /api/push/**}
 * 与 {@code /api/search}。鉴权与操作日志复用 {@code iboot-kernel}。
 *
 * <p>过渡期与管理端共用现有 schema 的六张表，不启用 Flyway；网关路由回退即可回滚。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
public class NotifyApplication {

    /**
     * 启动消息与通知服务。
     *
     * @param args 命令行参数
     */
    public static void main(final String[] args) {
        SpringApplication.run(NotifyApplication.class, args);
    }
}
