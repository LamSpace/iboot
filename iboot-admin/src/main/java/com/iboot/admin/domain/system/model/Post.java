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
 * 岗位实体（聚合根）
 *
 * @author iBoot
 */
public class Post {

    /**
     * 岗位ID
     */
    private Long id;

    /**
     * 岗位编码
     */
    private String postCode;

    /**
     * 岗位名称
     */
    private String postName;

    /**
     * 岗位排序
     */
    private Integer postSort;

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
    public Post() {
    }

    /**
     * Creates a new {@code Post} instance.
     *
     * @param id         岗位ID
     * @param postCode   岗位编码
     * @param postName   岗位名称
     * @param postSort   岗位排序
     * @param status     状态：0-停用，1-正常
     * @param createBy   创建人
     * @param createTime 创建时间
     * @param updateBy   更新人
     * @param updateTime 更新时间
     * @param deleted    逻辑删除：0-未删除，1-已删除
     * @param remark     备注
     */
    @SuppressWarnings("all")
    public Post(final Long id, final String postCode, final String postName, final Integer postSort,
                final Integer status, final String createBy, final LocalDateTime createTime, final String updateBy,
                final LocalDateTime updateTime, final Integer deleted, final String remark) {
        this.id = id;
        this.postCode = postCode;
        this.postName = postName;
        this.postSort = postSort;
        this.status = status;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static Post.PostBuilder builder() {
        return new Post.PostBuilder();
    }

    /**
     * 启用岗位
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 停用岗位
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 检查岗位是否启用
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    /**
     * 岗位ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 岗位ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 岗位编码
     */
    @SuppressWarnings("all")
    public String getPostCode() {
        return this.postCode;
    }

    /**
     * 岗位编码
     */
    @SuppressWarnings("all")
    public void setPostCode(final String postCode) {
        this.postCode = postCode;
    }

    /**
     * 岗位名称
     */
    @SuppressWarnings("all")
    public String getPostName() {
        return this.postName;
    }

    /**
     * 岗位名称
     */
    @SuppressWarnings("all")
    public void setPostName(final String postName) {
        this.postName = postName;
    }

    /**
     * 岗位排序
     */
    @SuppressWarnings("all")
    public Integer getPostSort() {
        return this.postSort;
    }

    /**
     * 岗位排序
     */
    @SuppressWarnings("all")
    public void setPostSort(final Integer postSort) {
        this.postSort = postSort;
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
        if (!(o instanceof Post))
            return false;
        final Post other = (Post) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$postSort = this.getPostSort();
        final java.lang.Object other$postSort = other.getPostSort();
        if (this$postSort == null ? other$postSort != null : !this$postSort.equals(other$postSort))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
            return false;
        final java.lang.Object this$postCode = this.getPostCode();
        final java.lang.Object other$postCode = other.getPostCode();
        if (this$postCode == null ? other$postCode != null : !this$postCode.equals(other$postCode))
            return false;
        final java.lang.Object this$postName = this.getPostName();
        final java.lang.Object other$postName = other.getPostName();
        if (this$postName == null ? other$postName != null : !this$postName.equals(other$postName))
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
        return other instanceof Post;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $postSort = this.getPostSort();
        result = result * PRIME + ($postSort == null ? 43 : $postSort.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $postCode = this.getPostCode();
        result = result * PRIME + ($postCode == null ? 43 : $postCode.hashCode());
        final java.lang.Object $postName = this.getPostName();
        result = result * PRIME + ($postName == null ? 43 : $postName.hashCode());
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
        return "Post(id=" + this.getId() + ", postCode=" + this.getPostCode() + ", postName=" + this.getPostName()
                + ", postSort=" + this.getPostSort() + ", status=" + this.getStatus() + ", createBy="
                + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy()
                + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", remark="
                + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class PostBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String postCode;

        @SuppressWarnings("all")
        private String postName;

        @SuppressWarnings("all")
        private Integer postSort;

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
        PostBuilder() {
        }

        /**
         * 岗位ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 岗位编码
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder postCode(final String postCode) {
            this.postCode = postCode;
            return this;
        }

        /**
         * 岗位名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder postName(final String postName) {
            this.postName = postName;
            return this;
        }

        /**
         * 岗位排序
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder postSort(final Integer postSort) {
            this.postSort = postSort;
            return this;
        }

        /**
         * 状态：0-停用，1-正常
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Post.PostBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public Post build() {
            return new Post(this.id, this.postCode, this.postName, this.postSort, this.status, this.createBy,
                    this.createTime, this.updateBy, this.updateTime, this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "Post.PostBuilder(id=" + this.id + ", postCode=" + this.postCode + ", postName=" + this.postName
                    + ", postSort=" + this.postSort + ", status=" + this.status + ", createBy=" + this.createBy
                    + ", createTime=" + this.createTime + ", updateBy=" + this.updateBy + ", updateTime="
                    + this.updateTime + ", deleted=" + this.deleted + ", remark=" + this.remark + ")";
        }

    }

}
