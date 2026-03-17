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
 * 菜单请求 DTO
 *
 * @author iBoot
 */
@Schema(description = "菜单请求")
public class MenuRequest {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "父菜单ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50")
    private String menuName;

    @Schema(description = "菜单类型：M-目录，C-菜单，F-按钮")
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    @Schema(description = "路由地址")
    @Size(max = 200, message = "路由地址长度不能超过200")
    private String path;

    @Schema(description = "组件路径")
    @Size(max = 255, message = "组件路径长度不能超过255")
    private String component;

    @Schema(description = "权限标识")
    @Size(max = 100, message = "权限标识长度不能超过100")
    private String perms;

    @Schema(description = "菜单图标")
    @Size(max = 100, message = "菜单图标长度不能超过100")
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
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;

    @SuppressWarnings("all")
    public MenuRequest() {
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

    @java.lang.Override
    @SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MenuRequest))
            return false;
        final MenuRequest other = (MenuRequest) o;
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
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MenuRequest;
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
        return result;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public java.lang.String toString() {
        return "MenuRequest(id=" + this.getId() + ", parentId=" + this.getParentId() + ", menuName="
                + this.getMenuName() + ", menuType=" + this.getMenuType() + ", path=" + this.getPath() + ", component="
                + this.getComponent() + ", perms=" + this.getPerms() + ", icon=" + this.getIcon() + ", orderNum="
                + this.getOrderNum() + ", isFrame=" + this.getIsFrame() + ", isCache=" + this.getIsCache()
                + ", visible=" + this.getVisible() + ", status=" + this.getStatus() + ", remark=" + this.getRemark()
                + ")";
    }

}
