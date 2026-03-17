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

import com.iboot.admin.domain.system.model.Dept;
import com.iboot.admin.domain.system.repository.DeptRepository;
import com.iboot.admin.infrastructure.persistence.mapper.DeptMapper;
import com.iboot.admin.infrastructure.persistence.po.DeptPO;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 部门仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现部门的持久化操作，支持部门树形结构管理
 * </p>
 *
 * @author iBoot
 */
@Repository
public class DeptRepositoryImpl implements DeptRepository {

    private final DeptMapper deptMapper;

    @SuppressWarnings("all")
    public DeptRepositoryImpl(final DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    /**
     * 保存部门
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param dept 部门实体对象
     *
     * @return 保存后的部门对象
     */
    @Override
    public Dept save(Dept dept) {
        DeptPO deptPO = convertToPO(dept);
        if (dept.getId() == null) {
            deptMapper.insert(deptPO);
        } else {
            deptMapper.update(deptPO);
        }
        return convertToDomain(deptPO);
    }

    /**
     * 更新部门
     *
     * @param dept 部门实体对象
     *
     * @return 是否更新成功
     */
    @Override
    public boolean update(Dept dept) {
        DeptPO deptPO = convertToPO(dept);
        return deptMapper.update(deptPO) > 0;
    }

    /**
     * 根据 ID 删除部门（逻辑删除）
     *
     * @param id 部门 ID
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return deptMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询部门
     *
     * @param id 部门 ID
     *
     * @return 部门实体，不存在则返回空
     */
    @Override
    public Optional<Dept> findById(Long id) {
        DeptPO deptPO = deptMapper.selectById(id);
        return Optional.ofNullable(deptPO).map(this::convertToDomain);
    }

    /**
     * 查询所有部门
     *
     * @return 部门列表
     */
    @Override
    public List<Dept> findAll() {
        return deptMapper.selectAll().stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 根据父 ID 查询子部门列表
     *
     * @param parentId 父部门 ID
     *
     * @return 子部门列表
     */
    @Override
    public List<Dept> findByParentId(Long parentId) {
        return deptMapper.selectByParentId(parentId).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 查询部门及其所有子部门 ID（递归）
     *
     * @param deptId 部门 ID
     *
     * @return 部门 ID 列表（包含自身和所有子部门）
     */
    @Override
    public List<Long> findChildrenIds(Long deptId) {
        return deptMapper.selectChildrenIds(deptId);
    }

    /**
     * 统计部门总数
     *
     * @return 部门总数
     */
    @Override
    public long count() {
        return deptMapper.count();
    }

    /**
     * 检查部门编码是否存在
     *
     * @param deptCode 部门编码
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByDeptCode(String deptCode) {
        return deptMapper.countByDeptCode(deptCode) > 0;
    }

    /**
     * 检查部门名称在同级是否存在
     *
     * @param deptName 部门名称
     * @param parentId 父部门 ID
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByDeptNameAndParentId(String deptName, Long parentId) {
        return deptMapper.countByDeptNameAndParentId(deptName, parentId) > 0;
    }

    /**
     * 检查是否有子部门
     *
     * @param deptId 部门 ID
     *
     * @return 是否有子部门
     */
    @Override
    public boolean hasChildren(Long deptId) {
        return deptMapper.countChildren(deptId) > 0;
    }

    /**
     * 获取每个部门的用户数量
     *
     * @return 部门 ID 到用户数量的映射
     */
    @Override
    public Map<Long, Integer> getDeptUserCounts() {
        List<DeptMapper.DeptUserCountDTO> counts = deptMapper.selectDeptUserCounts();
        Map<Long, Integer> result = new HashMap<>();
        for (DeptMapper.DeptUserCountDTO dto : counts) {
            result.put(dto.getDeptId(), dto.getUserCount());
        }
        return result;
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param dept 部门领域对象
     *
     * @return 部门持久化对象
     */
    private DeptPO convertToPO(Dept dept) {
        DeptPO deptPO = new DeptPO();
        deptPO.setId(dept.getId());
        deptPO.setParentId(dept.getParentId());
        deptPO.setDeptName(dept.getDeptName());
        deptPO.setDeptCode(dept.getDeptCode());
        deptPO.setOrderNum(dept.getOrderNum());
        deptPO.setLeader(dept.getLeader());
        deptPO.setPhone(dept.getPhone());
        deptPO.setEmail(dept.getEmail());
        deptPO.setStatus(dept.getStatus());
        deptPO.setCreateBy(dept.getCreateBy());
        deptPO.setCreateTime(dept.getCreateTime());
        deptPO.setUpdateBy(dept.getUpdateBy());
        deptPO.setUpdateTime(dept.getUpdateTime());
        deptPO.setDeleted(dept.getDeleted() != null ? dept.getDeleted() : 0);
        return deptPO;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param deptPO 部门持久化对象
     *
     * @return 部门领域对象
     */
    private Dept convertToDomain(DeptPO deptPO) {
        return Dept.builder()
                .id(deptPO.getId())
                .parentId(deptPO.getParentId())
                .deptName(deptPO.getDeptName())
                .deptCode(deptPO.getDeptCode())
                .orderNum(deptPO.getOrderNum())
                .leader(deptPO.getLeader())
                .phone(deptPO.getPhone())
                .email(deptPO.getEmail())
                .status(deptPO.getStatus())
                .createBy(deptPO.getCreateBy())
                .createTime(deptPO.getCreateTime())
                .updateBy(deptPO.getUpdateBy())
                .updateTime(deptPO.getUpdateTime())
                .deleted(deptPO.getDeleted())
                .build();
    }

}
