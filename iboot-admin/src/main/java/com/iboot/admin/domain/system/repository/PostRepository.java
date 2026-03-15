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

import com.iboot.admin.domain.system.model.Post;

import java.util.List;
import java.util.Optional;

/**
 * 岗位仓储接口
 * <p>
 * 负责岗位数据的持久化操作，提供岗位的新增、修改、删除和查询功能
 * </p>
 *
 * @author iBoot
 */
public interface PostRepository {

    /**
     * 保存岗位
     *
     * @param post 岗位实体
     * @return 保存后的岗位
     */
    Post save(Post post);

    /**
     * 更新岗位
     *
     * @param post 岗位实体
     * @return 是否更新成功
     */
    boolean update(Post post);

    /**
     * 根据 ID 删除岗位（逻辑删除）
     *
     * @param id 岗位 ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据 ID 查询岗位
     *
     * @param id 岗位 ID
     * @return 岗位实体，不存在则返回空
     */
    Optional<Post> findById(Long id);

    /**
     * 根据岗位编码查询岗位
     *
     * @param postCode 岗位编码
     * @return 岗位实体，不存在则返回空
     */
    Optional<Post> findByPostCode(String postCode);

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    List<Post> findAll();

    /**
     * 分页查询岗位
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 岗位列表
     */
    List<Post> findPage(int pageNum, int pageSize);

    /**
     * 统计岗位总数
     *
     * @return 岗位总数
     */
    long count();

    /**
     * 按条件分页查询岗位
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status 状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 岗位列表
     */
    List<Post> findPageByCondition(String postName, String postCode, Integer status, int pageNum, int pageSize);

    /**
     * 按条件统计岗位总数
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status 状态（可选）
     * @return 岗位总数
     */
    long countByCondition(String postName, String postCode, Integer status);

    /**
     * 按条件查询所有岗位（导出用）
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status 状态（可选）
     * @return 岗位列表
     */
    List<Post> findAllByCondition(String postName, String postCode, Integer status);

    /**
     * 检查岗位编码是否存在
     *
     * @param postCode 岗位编码
     * @return 是否存在
     */
    boolean existsByPostCode(String postCode);

    /**
     * 检查岗位名称是否存在
     *
     * @param postName 岗位名称
     * @return 是否存在
     */
    boolean existsByPostName(String postName);

    /**
     * 物理删除已逻辑删除的岗位记录（根据岗位编码）
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param postCode 岗位编码
     * @return 是否删除成功
     */
    boolean removeDeletedByPostCode(String postCode);
}
