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

package com.iboot.workflow.infrastructure.messaging;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;
import org.apache.rocketmq.client.apis.StaticSessionCredentialsProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
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
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RocketMQ 客户端服务端证书校验测试。
 *
 * <p>验证兼容层用本地 CA 信任库接受服务证书、拒绝不受信任证书，并在启用集成开关时实际投递消息。
 * 正向用例需要本地证书与 truststore；缺失时跳过。错误 CA 场景由回归用 JDK cacerts 再跑一次，
 * 通过 {@code IBOOT_ROCKETMQ_EXPECT_FAILURE=true} 断言投递失败。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class RocketMqServerTrustTest {

    private static final String SERVICE = "iboot-workflow";

    private static final String DEFAULT_TOPIC = "iboot-notification";

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
        Assumptions.assumeFalse(expectFailure(), "错误 CA 场景不执行正向断言");
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

    @Test
    void sendsMessageWhenConfigured() throws Exception {
        Assumptions.assumeTrue("true".equals(System.getenv("IBOOT_ROCKETMQ_INTEGRATION")),
                "未开启 RocketMQ 集成测试");
        boolean expectedFailure = expectFailure();
        try {
            ClientConfiguration configuration = clientConfiguration();
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            Message message = provider.newMessageBuilder()
                    .setTopic(topic())
                    .setBody(("trust-test-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8))
                    .build();
            try (Producer producer = provider.newProducerBuilder()
                    .setClientConfiguration(configuration)
                    .setTopics(topic())
                    .build()) {
                producer.send(message);
            }
            assertFalse(expectedFailure, "预期证书校验失败，但消息投递成功");
        } catch (Throwable error) {
            assertTrue(expectedFailure, "消息投递失败：" + error);
        }
    }

    private static X509TrustManager trustManager() {
        return (X509TrustManager) InsecureTrustManagerFactory.INSTANCE.getTrustManagers()[0];
    }

    private static ClientConfiguration clientConfiguration() {
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder()
                .setEndpoints(requireEnv("IBOOT_ROCKETMQ_ENDPOINTS"))
                .enableSsl(true)
                .setRequestTimeout(Duration.ofSeconds(5))
                .setMaxStartupAttempts(1);
        String accessKey = System.getenv("ROCKETMQ_WORKFLOW_ACCESS_KEY");
        String secretKey = System.getenv("ROCKETMQ_WORKFLOW_SECRET_KEY");
        if (accessKey != null && !accessKey.isBlank()) {
            SessionCredentialsProvider credentials =
                    new StaticSessionCredentialsProvider(accessKey, secretKey);
            builder.setCredentialProvider(credentials);
        }
        return builder.build();
    }

    private static Path certsDir() {
        return Path.of(System.getProperty("iboot.certs.dir", "../docker/certs")).toAbsolutePath();
    }

    private static boolean expectFailure() {
        return "true".equals(System.getenv("IBOOT_ROCKETMQ_EXPECT_FAILURE"));
    }

    private static String topic() {
        String topic = System.getenv("IBOOT_ROCKETMQ_TRUST_TOPIC");
        return topic == null || topic.isBlank() ? DEFAULT_TOPIC : topic;
    }

    private static String requireEnv(final String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量：" + name);
        }
        return value;
    }

    private static X509Certificate loadCertificate(final String pem) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }
}
