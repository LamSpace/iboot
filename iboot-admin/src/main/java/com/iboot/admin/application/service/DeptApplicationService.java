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
import com.iboot.admin.domain.system.model.Dept;
import com.iboot.admin.domain.system.repository.DeptRepository;
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
 * 部门应用服务
 * <p>
 * 负责部门的创建、更新、删除、查询以及组织架构图生成等业务逻辑处理， 支持部门树形结构构建和部门用户数量统计
 * </p>
 *
 * @author iBoot
 */
@Service
public class DeptApplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeptApplicationService.class);

    private final DeptRepository deptRepository;

    @SuppressWarnings("all")
    public DeptApplicationService(final DeptRepository deptRepository) {
        this.deptRepository = deptRepository;
    }

    /**
     * 创建部门
     * <p>
     * 校验部门编码唯一性、同级部门名称唯一性，设置默认状态为启用
     * </p>
     *
     * @param dept 部门实体
     *
     * @return 创建后的部门实体
     *
     * @throws BusinessException 当部门编码已存在或同级部门名称已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Dept createDept(Dept dept) {
        // 校验部门编码唯一性
        if (deptRepository.existsByDeptCode(dept.getDeptCode())) {
            throw new BusinessException("部门编码已存在");
        }
        // 校验同级部门名称唯一性
        if (deptRepository.existsByDeptNameAndParentId(dept.getDeptName(), dept.getParentId())) {
            throw new BusinessException("同级部门名称已存在");
        }
        dept.setCreateTime(LocalDateTime.now());
        dept.setStatus(1);
        return deptRepository.save(dept);
    }

    /**
     * 更新部门
     * <p>
     * 检查部门是否存在，不能将部门设置为自己的子部门， 检查是否形成循环引用
     * </p>
     *
     * @param dept 部门实体
     *
     * @return 是否更新成功
     *
     * @throws BusinessException 当部门不存在或上级部门设置不合法时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDept(Dept dept) {
        Dept existingDept = deptRepository.findById(dept.getId()).orElseThrow(() -> new BusinessException("部门不存在"));
        // 不能将部门设置为自己的子部门
        if (dept.getParentId() != null && dept.getParentId().equals(dept.getId())) {
            throw new BusinessException("上级部门不能是自己");
        }
        // 检查是否形成循环引用
        if (dept.getParentId() != null) {
            List<Long> childrenIds = deptRepository.findChildrenIds(dept.getId());
            if (childrenIds.contains(dept.getParentId())) {
                throw new BusinessException("上级部门不能是当前部门的下级");
            }
        }
        dept.setUpdateTime(LocalDateTime.now());
        return deptRepository.update(dept);
    }

    /**
     * 删除部门
     * <p>
     * 检查部门是否存在，存在下级部门时不允许删除
     * </p>
     *
     * @param id 部门 ID
     *
     * @return 是否删除成功
     *
     * @throws BusinessException 当部门不存在或存在下级部门时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDept(Long id) {
        if (!deptRepository.findById(id).isPresent()) {
            throw new BusinessException("部门不存在");
        }
        // 检查是否有子部门
        if (deptRepository.hasChildren(id)) {
            throw new BusinessException("存在下级部门，不允许删除");
        }
        return deptRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取部门
     *
     * @param id 部门 ID
     *
     * @return 部门实体
     *
     * @throws BusinessException 当部门不存在时抛出
     */
    public Dept getDeptById(Long id) {
        return deptRepository.findById(id).orElseThrow(() -> new BusinessException("部门不存在"));
    }

    /**
     * 获取部门树形结构
     * <p>
     * 将所有部门按照父子关系组织成树形结构
     * </p>
     *
     * @return 部门树形结构列表
     */
    public List<Dept> getDeptTree() {
        List<Dept> allDepts = deptRepository.findAll();
        return buildDeptTree(allDepts, 0L);
    }

    /**
     * 获取所有部门列表
     *
     * @return 部门列表
     */
    public List<Dept> getAllDepts() {
        return deptRepository.findAll();
    }

    /**
     * 统计部门总数
     *
     * @return 部门总数
     */
    public long countDepts() {
        return deptRepository.count();
    }

    /**
     * 获取部门及其所有子部门 ID
     *
     * @param deptId 部门 ID
     *
     * @return 部门及其所有子部门 ID 列表
     */
    public List<Long> getDeptAndChildrenIds(Long deptId) {
        return deptRepository.findChildrenIds(deptId);
    }

    /**
     * 获取每个部门的用户数量
     *
     * @return 部门 ID 到用户数量的映射
     */
    public Map<Long, Integer> getDeptUserCounts() {
        return deptRepository.getDeptUserCounts();
    }

    /**
     * 获取组织架构图数据
     * <p>
     * 返回部门树结构，包含每个部门的成员数量 使用 createBy 字段临时存储成员数量
     * </p>
     *
     * @return 组织架构图部门树
     */
    public List<Dept> getOrgChart() {
        List<Dept> allDepts = deptRepository.findAll();
        Map<Long, Integer> userCounts = deptRepository.getDeptUserCounts();
        // 为每个部门设置成员数量（通过扩展属性存储）
        for (Dept dept : allDepts) {
            Integer count = userCounts.getOrDefault(dept.getId(), 0);
            // 使用 remark 字段临时存储成员数量，稍后转换时使用
            dept.setCreateBy(count.toString());
        }
        return buildDeptTree(allDepts, 0L);
    }

    /**
     * 构建部门树形结构
     * <p>
     * 将所有部门按照父级 ID 分组，递归构建子部门树
     * </p>
     *
     * @param depts    所有部门列表
     * @param parentId 父级部门 ID
     *
     * @return 树形结构部门列表
     */
    private List<Dept> buildDeptTree(List<Dept> depts, Long parentId) {
        Map<Long, List<Dept>> groupedByParent = depts.stream()
                .collect(Collectors.groupingBy(dept -> dept.getParentId() == null ? 0L : dept.getParentId()));
        return buildTreeRecursive(groupedByParent, parentId);
    }

    /**
     * 递归构建部门树
     *
     * @param groupedByParent 按父级 ID 分组的部门
     * @param parentId        父级部门 ID
     *
     * @return 当前父级下的子部门树
     */
    private List<Dept> buildTreeRecursive(Map<Long, List<Dept>> groupedByParent, Long parentId) {
        List<Dept> children = groupedByParent.getOrDefault(parentId, new ArrayList<>());
        for (Dept child : children) {
            child.setChildren(buildTreeRecursive(groupedByParent, child.getId()));
        }
        return children;
    }

}
