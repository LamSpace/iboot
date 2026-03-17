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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 公告请求 DTO
 *
 * @author iBoot
 */
@Schema(description = "公告请求")
public class NoticeRequest {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "公告标题")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题长度不能超过200")
    private String noticeTitle;

    @Schema(description = "公告类型（字典 sys_notice_type）")
    @NotBlank(message = "公告类型不能为空")
    private String noticeType;

    @Schema(description = "公告内容")
    @NotBlank(message = "公告内容不能为空")
    private String noticeContent;

    @Schema(description = "是否置顶：0-否，1-是")
    private Integer isTop;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @SuppressWarnings("all")
    public NoticeRequest() {
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
    public String getNoticeTitle() {
        return this.noticeTitle;
    }

    @SuppressWarnings("all")
    public void setNoticeTitle(final String noticeTitle) {
        this.noticeTitle = noticeTitle;
    }

    @SuppressWarnings("all")
    public String getNoticeType() {
        return this.noticeType;
    }

    @SuppressWarnings("all")
    public void setNoticeType(final String noticeType) {
        this.noticeType = noticeType;
    }

    @SuppressWarnings("all")
    public String getNoticeContent() {
        return this.noticeContent;
    }

    @SuppressWarnings("all")
    public void setNoticeContent(final String noticeContent) {
        this.noticeContent = noticeContent;
    }

    @SuppressWarnings("all")
    public Integer getIsTop() {
        return this.isTop;
    }

    @SuppressWarnings("all")
    public void setIsTop(final Integer isTop) {
        this.isTop = isTop;
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
        if (!(o instanceof NoticeRequest))
            return false;
        final NoticeRequest other = (NoticeRequest) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$isTop = this.getIsTop();
        final java.lang.Object other$isTop = other.getIsTop();
        if (this$isTop == null ? other$isTop != null : !this$isTop.equals(other$isTop))
            return false;
        final java.lang.Object this$noticeTitle = this.getNoticeTitle();
        final java.lang.Object other$noticeTitle = other.getNoticeTitle();
        if (this$noticeTitle == null ? other$noticeTitle != null : !this$noticeTitle.equals(other$noticeTitle))
            return false;
        final java.lang.Object this$noticeType = this.getNoticeType();
        final java.lang.Object other$noticeType = other.getNoticeType();
        if (this$noticeType == null ? other$noticeType != null : !this$noticeType.equals(other$noticeType))
            return false;
        final java.lang.Object this$noticeContent = this.getNoticeContent();
        final java.lang.Object other$noticeContent = other.getNoticeContent();
        if (this$noticeContent == null ? other$noticeContent != null : !this$noticeContent.equals(other$noticeContent))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof NoticeRequest;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $isTop = this.getIsTop();
        result = result * PRIME + ($isTop == null ? 43 : $isTop.hashCode());
        final java.lang.Object $noticeTitle = this.getNoticeTitle();
        result = result * PRIME + ($noticeTitle == null ? 43 : $noticeTitle.hashCode());
        final java.lang.Object $noticeType = this.getNoticeType();
        result = result * PRIME + ($noticeType == null ? 43 : $noticeType.hashCode());
        final java.lang.Object $noticeContent = this.getNoticeContent();
        result = result * PRIME + ($noticeContent == null ? 43 : $noticeContent.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "NoticeRequest(id=" + this.getId() + ", noticeTitle=" + this.getNoticeTitle() + ", noticeType="
                + this.getNoticeType() + ", noticeContent=" + this.getNoticeContent() + ", isTop=" + this.getIsTop()
                + ", remark=" + this.getRemark() + ")";
    }

}
