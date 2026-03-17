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

import com.iboot.admin.infrastructure.persistence.po.RolePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper 接口
 * <p>
 * 负责角色数据的数据库操作，支持角色管理和菜单、部门关联
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface RoleMapper {

    /**
     * 插入角色记录
     *
     * @param role 角色持久化对象
     *
     * @return 影响的记录数
     */
    int insert(RolePO role);

    /**
     * 更新角色记录
     *
     * @param role 角色持久化对象
     *
     * @return 影响的记录数
     */
    int update(RolePO role);

    /**
     * 根据 ID 删除角色记录
     *
     * @param id 角色 ID
     *
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询角色记录
     *
     * @param id 角色 ID
     *
     * @return 角色持久化对象
     */
    RolePO selectById(@Param("id") Long id);

    /**
     * 根据角色编码查询角色记录
     *
     * @param roleCode 角色编码
     *
     * @return 角色持久化对象
     */
    RolePO selectByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 查询所有角色记录
     *
     * @return 角色持久化对象列表
     */
    List<RolePO> selectAll();

    /**
     * 分页查询角色记录
     *
     * @param offset 偏移量
     * @param limit  限制数量
     *
     * @return 角色持久化对象列表
     */
    List<RolePO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计角色总数
     *
     * @return 角色总数
     */
    long count();

    /**
     * 按条件分页查询角色记录
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status   状态（可选）
     * @param offset   偏移量
     * @param limit    限制数量
     *
     * @return 角色持久化对象列表
     */
    List<RolePO> selectPageByCondition(@Param("roleName") String roleName, @Param("roleCode") String roleCode,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按条件查询所有角色记录（不分页，用于导出）
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status   状态（可选）
     *
     * @return 角色持久化对象列表
     */
    List<RolePO> selectAllByCondition(@Param("roleName") String roleName, @Param("roleCode") String roleCode,
                                      @Param("status") Integer status);

    /**
     * 按条件统计角色总数
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status   状态（可选）
     *
     * @return 角色总数
     */
    long countByCondition(@Param("roleName") String roleName, @Param("roleCode") String roleCode,
                          @Param("status") Integer status);

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     *
     * @return 存在返回 1，否则返回 0
     */
    int existsByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 根据用户 ID 查询角色列表
     *
     * @param userId 用户 ID
     *
     * @return 角色持久化对象列表
     */
    List<RolePO> selectByUserId(@Param("userId") Long userId);

    /**
     * 检查角色名称是否存在
     *
     * @param roleName 角色名称
     *
     * @return 存在返回 1，否则返回 0
     */
    int existsByRoleName(@Param("roleName") String roleName);

    /**
     * 统计角色关联的用户数
     *
     * @param roleId 角色 ID
     *
     * @return 用户数
     */
    long countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 插入角色菜单关联记录
     *
     * @param roleId 角色 ID
     * @param menuId 菜单 ID
     *
     * @return 影响的记录数
     */
    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    /**
     * 根据角色 ID 删除角色菜单关联记录
     *
     * @param roleId 角色 ID
     *
     * @return 影响的记录数
     */
    int deleteRoleMenuByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色 ID 查询菜单 ID 列表
     *
     * @param roleId 角色 ID
     *
     * @return 菜单 ID 列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 插入角色部门关联记录
     *
     * @param roleId 角色 ID
     * @param deptId 部门 ID
     *
     * @return 影响的记录数
     */
    int insertRoleDept(@Param("roleId") Long roleId, @Param("deptId") Long deptId);

    /**
     * 根据角色 ID 删除角色部门关联记录
     *
     * @param roleId 角色 ID
     *
     * @return 影响的记录数
     */
    int deleteRoleDeptByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色 ID 查询部门 ID 列表
     *
     * @param roleId 角色 ID
     *
     * @return 部门 ID 列表
     */
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 物理删除已逻辑删除的角色记录（根据角色编码）
     *
     * @param roleCode 角色编码
     *
     * @return 影响的记录数
     */
    int removeDeletedByRoleCode(@Param("roleCode") String roleCode);

}
