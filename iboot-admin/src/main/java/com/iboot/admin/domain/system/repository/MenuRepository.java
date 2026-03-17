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

import com.iboot.admin.domain.system.model.Menu;

import java.util.List;
import java.util.Optional;

/**
 * 菜单仓储接口
 * <p>
 * 负责菜单数据的持久化操作，支持菜单树形结构管理和权限关联
 * </p>
 *
 * @author iBoot
 */
public interface MenuRepository {

    /**
     * 保存菜单
     *
     * @param menu 菜单实体
     *
     * @return 保存后的菜单
     */
    Menu save(Menu menu);

    /**
     * 根据 ID 查询菜单
     *
     * @param id 菜单 ID
     *
     * @return 菜单实体，不存在则返回空
     */
    Optional<Menu> findById(Long id);

    /**
     * 查询所有菜单
     *
     * @return 菜单列表
     */
    List<Menu> findAll();

    /**
     * 根据父菜单 ID 查询子菜单
     *
     * @param parentId 父菜单 ID
     *
     * @return 子菜单列表
     */
    List<Menu> findByParentId(Long parentId);

    /**
     * 更新菜单
     *
     * @param menu 菜单实体
     *
     * @return 是否更新成功
     */
    boolean update(Menu menu);

    /**
     * 根据 ID 删除菜单（逻辑删除）
     *
     * @param id 菜单 ID
     *
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据用户 ID 查询用户有权限的菜单
     *
     * @param userId 用户 ID
     *
     * @return 菜单列表
     */
    List<Menu> findByUserId(Long userId);

    /**
     * 根据角色 ID 查询角色拥有的菜单
     *
     * @param roleId 角色 ID
     *
     * @return 菜单列表
     */
    List<Menu> findByRoleId(Long roleId);

    /**
     * 根据权限标识查询菜单
     *
     * @param permission 权限标识
     *
     * @return 菜单实体，不存在则返回空
     */
    Optional<Menu> findByPermission(String permission);

    /**
     * 查询菜单树形结构
     *
     * @return 菜单树列表
     */
    List<Menu> findMenuTree();

    /**
     * 检查菜单名称是否已存在（同一父菜单下）
     *
     * @param menuName 菜单名称
     * @param parentId 父菜单 ID
     *
     * @return 是否存在
     */
    boolean existsByMenuNameAndParentId(String menuName, Long parentId);

    /**
     * 检查菜单是否有子菜单
     *
     * @param id 菜单 ID
     *
     * @return 是否有子菜单
     */
    boolean hasChildren(Long id);

    /**
     * 统计菜单总数
     *
     * @return 菜单总数
     */
    long count();

}
