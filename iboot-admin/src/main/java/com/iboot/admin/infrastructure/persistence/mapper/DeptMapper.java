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

package com.iboot.admin.infrastructure.persistence.mapper;

import com.iboot.admin.common.annotation.DataScope;
import com.iboot.admin.infrastructure.persistence.po.DeptPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门 Mapper 接口
 * <p>
 * 负责部门的数据库操作，提供增删改查和树形结构查询方法
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface DeptMapper {

    /**
     * 插入部门记录
     *
     * @param deptPO 部门持久化对象
     *
     * @return 影响的记录数
     */
    int insert(DeptPO deptPO);

    /**
     * 更新部门记录
     *
     * @param deptPO 部门持久化对象
     *
     * @return 影响的记录数
     */
    int update(DeptPO deptPO);

    /**
     * 根据 ID 删除部门
     *
     * @param id 部门 ID
     *
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询部门
     *
     * @param id 部门 ID
     *
     * @return 部门持久化对象
     */
    DeptPO selectById(@Param("id") Long id);

    /**
     * 查询所有部门（受数据权限控制）
     *
     * @return 部门列表
     */
    @DataScope(deptAlias = "d", userAlias = "d")
    List<DeptPO> selectAll();

    /**
     * 根据父 ID 查询子部门列表
     *
     * @param parentId 父部门 ID
     *
     * @return 子部门列表
     */
    List<DeptPO> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询部门及其所有子部门 ID（递归）
     *
     * @param deptId 部门 ID
     *
     * @return 部门 ID 列表
     */
    List<Long> selectChildrenIds(@Param("deptId") Long deptId);

    /**
     * 统计部门总数
     *
     * @return 总数
     */
    long count();

    /**
     * 根据部门编码统计数量
     *
     * @param deptCode 部门编码
     *
     * @return 数量
     */
    int countByDeptCode(@Param("deptCode") String deptCode);

    /**
     * 根据部门名称和父 ID 统计数量
     *
     * @param deptName 部门名称
     * @param parentId 父部门 ID
     *
     * @return 数量
     */
    int countByDeptNameAndParentId(@Param("deptName") String deptName, @Param("parentId") Long parentId);

    /**
     * 统计子部门数量
     *
     * @param deptId 部门 ID
     *
     * @return 子部门数量
     */
    int countChildren(@Param("deptId") Long deptId);

    /**
     * 统计每个部门的用户数量
     *
     * @return 部门 ID 和用户数量的映射列表
     */
    List<DeptUserCountDTO> selectDeptUserCounts();

    /**
     * 部门用户数量统计 DTO
     */
    class DeptUserCountDTO {

        /**
         * 部门 ID
         */
        private Long deptId;

        /**
         * 用户数量
         */
        private Integer userCount;

        @SuppressWarnings("all")
        public DeptUserCountDTO() {
        }

        /**
         * 部门 ID
         */
        @SuppressWarnings("all")
        public Long getDeptId() {
            return this.deptId;
        }

        /**
         * 部门 ID
         */
        @SuppressWarnings("all")
        public void setDeptId(final Long deptId) {
            this.deptId = deptId;
        }

        /**
         * 用户数量
         */
        @SuppressWarnings("all")
        public Integer getUserCount() {
            return this.userCount;
        }

        /**
         * 用户数量
         */
        @SuppressWarnings("all")
        public void setUserCount(final Integer userCount) {
            this.userCount = userCount;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public boolean equals(final java.lang.Object o) {
            if (o == this)
                return true;
            if (!(o instanceof DeptMapper.DeptUserCountDTO))
                return false;
            final DeptMapper.DeptUserCountDTO other = (DeptMapper.DeptUserCountDTO) o;
            if (!other.canEqual((java.lang.Object) this))
                return false;
            final java.lang.Object this$deptId = this.getDeptId();
            final java.lang.Object other$deptId = other.getDeptId();
            if (this$deptId == null ? other$deptId != null : !this$deptId.equals(other$deptId))
                return false;
            final java.lang.Object this$userCount = this.getUserCount();
            final java.lang.Object other$userCount = other.getUserCount();
            if (this$userCount == null ? other$userCount != null : !this$userCount.equals(other$userCount))
                return false;
            return true;
        }

        @SuppressWarnings("all")
        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof DeptMapper.DeptUserCountDTO;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $deptId = this.getDeptId();
            result = result * PRIME + ($deptId == null ? 43 : $deptId.hashCode());
            final java.lang.Object $userCount = this.getUserCount();
            result = result * PRIME + ($userCount == null ? 43 : $userCount.hashCode());
            return result;
        }

        @java.lang.Override
        @SuppressWarnings("all")
        public java.lang.String toString() {
            return "DeptMapper.DeptUserCountDTO(deptId=" + this.getDeptId() + ", userCount=" + this.getUserCount()
                    + ")";
        }

    }

}
