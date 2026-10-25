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

package com.iboot.ai.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.iboot.ai.infrastructure.config.RagProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link KnowledgeIndexInitializer} 的集成测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeIndexInitializerTest {

    private static final int ES_PORT = 9200;

    private static final String INDEX = "iboot-kb-chunk-test";

    @Container
    static final GenericContainer<?> ELASTICSEARCH = new GenericContainer<>(
            "docker.elastic.co/elasticsearch/elasticsearch:9.2.3")
            .withExposedPorts(ES_PORT)
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("xpack.security.enrollment.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(Wait.forHttp("/_cluster/health")
                    .forPort(ES_PORT)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private static Rest5Client restClient;

    private static ElasticsearchTransport transport;

    private static ElasticsearchClient client;

    private static KnowledgeIndexInitializer initializer;

    @BeforeAll
    static void setUp() {
        restClient = Rest5Client.builder(
                URI.create("http://" + ELASTICSEARCH.getHost() + ":" + ELASTICSEARCH.getMappedPort(ES_PORT)))
                .build();
        transport = new Rest5ClientTransport(restClient, new Jackson3JsonpMapper());
        client = new ElasticsearchClient(transport);
        RagProperties properties = new RagProperties();
        properties.setIndexName(INDEX);
        initializer = new KnowledgeIndexInitializer(client, properties);
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (transport != null) {
            transport.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }

    @BeforeEach
    void resetIndex() throws IOException {
        try {
            client.indices().delete(d -> d.index(INDEX));
        } catch (ElasticsearchException ignored) {
            // 首次运行时索引不存在
        }
    }

    /**
     * 索引与关键 mapping 正确，重复初始化幂等。
     */
    @Test
    void createsIndexIdempotently() throws IOException {
        initializer.ensureIndex();
        assertThatCode(initializer::ensureIndex).doesNotThrowAnyException();

        assertThat(client.indices().exists(e -> e.index(INDEX)).value()).isTrue();
        var mapping = client.indices().getMapping(m -> m.index(INDEX)).mappings().get(INDEX).mappings();
        assertThat(mapping.properties().get("kbId").isLong()).isTrue();
        assertThat(mapping.properties().get("content").isText()).isTrue();
        assertThat(mapping.properties().get("embedding").isDenseVector()).isTrue();
        assertThat(mapping.properties().get("embedding").denseVector().dims()).isEqualTo(1024);
    }
}
