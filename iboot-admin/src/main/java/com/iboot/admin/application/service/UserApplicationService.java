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
import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户应用服务
 * <p>
 * 提供用户的增删改查、密码管理、状态管理等业务功能
 * 用户密码使用 PasswordEncoder 进行加密存储
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建新用户
     * <p>
     * 校验用户名、邮箱、手机号的唯一性，对密码进行加密处理后保存用户
     * </p>
     *
     * @param user 用户实体
     * @return 创建后的用户实体
     * @throws BusinessException 当用户名、邮箱或手机号已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public User createUser(User user) {
        // 校验用户名唯一性
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 清理同名已删除用户记录（解决逻辑删除与唯一索引冲突问题）
        userRepository.removeDeletedByUsername(user.getUsername());

        // 校验邮箱唯一性
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessException("邮箱已被使用");
        }

        // 校验手机号唯一性
        if (user.getPhone() != null && userRepository.existsByPhone(user.getPhone())) {
            throw new BusinessException("手机号已被使用");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 保存用户
        return userRepository.save(user);
    }

    /**
     * 更新用户信息
     * <p>
     * 检查用户是否存在，校验邮箱、手机号的唯一性。
     * 系统用户禁止修改用户名和用户类型。
     * </p>
     *
     * @param user 用户实体
     * @return 是否更新成功
     * @throws BusinessException 当用户不存在或邮箱、手机号重复时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 如果修改了邮箱，校验唯一性
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new BusinessException("邮箱已被使用");
            }
        }

        // 如果修改了手机号，校验唯一性
        if (user.getPhone() != null && !user.getPhone().equals(existingUser.getPhone())) {
            if (userRepository.existsByPhone(user.getPhone())) {
                throw new BusinessException("手机号已被使用");
            }
        }

        // 系统用户禁止修改用户名和用户类型
        if (Constants.USER_TYPE_SYSTEM.equals(existingUser.getUserType())) {
            user.setUsername(existingUser.getUsername());
            user.setUserType(existingUser.getUserType());
        }

        return userRepository.update(user);
    }

    /**
     * 删除用户
     * <p>
     * 检查用户是否存在，系统内置用户不允许删除
     * </p>
     *
     * @param id 用户 ID
     * @return 是否删除成功
     * @throws BusinessException 当用户不存在或是系统内置用户时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (Constants.USER_TYPE_SYSTEM.equals(user.getUserType())) {
            throw new BusinessException("系统内置用户不允许删除");
        }
        return userRepository.deleteById(id);
    }

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户实体
     * @throws BusinessException 当用户不存在时抛出
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     * @throws BusinessException 当用户不存在时抛出
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 分页查询用户
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 用户列表
     */
    public List<User> getUserPage(int pageNum, int pageSize) {
        return userRepository.findPage(pageNum, pageSize);
    }

    /**
     * 统计用户总数
     *
     * @return 用户总数
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * 按条件分页查询用户
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 用户列表
     */
    public List<User> getUserPageByCondition(String username, String phone, Integer status, int pageNum, int pageSize) {
        return userRepository.findPageByCondition(username, phone, status, pageNum, pageSize);
    }

    /**
     * 按条件查询所有用户（不分页，用于导出）
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @return 用户列表
     */
    public List<User> getAllUsersByCondition(String username, String phone, Integer status) {
        return userRepository.findAllByCondition(username, phone, status);
    }

    /**
     * 按条件统计用户总数
     *
     * @param username 用户名（可选）
     * @param phone 手机号（可选）
     * @param status 状态（可选）
     * @return 用户总数
     */
    public long countUsersByCondition(String username, String phone, Integer status) {
        return userRepository.countByCondition(username, phone, status);
    }

    /**
     * 按创建时间统计用户数
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户数
     */
    public long countUsersByCreateTime(LocalDateTime startTime, LocalDateTime endTime) {
        return userRepository.countByCreateTime(startTime, endTime);
    }

    /**
     * 修改用户密码
     * <p>
     * 校验旧密码是否正确，使用 PasswordEncoder 加密新密码后保存
     * </p>
     *
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 是否修改成功
     * @throws BusinessException 当用户不存在或旧密码不正确时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);

        // 校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }

        // 设置新密码
        user.changePassword(passwordEncoder.encode(newPassword));

        return userRepository.update(user);
    }

    /**
     * 重置用户密码
     * <p>
     * 管理员重置用户密码，无需验证旧密码
     * </p>
     *
     * @param userId 用户 ID
     * @param newPassword 新密码
     * @return 是否重置成功
     * @throws BusinessException 当用户不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.changePassword(passwordEncoder.encode(newPassword));
        return userRepository.update(user);
    }

    /**
     * 启用或停用用户
     * <p>
     * 系统内置用户不允许停用
     * </p>
     *
     * @param userId 用户 ID
     * @param status 状态：1-启用，0-停用
     * @return 是否修改成功
     * @throws BusinessException 当用户不存在或是系统内置用户时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(Long userId, Integer status) {
        User user = getUserById(userId);
        if (Constants.USER_TYPE_SYSTEM.equals(user.getUserType()) && status == 0) {
            throw new BusinessException("系统内置用户不允许停用");
        }
        if (status == 1) {
            user.enable();
        } else {
            user.disable();
        }
        return userRepository.update(user);
    }
}
