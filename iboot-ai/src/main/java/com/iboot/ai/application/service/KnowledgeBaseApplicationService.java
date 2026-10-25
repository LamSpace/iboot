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

package com.iboot.ai.application.service;

import com.iboot.ai.domain.knowledge.DocumentChunk;
import com.iboot.ai.domain.knowledge.KnowledgeBase;
import com.iboot.ai.domain.knowledge.KnowledgeDocument;
import com.iboot.ai.domain.knowledge.KnowledgeStorage;
import com.iboot.ai.infrastructure.persistence.KnowledgeRepository;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.interfaces.dto.DocumentChunkResponse;
import com.iboot.ai.interfaces.dto.KnowledgeBaseRequest;
import com.iboot.ai.interfaces.dto.KnowledgeBaseResponse;
import com.iboot.ai.interfaces.dto.KnowledgeDocumentResponse;
import com.iboot.ai.interfaces.dto.KnowledgeSubmitResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 知识库管理应用服务。
 *
 * <p>负责知识库 CRUD、文档列表/状态/重试/删除与下载地址；所有入口先校验当前用户的知识库
 * 可见范围，修改与删除额外要求所有者或超级管理员。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeBaseApplicationService {

    private final KnowledgeRepository repository;

    private final KnowledgeAccessService accessService;

    private final KnowledgeIngestionService ingestionService;

    private final KnowledgeStorage storage;

    /**
     * 创建服务。
     *
     * @param repository       元数据仓储
     * @param accessService    权限范围计算
     * @param ingestionService 入库服务
     * @param storage          对象存储
     */
    public KnowledgeBaseApplicationService(KnowledgeRepository repository, KnowledgeAccessService accessService,
                                           KnowledgeIngestionService ingestionService, KnowledgeStorage storage) {
        this.repository = repository;
        this.accessService = accessService;
        this.ingestionService = ingestionService;
        this.storage = storage;
    }

    /**
     * 创建知识库。
     *
     * @param scope   当前用户权限范围
     * @param request 创建请求
     * @return 知识库
     */
    public KnowledgeBaseResponse create(PermissionScope scope, KnowledgeBaseRequest request) {
        int visibility = request.visibility() == null ? 5 : request.visibility();
        List<Long> sharedDeptIds = resolveSharedDepts(scope, visibility, scope.deptId(),
                request.sharedDeptIds());
        long id = repository.insertKnowledgeBase(request.name(), request.description(), visibility,
                scope.userId(), scope.deptId(), scope.username(), sharedDeptIds);
        return get(scope, id);
    }

    /**
     * 查询当前用户可见的知识库。
     *
     * @param scope 当前用户权限范围
     * @return 知识库列表
     */
    public List<KnowledgeBaseResponse> list(PermissionScope scope) {
        Set<Long> visible = new HashSet<>(accessService.visibleKnowledgeBaseIds(scope,
                repository.findKnowledgeBaseSummaries()));
        return repository.findAllKnowledgeBases().stream()
                .filter(base -> visible.contains(base.id()))
                .map(this::toResponse)
                .toList();
    }

    /**
     * 查询知识库。
     *
     * @param scope 当前用户权限范围
     * @param id    知识库 ID
     * @return 知识库
     */
    public KnowledgeBaseResponse get(PermissionScope scope, long id) {
        return toResponse(requireVisibleKb(scope, id));
    }

    /**
     * 更新知识库。
     *
     * @param scope   当前用户权限范围
     * @param id      知识库 ID
     * @param request 更新请求
     * @return 更新后的知识库
     */
    public KnowledgeBaseResponse update(PermissionScope scope, long id, KnowledgeBaseRequest request) {
        KnowledgeBase base = requireVisibleKb(scope, id);
        requireOwner(scope, base);
        int visibility = request.visibility() == null ? base.visibility() : request.visibility();
        List<Long> sharedDeptIds = resolveSharedDepts(scope, visibility, base.ownerDeptId(),
                request.sharedDeptIds());
        repository.updateKnowledgeBase(id, request.name(), request.description(), visibility,
                base.ownerDeptId(), sharedDeptIds);
        return get(scope, id);
    }

    /**
     * 删除知识库。
     *
     * @param scope 当前用户权限范围
     * @param id    知识库 ID
     */
    public void delete(PermissionScope scope, long id) {
        KnowledgeBase base = requireVisibleKb(scope, id);
        requireOwner(scope, base);
        ingestionService.deleteKnowledgeBase(id);
    }

    /**
     * 查询知识库下的文档。
     *
     * @param scope 当前用户权限范围
     * @param kbId  知识库 ID
     * @return 文档列表
     */
    public List<KnowledgeDocumentResponse> documents(PermissionScope scope, long kbId) {
        requireVisibleKb(scope, kbId);
        return repository.findDocumentsByKb(kbId).stream().map(this::toDocumentResponse).toList();
    }

    /**
     * 查询文档。
     *
     * @param scope 当前用户权限范围
     * @param docId 文档 ID
     * @return 文档
     */
    public KnowledgeDocumentResponse document(PermissionScope scope, long docId) {
        return toDocumentResponse(requireDocument(scope, docId));
    }

    /**
     * 上传文档。
     *
     * @param scope    当前用户权限范围
     * @param kbId     知识库 ID
     * @param fileName 文件名
     * @param content  文件内容
     * @param mimeType MIME 类型
     * @return 上传结果
     */
    public KnowledgeSubmitResponse upload(PermissionScope scope, long kbId, String fileName, byte[] content,
                                          String mimeType) {
        requireVisibleKb(scope, kbId);
        long ownerDeptId = scope.deptId() == null ? 0L : scope.deptId();
        KnowledgeIngestionService.SubmitResult result = ingestionService.submit(kbId, fileName, content, mimeType,
                scope.userId(), ownerDeptId);
        return new KnowledgeSubmitResponse(result.documentId(), result.duplicate());
    }

    /**
     * 重试文档入库。
     *
     * @param scope 当前用户权限范围
     * @param docId 文档 ID
     */
    public void retry(PermissionScope scope, long docId) {
        requireDocument(scope, docId);
        ingestionService.retry(docId);
    }

    /**
     * 删除文档。
     *
     * @param scope 当前用户权限范围
     * @param docId 文档 ID
     */
    public void deleteDocument(PermissionScope scope, long docId) {
        requireDocument(scope, docId);
        ingestionService.deleteDocument(docId);
    }

    /**
     * 查询文档分块。
     *
     * @param scope 当前用户权限范围
     * @param docId 文档 ID
     * @return 分块列表
     */
    public List<DocumentChunkResponse> chunks(PermissionScope scope, long docId) {
        requireDocument(scope, docId);
        return repository.findChunks(docId).stream()
                .map(chunk -> new DocumentChunkResponse(chunk.chunkIndex(), chunk.pageNo(), chunk.headingPath(),
                        chunk.content()))
                .toList();
    }

    /**
     * 生成文档下载地址。
     *
     * @param scope 当前用户权限范围
     * @param docId 文档 ID
     * @return 短时效下载地址
     */
    public String downloadUrl(PermissionScope scope, long docId) {
        KnowledgeDocument document = requireDocument(scope, docId);
        return storage.presignedGetUrl(document.storageKey(), Duration.ofMinutes(5));
    }

    private KnowledgeBase requireVisibleKb(PermissionScope scope, long kbId) {
        if (scope == null) {
            throw new RetrievalUnavailableException("缺少权限范围");
        }
        List<Long> visible = accessService.visibleKnowledgeBaseIds(scope,
                repository.findKnowledgeBaseSummaries());
        if (!visible.contains(kbId)) {
            throw new KnowledgeAccessDeniedException("无权访问知识库：" + kbId);
        }
        return repository.findKnowledgeBase(kbId)
                .orElseThrow(() -> new KnowledgeAccessDeniedException("知识库不存在：" + kbId));
    }

    private KnowledgeDocument requireDocument(PermissionScope scope, long docId) {
        KnowledgeDocument document = repository.findDocument(docId)
                .orElseThrow(() -> new KnowledgeAccessDeniedException("文档不存在：" + docId));
        requireVisibleKb(scope, document.kbId());
        return document;
    }

    private void requireOwner(PermissionScope scope, KnowledgeBase base) {
        if (!scope.superAdmin() && !Objects.equals(base.ownerUserId(), scope.userId())) {
            throw new KnowledgeAccessDeniedException("只有知识库所有者可以修改或删除");
        }
    }

    private List<Long> resolveSharedDepts(PermissionScope scope, int visibility, Long ownerDeptId,
                                          List<Long> requested) {
        return switch (visibility) {
            case 1, 5 -> List.of();
            case 2 -> requested == null ? List.of() : requested.stream()
                    .filter(Objects::nonNull).distinct().toList();
            case 3 -> ownerDeptId == null ? List.of() : List.of(ownerDeptId);
            case 4 -> {
                List<Long> visible = scope.kbVisibleDeptIds();
                yield visible.isEmpty() && ownerDeptId != null ? List.of(ownerDeptId) : visible;
            }
            default -> throw new IllegalArgumentException("不支持的可见范围：" + visibility);
        };
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase base) {
        return new KnowledgeBaseResponse(base.id(), base.name(), base.description(), base.visibility(),
                base.ownerUserId(), base.ownerDeptId(), base.sharedDeptIds(), base.createTime());
    }

    private KnowledgeDocumentResponse toDocumentResponse(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(document.id(), document.kbId(), document.fileName(),
                document.fileExt(), document.fileSize(), document.status().name(), document.errorCode(),
                document.errorMessage(), document.chunkCount(), document.createTime());
    }
}
