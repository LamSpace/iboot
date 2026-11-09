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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 请假额度客户端配置。
 *
 * <p>复用带负载均衡与超时的构建器，复制一份后加上 XID 拦截器，只影响额度调用，
 * 不改变通知等其它服务间调用的行为。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class LeaveQuotaClientConfig {

    /**
     * 创建指向系统服务的额度客户端。
     *
     * @param builder    负载均衡构建器
     * @param properties 额度客户端配置
     * @return 额度客户端
     */
    @Bean("leaveQuotaRestClient")
    public RestClient leaveQuotaRestClient(
            @Qualifier("loadBalancedRestClientBuilder") final RestClient.Builder builder,
            final WorkflowLeaveQuotaProperties properties) {
        return builder.clone()
                .baseUrl(properties.getBaseUrl())
                .requestInterceptor(new SeataXidClientInterceptor())
                .build();
    }
}
