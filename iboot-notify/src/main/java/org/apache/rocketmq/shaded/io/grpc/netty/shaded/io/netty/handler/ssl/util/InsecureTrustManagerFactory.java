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

package org.apache.rocketmq.shaded.io.grpc.netty.shaded.io.netty.handler.ssl.util;

import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

/**
 * RocketMQ 5.2.2 客户端信任管理器的安全兼容层。
 *
 * <p>官方客户端在 {@code RpcClientImpl} 中硬编码引用同名的
 * {@code InsecureTrustManagerFactory.INSTANCE}，导致 TLS 只加密、不校验服务端证书。上游最新发行版
 * 仍是 5.2.2，无法通过配置开启校验。本类使用相同 FQCN 放在 Spring Boot fat jar 的
 * {@code BOOT-INF/classes} 中，覆盖客户端 jar 内的实现，只替换信任管理器，不改变 gRPC 的 HTTPS
 * 主机名校验。
 *
 * <p>信任库来源：优先使用 {@code iboot.rocketmq.trustStore} 系统属性或
 * {@code IBOOT_ROCKETMQ_TRUSTSTORE} 环境变量；未配置时使用 JVM 默认信任库（即
 * {@code javax.net.ssl.trustStore}）。显式配置但加载失败时抛出初始化错误，让客户端失败关闭。
 *
 * <p>客户端升级到已修复服务端校验的版本后，必须删除本类。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class InsecureTrustManagerFactory {

    private static final String TRUST_STORE_PROPERTY = "iboot.rocketmq.trustStore";

    private static final String TRUST_STORE_ENV = "IBOOT_ROCKETMQ_TRUSTSTORE";

    private static final String TRUST_STORE_PASSWORD_PROPERTY = "iboot.rocketmq.trustStorePassword";

    private static final String TRUST_STORE_PASSWORD_ENV = "IBOOT_ROCKETMQ_TRUSTSTORE_PASSWORD";

    private static final String TRUST_STORE_TYPE_PROPERTY = "iboot.rocketmq.trustStoreType";

    private static final String TRUST_STORE_TYPE_ENV = "IBOOT_ROCKETMQ_TRUSTSTORE_TYPE";

    /**
     * 客户端直接读取的信任管理器实例。
     *
     * <p>初始化失败时抛出 {@link ExceptionInInitializerError}，不会退化为信任所有证书。
     */
    public static final TrustManagerFactory INSTANCE = create();

    private InsecureTrustManagerFactory() {
    }

    private static TrustManagerFactory create() {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            String location = firstNonBlank(
                    System.getProperty(TRUST_STORE_PROPERTY),
                    System.getenv(TRUST_STORE_ENV));
            if (location == null) {
                factory.init((KeyStore) null);
                return factory;
            }
            String password = firstNonBlank(
                    System.getProperty(TRUST_STORE_PASSWORD_PROPERTY),
                    System.getenv(TRUST_STORE_PASSWORD_ENV),
                    "changeit");
            String type = firstNonBlank(
                    System.getProperty(TRUST_STORE_TYPE_PROPERTY),
                    System.getenv(TRUST_STORE_TYPE_ENV),
                    "PKCS12");
            KeyStore keyStore = KeyStore.getInstance(type);
            try (InputStream input = Files.newInputStream(Path.of(location))) {
                keyStore.load(input, password.toCharArray());
            }
            factory.init(keyStore);
            return factory;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String firstNonBlank(final String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
