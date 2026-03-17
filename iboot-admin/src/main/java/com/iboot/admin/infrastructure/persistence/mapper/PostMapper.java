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
import com.iboot.admin.infrastructure.persistence.po.PostPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位 Mapper 接口
 * <p>
 * 负责岗位数据的数据库操作，支持岗位的管理和查询
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface PostMapper {

    /**
     * 插入岗位记录
     *
     * @param postPO 岗位持久化对象
     *
     * @return 影响的记录数
     */
    int insert(PostPO postPO);

    /**
     * 更新岗位记录
     *
     * @param postPO 岗位持久化对象
     *
     * @return 影响的记录数
     */
    int update(PostPO postPO);

    /**
     * 根据 ID 删除岗位
     *
     * @param id 岗位 ID
     *
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询岗位
     *
     * @param id 岗位 ID
     *
     * @return 岗位持久化对象
     */
    PostPO selectById(@Param("id") Long id);

    /**
     * 根据岗位编码查询岗位
     *
     * @param postCode 岗位编码
     *
     * @return 岗位持久化对象
     */
    PostPO selectByPostCode(@Param("postCode") String postCode);

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    List<PostPO> selectAll();

    /**
     * 分页查询岗位（受数据权限控制）
     *
     * @param offset 偏移量
     * @param limit  限制数量
     *
     * @return 岗位列表
     */
    @DataScope(deptAlias = "p", userAlias = "p")
    List<PostPO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计岗位总数
     *
     * @return 岗位总数
     */
    long count();

    /**
     * 按条件分页查询岗位记录（受数据权限控制）
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     * @param offset   偏移量
     * @param limit    限制数量
     *
     * @return 岗位列表
     */
    @DataScope(deptAlias = "p", userAlias = "p")
    List<PostPO> selectPageByCondition(@Param("postName") String postName, @Param("postCode") String postCode,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按条件统计岗位总数（受数据权限控制）
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     *
     * @return 岗位总数
     */
    @DataScope(deptAlias = "p", userAlias = "p")
    long countByCondition(@Param("postName") String postName, @Param("postCode") String postCode,
                          @Param("status") Integer status);

    /**
     * 按条件查询所有岗位（导出用）
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     *
     * @return 岗位列表
     */
    @DataScope(deptAlias = "p", userAlias = "p")
    List<PostPO> selectAllByCondition(@Param("postName") String postName, @Param("postCode") String postCode,
                                      @Param("status") Integer status);

    /**
     * 根据岗位编码统计数量
     *
     * @param postCode 岗位编码
     *
     * @return 数量
     */
    int countByPostCode(@Param("postCode") String postCode);

    /**
     * 根据岗位名称统计数量
     *
     * @param postName 岗位名称
     *
     * @return 数量
     */
    int countByPostName(@Param("postName") String postName);

    /**
     * 物理删除已逻辑删除的岗位记录（根据岗位编码）
     *
     * @param postCode 岗位编码
     *
     * @return 影响的记录数
     */
    int removeDeletedByPostCode(@Param("postCode") String postCode);

}
