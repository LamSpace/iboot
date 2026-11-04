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

package com.iboot.ops.infrastructure.config;

import com.iboot.ops.OpsApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 运维控制台安全链的契约测试。
 *
 * <p>固定三件事：控制台页面未认证时被拒绝、认证后可访问、健康端点保持匿名；
 * 同时验证密码缺失时进程在启动阶段失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@SpringBootTest(classes = OpsApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "iboot.ops.security.password=test-pass",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.boot.admin.discovery.enabled=false"
})
class OpsSecurityConfigTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * 未认证访问控制台页面应被拒绝。
     *
     * @throws Exception 请求执行失败时抛出
     */
    @Test
    void unauthenticatedConsoleIsRejected() throws Exception {
        assertThat(get("/sba", null).statusCode()).isEqualTo(401);
    }

    /**
     * 带正确凭证访问控制台页面应成功。
     *
     * @throws Exception 请求执行失败时抛出
     */
    @Test
    void authenticatedConsoleIsServed() throws Exception {
        assertThat(get("/sba", basicAuth("ops", "test-pass")).statusCode()).isEqualTo(200);
    }

    /**
     * 健康端点应保持匿名可达。
     *
     * @throws Exception 请求执行失败时抛出
     */
    @Test
    void healthEndpointIsPublic() throws Exception {
        assertThat(get("/actuator/health", null).statusCode()).isEqualTo(200);
    }

    /**
     * 密码缺失时应拒绝创建登录用户。
     */
    @Test
    void missingPasswordFailsFast() {
        OpsSecurityProperties properties = new OpsSecurityProperties();
        properties.setPassword("");

        assertThatThrownBy(() -> new OpsSecurityConfig()
                .opsUserDetailsService(properties, new BCryptPasswordEncoder()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IBOOT_OPS_PASSWORD");
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path))
                .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
