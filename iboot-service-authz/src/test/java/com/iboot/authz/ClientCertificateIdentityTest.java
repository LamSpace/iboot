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

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 客户端证书身份提取测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ClientCertificateIdentityTest {

    @Test
    void extractsCommonNameFromDistinguishedName() {
        assertThat(ClientCertificateIdentity.fromDistinguishedName("CN=iboot-system,O=iBoot"))
                .isEqualTo("iboot-system");
        assertThat(ClientCertificateIdentity.fromDistinguishedName("O=iBoot"))
                .isNull();
        assertThat(ClientCertificateIdentity.fromDistinguishedName("not-a-dn"))
                .isNull();
    }

    @Test
    void readsCommonNameFromCertificate() {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=iboot-gateway,O=iBoot"));

        assertThat(ClientCertificateIdentity.from(certificate)).isEqualTo("iboot-gateway");
        assertThat(ClientCertificateIdentity.from((X509Certificate) null)).isNull();
    }

    @Test
    void prefersSpiffeIdOverCommonName() throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=iboot-file,O=iBoot"));
        java.util.Collection<java.util.List<?>> sans = new java.util.ArrayList<>();
        sans.add(java.util.List.of(6, "spiffe://iboot.local/service/iboot-system"));
        when(certificate.getSubjectAlternativeNames()).thenReturn(sans);

        assertThat(ClientCertificateIdentity.from(certificate)).isEqualTo("iboot-system");
    }

    @Test
    void extractsSpiffeClientIdentity() throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        java.util.Collection<java.util.List<?>> sans = new java.util.ArrayList<>();
        sans.add(java.util.List.of(6, "spiffe://iboot.local/client/prometheus"));
        when(certificate.getSubjectAlternativeNames()).thenReturn(sans);

        assertThat(ClientCertificateIdentity.from(certificate)).isEqualTo("prometheus");
    }

    @Test
    void ignoresUnknownTrustDomainAndFallsBackToCommonName() throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getSubjectX500Principal())
                .thenReturn(new X500Principal("CN=iboot-gateway,O=iBoot"));
        java.util.Collection<java.util.List<?>> sans = new java.util.ArrayList<>();
        sans.add(java.util.List.of(6, "spiffe://other.local/service/iboot-system"));
        when(certificate.getSubjectAlternativeNames()).thenReturn(sans);

        assertThat(ClientCertificateIdentity.from(certificate)).isEqualTo("iboot-gateway");
    }
}
