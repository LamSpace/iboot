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

package com.iboot.monitor.interfaces.controller;

import com.iboot.kernel.security.JwtSecurityConfig;
import com.iboot.kernel.security.JwtTokenUtil;
import com.iboot.monitor.application.service.LoginLogApplicationService;
import com.iboot.monitor.application.service.OperateLogApplicationService;
import com.iboot.monitor.application.service.RunLogApplicationService;
import com.iboot.monitor.domain.monitor.model.LoginLog;
import com.iboot.monitor.infrastructure.config.ApiVersionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link LogController} 的接口测试。
 *
 * <p>验证端点映射、权限码与响应结构：列表返回分页结果，缺少权限时拒绝访问。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@WebMvcTest(controllers = LogController.class)
@Import({ApiVersionConfig.class, LogControllerTest.SecurityTestConfig.class})
class LogControllerTest {

    private static final String VERSION_HEADER = "Accept-Version";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginLogApplicationService loginLogApplicationService;

    @MockitoBean
    private OperateLogApplicationService operateLogApplicationService;

    @MockitoBean
    private RunLogApplicationService runLogApplicationService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private JwtSecurityConfig jwtSecurityConfig;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 有权限时返回登录日志分页结构。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void loginLogListReturnsPage() throws Exception {
        when(loginLogApplicationService.getLoginLogPage(anyInt(), anyInt())).thenReturn(List.of(loginLog()));
        when(loginLogApplicationService.countLoginLogs()).thenReturn(1L);

        mockMvc.perform(get("/api/log/login/list")
                        .with(authentication(auth("loginlog:list")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.total").value(1))
                .andExpect(jsonPath("$.data.data.data[0].username").value("admin"));
    }

    /**
     * 缺少权限码时拒绝访问。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void loginLogListRequiresPermission() throws Exception {
        mockMvc.perform(get("/api/log/login/list")
                        .with(authentication(auth("other:list")))
                        .header(VERSION_HEADER, "1"))
                .andExpect(status().isForbidden());
    }

    private LoginLog loginLog() {
        LoginLog log = new LoginLog();
        log.setId(1L);
        log.setUsername("admin");
        log.setIpAddress("127.0.0.1");
        log.setStatus(1);
        log.setMessage("登录成功");
        log.setLoginTime(LocalDateTime.of(2026, 11, 1, 10, 0, 0));
        return log;
    }

    private Authentication auth(String authority) {
        return new UsernamePasswordAuthenticationToken("admin", "n/a",
                List.of(new SimpleGrantedAuthority(authority)));
    }

    /**
     * 测试用安全配置：关闭 CSRF 并启用方法级权限。
     */
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfig {

        /**
         * 放行请求，权限由 {@code @PreAuthorize} 判定。
         *
         * @param http 安全链构建器
         * @return 安全过滤器链
         * @throws Exception 构建失败时抛出
         */
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }
    }
}
