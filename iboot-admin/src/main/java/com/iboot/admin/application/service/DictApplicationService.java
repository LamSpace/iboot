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
import com.iboot.admin.domain.system.model.DictData;
import com.iboot.admin.domain.system.model.DictType;
import com.iboot.admin.domain.system.repository.DictDataRepository;
import com.iboot.admin.domain.system.repository.DictTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典应用服务
 * <p>
 * 提供字典类型和字典数据的增删改查功能，支持字典标签查询
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictApplicationService {

    private final DictTypeRepository dictTypeRepository;
    private final DictDataRepository dictDataRepository;

    // ==================== 字典类型操作 ====================

    /**
     * 创建字典类型
     * <p>
     * 检查字典类型是否已存在，清理同类型已删除的记录，设置默认状态为启用
     * </p>
     *
     * @param dictType 字典类型实体
     * @return 创建后的字典类型
     * @throws BusinessException 当字典类型已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public DictType createDictType(DictType dictType) {
        if (dictTypeRepository.existsByDictType(dictType.getDictType())) {
            throw new BusinessException("字典类型已存在");
        }

        // 清理同类型已删除字典记录（解决逻辑删除与唯一索引冲突问题）
        dictTypeRepository.removeDeletedByDictType(dictType.getDictType());

        dictType.setCreateTime(LocalDateTime.now());
        dictType.setStatus(1);
        return dictTypeRepository.save(dictType);
    }

    /**
     * 更新字典类型
     * <p>
     * 检查字典类型是否存在，如果修改了字典类型需要同步更新字典数据
     * </p>
     *
     * @param dictType 字典类型实体
     * @return 是否更新成功
     * @throws BusinessException 当字典类型不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictType(DictType dictType) {
        DictType existing = dictTypeRepository.findById(dictType.getId())
                .orElseThrow(() -> new BusinessException("字典类型不存在"));

        // 如果修改了字典类型，需要同步更新字典数据
        if (!existing.getDictType().equals(dictType.getDictType())) {
            // 这里可以添加更新字典数据的逻辑
        }

        dictType.setUpdateTime(LocalDateTime.now());
        return dictTypeRepository.update(dictType);
    }

    /**
     * 删除字典类型
     * <p>
     * 检查字典类型是否存在，删除关联的字典数据，然后删除字典类型
     * </p>
     *
     * @param id 字典类型 ID
     * @return 是否删除成功
     * @throws BusinessException 当字典类型不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictType(Long id) {
        DictType dictType = dictTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("字典类型不存在"));

        // 删除关联的字典数据
        dictDataRepository.deleteByDictType(dictType.getDictType());

        return dictTypeRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取字典类型
     *
     * @param id 字典类型 ID
     * @return 字典类型实体
     * @throws BusinessException 当字典类型不存在时抛出
     */
    public DictType getDictTypeById(Long id) {
        return dictTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("字典类型不存在"));
    }

    /**
     * 获取所有字典类型
     *
     * @return 字典类型列表
     */
    public List<DictType> getAllDictTypes() {
        return dictTypeRepository.findAll();
    }

    /**
     * 分页获取字典类型
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 字典类型列表
     */
    public List<DictType> getDictTypePage(int pageNum, int pageSize) {
        return dictTypeRepository.findPage(pageNum, pageSize);
    }

    /**
     * 统计字典类型总数
     *
     * @return 字典类型总数
     */
    public long countDictTypes() {
        return dictTypeRepository.count();
    }

    // ==================== 字典数据操作 ====================

    /**
     * 创建字典数据
     * <p>
     * 检查字典数据是否已存在，设置默认状态为启用
     * </p>
     *
     * @param dictData 字典数据实体
     * @return 创建后的字典数据
     * @throws BusinessException 当字典数据已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public DictData createDictData(DictData dictData) {
        if (dictDataRepository.existsByDictTypeAndValue(dictData.getDictType(), dictData.getDictValue())) {
            throw new BusinessException("字典数据已存在");
        }

        dictData.setCreateTime(LocalDateTime.now());
        dictData.setStatus(1);
        return dictDataRepository.save(dictData);
    }

    /**
     * 更新字典数据
     * <p>
     * 检查字典数据是否存在，更新字典数据信息
     * </p>
     *
     * @param dictData 字典数据实体
     * @return 是否更新成功
     * @throws BusinessException 当字典数据不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDictData(DictData dictData) {
        dictDataRepository.findById(dictData.getId())
                .orElseThrow(() -> new BusinessException("字典数据不存在"));

        dictData.setUpdateTime(LocalDateTime.now());
        return dictDataRepository.update(dictData);
    }

    /**
     * 删除字典数据
     * <p>
     * 检查字典数据是否存在，执行逻辑删除
     * </p>
     *
     * @param id 字典数据 ID
     * @return 是否删除成功
     * @throws BusinessException 当字典数据不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDictData(Long id) {
        if (!dictDataRepository.findById(id).isPresent()) {
            throw new BusinessException("字典数据不存在");
        }

        return dictDataRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取字典数据
     *
     * @param id 字典数据 ID
     * @return 字典数据实体
     * @throws BusinessException 当字典数据不存在时抛出
     */
    public DictData getDictDataById(Long id) {
        return dictDataRepository.findById(id)
                .orElseThrow(() -> new BusinessException("字典数据不存在"));
    }

    /**
     * 根据字典类型获取字典数据列表
     *
     * @param dictType 字典类型
     * @return 字典数据列表
     */
    public List<DictData> getDictDataByType(String dictType) {
        return dictDataRepository.findByDictType(dictType);
    }

    /**
     * 获取所有字典数据（不分页，用于导出）
     *
     * @return 字典数据列表
     */
    public List<DictData> getAllDictData() {
        return dictDataRepository.findAll();
    }

    /**
     * 根据字典类型和键值获取字典标签
     * <p>
     * 用于前端显示时将字典值转换为可读的标签文本
     * </p>
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签，不存在则返回空字符串
     */
    public String getDictLabel(String dictType, String dictValue) {
        return dictDataRepository.findByDictTypeAndValue(dictType, dictValue)
                .map(DictData::getDictLabel)
                .orElse("");
    }
}
