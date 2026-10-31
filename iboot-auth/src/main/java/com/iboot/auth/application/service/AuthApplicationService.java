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
import com.iboot.auth.interfaces.dto.response.LoginResponse;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.security.JwtSecurityConfig;
import com.iboot.kernel.security.JwtTokenUtil;
import com.iboot.kernel.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证应用服务。
 *
 * <p>负责登录、登出与自助注册。登录校验用户名密码、账号状态与失败锁定，签发 JWT 并把会话写入
 * 共享 Redis；其余服务仍以该会话记录作为授权依据，因此本服务重启不会影响已登录用户。
 *
 * <p>会话超时、失败次数上限、锁定时长、是否开放注册、是否独占会话等参数读取自
 * {@code sys_config}，读取失败时回退到内置默认值。登录日志写入失败只记录错误，不改变登录结果。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private static final String CONFIG_KEY_REGISTER_USER = "sys.account.registerUser";

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

    private final JwtTokenUtil jwtTokenUtil;

    private final JwtSecurityConfig jwtSecurityConfig;

    private final RedisTemplate<String, Object> redisTemplate;

    private final LoginMetrics loginMetrics;

    /**
     * 创建认证服务。
     *
     * @param authDirectory 共享库访问器
     * @param passwordEncoder 密码编码器
     * @param jwtTokenUtil 令牌工具
     * @param jwtSecurityConfig 令牌与会话配置
     * @param redisTemplate 共享会话 Redis 模板
     * @param loginMetrics 登录相关指标
     */
    public AuthApplicationService(AuthDirectory authDirectory, PasswordEncoder passwordEncoder,
                                  JwtTokenUtil jwtTokenUtil, JwtSecurityConfig jwtSecurityConfig,
                                  RedisTemplate<String, Object> redisTemplate, LoginMetrics loginMetrics) {
        this.authDirectory = authDirectory;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtSecurityConfig = jwtSecurityConfig;
        this.redisTemplate = redisTemplate;
        this.loginMetrics = loginMetrics;
    }

    /**
     * 用户登录。
     *
     * <p>依次执行账号锁定检查、用户名查询、密码校验与状态校验；成功后更新最后登录信息、记录登录日志，
     * 并写入令牌会话。登录失败会累加失败次数，达到阈值后触发账户锁定。
     *
     * @param username 用户名
     * @param password 密码
     * @param loginIp 登录 IP
     * @param userAgent 浏览器 User-Agent
     * @return 登录结果，包含令牌、用户信息、权限与角色
     * @throws BusinessException 用户名或密码错误、用户被禁用、账户被锁定时抛出
     */
    public LoginResponse login(String username, String password, String loginIp, String userAgent) {
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

        long sessionTimeoutMinutes = getConfigValueAsLong(CONFIG_KEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.USER_ID, user.id());
        claims.put("username", user.username());
        long jwtExpirationSeconds = jwtSecurityConfig.getExpiration();
        String token = jwtTokenUtil.createToken(claims, jwtExpirationSeconds);

        List<String> permissions = resolvePermissions(user.roleIds());
        List<String> roles = resolveRoleCodes(user.roleIds());

        Map<String, Object> loginUser = new HashMap<>();
        loginUser.put(Constants.USER_ID, user.id());
        loginUser.put("username", user.username());
        loginUser.put(Constants.PERMISSIONS, permissions);
        loginUser.put(Constants.ROLES, roles);
        loginUser.put("loginIp", loginIp);
        loginUser.put("browser", browser);
        loginUser.put("os", os);
        loginUser.put("loginTime", loginTime.toString());
        loginUser.put("deptId", user.deptId());
        loginUser.put("nickname", user.nickname());

        evictPreviousSession(user.id());
        String redisKey = Constants.LOGIN_TOKEN_KEY + token;
        redisTemplate.opsForValue().set(redisKey, loginUser, sessionTimeoutMinutes, TimeUnit.MINUTES);
        String onlineKey = Constants.ONLINE_TOKEN_KEY + user.id();
        redisTemplate.opsForValue().set(onlineKey, token, sessionTimeoutMinutes, TimeUnit.MINUTES);

        recordLoginLog(username, loginIp, browser, os, 1, "登录成功");
        loginMetrics.recordLoginSuccess();

        return new LoginResponse(token, user.id(), user.username(), user.nickname(), user.avatar(),
                permissions, roles);
    }

    /**
     * 用户登出。
     *
     * <p>删除令牌对应的共享会话；仅当在线映射仍指向当前令牌时才一并清理，避免误删新会话。
     * 未携带令牌时按成功处理。
     *
     * @param token 访问令牌，可为 {@code null}
     */
    public void logout(String token) {
        if (token == null) {
            return;
        }
        String redisKey = Constants.LOGIN_TOKEN_KEY + token;
        @SuppressWarnings("unchecked")
        Map<String, Object> loginUser = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
        if (loginUser != null && loginUser.get(Constants.USER_ID) != null) {
            String onlineKey = Constants.ONLINE_TOKEN_KEY + loginUser.get(Constants.USER_ID);
            Object mappedToken = redisTemplate.opsForValue().get(onlineKey);
            if (token.equals(mappedToken)) {
                redisTemplate.delete(onlineKey);
                loginMetrics.recordLogout();
            }
        }
        redisTemplate.delete(redisKey);
    }

    /**
     * 用户自助注册。
     *
     * <p>仅在系统参数开启注册时可用；用户名唯一性在此校验，同名逻辑删除记录会被物理清理，
     * 以免与唯一索引冲突。注册用户固定为普通用户（类型 2）且状态正常。
     *
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称；为空时回落为用户名
     * @throws BusinessException 注册未开启或用户名已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String nickname) {
        String registerEnabled = authDirectory.findConfigValue(CONFIG_KEY_REGISTER_USER);
        if (!"true".equalsIgnoreCase(registerEnabled)) {
            throw new BusinessException("当前系统未开启注册功能");
        }
        if (authDirectory.usernameExists(username)) {
            throw new BusinessException("用户名已存在");
        }
        authDirectory.removeDeletedUsersByUsername(username);
        String displayName = nickname != null && !nickname.isBlank() ? nickname : username;
        authDirectory.insertRegisteredUser(username, passwordEncoder.encode(password), displayName);
        log.info("用户注册成功：{}", username);
    }

    /**
     * 解析角色对应的权限集合。
     *
     * <p>命中超级管理员角色时返回全部权限，否则取角色关联菜单的非空权限标识。
     *
     * @param roleIds 角色 ID 列表
     * @return 权限集合
     */
    private List<String> resolvePermissions(List<Long> roleIds) {
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
    private List<String> resolveRoleCodes(List<Long> roleIds) {
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
     * 按独占会话配置清除该用户的旧会话。
     *
     * @param userId 用户 ID
     */
    private void evictPreviousSession(Long userId) {
        String exclusive = authDirectory.findConfigValue(CONFIG_KEY_SESSION_EXCLUSIVE);
        if (!"true".equalsIgnoreCase(exclusive)) {
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
     * 写入登录日志；失败只记录错误，不改变登录结果。
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
    private String parseBrowser(String userAgent) {
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
    private String parseOs(String userAgent) {
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
}
