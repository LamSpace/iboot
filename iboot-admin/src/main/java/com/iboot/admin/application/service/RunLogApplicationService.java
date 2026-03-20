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

package com.iboot.admin.application.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import tools.jackson.databind.node.ObjectNode;
import com.iboot.admin.interfaces.dto.response.RunLogResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 运行日志应用服务
 * <p>
 * 封装 Elasticsearch 查询，提供应用运行日志的搜索和查看功能 支持按日志级别、关键字、Logger 名称、线程名和时间范围进行过滤
 * </p>
 *
 * @author iBoot
 */
@Service
public class RunLogApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RunLogApplicationService.class);

    private static final String INDEX_PATTERN = "iboot-logs-*";

    private static final DateTimeFormatter ES_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final ElasticsearchClient esClient;

    @SuppressWarnings("all")
    public RunLogApplicationService(final ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * 搜索运行日志
     * <p>
     * 支持多条件组合查询，结果按时间倒序排列
     * </p>
     *
     * @param level     日志级别（可选，如 INFO、ERROR 等）
     * @param keyword   关键字搜索（可选，全文匹配 message 字段）
     * @param logger    Logger 类名前缀（可选）
     * @param thread    线程名（可选）
     * @param beginTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @param pageNum   页码，从 1 开始
     * @param pageSize  每页大小
     *
     * @return 运行日志响应列表
     */
    public List<RunLogResponse> searchRunLogs(String level, String keyword, String logger, String thread,
                                              LocalDateTime beginTime, LocalDateTime endTime, int pageNum, int pageSize) {
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            // 日志级别过滤
            if (level != null && !level.isEmpty()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t.field("level.keyword").value(level.toUpperCase()))));
            }
            // 关键字全文搜索
            if (keyword != null && !keyword.isEmpty()) {
                boolBuilder.must(Query.of(q -> q.matchPhrase(m -> m.field("message").query(keyword))));
            }
            // Logger 类名前缀匹配
            if (logger != null && !logger.isEmpty()) {
                boolBuilder.filter(Query.of(q -> q.prefix(p -> p.field("logger_name.keyword").value(logger))));
            }
            // 线程名过滤
            if (thread != null && !thread.isEmpty()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t.field("thread_name.keyword").value(thread))));
            }
            // 时间范围
            if (beginTime != null || endTime != null) {
                boolBuilder.filter(Query.of(q -> q.range(r -> r.date(d -> {
                    d.field("@timestamp");
                    if (beginTime != null) {
                        d.gte(beginTime.format(ES_DATE_FORMATTER));
                    }
                    if (endTime != null) {
                        d.lte(endTime.format(ES_DATE_FORMATTER));
                    }
                    return d;
                }))));
            }
            int from = (pageNum - 1) * pageSize;
            SearchResponse<ObjectNode> response = esClient.search(s -> s.index(INDEX_PATTERN)
                    .query(Query.of(q -> q.bool(boolBuilder.build())))
                    .sort(sort -> sort.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                    .from(from)
                    .size(pageSize), ObjectNode.class);
            List<RunLogResponse> results = new ArrayList<>();
            for (Hit<ObjectNode> hit : response.hits().hits()) {
                results.add(convertHitToResponse(hit));
            }
            return results;
        } catch (ElasticsearchException e) {
            // 索引不存在等情况，返回空结果
            if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                log.debug("ES 索引尚未创建，返回空结果：{}", e.getMessage());
                return Collections.emptyList();
            }
            log.error("搜索运行日志失败", e);
            return Collections.emptyList();
        } catch (IOException e) {
            log.error("搜索运行日志 IO 异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 统计运行日志总数
     * <p>
     * 支持多条件组合查询
     * </p>
     *
     * @param level     日志级别（可选）
     * @param keyword   关键字搜索（可选）
     * @param logger    Logger 类名前缀（可选）
     * @param thread    线程名（可选）
     * @param beginTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     *
     * @return 符合条件的日志总数
     */
    public long countRunLogs(String level, String keyword, String logger, String thread, LocalDateTime beginTime,
                             LocalDateTime endTime) {
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
            if (level != null && !level.isEmpty()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t.field("level.keyword").value(level.toUpperCase()))));
            }
            if (keyword != null && !keyword.isEmpty()) {
                boolBuilder.must(Query.of(q -> q.matchPhrase(m -> m.field("message").query(keyword))));
            }
            if (logger != null && !logger.isEmpty()) {
                boolBuilder.filter(Query.of(q -> q.prefix(p -> p.field("logger_name.keyword").value(logger))));
            }
            if (thread != null && !thread.isEmpty()) {
                boolBuilder.filter(Query.of(q -> q.term(t -> t.field("thread_name.keyword").value(thread))));
            }
            if (beginTime != null || endTime != null) {
                boolBuilder.filter(Query.of(q -> q.range(r -> r.date(d -> {
                    d.field("@timestamp");
                    if (beginTime != null) {
                        d.gte(beginTime.format(ES_DATE_FORMATTER));
                    }
                    if (endTime != null) {
                        d.lte(endTime.format(ES_DATE_FORMATTER));
                    }
                    return d;
                }))));
            }
            SearchResponse<ObjectNode> response = esClient.search(
                    s -> s.index(INDEX_PATTERN).query(Query.of(q -> q.bool(boolBuilder.build()))).size(0),
                    ObjectNode.class);
            return response.hits().total() != null ? response.hits().total().value() : 0;
        } catch (ElasticsearchException e) {
            if (e.getMessage() != null && e.getMessage().contains("index_not_found")) {
                return 0;
            }
            log.error("统计运行日志失败", e);
            return 0;
        } catch (IOException e) {
            log.error("统计运行日志 IO 异常", e);
            return 0;
        }
    }

    /**
     * 根据 ID 获取单条运行日志
     *
     * @param id 日志 ID
     *
     * @return 运行日志响应对象，不存在则返回空 Optional
     */
    public Optional<RunLogResponse> getRunLogById(String id) {
        try {
            // 搜索所有索引中匹配 ID 的文档
            SearchResponse<ObjectNode> response = esClient.search(
                    s -> s.index(INDEX_PATTERN).query(Query.of(q -> q.ids(ids -> ids.values(id)))).size(1),
                    ObjectNode.class);
            if (response.hits().hits().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(convertHitToResponse(response.hits().hits().getFirst()));
        } catch (Exception e) {
            log.error("获取运行日志详情失败，id: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 将 Elasticsearch Hit 转换为运行日志响应对象
     *
     * @param hit Elasticsearch 搜索结果
     *
     * @return 运行日志响应对象
     */
    private RunLogResponse convertHitToResponse(Hit<ObjectNode> hit) {
        ObjectNode source = hit.source();
        if (source == null) {
            return RunLogResponse.builder().id(hit.id()).build();
        }
        return RunLogResponse.builder()
                .id(hit.id())
                .timestamp(getTextValue(source, "@timestamp"))
                .level(getTextValue(source, "level"))
                .thread(getTextValue(source, "thread_name"))
                .logger(getTextValue(source, "logger_name"))
                .message(getTextValue(source, "message"))
                .build();
    }

    /**
     * 从 JSON 节点获取文本值
     *
     * @param node  JSON 节点
     * @param field 字段名
     *
     * @return 文本值，不存在则返回 null
     */
    private String getTextValue(ObjectNode node, String field) {
        var value = node.get(field);
        return value != null && !value.isNull() ? value.toString() : null;
    }

}
