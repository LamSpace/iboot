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

package com.iboot.admin.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc OpenAPI 配置。
 *
 * <p>定义 API 文档的基本信息、服务器地址和全局安全方案，供 Swagger UI 展示。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    /**
     * 创建 OpenAPI 文档定义。
     *
     * @return OpenAPI 配置，包含标题、版本、联系人和服务器信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("本地开发环境");

        return new OpenAPI()
                .openapi("3.1.0")
                .info(new Info().title("iBoot 后台管理系统 API")
                        .version("1.0.0")
                        .description("基于 Spring Boot 4.0 + MyBatis 的后台管理系统")
                        .contact(new Contact().name("iBoot Team").email("support@iboot.com").url("https://iboot.com"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(server))
                .components(new Components());
    }

    /**
     * 创建包含所有接口的 API 分组。
     *
     * @return 分组配置
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }

}
