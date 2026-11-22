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
import java.security.cert.X509Certificate;

/**
 * 从已校验的客户端证书提取调用方身份。
 *
 * <p>只读取证书 Subject 的 CN，不解析证书链，也不信任任何请求头。Servlet 与
 * WebFlux 的证书读取分别在 {@link ServletClientCertificateIdentity} 与
 * {@link WebFluxClientCertificateIdentity} 中完成，避免把 WebFlux 依赖带入
 * Servlet 宿主。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ClientCertificateIdentity {

    private ClientCertificateIdentity() {
    }

    /**
     * 从证书提取 CN。
     *
     * @param certificate 客户端证书
     * @return CN；缺失或无法解析时返回 {@code null}
     */
    public static String from(final X509Certificate certificate) {
        if (certificate == null) {
            return null;
        }
        return fromDistinguishedName(certificate.getSubjectX500Principal().getName());
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
