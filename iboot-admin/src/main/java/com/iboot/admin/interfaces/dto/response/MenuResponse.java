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
import java.util.List;

/**
 * 菜单响应 DTO
 *
 * @author iBoot
 */
@Schema(description = "菜单响应")
public class MenuResponse {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型：M-目录，C-菜单，F-按钮")
    private String menuType;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "显示顺序")
    private Integer orderNum;

    @Schema(description = "是否为外链：0-否，1-是")
    private Integer isFrame;

    @Schema(description = "是否缓存：0-否，1-是")
    private Integer isCache;

    @Schema(description = "是否可见：0-隐藏，1-显示")
    private Integer visible;

    @Schema(description = "状态：0-停用，1-正常")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子菜单列表")
    private List<MenuResponse> children;

    @SuppressWarnings("all")
    public MenuResponse() {
    }

    @SuppressWarnings("all")
    public MenuResponse(final Long id, final Long parentId, final String menuName, final String menuType,
                        final String path, final String component, final String perms, final String icon, final Integer orderNum,
                        final Integer isFrame, final Integer isCache, final Integer visible, final Integer status,
                        final String remark, final LocalDateTime createTime, final List<MenuResponse> children) {
        this.id = id;
        this.parentId = parentId;
        this.menuName = menuName;
        this.menuType = menuType;
        this.path = path;
        this.component = component;
        this.perms = perms;
        this.icon = icon;
        this.orderNum = orderNum;
        this.isFrame = isFrame;
        this.isCache = isCache;
        this.visible = visible;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
        this.children = children;
    }

    @SuppressWarnings("all")
    public static MenuResponse.MenuResponseBuilder builder() {
        return new MenuResponse.MenuResponseBuilder();
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
    public Long getParentId() {
        return this.parentId;
    }

    @SuppressWarnings("all")
    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    @SuppressWarnings("all")
    public String getMenuName() {
        return this.menuName;
    }

    @SuppressWarnings("all")
    public void setMenuName(final String menuName) {
        this.menuName = menuName;
    }

    @SuppressWarnings("all")
    public String getMenuType() {
        return this.menuType;
    }

    @SuppressWarnings("all")
    public void setMenuType(final String menuType) {
        this.menuType = menuType;
    }

    @SuppressWarnings("all")
    public String getPath() {
        return this.path;
    }

    @SuppressWarnings("all")
    public void setPath(final String path) {
        this.path = path;
    }

    @SuppressWarnings("all")
    public String getComponent() {
        return this.component;
    }

    @SuppressWarnings("all")
    public void setComponent(final String component) {
        this.component = component;
    }

    @SuppressWarnings("all")
    public String getPerms() {
        return this.perms;
    }

    @SuppressWarnings("all")
    public void setPerms(final String perms) {
        this.perms = perms;
    }

    @SuppressWarnings("all")
    public String getIcon() {
        return this.icon;
    }

    @SuppressWarnings("all")
    public void setIcon(final String icon) {
        this.icon = icon;
    }

    @SuppressWarnings("all")
    public Integer getOrderNum() {
        return this.orderNum;
    }

    @SuppressWarnings("all")
    public void setOrderNum(final Integer orderNum) {
        this.orderNum = orderNum;
    }

    @SuppressWarnings("all")
    public Integer getIsFrame() {
        return this.isFrame;
    }

    @SuppressWarnings("all")
    public void setIsFrame(final Integer isFrame) {
        this.isFrame = isFrame;
    }

    @SuppressWarnings("all")
    public Integer getIsCache() {
        return this.isCache;
    }

    @SuppressWarnings("all")
    public void setIsCache(final Integer isCache) {
        this.isCache = isCache;
    }

    @SuppressWarnings("all")
    public Integer getVisible() {
        return this.visible;
    }

    @SuppressWarnings("all")
    public void setVisible(final Integer visible) {
        this.visible = visible;
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

    @SuppressWarnings("all")
    public List<MenuResponse> getChildren() {
        return this.children;
    }

    @SuppressWarnings("all")
    public void setChildren(final List<MenuResponse> children) {
        this.children = children;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MenuResponse))
            return false;
        final MenuResponse other = (MenuResponse) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId))
            return false;
        final java.lang.Object this$orderNum = this.getOrderNum();
        final java.lang.Object other$orderNum = other.getOrderNum();
        if (this$orderNum == null ? other$orderNum != null : !this$orderNum.equals(other$orderNum))
            return false;
        final java.lang.Object this$isFrame = this.getIsFrame();
        final java.lang.Object other$isFrame = other.getIsFrame();
        if (this$isFrame == null ? other$isFrame != null : !this$isFrame.equals(other$isFrame))
            return false;
        final java.lang.Object this$isCache = this.getIsCache();
        final java.lang.Object other$isCache = other.getIsCache();
        if (this$isCache == null ? other$isCache != null : !this$isCache.equals(other$isCache))
            return false;
        final java.lang.Object this$visible = this.getVisible();
        final java.lang.Object other$visible = other.getVisible();
        if (this$visible == null ? other$visible != null : !this$visible.equals(other$visible))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$menuName = this.getMenuName();
        final java.lang.Object other$menuName = other.getMenuName();
        if (this$menuName == null ? other$menuName != null : !this$menuName.equals(other$menuName))
            return false;
        final java.lang.Object this$menuType = this.getMenuType();
        final java.lang.Object other$menuType = other.getMenuType();
        if (this$menuType == null ? other$menuType != null : !this$menuType.equals(other$menuType))
            return false;
        final java.lang.Object this$path = this.getPath();
        final java.lang.Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path))
            return false;
        final java.lang.Object this$component = this.getComponent();
        final java.lang.Object other$component = other.getComponent();
        if (this$component == null ? other$component != null : !this$component.equals(other$component))
            return false;
        final java.lang.Object this$perms = this.getPerms();
        final java.lang.Object other$perms = other.getPerms();
        if (this$perms == null ? other$perms != null : !this$perms.equals(other$perms))
            return false;
        final java.lang.Object this$icon = this.getIcon();
        final java.lang.Object other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon))
            return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark))
            return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MenuResponse;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $orderNum = this.getOrderNum();
        result = result * PRIME + ($orderNum == null ? 43 : $orderNum.hashCode());
        final java.lang.Object $isFrame = this.getIsFrame();
        result = result * PRIME + ($isFrame == null ? 43 : $isFrame.hashCode());
        final java.lang.Object $isCache = this.getIsCache();
        result = result * PRIME + ($isCache == null ? 43 : $isCache.hashCode());
        final java.lang.Object $visible = this.getVisible();
        result = result * PRIME + ($visible == null ? 43 : $visible.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $menuName = this.getMenuName();
        result = result * PRIME + ($menuName == null ? 43 : $menuName.hashCode());
        final java.lang.Object $menuType = this.getMenuType();
        result = result * PRIME + ($menuType == null ? 43 : $menuType.hashCode());
        final java.lang.Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final java.lang.Object $component = this.getComponent();
        result = result * PRIME + ($component == null ? 43 : $component.hashCode());
        final java.lang.Object $perms = this.getPerms();
        result = result * PRIME + ($perms == null ? 43 : $perms.hashCode());
        final java.lang.Object $icon = this.getIcon();
        result = result * PRIME + ($icon == null ? 43 : $icon.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MenuResponse(id=" + this.getId() + ", parentId=" + this.getParentId() + ", menuName="
                + this.getMenuName() + ", menuType=" + this.getMenuType() + ", path=" + this.getPath() + ", component="
                + this.getComponent() + ", perms=" + this.getPerms() + ", icon=" + this.getIcon() + ", orderNum="
                + this.getOrderNum() + ", isFrame=" + this.getIsFrame() + ", isCache=" + this.getIsCache()
                + ", visible=" + this.getVisible() + ", status=" + this.getStatus() + ", remark=" + this.getRemark()
                + ", createTime=" + this.getCreateTime() + ", children=" + this.getChildren() + ")";
    }

    @SuppressWarnings("all")
    public static class MenuResponseBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private Long parentId;

        @SuppressWarnings("all")
        private String menuName;

        @SuppressWarnings("all")
        private String menuType;

        @SuppressWarnings("all")
        private String path;

        @SuppressWarnings("all")
        private String component;

        @SuppressWarnings("all")
        private String perms;

        @SuppressWarnings("all")
        private String icon;

        @SuppressWarnings("all")
        private Integer orderNum;

        @SuppressWarnings("all")
        private Integer isFrame;

        @SuppressWarnings("all")
        private Integer isCache;

        @SuppressWarnings("all")
        private Integer visible;

        @SuppressWarnings("all")
        private Integer status;

        @SuppressWarnings("all")
        private String remark;

        @SuppressWarnings("all")
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        private List<MenuResponse> children;

        @SuppressWarnings("all")
        MenuResponseBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder parentId(final Long parentId) {
            this.parentId = parentId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder menuName(final String menuName) {
            this.menuName = menuName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder menuType(final String menuType) {
            this.menuType = menuType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder path(final String path) {
            this.path = path;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder component(final String component) {
            this.component = component;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder perms(final String perms) {
            this.perms = perms;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder icon(final String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder isFrame(final Integer isFrame) {
            this.isFrame = isFrame;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder isCache(final Integer isCache) {
            this.isCache = isCache;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder visible(final Integer visible) {
            this.visible = visible;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuResponse.MenuResponseBuilder children(final List<MenuResponse> children) {
            this.children = children;
            return this;
        }

        @SuppressWarnings("all")
        public MenuResponse build() {
            return new MenuResponse(this.id, this.parentId, this.menuName, this.menuType, this.path, this.component,
                    this.perms, this.icon, this.orderNum, this.isFrame, this.isCache, this.visible, this.status,
                    this.remark, this.createTime, this.children);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "MenuResponse.MenuResponseBuilder(id=" + this.id + ", parentId=" + this.parentId + ", menuName="
                    + this.menuName + ", menuType=" + this.menuType + ", path=" + this.path + ", component="
                    + this.component + ", perms=" + this.perms + ", icon=" + this.icon + ", orderNum=" + this.orderNum
                    + ", isFrame=" + this.isFrame + ", isCache=" + this.isCache + ", visible=" + this.visible
                    + ", status=" + this.status + ", remark=" + this.remark + ", createTime=" + this.createTime
                    + ", children=" + this.children + ")";
        }

    }

}
