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

import com.iboot.auth.infrastructure.metrics.LoginMetrics;
import com.iboot.auth.infrastructure.persistence.AuthDirectory;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.security.JwtTokenVerifier;
import com.iboot.kernel.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 认证应用服务。
 *
 * <p>负责登出与自助注册。用户登录已改为 OAuth2 授权码流程，凭据校验与令牌签发由授权服务器承担；
 * 本服务只保留「撤销当前会话」与「自助注册」这两件与协议无关的职责。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class AuthApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private static final String CONFIG_KEY_REGISTER_USER = "sys.account.registerUser";

    private final AuthDirectory authDirectory;

    private final PasswordEncoder passwordEncoder;

    private final RedisTemplate<String, Object> redisTemplate;

    private final LoginMetrics loginMetrics;

    private final JwtTokenVerifier tokenVerifier;

    /**
     * 创建认证服务。
     *
     * @param authDirectory 共享库访问器
     * @param passwordEncoder 密码编码器
     * @param redisTemplate 共享会话 Redis 模板
     * @param loginMetrics 登录相关指标
     * @param tokenVerifier 令牌校验器，用于解析自包含令牌的会话标识
     */
    public AuthApplicationService(AuthDirectory authDirectory, PasswordEncoder passwordEncoder,
                                  RedisTemplate<String, Object> redisTemplate, LoginMetrics loginMetrics,
                                  JwtTokenVerifier tokenVerifier) {
        this.authDirectory = authDirectory;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.loginMetrics = loginMetrics;
        this.tokenVerifier = tokenVerifier;
    }

    /**
     * 用户登出。
     *
     * <p>兼容两种令牌：旧的对称签名令牌以原文作为会话键；自包含令牌先解析 {@code jti}，
     * 再删除对应的会话。仅当在线映射仍指向该会话时才一并清理，避免误删新会话。
     * 未携带令牌或令牌无法解析时按成功处理。
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
            revokeOnlineMapping(loginUser.get(Constants.USER_ID), token);
            redisTemplate.delete(redisKey);
            return;
        }
        String sessionId = sessionIdOf(token);
        if (sessionId == null) {
            return;
        }
        String sessionKey = Constants.LOGIN_TOKEN_KEY + sessionId;
        @SuppressWarnings("unchecked")
        Map<String, Object> session = (Map<String, Object>) redisTemplate.opsForValue().get(sessionKey);
        if (session != null && session.get(Constants.USER_ID) != null) {
            revokeOnlineMapping(session.get(Constants.USER_ID), sessionId);
        }
        redisTemplate.delete(sessionKey);
    }

    /**
     * 解析自包含令牌的会话标识。
     *
     * @param token 访问令牌
     * @return 会话标识；令牌无法解析时返回 {@code null}
     */
    private String sessionIdOf(String token) {
        try {
            JwtTokenVerifier.VerifiedToken verified = tokenVerifier.verify(token);
            return verified == null ? null : verified.claims().getId();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 在线映射仍指向该会话时清理，避免把新登录的会话误删。
     *
     * @param userId 用户 ID
     * @param sessionId 会话标识
     */
    private void revokeOnlineMapping(Object userId, String sessionId) {
        String onlineKey = Constants.ONLINE_TOKEN_KEY + userId;
        Object mappedToken = redisTemplate.opsForValue().get(onlineKey);
        if (sessionId.equals(mappedToken)) {
            redisTemplate.delete(onlineKey);
            loginMetrics.recordLogout();
        }
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
}
