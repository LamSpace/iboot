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

import com.iboot.admin.domain.system.model.Menu;
import com.iboot.admin.domain.system.repository.MenuRepository;
import com.iboot.admin.infrastructure.persistence.mapper.MenuMapper;
import com.iboot.admin.infrastructure.persistence.po.MenuPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 菜单仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现菜单数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuMapper menuMapper;

    @SuppressWarnings("all")
    public MenuRepositoryImpl(final MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * 保存菜单
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param menu 菜单实体
     *
     * @return 保存后的菜单
     */
    @Override
    public Menu save(Menu menu) {
        MenuPO po = convertToPO(menu);
        if (menu.getId() == null) {
            menuMapper.insert(po);
        } else {
            menuMapper.update(po);
        }
        return convertToDomain(po);
    }

    /**
     * 根据 ID 查询菜单
     *
     * @param id 菜单 ID
     *
     * @return 菜单实体，不存在则返回空
     */
    @Override
    public Optional<Menu> findById(Long id) {
        return Optional.ofNullable(menuMapper.selectById(id)).map(this::convertToDomain);
    }

    /**
     * 查询所有菜单
     *
     * @return 菜单列表
     */
    @Override
    public List<Menu> findAll() {
        return menuMapper.selectAll().stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 根据父菜单 ID 查询子菜单
     *
     * @param parentId 父菜单 ID
     *
     * @return 子菜单列表
     */
    @Override
    public List<Menu> findByParentId(Long parentId) {
        return menuMapper.selectByParentId(parentId).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 更新菜单
     *
     * @param menu 菜单实体
     *
     * @return 是否更新成功
     */
    @Override
    public boolean update(Menu menu) {
        return menuMapper.update(convertToPO(menu)) > 0;
    }

    /**
     * 根据 ID 删除菜单（逻辑删除）
     *
     * @param id 菜单 ID
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return menuMapper.deleteById(id) > 0;
    }

    /**
     * 根据用户 ID 查询用户有权限的菜单
     *
     * @param userId 用户 ID
     *
     * @return 菜单列表
     */
    @Override
    public List<Menu> findByUserId(Long userId) {
        return menuMapper.selectByUserId(userId).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 根据角色 ID 查询角色拥有的菜单
     *
     * @param roleId 角色 ID
     *
     * @return 菜单列表
     */
    @Override
    public List<Menu> findByRoleId(Long roleId) {
        return menuMapper.selectByRoleId(roleId).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 根据权限标识查询菜单
     *
     * @param permission 权限标识
     *
     * @return 菜单实体，不存在则返回空
     */
    @Override
    public Optional<Menu> findByPermission(String permission) {
        return Optional.ofNullable(menuMapper.selectByPermission(permission)).map(this::convertToDomain);
    }

    /**
     * 查询菜单树形结构
     *
     * @return 菜单树列表
     */
    @Override
    public List<Menu> findMenuTree() {
        return menuMapper.selectAll().stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 检查菜单名称是否已存在（同一父菜单下）
     *
     * @param menuName 菜单名称
     * @param parentId 父菜单 ID
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByMenuNameAndParentId(String menuName, Long parentId) {
        return menuMapper.existsByMenuNameAndParentId(menuName, parentId) > 0;
    }

    /**
     * 检查菜单是否有子菜单
     *
     * @param id 菜单 ID
     *
     * @return 是否有子菜单
     */
    @Override
    public boolean hasChildren(Long id) {
        return menuMapper.countByParentId(id) > 0;
    }

    /**
     * 统计菜单总数
     *
     * @return 菜单总数
     */
    @Override
    public long count() {
        return menuMapper.count();
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param menu 菜单领域对象
     *
     * @return 菜单持久化对象
     */
    private MenuPO convertToPO(Menu menu) {
        MenuPO po = new MenuPO();
        po.setId(menu.getId());
        po.setMenuName(menu.getMenuName());
        po.setParentId(menu.getParentId());
        po.setMenuType(menu.getMenuType());
        po.setPath(menu.getPath());
        po.setComponent(menu.getComponent());
        po.setPermission(menu.getPermission());
        po.setIcon(menu.getIcon());
        po.setOrderNum(menu.getOrderNum());
        po.setIsFrame(menu.getIsFrame());
        po.setIsCache(menu.getIsCache());
        po.setVisible(menu.getVisible());
        po.setStatus(menu.getStatus());
        po.setCreateBy(menu.getCreateBy());
        po.setCreateTime(menu.getCreateTime());
        po.setUpdateBy(menu.getUpdateBy());
        po.setUpdateTime(menu.getUpdateTime());
        po.setDeleted(menu.getDeleted() != null ? menu.getDeleted() : 0);
        po.setRemark(menu.getRemark());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 菜单持久化对象
     *
     * @return 菜单领域对象
     */
    private Menu convertToDomain(MenuPO po) {
        return Menu.builder()
                .id(po.getId())
                .menuName(po.getMenuName())
                .parentId(po.getParentId())
                .menuType(po.getMenuType())
                .path(po.getPath())
                .component(po.getComponent())
                .permission(po.getPermission())
                .icon(po.getIcon())
                .orderNum(po.getOrderNum())
                .isFrame(po.getIsFrame())
                .isCache(po.getIsCache())
                .visible(po.getVisible())
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
