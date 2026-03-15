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

import com.iboot.admin.domain.system.model.DictType;

import java.util.List;
import java.util.Optional;

/**
 * 字典类型仓储接口
 * <p>
 * 负责字典类型数据的持久化操作，提供完整的增删改查功能
 * </p>
 *
 * @author iBoot
 */
public interface DictTypeRepository {

    /**
     * 保存字典类型
     *
     * @param dictType 字典类型实体对象
     * @return 保存后的字典类型对象
     */
    DictType save(DictType dictType);

    /**
     * 更新字典类型
     *
     * @param dictType 字典类型实体对象
     * @return 是否更新成功
     */
    boolean update(DictType dictType);

    /**
     * 根据 ID 删除字典类型（逻辑删除）
     *
     * @param id 字典类型 ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询字典类型
     *
     * @param id 字典类型 ID
     * @return 字典类型实体，不存在则返回空
     */
    Optional<DictType> findById(Long id);

    /**
     * 根据字典类型查询
     *
     * @param dictType 字典类型标识
     * @return 字典类型实体，不存在则返回空
     */
    Optional<DictType> findByDictType(String dictType);

    /**
     * 查询所有字典类型
     *
     * @return 字典类型列表
     */
    List<DictType> findAll();

    /**
     * 分页查询字典类型
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 字典类型列表
     */
    List<DictType> findPage(int pageNum, int pageSize);

    /**
     * 统计字典类型总数
     *
     * @return 字典类型总数
     */
    long count();

    /**
     * 检查字典类型是否存在
     *
     * @param dictType 字典类型标识
     * @return 是否存在
     */
    boolean existsByDictType(String dictType);

    /**
     * 物理删除已逻辑删除的字典类型记录
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param dictType 字典类型标识
     * @return 是否删除成功
     */
    boolean removeDeletedByDictType(String dictType);
}
