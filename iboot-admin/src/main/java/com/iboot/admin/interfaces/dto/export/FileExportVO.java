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

package com.iboot.admin.interfaces.dto.export;

import com.iboot.admin.common.annotation.ExcelColumn;

import java.time.LocalDateTime;

/**
 * 文件导出VO
 *
 * @author iBoot
 */
public class FileExportVO {

    @ExcelColumn(name = "文件ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "文件名称", order = 2, width = 35)
    private String fileName;

    @ExcelColumn(name = "文件大小", order = 3, width = 15)
    private String readableSize;

    @ExcelColumn(name = "文件类型", order = 4, width = 15)
    private String fileType;

    @ExcelColumn(name = "扩展名", order = 5, width = 10)
    private String fileExt;

    @ExcelColumn(name = "文件分类", order = 6, width = 12, dictType = "sys_file_category")
    private String fileCategory;

    @ExcelColumn(name = "上传人", order = 7, width = 15)
    private String uploadBy;

    @ExcelColumn(name = "上传时间", order = 8, width = 20)
    private LocalDateTime uploadTime;

    @ExcelColumn(name = "备注", order = 9, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 10, width = 20)
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public FileExportVO() {
    }

    @SuppressWarnings("all")
    public FileExportVO(final Long id, final String fileName, final String readableSize, final String fileType,
                        final String fileExt, final String fileCategory, final String uploadBy, final LocalDateTime uploadTime,
                        final String remark, final LocalDateTime createTime) {
        this.id = id;
        this.fileName = fileName;
        this.readableSize = readableSize;
        this.fileType = fileType;
        this.fileExt = fileExt;
        this.fileCategory = fileCategory;
        this.uploadBy = uploadBy;
        this.uploadTime = uploadTime;
        this.remark = remark;
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static FileExportVO.FileExportVOBuilder builder() {
        return new FileExportVO.FileExportVOBuilder();
    }

    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @SuppressWarnings("all")
    public String getFileName() {
        return this.fileName;
    }

    @SuppressWarnings("all")
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    @SuppressWarnings("all")
    public String getReadableSize() {
        return this.readableSize;
    }

    @SuppressWarnings("all")
    public void setReadableSize(final String readableSize) {
        this.readableSize = readableSize;
    }

    @SuppressWarnings("all")
    public String getFileType() {
        return this.fileType;
    }

    @SuppressWarnings("all")
    public void setFileType(final String fileType) {
        this.fileType = fileType;
    }

    @SuppressWarnings("all")
    public String getFileExt() {
        return this.fileExt;
    }

    @SuppressWarnings("all")
    public void setFileExt(final String fileExt) {
        this.fileExt = fileExt;
    }

    @SuppressWarnings("all")
    public String getFileCategory() {
        return this.fileCategory;
    }

    @SuppressWarnings("all")
    public void setFileCategory(final String fileCategory) {
        this.fileCategory = fileCategory;
    }

    @SuppressWarnings("all")
    public String getUploadBy() {
        return this.uploadBy;
    }

    @SuppressWarnings("all")
    public void setUploadBy(final String uploadBy) {
        this.uploadBy = uploadBy;
    }

    @SuppressWarnings("all")
    public LocalDateTime getUploadTime() {
        return this.uploadTime;
    }

    @SuppressWarnings("all")
    public void setUploadTime(final LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FileExportVO))
            return false;
        final FileExportVO other = (FileExportVO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$fileName = this.getFileName();
        final java.lang.Object other$fileName = other.getFileName();
        if (this$fileName == null ? other$fileName != null : !this$fileName.equals(other$fileName))
            return false;
        final java.lang.Object this$readableSize = this.getReadableSize();
        final java.lang.Object other$readableSize = other.getReadableSize();
        if (this$readableSize == null ? other$readableSize != null : !this$readableSize.equals(other$readableSize))
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
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FileExportVO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $fileName = this.getFileName();
        result = result * PRIME + ($fileName == null ? 43 : $fileName.hashCode());
        final java.lang.Object $readableSize = this.getReadableSize();
        result = result * PRIME + ($readableSize == null ? 43 : $readableSize.hashCode());
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
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "FileExportVO(id=" + this.getId() + ", fileName=" + this.getFileName() + ", readableSize="
                + this.getReadableSize() + ", fileType=" + this.getFileType() + ", fileExt=" + this.getFileExt()
                + ", fileCategory=" + this.getFileCategory() + ", uploadBy=" + this.getUploadBy() + ", uploadTime="
                + this.getUploadTime() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class FileExportVOBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String fileName;

        @SuppressWarnings("all")
        private String readableSize;

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
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        FileExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder fileName(final String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder readableSize(final String readableSize) {
            this.readableSize = readableSize;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder fileType(final String fileType) {
            this.fileType = fileType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder fileExt(final String fileExt) {
            this.fileExt = fileExt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder fileCategory(final String fileCategory) {
            this.fileCategory = fileCategory;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder uploadBy(final String uploadBy) {
            this.uploadBy = uploadBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder uploadTime(final LocalDateTime uploadTime) {
            this.uploadTime = uploadTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileExportVO.FileExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public FileExportVO build() {
            return new FileExportVO(this.id, this.fileName, this.readableSize, this.fileType, this.fileExt,
                    this.fileCategory, this.uploadBy, this.uploadTime, this.remark, this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "FileExportVO.FileExportVOBuilder(id=" + this.id + ", fileName=" + this.fileName + ", readableSize="
                    + this.readableSize + ", fileType=" + this.fileType + ", fileExt=" + this.fileExt
                    + ", fileCategory=" + this.fileCategory + ", uploadBy=" + this.uploadBy + ", uploadTime="
                    + this.uploadTime + ", remark=" + this.remark + ", createTime=" + this.createTime + ")";
        }

    }

}
