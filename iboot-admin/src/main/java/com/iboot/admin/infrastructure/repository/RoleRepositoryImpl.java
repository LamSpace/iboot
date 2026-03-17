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

package com.iboot.admin.infrastructure.repository;

import com.iboot.admin.domain.system.model.Role;
import com.iboot.admin.domain.system.repository.RoleRepository;
import com.iboot.admin.infrastructure.persistence.mapper.RoleMapper;
import com.iboot.admin.infrastructure.persistence.po.RolePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 角色仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现角色数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    @SuppressWarnings("all")
    public RoleRepositoryImpl(final RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 保存角色
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param role 角色实体
     *
     * @return 保存后的角色
     */
    @Override
    public Role save(Role role) {
        RolePO po = convertToPO(role);
        if (role.getId() == null) {
            roleMapper.insert(po);
        } else {
            roleMapper.update(po);
        }
        return convertToDomain(po);
    }

    /**
     * 根据 ID 查询角色
     *
     * @param id 角色 ID
     *
     * @return 角色实体，不存在则返回空
     */
    @Override
    public Optional<Role> findById(Long id) {
        return Optional.ofNullable(roleMapper.selectById(id)).map(this::convertToDomain);
    }

    /**
     * 根据角色编码查询角色
     *
     * @param roleCode 角色编码
     *
     * @return 角色实体，不存在则返回空
     */
    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        return Optional.ofNullable(roleMapper.selectByRoleCode(roleCode)).map(this::convertToDomain);
    }

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    @Override
    public List<Role> findAll() {
        return roleMapper.selectAll().stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 分页查询角色
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 角色列表
     */
    @Override
    public List<Role> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return roleMapper.selectPage(offset, pageSize).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 统计角色总数
     *
     * @return 角色总数
     */
    @Override
    public long count() {
        return roleMapper.count();
    }

    /**
     * 按条件分页查询角色
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status   状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     *
     * @return 角色列表
     */
    @Override
    public List<Role> findPageByCondition(String roleName, String roleCode, Integer status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return roleMapper.selectPageByCondition(roleName, roleCode, status, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件查询所有角色（不分页，用于导出）
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status   状态（可选）
     *
     * @return 角色列表
     */
    @Override
    public List<Role> findAllByCondition(String roleName, String roleCode, Integer status) {
        return roleMapper.selectAllByCondition(roleName, roleCode, status)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计角色总数
     *
     * @param roleName 角色名称（可选）
     * @param roleCode 角色编码（可选）
     * @param status   状态（可选）
     *
     * @return 角色总数
     */
    @Override
    public long countByCondition(String roleName, String roleCode, Integer status) {
        return roleMapper.countByCondition(roleName, roleCode, status);
    }

    /**
     * 更新角色
     *
     * @param role 角色实体
     *
     * @return 是否更新成功
     */
    @Override
    public boolean update(Role role) {
        return roleMapper.update(convertToPO(role)) > 0;
    }

    /**
     * 根据 ID 删除角色（逻辑删除）
     *
     * @param id 角色 ID
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return roleMapper.deleteById(id) > 0;
    }

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByRoleCode(String roleCode) {
        return roleMapper.existsByRoleCode(roleCode) > 0;
    }

    /**
     * 检查角色名称是否存在
     *
     * @param roleName 角色名称
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByRoleName(String roleName) {
        return roleMapper.existsByRoleName(roleName) > 0;
    }

    /**
     * 根据用户 ID 查询用户拥有的角色
     *
     * @param userId 用户 ID
     *
     * @return 角色列表
     */
    @Override
    public List<Role> findByUserId(Long userId) {
        return roleMapper.selectByUserId(userId).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 统计拥有指定角色的用户数
     *
     * @param roleId 角色 ID
     *
     * @return 用户数
     */
    @Override
    public long countUsersByRoleId(Long roleId) {
        return roleMapper.countUsersByRoleId(roleId);
    }

    /**
     * 删除角色的菜单关联
     *
     * @param roleId 角色 ID
     */
    @Override
    public void deleteRoleMenus(Long roleId) {
        roleMapper.deleteRoleMenuByRoleId(roleId);
    }

    /**
     * 插入角色的菜单关联
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表
     */
    @Override
    public void insertRoleMenus(Long roleId, List<Long> menuIds) {
        for (Long menuId : menuIds) {
            roleMapper.insertRoleMenu(roleId, menuId);
        }
    }

    /**
     * 删除角色的部门关联（数据权限）
     *
     * @param roleId 角色 ID
     */
    @Override
    public void deleteRoleDepts(Long roleId) {
        roleMapper.deleteRoleDeptByRoleId(roleId);
    }

    /**
     * 插入角色的部门关联（数据权限）
     *
     * @param roleId  角色 ID
     * @param deptIds 部门 ID 列表
     */
    @Override
    public void insertRoleDepts(Long roleId, List<Long> deptIds) {
        for (Long deptId : deptIds) {
            roleMapper.insertRoleDept(roleId, deptId);
        }
    }

    /**
     * 查询角色拥有的菜单 ID 列表
     *
     * @param roleId 角色 ID
     *
     * @return 菜单 ID 列表
     */
    @Override
    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        return roleMapper.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 查询角色拥有的部门 ID 列表（数据权限）
     *
     * @param roleId 角色 ID
     *
     * @return 部门 ID 列表
     */
    @Override
    public List<Long> selectDeptIdsByRoleId(Long roleId) {
        return roleMapper.selectDeptIdsByRoleId(roleId);
    }

    /**
     * 物理删除已逻辑删除的角色记录（根据角色编码）
     *
     * @param roleCode 角色编码
     *
     * @return 是否删除成功
     */
    @Override
    public boolean removeDeletedByRoleCode(String roleCode) {
        return roleMapper.removeDeletedByRoleCode(roleCode) > 0;
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param role 角色领域对象
     *
     * @return 角色持久化对象
     */
    private RolePO convertToPO(Role role) {
        RolePO po = new RolePO();
        po.setId(role.getId());
        po.setRoleName(role.getRoleName());
        po.setRoleCode(role.getRoleCode());
        po.setRoleSort(role.getRoleSort());
        po.setDataScope(role.getDataScope());
        po.setStatus(role.getStatus());
        po.setCreateBy(role.getCreateBy());
        po.setCreateTime(role.getCreateTime());
        po.setUpdateBy(role.getUpdateBy());
        po.setUpdateTime(role.getUpdateTime());
        po.setDeleted(role.getDeleted() != null ? role.getDeleted() : 0);
        po.setRemark(role.getRemark());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 角色持久化对象
     *
     * @return 角色领域对象
     */
    private Role convertToDomain(RolePO po) {
        return Role.builder()
                .id(po.getId())
                .roleName(po.getRoleName())
                .roleCode(po.getRoleCode())
                .roleSort(po.getRoleSort())
                .dataScope(po.getDataScope())
                .status(po.getStatus())
                .createBy(po.getCreateBy())
                .createTime(po.getCreateTime())
                .updateBy(po.getUpdateBy())
                .updateTime(po.getUpdateTime())
                .deleted(po.getDeleted())
                .remark(po.getRemark())
                .build();
    }

}
