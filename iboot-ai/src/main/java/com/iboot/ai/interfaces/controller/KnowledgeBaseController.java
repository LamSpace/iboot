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

package com.iboot.ai.interfaces.controller;

import com.iboot.ai.application.service.KnowledgeBaseApplicationService;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.infrastructure.security.PermissionScopeClient;
import com.iboot.ai.interfaces.dto.DocumentChunkResponse;
import com.iboot.ai.interfaces.dto.KnowledgeBaseRequest;
import com.iboot.ai.interfaces.dto.KnowledgeBaseResponse;
import com.iboot.ai.interfaces.dto.KnowledgeDocumentResponse;
import com.iboot.ai.interfaces.dto.KnowledgeSubmitResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 知识库与文档管理接口。
 *
 * <p>读接口要求 {@code ai:kb:query}，写接口要求 {@code ai:kb:manage}；所有请求都先取得
 * 当前用户权限范围，再由应用服务做可见性与所有者校验。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/ai")
@ConditionalOnProperty(prefix = "iboot.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeBaseController {

    private final KnowledgeBaseApplicationService service;

    private final PermissionScopeClient permissionScopeClient;

    /**
     * 创建控制器。
     *
     * @param service               知识库应用服务
     * @param permissionScopeClient 权限范围客户端
     */
    public KnowledgeBaseController(KnowledgeBaseApplicationService service,
                                   PermissionScopeClient permissionScopeClient) {
        this.service = service;
        this.permissionScopeClient = permissionScopeClient;
    }

    /**
     * 创建知识库。
     *
     * @param request 请求
     * @return 知识库
     */
    @PostMapping("/knowledge-bases")
    @PreAuthorize("hasAuthority('ai:kb:manage')")
    public KnowledgeBaseResponse create(@Valid @RequestBody KnowledgeBaseRequest request) {
        return service.create(scope(), request);
    }

    /**
     * 查询可见知识库。
     *
     * @return 知识库列表
     */
    @GetMapping("/knowledge-bases")
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public List<KnowledgeBaseResponse> list() {
        return service.list(scope());
    }

    /**
     * 查询知识库。
     *
     * @param id 知识库 ID
     * @return 知识库
     */
    @GetMapping("/knowledge-bases/{id}")
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public KnowledgeBaseResponse get(@PathVariable long id) {
        return service.get(scope(), id);
    }

    /**
     * 更新知识库。
     *
     * @param id      知识库 ID
     * @param request 请求
     * @return 更新后的知识库
     */
    @PutMapping("/knowledge-bases/{id}")
    @PreAuthorize("hasAuthority('ai:kb:manage')")
    public KnowledgeBaseResponse update(@PathVariable long id,
                                        @Valid @RequestBody KnowledgeBaseRequest request) {
        return service.update(scope(), id, request);
    }

    /**
     * 删除知识库。
     *
     * @param id 知识库 ID
     */
    @DeleteMapping("/knowledge-bases/{id}")
    @PreAuthorize("hasAuthority('ai:kb:manage')")
    public void delete(@PathVariable long id) {
        service.delete(scope(), id);
    }

    /**
     * 上传文档。
     *
     * @param kbId 知识库 ID
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/knowledge-bases/{kbId}/documents")
    @PreAuthorize("hasAuthority('ai:kb:manage')")
    public KnowledgeSubmitResponse upload(@PathVariable long kbId, @RequestParam("file") MultipartFile file) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("读取上传文件失败", e);
        }
        String fileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        return service.upload(scope(), kbId, fileName, content, file.getContentType());
    }

    /**
     * 查询知识库文档。
     *
     * @param kbId 知识库 ID
     * @return 文档列表
     */
    @GetMapping("/knowledge-bases/{kbId}/documents")
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public List<KnowledgeDocumentResponse> documents(@PathVariable long kbId) {
        return service.documents(scope(), kbId);
    }

    /**
     * 查询文档。
     *
     * @param id 文档 ID
     * @return 文档
     */
    @GetMapping("/knowledge-documents/{id}")
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public KnowledgeDocumentResponse document(@PathVariable long id) {
        return service.document(scope(), id);
    }

    /**
     * 重试文档入库。
     *
     * @param id 文档 ID
     */
    @PostMapping("/knowledge-documents/{id}/retry")
    @PreAuthorize("hasAuthority('ai:kb:manage')")
    public void retry(@PathVariable long id) {
        service.retry(scope(), id);
    }

    /**
     * 重建文档索引。
     *
     * @param id 文档 ID
     */
    @PostMapping("/knowledge-documents/{id}/reindex")
    @PreAuthorize("hasAuthority('ai:kb:manage')")
    public void reindex(@PathVariable long id) {
        service.retry(scope(), id);
    }

    /**
     * 删除文档。
     *
     * @param id 文档 ID
     */
    @DeleteMapping("/knowledge-documents/{id}")
    @PreAuthorize("hasAuthority('ai:kb:manage')")
    public void deleteDocument(@PathVariable long id) {
        service.deleteDocument(scope(), id);
    }

    /**
     * 查询文档分块。
     *
     * @param id 文档 ID
     * @return 分块列表
     */
    @GetMapping("/knowledge-documents/{id}/chunks")
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public List<DocumentChunkResponse> chunks(@PathVariable long id) {
        return service.chunks(scope(), id);
    }

    /**
     * 生成文档下载地址。
     *
     * @param id 文档 ID
     * @return 下载地址
     */
    @GetMapping("/knowledge-documents/{id}/download")
    @PreAuthorize("hasAuthority('ai:kb:query')")
    public Map<String, String> download(@PathVariable long id) {
        return Map.of("url", service.downloadUrl(scope(), id));
    }

    private PermissionScope scope() {
        return permissionScopeClient.fetchCurrentUserScope();
    }
}
