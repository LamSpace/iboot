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

package com.iboot.admin.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 消息模板响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "消息模板响应")
public class MessageTemplateResponse {

    @Schema(description = "模板ID")
    private Long id;

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板内容")
    private String templateContent;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;

    @SuppressWarnings("all")
    public MessageTemplateResponse() {
    }

    @SuppressWarnings("all")
    public MessageTemplateResponse(final Long id, final String templateCode, final String templateName,
                                   final String templateContent, final String messageType, final Integer status, final String createBy,
                                   final LocalDateTime createTime, final String remark) {
        this.id = id;
        this.templateCode = templateCode;
        this.templateName = templateName;
        this.templateContent = templateContent;
        this.messageType = messageType;
        this.status = status;
        this.createBy = createBy;
        this.createTime = createTime;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static MessageTemplateResponse.MessageTemplateResponseBuilder builder() {
        return new MessageTemplateResponse.MessageTemplateResponseBuilder();
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
    public String getTemplateCode() {
        return this.templateCode;
    }

    @SuppressWarnings("all")
    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    @SuppressWarnings("all")
    public String getTemplateName() {
        return this.templateName;
    }

    @SuppressWarnings("all")
    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    @SuppressWarnings("all")
    public String getTemplateContent() {
        return this.templateContent;
    }

    @SuppressWarnings("all")
    public void setTemplateContent(final String templateContent) {
        this.templateContent = templateContent;
    }

    @SuppressWarnings("all")
    public String getMessageType() {
        return this.messageType;
    }

    @SuppressWarnings("all")
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    @SuppressWarnings("all")
    public String getCreateBy() {
        return this.createBy;
    }

    @SuppressWarnings("all")
    public void setCreateBy(final String createBy) {
        this.createBy = createBy;
    }

    @SuppressWarnings("all")
    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    @SuppressWarnings("all")
    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
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
        if (!(o instanceof MessageTemplateResponse))
            return false;
        final MessageTemplateResponse other = (MessageTemplateResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$templateCode = this.getTemplateCode();
        final java.lang.Object other$templateCode = other.getTemplateCode();
        if (this$templateCode == null ? other$templateCode != null : !this$templateCode.equals(other$templateCode))
            return false;
        final java.lang.Object this$templateName = this.getTemplateName();
        final java.lang.Object other$templateName = other.getTemplateName();
        if (this$templateName == null ? other$templateName != null : !this$templateName.equals(other$templateName))
            return false;
        final java.lang.Object this$templateContent = this.getTemplateContent();
        final java.lang.Object other$templateContent = other.getTemplateContent();
        if (this$templateContent == null ? other$templateContent != null
                : !this$templateContent.equals(other$templateContent))
            return false;
        final java.lang.Object this$messageType = this.getMessageType();
        final java.lang.Object other$messageType = other.getMessageType();
        if (this$messageType == null ? other$messageType != null : !this$messageType.equals(other$messageType))
            return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MessageTemplateResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $templateCode = this.getTemplateCode();
        result = result * PRIME + ($templateCode == null ? 43 : $templateCode.hashCode());
        final java.lang.Object $templateName = this.getTemplateName();
        result = result * PRIME + ($templateName == null ? 43 : $templateName.hashCode());
        final java.lang.Object $templateContent = this.getTemplateContent();
        result = result * PRIME + ($templateContent == null ? 43 : $templateContent.hashCode());
        final java.lang.Object $messageType = this.getMessageType();
        result = result * PRIME + ($messageType == null ? 43 : $messageType.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MessageTemplateResponse(id=" + this.getId() + ", templateCode=" + this.getTemplateCode()
                + ", templateName=" + this.getTemplateName() + ", templateContent=" + this.getTemplateContent()
                + ", messageType=" + this.getMessageType() + ", status=" + this.getStatus() + ", createBy="
                + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class MessageTemplateResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String templateCode;

        @SuppressWarnings("all")
        private String templateName;

        @SuppressWarnings("all")
        private String templateContent;

        @SuppressWarnings("all")
        private String messageType;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String createBy;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        MessageTemplateResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder templateCode(final String templateCode) {
            this.templateCode = templateCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder templateName(final String templateName) {
            this.templateName = templateName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder templateContent(final String templateContent) {
            this.templateContent = templateContent;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder messageType(final String messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplateResponse.MessageTemplateResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public MessageTemplateResponse build() {
            return new MessageTemplateResponse(this.id, this.templateCode, this.templateName, this.templateContent,
                    this.messageType, this.status, this.createBy, this.createTime, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "MessageTemplateResponse.MessageTemplateResponseBuilder(id=" + this.id + ", templateCode="
                    + this.templateCode + ", templateName=" + this.templateName + ", templateContent="
                    + this.templateContent + ", messageType=" + this.messageType + ", status=" + this.status
                    + ", createBy=" + this.createBy + ", createTime=" + this.createTime + ", remark=" + this.remark
                    + ")";
        }

    }

}
