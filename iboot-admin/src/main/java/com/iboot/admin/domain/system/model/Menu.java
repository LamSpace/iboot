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
import java.util.List;

/**
 * 菜单实体（聚合根）
 *
 * @author iBoot
 */
public class Menu {

    private Long id;

    private String menuName;

    private Long parentId;

    private String menuType;

    private String path;

    private String component;

    private String permission;

    private String icon;

    private Integer orderNum;

    private Integer isFrame;

    private Integer isCache;

    private Integer visible;

    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private Integer deleted;

    private String remark;

    /**
     * 子菜单列表（非持久化字段，用于树形结构构建）
     */
    private List<Menu> children;

    @SuppressWarnings("all")
    public Menu() {
        this.children = Menu.$default$children();
    }

    /**
     * Creates a new {@code Menu} instance.
     *
     * @param id
     * @param menuName
     * @param parentId
     * @param menuType
     * @param path
     * @param component
     * @param permission
     * @param icon
     * @param orderNum
     * @param isFrame
     * @param isCache
     * @param visible
     * @param status
     * @param createBy
     * @param createTime
     * @param updateBy
     * @param updateTime
     * @param deleted
     * @param remark
     * @param children   子菜单列表（非持久化字段，用于树形结构构建）
     */
    @SuppressWarnings("all")
    public Menu(final Long id, final String menuName, final Long parentId, final String menuType, final String path,
                final String component, final String permission, final String icon, final Integer orderNum,
                final Integer isFrame, final Integer isCache, final Integer visible, final Integer status,
                final String createBy, final LocalDateTime createTime, final String updateBy,
                final LocalDateTime updateTime, final Integer deleted, final String remark, final List<Menu> children) {
        this.id = id;
        this.menuName = menuName;
        this.parentId = parentId;
        this.menuType = menuType;
        this.path = path;
        this.component = component;
        this.permission = permission;
        this.icon = icon;
        this.orderNum = orderNum;
        this.isFrame = isFrame;
        this.isCache = isCache;
        this.visible = visible;
        this.status = status;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
        this.children = children;
    }

    @SuppressWarnings("all")
    private static List<Menu> $default$children() {
        return null;
    }

    @SuppressWarnings("all")
    public static Menu.MenuBuilder builder() {
        return new Menu.MenuBuilder();
    }

    /**
     * 检查是否为目录类型菜单
     *
     * @return 如果是目录类型则返回true，否则返回false
     */
    public boolean isDirectory() {
        return "M".equals(this.menuType);
    }

    /**
     * 检查是否为菜单类型
     *
     * @return 如果是菜单类型则返回true，否则返回false
     */
    public boolean isMenu() {
        return "C".equals(this.menuType);
    }

    /**
     * 检查是否为按钮类型
     *
     * @return 如果是按钮类型则返回true，否则返回false
     */
    public boolean isButton() {
        return "F".equals(this.menuType);
    }

    /**
     * 检查菜单是否可见
     *
     * @return 如果菜单可见则返回true，否则返回false
     */
    public boolean isVisible() {
        return this.visible != null && this.visible == 1;
    }

    /**
     * 启用菜单
     *
     * @return 启用后的菜单对象
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 停用菜单
     *
     * @return 停用后的菜单对象
     */
    public void disable() {
        this.status = 0;
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
    public String getMenuName() {
        return this.menuName;
    }

    @SuppressWarnings("all")
    public void setMenuName(final String menuName) {
        this.menuName = menuName;
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
    public String getPermission() {
        return this.permission;
    }

    @SuppressWarnings("all")
    public void setPermission(final String permission) {
        this.permission = permission;
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
    public String getUpdateBy() {
        return this.updateBy;
    }

    @SuppressWarnings("all")
    public void setUpdateBy(final String updateBy) {
        this.updateBy = updateBy;
    }

    @SuppressWarnings("all")
    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    @SuppressWarnings("all")
    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @SuppressWarnings("all")
    public Integer getDeleted() {
        return this.deleted;
    }

    @SuppressWarnings("all")
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @SuppressWarnings("all")
    public String getRemark() {
        return this.remark;
    }

    @SuppressWarnings("all")
    public void setRemark(final String remark) {
        this.remark = remark;
    }

    /**
     * 子菜单列表（非持久化字段，用于树形结构构建）
     */
    @SuppressWarnings("all")
    public List<Menu> getChildren() {
        return this.children;
    }

    /**
     * 子菜单列表（非持久化字段，用于树形结构构建）
     */
    @SuppressWarnings("all")
    public void setChildren(final List<Menu> children) {
        this.children = children;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Menu))
            return false;
        final Menu other = (Menu) o;
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
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
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
        final java.lang.Object this$permission = this.getPermission();
        final java.lang.Object other$permission = other.getPermission();
        if (this$permission == null ? other$permission != null : !this$permission.equals(other$permission))
            return false;
        final java.lang.Object this$icon = this.getIcon();
        final java.lang.Object other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon))
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
        final java.lang.Object this$children = this.getChildren();
        final java.lang.Object other$children = other.getChildren();
        if (this$children == null ? other$children != null : !this$children.equals(other$children))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Menu;
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
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $menuName = this.getMenuName();
        result = result * PRIME + ($menuName == null ? 43 : $menuName.hashCode());
        final java.lang.Object $menuType = this.getMenuType();
        result = result * PRIME + ($menuType == null ? 43 : $menuType.hashCode());
        final java.lang.Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final java.lang.Object $component = this.getComponent();
        result = result * PRIME + ($component == null ? 43 : $component.hashCode());
        final java.lang.Object $permission = this.getPermission();
        result = result * PRIME + ($permission == null ? 43 : $permission.hashCode());
        final java.lang.Object $icon = this.getIcon();
        result = result * PRIME + ($icon == null ? 43 : $icon.hashCode());
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
        final java.lang.Object $children = this.getChildren();
        result = result * PRIME + ($children == null ? 43 : $children.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "Menu(id=" + this.getId() + ", menuName=" + this.getMenuName() + ", parentId=" + this.getParentId()
                + ", menuType=" + this.getMenuType() + ", path=" + this.getPath() + ", component=" + this.getComponent()
                + ", permission=" + this.getPermission() + ", icon=" + this.getIcon() + ", orderNum="
                + this.getOrderNum() + ", isFrame=" + this.getIsFrame() + ", isCache=" + this.getIsCache()
                + ", visible=" + this.getVisible() + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy()
                + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime="
                + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark()
                + ", children=" + this.getChildren() + ")";
    }

    @SuppressWarnings("all")
    public static class MenuBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String menuName;

        @SuppressWarnings("all")
        private Long parentId;

        @SuppressWarnings("all")
        private String menuType;

        @SuppressWarnings("all")
        private String path;

        @SuppressWarnings("all")
        private String component;

        @SuppressWarnings("all")
        private String permission;

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
        private boolean children$set;

        @SuppressWarnings("all")
        private List<Menu> children$value;

        @SuppressWarnings("all")
        MenuBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder menuName(final String menuName) {
            this.menuName = menuName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder parentId(final Long parentId) {
            this.parentId = parentId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder menuType(final String menuType) {
            this.menuType = menuType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder path(final String path) {
            this.path = path;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder component(final String component) {
            this.component = component;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder permission(final String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder icon(final String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder isFrame(final Integer isFrame) {
            this.isFrame = isFrame;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder isCache(final Integer isCache) {
            this.isCache = isCache;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder visible(final Integer visible) {
            this.visible = visible;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * 子菜单列表（非持久化字段，用于树形结构构建）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Menu.MenuBuilder children(final List<Menu> children) {
            this.children$value = children;
            children$set = true;
            return this;
        }

        @SuppressWarnings("all")
        public Menu build() {
            List<Menu> children$value = this.children$value;
            if (!this.children$set)
                children$value = Menu.$default$children();
            return new Menu(this.id, this.menuName, this.parentId, this.menuType, this.path, this.component,
                    this.permission, this.icon, this.orderNum, this.isFrame, this.isCache, this.visible, this.status,
                    this.createBy, this.createTime, this.updateBy, this.updateTime, this.deleted, this.remark,
                    children$value);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "Menu.MenuBuilder(id=" + this.id + ", menuName=" + this.menuName + ", parentId=" + this.parentId
                    + ", menuType=" + this.menuType + ", path=" + this.path + ", component=" + this.component
                    + ", permission=" + this.permission + ", icon=" + this.icon + ", orderNum=" + this.orderNum
                    + ", isFrame=" + this.isFrame + ", isCache=" + this.isCache + ", visible=" + this.visible
                    + ", status=" + this.status + ", createBy=" + this.createBy + ", createTime=" + this.createTime
                    + ", updateBy=" + this.updateBy + ", updateTime=" + this.updateTime + ", deleted=" + this.deleted
                    + ", remark=" + this.remark + ", children$value=" + this.children$value + ")";
        }

    }

}
