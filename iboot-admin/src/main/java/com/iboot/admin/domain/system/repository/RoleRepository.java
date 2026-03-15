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

package com.iboot.admin.domain.system.repository;

import com.iboot.admin.domain.system.model.Role;

import java.util.List;
import java.util.Optional;

/**
 * 角色仓储接口
 * <p>
 * 负责角色数据的持久化操作，支持角色管理、菜单关联和数据权限管理
 * </p>
 *
 * @author iBoot
 */
public interface RoleRepository {

    /**
     * 保存角色
     *
     * @param role 角色实体
     * @return 保存后的角色
     */
    Role save(Role role);

    /**
     * 根据 ID 查询角色
     *
     * @param id 角色 ID
     * @return 角色实体，不存在则返回空
     */
    Optional<Role> findById(Long id);

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     * @return 角色实体，不存在则返回空
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    List<Role> findAll();

    /**
     * 分页查询角色
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 角色列表
     */
    List<Role> findPage(int pageNum, int pageSize);

    /**
     * 统计角色总数
     *
     * @return 角色总数
     */
    long count();

    /**
     * 按条件分页查询角色
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status 状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 角色列表
     */
    List<Role> findPageByCondition(String roleName, String roleCode, Integer status, int pageNum, int pageSize);

    /**
     * 按条件查询所有角色（不分页，用于导出）
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status 状态（可选）
     * @return 角色列表
     */
    List<Role> findAllByCondition(String roleName, String roleCode, Integer status);

    /**
     * 按条件统计角色总数
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status 状态（可选）
     * @return 角色总数
     */
    long countByCondition(String roleName, String roleCode, Integer status);

    /**
     * 更新角色
     *
     * @param role 角色实体
     * @return 是否更新成功
     */
    boolean update(Role role);

    /**
     * 根据 ID 删除角色（逻辑删除）
     *
     * @param id 角色 ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     * @return 是否存在
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * 检查角色名称是否存在
     *
     * @param roleName 角色名称
     * @return 是否存在
     */
    boolean existsByRoleName(String roleName);

    /**
     * 根据用户 ID 查询用户拥有的角色
     *
     * @param userId 用户 ID
     * @return 角色列表
     */
    List<Role> findByUserId(Long userId);

    /**
     * 统计拥有指定角色的用户数
     *
     * @param roleId 角色 ID
     * @return 用户数
     */
    long countUsersByRoleId(Long roleId);

    /**
     * 删除角色的菜单关联
     *
     * @param roleId 角色 ID
     */
    void deleteRoleMenus(Long roleId);

    /**
     * 插入角色的菜单关联
     *
     * @param roleId 角色 ID
     * @param menuIds 菜单 ID 列表
     */
    void insertRoleMenus(Long roleId, List<Long> menuIds);

    /**
     * 删除角色的部门关联（数据权限）
     *
     * @param roleId 角色 ID
     */
    void deleteRoleDepts(Long roleId);

    /**
     * 插入角色的部门关联（数据权限）
     *
     * @param roleId 角色 ID
     * @param deptIds 部门 ID 列表
     */
    void insertRoleDepts(Long roleId, List<Long> deptIds);

    /**
     * 查询角色拥有的菜单 ID 列表
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    List<Long> selectMenuIdsByRoleId(Long roleId);

    /**
     * 查询角色拥有的部门 ID 列表（数据权限）
     *
     * @param roleId 角色 ID
     * @return 部门 ID 列表
     */
    List<Long> selectDeptIdsByRoleId(Long roleId);

    /**
     * 物理删除已逻辑删除的角色记录（根据角色编码）
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param roleCode 角色编码
     * @return 是否删除成功
     */
    boolean removeDeletedByRoleCode(String roleCode);
}
