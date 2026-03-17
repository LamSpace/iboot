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

package com.iboot.admin.infrastructure.persistence.po;

import java.time.LocalDateTime;

/**
 * 文件信息持久化对象
 * <p>
 * 对应数据库表：sys_file
 * </p>
 *
 * @author iBoot
 */
public class FilePO {

    /**
     * 文件 ID
     */
    private Long id;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件 MIME 类型
     */
    private String fileType;

    /**
     * 文件扩展名
     */
    private String fileExt;

    /**
     * 文件分类
     */
    private String fileCategory;

    /**
     * 所属部门 ID
     */
    private Long deptId;

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
    public FilePO() {
    }

    /**
     * 文件 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 文件 ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 文件名称
     */
    @SuppressWarnings("all")
    public String getFileName() {
        return this.fileName;
    }

    /**
     * 文件名称
     */
    @SuppressWarnings("all")
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * 文件路径
     */
    @SuppressWarnings("all")
    public String getFilePath() {
        return this.filePath;
    }

    /**
     * 文件路径
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
     * 文件 MIME 类型
     */
    @SuppressWarnings("all")
    public String getFileType() {
        return this.fileType;
    }

    /**
     * 文件 MIME 类型
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
     * 文件分类
     */
    @SuppressWarnings("all")
    public String getFileCategory() {
        return this.fileCategory;
    }

    /**
     * 文件分类
     */
    @SuppressWarnings("all")
    public void setFileCategory(final String fileCategory) {
        this.fileCategory = fileCategory;
    }

    /**
     * 所属部门 ID
     */
    @SuppressWarnings("all")
    public Long getDeptId() {
        return this.deptId;
    }

    /**
     * 所属部门 ID
     */
    @SuppressWarnings("all")
    public void setDeptId(final Long deptId) {
        this.deptId = deptId;
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
        if (!(o instanceof FilePO))
            return false;
        final FilePO other = (FilePO) o;
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
        final java.lang.Object this$deptId = this.getDeptId();
        final java.lang.Object other$deptId = other.getDeptId();
        if (this$deptId == null ? other$deptId != null : !this$deptId.equals(other$deptId))
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
        return other instanceof FilePO;
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
        final java.lang.Object $deptId = this.getDeptId();
        result = result * PRIME + ($deptId == null ? 43 : $deptId.hashCode());
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
        return "FilePO(id=" + this.getId() + ", fileName=" + this.getFileName() + ", filePath=" + this.getFilePath()
                + ", fileSize=" + this.getFileSize() + ", fileType=" + this.getFileType() + ", fileExt="
                + this.getFileExt() + ", fileCategory=" + this.getFileCategory() + ", deptId=" + this.getDeptId()
                + ", uploadBy=" + this.getUploadBy() + ", uploadTime=" + this.getUploadTime() + ", createBy="
                + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy()
                + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", remark="
                + this.getRemark() + ")";
    }

}
