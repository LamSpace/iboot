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

package com.iboot.ai.infrastructure.persistence;

import com.iboot.ai.domain.knowledge.DocumentChunk;
import com.iboot.ai.domain.knowledge.DocumentStatus;
import com.iboot.ai.domain.knowledge.KnowledgeBase;
import com.iboot.ai.domain.knowledge.KnowledgeBaseSummary;
import com.iboot.ai.domain.knowledge.KnowledgeDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 知识库元数据仓储。
 *
 * <p>使用 Spring JDBC 访问 {@code iboot_ai} schema，覆盖知识库、文档、分块与共享部门。
 * 仅在 {@code iboot.ai.rag.enabled=true} 时装配，避免纯对话模式依赖 MySQL。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeRepository {

    private static final String KB_COLUMNS = "id, name, description, visibility, owner_user_id, owner_dept_id, "
            + "created_by, create_time, update_time";

    private static final String DOC_COLUMNS = "id, kb_id, file_name, file_ext, file_size, mime_type, storage_key, "
            + "content_hash, owner_user_id, owner_dept_id, status, error_code, error_message, chunk_count, attempts, "
            + "next_attempt_at, create_time, update_time";

    private final NamedParameterJdbcTemplate namedJdbc;

    private final JdbcTemplate jdbc;

    /**
     * 创建仓储。
     *
     * @param namedJdbc 命名参数 JDBC 模板
     * @param jdbc      普通 JDBC 模板
     */
    public KnowledgeRepository(NamedParameterJdbcTemplate namedJdbc, JdbcTemplate jdbc) {
        this.namedJdbc = namedJdbc;
        this.jdbc = jdbc;
    }

    /**
     * 新建知识库并写入共享部门。
     *
     * @param name         名称
     * @param description  描述
     * @param visibility   可见范围
     * @param ownerUserId  所有者用户 ID
     * @param ownerDeptId  所有者部门 ID
     * @param createdBy    创建人
     * @param sharedDeptIds 共享部门集合
     * @return 知识库 ID
     */
    public long insertKnowledgeBase(String name, String description, int visibility, long ownerUserId,
                                    Long ownerDeptId, String createdBy, List<Long> sharedDeptIds) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedJdbc.update("""
                INSERT INTO ai_knowledge_base (name, description, visibility, owner_user_id, owner_dept_id, created_by)
                VALUES (:name, :description, :visibility, :ownerUserId, :ownerDeptId, :createdBy)
                """, new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("description", description)
                .addValue("visibility", visibility)
                .addValue("ownerUserId", ownerUserId)
                .addValue("ownerDeptId", ownerDeptId)
                .addValue("createdBy", createdBy), keyHolder, new String[]{"id"});
        long id = keyHolder.getKey().longValue();
        replaceSharedDepts(id, sharedDeptIds);
        return id;
    }

    /**
     * 查询知识库。
     *
     * @param id 知识库 ID
     * @return 知识库；不存在或已删除时为空
     */
    public Optional<KnowledgeBase> findKnowledgeBase(long id) {
        List<KnowledgeBase> bases = namedJdbc.query(
                "SELECT " + KB_COLUMNS + " FROM ai_knowledge_base WHERE id = :id AND deleted = 0",
                Map.of("id", id), this::mapKnowledgeBase);
        return bases.stream().findFirst()
                .map(base -> new KnowledgeBase(base.id(), base.name(), base.description(), base.visibility(),
                        base.ownerUserId(), base.ownerDeptId(), findSharedDeptIds(base.id()), base.createdBy(),
                        base.createTime(), base.updateTime()));
    }

    /**
     * 查询所有未删除知识库。
     *
     * @return 知识库列表
     */
    public List<KnowledgeBase> findAllKnowledgeBases() {
        Map<Long, List<Long>> shared = findAllSharedDepts();
        return namedJdbc.query(
                "SELECT " + KB_COLUMNS + " FROM ai_knowledge_base WHERE deleted = 0 ORDER BY id",
                Map.of(), (rs, rowNum) -> {
                    KnowledgeBase base = mapKnowledgeBase(rs, rowNum);
                    return new KnowledgeBase(base.id(), base.name(), base.description(), base.visibility(),
                            base.ownerUserId(), base.ownerDeptId(), shared.getOrDefault(base.id(), List.of()),
                            base.createdBy(), base.createTime(), base.updateTime());
                });
    }

    /**
     * 查询知识库共享判断所需的摘要。
     *
     * @return 摘要列表
     */
    public List<KnowledgeBaseSummary> findKnowledgeBaseSummaries() {
        Map<Long, List<Long>> shared = findAllSharedDepts();
        return namedJdbc.query("""
                        SELECT id, visibility, owner_user_id, owner_dept_id
                        FROM ai_knowledge_base WHERE deleted = 0 ORDER BY id
                        """, Map.of(), (rs, rowNum) -> new KnowledgeBaseSummary(
                        rs.getLong("id"),
                        rs.getInt("visibility"),
                        rs.getLong("owner_user_id"),
                        rs.getObject("owner_dept_id", Long.class),
                        shared.getOrDefault(rs.getLong("id"), List.of())));
    }

    /**
     * 更新知识库并替换共享部门。
     *
     * @param id           知识库 ID
     * @param name         名称
     * @param description  描述
     * @param visibility   可见范围
     * @param ownerDeptId  所有者部门 ID
     * @param sharedDeptIds 共享部门集合
     */
    public void updateKnowledgeBase(long id, String name, String description, int visibility, Long ownerDeptId,
                                    List<Long> sharedDeptIds) {
        namedJdbc.update("""
                UPDATE ai_knowledge_base
                SET name = :name, description = :description, visibility = :visibility, owner_dept_id = :ownerDeptId
                WHERE id = :id AND deleted = 0
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", name)
                .addValue("description", description)
                .addValue("visibility", visibility)
                .addValue("ownerDeptId", ownerDeptId));
        replaceSharedDepts(id, sharedDeptIds);
    }

    /**
     * 逻辑删除知识库，并清理共享部门、文档与分块。
     *
     * @param id 知识库 ID
     */
    public void softDeleteKnowledgeBase(long id) {
        jdbc.update("DELETE FROM ai_document_chunk WHERE doc_id IN "
                + "(SELECT id FROM ai_document WHERE kb_id = ? AND deleted = 0)", id);
        jdbc.update("UPDATE ai_document SET deleted = id WHERE kb_id = ? AND deleted = 0", id);
        jdbc.update("DELETE FROM ai_kb_share_dept WHERE kb_id = ?", id);
        jdbc.update("UPDATE ai_knowledge_base SET deleted = id WHERE id = ? AND deleted = 0", id);
    }

    /**
     * 新建文档。
     *
     * @param kbId          知识库 ID
     * @param fileName      原始文件名
     * @param fileExt       扩展名
     * @param fileSize      文件大小
     * @param mimeType      MIME 类型
     * @param storageKey    对象存储路径
     * @param contentHash   内容哈希
     * @param ownerUserId   上传人用户 ID
     * @param ownerDeptId   上传人部门 ID
     * @return 文档 ID
     */
    public long insertDocument(long kbId, String fileName, String fileExt, long fileSize, String mimeType,
                               String storageKey, String contentHash, long ownerUserId, long ownerDeptId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        namedJdbc.update("""
                INSERT INTO ai_document (kb_id, file_name, file_ext, file_size, mime_type, storage_key, content_hash,
                                         owner_user_id, owner_dept_id, status)
                VALUES (:kbId, :fileName, :fileExt, :fileSize, :mimeType, :storageKey, :contentHash,
                        :ownerUserId, :ownerDeptId, 'PENDING')
                """, new MapSqlParameterSource()
                .addValue("kbId", kbId)
                .addValue("fileName", fileName)
                .addValue("fileExt", fileExt)
                .addValue("fileSize", fileSize)
                .addValue("mimeType", mimeType)
                .addValue("storageKey", storageKey)
                .addValue("contentHash", contentHash)
                .addValue("ownerUserId", ownerUserId)
                .addValue("ownerDeptId", ownerDeptId), keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    /**
     * 查询文档。
     *
     * @param id 文档 ID
     * @return 文档；不存在或已删除时为空
     */
    public Optional<KnowledgeDocument> findDocument(long id) {
        return namedJdbc.query("SELECT " + DOC_COLUMNS + " FROM ai_document WHERE id = :id AND deleted = 0",
                Map.of("id", id), this::mapDocument).stream().findFirst();
    }

    /**
     * 查询知识库下的文档。
     *
     * @param kbId 知识库 ID
     * @return 文档列表
     */
    public List<KnowledgeDocument> findDocumentsByKb(long kbId) {
        return namedJdbc.query("SELECT " + DOC_COLUMNS
                        + " FROM ai_document WHERE kb_id = :kbId AND deleted = 0 ORDER BY id",
                Map.of("kbId", kbId), this::mapDocument);
    }

    /**
     * 按知识库与内容哈希查询未删除文档。
     *
     * @param kbId        知识库 ID
     * @param contentHash 内容哈希
     * @return 文档；不存在时为空
     */
    public Optional<KnowledgeDocument> findActiveDocumentByHash(long kbId, String contentHash) {
        return namedJdbc.query("SELECT " + DOC_COLUMNS
                        + " FROM ai_document WHERE kb_id = :kbId AND content_hash = :hash AND deleted = 0",
                Map.of("kbId", kbId, "hash", contentHash), this::mapDocument).stream().findFirst();
    }

    /**
     * 认领可处理文档。
     *
     * <p>使用 {@code FOR UPDATE SKIP LOCKED}，调用方必须在事务中执行。
     *
     * @param now         当前时间
     * @param staleBefore 处理中任务的失效时间
     * @param maxAttempts 最大尝试次数
     * @param limit       最大数量
     * @return 可处理文档
     */
    public List<KnowledgeDocument> findClaimable(LocalDateTime now, LocalDateTime staleBefore, int maxAttempts,
                                                 int limit) {
        return namedJdbc.query("SELECT " + DOC_COLUMNS + """
                         FROM ai_document
                         WHERE deleted = 0
                           AND ((status = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= :now))
                             OR (status = 'PROCESSING' AND update_time < :staleBefore)
                             OR (status = 'FAILED' AND attempts < :maxAttempts
                                 AND next_attempt_at IS NOT NULL AND next_attempt_at <= :now))
                         ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED
                        """, new MapSqlParameterSource()
                        .addValue("now", now)
                        .addValue("staleBefore", staleBefore)
                        .addValue("maxAttempts", maxAttempts)
                        .addValue("limit", limit), this::mapDocument);
    }

    /**
     * 标记文档处理中并累加尝试次数。
     *
     * @param id  文档 ID
     * @param now 当前时间
     * @return 是否成功认领
     */
    public boolean markProcessing(long id, LocalDateTime now) {
        return namedJdbc.update("""
                UPDATE ai_document
                SET status = 'PROCESSING', attempts = attempts + 1, update_time = :now
                WHERE id = :id AND deleted = 0 AND status IN ('PENDING', 'PROCESSING')
                """, new MapSqlParameterSource().addValue("id", id).addValue("now", now)) > 0;
    }

    /**
     * 标记文档就绪。
     *
     * @param id         文档 ID
     * @param chunkCount 分块数量
     */
    public void markReady(long id, int chunkCount) {
        namedJdbc.update("""
                UPDATE ai_document
                SET status = 'READY', chunk_count = :chunkCount, error_code = NULL, error_message = NULL,
                    next_attempt_at = NULL
                WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", id).addValue("chunkCount", chunkCount));
    }

    /**
     * 标记文档失败。
     *
     * @param id            文档 ID
     * @param errorCode     错误码
     * @param errorMessage  错误说明
     * @param nextAttemptAt 下次重试时间
     */
    public void markFailed(long id, String errorCode, String errorMessage, LocalDateTime nextAttemptAt) {
        namedJdbc.update("""
                UPDATE ai_document
                SET status = 'FAILED', error_code = :errorCode, error_message = :errorMessage,
                    next_attempt_at = :nextAttemptAt
                WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("errorCode", errorCode)
                .addValue("errorMessage", errorMessage)
                .addValue("nextAttemptAt", nextAttemptAt));
    }

    /**
     * 重置文档为待处理。
     *
     * @param id 文档 ID
     */
    public void resetForRetry(long id) {
        namedJdbc.update("""
                UPDATE ai_document
                SET status = 'PENDING', error_code = NULL, error_message = NULL, next_attempt_at = NULL
                WHERE id = :id AND deleted = 0
                """, Map.of("id", id));
    }

    /**
     * 逻辑删除文档并清理分块。
     *
     * @param id 文档 ID
     */
    public void softDeleteDocument(long id) {
        deleteChunks(id);
        jdbc.update("UPDATE ai_document SET deleted = id WHERE id = ? AND deleted = 0", id);
    }

    /**
     * 保存文档分块，先清空旧分块。
     *
     * @param docId  文档 ID
     * @param chunks 分块列表
     */
    public void saveChunks(long docId, List<DocumentChunk> chunks) {
        deleteChunks(docId);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        List<Object[]> batch = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            batch.add(new Object[]{docId, chunk.chunkIndex(), chunk.pageNo(), chunk.headingPath(), chunk.content(),
                    chunk.tokenCount()});
        }
        jdbc.batchUpdate("""
                INSERT INTO ai_document_chunk (doc_id, chunk_index, page_no, heading_path, content, token_count)
                VALUES (?, ?, ?, ?, ?, ?)
                """, batch);
    }

    /**
     * 查询文档分块。
     *
     * @param docId 文档 ID
     * @return 分块列表
     */
    public List<DocumentChunk> findChunks(long docId) {
        return jdbc.query("""
                SELECT id, doc_id, chunk_index, page_no, heading_path, content, token_count
                FROM ai_document_chunk WHERE doc_id = ? ORDER BY chunk_index
                """, (rs, rowNum) -> new DocumentChunk(
                rs.getLong("id"),
                rs.getLong("doc_id"),
                rs.getInt("chunk_index"),
                rs.getObject("page_no", Integer.class),
                rs.getString("heading_path"),
                rs.getString("content"),
                rs.getInt("token_count")), docId);
    }

    /**
     * 删除文档分块。
     *
     * @param docId 文档 ID
     */
    public void deleteChunks(long docId) {
        jdbc.update("DELETE FROM ai_document_chunk WHERE doc_id = ?", docId);
    }

    private void replaceSharedDepts(long kbId, List<Long> sharedDeptIds) {
        jdbc.update("DELETE FROM ai_kb_share_dept WHERE kb_id = ?", kbId);
        if (sharedDeptIds == null || sharedDeptIds.isEmpty()) {
            return;
        }
        List<Object[]> batch = sharedDeptIds.stream()
                .distinct()
                .map(deptId -> new Object[]{kbId, deptId})
                .toList();
        jdbc.batchUpdate("INSERT INTO ai_kb_share_dept (kb_id, dept_id) VALUES (?, ?)", batch);
    }

    private List<Long> findSharedDeptIds(long kbId) {
        return jdbc.queryForList("SELECT dept_id FROM ai_kb_share_dept WHERE kb_id = ? ORDER BY dept_id",
                Long.class, kbId);
    }

    private Map<Long, List<Long>> findAllSharedDepts() {
        Map<Long, List<Long>> result = new HashMap<>();
        jdbc.query("SELECT kb_id, dept_id FROM ai_kb_share_dept ORDER BY kb_id, dept_id", rs -> {
            result.computeIfAbsent(rs.getLong("kb_id"), key -> new ArrayList<>()).add(rs.getLong("dept_id"));
        });
        return result;
    }

    private KnowledgeBase mapKnowledgeBase(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeBase(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("visibility"),
                rs.getLong("owner_user_id"),
                rs.getObject("owner_dept_id", Long.class),
                List.of(),
                rs.getString("created_by"),
                rs.getObject("create_time", LocalDateTime.class),
                rs.getObject("update_time", LocalDateTime.class));
    }

    private KnowledgeDocument mapDocument(ResultSet rs, int rowNum) throws SQLException {
        return new KnowledgeDocument(
                rs.getLong("id"),
                rs.getLong("kb_id"),
                rs.getString("file_name"),
                rs.getString("file_ext"),
                rs.getLong("file_size"),
                rs.getString("mime_type"),
                rs.getString("storage_key"),
                rs.getString("content_hash"),
                rs.getLong("owner_user_id"),
                rs.getLong("owner_dept_id"),
                DocumentStatus.valueOf(rs.getString("status")),
                rs.getString("error_code"),
                rs.getString("error_message"),
                rs.getInt("chunk_count"),
                rs.getInt("attempts"),
                rs.getObject("next_attempt_at", LocalDateTime.class),
                rs.getObject("create_time", LocalDateTime.class),
                rs.getObject("update_time", LocalDateTime.class));
    }
}
