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

import com.iboot.admin.common.annotation.DataScope;
import com.iboot.admin.infrastructure.persistence.po.DictTypePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典类型 Mapper 接口
 * <p>
 * 负责字典类型的数据库操作，提供增删改查方法
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface DictTypeMapper {

    /**
     * 插入字典类型记录
     *
     * @param dictTypePO 字典类型持久化对象
     * @return 影响的记录数
     */
    int insert(DictTypePO dictTypePO);

    /**
     * 更新字典类型记录
     *
     * @param dictTypePO 字典类型持久化对象
     * @return 影响的记录数
     */
    int update(DictTypePO dictTypePO);

    /**
     * 根据 ID 删除字典类型
     *
     * @param id 字典类型 ID
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询字典类型
     *
     * @param id 字典类型 ID
     * @return 字典类型持久化对象
     */
    DictTypePO selectById(@Param("id") Long id);

    /**
     * 根据字典类型查询
     *
     * @param dictType 字典类型
     * @return 字典类型持久化对象
     */
    DictTypePO selectByDictType(@Param("dictType") String dictType);

    /**
     * 查询所有字典类型
     *
     * @return 字典类型列表
     */
    List<DictTypePO> selectAll();

    /**
     * 分页查询字典类型
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 字典类型列表
     */
    List<DictTypePO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计字典类型总数
     *
     * @return 总数
     */
    long count();

    /**
     * 根据字典类型统计数量
     *
     * @param dictType 字典类型
     * @return 数量
     */
    int countByDictType(@Param("dictType") String dictType);

    /**
     * 物理删除已逻辑删除的字典类型记录
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param dictType 字典类型
     * @return 影响的记录数
     */
    int removeDeletedByDictType(@Param("dictType") String dictType);
}
