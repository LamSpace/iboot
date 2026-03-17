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
 * 角色实体（聚合根）
 *
 * @author iBoot
 */
public class Role {

    /**
     * 角色 ID
     */
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色排序
     */
    private Integer roleSort;

    /**
     * 数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人
     */
    private Integer dataScope;

    /**
     * 菜单 ID 列表
     */
    private List<Long> menuIds;

    /**
     * 部门 ID 列表（数据权限）
     */
    private List<Long> deptIds;

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
    public Role() {
    }

    /**
     * Creates a new {@code Role} instance.
     *
     * @param id         角色 ID
     * @param roleName   角色名称
     * @param roleCode   角色编码
     * @param roleSort   角色排序
     * @param dataScope  数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人
     * @param menuIds    菜单 ID 列表
     * @param deptIds    部门 ID 列表（数据权限）
     * @param status     状态：0-停用，1-正常
     * @param createBy   创建人
     * @param createTime 创建时间
     * @param updateBy   更新人
     * @param updateTime 更新时间
     * @param deleted    逻辑删除：0-未删除，1-已删除
     * @param remark     备注
     */
    @SuppressWarnings("all")
    public Role(final Long id, final String roleName, final String roleCode, final Integer roleSort,
                final Integer dataScope, final List<Long> menuIds, final List<Long> deptIds, final Integer status,
                final String createBy, final LocalDateTime createTime, final String updateBy,
                final LocalDateTime updateTime, final Integer deleted, final String remark) {
        this.id = id;
        this.roleName = roleName;
        this.roleCode = roleCode;
        this.roleSort = roleSort;
        this.dataScope = dataScope;
        this.menuIds = menuIds;
        this.deptIds = deptIds;
        this.status = status;
        this.createBy = createBy;
        this.createTime = createTime;
        this.updateBy = updateBy;
        this.updateTime = updateTime;
        this.deleted = deleted;
        this.remark = remark;
    }

    @SuppressWarnings("all")
    public static Role.RoleBuilder builder() {
        return new Role.RoleBuilder();
    }

    /**
     * 启用角色
     *
     * @return 启用后的角色对象
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 停用角色
     *
     * @return 停用后的角色对象
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 检查角色是否启用
     *
     * @return 如果角色启用则返回true，否则返回false
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    /**
     * 分配菜单权限
     *
     * @param menuIds 菜单ID列表
     */
    public void assignMenus(List<Long> menuIds) {
        this.menuIds = menuIds;
    }

    /**
     * 设置数据权限
     *
     * @param dataScope 数据权限范围
     * @param deptIds   部门ID列表
     */
    public void setDataPermission(Integer dataScope, List<Long> deptIds) {
        this.dataScope = dataScope;
        this.deptIds = deptIds;
    }

    /**
     * 角色 ID
     */
    @SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    /**
     * 角色 ID
     */
    @SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 角色名称
     */
    @SuppressWarnings("all")
    public String getRoleName() {
        return this.roleName;
    }

    /**
     * 角色名称
     */
    @SuppressWarnings("all")
    public void setRoleName(final String roleName) {
        this.roleName = roleName;
    }

    /**
     * 角色编码
     */
    @SuppressWarnings("all")
    public String getRoleCode() {
        return this.roleCode;
    }

    /**
     * 角色编码
     */
    @SuppressWarnings("all")
    public void setRoleCode(final String roleCode) {
        this.roleCode = roleCode;
    }

    /**
     * 角色排序
     */
    @SuppressWarnings("all")
    public Integer getRoleSort() {
        return this.roleSort;
    }

    /**
     * 角色排序
     */
    @SuppressWarnings("all")
    public void setRoleSort(final Integer roleSort) {
        this.roleSort = roleSort;
    }

    /**
     * 数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人
     */
    @SuppressWarnings("all")
    public Integer getDataScope() {
        return this.dataScope;
    }

    /**
     * 数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人
     */
    @SuppressWarnings("all")
    public void setDataScope(final Integer dataScope) {
        this.dataScope = dataScope;
    }

    /**
     * 菜单 ID 列表
     */
    @SuppressWarnings("all")
    public List<Long> getMenuIds() {
        return this.menuIds;
    }

    /**
     * 菜单 ID 列表
     */
    @SuppressWarnings("all")
    public void setMenuIds(final List<Long> menuIds) {
        this.menuIds = menuIds;
    }

    /**
     * 部门 ID 列表（数据权限）
     */
    @SuppressWarnings("all")
    public List<Long> getDeptIds() {
        return this.deptIds;
    }

    /**
     * 部门 ID 列表（数据权限）
     */
    @SuppressWarnings("all")
    public void setDeptIds(final List<Long> deptIds) {
        this.deptIds = deptIds;
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
        if (!(o instanceof Role))
            return false;
        final Role other = (Role) o;
        if (!other.canEqual((java.lang.Object) this))
            return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id))
            return false;
        final java.lang.Object this$roleSort = this.getRoleSort();
        final java.lang.Object other$roleSort = other.getRoleSort();
        if (this$roleSort == null ? other$roleSort != null : !this$roleSort.equals(other$roleSort))
            return false;
        final java.lang.Object this$dataScope = this.getDataScope();
        final java.lang.Object other$dataScope = other.getDataScope();
        if (this$dataScope == null ? other$dataScope != null : !this$dataScope.equals(other$dataScope))
            return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status))
            return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted))
            return false;
        final java.lang.Object this$roleName = this.getRoleName();
        final java.lang.Object other$roleName = other.getRoleName();
        if (this$roleName == null ? other$roleName != null : !this$roleName.equals(other$roleName))
            return false;
        final java.lang.Object this$roleCode = this.getRoleCode();
        final java.lang.Object other$roleCode = other.getRoleCode();
        if (this$roleCode == null ? other$roleCode != null : !this$roleCode.equals(other$roleCode))
            return false;
        final java.lang.Object this$menuIds = this.getMenuIds();
        final java.lang.Object other$menuIds = other.getMenuIds();
        if (this$menuIds == null ? other$menuIds != null : !this$menuIds.equals(other$menuIds))
            return false;
        final java.lang.Object this$deptIds = this.getDeptIds();
        final java.lang.Object other$deptIds = other.getDeptIds();
        if (this$deptIds == null ? other$deptIds != null : !this$deptIds.equals(other$deptIds))
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
        return other instanceof Role;
    }

    @java.lang.Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $roleSort = this.getRoleSort();
        result = result * PRIME + ($roleSort == null ? 43 : $roleSort.hashCode());
        final java.lang.Object $dataScope = this.getDataScope();
        result = result * PRIME + ($dataScope == null ? 43 : $dataScope.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $roleName = this.getRoleName();
        result = result * PRIME + ($roleName == null ? 43 : $roleName.hashCode());
        final java.lang.Object $roleCode = this.getRoleCode();
        result = result * PRIME + ($roleCode == null ? 43 : $roleCode.hashCode());
        final java.lang.Object $menuIds = this.getMenuIds();
        result = result * PRIME + ($menuIds == null ? 43 : $menuIds.hashCode());
        final java.lang.Object $deptIds = this.getDeptIds();
        result = result * PRIME + ($deptIds == null ? 43 : $deptIds.hashCode());
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
        return "Role(id=" + this.getId() + ", roleName=" + this.getRoleName() + ", roleCode=" + this.getRoleCode()
                + ", roleSort=" + this.getRoleSort() + ", dataScope=" + this.getDataScope() + ", menuIds="
                + this.getMenuIds() + ", deptIds=" + this.getDeptIds() + ", status=" + this.getStatus() + ", createBy="
                + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateBy=" + this.getUpdateBy()
                + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ", remark="
                + this.getRemark() + ")";
    }

    @SuppressWarnings("all")
    public static class RoleBuilder {

        @SuppressWarnings("all")
        private Long id;

        @SuppressWarnings("all")
        private String roleName;

        @SuppressWarnings("all")
        private String roleCode;

        @SuppressWarnings("all")
        private Integer roleSort;

        @SuppressWarnings("all")
        private Integer dataScope;

        @SuppressWarnings("all")
        private List<Long> menuIds;

        @SuppressWarnings("all")
        private List<Long> deptIds;

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
        RoleBuilder() {
        }

        /**
         * 角色 ID
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * 角色名称
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder roleName(final String roleName) {
            this.roleName = roleName;
            return this;
        }

        /**
         * 角色编码
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder roleCode(final String roleCode) {
            this.roleCode = roleCode;
            return this;
        }

        /**
         * 角色排序
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder roleSort(final Integer roleSort) {
            this.roleSort = roleSort;
            return this;
        }

        /**
         * 数据权限范围：1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder dataScope(final Integer dataScope) {
            this.dataScope = dataScope;
            return this;
        }

        /**
         * 菜单 ID 列表
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder menuIds(final List<Long> menuIds) {
            this.menuIds = menuIds;
            return this;
        }

        /**
         * 部门 ID 列表（数据权限）
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder deptIds(final List<Long> deptIds) {
            this.deptIds = deptIds;
            return this;
        }

        /**
         * 状态：0-停用，1-正常
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * 创建人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder createBy(final String createBy) {
            this.createBy = createBy;
            return this;
        }

        /**
         * 创建时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder createTime(final LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 更新人
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder updateBy(final String updateBy) {
            this.updateBy = updateBy;
            return this;
        }

        /**
         * 更新时间
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        /**
         * 逻辑删除：0-未删除，1-已删除
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder deleted(final Integer deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * 备注
         *
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public Role.RoleBuilder remark(final String remark) {
            this.remark = remark;
            return this;
        }

        @SuppressWarnings("all")
        public Role build() {
            return new Role(this.id, this.roleName, this.roleCode, this.roleSort, this.dataScope, this.menuIds,
                    this.deptIds, this.status, this.createBy, this.createTime, this.updateBy, this.updateTime,
                    this.deleted, this.remark);
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "Role.RoleBuilder(id=" + this.id + ", roleName=" + this.roleName + ", roleCode=" + this.roleCode
                    + ", roleSort=" + this.roleSort + ", dataScope=" + this.dataScope + ", menuIds=" + this.menuIds
                    + ", deptIds=" + this.deptIds + ", status=" + this.status + ", createBy=" + this.createBy
                    + ", createTime=" + this.createTime + ", updateBy=" + this.updateBy + ", updateTime="
                    + this.updateTime + ", deleted=" + this.deleted + ", remark=" + this.remark + ")";
        }

    }

}
