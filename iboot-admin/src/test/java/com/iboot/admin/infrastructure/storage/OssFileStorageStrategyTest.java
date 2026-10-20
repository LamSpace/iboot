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

package com.iboot.admin.infrastructure.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.VoidResult;
import com.iboot.admin.domain.system.model.StorageObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阿里云 OSS 存储策略的 SDK 调用映射测试。
 *
 * <p>使用 mock {@link OSS} 接口验证方法到 SDK 调用的映射，不访问真实云资源。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see OssFileStorageStrategy
 */
class OssFileStorageStrategyTest {

    private static final String BUCKET = "iboot-files";

    private final OSS oss = mock(OSS.class);

    private final OssProperties properties = mock(OssProperties.class);

    private final OssFileStorageStrategy strategy = new OssFileStorageStrategy(properties, oss);

    @Test
    void savePutsStreamWithContentMetadata() throws Exception {
        byte[] content = "hello oss".getBytes(StandardCharsets.UTF_8);
        when(properties.getBucketName()).thenReturn(BUCKET);
        when(oss.putObject(eq(BUCKET), eq("2026/10/19/a.txt"), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(new PutObjectResult());

        String path = strategy.save(StorageObject.builder()
                .inputStream(new ByteArrayInputStream(content))
                .size((long) content.length)
                .contentType("text/plain")
                .fileName("a.txt")
                .build(), "2026/10/19/a.txt");

        assertEquals("2026/10/19/a.txt", path);
        ArgumentCaptor<ObjectMetadata> metadata = ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(oss).putObject(eq(BUCKET), eq("2026/10/19/a.txt"), any(InputStream.class), metadata.capture());
        assertEquals("text/plain", metadata.getValue().getContentType());
        assertEquals(content.length, metadata.getValue().getContentLength());
    }

    @Test
    void getFileStreamReturnsContentAndMetadata() throws Exception {
        byte[] content = "oss content".getBytes(StandardCharsets.UTF_8);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.setContentType("text/plain");
        OSSObject ossObject = mock(OSSObject.class);
        when(properties.getBucketName()).thenReturn(BUCKET);
        when(oss.getObject(BUCKET, "2026/10/19/a.txt")).thenReturn(ossObject);
        when(ossObject.getObjectContent()).thenReturn(new ByteArrayInputStream(content));
        when(ossObject.getObjectMetadata()).thenReturn(metadata);

        StorageObject stored = strategy.getFileStream("2026/10/19/a.txt");

        assertEquals(content.length, stored.getSize());
        assertEquals("text/plain", stored.getContentType());
        assertEquals("a.txt", stored.getFileName());
        assertArrayEquals(content, stored.getInputStream().readAllBytes());
    }

    @Test
    void getFileUrlGeneratesPresignedUrl() throws Exception {
        URL presigned = new URL("https://iboot-files.oss-cn-hangzhou.aliyuncs.com/a.txt?sign=x");
        when(properties.getBucketName()).thenReturn(BUCKET);
        when(properties.getUrlExpireSeconds()).thenReturn(3600);
        when(oss.generatePresignedUrl(eq(BUCKET), eq("a.txt"), any(Date.class))).thenReturn(presigned);

        assertEquals(presigned.toString(), strategy.getFileUrl("a.txt"));
    }

    @Test
    void existsDelegatesToSdk() {
        when(properties.getBucketName()).thenReturn(BUCKET);
        when(oss.doesObjectExist(BUCKET, "a.txt")).thenReturn(true);

        assertTrue(strategy.exists("a.txt"));
    }

    @Test
    void deleteRemovesExistingObject() {
        when(properties.getBucketName()).thenReturn(BUCKET);
        when(oss.doesObjectExist(BUCKET, "a.txt")).thenReturn(true);
        when(oss.deleteObject(BUCKET, "a.txt")).thenReturn(new VoidResult());

        assertTrue(strategy.delete("a.txt"));
        verify(oss).deleteObject(BUCKET, "a.txt");
    }

    @Test
    void deleteMissingObjectReturnsFalseWithoutDeleting() {
        when(properties.getBucketName()).thenReturn(BUCKET);
        when(oss.doesObjectExist(BUCKET, "missing.txt")).thenReturn(false);

        assertFalse(strategy.delete("missing.txt"));
        verify(oss, never()).deleteObject(BUCKET, "missing.txt");
    }

    @Test
    void reportsOssProviderSemantics() {
        assertEquals("oss", strategy.getStorageType());
        assertTrue(strategy.isObjectStorage());
    }
}
