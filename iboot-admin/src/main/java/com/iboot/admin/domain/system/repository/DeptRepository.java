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

import com.iboot.admin.domain.system.model.Dept;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 部门仓储接口
 * <p>
 * 负责部门数据的持久化操作，提供部门树形结构管理和用户统计功能
 * </p>
 *
 * @author iBoot
 */
public interface DeptRepository {

    /**
     * 保存部门
     *
     * @param dept 部门实体对象
     *
     * @return 保存后的部门对象
     */
    Dept save(Dept dept);

    /**
     * 更新部门
     *
     * @param dept 部门实体对象
     *
     * @return 是否更新成功
     */
    boolean update(Dept dept);

    /**
     * 根据 ID 删除部门（逻辑删除）
     *
     * @param id 部门 ID
     *
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询部门
     *
     * @param id 部门 ID
     *
     * @return 部门实体，不存在则返回空
     */
    Optional<Dept> findById(Long id);

    /**
     * 查询所有部门
     *
     * @return 部门列表
     */
    List<Dept> findAll();

    /**
     * 根据父 ID 查询子部门列表
     *
     * @param parentId 父部门 ID
     *
     * @return 子部门列表
     */
    List<Dept> findByParentId(Long parentId);

    /**
     * 查询部门及其所有子部门 ID（递归）
     * <p>
     * 用于级联删除或统计时获取完整的部门树
     * </p>
     *
     * @param deptId 部门 ID
     *
     * @return 部门 ID 列表（包含自身和所有子部门）
     */
    List<Long> findChildrenIds(Long deptId);

    /**
     * 统计部门总数
     *
     * @return 部门总数
     */
    long count();

    /**
     * 检查部门编码是否存在
     *
     * @param deptCode 部门编码
     *
     * @return 是否存在
     */
    boolean existsByDeptCode(String deptCode);

    /**
     * 检查部门名称在同级是否存在
     * <p>
     * 同一父部门下不能有重名的子部门
     * </p>
     *
     * @param deptName 部门名称
     * @param parentId 父部门 ID
     *
     * @return 是否存在
     */
    boolean existsByDeptNameAndParentId(String deptName, Long parentId);

    /**
     * 检查是否有子部门
     *
     * @param deptId 部门 ID
     *
     * @return 是否有子部门
     */
    boolean hasChildren(Long deptId);

    /**
     * 获取每个部门的用户数量
     *
     * @return 部门 ID 到用户数量的映射
     */
    Map<Long, Integer> getDeptUserCounts();

}
