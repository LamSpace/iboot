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
 * 菜单导出VO
 *
 * @author iBoot
 */
public class MenuExportVO {

    @ExcelColumn(name = "菜单ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "层级", order = 2, width = 8)
    private Integer level;

    @ExcelColumn(name = "菜单名称", order = 3, width = 20)
    private String menuName;

    @ExcelColumn(name = "菜单类型", order = 4, width = 10)
    private String menuType;

    @ExcelColumn(name = "路由地址", order = 5, width = 25)
    private String path;

    @ExcelColumn(name = "组件路径", order = 6, width = 30)
    private String component;

    @ExcelColumn(name = "权限标识", order = 7, width = 25)
    private String perms;

    @ExcelColumn(name = "图标", order = 8, width = 15)
    private String icon;

    @ExcelColumn(name = "排序", order = 9, width = 8)
    private Integer orderNum;

    @ExcelColumn(name = "是否外链", order = 10, width = 10)
    private Integer isFrame;

    @ExcelColumn(name = "是否缓存", order = 11, width = 10)
    private Integer isCache;

    @ExcelColumn(name = "是否可见", order = 12, width = 10)
    private Integer visible;

    @ExcelColumn(name = "状态", order = 13, width = 8, dictType = "sys_normal_disable")
    private Integer status;

    @ExcelColumn(name = "创建时间", order = 14, width = 20)
    private LocalDateTime createTime;

    @SuppressWarnings("all")
    public MenuExportVO() {
    }

    @SuppressWarnings("all")
    public MenuExportVO(final Long id, final Integer level, final String menuName, final String menuType,
                        final String path, final String component, final String perms, final String icon, final Integer orderNum,
                        final Integer isFrame, final Integer isCache, final Integer visible, final Integer status,
                        final LocalDateTime createTime) {
        this.id = id;
        this.level = level;
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
        this.createTime = createTime;
    }

    @SuppressWarnings("all")
    public static MenuExportVO.MenuExportVOBuilder builder() {
        return new MenuExportVO.MenuExportVOBuilder();
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
    public Integer getLevel() {
        return this.level;
    }

    @SuppressWarnings("all")
    public void setLevel(final Integer level) {
        this.level = level;
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
        if (!(o instanceof MenuExportVO))
            return false;
        final MenuExportVO other = (MenuExportVO) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$level = this.getLevel();
        final java.lang.Object other$level = other.getLevel();
        if (this$level == null ? other$level != null : !this$level.equals(other$level))
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
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime))
            return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MenuExportVO;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $level = this.getLevel();
        result = result * PRIME + ($level == null ? 43 : $level.hashCode());
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
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MenuExportVO(id=" + this.getId() + ", level=" + this.getLevel() + ", menuName=" + this.getMenuName()
                + ", menuType=" + this.getMenuType() + ", path=" + this.getPath() + ", component=" + this.getComponent()
                + ", perms=" + this.getPerms() + ", icon=" + this.getIcon() + ", orderNum=" + this.getOrderNum()
                + ", isFrame=" + this.getIsFrame() + ", isCache=" + this.getIsCache() + ", visible=" + this.getVisible()
                + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ")";
    }

    @SuppressWarnings("all")
    public static class MenuExportVOBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private Integer level;

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
        private LocalDateTime createTime;

        @SuppressWarnings("all")
        MenuExportVOBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder level(final Integer level) {
            this.level = level;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder menuName(final String menuName) {
            this.menuName = menuName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder menuType(final String menuType) {
            this.menuType = menuType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder path(final String path) {
            this.path = path;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder component(final String component) {
            this.component = component;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder perms(final String perms) {
            this.perms = perms;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder icon(final String icon) {
            this.icon = icon;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder orderNum(final Integer orderNum) {
            this.orderNum = orderNum;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder isFrame(final Integer isFrame) {
            this.isFrame = isFrame;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder isCache(final Integer isCache) {
            this.isCache = isCache;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder visible(final Integer visible) {
            this.visible = visible;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public MenuExportVO.MenuExportVOBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        @SuppressWarnings("all")
        public MenuExportVO build() {
            return new MenuExportVO(this.id, this.level, this.menuName, this.menuType, this.path, this.component,
                    this.perms, this.icon, this.orderNum, this.isFrame, this.isCache, this.visible, this.status,
                    this.createTime);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "MenuExportVO.MenuExportVOBuilder(id=" + this.id + ", level=" + this.level + ", menuName="
                    + this.menuName + ", menuType=" + this.menuType + ", path=" + this.path + ", component="
                    + this.component + ", perms=" + this.perms + ", icon=" + this.icon + ", orderNum=" + this.orderNum
                    + ", isFrame=" + this.isFrame + ", isCache=" + this.isCache + ", visible=" + this.visible
                    + ", status=" + this.status + ", createTime=" + this.createTime + ")";
        }

    }

}
