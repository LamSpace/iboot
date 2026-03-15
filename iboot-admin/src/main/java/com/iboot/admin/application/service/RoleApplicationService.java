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

package com.iboot.admin.application.service;

import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.Role;
import com.iboot.admin.domain.system.repository.RoleRepository;
import com.iboot.admin.infrastructure.interceptor.DataScopeInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色应用服务
 * <p>
 * 负责角色的创建、更新、删除、查询、权限分配和数据权限设置等业务逻辑处理，
 * 支持菜单权限分配和数据范围权限设置
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final DataScopeInterceptor dataScopeInterceptor;

    /**
     * 创建角色
     * <p>
     * 校验角色编码和名称的唯一性，清理同编码已删除角色记录，
     * 设置默认状态为启用
     * </p>
     *
     * @param role 角色实体
     * @return 创建后的角色实体
     * @throws BusinessException 当角色编码或名称已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Role createRole(Role role) {
        // 校验角色编码唯一性
        if (roleRepository.existsByRoleCode(role.getRoleCode())) {
            throw new BusinessException("角色编码已存在");
        }

        // 清理同编码已删除角色记录（解决逻辑删除与唯一索引冲突问题）
        roleRepository.removeDeletedByRoleCode(role.getRoleCode());

        // 校验角色名称唯一性
        if (roleRepository.existsByRoleName(role.getRoleName())) {
            throw new BusinessException("角色名称已存在");
        }

        role.setCreateTime(LocalDateTime.now());
        role.setStatus(1);
        return roleRepository.save(role);
    }

    /**
     * 更新角色
     * <p>
     * 检查角色是否存在，超级管理员角色禁止修改角色编码
     * </p>
     *
     * @param role 角色实体
     * @return 是否更新成功
     * @throws BusinessException 当角色不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(Role role) {
        Role existingRole = roleRepository.findById(role.getId())
                .orElseThrow(() -> new BusinessException("角色不存在"));

        // 超级管理员角色禁止修改角色编码
        if (Constants.ROLE_ADMIN_CODE.equals(existingRole.getRoleCode())) {
            role.setRoleCode(existingRole.getRoleCode());
        }

        role.setUpdateTime(LocalDateTime.now());
        return roleRepository.update(role);
    }

    /**
     * 删除角色
     <p>
     * 检查角色是否存在，超级管理员角色不允许删除，
     * 已分配给用户的角色不能删除
     * </p>
     *
     * @param id 角色 ID
     * @return 是否删除成功
     * @throws BusinessException 当角色不存在、是超级管理员角色或已分配给用户时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));

        if (Constants.ROLE_ADMIN_CODE.equals(role.getRoleCode())) {
            throw new BusinessException("超级管理员角色不允许删除");
        }

        // 检查是否有用户关联此角色
        if (roleRepository.countUsersByRoleId(id) > 0) {
            throw new BusinessException("角色已分配给用户，不能删除");
        }

        return roleRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取角色
     *
     * @param id 角色 ID
     * @return 角色实体
     * @throws BusinessException 当角色不存在时抛出
     */
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
    }

    /**
     * 获取所有角色
     *
     * @return 角色列表
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * 分页获取角色
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 角色列表
     */
    public List<Role> getRolePage(int pageNum, int pageSize) {
        return roleRepository.findPage(pageNum, pageSize);
    }

    /**
     * 统计角色总数
     *
     * @return 角色总数
     */
    public long countRoles() {
        return roleRepository.count();
    }

    /**
     * 按条件分页获取角色
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status 状态（可选）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 角色列表
     */
    public List<Role> getRolePageByCondition(String roleName, String roleCode, Integer status, int pageNum, int pageSize) {
        return roleRepository.findPageByCondition(roleName, roleCode, status, pageNum, pageSize);
    }

    /**
     * 按条件获取所有角色（不分页，用于导出）
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status 状态（可选）
     * @return 角色列表
     */
    public List<Role> getAllRolesByCondition(String roleName, String roleCode, Integer status) {
        return roleRepository.findAllByCondition(roleName, roleCode, status);
    }

    /**
     * 按条件统计角色总数
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status 状态（可选）
     * @return 角色总数
     */
    public long countRolesByCondition(String roleName, String roleCode, Integer status) {
        return roleRepository.countByCondition(roleName, roleCode, status);
    }

    /**
     * 分配菜单权限
     * <p>
     * 检查角色是否存在，先删除旧的角色 - 菜单关联，再插入新的关联
     * </p>
     *
     * @param roleId 角色 ID
     * @param menuIds 菜单 ID 列表
     * @return 是否分配成功
     * @throws BusinessException 当角色不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenus(Long roleId, List<Long> menuIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("角色不存在"));

        role.assignMenus(menuIds);

        // 先删除旧的角色 - 菜单关联
        roleRepository.deleteRoleMenus(roleId);

        // 插入新的关联
        if (menuIds != null && !menuIds.isEmpty()) {
            roleRepository.insertRoleMenus(roleId, menuIds);
        }

        return true;
    }

    /**
     * 设置数据权限
     * <p>
     * 检查角色是否存在，设置数据范围，如果是自定义数据范围则需要指定部门列表。
     * 完成后清除数据权限缓存。
     * </p>
     *
     * @param roleId 角色 ID
     * @param dataScope 数据范围
     * @param deptIds 部门 ID 列表（自定义数据范围时需要）
     * @return 是否设置成功
     * @throws BusinessException 当角色不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean setDataPermission(Long roleId, Integer dataScope, List<Long> deptIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("角色不存在"));

        role.setDataPermission(dataScope, deptIds);
        roleRepository.update(role);

        // 先删除旧的角色 - 部门关联
        roleRepository.deleteRoleDepts(roleId);

        // 如果是自定义数据权限，插入新的关联
        if (dataScope == 2 && deptIds != null && !deptIds.isEmpty()) {
            roleRepository.insertRoleDepts(roleId, deptIds);
        }

        // 清除数据权限缓存（所有用户的缓存都需要清除，因为角色权限已变更）
        dataScopeInterceptor.clearUserScopeCache(null);

        return true;
    }

    /**
     * 获取角色的菜单 ID 列表
     *
     * @param roleId 角色 ID
     * @return 菜单 ID 列表
     */
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleRepository.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 获取角色的部门 ID 列表（数据权限）
     *
     * @param roleId 角色 ID
     * @return 部门 ID 列表
     */
    public List<Long> getRoleDeptIds(Long roleId) {
        return roleRepository.selectDeptIdsByRoleId(roleId);
    }

    /**
     * 修改角色状态
     *
     * @param id 角色 ID
     * @param status 状态：1-启用，0-停用
     * @return 是否修改成功
     * @throws BusinessException 当角色不存在或超级管理员角色停用时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(Long id, Integer status) {
        Role role = getRoleById(id);
        if (Constants.ROLE_ADMIN_CODE.equals(role.getRoleCode()) && status == 0) {
            throw new BusinessException("超级管理员角色不允许停用");
        }
        if (status == 1) {
            role.enable();
        } else {
            role.disable();
        }
        return roleRepository.update(role);
    }
}
