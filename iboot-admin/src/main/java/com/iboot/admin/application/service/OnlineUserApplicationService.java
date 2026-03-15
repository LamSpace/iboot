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
import com.iboot.admin.domain.system.model.Dept;
import com.iboot.admin.domain.system.repository.DeptRepository;
import com.iboot.admin.interfaces.dto.response.OnlineUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线用户应用服务
 * <p>
 * 提供在线用户列表查询、强制退出用户、统计在线人数等功能
 * 用户登录信息存储在 Redis 中，通过令牌进行会话管理
 * </p>
 *
 * @author iBoot
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserApplicationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeptRepository deptRepository;

    /**
     * 获取所有在线用户列表
     * <p>
     * 从 Redis 中扫描所有登录令牌，解析用户登录信息，支持按用户名和登录 IP 模糊搜索
     * </p>
     *
     * @param username 用户名（可选，支持模糊搜索）
     * @param loginIp 登录 IP 地址（可选，支持模糊搜索）
     * @return 在线用户响应列表，按登录时间倒序排列
     */
    public List<OnlineUserResponse> getOnlineUsers(String username, String loginIp) {
        Set<String> keys = redisTemplate.keys(Constants.LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<OnlineUserResponse> onlineUsers = new ArrayList<>();
        // 预加载部门数据用于名称翻译
        Map<Long, String> deptNameMap = buildDeptNameMap();

        for (String key : keys) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> loginUser = (Map<String, Object>) redisTemplate.opsForValue().get(key);
                if (loginUser == null) {
                    continue;
                }

                String tokenSuffix = key.substring(Constants.LOGIN_TOKEN_KEY.length());
                OnlineUserResponse user = convertToResponse(loginUser, tokenSuffix, deptNameMap);

                // 按条件过滤
                if (username != null && !username.isBlank()
                        && (user.getUsername() == null || !user.getUsername().toLowerCase().contains(username.toLowerCase()))) {
                    continue;
                }
                if (loginIp != null && !loginIp.isBlank()
                        && (user.getLoginIp() == null || !user.getLoginIp().contains(loginIp))) {
                    continue;
                }

                onlineUsers.add(user);
            } catch (Exception e) {
                log.warn("解析在线用户信息失败，key: {}", key, e);
            }
        }

        // 按登录时间倒序排列
        onlineUsers.sort((a, b) -> {
            if (a.getLoginTime() == null) return 1;
            if (b.getLoginTime() == null) return -1;
            return b.getLoginTime().compareTo(a.getLoginTime());
        });

        return onlineUsers;
    }

    /**
     * 强制退出在线用户
     * <p>
     * 根据脱敏的令牌 ID 查找完整的 Redis 键，删除用户会话缓存
     * 同时清理 online_token 映射关系
     * </p>
     *
     * @param tokenId 脱敏后的令牌 ID
     * @throws BusinessException 当用户会话不存在或已过期时抛出
     */
    public void forceLogout(String tokenId) {
        // 根据脱敏的 tokenId 查找完整 key
        Set<String> keys = redisTemplate.keys(Constants.LOGIN_TOKEN_KEY + "*");
        if (keys == null || keys.isEmpty()) {
            throw new BusinessException("用户会话不存在或已过期");
        }

        String targetKey = null;
        for (String key : keys) {
            String token = key.substring(Constants.LOGIN_TOKEN_KEY.length());
            if (maskToken(token).equals(tokenId)) {
                targetKey = key;
                break;
            }
        }

        if (targetKey == null) {
            throw new BusinessException("用户会话不存在或已过期");
        }

        // 获取用户 ID 用于清理 online_token 映射
        @SuppressWarnings("unchecked")
        Map<String, Object> loginUser = (Map<String, Object>) redisTemplate.opsForValue().get(targetKey);
        if (loginUser != null && loginUser.get(Constants.USER_ID) != null) {
            String fullToken = targetKey.substring(Constants.LOGIN_TOKEN_KEY.length());
            String onlineKey = Constants.ONLINE_TOKEN_KEY + loginUser.get(Constants.USER_ID);
            Object mappedToken = redisTemplate.opsForValue().get(onlineKey);
            if (fullToken.equals(mappedToken)) {
                redisTemplate.delete(onlineKey);
            }
        }

        redisTemplate.delete(targetKey);
        log.info("强制退出在线用户，tokenId: {}", tokenId);
    }

    /**
     * 获取在线用户总数
     * <p>
     * 统计 Redis 中存储的登录令牌数量
     * </p>
     *
     * @return 在线用户总数
     */
    public long getOnlineUserCount() {
        Set<String> keys = redisTemplate.keys(Constants.LOGIN_TOKEN_KEY + "*");
        return keys == null ? 0 : keys.size();
    }

    /**
     * 将 Redis 中的登录用户信息转换为响应 DTO
     *
     * @param loginUser Redis 中存储的登录用户信息 Map
     * @param token 登录令牌
     * @param deptNameMap 部门 ID 到名称的映射
     * @return 在线用户响应对象
     */
    private OnlineUserResponse convertToResponse(Map<String, Object> loginUser, String token, Map<Long, String> deptNameMap) {
        Long userId = loginUser.get(Constants.USER_ID) != null
                ? Long.valueOf(loginUser.get(Constants.USER_ID).toString()) : null;
        String uname = loginUser.get("username") != null ? loginUser.get("username").toString() : null;
        String nickname = loginUser.get("nickname") != null ? loginUser.get("nickname").toString() : null;
        String lip = loginUser.get("loginIp") != null ? loginUser.get("loginIp").toString() : null;
        String browser = loginUser.get("browser") != null ? loginUser.get("browser").toString() : null;
        String os = loginUser.get("os") != null ? loginUser.get("os").toString() : null;
        String loginTime = loginUser.get("loginTime") != null ? loginUser.get("loginTime").toString() : null;

        Long deptId = null;
        if (loginUser.get("deptId") != null) {
            try {
                deptId = Long.valueOf(loginUser.get("deptId").toString());
            } catch (NumberFormatException ignored) {
            }
        }

        String deptName = deptId != null ? deptNameMap.getOrDefault(deptId, "") : "";

        @SuppressWarnings("unchecked")
        List<String> roles = loginUser.get(Constants.ROLES) instanceof List
                ? (List<String>) loginUser.get(Constants.ROLES) : Collections.emptyList();

        return OnlineUserResponse.builder()
                .tokenId(maskToken(token))
                .userId(userId)
                .username(uname)
                .nickname(nickname)
                .deptId(deptId)
                .deptName(deptName)
                .loginIp(lip)
                .browser(browser)
                .os(os)
                .loginTime(loginTime)
                .roles(roles)
                .build();
    }

    /**
     * 令牌脱敏：保留前 8 位和后 8 位，中间用 **** 替代
     *
     * @param token 原始令牌
     * @return 脱敏后的令牌
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 16) {
            return token;
        }
        return token.substring(0, 8) + "****" + token.substring(token.length() - 8);
    }

    /**
     * 构建部门 ID 到名称的映射
     *
     * @return 部门 ID 到名称的 Map
     */
    private Map<Long, String> buildDeptNameMap() {
        try {
            List<Dept> depts = deptRepository.findAll();
            return depts.stream()
                    .filter(d -> d.getId() != null && d.getDeptName() != null)
                    .collect(Collectors.toMap(Dept::getId, Dept::getDeptName, (a, b) -> a));
        } catch (Exception e) {
            log.warn("加载部门数据失败", e);
            return Collections.emptyMap();
        }
    }
}
