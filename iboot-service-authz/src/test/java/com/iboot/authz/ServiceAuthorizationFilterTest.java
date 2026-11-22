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

package com.iboot.authz;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Servlet 服务间授权过滤器测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ServiceAuthorizationFilterTest {

    @Test
    void disabledFilterPassesThroughWithoutCertificate() throws Exception {
        ServiceAuthorizationProperties properties = properties(false, "iboot-gateway");
        ServiceAuthorizationFilter filter = new ServiceAuthorizationFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void deniesCallerOutsideWhitelist() throws Exception {
        ServiceAuthorizationProperties properties = properties(true, "iboot-gateway");
        ServiceAuthorizationFilter filter = new ServiceAuthorizationFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/list");
        request.setAttribute("jakarta.servlet.request.X509Certificate",
                new X509Certificate[]{certificate("CN=iboot-file,O=iBoot")});
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("调用方无权访问");
    }

    @Test
    void allowsWhitelistedCaller() throws Exception {
        ServiceAuthorizationProperties properties = properties(true, "iboot-gateway");
        ServiceAuthorizationFilter filter = new ServiceAuthorizationFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/list");
        request.setAttribute("jakarta.servlet.request.X509Certificate",
                new X509Certificate[]{certificate("CN=iboot-gateway,O=iBoot")});
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private static ServiceAuthorizationProperties properties(final boolean enabled, final String caller) {
        ServiceAuthorizationRule rule = new ServiceAuthorizationRule();
        rule.setPaths(List.of("/api/**"));
        rule.setCallers(List.of(caller));
        ServiceAuthorizationProperties properties = new ServiceAuthorizationProperties();
        properties.setEnabled(enabled);
        properties.setRules(List.of(rule));
        return properties;
    }

    private static X509Certificate certificate(final String distinguishedName) {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal(distinguishedName));
        return certificate;
    }
}
