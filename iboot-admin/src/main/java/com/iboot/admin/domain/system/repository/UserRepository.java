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

import com.iboot.admin.domain.system.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口
 * <p>
 * 负责用户数据的持久化操作，提供用户信息管理、认证和统计功能
 * </p>
 *
 * @author iBoot
 */
public interface UserRepository {

    /**
     * 保存用户
     *
     * @param user 用户实体
     *
     * @return 保存后的用户
     */
    User save(User user);

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     *
     * @return 用户实体，不存在则返回空
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     *
     * @return 用户实体，不存在则返回空
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱地址
     *
     * @return 用户实体，不存在则返回空
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号码
     *
     * @return 用户实体，不存在则返回空
     */
    Optional<User> findByPhone(String phone);

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    List<User> findAll();

    /**
     * 分页查询用户
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页数量
     *
     * @return 用户列表
     */
    List<User> findPage(int pageNum, int pageSize);

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    long count();

    /**
     * 按条件分页查询用户
     *
     * @param username 用户名（可选）
     * @param phone    手机号（可选）
     * @param status   状态（可选）
     * @param pageNum  页码
     * @param pageSize 每页数量
     *
     * @return 用户列表
     */
    List<User> findPageByCondition(String username, String phone, Integer status, int pageNum, int pageSize);

    /**
     * 按条件查询所有用户（不分页，用于导出）
     *
     * @param username 用户名（可选）
     * @param phone    手机号（可选）
     * @param status   状态（可选）
     *
     * @return 用户列表
     */
    List<User> findAllByCondition(String username, String phone, Integer status);

    /**
     * 按条件统计用户总数
     *
     * @param username 用户名（可选）
     * @param phone    手机号（可选）
     * @param status   状态（可选）
     *
     * @return 用户总数
     */
    long countByCondition(String username, String phone, Integer status);

    /**
     * 更新用户
     *
     * @param user 用户实体
     *
     * @return 是否更新成功
     */
    boolean update(User user);

    /**
     * 删除用户（逻辑删除）
     *
     * @param id 用户 ID
     *
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     *
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     *
     * @return 是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号码
     *
     * @return 是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据部门 ID 查询用户
     *
     * @param deptId 部门 ID
     *
     * @return 用户列表
     */
    List<User> findByDeptId(Long deptId);

    /**
     * 根据角色 ID 查询用户
     *
     * @param roleId 角色 ID
     *
     * @return 用户列表
     */
    List<User> findByRoleId(Long roleId);

    /**
     * 物理删除已逻辑删除的用户记录（根据用户名）
     * <p>
     * 用于清理已被逻辑删除的历史数据
     * </p>
     *
     * @param username 用户名
     *
     * @return 是否删除成功
     */
    boolean removeDeletedByUsername(String username);

    /**
     * 按创建时间统计用户数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     *
     * @return 用户数
     */
    long countByCreateTime(LocalDateTime startTime, LocalDateTime endTime);

}
