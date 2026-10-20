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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.FileInfo;
import com.iboot.admin.domain.system.repository.FileRepository;
import com.iboot.admin.domain.system.service.FileStorageStrategy;
import com.iboot.admin.infrastructure.storage.InMemoryFileStorageStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 文件应用服务的存储协作测试。
 *
 * <p>使用内存存储实现与 mock 仓储验证上传、删除、下载 URL 以及上传校验在调用存储前生效。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see FileApplicationService
 */
class FileApplicationServiceTest {

    private FileRepository fileRepository;

    private ConfigApplicationService configApplicationService;

    private InMemoryFileStorageStrategy storage;

    private FileApplicationService service;

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        configApplicationService = mock(ConfigApplicationService.class);
        storage = new InMemoryFileStorageStrategy();
        service = new FileApplicationService(fileRepository, configApplicationService, storage);
        when(configApplicationService.getConfigValue("sys.file.maxSize")).thenReturn("50");
        when(fileRepository.save(any(FileInfo.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void uploadStoresContentAndPersistsMetadata() throws IOException {
        byte[] content = "文件内容".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "报告.txt", "text/plain", content);

        FileInfo saved = service.uploadFile(file, null, "tester");

        assertNotNull(saved.getFilePath());
        assertTrue(saved.getFilePath().endsWith(".txt"));
        assertTrue(storage.exists(saved.getFilePath()));
        assertArrayEquals(content, storage.getFileStream(saved.getFilePath()).getInputStream().readAllBytes());
        verify(fileRepository).save(any(FileInfo.class));
    }

    @Test
    void deleteRemovesStorageObjectAndRepositoryRecord() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain",
                "delete me".getBytes(StandardCharsets.UTF_8));
        FileInfo saved = service.uploadFile(file, null, "tester");
        FileInfo existing = FileInfo.builder().id(1L).filePath(saved.getFilePath()).build();
        when(fileRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(fileRepository.deleteById(1L)).thenReturn(true);

        boolean deleted = service.deleteFile(1L);

        assertTrue(deleted);
        assertFalse(storage.exists(saved.getFilePath()));
    }

    @Test
    void downloadUrlComesFromActiveStrategy() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain",
                "url".getBytes(StandardCharsets.UTF_8));
        FileInfo saved = service.uploadFile(file, null, "tester");
        when(fileRepository.findById(1L))
                .thenReturn(Optional.of(FileInfo.builder().id(1L).filePath(saved.getFilePath()).build()));

        String url = service.getFileDownloadUrl(1L);

        assertEquals("memory://" + saved.getFilePath(), url);
        assertFalse(service.isObjectStorage());
    }

    @Test
    void emptyFileIsRejectedBeforeStorageIsCalled() {
        FileStorageStrategy strategy = mock(FileStorageStrategy.class);
        FileApplicationService guarded = new FileApplicationService(
                fileRepository, configApplicationService, strategy);
        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(BusinessException.class, () -> guarded.uploadFile(empty, null, "tester"));
        verifyNoInteractions(strategy);
    }

    @Test
    void oversizedFileIsRejectedBeforeStorageIsCalled() {
        when(configApplicationService.getConfigValue("sys.file.maxSize")).thenReturn("1");
        FileStorageStrategy strategy = mock(FileStorageStrategy.class);
        FileApplicationService guarded = new FileApplicationService(
                fileRepository, configApplicationService, strategy);
        MockMultipartFile oversized = new MockMultipartFile("file", "big.txt", "text/plain", new byte[1024 * 1024 + 1]);

        assertThrows(BusinessException.class, () -> guarded.uploadFile(oversized, null, "tester"));
        verifyNoInteractions(strategy);
    }

    @Test
    void disallowedExtensionIsRejectedBeforeStorageIsCalled() {
        when(configApplicationService.getConfigValue("sys.file.allowedExtensions")).thenReturn("txt,png");
        FileStorageStrategy strategy = mock(FileStorageStrategy.class);
        FileApplicationService guarded = new FileApplicationService(
                fileRepository, configApplicationService, strategy);
        MockMultipartFile executable = new MockMultipartFile("file", "evil.exe", "application/octet-stream",
                "x".getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessException.class, () -> guarded.uploadFile(executable, null, "tester"));
        verifyNoInteractions(strategy);
    }
}
