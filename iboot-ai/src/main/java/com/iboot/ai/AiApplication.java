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

package com.iboot.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * iBoot AI 服务入口。
 *
 * <p>独立于 {@code iboot-admin} 运行，承载对话、会话记忆与用量观测。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class AiApplication {

    /**
     * 启动 AI 服务。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
