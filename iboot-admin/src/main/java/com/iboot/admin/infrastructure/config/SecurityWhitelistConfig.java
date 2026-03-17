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

import org.springframework.stereotype.Component;

/**
 * 安全白名单配置类 统一管理不需要身份验证的路径配置
 *
 * @author iBoot
 */
@Component
public class SecurityWhitelistConfig {

    /**
     * 白名单路径 - 不需要身份验证的路径
     */
    public static final String[] WHITE_LIST_PATHS = {
            // 认证相关路径
            "/api/auth/login", "/api/auth/logout", "/api/auth/register",

            // 公开配置接口
            "/api/config/public",

            // Swagger UI 相关路径
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs", "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config", "/swagger-resources/**", "/webjars/**", "/doc.html",

            // 静态资源路径
            "/favicon.ico", "/error", "/static/**",

            // 根路径静态资源（通配符）
            "/**/*.ico", "/**/*.png", "/**/*.jpg", "/**/*.jpeg", "/**/*.gif", "/**/*.css", "/**/*.js",

            // 监控相关路径
            "/actuator/**", "/druid/**",

            // Spring Boot Admin 路径
            "/sba/**", "/instances/**", "/applications/**"};

    /**
     * 检查给定路径是否在白名单中
     *
     * @param requestURI 请求路径
     *
     * @return 是否在白名单中
     */
    public static boolean isWhitelisted(String requestURI) {
        for (String pattern : WHITE_LIST_PATHS) {
            if (matchesPattern(requestURI, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查路径是否匹配模式
     *
     * @param path    路径
     * @param pattern 模式
     *
     * @return 是否匹配
     */
    private static boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            // 处理 /** 通配符
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        } else if (pattern.startsWith("/**/*.")) {
            // 处理文件扩展名通配符
            String extension = pattern.substring(5); // 去掉 /**/.
            return path.endsWith(extension);
        } else if (pattern.contains("*")) {
            // 简单的前缀匹配
            String prefix = pattern.substring(0, pattern.indexOf('*'));
            return path.startsWith(prefix);
        } else {
            // 精确匹配
            return path.equals(pattern);
        }
    }

}