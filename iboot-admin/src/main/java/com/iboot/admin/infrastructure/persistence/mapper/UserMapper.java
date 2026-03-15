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
import com.iboot.admin.infrastructure.persistence.po.UserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户 Mapper 接口
 * <p>
 * 负责用户数据的数据库操作，支持用户管理、角色关联和岗位关联
 * </p>
 *
 * @author iBoot
 */
@Mapper
public interface UserMapper {

    /**
     * 插入用户记录
     *
     * @param user 用户持久化对象
     * @return 影响的记录数
     */
    int insert(UserPO user);

    /**
     * 更新用户记录
     *
     * @param user 用户持久化对象
     * @return 影响的记录数
     */
    int update(UserPO user);

    /**
     * 根据 ID 删除用户记录
     *
     * @param id 用户 ID
     * @return 影响的记录数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据 ID 查询用户记录
     *
     * @param id 用户 ID
     * @return 用户持久化对象
     */
    UserPO selectById(@Param("id") Long id);

    /**
     * 根据用户名查询用户记录
     *
     * @param username 用户名
     * @return 用户持久化对象
     */
    UserPO selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户记录
     *
     * @param email 邮箱地址
     * @return 用户持久化对象
     */
    UserPO selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户记录
     *
     * @param phone 手机号码
     * @return 用户持久化对象
     */
    UserPO selectByPhone(@Param("phone") String phone);

    /**
     * 查询所有用户记录
     *
     * @return 用户持久化对象列表
     */
    List<UserPO> selectAll();

    /**
     * 分页查询用户记录（受数据权限控制）
     *
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户持久化对象列表
     */
    @DataScope(deptAlias = "u", userAlias = "u")
    List<UserPO> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    long count();

    /**
     * 按条件分页查询用户记录（受数据权限控制）
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 用户持久化对象列表
     */
    @DataScope(deptAlias = "u", userAlias = "u")
    List<UserPO> selectPageByCondition(@Param("username") String username,
                                       @Param("phone") String phone,
                                       @Param("status") Integer status,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    /**
     * 按条件查询所有用户记录（不分页，用于导出）
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @return 用户持久化对象列表
     */
    @DataScope(deptAlias = "u", userAlias = "u")
    List<UserPO> selectAllByCondition(@Param("username") String username,
                                      @Param("phone") String phone,
                                      @Param("status") Integer status);

    /**
     * 按条件统计用户总数（受数据权限控制）
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @return 用户总数
     */
    @DataScope(deptAlias = "u", userAlias = "u")
    long countByCondition(@Param("username") String username,
                          @Param("phone") String phone,
                          @Param("status") Integer status);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 存在返回 1，否则返回 0
     */
    int existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     * @return 存在返回 1，否则返回 0
     */
    int existsByEmail(@Param("email") String email);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号码
     * @return 存在返回 1，否则返回 0
     */
    int existsByPhone(@Param("phone") String phone);

    /**
     * 根据部门 ID 查询用户列表
     *
     * @param deptId 部门 ID
     * @return 用户持久化对象列表
     */
    List<UserPO> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据角色 ID 查询用户列表
     *
     * @param roleId 角色 ID
     * @return 用户持久化对象列表
     */
    List<UserPO> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 插入用户角色关联记录
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @return 影响的记录数
     */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 根据用户 ID 删除用户角色关联记录
     *
     * @param userId 用户 ID
     * @return 影响的记录数
     */
    int deleteUserRoleByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询角色 ID 列表
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 插入用户岗位关联记录
     *
     * @param userId 用户 ID
     * @param postId 岗位 ID
     * @return 影响的记录数
     */
    int insertUserPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 根据用户 ID 删除用户岗位关联记录
     *
     * @param userId 用户 ID
     * @return 影响的记录数
     */
    int deleteUserPostByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询岗位 ID 列表
     *
     * @param userId 用户 ID
     * @return 岗位 ID 列表
     */
    List<Long> selectPostIdsByUserId(@Param("userId") Long userId);

    /**
     * 物理删除已逻辑删除的用户记录（根据用户名）
     *
     * @param username 用户名
     * @return 影响的记录数
     */
    int removeDeletedByUsername(@Param("username") String username);

    /**
     * 按创建时间统计用户数（受数据权限控制）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户数
     */
    @DataScope(deptAlias = "u", userAlias = "u")
    long countByCreateTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
