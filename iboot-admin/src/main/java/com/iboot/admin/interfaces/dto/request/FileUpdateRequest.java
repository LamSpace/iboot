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

package com.iboot.admin.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 文件信息更新请求 DTO
 *
 * @author iBoot
 */
@Schema(description = "文件信息更新请求")
public class FileUpdateRequest {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "文件名称")
    @Size(max = 255, message = "文件名称长度不能超过255")
    private String fileName;

    @Schema(description = "文件分类")
    @Size(max = 100, message = "文件分类长度不能超过100")
    private String fileCategory;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @SuppressWarnings("all")
    public FileUpdateRequest() {
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
    public String getFileCategory() {
        return this.fileCategory;
    }

    @SuppressWarnings("all")
    public void setFileCategory(final String fileCategory) {
        this.fileCategory = fileCategory;
    }

    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FileUpdateRequest))
            return false;
        final FileUpdateRequest other = (FileUpdateRequest) o;
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
        final java.lang.Object this$fileCategory = this.getFileCategory();
        final java.lang.Object other$fileCategory = other.getFileCategory();
        if (this$fileCategory == null ? other$fileCategory != null : !this$fileCategory.equals(other$fileCategory))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FileUpdateRequest;
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
        final java.lang.Object $fileCategory = this.getFileCategory();
        result = result * PRIME + ($fileCategory == null ? 43 : $fileCategory.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "FileUpdateRequest(id=" + this.getId() + ", fileName=" + this.getFileName() + ", fileCategory="
                + this.getFileCategory() + ", remark=" + this.getRemark() + ")";
    }

}
