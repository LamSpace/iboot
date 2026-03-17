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

package com.iboot.admin.domain.system.model;

import java.time.LocalDateTime;

/**
 * 文件信息实体
 *
 * @author iBoot
 */
public class FileInfo {

    /**
     * 文件ID
     */
    private Long id;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 存储路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * MIME类型
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExt;

    /**
     * 文件分类（关联字典 sys_file_category）
     */
    private String fileCategory;

    /**
     * 上传人
     */
    private String uploadBy;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;

    /**
     * 备注
     */
    private String remark;

    @SuppressWarnings("all")
    public FileInfo() {
    }

    /**
     * Creates a new {@code FileInfo} instance.
     *
     * @param id           文件ID
     * @param fileName     原始文件名
     * @param filePath     存储路径
     * @param fileSize     文件大小（字节）
     * @param fileType     MIME类型
     * @param fileExt      文件扩展名
     * @param fileCategory 文件分类（关联字典 sys_file_category）
     * @param uploadBy     上传人
     * @param uploadTime   上传时间
     * @param createBy     创建人
     * @param createTime   创建时间
     * @param updateBy     更新人
     * @param updateTime   更新时间
     * @param deleted      逻辑删除：0-未删除，1-已删除
     * @param remark       备注
     */
    @SuppressWarnings("all")
    public FileInfo(final Long id, final String fileName, final String filePath, final Long fileSize,
                    final String fileType, final String fileExt, final String fileCategory, final String uploadBy,
                    final LocalDateTime uploadTime, final String createBy, final LocalDateTime createTime,
                    final String updateBy, final LocalDateTime updateTime, final Integer deleted, final String remark) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.fileExt = fileExt;
        this.fileCategory = fileCategory;
        this.uploadBy = uploadBy;
        this.uploadTime = uploadTime;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static FileInfo.FileInfoBuilder builder() {
        return new FileInfo.FileInfoBuilder();
    }

    /**
     * 获取可读的文件大小
     */
    public String getReadableSize() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSize;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * 判断是否为图片文件
     */
    public boolean isImage() {
        return fileType != null && fileType.startsWith("image/");
    }

    /**
     * 文件ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 文件ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 原始文件名
     */
    @SuppressWarnings("all")
    public String getFileName() {
        return this.fileName;
    }

    /**
     * 原始文件名
     */
    @SuppressWarnings("all")
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * 存储路径
     */
    @SuppressWarnings("all")
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * 存储路径
     */
    @SuppressWarnings("all")
    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    /**
     * 文件大小（字节）
     */
    @SuppressWarnings("all")
    public Long getFileSize() {
        return this.fileSize;
    }

    /**
     * 文件大小（字节）
     */
    @SuppressWarnings("all")
    public void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * MIME类型
     */
    @SuppressWarnings("all")
    public String getFileType() {
        return this.fileType;
    }

    /**
     * MIME类型
     */
    @SuppressWarnings("all")
    public void setFileType(final String fileType) {
        this.fileType = fileType;
    }

    /**
     * 文件扩展名
     */
    @SuppressWarnings("all")
    public String getFileExt() {
        return this.fileExt;
    }

    /**
     * 文件扩展名
     */
    @SuppressWarnings("all")
    public void setFileExt(final String fileExt) {
        this.fileExt = fileExt;
    }

    /**
     * 文件分类（关联字典 sys_file_category）
     */
    @SuppressWarnings("all")
    public String getFileCategory() {
        return this.fileCategory;
    }

    /**
     * 文件分类（关联字典 sys_file_category）
     */
    @SuppressWarnings("all")
    public void setFileCategory(final String fileCategory) {
        this.fileCategory = fileCategory;
    }

    /**
     * 上传人
     */
    @SuppressWarnings("all")
    public String getUploadBy() {
        return this.uploadBy;
    }

    /**
     * 上传人
     */
    @SuppressWarnings("all")
    public void setUploadBy(final String uploadBy) {
        this.uploadBy = uploadBy;
    }

    /**
     * 上传时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getUploadTime() {
        return this.uploadTime;
    }

    /**
     * 上传时间
     */
    @SuppressWarnings("all")
    public void setUploadTime(final LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    /**
     * 创建人
     */
    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    /**
     * 创建人
     */
    @SuppressWarnings("all")
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    /**
     * 创建时间
     */
    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 更新人
     */
    @SuppressWarnings("all")
    public String getUpdateBy() {
        return this.updateBy;
    }

    /**
     * 更新人
     */
    @SuppressWarnings("all")
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    /**
     * 更新时间
     */
    @SuppressWarnings("all")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @SuppressWarnings("all")
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    /**
     * 备注
     */
    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FileInfo))
            return false;
        final FileInfo other = (FileInfo) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$fileSize = this.getFileSize();
        final java.lang.Object other$fileSize = other.getFileSize();
        if (this$fileSize == null ? other$fileSize != null : !this$fileSize.equals(other$fileSize))
            return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
            return false;
        final java.lang.Object this$fileName = this.getFileName();
        final java.lang.Object other$fileName = other.getFileName();
        if (this$fileName == null ? other$fileName != null : !this$fileName.equals(other$fileName))
            return false;
        final java.lang.Object this$filePath = this.getFilePath();
        final java.lang.Object other$filePath = other.getFilePath();
        if (this$filePath == null ? other$filePath != null : !this$filePath.equals(other$filePath))
            return false;
        final java.lang.Object this$fileType = this.getFileType();
        final java.lang.Object other$fileType = other.getFileType();
        if (this$fileType == null ? other$fileType != null : !this$fileType.equals(other$fileType))
            return false;
        final java.lang.Object this$fileExt = this.getFileExt();
        final java.lang.Object other$fileExt = other.getFileExt();
        if (this$fileExt == null ? other$fileExt != null : !this$fileExt.equals(other$fileExt))
            return false;
        final java.lang.Object this$fileCategory = this.getFileCategory();
        final java.lang.Object other$fileCategory = other.getFileCategory();
        if (this$fileCategory == null ? other$fileCategory != null : !this$fileCategory.equals(other$fileCategory))
            return false;
        final java.lang.Object this$uploadBy = this.getUploadBy();
        final java.lang.Object other$uploadBy = other.getUploadBy();
        if (this$uploadBy == null ? other$uploadBy != null : !this$uploadBy.equals(other$uploadBy))
            return false;
        final java.lang.Object this$uploadTime = this.getUploadTime();
        final java.lang.Object other$uploadTime = other.getUploadTime();
        if (this$uploadTime == null ? other$uploadTime != null : !this$uploadTime.equals(other$uploadTime))
            return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$updateBy = this.getUpdateBy();
        final java.lang.Object other$updateBy = other.getUpdateBy();
        if (this$updateBy == null ? other$updateBy != null : !this$updateBy.equals(other$updateBy))
            return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FileInfo;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $fileSize = this.getFileSize();
        result = result * PRIME + ($fileSize == null ? 43 : $fileSize.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $fileName = this.getFileName();
        result = result * PRIME + ($fileName == null ? 43 : $fileName.hashCode());
        final java.lang.Object $filePath = this.getFilePath();
        result = result * PRIME + ($filePath == null ? 43 : $filePath.hashCode());
        final java.lang.Object $fileType = this.getFileType();
        result = result * PRIME + ($fileType == null ? 43 : $fileType.hashCode());
        final java.lang.Object $fileExt = this.getFileExt();
        result = result * PRIME + ($fileExt == null ? 43 : $fileExt.hashCode());
        final java.lang.Object $fileCategory = this.getFileCategory();
        result = result * PRIME + ($fileCategory == null ? 43 : $fileCategory.hashCode());
        final java.lang.Object $uploadBy = this.getUploadBy();
        result = result * PRIME + ($uploadBy == null ? 43 : $uploadBy.hashCode());
        final java.lang.Object $uploadTime = this.getUploadTime();
        result = result * PRIME + ($uploadTime == null ? 43 : $uploadTime.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateBy = this.getUpdateBy();
        result = result * PRIME + ($updateBy == null ? 43 : $updateBy.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "FileInfo(id=" + this.getId() + ", fileName=" + this.getFileName() + ", filePath=" + this.getFilePath()
                + ", fileSize=" + this.getFileSize() + ", fileType=" + this.getFileType() + ", fileExt="
                + this.getFileExt() + ", fileCategory=" + this.getFileCategory() + ", uploadBy=" + this.getUploadBy()
                + ", uploadTime=" + this.getUploadTime() + ", createBy=" + this.getCreateBy() + ", createTime="
                + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime=" + this.getUpdateTime()
                + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class FileInfoBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String fileName;

        @SuppressWarnings("all")
        private String filePath;

        @SuppressWarnings("all")
        private Long fileSize;

        @SuppressWarnings("all")
        private String fileType;

        @SuppressWarnings("all")
        private String fileExt;

        @SuppressWarnings("all")
        private String fileCategory;

        @SuppressWarnings("all")
        private String uploadBy;

        @SuppressWarnings("all")
        private LocalDateTime uploadTime;

        @SuppressWarnings("all")
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String updateBy;

        @SuppressWarnings("all")
        private LocalDateTime updateTime;

        @SuppressWarnings("all")
        private Integer deleted;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        FileInfoBuilder() {
        }

        /**
         * 文件ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 原始文件名
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * 存储路径
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder filePath(final String filePath) {
            this.filePath = filePath;
            return this;
        }

        /**
         * 文件大小（字节）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder fileSize(final Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        /**
         * MIME类型
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder fileType(final String fileType) {
            this.fileType = fileType;
            return this;
        }

        /**
         * 文件扩展名
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder fileExt(final String fileExt) {
            this.fileExt = fileExt;
            return this;
        }

        /**
         * 文件分类（关联字典 sys_file_category）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder fileCategory(final String fileCategory) {
            this.fileCategory = fileCategory;
            return this;
        }

        /**
         * 上传人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder uploadBy(final String uploadBy) {
            this.uploadBy = uploadBy;
            return this;
        }

        /**
         * 上传时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder uploadTime(final LocalDateTime uploadTime) {
            this.uploadTime = uploadTime;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileInfo.FileInfoBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public FileInfo build() {
            return new FileInfo(this.id, this.fileName, this.filePath, this.fileSize, this.fileType, this.fileExt,
                    this.fileCategory, this.uploadBy, this.uploadTime, this.createBy, this.createTime, this.updateBy,
                    this.updateTime, this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "FileInfo.FileInfoBuilder(id=" + this.id + ", fileName=" + this.fileName + ", filePath="
                    + this.filePath + ", fileSize=" + this.fileSize + ", fileType=" + this.fileType + ", fileExt="
                    + this.fileExt + ", fileCategory=" + this.fileCategory + ", uploadBy=" + this.uploadBy
                    + ", uploadTime=" + this.uploadTime + ", createBy=" + this.createBy + ", createTime="
                    + this.createTime + ", updateBy=" + this.updateBy + ", updateTime=" + this.updateTime + ", deleted="
                    + this.deleted + ", remark=" + this.remark + ")";
        }

    }

}
