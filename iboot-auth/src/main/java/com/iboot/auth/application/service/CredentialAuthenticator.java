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

package com.iboot.auth.application.service;

import com.iboot.auth.domain.model.AuthUser;
import com.iboot.auth.infrastructure.metrics.LoginMetrics;
import com.iboot.auth.infrastructure.persistence.AuthDirectory;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 凭据认证器。
 *
 * <p>集中实现用户名密码校验的全部业务规则：失败计数与锁定、用户禁用判定、登录日志、
 * 最后登录信息更新与登录指标。旧的用户名密码登录接口与 OAuth2 授权服务器共用这一份规则，
 * 避免两条认证入口出现行为差异。
 *
 * <p>失败计数与锁定时间保存在共享 Redis（{@code login_retry:*}），因此多实例部署时判定一致。
 * 登录日志写入失败只记录错误，不改变认证结果。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class CredentialAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(CredentialAuthenticator.class);

    private static final String CONFIG_KEY_SESSION_TIMEOUT = "sys.session.timeout";

    private static final String CONFIG_KEY_LOGIN_MAX_RETRY = "sys.login.maxRetry";

    private static final String CONFIG_KEY_LOGIN_LOCK_TIME = "sys.login.lockTime";

    private static final String CONFIG_KEY_SESSION_EXCLUSIVE = "sys.session.exclusive";

    /**
     * 默认会话超时时间（分钟）。
     */
    private static final long DEFAULT_SESSION_TIMEOUT = 30;

    /**
     * 默认最大登录重试次数。
     */
    private static final int DEFAULT_MAX_RETRY = 5;

    /**
     * 默认锁定时间（分钟）。
     */
    private static final long DEFAULT_LOCK_TIME = 10;

    private final AuthDirectory authDirectory;

    private final PasswordEncoder passwordEncoder;

    private final RedisTemplate<String, Object> redisTemplate;

    private final LoginMetrics loginMetrics;

    /**
     * 创建凭据认证器。
     *
     * @param authDirectory 共享库访问器
     * @param passwordEncoder 密码编码器
     * @param redisTemplate 共享会话 Redis 模板
     * @param loginMetrics 登录相关指标
     */
    public CredentialAuthenticator(AuthDirectory authDirectory, PasswordEncoder passwordEncoder,
                                   RedisTemplate<String, Object> redisTemplate, LoginMetrics loginMetrics) {
        this.authDirectory = authDirectory;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.loginMetrics = loginMetrics;
    }

    /**
     * 校验用户名与密码。
     *
     * <p>依次执行锁定检查、用户查询、密码校验与状态校验；成功后清除失败计数、更新最后登录信息
     * 并记录成功日志。失败时累加失败次数，达到阈值后触发锁定。
     *
     * @param username 用户名
     * @param password 密码
     * @param loginIp 登录 IP
     * @param userAgent 浏览器 User-Agent
     * @return 认证通过的用户与会话所需的登录上下文
     * @throws BusinessException 用户名或密码错误、用户被禁用、账户被锁定时抛出
     */
    public AuthenticatedUser verify(String username, String password, String loginIp, String userAgent) {
        String browser = parseBrowser(userAgent);
        String os = parseOs(userAgent);
        checkLoginLock(username, loginIp, browser, os);

        AuthUser user;
        try {
            user = authDirectory.findUserByUsername(username)
                    .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        } catch (BusinessException e) {
            recordLoginRetry(username);
            recordLoginLog(username, loginIp, browser, os, 0, "用户名或密码错误");
            loginMetrics.recordLoginFailed();
            throw e;
        }
        if (!passwordEncoder.matches(password, user.password())) {
            recordLoginRetry(username);
            recordLoginLog(username, loginIp, browser, os, 0, "用户名或密码错误");
            loginMetrics.recordLoginFailed();
            throw new BusinessException("用户名或密码错误");
        }
        if (!user.enabled()) {
            recordLoginLog(username, loginIp, browser, os, 0, "用户已被禁用");
            loginMetrics.recordLoginFailed();
            throw new BusinessException("用户已被禁用");
        }
        clearLoginRetry(username);

        LocalDateTime loginTime = LocalDateTime.now();
        authDirectory.updateLoginInfo(user.id(), loginIp, loginTime);
        recordLoginLog(username, loginIp, browser, os, 1, "登录成功");
        loginMetrics.recordLoginSuccess();
        return new AuthenticatedUser(user, browser, os, loginTime);
    }

    /**
     * 解析角色对应的权限集合。
     *
     * <p>命中超级管理员角色时返回全部权限，否则取角色关联菜单的非空权限标识。
     *
     * @param roleIds 角色 ID 列表
     * @return 权限集合
     */
    public List<String> resolvePermissions(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        for (Long roleId : roleIds) {
            if (authDirectory.findRoleCode(roleId).filter(Constants.ROLE_ADMIN_CODE::equals).isPresent()) {
                return new ArrayList<>(authDirectory.findAllPermissions());
            }
        }
        return new ArrayList<>(authDirectory.findPermissionsByRoleIds(roleIds));
    }

    /**
     * 解析角色编码列表。
     *
     * @param roleIds 角色 ID 列表
     * @return 角色编码集合
     */
    public List<String> resolveRoleCodes(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> roles = new ArrayList<>();
        for (Long roleId : roleIds) {
            authDirectory.findRoleCode(roleId).ifPresent(roles::add);
        }
        return roles;
    }

    /**
     * 读取会话超时时间。
     *
     * @return 会话超时时间（分钟）
     */
    public long sessionTimeoutMinutes() {
        return getConfigValueAsLong(CONFIG_KEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);
    }

    /**
     * 判断是否启用独占会话。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isSessionExclusive() {
        String exclusive = authDirectory.findConfigValue(CONFIG_KEY_SESSION_EXCLUSIVE);
        return "true".equalsIgnoreCase(exclusive);
    }

    /**
     * 按独占会话配置清除该用户的旧会话。
     *
     * @param userId 用户 ID
     */
    public void evictPreviousSession(Long userId) {
        if (!isSessionExclusive()) {
            return;
        }
        String onlineKey = Constants.ONLINE_TOKEN_KEY + userId;
        Object oldToken = redisTemplate.opsForValue().get(onlineKey);
        if (oldToken != null) {
            redisTemplate.delete(Constants.LOGIN_TOKEN_KEY + oldToken);
            redisTemplate.delete(onlineKey);
            log.info("独占会话：已清除用户 {} 的旧会话", userId);
        }
    }

    /**
     * 检查账户是否处于锁定状态。
     *
     * @param username 用户名
     * @param loginIp 登录 IP
     * @param browser 浏览器
     * @param os 操作系统
     * @throws BusinessException 失败次数达到阈值时抛出
     */
    private void checkLoginLock(String username, String loginIp, String browser, String os) {
        int maxRetry = getConfigValueAsInt(CONFIG_KEY_LOGIN_MAX_RETRY, DEFAULT_MAX_RETRY);
        String retryKey = Constants.LOGIN_RETRY_KEY + username;
        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);
        if (retryCount != null && retryCount >= maxRetry) {
            long lockTime = getConfigValueAsLong(CONFIG_KEY_LOGIN_LOCK_TIME, DEFAULT_LOCK_TIME);
            recordLoginLog(username, loginIp, browser, os, 0,
                    "密码错误次数过多，账户锁定" + lockTime + "分钟");
            loginMetrics.recordLoginFailed();
            throw new BusinessException("密码错误次数过多，账户锁定" + lockTime + "分钟");
        }
    }

    /**
     * 累加登录失败次数，达到阈值后按锁定时间保留计数。
     *
     * @param username 用户名
     */
    private void recordLoginRetry(String username) {
        int maxRetry = getConfigValueAsInt(CONFIG_KEY_LOGIN_MAX_RETRY, DEFAULT_MAX_RETRY);
        long lockTime = getConfigValueAsLong(CONFIG_KEY_LOGIN_LOCK_TIME, DEFAULT_LOCK_TIME);
        String retryKey = Constants.LOGIN_RETRY_KEY + username;
        Long retryCount = redisTemplate.opsForValue().increment(retryKey);
        redisTemplate.expire(retryKey, lockTime, TimeUnit.MINUTES);
        if (retryCount != null && retryCount >= maxRetry) {
            log.warn("用户 {} 登录失败次数达到上限 {}，账户锁定 {} 分钟", username, maxRetry, lockTime);
        }
    }

    /**
     * 清除登录失败计数。
     *
     * @param username 用户名
     */
    private void clearLoginRetry(String username) {
        redisTemplate.delete(Constants.LOGIN_RETRY_KEY + username);
    }

    /**
     * 写入登录日志；失败只记录错误，不改变认证结果。
     *
     * @param username 用户名
     * @param loginIp 登录 IP
     * @param browser 浏览器
     * @param os 操作系统
     * @param status 登录状态：1-成功，0-失败
     * @param message 结果说明
     */
    private void recordLoginLog(String username, String loginIp, String browser, String os,
                                Integer status, String message) {
        try {
            authDirectory.insertLoginLog(username, loginIp, "", browser, os, status, message, LocalDateTime.now());
        } catch (RuntimeException e) {
            log.error("记录登录日志失败：{} - {}", username, e.getMessage());
        }
    }

    /**
     * 读取整型系统参数，格式错误时回退默认值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private int getConfigValueAsInt(String key, int defaultValue) {
        try {
            String value = authDirectory.findConfigValue(key);
            return value != null ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("系统配置 {} 值格式错误，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 读取长整型系统参数，格式错误时回退默认值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private long getConfigValueAsLong(String key, long defaultValue) {
        try {
            String value = authDirectory.findConfigValue(key);
            return value != null ? Long.parseLong(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("系统配置 {} 值格式错误，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 从 User-Agent 解析浏览器名称。
     *
     * @param userAgent User-Agent 字符串
     * @return 浏览器名称；无法识别时为 {@code Unknown}
     */
    public static String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        if (userAgent.contains("Edg/")) {
            return "Edge";
        } else if (userAgent.contains("Chrome/")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox/")) {
            return "Firefox";
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) {
            return "Safari";
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "IE";
        } else if (userAgent.contains("Opera") || userAgent.contains("OPR/")) {
            return "Opera";
        }
        return "Unknown";
    }

    /**
     * 从 User-Agent 解析操作系统名称。
     *
     * @param userAgent User-Agent 字符串
     * @return 操作系统名称；无法识别时为 {@code Unknown}
     */
    public static String parseOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return userAgent.contains("Android") ? "Android" : "Linux";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else if (userAgent.contains("Android")) {
            return "Android";
        }
        return "Unknown";
    }

    /**
     * 认证通过的用户与会话上下文。
     *
     * @param user 用户
     * @param browser 浏览器
     * @param os 操作系统
     * @param loginTime 登录时间
     */
    public record AuthenticatedUser(AuthUser user, String browser, String os, LocalDateTime loginTime) {
    }
}
