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

package com.iboot.workflow.infrastructure.config;

import org.apache.seata.integration.http.JakartaSeataWebMvcConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Seata 资源方的 MVC 配置。
 *
 * <p>审批服务既发起全局事务，也作为 Flowable 与申请状态写入的资源方。注册 Jakarta
 * 版本的 XID 传播拦截器，使进入本服务的 {@code TX_XID} 在 Controller 与本地事务
 * 开启前绑定到 {@code RootContext}，并在请求结束后解绑。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(prefix = "seata", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeataWebConfig {

    /**
     * 注册 Seata 的 MVC 拦截器。
     *
     * @return Web MVC 配置器
     */
    @Bean
    public WebMvcConfigurer seataWebMvcConfigurer() {
        return new JakartaSeataWebMvcConfigurer();
    }
}
