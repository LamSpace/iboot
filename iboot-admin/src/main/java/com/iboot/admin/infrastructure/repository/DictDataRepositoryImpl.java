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

import com.iboot.admin.domain.system.model.DictData;
import com.iboot.admin.domain.system.repository.DictDataRepository;
import com.iboot.admin.infrastructure.persistence.mapper.DictDataMapper;
import com.iboot.admin.infrastructure.persistence.po.DictDataPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 字典数据仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现字典数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class DictDataRepositoryImpl implements DictDataRepository {

    private final DictDataMapper dictDataMapper;

    /**
     * 保存字典数据
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param dictData 字典数据实体对象
     * @return 保存后的字典数据对象
     */
    @Override
    public DictData save(DictData dictData) {
        DictDataPO po = convertToPO(dictData);
        if (dictData.getId() == null) {
            dictDataMapper.insert(po);
        } else {
            dictDataMapper.update(po);
        }
        return convertToDomain(po);
    }

    /**
     * 更新字典数据
     *
     * @param dictData 字典数据实体对象
     * @return 是否更新成功
     */
    @Override
    public boolean update(DictData dictData) {
        DictDataPO po = convertToPO(dictData);
        return dictDataMapper.update(po) > 0;
    }

    /**
     * 根据 ID 删除字典数据（逻辑删除）
     *
     * @param id 字典数据 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return dictDataMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询字典数据
     *
     * @param id 字典数据 ID
     * @return 字典数据实体，不存在则返回空
     */
    @Override
    public Optional<DictData> findById(Long id) {
        DictDataPO po = dictDataMapper.selectById(id);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    @Override
    public List<DictData> findByDictType(String dictType) {
        return dictDataMapper.selectByDictType(dictType).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 根据字典类型和值查询字典数据
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典数据实体，不存在则返回空
     */
    @Override
    public Optional<DictData> findByDictTypeAndValue(String dictType, String dictValue) {
        DictDataPO po = dictDataMapper.selectByDictTypeAndValue(dictType, dictValue);
        return Optional.ofNullable(po).map(this::convertToDomain);
    }

    /**
     * 查询所有字典数据
     *
     * @return 字典数据列表
     */
    @Override
    public List<DictData> findAll() {
        return dictDataMapper.selectAll().stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计字典数据总数
     *
     * @return 字典数据总数
     */
    @Override
    public long count() {
        return dictDataMapper.count();
    }

    /**
     * 检查字典数据是否存在
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 是否存在
     */
    @Override
    public boolean existsByDictTypeAndValue(String dictType, String dictValue) {
        return dictDataMapper.countByDictTypeAndValue(dictType, dictValue) > 0;
    }

    /**
     * 根据字典类型删除所有字典数据
     *
     * @param dictType 字典类型
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByDictType(String dictType) {
        return dictDataMapper.deleteByDictType(dictType) > 0;
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param dictData 字典数据领域对象
     * @return 字典数据持久化对象
     */
    private DictDataPO convertToPO(DictData dictData) {
        DictDataPO po = new DictDataPO();
        po.setId(dictData.getId());
        po.setDictType(dictData.getDictType());
        po.setDictLabel(dictData.getDictLabel());
        po.setDictValue(dictData.getDictValue());
        po.setDictSort(dictData.getDictSort());
        po.setCssClass(dictData.getCssClass());
        po.setListClass(dictData.getListClass());
        po.setIsDefault(dictData.getIsDefault());
        po.setStatus(dictData.getStatus());
        po.setCreateBy(dictData.getCreateBy());
        po.setCreateTime(dictData.getCreateTime());
        po.setUpdateBy(dictData.getUpdateBy());
        po.setUpdateTime(dictData.getUpdateTime());
        po.setDeleted(dictData.getDeleted() != null ? dictData.getDeleted() : 0);
        po.setRemark(dictData.getRemark());
        return po;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param po 字典数据持久化对象
     * @return 字典数据领域对象
     */
    private DictData convertToDomain(DictDataPO po) {
        return DictData.builder()
                .id(po.getId())
                .dictType(po.getDictType())
                .dictLabel(po.getDictLabel())
                .dictValue(po.getDictValue())
                .dictSort(po.getDictSort())
                .cssClass(po.getCssClass())
                .listClass(po.getListClass())
                .isDefault(po.getIsDefault())
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
