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

import jakarta.servlet.http.HttpServletRequest;

import java.security.cert.X509Certificate;

/**
 * Servlet 请求的客户端证书身份提取。
 *
 * <p>Tomcat 在完成双向 TLS 后把对端证书放入
 * {@code jakarta.servlet.request.X509Certificate}；过滤器只读取该属性，不接受
 * 请求头或代理注入的身份。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ServletClientCertificateIdentity {

    private static final String CERTIFICATE_ATTRIBUTE = "jakarta.servlet.request.X509Certificate";

    private ServletClientCertificateIdentity() {
    }

    /**
     * 提取调用方 CN。
     *
     * @param request Servlet 请求
     * @return CN；没有证书或无法解析时返回 {@code null}
     */
    public static String from(final HttpServletRequest request) {
        Object attribute = request.getAttribute(CERTIFICATE_ATTRIBUTE);
        if (attribute instanceof X509Certificate[] certificates && certificates.length > 0) {
            return ClientCertificateIdentity.from(certificates[0]);
        }
        return null;
    }
}
