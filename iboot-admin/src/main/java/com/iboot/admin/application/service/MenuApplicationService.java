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

import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.Menu;
import com.iboot.admin.domain.system.repository.MenuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单应用服务
 * <p>
 * 负责菜单的创建、更新、删除、查询等业务逻辑处理， 支持菜单树形结构构建和用户权限菜单过滤
 * </p>
 *
 * @author iBoot
 */
@Service
public class MenuApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MenuApplicationService.class);

    private final MenuRepository menuRepository;

    @SuppressWarnings("all")
    public MenuApplicationService(final MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /**
     * 创建菜单
     * <p>
     * 校验同级菜单名称唯一性，设置默认状态为启用
     * </p>
     *
     * @param menu 菜单实体
     *
     * @return 创建后的菜单实体
     *
     * @throws BusinessException 当同级菜单名称已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Menu createMenu(Menu menu) {
        // 校验同级菜单名称唯一性
        if (menuRepository.existsByMenuNameAndParentId(menu.getMenuName(), menu.getParentId())) {
            throw new BusinessException("同级菜单名称已存在");
        }
        menu.setCreateTime(LocalDateTime.now());
        menu.setStatus(1);
        return menuRepository.save(menu);
    }

    /**
     * 更新菜单
     * <p>
     * 检查菜单是否存在，不能将菜单设置为自己的子菜单
     * </p>
     *
     * @param menu 菜单实体
     *
     * @return 是否更新成功
     *
     * @throws BusinessException 当菜单不存在或上级菜单设置不合法时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(Menu menu) {
        Menu existingMenu = menuRepository.findById(menu.getId()).orElseThrow(() -> new BusinessException("菜单不存在"));
        // 不能将菜单设置为自己的子菜单
        if (menu.getParentId() != null && menu.getParentId().equals(menu.getId())) {
            throw new BusinessException("上级菜单不能是自己");
        }
        menu.setUpdateTime(LocalDateTime.now());
        return menuRepository.update(menu);
    }

    /**
     * 删除菜单
     * <p>
     * 检查菜单是否存在，存在子菜单时不允许删除
     * </p>
     *
     * @param id 菜单 ID
     *
     * @return 是否删除成功
     *
     * @throws BusinessException 当菜单不存在或存在子菜单时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long id) {
        if (!menuRepository.findById(id).isPresent()) {
            throw new BusinessException("菜单不存在");
        }
        // 检查是否有子菜单
        if (menuRepository.hasChildren(id)) {
            throw new BusinessException("存在子菜单，不允许删除");
        }
        return menuRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取菜单
     *
     * @param id 菜单 ID
     *
     * @return 菜单实体
     *
     * @throws BusinessException 当菜单不存在时抛出
     */
    public Menu getMenuById(Long id) {
        return menuRepository.findById(id).orElseThrow(() -> new BusinessException("菜单不存在"));
    }

    /**
     * 获取菜单树形结构
     * <p>
     * 将所有菜单按照父子关系组织成树形结构
     * </p>
     *
     * @return 菜单树形结构列表
     */
    public List<Menu> getMenuTree() {
        List<Menu> allMenus = menuRepository.findAll();
        return buildMenuTree(allMenus, 0L);
    }

    /**
     * 根据用户 ID 获取菜单树（用于前端路由）
     * <p>
     * 获取用户有权限的菜单，过滤按钮类型，只返回目录和菜单
     * </p>
     *
     * @param userId 用户 ID
     *
     * @return 用户可访问的菜单树形结构列表
     */
    public List<Menu> getUserMenuTree(Long userId) {
        List<Menu> userMenus = menuRepository.findByUserId(userId);
        // 过滤按钮类型，只返回目录和菜单
        List<Menu> routeMenus = userMenus.stream()
                .filter(menu -> !"F".equals(menu.getMenuType()))
                .collect(Collectors.toList());
        return buildMenuTree(routeMenus, 0L);
    }

    /**
     * 根据用户 ID 获取权限标识列表
     * <p>
     * 用于 Spring Security 的权限校验
     * </p>
     *
     * @param userId 用户 ID
     *
     * @return 权限标识列表
     */
    public List<String> getUserPermissions(Long userId) {
        List<Menu> userMenus = menuRepository.findByUserId(userId);
        return userMenus.stream()
                .filter(menu -> menu.getPermission() != null && !menu.getPermission().isEmpty())
                .map(Menu::getPermission)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有菜单列表
     *
     * @return 菜单列表
     */
    public List<Menu> getAllMenus() {
        return menuRepository.findAll();
    }

    /**
     * 统计菜单总数
     *
     * @return 菜单总数
     */
    public long countMenus() {
        return menuRepository.count();
    }

    /**
     * 构建菜单树形结构
     * <p>
     * 将所有菜单按照父级 ID 分组，递归构建子菜单树
     * </p>
     *
     * @param menus    所有菜单列表
     * @param parentId 父级菜单 ID
     *
     * @return 树形结构菜单列表
     */
    private List<Menu> buildMenuTree(List<Menu> menus, Long parentId) {
        Map<Long, List<Menu>> groupedByParent = menus.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        return buildTreeRecursive(groupedByParent, parentId);
    }

    /**
     * 递归构建菜单树
     *
     * @param groupedByParent 按父级 ID 分组的菜单
     * @param parentId        父级菜单 ID
     *
     * @return 当前父级下的子菜单树
     */
    private List<Menu> buildTreeRecursive(Map<Long, List<Menu>> groupedByParent, Long parentId) {
        List<Menu> children = groupedByParent.getOrDefault(parentId, new ArrayList<>());
        for (Menu child : children) {
            child.setChildren(buildTreeRecursive(groupedByParent, child.getId()));
        }
        return children;
    }

}
