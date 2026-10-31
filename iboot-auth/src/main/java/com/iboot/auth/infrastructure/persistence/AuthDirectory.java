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

package com.iboot.auth.infrastructure.persistence;

import com.iboot.auth.domain.model.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 认证所需的共享库访问器。
 *
 * <p>过渡期认证服务的数据源直接指向 {@code iboot_admin}，因此这里使用未限定的表名。访问范围
 * 严格限定为：读取用户、角色、菜单权限与系统参数；写入最后登录信息、注册用户与登录日志。
 *
 * <p>退出条件：{@code iboot-system} 拆分后，本类改为调用系统服务的内部接口，
 * 认证服务不再直接持有系统域表的写权限。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Repository
public class AuthDirectory {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建共享库访问器。
     *
     * @param jdbcTemplate 共享库访问模板
     */
    public AuthDirectory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按用户名查询认证所需的用户视图。
     *
     * @param username 用户名
     * @return 用户视图；不存在或已逻辑删除时返回空
     */
    public Optional<AuthUser> findUserByUsername(String username) {
        List<AuthUser> users = jdbcTemplate.query(
                "SELECT id, username, password, nickname, avatar, dept_id, status "
                        + "FROM sys_user WHERE username = ? AND deleted = 0",
                (rs, rowNum) -> {
                    long deptIdValue = rs.getLong("dept_id");
                    Long deptId = rs.wasNull() ? null : deptIdValue;
                    return new AuthUser(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("nickname"),
                            rs.getString("avatar"),
                            deptId,
                            rs.getInt("status"),
                            new ArrayList<>());
                },
                username);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        AuthUser user = users.get(0);
        List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT role_id FROM sys_user_role WHERE user_id = ?", Long.class, user.id());
        return Optional.of(new AuthUser(user.id(), user.username(), user.password(), user.nickname(),
                user.avatar(), user.deptId(), user.status(), roleIds));
    }

    /**
     * 判断用户名是否已被占用。
     *
     * @param username 用户名
     * @return 存在未删除的同名用户时返回 {@code true}
     */
    public boolean usernameExists(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ? AND deleted = 0", Integer.class, username);
        return count != null && count > 0;
    }

    /**
     * 物理删除已逻辑删除的同名用户，避免与唯一索引冲突。
     *
     * @param username 用户名
     */
    public void removeDeletedUsersByUsername(String username) {
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = ? AND deleted = 1", username);
    }

    /**
     * 插入自助注册的普通用户。
     *
     * @param username 用户名
     * @param encodedPassword 已加密的密码
     * @param nickname 昵称
     */
    public void insertRegisteredUser(String username, String encodedPassword, String nickname) {
        jdbcTemplate.update(
                "INSERT INTO sys_user (username, password, nickname, user_type, status, create_time, deleted) "
                        + "VALUES (?, ?, ?, 2, 1, NOW(), 0)",
                username, encodedPassword, nickname);
    }

    /**
     * 更新用户的最后登录信息。
     *
     * @param userId 用户 ID
     * @param loginIp 登录 IP
     * @param loginTime 登录时间
     */
    public void updateLoginInfo(Long userId, String loginIp, LocalDateTime loginTime) {
        jdbcTemplate.update("UPDATE sys_user SET login_ip = ?, login_time = ? WHERE id = ? AND deleted = 0",
                loginIp, loginTime, userId);
    }

    /**
     * 按角色 ID 查询角色编码。
     *
     * @param roleId 角色 ID
     * @return 角色编码；角色不存在或已逻辑删除时返回空
     */
    public Optional<String> findRoleCode(Long roleId) {
        List<String> codes = jdbcTemplate.queryForList(
                "SELECT role_code FROM sys_role WHERE id = ? AND deleted = 0", String.class, roleId);
        return codes.isEmpty() ? Optional.empty() : Optional.of(codes.get(0));
    }

    /**
     * 查询指定角色关联的非空权限标识。
     *
     * @param roleIds 角色 ID 列表
     * @return 权限标识集合；{@code roleIds} 为空时返回空集合
     */
    public Set<String> findPermissionsByRoleIds(List<Long> roleIds) {
        Set<String> permissions = new LinkedHashSet<>();
        if (roleIds == null || roleIds.isEmpty()) {
            return permissions;
        }
        String placeholders = String.join(",", Collections.nCopies(roleIds.size(), "?"));
        permissions.addAll(jdbcTemplate.queryForList(
                "SELECT DISTINCT m.permission FROM sys_menu m "
                        + "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id "
                        + "WHERE rm.role_id IN (" + placeholders + ") AND m.deleted = 0 "
                        + "AND m.permission IS NOT NULL AND m.permission <> ''",
                String.class, roleIds.toArray()));
        return permissions;
    }

    /**
     * 查询系统中全部非空权限标识，供超级管理员使用。
     *
     * @return 权限标识集合
     */
    public Set<String> findAllPermissions() {
        return new LinkedHashSet<>(jdbcTemplate.queryForList(
                "SELECT DISTINCT permission FROM sys_menu WHERE deleted = 0 "
                        + "AND permission IS NOT NULL AND permission <> ''",
                String.class));
    }

    /**
     * 按键读取系统参数值。
     *
     * @param configKey 配置键
     * @return 配置值；不存在时返回 {@code null}
     */
    public String findConfigValue(String configKey) {
        List<String> values = jdbcTemplate.queryForList(
                "SELECT config_value FROM sys_config WHERE config_key = ? AND deleted = 0 LIMIT 1",
                String.class, configKey);
        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * 写入一条登录日志。
     *
     * @param username 用户名
     * @param ipAddress 登录 IP
     * @param loginLocation 登录地点
     * @param browser 浏览器
     * @param os 操作系统
     * @param status 登录状态：1-成功，0-失败
     * @param message 结果说明
     * @param loginTime 登录时间
     */
    public void insertLoginLog(String username, String ipAddress, String loginLocation, String browser,
                               String os, Integer status, String message, LocalDateTime loginTime) {
        jdbcTemplate.update(
                "INSERT INTO sys_login_log (username, ip_address, login_location, browser, os, status, message, "
                        + "login_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                username, ipAddress, loginLocation, browser, os, status, message, loginTime);
    }
}
