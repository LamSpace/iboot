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

import java.io.InputStream;

/**
 * 存储对象DTO 封装从存储系统获取的文件流和元数据
 *
 * @author iBoot
 */
public class StorageObject {

    /**
     * 文件输入流
     */
    private InputStream inputStream;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * MIME类型（如 image/png）
     */
    private String contentType;

    /**
     * 文件名
     */
    private String fileName;

    @SuppressWarnings("all")
    public StorageObject() {
    }

    /**
     * Creates a new {@code StorageObject} instance.
     *
     * @param inputStream 文件输入流
     * @param size        文件大小（字节）
     * @param contentType MIME类型（如 image/png）
     * @param fileName    文件名
     */
    @SuppressWarnings("all")
    public StorageObject(final InputStream inputStream, final Long size, final String contentType,
                         final String fileName) {
        this.inputStream = inputStream;
        this.size = size;
        this.contentType = contentType;
        this.fileName = fileName;
    }

    @SuppressWarnings("all")
    public static StorageObject.StorageObjectBuilder builder() {
        return new StorageObject.StorageObjectBuilder();
    }

    /**
     * 文件输入流
     */
    @SuppressWarnings("all")
    public InputStream getInputStream() {
        return this.inputStream;
    }

    /**
     * 文件输入流
     */
    @SuppressWarnings("all")
    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * 文件大小（字节）
     */
    @SuppressWarnings("all")
    public Long getSize() {
        return this.size;
    }

    /**
     * 文件大小（字节）
     */
    @SuppressWarnings("all")
    public void setSize(final Long size) {
        this.size = size;
    }

    /**
     * MIME类型（如 image/png）
     */
    @SuppressWarnings("all")
    public String getContentType() {
        return this.contentType;
    }

    /**
     * MIME类型（如 image/png）
     */
    @SuppressWarnings("all")
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * 文件名
     */
    @SuppressWarnings("all")
    public String getFileName() {
        return this.fileName;
    }

    /**
     * 文件名
     */
    @SuppressWarnings("all")
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StorageObject))
            return false;
        final StorageObject other = (StorageObject) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size))
            return false;
        final java.lang.Object this$inputStream = this.getInputStream();
        final java.lang.Object other$inputStream = other.getInputStream();
        if (this$inputStream == null ? other$inputStream != null : !this$inputStream.equals(other$inputStream))
            return false;
        final java.lang.Object this$contentType = this.getContentType();
        final java.lang.Object other$contentType = other.getContentType();
        if (this$contentType == null ? other$contentType != null : !this$contentType.equals(other$contentType))
            return false;
        final java.lang.Object this$fileName = this.getFileName();
        final java.lang.Object other$fileName = other.getFileName();
        if (this$fileName == null ? other$fileName != null : !this$fileName.equals(other$fileName))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof StorageObject;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        final java.lang.Object $inputStream = this.getInputStream();
        result = result * PRIME + ($inputStream == null ? 43 : $inputStream.hashCode());
        final java.lang.Object $contentType = this.getContentType();
        result = result * PRIME + ($contentType == null ? 43 : $contentType.hashCode());
        final java.lang.Object $fileName = this.getFileName();
        result = result * PRIME + ($fileName == null ? 43 : $fileName.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "StorageObject(inputStream=" + this.getInputStream() + ", size=" + this.getSize() + ", contentType="
                + this.getContentType() + ", fileName=" + this.getFileName() + ")";
    }

    @SuppressWarnings("all")
    public static class StorageObjectBuilder {

        @SuppressWarnings("all")
        private InputStream inputStream;

        @SuppressWarnings("all")
        private Long size;

        @SuppressWarnings("all")
        private String contentType;

        @SuppressWarnings("all")
        private String fileName;

        @SuppressWarnings("all")
        StorageObjectBuilder() {
        }

        /**
         * 文件输入流
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public StorageObject.StorageObjectBuilder inputStream(final InputStream inputStream) {
            this.inputStream = inputStream;
            return this;
        }

        /**
         * 文件大小（字节）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public StorageObject.StorageObjectBuilder size(final Long size) {
            this.size = size;
            return this;
        }

        /**
         * MIME类型（如 image/png）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public StorageObject.StorageObjectBuilder contentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * 文件名
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public StorageObject.StorageObjectBuilder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        @SuppressWarnings("all")
        public StorageObject build() {
            return new StorageObject(this.inputStream, this.size, this.contentType, this.fileName);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "StorageObject.StorageObjectBuilder(inputStream=" + this.inputStream + ", size=" + this.size
                    + ", contentType=" + this.contentType + ", fileName=" + this.fileName + ")";
        }

    }

}
