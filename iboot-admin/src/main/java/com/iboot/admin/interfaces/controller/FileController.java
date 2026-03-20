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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.FileApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.FileInfo;
import com.iboot.admin.infrastructure.security.SecurityUtils;
import com.iboot.admin.interfaces.dto.export.FileExportVO;
import com.iboot.admin.interfaces.dto.request.FileUpdateRequest;
import com.iboot.admin.interfaces.dto.response.FileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件管理控制器
 *
 * @author iBoot
 */
@Tag(name = "文件管理", description = "文件上传下载管理相关接口")
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileApplicationService fileApplicationService;

    @SuppressWarnings("all")
    public FileController(final FileApplicationService fileApplicationService) {
        this.fileApplicationService = fileApplicationService;
    }

    @Operation(summary = "上传文件")
    @PostMapping(version = "1", value = "/upload")
    @PreAuthorize("hasAuthority('file:upload')")
    @Log(title = "文件管理", businessType = BusinessTypeEnum.INSERT)
    public Result<FileResponse> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "fileCategory", required = false) String fileCategory) {
        String username = SecurityUtils.getCurrentUsername();
        FileInfo fileInfo = fileApplicationService.uploadFile(file, fileCategory, username);
        return Result.success(convertToResponse(fileInfo));
    }

    @Operation(summary = "查询文件列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('file:list')")
    public Result<PageResult<FileResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String fileName,
                                                 @RequestParam(required = false) String fileCategory,
                                                 @RequestParam(required = false) String fileExt) {
        List<FileInfo> files;
        long total;
        if (fileName != null || fileCategory != null || fileExt != null) {
            files = fileApplicationService.getFilePageByCondition(fileName, fileCategory, fileExt, pageNum, pageSize);
            total = fileApplicationService.countFilesByCondition(fileName, fileCategory, fileExt);
        } else {
            files = fileApplicationService.getFilePage(pageNum, pageSize);
            total = fileApplicationService.countFiles();
        }
        List<FileResponse> responses = files.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageResult<FileResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @Operation(summary = "查询文件详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('file:list')")
    public Result<FileResponse> getById(@PathVariable Long id) {
        FileInfo fileInfo = fileApplicationService.getFileById(id);
        return Result.success(convertToResponse(fileInfo));
    }

    @Operation(summary = "获取文件下载URL", description = "MinIO模式返回预签名URL，本地模式返回下载接口路径")
    @GetMapping(version = "1", value = "/download-url/{id}")
    @PreAuthorize("hasAuthority('file:download')")
    public Result<Map<String, String>> getDownloadUrl(@PathVariable Long id) {
        String downloadUrl = fileApplicationService.getFileDownloadUrl(id);
        String storageType = fileApplicationService.getStorageType();
        // 如果是本地存储，返回完整的下载接口路径
        if ("local".equals(storageType)) {
            downloadUrl = "/api/file/download/" + id;
        }
        return Result.success(Map.of("url", downloadUrl, "storageType", storageType));
    }

    @Operation(summary = "下载文件")
    @GetMapping(version = "1", value = "/download/{id}")
    @PreAuthorize("hasAuthority('file:download')")
    @Log(title = "文件管理", businessType = BusinessTypeEnum.EXPORT)
    public ResponseEntity<?> download(@PathVariable Long id) throws IOException {
        FileInfo fileInfo = fileApplicationService.getFileById(id);
        // MinIO模式：重定向到预签名URL
        if (fileApplicationService.isMinioStorage()) {
            String presignedUrl = fileApplicationService.getFileDownloadUrl(id);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(presignedUrl)).build();
        }
        // 本地模式：返回文件流
        Path diskPath = fileApplicationService.getFileDiskPath(fileInfo);
        Resource resource = new UrlResource(diskPath.toUri());
        if (!resource.exists()) {
            throw new IOException("文件不存在于磁盘：" + fileInfo.getFilePath());
        }
        String encodedFileName = URLEncoder.encode(fileInfo.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @Operation(summary = "修改文件信息")
    @PutMapping
    @PreAuthorize("hasAuthority('file:list')")
    @Log(title = "文件管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody FileUpdateRequest request) {
        FileInfo fileInfo = FileInfo.builder()
                .id(request.getId())
                .fileName(request.getFileName())
                .fileCategory(request.getFileCategory())
                .remark(request.getRemark())
                .updateBy(SecurityUtils.getCurrentUsername())
                .build();
        fileApplicationService.updateFileInfo(fileInfo);
        return Result.success();
    }

    @Operation(summary = "删除文件")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('file:remove')")
    @Log(title = "文件管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        fileApplicationService.deleteFile(id);
        return Result.success();
    }

    @Operation(summary = "导出文件列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('file:export')")
    @Log(title = "文件管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String fileName,
                       @RequestParam(required = false) String fileCategory) throws IOException {
        List<FileInfo> files;
        if (fileName != null || fileCategory != null) {
            files = fileApplicationService.getAllFilesByCondition(fileName, fileCategory);
        } else {
            files = fileApplicationService.getAllFiles();
        }
        List<FileExportVO> exportList = files.stream().map(this::convertToExportVO).collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, FileExportVO.class, "文件列表", "文件数据");
    }

    private FileResponse convertToResponse(FileInfo fileInfo) {
        return FileResponse.builder()
                .id(fileInfo.getId())
                .fileName(fileInfo.getFileName())
                .fileSize(fileInfo.getFileSize())
                .readableSize(fileInfo.getReadableSize())
                .fileType(fileInfo.getFileType())
                .fileExt(fileInfo.getFileExt())
                .fileCategory(fileInfo.getFileCategory())
                .uploadBy(fileInfo.getUploadBy())
                .uploadTime(fileInfo.getUploadTime())
                .remark(fileInfo.getRemark())
                .createTime(fileInfo.getCreateTime())
                .build();
    }

    private FileExportVO convertToExportVO(FileInfo fileInfo) {
        return FileExportVO.builder()
                .id(fileInfo.getId())
                .fileName(fileInfo.getFileName())
                .readableSize(fileInfo.getReadableSize())
                .fileType(fileInfo.getFileType())
                .fileExt(fileInfo.getFileExt())
                .fileCategory(fileInfo.getFileCategory())
                .uploadBy(fileInfo.getUploadBy())
                .uploadTime(fileInfo.getUploadTime())
                .remark(fileInfo.getRemark())
                .createTime(fileInfo.getCreateTime())
                .build();
    }

}
