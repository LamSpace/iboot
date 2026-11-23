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

package com.iboot.notify.infrastructure.messaging;

import org.apache.rocketmq.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 通知服务侧 RocketMQ 信任管理器兼容层测试。
 *
 * <p>通知服务只消费、不发布，因此这里只验证本地 CA 证书被接受、不受信任证书被拒绝；
 * 实际消费链路由 {@code rocketmq-acl.sh} 与运行中的服务覆盖。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RocketMqServerTrustTest {

    private static final String SERVICE = "iboot-notify";

    private static final String UNTRUSTED_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIDHzCCAgegAwIBAgIUdV5M7eTsLa/Bi1lF4atqViBRNaAwDQYJKoZIhvcNAQEL
            BQAwHzEdMBsGA1UEAwwUaWJvb3QtdW50cnVzdGVkLXRlc3QwHhcNMjYxMTIzMTEx
            MzE2WhcNMzYxMTIwMTExMzE2WjAfMR0wGwYDVQQDDBRpYm9vdC11bnRydXN0ZWQt
            dGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJxpu8XO3VwfThQV
            onCzERDfqsUUks8OGe4zIxCpMcF0VYALmsaw8ss/5kY17btwY6wEcvbTzJeVvEHh
            hDcwuhJDenhjP8bbuSfY2COD053wzzeL3YQXXs7qsw7YYgB/vQBXnpBabq6tj4V7
            2Xr+7EUGcmnK/SwAKYh37wyFclOJkvDY5+z4scEiEPyxGUkezbaym5oqnNYaum6e
            MDW+D/FkHkMlr9+nGyYc1yNEzhIpIDOjjmQnd1PPsHBogE5VdYssg8bsYq6JLKFK
            r0j+YtMHvsVSXMMQTCY3b6RgVJ3kPYQNVBEBFYTbMArArAohUnaj3FWWPM0h0Blt
            OjLdNDsCAwEAAaNTMFEwHQYDVR0OBBYEFNU+K0X2gHyzyLNpEE6bSB7A0MKVMB8G
            A1UdIwQYMBaAFNU+K0X2gHyzyLNpEE6bSB7A0MKVMA8GA1UdEwEB/wQFMAMBAf8w
            DQYJKoZIhvcNAQELBQADggEBAB+jdkgz9nByE7wFEJsfEyVx1VYJHZm01pYwzDCF
            34lJ4BpATAS80mS0PN5ilwJ9llnfMzvK6Q+zgd6yByXYgY37OO4xR+YptyfNe21J
            Vm6Ik8tGH6UyAYmP2qGkU8RIDVsGPZ7yaC+V5AzRLOxlGhWPNUjNz/oaqNeH9COQ
            TLlRVEmKvWK0WIl+DoZcIQVanmvbPQBlJjiF6/iBNNnY+v6xmy+aeufpKTUavpWV
            BubKqN7nSxoUJ/qbamtzD45T7nLgCUUIAYyrybQVhD9uTeKH4b612vFpc5rrYPLp
            rZ9YtGd6WR0IicRhZZeZocbN5N8KiFd8lbhSrTe3yPbwBLw=
            -----END CERTIFICATE-----
            """;

    @Test
    void trustsLocalCaLeaf() throws Exception {
        Path certs = certsDir();
        Assumptions.assumeTrue(Files.exists(certs.resolve("truststore.p12")), "缺少本地 truststore，跳过");
        Assumptions.assumeTrue(System.getProperty("javax.net.ssl.trustStore", "").contains("docker/certs"),
                "JVM 未使用本地 truststore，跳过");
        X509TrustManager trustManager = trustManager();
        X509Certificate leaf = loadCertificate(Files.readString(certs.resolve("services/" + SERVICE + ".pem")));
        trustManager.checkServerTrusted(new X509Certificate[]{leaf}, "RSA");
    }

    @Test
    void rejectsUntrustedCertificate() throws Exception {
        X509TrustManager trustManager = trustManager();
        X509Certificate untrusted = loadCertificate(UNTRUSTED_PEM);
        assertThrows(CertificateException.class,
                () -> trustManager.checkServerTrusted(new X509Certificate[]{untrusted}, "RSA"));
    }

    private static X509TrustManager trustManager() {
        return (X509TrustManager) InsecureTrustManagerFactory.INSTANCE.getTrustManagers()[0];
    }

    private static Path certsDir() {
        return Path.of(System.getProperty("iboot.certs.dir", "../docker/certs")).toAbsolutePath();
    }

    private static X509Certificate loadCertificate(final String pem) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }
}
