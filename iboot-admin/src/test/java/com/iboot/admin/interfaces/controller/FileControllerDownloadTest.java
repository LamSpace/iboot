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
import com.iboot.admin.domain.system.model.FileInfo;
import com.iboot.admin.domain.system.model.StorageObject;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 文件下载接口的存储模式分支测试。
 *
 * <p>验证对象存储模式返回 302 预签名 URL，本地模式输出文件流并保留非 ASCII 文件名。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see FileController
 */
class FileControllerDownloadTest {

    private final FileApplicationService fileApplicationService = mock(FileApplicationService.class);

    private final FileController controller = new FileController(fileApplicationService);

    @Test
    void objectStorageModeRedirectsToPresignedUrl() throws Exception {
        FileInfo fileInfo = fileInfo();
        when(fileApplicationService.getFileById(1L)).thenReturn(fileInfo);
        when(fileApplicationService.isObjectStorage()).thenReturn(true);
        when(fileApplicationService.getFileDownloadUrl(1L))
                .thenReturn("https://oss.example.com/iboot-files/2026/10/19/a.txt?sign=x");

        ResponseEntity<?> response = controller.download(1L);

        assertEquals(302, response.getStatusCode().value());
        assertEquals(URI.create("https://oss.example.com/iboot-files/2026/10/19/a.txt?sign=x"),
                response.getHeaders().getLocation());
    }

    @Test
    void localModeStreamsContentWithEncodedChineseFilename() throws Exception {
        byte[] content = "文件内容".getBytes(StandardCharsets.UTF_8);
        FileInfo fileInfo = fileInfo();
        when(fileApplicationService.getFileById(1L)).thenReturn(fileInfo);
        when(fileApplicationService.isObjectStorage()).thenReturn(false);
        when(fileApplicationService.getFileStream(1L)).thenReturn(StorageObject.builder()
                .inputStream(new ByteArrayInputStream(content))
                .size((long) content.length)
                .contentType("text/plain")
                .fileName("c9671d7eebdf463094d3fc98b2ded11c.txt")
                .build());

        ResponseEntity<?> response = controller.download(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("text/plain", response.getHeaders().getContentType().toString());
        assertEquals(content.length, response.getHeaders().getContentLength());
        assertNotNull(response.getHeaders().getContentDisposition());
        assertEquals("报告.txt", response.getHeaders().getContentDisposition().getFilename());
        assertTrue(response.getHeaders().getContentDisposition().isAttachment());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("filename*=UTF-8''"));
        byte[] actual = ((InputStreamResource) response.getBody()).getInputStream().readAllBytes();
        assertArrayEquals(content, actual);
    }

    private FileInfo fileInfo() {
        return FileInfo.builder()
                .id(1L)
                .fileName("报告.txt")
                .filePath("2026/10/19/a.txt")
                .build();
    }
}
