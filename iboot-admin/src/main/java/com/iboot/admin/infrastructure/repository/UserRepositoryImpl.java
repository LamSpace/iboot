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

import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.infrastructure.persistence.mapper.UserMapper;
import com.iboot.admin.infrastructure.persistence.po.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户仓储实现类
 * <p>
 * 基于 MyBatis Mapper 实现用户数据的持久化操作，处理用户与角色、岗位的关联关系
 * </p>
 *
 * @author iBoot
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    /**
     * 保存用户
     * <p>
     * 如果 ID 为空则插入新记录，否则更新现有记录。
     * 同时保存用户与角色、岗位的关联关系。
     * </p>
     *
     * @param user 用户实体
     * @return 保存后的用户
     */
    @Override
    public User save(User user) {
        UserPO userPO = convertToPO(user);
        if (user.getId() == null) {
            userMapper.insert(userPO);
        } else {
            userMapper.update(userPO);
        }

        // 保存用户角色关联
        if (user.getRoleIds() != null) {
            userMapper.deleteUserRoleByUserId(userPO.getId());
            for (Long roleId : user.getRoleIds()) {
                userMapper.insertUserRole(userPO.getId(), roleId);
            }
        }

        // 保存用户岗位关联
        if (user.getPostIds() != null) {
            userMapper.deleteUserPostByUserId(userPO.getId());
            for (Long postId : user.getPostIds()) {
                userMapper.insertUserPost(userPO.getId(), postId);
            }
        }

        return convertToDomain(userPO);
    }

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户实体，不存在则返回空
     */
    @Override
    public Optional<User> findById(Long id) {
        UserPO userPO = userMapper.selectById(id);
        return Optional.ofNullable(userPO).map(this::convertToDomain);
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体，不存在则返回空
     */
    @Override
    public Optional<User> findByUsername(String username) {
        UserPO userPO = userMapper.selectByUsername(username);
        return Optional.ofNullable(userPO).map(this::convertToDomain);
    }

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱地址
     * @return 用户实体，不存在则返回空
     */
    @Override
    public Optional<User> findByEmail(String email) {
        UserPO userPO = userMapper.selectByEmail(email);
        return Optional.ofNullable(userPO).map(this::convertToDomain);
    }

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户实体，不存在则返回空
     */
    @Override
    public Optional<User> findByPhone(String phone) {
        UserPO userPO = userMapper.selectByPhone(phone);
        return Optional.ofNullable(userPO).map(this::convertToDomain);
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    @Override
    public List<User> findAll() {
        return userMapper.selectAll().stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询用户
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 用户列表
     */
    @Override
    public List<User> findPage(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return userMapper.selectPage(offset, pageSize).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    @Override
    public long count() {
        return userMapper.count();
    }

    /**
     * 按条件分页查询用户
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 用户列表
     */
    @Override
    public List<User> findPageByCondition(String username, String phone, Integer status, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return userMapper.selectPageByCondition(username, phone, status, offset, pageSize).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件查询所有用户（不分页，用于导出）
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @return 用户列表
     */
    @Override
    public List<User> findAllByCondition(String username, String phone, Integer status) {
        return userMapper.selectAllByCondition(username, phone, status).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 按条件统计用户总数
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @return 用户总数
     */
    @Override
    public long countByCondition(String username, String phone, Integer status) {
        return userMapper.countByCondition(username, phone, status);
    }

    /**
     * 更新用户信息
     * <p>
     * 更新用户基本信息，同时更新用户与角色、岗位的关联关系。
     * </p>
     *
     * @param user 用户实体
     * @return 是否更新成功
     */
    @Override
    public boolean update(User user) {
        UserPO userPO = convertToPO(user);
        boolean result = userMapper.update(userPO) > 0;

        // 更新用户角色关联
        if (user.getRoleIds() != null) {
            userMapper.deleteUserRoleByUserId(user.getId());
            for (Long roleId : user.getRoleIds()) {
                userMapper.insertUserRole(user.getId(), roleId);
            }
        }

        // 更新用户岗位关联
        if (user.getPostIds() != null) {
            userMapper.deleteUserPostByUserId(user.getId());
            for (Long postId : user.getPostIds()) {
                userMapper.insertUserPost(user.getId(), postId);
            }
        }

        return result;
    }

    /**
     * 根据 ID 删除用户（逻辑删除）
     *
     * @param id 用户 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteById(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username) > 0;
    }

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱地址
     * @return 是否存在
     */
    @Override
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email) > 0;
    }

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @return 是否存在
     */
    @Override
    public boolean existsByPhone(String phone) {
        return userMapper.existsByPhone(phone) > 0;
    }

    /**
     * 根据部门 ID 查询用户列表
     *
     * @param deptId 部门 ID
     * @return 用户列表
     */
    @Override
    public List<User> findByDeptId(Long deptId) {
        return userMapper.selectByDeptId(deptId).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色 ID 查询用户列表
     *
     * @param roleId 角色 ID
     * @return 用户列表
     */
    @Override
    public List<User> findByRoleId(Long roleId) {
        return userMapper.selectByRoleId(roleId).stream()
                .map(this::convertToDomain)
                .collect(Collectors.toList());
    }

    /**
     * 物理删除已逻辑删除的用户记录（根据用户名）
     *
     * @param username 用户名
     * @return 是否删除成功
     */
    @Override
    public boolean removeDeletedByUsername(String username) {
        return userMapper.removeDeletedByUsername(username) > 0;
    }

    /**
     * 统计指定时间范围内创建的用户数量
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户数量
     */
    @Override
    public long countByCreateTime(LocalDateTime startTime, LocalDateTime endTime) {
        return userMapper.countByCreateTime(startTime, endTime);
    }

    /**
     * 将领域对象转换为持久化对象
     *
     * @param user 用户领域对象
     * @return 用户持久化对象
     */
    private UserPO convertToPO(User user) {
        UserPO userPO = new UserPO();
        userPO.setId(user.getId());
        userPO.setUsername(user.getUsername());
        userPO.setPassword(user.getPassword());
        userPO.setNickname(user.getNickname());
        userPO.setEmail(user.getEmail());
        userPO.setPhone(user.getPhone());
        userPO.setGender(user.getGender());
        userPO.setAvatar(user.getAvatar());
        userPO.setUserType(user.getUserType());
        userPO.setDeptId(user.getDeptId());
        userPO.setStatus(user.getStatus());
        userPO.setLoginIp(user.getLoginIp());
        userPO.setLoginTime(user.getLoginTime());
        userPO.setCreateBy(user.getCreateBy());
        userPO.setCreateTime(user.getCreateTime());
        userPO.setUpdateBy(user.getUpdateBy());
        userPO.setUpdateTime(user.getUpdateTime());
        userPO.setDeleted(user.getDeleted() != null ? user.getDeleted() : 0);
        userPO.setRemark(user.getRemark());
        return userPO;
    }

    /**
     * 将持久化对象转换为领域对象
     * <p>
     * 转换时会同时查询用户关联的角色 ID 列表和岗位 ID 列表。
     * </p>
     *
     * @param userPO 用户持久化对象
     * @return 用户领域对象
     */
    private User convertToDomain(UserPO userPO) {
        // 查询用户关联的角色 ID 列表
        List<Long> roleIds = userMapper.selectRoleIdsByUserId(userPO.getId());
        // 查询用户关联的岗位 ID 列表
        List<Long> postIds = userMapper.selectPostIdsByUserId(userPO.getId());

        return User.builder()
                .id(userPO.getId())
                .username(userPO.getUsername())
                .password(userPO.getPassword())
                .nickname(userPO.getNickname())
                .email(userPO.getEmail())
                .phone(userPO.getPhone())
                .gender(userPO.getGender())
                .avatar(userPO.getAvatar())
                .userType(userPO.getUserType())
                .deptId(userPO.getDeptId())
                .roleIds(roleIds)
                .postIds(postIds)
                .status(userPO.getStatus())
                .loginIp(userPO.getLoginIp())
                .loginTime(userPO.getLoginTime())
                .createBy(userPO.getCreateBy())
                .createTime(userPO.getCreateTime())
                .updateBy(userPO.getUpdateBy())
                .updateTime(userPO.getUpdateTime())
                .deleted(userPO.getDeleted())
                .remark(userPO.getRemark())
                .build();
    }
}
