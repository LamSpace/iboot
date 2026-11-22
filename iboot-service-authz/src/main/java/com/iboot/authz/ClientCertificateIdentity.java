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

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.net.URI;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

/**
 * 从已校验的客户端证书提取调用方身份。
 *
 * <p>优先读取 URI SAN 中的 SPIFFE ID（信任域 {@value #TRUST_DOMAIN}），路径
 * {@code /service/<name>} 或 {@code /client/<name>} 的最后一段即调用方名；没有可用
 * SPIFFE ID 时回退到 Subject CN。只解析证书，不信任任何请求头。Servlet 与 WebFlux
 * 的证书读取分别在 {@link ServletClientCertificateIdentity} 与
 * {@link WebFluxClientCertificateIdentity} 中完成，避免把 WebFlux 依赖带入
 * Servlet 宿主。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ClientCertificateIdentity {

    private static final String SPIFFE_SCHEME = "spiffe";

    private static final String TRUST_DOMAIN = "iboot.local";

    private static final String SERVICE_PREFIX = "/service/";

    private static final String CLIENT_PREFIX = "/client/";

    private ClientCertificateIdentity() {
    }

    /**
     * 从证书提取调用方身份：SPIFFE ID 优先，CN 回退。
     *
     * @param certificate 客户端证书
     * @return 调用方名；无法解析时返回 {@code null}
     */
    public static String from(final X509Certificate certificate) {
        if (certificate == null) {
            return null;
        }
        String spiffe = fromSpiffe(certificate);
        if (spiffe != null) {
            return spiffe;
        }
        return fromDistinguishedName(certificate.getSubjectX500Principal().getName());
    }

    /**
     * 从 URI SAN 提取 SPIFFE 身份名。
     *
     * @param certificate 客户端证书
     * @return 服务名或客户端名；没有匹配的 SPIFFE ID 时返回 {@code null}
     */
    static String fromSpiffe(final X509Certificate certificate) {
        final Collection<List<?>> subjectAltNames;
        try {
            subjectAltNames = certificate.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            return null;
        }
        if (subjectAltNames == null) {
            return null;
        }
        for (List<?> entry : subjectAltNames) {
            if (entry == null || entry.size() < 2) {
                continue;
            }
            Object type = entry.get(0);
            Object value = entry.get(1);
            if (!(type instanceof Integer) || !Integer.valueOf(6).equals(type) || !(value instanceof String text)) {
                continue;
            }
            String name = fromSpiffeUri(text);
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    private static String fromSpiffeUri(final String text) {
        final URI uri;
        try {
            uri = URI.create(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!SPIFFE_SCHEME.equals(uri.getScheme()) || !TRUST_DOMAIN.equals(uri.getHost())) {
            return null;
        }
        String path = uri.getPath();
        if (path == null) {
            return null;
        }
        if (path.startsWith(SERVICE_PREFIX)) {
            return nonBlank(path.substring(SERVICE_PREFIX.length()));
        }
        if (path.startsWith(CLIENT_PREFIX)) {
            return nonBlank(path.substring(CLIENT_PREFIX.length()));
        }
        return null;
    }

    private static String nonBlank(final String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 从可分辨名称提取 CN。
     *
     * @param distinguishedName X.500 可分辨名称
     * @return CN；缺失或无法解析时返回 {@code null}
     */
    static String fromDistinguishedName(final String distinguishedName) {
        if (distinguishedName == null || distinguishedName.isBlank()) {
            return null;
        }
        try {
            LdapName ldapName = new LdapName(distinguishedName);
            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    Object value = rdn.getValue();
                    return value == null ? null : value.toString();
                }
            }
        } catch (InvalidNameException ignored) {
            return null;
        }
        return null;
    }
}
