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

import com.iboot.admin.domain.system.model.DictType;
import com.iboot.admin.domain.system.repository.DictTypeRepository;
import com.iboot.admin.infrastructure.persistence.mapper.DictTypeMapper;
import com.iboot.admin.infrastructure.persistence.po.DictTypePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 字典类型仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现字典类型的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class DictTypeRepositoryImpl implements DictTypeRepository {

    private final DictTypeMapper dictTypeMapper;

    /**
     * 保存字典类型
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param dictType 字典类型实体对象
     * @return 保存后的字典类型对象
     */
    @Override
    public DictType save(DictType dictType) {
        DictTypePO dictTypePO = convertToPO(dictType);
        if (dictType.getId() == null) {
            dictTypeMapper.insert(dictTypePO);
        } else {
            dictTypeMapper.update(dictTypePO);
        }
        return convertToDomain(dictTypePO);
    }

    /**
     * 更新字典类型
     *
     * @param dictType 字典类型实体对象
     * @return 是否更新成功
     */
    @Override
    public boolean update(DictType dictType) {
        DictTypePO dictTypePO = convertToPO(dictType);
        return dictTypeMapper.update(dictTypePO) > 0;
    }

    /**
     * 根据 ID 删除字典类型（逻辑删除）
     *
     * @param id 字典类型 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return dictTypeMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询字典类型
     *
     * @param id 字典类型 ID
     * @return 字典类型实体，不存在则返回空
     */
    @Override
    public Optional<DictType> findById(Long id) {
        DictTypePO dictTypePO = dictTypeMapper.selectById(id);
        return Optional.ofNullable(dictTypePO).map(this::convertToDomain);
    }

    /**
     * 根据字典类型查询
     *
     * @param dictType 字典类型标识
     * @return 字典类型实体，不存在则返回空
     */
    @Override
    public Optional<DictType> findByDictType(String dictType) {
        DictTypePO dictTypePO = dictTypeMapper.selectByDictType(dictType);
        return Optional.ofNullable(dictTypePO).map(this::convertToDomain);
    }

    /**
     * 查询所有字典类型
     *
     * @return 字典类型列表
     */
    @Override
    public List<DictType> findAll() {
        return dictTypeMapper.selectAll().stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询字典类型
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 字典类型列表
     */
    @Override
    public List<DictType> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return dictTypeMapper.selectPage(offset, pageSize).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计字典类型总数
     *
     * @return 字典类型总数
     */
    @Override
    public long count() {
        return dictTypeMapper.count();
    }

    /**
     * 检查字典类型是否存在
     *
     * @param dictType 字典类型标识
     * @return 是否存在
     */
    @Override
    public boolean existsByDictType(String dictType) {
        return dictTypeMapper.countByDictType(dictType) > 0;
    }

    /**
     * 物理删除已逻辑删除的字典类型记录
     *
     * @param dictType 字典类型标识
     * @return 是否删除成功
     */
    @Override
    public boolean removeDeletedByDictType(String dictType) {
        return dictTypeMapper.removeDeletedByDictType(dictType) > 0;
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param dictType 字典类型领域对象
     * @return 字典类型持久化对象
     */
    private DictTypePO convertToPO(DictType dictType) {
        DictTypePO po = new DictTypePO();
        po.setId(dictType.getId());
        po.setDictName(dictType.getDictName());
        po.setDictType(dictType.getDictType());
        po.setStatus(dictType.getStatus());
        po.setCreateBy(dictType.getCreateBy());
        po.setCreateTime(dictType.getCreateTime());
        po.setUpdateBy(dictType.getUpdateBy());
        po.setUpdateTime(dictType.getUpdateTime());
        po.setDeleted(dictType.getDeleted() != null ? dictType.getDeleted() : 0);
        po.setRemark(dictType.getRemark());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 字典类型持久化对象
     * @return 字典类型领域对象
     */
    private DictType convertToDomain(DictTypePO po) {
        return DictType.builder()
                .id(po.getId())
                .dictName(po.getDictName())
                .dictType(po.getDictType())
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
