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

import com.iboot.admin.common.constant.Constants;
import com.iboot.admin.common.exception.BusinessException;
import com.iboot.admin.domain.system.model.Menu;
import com.iboot.admin.domain.system.model.Role;
import com.iboot.admin.domain.system.model.User;
import com.iboot.admin.domain.system.repository.MenuRepository;
import com.iboot.admin.domain.system.repository.RoleRepository;
import com.iboot.admin.domain.system.repository.UserRepository;
import com.iboot.admin.infrastructure.config.JwtSecurityConfig;
import com.iboot.admin.infrastructure.security.JwtTokenUtil;
import com.iboot.admin.interfaces.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 认证应用服务
 * <p>
 * 负责用户登录、登出、注册等认证相关业务逻辑处理。
 * 支持 JWT 令牌生成和验证、Redis 会话管理、登录失败锁定、独占会话控制等功能。
 * 会话超时时间、最大重试次数、锁定时间等参数通过 sys_config 配置动态读取。
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private static final String CONFIG_KEY_REGISTER_USER = "sys.account.registerUser";
    private static final String CONFIG_KEY_SESSION_TIMEOUT = "sys.session.timeout";
    private static final String CONFIG_KEY_LOGIN_MAX_RETRY = "sys.login.maxRetry";
    private static final String CONFIG_KEY_LOGIN_LOCK_TIME = "sys.login.lockTime";
    private static final String CONFIG_KEY_SESSION_EXCLUSIVE = "sys.session.exclusive";

    /** 默认会话超时时间（分钟） */
    private static final long DEFAULT_SESSION_TIMEOUT = 30;
    /** 默认最大登录重试次数 */
    private static final int DEFAULT_MAX_RETRY = 5;
    /** 默认锁定时间（分钟） */
    private static final long DEFAULT_LOCK_TIME = 10;

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtSecurityConfig jwtSecurityConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LoginLogApplicationService loginLogApplicationService;
    private final ConfigApplicationService configApplicationService;
    private final BusinessMetricsService businessMetricsService;

    /**
     * 用户登录
     * <p>
     * 验证用户名密码，检查账户状态，生成 JWT 令牌并存入 Redis 会话。
     * 支持登录失败重试限制和账户锁定机制。
     * </p>
     *
     * @param username  用户名
     * @param password  密码
     * @param loginIp   登录 IP 地址
     * @param userAgent 浏览器 User-Agent
     * @return 登录响应，包含令牌和用户信息
     * @throws BusinessException 当用户名或密码错误、用户被禁用、账户被锁定时抛出
     */
    public LoginResponse login(String username, String password, String loginIp, String userAgent) {
        String browser = parseBrowser(userAgent);
        String os = parseOs(userAgent);

        // 检查账户是否被锁定
        checkLoginLock(username, loginIp, browser, os);

        // 查询用户
        User user;
        try {
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        } catch (BusinessException e) {
            recordLoginRetry(username);
            loginLogApplicationService.recordLoginLog(username, loginIp, "", browser, os, 0, "用户名或密码错误");
            businessMetricsService.recordUserLoginFailed();
            throw e;
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordLoginRetry(username);
            loginLogApplicationService.recordLoginLog(username, loginIp, "", browser, os, 0, "用户名或密码错误");
            businessMetricsService.recordUserLoginFailed();
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (!user.isEnabled()) {
            loginLogApplicationService.recordLoginLog(username, loginIp, "", browser, os, 0, "用户已被禁用");
            businessMetricsService.recordUserLoginFailed();
            throw new BusinessException("用户已被禁用");
        }

        // 登录成功，清除重试计数
        clearLoginRetry(username);

        // 更新登录信息
        user.updateLoginInfo(loginIp, LocalDateTime.now());
        userRepository.update(user);

        // 读取会话超时配置（分钟）
        long sessionTimeoutMinutes = getConfigValueAsLong(CONFIG_KEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);

        // 生成 Token（JWT 过期时间使用独立的兜底配置，与 Redis 会话超时解耦）
        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.USER_ID, user.getId());
        claims.put("username", user.getUsername());
        long jwtExpirationSeconds = jwtSecurityConfig.getExpiration();
        String token = jwtTokenUtil.createToken(claims, jwtExpirationSeconds);

        // 获取用户权限
        List<String> permissions = getPermissionsByRoleIds(user.getRoleIds());
        List<String> roles = getRoleNamesByRoleIds(user.getRoleIds());

        // 将登录信息存入 Redis（使用相同的超时时间）
        Map<String, Object> loginUser = new HashMap<>();
        loginUser.put(Constants.USER_ID, user.getId());
        loginUser.put("username", user.getUsername());
        loginUser.put(Constants.PERMISSIONS, permissions);
        loginUser.put(Constants.ROLES, roles);
        loginUser.put("loginIp", loginIp);
        loginUser.put("browser", browser);
        loginUser.put("os", os);
        loginUser.put("loginTime", LocalDateTime.now().toString());
        loginUser.put("deptId", user.getDeptId());
        loginUser.put("nickname", user.getNickname());

        // 独占会话：如果开启了会话互斥，清除该用户之前的会话
        evictPreviousSession(user.getId());

        String redisKey = Constants.LOGIN_TOKEN_KEY + token;
        redisTemplate.opsForValue().set(redisKey, loginUser, sessionTimeoutMinutes, TimeUnit.MINUTES);

        // 记录用户与 Token 的映射关系（用于独占会话和快速查找）
        String onlineKey = Constants.ONLINE_TOKEN_KEY + user.getId();
        redisTemplate.opsForValue().set(onlineKey, token, sessionTimeoutMinutes, TimeUnit.MINUTES);

        // 记录登录成功日志
        loginLogApplicationService.recordLoginLog(username, loginIp, "", browser, os, 1, "登录成功");

        // 记录登录成功指标
        businessMetricsService.recordUserLoginSuccess();

        // 构建响应
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .permissions(permissions)
                .roles(roles)
                .build();
    }

    /**
     * 用户登出
     * <p>
     * 清除 Redis 中的会话信息，如果开启了独占会话则同时清除用户 -Token 映射。
     * </p>
     *
     * @param token JWT 令牌
     */
    public void logout(String token) {
        if (token != null) {
            String redisKey = Constants.LOGIN_TOKEN_KEY + token;
            // 先获取会话中的 userId，用于清理映射
            @SuppressWarnings("unchecked")
            Map<String, Object> loginUser = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
            if (loginUser != null && loginUser.get(Constants.USER_ID) != null) {
                String onlineKey = Constants.ONLINE_TOKEN_KEY + loginUser.get(Constants.USER_ID);
                // 仅当映射的 token 与当前 token 一致时才清除（避免误删新会话的映射）
                Object mappedToken = redisTemplate.opsForValue().get(onlineKey);
                if (token.equals(mappedToken)) {
                    redisTemplate.delete(onlineKey);
                    // 记录登出指标
                    businessMetricsService.recordUserLogout();
                }
            }
            redisTemplate.delete(redisKey);
        }
    }

    /**
     * 用户自主注册
     * <p>
     * 检查注册功能是否开启，校验用户名唯一性，创建普通用户账户。
     * 注册用户默认用户类型为 2（普通用户），状态为 1（正常）。
     * </p>
     *
     * @param username 用户名
     * @param password 密码
     * @param nickname 昵称
     * @throws BusinessException 当注册功能未开启、用户名已存在时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, String password, String nickname) {
        // 检查注册功能是否开启
        String registerEnabled = configApplicationService.getConfigValue(CONFIG_KEY_REGISTER_USER);
        if (!"true".equalsIgnoreCase(registerEnabled)) {
            throw new BusinessException("当前系统未开启注册功能");
        }

        // 校验用户名唯一性
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }

        // 清理同名已删除用户记录（解决逻辑删除与唯一索引冲突问题）
        userRepository.removeDeletedByUsername(username);

        // 构建注册用户
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname != null && !nickname.isBlank() ? nickname : username)
                .userType(2) // 普通用户
                .status(1)   // 正常状态
                .build();

        userRepository.save(user);
        log.info("用户注册成功：{}", username);
    }

    /**
     * 根据角色 ID 列表获取权限列表
     * <p>
     * 检查是否包含超级管理员角色，超级管理员拥有所有权限。
     * 否则从数据库查询角色关联的菜单权限。
     * </p>
     *
     * @param roleIds 角色 ID 列表
     * @return 权限列表
     */
    private List<String> getPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 检查是否包含超级管理员角色，超级管理员拥有所有权限
        for (Long roleId : roleIds) {
            Optional<Role> role = roleRepository.findById(roleId);
            if (role.isPresent() && Constants.ROLE_ADMIN_CODE.equals(role.get().getRoleCode())) {
                return getAllPermissions();
            }
        }

        // 从数据库查询角色关联的菜单权限
        Set<String> permissions = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Menu> menus = menuRepository.findByRoleId(roleId);
            for (Menu menu : menus) {
                String permission = menu.getPermission();
                if (permission != null && !permission.isBlank()) {
                    permissions.add(permission);
                }
            }
        }

        return new ArrayList<>(permissions);
    }

    /**
     * 获取系统中所有菜单的权限标识（超级管理员使用）
     *
     * @return 所有权限列表
     */
    private List<String> getAllPermissions() {
        Set<String> permissions = new HashSet<>();
        List<Menu> allMenus = menuRepository.findAll();
        for (Menu menu : allMenus) {
            String permission = menu.getPermission();
            if (permission != null && !permission.isBlank()) {
                permissions.add(permission);
            }
        }
        return new ArrayList<>(permissions);
    }

    /**
     * 根据角色 ID 列表获取角色名称列表
     * <p>
     * 从数据库查询角色编码（RoleCode）作为角色标识。
     * </p>
     *
     * @param roleIds 角色 ID 列表
     * @return 角色名称列表
     */
    private List<String> getRoleNamesByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 从数据库查询角色编码
        List<String> roles = new ArrayList<>();
        for (Long roleId : roleIds) {
            roleRepository.findById(roleId).ifPresent(role -> {
                roles.add(role.getRoleCode());
            });
        }

        return roles;
    }

    /**
     * 检查账户是否被锁定
     * <p>
     * 检查登录失败重试次数是否超过阈值，超过则抛出锁定异常。
     * </p>
     *
     * @param username 用户名
     * @param loginIp 登录 IP
     * @param browser 浏览器信息
     * @param os 操作系统信息
     * @throws BusinessException 当账户被锁定时抛出
     */
    private void checkLoginLock(String username, String loginIp, String browser, String os) {
        int maxRetry = getConfigValueAsInt(CONFIG_KEY_LOGIN_MAX_RETRY, DEFAULT_MAX_RETRY);
        String retryKey = Constants.LOGIN_RETRY_KEY + username;
        Integer retryCount = (Integer) redisTemplate.opsForValue().get(retryKey);

        if (retryCount != null && retryCount >= maxRetry) {
            long lockTime = getConfigValueAsLong(CONFIG_KEY_LOGIN_LOCK_TIME, DEFAULT_LOCK_TIME);
            loginLogApplicationService.recordLoginLog(username, loginIp, "", browser, os, 0,
                    "密码错误次数过多，账户锁定" + lockTime + "分钟");
            businessMetricsService.recordUserLoginFailed();
            throw new BusinessException("密码错误次数过多，账户锁定" + lockTime + "分钟");
        }
    }

    /**
     * 记录登录重试次数
     * <p>
     * 每次登录失败时递增重试计数，达到阈值时设置过期时间实现账户锁定。
     * </p>
     *
     * @param username 用户名
     */
    private void recordLoginRetry(String username) {
        int maxRetry = getConfigValueAsInt(CONFIG_KEY_LOGIN_MAX_RETRY, DEFAULT_MAX_RETRY);
        long lockTime = getConfigValueAsLong(CONFIG_KEY_LOGIN_LOCK_TIME, DEFAULT_LOCK_TIME);
        String retryKey = Constants.LOGIN_RETRY_KEY + username;

        Long retryCount = redisTemplate.opsForValue().increment(retryKey);
        if (retryCount != null && retryCount >= maxRetry) {
            redisTemplate.expire(retryKey, lockTime, TimeUnit.MINUTES);
            log.warn("用户 {} 登录失败次数达到上限 {}，账户锁定 {} 分钟", username, maxRetry, lockTime);
        } else {
            // 未达到上限时，设置较长的过期时间（锁定时间），避免 key 永久存在
            redisTemplate.expire(retryKey, lockTime, TimeUnit.MINUTES);
        }
    }

    /**
     * 清除登录重试计数
     * <p>
     * 登录成功时清除重试计数。
     * </p>
     *
     * @param username 用户名
     */
    private void clearLoginRetry(String username) {
        redisTemplate.delete(Constants.LOGIN_RETRY_KEY + username);
    }

    /**
     * 获取会话超时时间（分钟）
     *
     * @return 会话超时时间
     */
    public long getSessionTimeoutMinutes() {
        return getConfigValueAsLong(CONFIG_KEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);
    }

    /**
     * 清除用户之前的会话（独占会话策略）
     * <p>
     * 根据系统配置 sys.session.exclusive 决定是否强制踢掉用户的旧会话。
     * 开启后，同一用户只能保留最新的一个会话，旧会话自动失效。
     * </p>
     *
     * @param userId 用户 ID
     */
    private void evictPreviousSession(Long userId) {
        // 读取配置：是否开启独占会话
        String exclusive = configApplicationService.getConfigValue(CONFIG_KEY_SESSION_EXCLUSIVE);
        if (!"true".equalsIgnoreCase(exclusive)) {
            return;
        }

        String onlineKey = Constants.ONLINE_TOKEN_KEY + userId;
        Object oldToken = redisTemplate.opsForValue().get(onlineKey);
        if (oldToken != null) {
            String oldTokenKey = Constants.LOGIN_TOKEN_KEY + oldToken;
            redisTemplate.delete(oldTokenKey);
            redisTemplate.delete(onlineKey);
            log.info("独占会话：已清除用户 {} 的旧会话", userId);
        }
    }

    /**
     * 从系统配置获取 int 值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private int getConfigValueAsInt(String key, int defaultValue) {
        try {
            String value = configApplicationService.getConfigValue(key);
            return value != null ? Integer.parseInt(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("系统配置 {} 值格式错误，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 从系统配置获取 long 值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    private long getConfigValueAsLong(String key, long defaultValue) {
        try {
            String value = configApplicationService.getConfigValue(key);
            return value != null ? Long.parseLong(value.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("系统配置 {} 值格式错误，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 从 User-Agent 中解析浏览器信息
     * <p>
     * 支持的浏览器：Edge、Chrome、Firefox、Safari、IE、Opera。
     * </p>
     *
     * @param userAgent 浏览器 User-Agent 字符串
     * @return 浏览器名称
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
     * 从 User-Agent 中解析操作系统信息
     * <p>
     * 支持的操作系统：Windows、macOS、Linux、Android、iOS。
     * </p>
     *
     * @param userAgent 浏览器 User-Agent 字符串
     * @return 操作系统名称
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
            if (userAgent.contains("Android")) {
                return "Android";
            }
            return "Linux";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        } else if (userAgent.contains("Android")) {
            return "Android";
        }
        return "Unknown";
    }
}
