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

import org.springframework.http.server.reactive.SslInfo;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.security.cert.X509Certificate;

/**
 * WebFlux 请求的客户端证书身份提取。
 *
 * <p>Reactor Netty 完成双向 TLS 后，远端证书可从
 * {@link ServerHttpRequest#getSslInfo()} 取得；过滤器只读取该信息。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class WebFluxClientCertificateIdentity {

    private WebFluxClientCertificateIdentity() {
    }

    /**
     * 提取调用方 CN。
     *
     * @param request WebFlux 请求
     * @return CN；没有证书或无法解析时返回 {@code null}
     */
    public static String from(final ServerHttpRequest request) {
        SslInfo sslInfo = request.getSslInfo();
        if (sslInfo == null) {
            return null;
        }
        X509Certificate[] certificates = sslInfo.getPeerCertificates();
        if (certificates == null || certificates.length == 0) {
            return null;
        }
        return ClientCertificateIdentity.from(certificates[0]);
    }
}
