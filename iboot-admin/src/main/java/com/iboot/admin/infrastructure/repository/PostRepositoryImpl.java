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

import com.iboot.admin.domain.system.model.Post;
import com.iboot.admin.domain.system.repository.PostRepository;
import com.iboot.admin.infrastructure.persistence.mapper.PostMapper;
import com.iboot.admin.infrastructure.persistence.po.PostPO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 岗位仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现岗位数据的持久化操作
 * </p>
 *
 * @author iBoot
 */
@Repository
public class PostRepositoryImpl implements PostRepository {

    private final PostMapper postMapper;

    @SuppressWarnings("all")
    public PostRepositoryImpl(final PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    /**
     * 保存岗位
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录
     * </p>
     *
     * @param post 岗位实体
     *
     * @return 保存后的岗位
     */
    @Override
    public Post save(Post post) {
        PostPO postPO = convertToPO(post);
        if (post.getId() == null) {
            postMapper.insert(postPO);
        } else {
            postMapper.update(postPO);
        }
        return convertToDomain(postPO);
    }

    /**
     * 更新岗位
     *
     * @param post 岗位实体
     *
     * @return 是否更新成功
     */
    @Override
    public boolean update(Post post) {
        PostPO postPO = convertToPO(post);
        return postMapper.update(postPO) > 0;
    }

    /**
     * 根据 ID 删除岗位（逻辑删除）
     *
     * @param id 岗位 ID
     *
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return postMapper.deleteById(id) > 0;
    }

    /**
     * 根据 ID 查询岗位
     *
     * @param id 岗位 ID
     *
     * @return 岗位实体，不存在则返回空
     */
    @Override
    public Optional<Post> findById(Long id) {
        PostPO postPO = postMapper.selectById(id);
        return Optional.ofNullable(postPO).map(this::convertToDomain);
    }

    /**
     * 根据岗位编码查询岗位
     *
     * @param postCode 岗位编码
     *
     * @return 岗位实体，不存在则返回空
     */
    @Override
    public Optional<Post> findByPostCode(String postCode) {
        PostPO postPO = postMapper.selectByPostCode(postCode);
        return Optional.ofNullable(postPO).map(this::convertToDomain);
    }

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    @Override
    public List<Post> findAll() {
        return postMapper.selectAll().stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 分页查询岗位
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 岗位列表
     */
    @Override
    public List<Post> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return postMapper.selectPage(offset, pageSize).stream().map(this::convertToDomain).collect(Collectors.toList());
    }

    /**
     * 统计岗位总数
     *
     * @return 岗位总数
     */
    @Override
    public long count() {
        return postMapper.count();
    }

    /**
     * 按条件分页查询岗位
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     *
     * @return 岗位列表
     */
    @Override
    public List<Post> findPageByCondition(String postName, String postCode, Integer status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return postMapper.selectPageByCondition(postName, postCode, status, offset, pageSize)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计岗位总数
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     *
     * @return 岗位总数
     */
    @Override
    public long countByCondition(String postName, String postCode, Integer status) {
        return postMapper.countByCondition(postName, postCode, status);
    }

    /**
     * 按条件查询所有岗位（导出用）
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     *
     * @return 岗位列表
     */
    @Override
    public List<Post> findAllByCondition(String postName, String postCode, Integer status) {
        return postMapper.selectAllByCondition(postName, postCode, status)
                .stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 检查岗位编码是否存在
     *
     * @param postCode 岗位编码
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByPostCode(String postCode) {
        return postMapper.countByPostCode(postCode) > 0;
    }

    /**
     * 检查岗位名称是否存在
     *
     * @param postName 岗位名称
     *
     * @return 是否存在
     */
    @Override
    public boolean existsByPostName(String postName) {
        return postMapper.countByPostName(postName) > 0;
    }

    /**
     * 物理删除已逻辑删除的岗位记录（根据岗位编码）
     *
     * @param postCode 岗位编码
     *
     * @return 是否删除成功
     */
    @Override
    public boolean removeDeletedByPostCode(String postCode) {
        return postMapper.removeDeletedByPostCode(postCode) > 0;
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param post 岗位领域对象
     *
     * @return 岗位持久化对象
     */
    private PostPO convertToPO(Post post) {
        PostPO postPO = new PostPO();
        postPO.setId(post.getId());
        postPO.setPostCode(post.getPostCode());
        postPO.setPostName(post.getPostName());
        postPO.setPostSort(post.getPostSort());
        postPO.setStatus(post.getStatus());
        postPO.setCreateBy(post.getCreateBy());
        postPO.setCreateTime(post.getCreateTime());
        postPO.setUpdateBy(post.getUpdateBy());
        postPO.setUpdateTime(post.getUpdateTime());
        postPO.setDeleted(post.getDeleted() != null ? post.getDeleted() : 0);
        postPO.setRemark(post.getRemark());
        return postPO;
    }

    /**
     * 将持久化对象转换为领域对象
     *
     * @param postPO 岗位持久化对象
     *
     * @return 岗位领域对象
     */
    private Post convertToDomain(PostPO postPO) {
        return Post.builder()
                .id(postPO.getId())
                .postCode(postPO.getPostCode())
                .postName(postPO.getPostName())
                .postSort(postPO.getPostSort())
                .status(postPO.getStatus())
                .createBy(postPO.getCreateBy())
                .createTime(postPO.getCreateTime())
                .updateBy(postPO.getUpdateBy())
                .updateTime(postPO.getUpdateTime())
                .deleted(postPO.getDeleted())
                .remark(postPO.getRemark())
                .build();
    }

}
