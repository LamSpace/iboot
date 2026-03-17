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

import com.iboot.admin.infrastructure.persistence.po.MenuPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper 接口
 * <p>
 * 负责菜单数据的数据库操作，支持菜单树形结构和权限关联
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface MenuMapper {

    /**
     * 插入菜单记录
     *
     * @param menu 菜单持久化对象
     *
     * @return 影响的记录数
     */
    int insert(MenuPO menu);

    /**
     * 更新菜单记录
     *
     * @param menu 菜单持久化对象
     *
     * @return 影响的记录数
     */
    int update(MenuPO menu);

    /**
     * 根据 ID 删除菜单记录
     *
     * @param id 菜单 ID
     *
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询菜单记录
     *
     * @param id 菜单 ID
     *
     * @return 菜单持久化对象
     */
    MenuPO selectById(@Param("id") Long id);

    /**
     * 根据权限标识查询菜单记录
     *
     * @param permission 权限标识
     *
     * @return 菜单持久化对象
     */
    MenuPO selectByPermission(@Param("permission") String permission);

    /**
     * 查询所有菜单记录
     *
     * @return 菜单持久化对象列表
     */
    List<MenuPO> selectAll();

    /**
     * 根据父菜单 ID 查询子菜单列表
     *
     * @param parentId 父菜单 ID
     *
     * @return 菜单持久化对象列表
     */
    List<MenuPO> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据用户 ID 查询菜单列表
     *
     * @param userId 用户 ID
     *
     * @return 菜单持久化对象列表
     */
    List<MenuPO> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据角色 ID 查询菜单列表
     *
     * @param roleId 角色 ID
     *
     * @return 菜单持久化对象列表
     */
    List<MenuPO> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 检查同级菜单名称是否存在
     *
     * @param menuName 菜单名称
     * @param parentId 父菜单 ID
     *
     * @return 存在返回 1，否则返回 0
     */
    int existsByMenuNameAndParentId(@Param("menuName") String menuName, @Param("parentId") Long parentId);

    /**
     * 统计子菜单数量
     *
     * @param parentId 父菜单 ID
     *
     * @return 子菜单数量
     */
    long countByParentId(@Param("parentId") Long parentId);

    /**
     * 统计菜单总数
     *
     * @return 菜单总数
     */
    long count();

}
