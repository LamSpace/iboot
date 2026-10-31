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

import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户应用服务。
 *
 * <p>负责用户聚合根的用例编排：创建、查询、更新、删除、状态切换、密码修改与重置。
 * 本类不直接访问数据库，所有持久化操作通过 {@link UserRepository} 完成；
 * 用户密码在写入前由 {@link PasswordEncoder} 加密。
 *
 * <p>所有写操作都在 {@link Transactional} 事务中执行，可预期的业务错误以
 * {@link BusinessException} 抛出。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see UserRepository
 * @see com.iboot.admin.interfaces.controller.UserController
 */
@Service
public class UserApplicationService {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @SuppressWarnings("all")
    public UserApplicationService(final UserRepository userRepository, final PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 创建新用户。
     *
     * <p>依次校验用户名、邮箱、手机号的唯一性；若存在同名逻辑删除记录，先物理清理再创建，
     * 以避免逻辑删除与唯一索引冲突。密码在持久化前使用 {@link PasswordEncoder} 加密；
     * 任一唯一性校验失败时事务回滚。
     *
     * @param user 待创建的用户，用户名和密码不能为 {@code null}
     * @return 已持久化的用户，包含数据库生成的主键
     * @throws BusinessException 用户名、邮箱或手机号已存在时抛出
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
     * 更新用户信息。
     *
     * <p>先确认用户存在；邮箱或手机号发生变化时重新校验唯一性。
     * 系统内置用户的用户名和用户类型不允许修改，这两个字段会被恢复为数据库中的原值。
     *
     * @param user 待更新的用户，{@code id} 不能为 {@code null}
     * @return {@code true} 表示更新成功；{@code false} 表示没有记录被更新
     * @throws BusinessException 用户不存在，或新邮箱、手机号已被占用时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        User existingUser = userRepository.findById(user.getId()).orElseThrow(() -> new BusinessException("用户不存在"));
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
     * 删除用户。
     *
     * <p>先确认用户存在；系统内置用户不允许删除。删除方式由仓储层决定。
     *
     * @param id 用户 ID
     * @return {@code true} 表示删除成功；{@code false} 表示没有记录被删除
     * @throws BusinessException 用户不存在或是系统内置用户时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
        if (Constants.USER_TYPE_SYSTEM.equals(user.getUserType())) {
            throw new BusinessException("系统内置用户不允许删除");
        }
        return userRepository.deleteById(id);
    }

    /**
     * 根据 ID 查询用户。
     *
     * @param id 用户 ID
     * @return 对应的用户实体，不会返回 {@code null}
     * @throws BusinessException 用户不存在时抛出
     */
    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 对应的用户实体，不会返回 {@code null}
     * @throws BusinessException 用户不存在时抛出
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /**
     * 查询所有未删除的用户。
     *
     * @return 用户列表；没有数据时返回空列表
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 分页查询用户。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 当前页的用户列表
     */
    public List<User> getUserPage(int pageNum, int pageSize) {
        return userRepository.findPage(pageNum, pageSize);
    }

    /**
     * 统计未删除用户总数。
     *
     * @return 用户总数
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * 按条件分页查询用户。
     *
     * <p>用户名和手机号使用模糊匹配；空字符串与 {@code null} 等价，表示不参与筛选。
     *
     * @param username 用户名关键字，可为 {@code null}
     * @param phone 手机号关键字，可为 {@code null}
     * @param status 状态：0-停用，1-启用；为 {@code null} 时不筛选
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @return 当前页的用户列表
     */
    public List<User> getUserPageByCondition(String username, String phone, Integer status, int pageNum, int pageSize) {
        return userRepository.findPageByCondition(username, phone, status, pageNum, pageSize);
    }

    /**
     * 按条件查询所有用户，不分页，用于导出。
     *
     * <p>筛选规则与 {@link #getUserPageByCondition(String, String, Integer, int, int)} 一致。
     *
     * @param username 用户名关键字，可为 {@code null}
     * @param phone 手机号关键字，可为 {@code null}
     * @param status 状态：0-停用，1-启用；为 {@code null} 时不筛选
     * @return 符合条件的用户列表；没有数据时返回空列表
     */
    public List<User> getAllUsersByCondition(String username, String phone, Integer status) {
        return userRepository.findAllByCondition(username, phone, status);
    }

    /**
     * 按条件统计用户总数。
     *
     * <p>筛选规则与 {@link #getUserPageByCondition(String, String, Integer, int, int)} 一致。
     *
     * @param username 用户名关键字，可为 {@code null}
     * @param phone 手机号关键字，可为 {@code null}
     * @param status 状态：0-停用，1-启用；为 {@code null} 时不筛选
     * @return 符合条件的用户总数
     */
    public long countUsersByCondition(String username, String phone, Integer status) {
        return userRepository.countByCondition(username, phone, status);
    }

    /**
     * 按创建时间统计用户数。
     *
     * <p>统计区间为闭区间，包含 {@code startTime} 和 {@code endTime} 两个边界值。
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 创建时间落在区间内的用户数
     */
    public long countUsersByCreateTime(LocalDateTime startTime, LocalDateTime endTime) {
        return userRepository.countByCreateTime(startTime, endTime);
    }

    /**
     * 修改用户密码。
     *
     * <p>先校验旧密码；校验通过后使用 {@link PasswordEncoder} 加密新密码并持久化。
     *
     * @param userId 用户 ID
     * @param oldPassword 旧密码明文
     * @param newPassword 新密码明文
     * @return {@code true} 表示修改成功
     * @throws BusinessException 用户不存在或旧密码不正确时抛出
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
     * 重置用户密码。
     *
     * <p>管理员操作，不校验旧密码；新密码加密后持久化。
     *
     * @param userId 用户 ID
     * @param newPassword 新密码明文
     * @return {@code true} 表示重置成功
     * @throws BusinessException 用户不存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) {
        User user = getUserById(userId);
        user.changePassword(passwordEncoder.encode(newPassword));
        return userRepository.update(user);
    }

    /**
     * 启用或停用用户。
     *
     * <p>系统内置用户不允许停用；状态变更通过 {@link User#enable()} 或
     * {@link User#disable()} 完成。
     *
     * @param userId 用户 ID
     * @param status 目标状态：1-启用，0-停用
     * @return {@code true} 表示修改成功
     * @throws BusinessException 用户不存在，或尝试停用系统内置用户时抛出
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
