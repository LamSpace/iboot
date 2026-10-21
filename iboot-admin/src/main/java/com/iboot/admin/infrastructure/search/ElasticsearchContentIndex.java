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
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 业务内容索引的 Elasticsearch 适配器。
 *
 * <p>负责索引与 mapping 的幂等创建、文档写入与删除，以及统一检索查询。索引名固定为
 * {@code iboot-content-v1}，与运行日志索引 {@code iboot-logs-*} 完全隔离。中文使用
 * Elasticsearch 内置的 {@code cjk} 分析器，无需安装插件。
 *
 * <p>写方法不吞异常，由调用方决定降级策略；检索后端不可用时由应用服务返回空结果。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class ElasticsearchContentIndex {

    /**
     * 业务内容索引名称。
     */
    public static final String INDEX_NAME = "iboot-content-v1";

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchContentIndex.class);

    /**
     * 高亮片段的最大数量。
     */
    private static final int MAX_FRAGMENTS = 3;

    private final ElasticsearchClient esClient;

    @SuppressWarnings("all")
    public ElasticsearchContentIndex(final ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 幂等创建索引与 mapping。
     *
     * <p>索引已存在时直接返回；并发创建导致的资源已存在异常会被忽略。mapping 中标题与正文
     * 使用 {@code cjk} 分析器，时间字段为 {@code date} 类型。
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void ensureIndex() throws IOException {
        if (esClient.indices().exists(e -> e.index(INDEX_NAME)).value()) {
            return;
        }
        try {
            esClient.indices().create(c -> c.index(INDEX_NAME).mappings(m -> m
                    .properties("docType", p -> p.keyword(k -> k))
                    .properties("sourceId", p -> p.long_(l -> l))
                    .properties("receiverId", p -> p.long_(l -> l))
                    .properties("title", p -> p.text(t -> t.analyzer("cjk")
                            .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(256)))))
                    .properties("content", p -> p.text(t -> t.analyzer("cjk")))
                    .properties("category", p -> p.keyword(k -> k))
                    .properties("createdBy", p -> p.keyword(k -> k))
                    .properties("createdAt", p -> p.date(d -> d))
                    .properties("updatedAt", p -> p.date(d -> d))));
            log.info("内容索引已创建：{}", INDEX_NAME);
        } catch (ElasticsearchException e) {
            if (!isResourceAlreadyExists(e)) {
                throw e;
            }
        }
    }

    /**
     * 写入或覆盖单个索引文档。
     *
     * @param document 索引文档
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void upsert(final ContentDocument document) throws IOException {
        esClient.index(i -> i.index(INDEX_NAME).id(document.documentId()).document(document));
    }

    /**
     * 批量写入或覆盖索引文档。
     *
     * <p>单条失败不会中断其余文档，失败项以 WARN 级别记录，交给定时对账重放。
     *
     * @param documents 索引文档列表
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void upsertAll(final List<ContentDocument> documents) throws IOException {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        List<BulkOperation> operations = documents.stream()
                .map(document -> BulkOperation.of(b -> b.index(
                        i -> i.index(INDEX_NAME).id(document.documentId()).document(document))))
                .toList();
        BulkResponse response = esClient.bulk(b -> b.operations(operations));
        if (response.errors()) {
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    log.warn("内容索引批量写入失败，id: {}, 原因: {}", item.id(), item.error().reason());
                }
            }
        }
    }

    /**
     * 按确定性文档 ID 删除单个文档。
     *
     * <p>文档不存在时视为成功。
     *
     * @param docType    文档类型
     * @param sourceId   公告 ID 或消息 ID
     * @param receiverId 接收人用户 ID，公告文档传 {@code null}
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void delete(final ContentDocType docType, final Long sourceId, final Long receiverId) throws IOException {
        try {
            esClient.delete(d -> d.index(INDEX_NAME).id(ContentDocument.documentId(docType, sourceId, receiverId)));
        } catch (ElasticsearchException e) {
            if (!isNotFound(e) && !isIndexNotFound(e)) {
                throw e;
            }
        }
    }

    /**
     * 删除某条源数据对应的全部索引文档。
     *
     * @param docType  文档类型
     * @param sourceId 公告 ID 或消息 ID
     *
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public void deleteBySource(final ContentDocType docType, final Long sourceId) throws IOException {
        try {
            esClient.deleteByQuery(q -> q.index(INDEX_NAME).query(query -> query.bool(b -> b
                    .filter(f -> f.term(t -> t.field("docType").value(docType.getCode())))
                    .filter(f -> f.term(t -> t.field("sourceId").value(sourceId))))));
        } catch (ElasticsearchException e) {
            if (!isIndexNotFound(e)) {
                throw e;
            }
        }
    }

    /**
     * 检索业务内容。
     *
     * <p>查询同时施加两层过滤：请求的类型范围，以及可见性规则（公告对所有人可见，消息只对
     * 当前接收人可见）。返回标题与正文的高亮片段。
     *
     * @param types      允许的文档类型
     * @param keyword    检索关键字
     * @param receiverId 当前用户 ID，用于过滤消息
     * @param from       起始偏移量
     * @param size       返回数量
     *
     * @return 检索结果
     * @throws IOException 与 Elasticsearch 通信失败时抛出
     */
    public ContentSearchResult search(final Set<ContentDocType> types, final String keyword, final Long receiverId,
                                      final int from, final int size) throws IOException {
        SearchResponse<ObjectNode> response = esClient.search(s -> s
                .index(INDEX_NAME)
                .from(from)
                .size(size)
                .query(q -> q.bool(b -> {
                    b.must(m -> m.multiMatch(mm -> mm.query(keyword).fields(List.of("title^2", "content"))));
                    b.filter(f -> f.terms(t -> t.field("docType")
                            .terms(tv -> tv.value(types.stream()
                                    .map(type -> FieldValue.of(type.getCode()))
                                    .toList()))));
                    b.filter(f -> f.bool(v -> v
                            .should(sh -> sh.term(t -> t.field("docType").value(ContentDocType.NOTICE.getCode())))
                            .should(sh -> sh.bool(bb -> bb
                                    .filter(t -> t.term(tf -> tf.field("docType").value(ContentDocType.MESSAGE.getCode())))
                                    .filter(t -> t.term(tf -> tf.field("receiverId").value(receiverId)))))
                            .minimumShouldMatch("1")));
                    return b;
                }))
                .highlight(h -> h
                        .preTags("<em>")
                        .postTags("</em>")
                        .numberOfFragments(MAX_FRAGMENTS)
                        .fields(List.of(
                                NamedValue.of("title", HighlightField.of(f -> f)),
                                NamedValue.of("content", HighlightField.of(f -> f))))),
                ObjectNode.class);

        List<ContentSearchHit> hits = new ArrayList<>();
        for (Hit<ObjectNode> hit : response.hits().hits()) {
            ObjectNode source = hit.source();
            if (source == null) {
                continue;
            }
            hits.add(new ContentSearchHit(
                    text(source, "docType"),
                    number(source, "sourceId"),
                    text(source, "title"),
                    fragments(hit),
                    text(source, "updatedAt")));
        }
        long total = response.hits().total() != null ? response.hits().total().value() : hits.size();
        return new ContentSearchResult(hits, total);
    }

    /**
     * 提取高亮片段，标题片段排在正文片段之前。
     *
     * @param hit 检索命中
     *
     * @return 高亮片段列表
     */
    private List<String> fragments(final Hit<ObjectNode> hit) {
        List<String> fragments = new ArrayList<>();
        if (hit.highlight() == null) {
            return fragments;
        }
        fragments.addAll(hit.highlight().getOrDefault("title", List.of()));
        fragments.addAll(hit.highlight().getOrDefault("content", List.of()));
        return fragments;
    }

    /**
     * 读取 JSON 节点中的文本字段。
     *
     * @param node  文档源
     * @param field 字段名
     *
     * @return 文本值，字段不存在时为 {@code null}
     */
    private String text(final ObjectNode node, final String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asString() : null;
    }

    /**
     * 读取 JSON 节点中的数字字段。
     *
     * @param node  文档源
     * @param field 字段名
     *
     * @return 数字值，字段不存在时为 {@code null}
     */
    private Long number(final ObjectNode node, final String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asLong() : null;
    }

    /**
     * 判断异常是否表示索引已存在。
     *
     * @param e Elasticsearch 异常
     *
     * @return 资源已存在时返回 {@code true}
     */
    private boolean isResourceAlreadyExists(final ElasticsearchException e) {
        return e.getMessage() != null && e.getMessage().contains("resource_already_exists_exception");
    }

    /**
     * 判断异常是否表示索引不存在。
     *
     * @param e Elasticsearch 异常
     *
     * @return 索引不存在时返回 {@code true}
     */
    private boolean isIndexNotFound(final ElasticsearchException e) {
        return e.getMessage() != null && e.getMessage().contains("index_not_found");
    }

    /**
     * 判断异常是否表示文档不存在。
     *
     * @param e Elasticsearch 异常
     *
     * @return 文档不存在时返回 {@code true}
     */
    private boolean isNotFound(final ElasticsearchException e) {
        return e.getMessage() != null && e.getMessage().contains("not_found");
    }

}
