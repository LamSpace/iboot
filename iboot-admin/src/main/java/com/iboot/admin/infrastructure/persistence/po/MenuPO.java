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
 * 菜单持久化对象
 * <p>
 * 对应数据库表：sys_menu
 * </p>
 *
 * @author iBoot
 */
public class MenuPO {

    /**
     * 菜单 ID
     */
    private Long id;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 父菜单 ID
     */
    private Long parentId;

    /**
     * 菜单类型：M-目录，C-菜单，F-按钮
     */
    private String menuType;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String permission;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 是否外链：0-否，1-是
     */
    private Integer isFrame;

    /**
     * 是否缓存：0-不缓存，1-缓存
     */
    private Integer isCache;

    /**
     * 显示状态：0-隐藏，1-显示
     */
    private Integer visible;

    /**
     * 菜单状态：0-停用，1-正常
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
    public MenuPO() {
    }

    /**
     * Creates a new {@code MenuPO} instance.
     *
     * @param id         菜单 ID
     * @param menuName   菜单名称
     * @param parentId   父菜单 ID
     * @param menuType   菜单类型：M-目录，C-菜单，F-按钮
     * @param path       路由地址
     * @param component  组件路径
     * @param permission 权限标识
     * @param icon       菜单图标
     * @param orderNum   显示顺序
     * @param isFrame    是否外链：0-否，1-是
     * @param isCache    是否缓存：0-不缓存，1-缓存
     * @param visible    显示状态：0-隐藏，1-显示
     * @param status     菜单状态：0-停用，1-正常
     * @param createBy   创建人
     * @param createTime 创建时间
     * @param updateBy   更新人
     * @param updateTime 更新时间
     * @param deleted    逻辑删除：0-未删除，1-已删除
     * @param remark     备注
     */
    @SuppressWarnings("all")
    public MenuPO(final Long id, final String menuName, final Long parentId, final String menuType, final String path,
                  final String component, final String permission, final String icon, final Integer orderNum,
                  final Integer isFrame, final Integer isCache, final Integer visible, final Integer status,
                  final String createBy, final LocalDateTime createTime, final String updateBy,
                  final LocalDateTime updateTime, final Integer deleted, final String remark) {
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
    }

    @SuppressWarnings("all")
    public static MenuPO.MenuPOBuilder builder() {
        return new MenuPO.MenuPOBuilder();
    }

    /**
     * 菜单 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 菜单 ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 菜单名称
     */
    @SuppressWarnings("all")
    public String getMenuName() {
        return this.menuName;
    }

    /**
     * 菜单名称
     */
    @SuppressWarnings("all")
    public void setMenuName(final String menuName) {
        this.menuName = menuName;
    }

    /**
     * 父菜单 ID
     */
    @SuppressWarnings("all")
    public Long getParentId() {
        return this.parentId;
    }

    /**
     * 父菜单 ID
     */
    @SuppressWarnings("all")
    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 菜单类型：M-目录，C-菜单，F-按钮
     */
    @SuppressWarnings("all")
    public String getMenuType() {
        return this.menuType;
    }

    /**
     * 菜单类型：M-目录，C-菜单，F-按钮
     */
    @SuppressWarnings("all")
    public void setMenuType(final String menuType) {
        this.menuType = menuType;
    }

    /**
     * 路由地址
     */
    @SuppressWarnings("all")
    public String getPath() {
        return this.path;
    }

    /**
     * 路由地址
     */
    @SuppressWarnings("all")
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * 组件路径
     */
    @SuppressWarnings("all")
    public String getComponent() {
        return this.component;
    }

    /**
     * 组件路径
     */
    @SuppressWarnings("all")
    public void setComponent(final String component) {
        this.component = component;
    }

    /**
     * 权限标识
     */
    @SuppressWarnings("all")
    public String getPermission() {
        return this.permission;
    }

    /**
     * 权限标识
     */
    @SuppressWarnings("all")
    public void setPermission(final String permission) {
        this.permission = permission;
    }

    /**
     * 菜单图标
     */
    @SuppressWarnings("all")
    public String getIcon() {
        return this.icon;
    }

    /**
     * 菜单图标
     */
    @SuppressWarnings("all")
    public void setIcon(final String icon) {
        this.icon = icon;
    }

    /**
     * 显示顺序
     */
    @SuppressWarnings("all")
    public Integer getOrderNum() {
        return this.orderNum;
    }

    /**
     * 显示顺序
     */
    @SuppressWarnings("all")
    public void setOrderNum(final Integer orderNum) {
        this.orderNum = orderNum;
    }

    /**
     * 是否外链：0-否，1-是
     */
    @SuppressWarnings("all")
    public Integer getIsFrame() {
        return this.isFrame;
    }

    /**
     * 是否外链：0-否，1-是
     */
    @SuppressWarnings("all")
    public void setIsFrame(final Integer isFrame) {
        this.isFrame = isFrame;
    }

    /**
     * 是否缓存：0-不缓存，1-缓存
     */
    @SuppressWarnings("all")
    public Integer getIsCache() {
        return this.isCache;
    }

    /**
     * 是否缓存：0-不缓存，1-缓存
     */
    @SuppressWarnings("all")
    public void setIsCache(final Integer isCache) {
        this.isCache = isCache;
    }

    /**
     * 显示状态：0-隐藏，1-显示
     */
    @SuppressWarnings("all")
    public Integer getVisible() {
        return this.visible;
    }

    /**
     * 显示状态：0-隐藏，1-显示
     */
    @SuppressWarnings("all")
    public void setVisible(final Integer visible) {
        this.visible = visible;
    }

    /**
     * 菜单状态：0-停用，1-正常
     */
    @SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    /**
     * 菜单状态：0-停用，1-正常
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
        if (!(o instanceof MenuPO))
            return false;
        final MenuPO other = (MenuPO) o;
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
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MenuPO;
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
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MenuPO(id=" + this.getId() + ", menuName=" + this.getMenuName() + ", parentId=" + this.getParentId()
                + ", menuType=" + this.getMenuType() + ", path=" + this.getPath() + ", component=" + this.getComponent()
                + ", permission=" + this.getPermission() + ", icon=" + this.getIcon() + ", orderNum="
                + this.getOrderNum() + ", isFrame=" + this.getIsFrame() + ", isCache=" + this.getIsCache()
                + ", visible=" + this.getVisible() + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy()
                + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy() + ", updateTime="
                + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", remark=" + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class MenuPOBuilder {

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
        MenuPOBuilder() {
        }

        /**
         * 菜单 ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 菜单名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder menuName(final String menuName) {
            this.menuName = menuName;
            return this;
        }

        /**
         * 父菜单 ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder parentId(final Long parentId) {
            this.parentId = parentId;
            return this;
        }

        /**
         * 菜单类型：M-目录，C-菜单，F-按钮
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder menuType(final String menuType) {
            this.menuType = menuType;
            return this;
        }

        /**
         * 路由地址
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder path(final String path) {
            this.path = path;
            return this;
        }

        /**
         * 组件路径
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder component(final String component) {
            this.component = component;
            return this;
        }

        /**
         * 权限标识
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder permission(final String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * 菜单图标
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder icon(final String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * 显示顺序
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * 是否外链：0-否，1-是
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder isFrame(final Integer isFrame) {
            this.isFrame = isFrame;
            return this;
        }

        /**
         * 是否缓存：0-不缓存，1-缓存
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder isCache(final Integer isCache) {
            this.isCache = isCache;
            return this;
        }

        /**
         * 显示状态：0-隐藏，1-显示
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder visible(final Integer visible) {
            this.visible = visible;
            return this;
        }

        /**
         * 菜单状态：0-停用，1-正常
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuPO.MenuPOBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public MenuPO build() {
            return new MenuPO(this.id, this.menuName, this.parentId, this.menuType, this.path, this.component,
                    this.permission, this.icon, this.orderNum, this.isFrame, this.isCache, this.visible, this.status,
                    this.createBy, this.createTime, this.updateBy, this.updateTime, this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "MenuPO.MenuPOBuilder(id=" + this.id + ", menuName=" + this.menuName + ", parentId=" + this.parentId
                    + ", menuType=" + this.menuType + ", path=" + this.path + ", component=" + this.component
                    + ", permission=" + this.permission + ", icon=" + this.icon + ", orderNum=" + this.orderNum
                    + ", isFrame=" + this.isFrame + ", isCache=" + this.isCache + ", visible=" + this.visible
                    + ", status=" + this.status + ", createBy=" + this.createBy + ", createTime=" + this.createTime
                    + ", updateBy=" + this.updateBy + ", updateTime=" + this.updateTime + ", deleted=" + this.deleted
                    + ", remark=" + this.remark + ")";
        }

    }

}
