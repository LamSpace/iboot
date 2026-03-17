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
import com.iboot.admin.domain.system.model.Post;
import com.iboot.admin.domain.system.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位应用服务
 * <p>
 * 负责岗位的创建、更新、删除、查询等业务逻辑处理， 支持岗位状态管理和条件查询
 * </p>
 *
 * @author iBoot
 */
@Service
public class PostApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PostApplicationService.class);

    private final PostRepository postRepository;

    @SuppressWarnings("all")
    public PostApplicationService(final PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    /**
     * 创建岗位
     * <p>
     * 校验岗位编码和名称的唯一性，清理同编码已删除岗位记录， 设置默认状态为启用
     * </p>
     *
     * @param post 岗位实体
     *
     * @return 创建后的岗位实体
     *
     * @throws BusinessException 当岗位编码或名称已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Post createPost(Post post) {
        // 校验岗位编码唯一性
        if (postRepository.existsByPostCode(post.getPostCode())) {
            throw new BusinessException("岗位编码已存在");
        }
        // 清理同编码已删除岗位记录（解决逻辑删除与唯一索引冲突问题）
        postRepository.removeDeletedByPostCode(post.getPostCode());
        // 校验岗位名称唯一性
        if (postRepository.existsByPostName(post.getPostName())) {
            throw new BusinessException("岗位名称已存在");
        }
        post.setCreateTime(LocalDateTime.now());
        post.setStatus(1);
        return postRepository.save(post);
    }

    /**
     * 更新岗位
     * <p>
     * 检查岗位是否存在，更新岗位信息
     * </p>
     *
     * @param post 岗位实体
     *
     * @return 是否更新成功
     *
     * @throws BusinessException 当岗位不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePost(Post post) {
        Post existingPost = postRepository.findById(post.getId()).orElseThrow(() -> new BusinessException("岗位不存在"));
        post.setUpdateTime(LocalDateTime.now());
        return postRepository.update(post);
    }

    /**
     * 删除岗位
     * <p>
     * 检查岗位是否存在，执行逻辑删除
     * </p>
     *
     * @param id 岗位 ID
     *
     * @return 是否删除成功
     *
     * @throws BusinessException 当岗位不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePost(Long id) {
        if (!postRepository.findById(id).isPresent()) {
            throw new BusinessException("岗位不存在");
        }
        return postRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取岗位
     *
     * @param id 岗位 ID
     *
     * @return 岗位实体
     *
     * @throws BusinessException 当岗位不存在时抛出
     */
    public Post getPostById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new BusinessException("岗位不存在"));
    }

    /**
     * 获取所有岗位
     *
     * @return 岗位列表
     */
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    /**
     * 分页获取岗位
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 岗位列表
     */
    public List<Post> getPostPage(int pageNum, int pageSize) {
        return postRepository.findPage(pageNum, pageSize);
    }

    /**
     * 统计岗位总数
     *
     * @return 岗位总数
     */
    public long countPosts() {
        return postRepository.count();
    }

    /**
     * 按条件分页获取岗位
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 岗位列表
     */
    public List<Post> getPostPageByCondition(String postName, String postCode, Integer status, int pageNum,
                                             int pageSize) {
        return postRepository.findPageByCondition(postName, postCode, status, pageNum, pageSize);
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
    public long countPostsByCondition(String postName, String postCode, Integer status) {
        return postRepository.countByCondition(postName, postCode, status);
    }

    /**
     * 按条件获取所有岗位（不分页，用于导出）
     *
     * @param postName 岗位名称（可选）
     * @param postCode 岗位编码（可选）
     * @param status   状态（可选）
     *
     * @return 岗位列表
     */
    public List<Post> getAllPostsByCondition(String postName, String postCode, Integer status) {
        return postRepository.findAllByCondition(postName, postCode, status);
    }

    /**
     * 修改岗位状态
     *
     * @param id     岗位 ID
     * @param status 状态：1-启用，0-停用
     *
     * @return 是否修改成功
     *
     * @throws BusinessException 当岗位不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(Long id, Integer status) {
        Post post = getPostById(id);
        if (status == 1) {
            post.enable();
        } else {
            post.disable();
        }
        return postRepository.update(post);
    }

}
