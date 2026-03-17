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
import java.util.Map;

/**
 * 消息模板实体
 *
 * @author iBoot
 */
public class MessageTemplate {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板内容（支持${变量名}占位符）
     */
    private String templateContent;

    /**
     * 消息类型（字典 sys_message_type）
     */
    private String messageType;

    /**
     * 状态：0-停用，1-正常
     */
    private Integer status;

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
    public MessageTemplate() {
    }

    /**
     * Creates a new {@code MessageTemplate} instance.
     *
     * @param id              模板ID
     * @param templateCode    模板编码
     * @param templateName    模板名称
     * @param templateContent 模板内容（支持${变量名}占位符）
     * @param messageType     消息类型（字典 sys_message_type）
     * @param status          状态：0-停用，1-正常
     * @param createBy        创建人
     * @param createTime      创建时间
     * @param updateBy        更新人
     * @param updateTime      更新时间
     * @param deleted         逻辑删除：0-未删除，1-已删除
     * @param remark          备注
     */
    @SuppressWarnings("all")
    public MessageTemplate(final Long id, final String templateCode, final String templateName,
                           final String templateContent, final String messageType, final Integer status, final String createBy,
                           final LocalDateTime createTime, final String updateBy, final LocalDateTime updateTime,
                           final Integer deleted, final String remark) {
        this.id = id;
        this.templateCode = templateCode;
        this.templateName = templateName;
        this.templateContent = templateContent;
        this.messageType = messageType;
        this.status = status;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static MessageTemplate.MessageTemplateBuilder builder() {
        return new MessageTemplate.MessageTemplateBuilder();
    }

    /**
     * 启用模板
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 停用模板
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return Integer.valueOf(1).equals(this.status);
    }

    /**
     * 填充模板内容，将 ${key} 替换为 params 中对应的值
     *
     * @param params 变量参数
     *
     * @return 填充后的内容
     */
    public String fillContent(Map<String, String> params) {
        if (this.templateContent == null || params == null || params.isEmpty()) {
            return this.templateContent;
        }
        String result = this.templateContent;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * 模板ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 模板ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 模板编码
     */
    @SuppressWarnings("all")
    public String getTemplateCode() {
        return this.templateCode;
    }

    /**
     * 模板编码
     */
    @SuppressWarnings("all")
    public void setTemplateCode(final String templateCode) {
        this.templateCode = templateCode;
    }

    /**
     * 模板名称
     */
    @SuppressWarnings("all")
    public String getTemplateName() {
        return this.templateName;
    }

    /**
     * 模板名称
     */
    @SuppressWarnings("all")
    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    /**
     * 模板内容（支持${变量名}占位符）
     */
    @SuppressWarnings("all")
    public String getTemplateContent() {
        return this.templateContent;
    }

    /**
     * 模板内容（支持${变量名}占位符）
     */
    @SuppressWarnings("all")
    public void setTemplateContent(final String templateContent) {
        this.templateContent = templateContent;
    }

    /**
     * 消息类型（字典 sys_message_type）
     */
    @SuppressWarnings("all")
    public String getMessageType() {
        return this.messageType;
    }

    /**
     * 消息类型（字典 sys_message_type）
     */
    @SuppressWarnings("all")
    public void setMessageType(final String messageType) {
        this.messageType = messageType;
    }

    /**
     * 状态：0-停用，1-正常
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 状态：0-停用，1-正常
     */
    @SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
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
        if (!(o instanceof MessageTemplate))
            return false;
        final MessageTemplate other = (MessageTemplate) o;
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
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
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
        return other instanceof MessageTemplate;
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
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
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
        return "MessageTemplate(id=" + this.getId() + ", templateCode=" + this.getTemplateCode() + ", templateName="
                + this.getTemplateName() + ", templateContent=" + this.getTemplateContent() + ", messageType="
                + this.getMessageType() + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy()
                + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime="
                + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class MessageTemplateBuilder {

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
        private String updateBy;

        @SuppressWarnings("all")
        private LocalDateTime updateTime;

        @SuppressWarnings("all")
        private Integer deleted;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        MessageTemplateBuilder() {
        }

        /**
         * 模板ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 模板编码
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder templateCode(final String templateCode) {
            this.templateCode = templateCode;
            return this;
        }

        /**
         * 模板名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder templateName(final String templateName) {
            this.templateName = templateName;
            return this;
        }

        /**
         * 模板内容（支持${变量名}占位符）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder templateContent(final String templateContent) {
            this.templateContent = templateContent;
            return this;
        }

        /**
         * 消息类型（字典 sys_message_type）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder messageType(final String messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * 状态：0-停用，1-正常
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MessageTemplate.MessageTemplateBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public MessageTemplate build() {
            return new MessageTemplate(this.id, this.templateCode, this.templateName, this.templateContent,
                    this.messageType, this.status, this.createBy, this.createTime, this.updateBy, this.updateTime,
                    this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "MessageTemplate.MessageTemplateBuilder(id=" + this.id + ", templateCode=" + this.templateCode
                    + ", templateName=" + this.templateName + ", templateContent=" + this.templateContent
                    + ", messageType=" + this.messageType + ", status=" + this.status + ", createBy=" + this.createBy
                    + ", createTime=" + this.createTime + ", updateBy=" + this.updateBy + ", updateTime="
                    + this.updateTime + ", deleted=" + this.deleted + ", remark=" + this.remark + ")";
        }

    }

}
