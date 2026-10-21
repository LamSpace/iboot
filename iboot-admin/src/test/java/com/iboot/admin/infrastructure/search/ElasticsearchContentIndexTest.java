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

package com.iboot.admin.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ElasticsearchContentIndex} 的集成测试。
 *
 * <p>使用 Testcontainers 启动真实 Elasticsearch 9.2.3，验证索引创建、中文检索、幂等写入、
 * 删除语义与接收人隔离。运行环境必须提供 Docker。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers
class ElasticsearchContentIndexTest {

    private static final int ES_PORT = 9200;

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

    private static ElasticsearchContentIndex contentIndex;

    /**
     * 建立指向测试容器的客户端。
     */
    @BeforeAll
    static void setUpClient() {
        restClient = Rest5Client.builder(
                URI.create("http://" + ELASTICSEARCH.getHost() + ":" + ELASTICSEARCH.getMappedPort(ES_PORT))).build();
        transport = new Rest5ClientTransport(restClient, new Jackson3JsonpMapper());
        client = new ElasticsearchClient(transport);
        contentIndex = new ElasticsearchContentIndex(client);
    }

    /**
     * 关闭客户端。
     *
     * @throws IOException 关闭失败时抛出
     */
    @AfterAll
    static void tearDownClient() throws IOException {
        if (transport != null) {
            transport.close();
        }
        if (restClient != null) {
            restClient.close();
        }
    }

    /**
     * 每个用例前重建索引，保证用例之间互不影响。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @BeforeEach
    void resetIndex() throws IOException {
        try {
            client.indices().delete(d -> d.index(ElasticsearchContentIndex.INDEX_NAME));
        } catch (ElasticsearchException ignored) {
            // 首次运行时索引尚不存在
        }
        contentIndex.ensureIndex();
    }

    /**
     * 索引创建应幂等。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldCreateIndexIdempotently() throws IOException {
        contentIndex.ensureIndex();
        contentIndex.ensureIndex();

        boolean exists = client.indices().exists(e -> e.index(ElasticsearchContentIndex.INDEX_NAME)).value();
        assertThat(exists).isTrue();
    }

    /**
     * 中文关键字应命中正文并返回高亮片段。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldMatchChineseKeywordAndHighlight() throws IOException {
        contentIndex.upsert(noticeDocument("年度公告", "年度绩效评估安排"));
        refresh();

        ContentSearchResult result = contentIndex.search(Set.of(ContentDocType.NOTICE), "绩效", 1L, 0, 20);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().getFirst().getTitle()).isEqualTo("年度公告");
        assertThat(result.getHits().getFirst().getHighlights()).isNotEmpty();
        assertThat(result.getHits().getFirst().getHighlights()).anyMatch(fragment -> fragment.contains("绩效"));
    }

    /**
     * 同一文档重复写入应只保留一份。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldUpsertWithoutDuplicates() throws IOException {
        contentIndex.upsert(noticeDocument("旧标题", "系统升级通知"));
        refresh();
        contentIndex.upsert(noticeDocument("新标题", "系统升级通知"));
        refresh();

        ContentSearchResult result = contentIndex.search(Set.of(ContentDocType.NOTICE), "系统升级", 1L, 0, 20);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getHits().getFirst().getTitle()).isEqualTo("新标题");
    }

    /**
     * 按文档 ID 删除只影响对应接收人。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldDeleteByDocumentId() throws IOException {
        contentIndex.upsertAll(List.of(
                messageDocument(5L, 7L, "奖金通知", "绩效奖金发放"),
                messageDocument(5L, 8L, "奖金通知", "绩效奖金发放")));
        refresh();

        contentIndex.delete(ContentDocType.MESSAGE, 5L, 7L);
        refresh();

        assertThat(contentIndex.search(Set.of(ContentDocType.MESSAGE), "绩效", 7L, 0, 20).getTotal()).isZero();
        assertThat(contentIndex.search(Set.of(ContentDocType.MESSAGE), "绩效", 8L, 0, 20).getTotal()).isEqualTo(1);
    }

    /**
     * 按源数据删除应移除该消息的全部接收人文档。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldDeleteBySource() throws IOException {
        contentIndex.upsertAll(List.of(
                messageDocument(5L, 7L, "奖金通知", "绩效奖金发放"),
                messageDocument(5L, 8L, "奖金通知", "绩效奖金发放")));
        refresh();

        contentIndex.deleteBySource(ContentDocType.MESSAGE, 5L);
        refresh();

        assertThat(contentIndex.search(Set.of(ContentDocType.MESSAGE), "绩效", 7L, 0, 20).getTotal()).isZero();
        assertThat(contentIndex.search(Set.of(ContentDocType.MESSAGE), "绩效", 8L, 0, 20).getTotal()).isZero();
    }

    /**
     * 消息只对实际接收人可见，公告对所有登录用户可见。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldIsolateMessageReceivers() throws IOException {
        contentIndex.upsert(noticeDocument("年度公告", "年度绩效评估安排"));
        contentIndex.upsertAll(List.of(
                messageDocument(5L, 7L, "奖金通知", "绩效奖金发放"),
                messageDocument(5L, 8L, "奖金通知", "绩效奖金发放")));
        refresh();

        ContentSearchResult user7 = contentIndex.search(Set.of(ContentDocType.NOTICE, ContentDocType.MESSAGE), "绩效",
                7L, 0, 20);
        ContentSearchResult user9 = contentIndex.search(Set.of(ContentDocType.NOTICE, ContentDocType.MESSAGE), "绩效",
                9L, 0, 20);

        assertThat(user7.getTotal()).isEqualTo(2);
        assertThat(user7.getHits()).anyMatch(hit -> "message".equals(hit.getDocType()));
        assertThat(user9.getTotal()).isEqualTo(1);
        assertThat(user9.getHits()).allMatch(hit -> "notice".equals(hit.getDocType()));
    }

    /**
     * 类型过滤只返回指定类型。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldFilterByType() throws IOException {
        contentIndex.upsert(noticeDocument("年度公告", "年度绩效评估安排"));
        contentIndex.upsert(messageDocument(5L, 7L, "奖金通知", "绩效奖金发放"));
        refresh();

        ContentSearchResult notices = contentIndex.search(Set.of(ContentDocType.NOTICE), "绩效", 7L, 0, 20);
        ContentSearchResult messages = contentIndex.search(Set.of(ContentDocType.MESSAGE), "绩效", 7L, 0, 20);

        assertThat(notices.getHits()).extracting(ContentSearchHit::getDocType).containsOnly("notice");
        assertThat(messages.getHits()).extracting(ContentSearchHit::getDocType).containsOnly("message");
    }

    /**
     * 分页应返回正确的命中数与总数。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    @Test
    void shouldPaginate() throws IOException {
        contentIndex.upsertAll(List.of(
                noticeDocument(1L, "公告一", "绩效评估一"),
                noticeDocument(2L, "公告二", "绩效评估二"),
                noticeDocument(3L, "公告三", "绩效评估三")));
        refresh();

        ContentSearchResult first = contentIndex.search(Set.of(ContentDocType.NOTICE), "绩效", 1L, 0, 2);
        ContentSearchResult second = contentIndex.search(Set.of(ContentDocType.NOTICE), "绩效", 1L, 2, 2);

        assertThat(first.getTotal()).isEqualTo(3);
        assertThat(first.getHits()).hasSize(2);
        assertThat(second.getTotal()).isEqualTo(3);
        assertThat(second.getHits()).hasSize(1);
    }

    /**
     * 刷新索引，使写入的文档立即可检索。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    private void refresh() throws IOException {
        client.indices().refresh(r -> r.index(ElasticsearchContentIndex.INDEX_NAME));
    }

    /**
     * 构造公告索引文档。
     *
     * @param title   标题
     * @param content 正文
     *
     * @return 公告索引文档
     */
    private ContentDocument noticeDocument(final String title, final String content) {
        return noticeDocument(1L, title, content);
    }

    /**
     * 构造指定 ID 的公告索引文档。
     *
     * @param id      公告 ID
     * @param title   标题
     * @param content 正文
     *
     * @return 公告索引文档
     */
    private ContentDocument noticeDocument(final Long id, final String title, final String content) {
        return new ContentDocument(ContentDocType.NOTICE, id, null, title, content, "1", "admin",
                "2026-10-21T08:30:00", "2026-10-21T09:00:00");
    }

    /**
     * 构造消息索引文档。
     *
     * @param messageId  消息 ID
     * @param receiverId 接收人 ID
     * @param title      标题
     * @param content    正文
     *
     * @return 消息索引文档
     */
    private ContentDocument messageDocument(final Long messageId, final Long receiverId, final String title,
                                            final String content) {
        return new ContentDocument(ContentDocType.MESSAGE, messageId, receiverId, title, content, "1", "admin",
                "2026-10-21T08:30:00", "2026-10-21T09:00:00");
    }

}
