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

import com.iboot.admin.infrastructure.persistence.po.DictDataPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据 Mapper 接口
 * <p>
 * 负责字典数据的数据库操作，提供增删改查方法
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface DictDataMapper {

    /**
     * 插入字典数据记录
     *
     * @param dictDataPO 字典数据持久化对象
     *
     * @return 影响的记录数
     */
    int insert(DictDataPO dictDataPO);

    /**
     * 更新字典数据记录
     *
     * @param dictDataPO 字典数据持久化对象
     *
     * @return 影响的记录数
     */
    int update(DictDataPO dictDataPO);

    /**
     * 根据 ID 删除字典数据
     *
     * @param id 字典数据 ID
     *
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询字典数据
     *
     * @param id 字典数据 ID
     *
     * @return 字典数据持久化对象
     */
    DictDataPO selectById(@Param("id") Long id);

    /**
     * 根据字典类型查询字典数据列表
     *
     * @param dictType 字典类型
     *
     * @return 字典数据列表
     */
    List<DictDataPO> selectByDictType(@Param("dictType") String dictType);

    /**
     * 根据字典类型和值查询字典数据
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     *
     * @return 字典数据持久化对象
     */
    DictDataPO selectByDictTypeAndValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue);

    /**
     * 查询所有字典数据
     *
     * @return 字典数据列表
     */
    List<DictDataPO> selectAll();

    /**
     * 统计字典数据总数
     *
     * @return 总数
     */
    long count();

    /**
     * 根据字典类型和值统计数量
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     *
     * @return 数量
     */
    int countByDictTypeAndValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue);

    /**
     * 根据字典类型删除字典数据
     *
     * @param dictType 字典类型
     *
     * @return 影响的记录数
     */
    int deleteByDictType(@Param("dictType") String dictType);

}
