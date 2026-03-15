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

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全白名单配置测试类
 * 
 * @author iBoot
 */
@SpringBootTest
class SecurityWhitelistConfigTest {

    @Test
    void testWhitelistPathsExist() {
        // 验证白名单路径数组不为空
        assertNotNull(SecurityWhitelistConfig.WHITE_LIST_PATHS);
        assertTrue(SecurityWhitelistConfig.WHITE_LIST_PATHS.length > 0);
    }

    @Test
    void testCommonPathsAreWhitelisted() {
        // 测试常见的白名单路径
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/api/auth/login"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/swagger-ui/index.html"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/v3/api-docs"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/favicon.ico"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/actuator/health"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/druid/index.html"));
    }

    @Test
    void testStaticResourcePatterns() {
        // 测试静态资源通配符路径
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/static/css/app.css"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/static/js/app.js"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/images/logo.png"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/assets/favicon.ico"));
    }

    @Test
    void testNonWhitelistedPaths() {
        // 测试不应该在白名单中的路径
        assertFalse(SecurityWhitelistConfig.isWhitelisted("/api/users"));
        assertFalse(SecurityWhitelistConfig.isWhitelisted("/admin/dashboard"));
        assertFalse(SecurityWhitelistConfig.isWhitelisted("/private/data"));
    }

    @Test
    void testWildcardMatching() {
        // 测试通配符匹配
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/swagger-ui/some-page"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/v3/api-docs/swagger-config"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/any/path/file.png"));
        assertTrue(SecurityWhitelistConfig.isWhitelisted("/any/path/style.css"));
    }
}