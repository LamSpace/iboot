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

import com.iboot.admin.domain.system.model.DictData;

import java.util.List;
import java.util.Optional;

/**
 * 字典数据仓储接口
 *
 * @author iBoot
 */
public interface DictDataRepository {

    /**
     * 保存字典数据
     *
     * @param dictData 字典数据实体对象
     *
     * @return 保存后的字典数据对象
     */
    DictData save(DictData dictData);

    /**
     * 更新字典数据
     *
     * @param dictData 字典数据实体对象
     *
     * @return 是否更新成功
     */
    boolean update(DictData dictData);

    /**
     * 根据ID删除字典数据（逻辑删除）
     */
    boolean deleteById(Long id);

    /**
     * 根据ID查询字典数据
     */
    Optional<DictData> findById(Long id);

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     *
     * @return 字典数据列表
     */
    List<DictData> findByDictType(String dictType);

    /**
     * 根据字典类型和键值查询
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     *
     * @return 字典数据实体，不存在则返回空
     */
    Optional<DictData> findByDictTypeAndValue(String dictType, String dictValue);

    /**
     * 查询所有字典数据
     *
     * @return 字典数据列表
     */
    List<DictData> findAll();

    /**
     * 统计字典数据总数
     *
     * @return 字典数据总数
     */
    long count();

    /**
     * 检查字典数据是否存在
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     *
     * @return 是否存在
     */
    boolean existsByDictTypeAndValue(String dictType, String dictValue);

    /**
     * 根据字典类型删除所有字典数据
     *
     * @param dictType 字典类型
     *
     * @return 是否删除成功
     */
    boolean deleteByDictType(String dictType);

}
