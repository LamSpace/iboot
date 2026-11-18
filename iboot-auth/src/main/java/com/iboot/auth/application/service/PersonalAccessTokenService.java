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
import com.iboot.auth.domain.model.PersonalAccessTokenCreated;
import com.iboot.auth.domain.model.PersonalAccessTokenRecord;
import com.iboot.auth.domain.model.PersonalAccessTokenSummary;
import com.iboot.auth.infrastructure.persistence.AuthDirectory;
import com.iboot.auth.infrastructure.persistence.PersonalAccessTokenRepository;
import com.iboot.auth.infrastructure.security.AuthServerProperties;
import com.iboot.auth.infrastructure.security.PersonalAccessTokenProperties;
import com.iboot.kernel.security.Constants;
import com.iboot.kernel.web.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 个人访问令牌应用服务。
 *
 * <p>负责令牌的签发、列表与撤销：签发时读取用户、角色与权限形成快照，使用认证服务
 * 的签名密钥生成 JWT，并写入 {@code sys_user_pat} 与共享存储的 {@code pat_tokens:<jti>}；
 * 撤销时删除共享存储键并标记元数据。令牌原文只在创建结果中出现一次。
 *
 * <p>权限快照与交互式登录保持一致：超级管理员取全部权限，其他用户按角色取菜单权限。
 * 角色或权限调整不会改写已签发令牌，需要撤销并重建。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class PersonalAccessTokenService {

    private static final Logger log = LoggerFactory.getLogger(PersonalAccessTokenService.class);

    /**
     * 有效状态。
     */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /**
     * 已撤销状态。
     */
    public static final String STATUS_REVOKED = "REVOKED";

    /**
     * 已过期状态。
     */
    public static final String STATUS_EXPIRED = "EXPIRED";

    private static final int USER_STATUS_ENABLED = 1;

    private static final int MAX_NAME_LENGTH = 64;

    private static final int MAX_SCOPE_COUNT = 50;

    private final PersonalAccessTokenRepository repository;

    private final AuthDirectory authDirectory;

    private final JwtEncoder jwtEncoder;

    private final RedisTemplate<String, Object> redisTemplate;

    private final AuthServerProperties authProperties;

    private final PersonalAccessTokenProperties properties;

    private final Clock clock;

    /**
     * 创建应用服务。
     *
     * @param repository 令牌元数据仓储
     * @param authDirectory 共享库访问器
     * @param jwtEncoder JWT 编码器
     * @param redisTemplate 共享存储访问模板
     * @param authProperties 授权服务器配置
     * @param properties PAT 策略配置
     */
    @Autowired
    public PersonalAccessTokenService(PersonalAccessTokenRepository repository,
                                      AuthDirectory authDirectory,
                                      JwtEncoder jwtEncoder,
                                      RedisTemplate<String, Object> redisTemplate,
                                      AuthServerProperties authProperties,
                                      PersonalAccessTokenProperties properties) {
        this(repository, authDirectory, jwtEncoder, redisTemplate, authProperties, properties,
                Clock.systemDefaultZone());
    }

    /**
     * 创建应用服务，并注入可测试的时钟。
     *
     * @param repository 令牌元数据仓储
     * @param authDirectory 共享库访问器
     * @param jwtEncoder JWT 编码器
     * @param redisTemplate 共享存储访问模板
     * @param authProperties 授权服务器配置
     * @param properties PAT 策略配置
     * @param clock 时钟
     */
    PersonalAccessTokenService(PersonalAccessTokenRepository repository,
                               AuthDirectory authDirectory,
                               JwtEncoder jwtEncoder,
                               RedisTemplate<String, Object> redisTemplate,
                               AuthServerProperties authProperties,
                               PersonalAccessTokenProperties properties,
                               Clock clock) {
        this.repository = repository;
        this.authDirectory = authDirectory;
        this.jwtEncoder = jwtEncoder;
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 为用户签发个人访问令牌。
     *
     * <p>有效期缺省时取配置的默认值；用户必须存在、处于启用状态，且活动令牌未达上限。
     *
     * @param userId 用户 ID
     * @param name 令牌名称
     * @param expiresDays 有效期天数，可为 {@code null}
     * @param requestedScopes 请求的权限码范围；空表示继承全部权限
     * @return 新令牌，包含只出现一次的明文
     * @throws BusinessException 名称非法、有效期越界、用户不可用、数量超限或 scope 非法时抛出
     */
    public PersonalAccessTokenCreated create(Long userId, String name, Long expiresDays,
                                             List<String> requestedScopes) {
        String normalizedName = normalizeName(name);
        long days = resolveDays(expiresDays);
        AuthUser user = authDirectory.findUserById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.status() != USER_STATUS_ENABLED) {
            throw new BusinessException("用户已停用，无法签发访问令牌");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (repository.countActive(userId, now) >= properties.getMaxPerUser()) {
            throw new BusinessException("活动令牌数量已达上限（" + properties.getMaxPerUser()
                    + "），请先撤销不再使用的令牌");
        }

        List<String> roles = authDirectory.findRoleCodesByRoleIds(user.roleIds());
        List<String> availablePermissions = resolvePermissions(user.roleIds());
        List<String> scopes = normalizeScopes(requestedScopes, availablePermissions);
        List<String> permissions = scopes.isEmpty() ? availablePermissions : scopes;
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(days, ChronoUnit.DAYS);
        String tokenId = UUID.randomUUID().toString();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                claims(user, roles, permissions, scopes, tokenId, issuedAt, expiresAt))).getTokenValue();
        LocalDateTime expires = LocalDateTime.ofInstant(expiresAt, clock.getZone());

        repository.insert(userId, normalizedName, tokenId, scopesCsv(scopes), expires);
        try {
            writeRevocationRecord(tokenId, userId, normalizedName, issuedAt, expiresAt);
        } catch (RuntimeException e) {
            log.error("写入 PAT 撤销记录失败：userId={}", userId, e);
            repository.findByTokenId(tokenId)
                    .ifPresent(row -> repository.revoke(row.id(), userId, LocalDateTime.now(clock)));
            throw new BusinessException("签发访问令牌失败，请稍后重试");
        }

        PersonalAccessTokenRecord record = repository.findByTokenId(tokenId)
                .orElseThrow(() -> new BusinessException("签发访问令牌失败，请稍后重试"));
        return new PersonalAccessTokenCreated(record.id(), record.name(), record.scopes(), token,
                record.createTime(), record.expiresAt());
    }

    /**
     * 列出用户全部令牌元数据。
     *
     * <p>状态由元数据与共享存储共同判定：记录已撤销或撤销键缺失为已撤销，过期时间为
     * 已过期，其余为有效。最近使用时间读取共享存储，缺失时为 {@code null}。
     *
     * @param userId 用户 ID
     * @return 令牌列表，按创建时间倒序
     */
    public List<PersonalAccessTokenSummary> list(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PersonalAccessTokenRecord> records = repository.findByUserId(userId);
        List<PersonalAccessTokenSummary> summaries = new ArrayList<>(records.size());
        for (PersonalAccessTokenRecord record : records) {
            boolean validKey = Boolean.TRUE.equals(
                    redisTemplate.hasKey(Constants.PAT_TOKEN_KEY + record.tokenId()));
            summaries.add(new PersonalAccessTokenSummary(record.id(), record.name(), record.scopes(),
                    status(record, now, validKey), record.createTime(), record.expiresAt(),
                    lastUsedAt(record.tokenId())));
        }
        return summaries;
    }

    /**
     * 撤销用户自己的一条令牌。
     *
     * <p>先删除共享存储中的撤销记录使请求立即失效，再标记元数据；重复撤销按成功处理。
     *
     * @param userId 用户 ID
     * @param id 记录 ID
     * @throws BusinessException 令牌不存在或不属于该用户时抛出
     */
    public void revoke(Long userId, Long id) {
        PersonalAccessTokenRecord record = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException("令牌不存在"));
        redisTemplate.delete(Constants.PAT_TOKEN_KEY + record.tokenId());
        redisTemplate.delete(Constants.PAT_LAST_USED_KEY + record.tokenId());
        repository.revoke(id, userId, LocalDateTime.now(clock));
    }

    /**
     * 构造令牌声明。
     *
     * @param user 用户
     * @param roles 角色编码
     * @param permissions 有效权限码
     * @param scopes 权限范围；空列表表示继承全部权限
     * @param tokenId 令牌标识
     * @param issuedAt 签发时间
     * @param expiresAt 过期时间
     * @return 声明集合
     */
    private JwtClaimsSet claims(AuthUser user, List<String> roles, List<String> permissions,
                                List<String> scopes, String tokenId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(authProperties.getIssuer())
                .subject(String.valueOf(user.id()))
                .id(tokenId)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("userId", user.id())
                .claim("username", user.username())
                .claim("nickname", user.nickname())
                .claim(Constants.ROLES, roles)
                .claim(Constants.PERMISSIONS, permissions)
                .claim(Constants.PAT_CLAIM, true);
        if (!scopes.isEmpty()) {
            builder.claim(Constants.PAT_SCOPES_CLAIM, scopes);
        }
        if (user.deptId() != null) {
            builder.claim("deptId", user.deptId());
        }
        return builder.build();
    }

    /**
     * 写入共享存储中的 PAT 撤销记录，TTL 与令牌剩余有效期一致。
     *
     * @param tokenId 令牌标识
     * @param userId 用户 ID
     * @param name 令牌名称
     * @param issuedAt 签发时间
     * @param expiresAt 过期时间
     */
    private void writeRevocationRecord(String tokenId, Long userId, String name,
                                       Instant issuedAt, Instant expiresAt) {
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.USER_ID, userId);
        record.put("name", name);
        Duration ttl = Duration.between(issuedAt, expiresAt);
        redisTemplate.opsForValue().set(Constants.PAT_TOKEN_KEY + tokenId, record, ttl);
    }

    /**
     * 计算令牌状态。
     *
     * @param record 元数据
     * @param now 当前时间
     * @param validKey 撤销记录是否存在
     * @return 状态
     */
    private String status(PersonalAccessTokenRecord record, LocalDateTime now, boolean validKey) {
        if (record.revoked() || !validKey) {
            return STATUS_REVOKED;
        }
        if (!record.expiresAt().isAfter(now)) {
            return STATUS_EXPIRED;
        }
        return STATUS_ACTIVE;
    }

    /**
     * 读取最近使用时间。
     *
     * @param tokenId 令牌标识
     * @return 最近使用时间；未记录或格式非法时返回 {@code null}
     */
    private LocalDateTime lastUsedAt(String tokenId) {
        Object value = redisTemplate.opsForValue().get(Constants.PAT_LAST_USED_KEY + tokenId);
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(value.toString()), clock.getZone());
        } catch (DateTimeParseException e) {
            log.debug("解析 PAT 最近使用时间失败：{}", value);
            return null;
        }
    }

    /**
     * 解析用户权限，超级管理员取全部权限。
     *
     * @param roleIds 角色 ID 列表
     * @return 权限码列表
     */
    private List<String> resolvePermissions(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        for (Long roleId : roleIds) {
            if (authDirectory.findRoleCode(roleId)
                    .filter(Constants.ROLE_ADMIN_CODE::equals).isPresent()) {
                return new ArrayList<>(authDirectory.findAllPermissions());
            }
        }
        return new ArrayList<>(authDirectory.findPermissionsByRoleIds(roleIds));
    }

    /**
     * 规范化并校验请求的权限范围。
     *
     * <p>每个权限码都必须属于用户当前权限；重复项按首次出现顺序去重。
     *
     * @param requested 请求的权限码，可为 {@code null}
     * @param available 用户当前权限
     * @return 规范化后的权限码列表；未请求时返回空列表
     * @throws BusinessException 权限码为空、不在用户权限内或超过数量上限时抛出
     */
    private List<String> normalizeScopes(List<String> requested, List<String> available) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String scope : requested) {
            if (scope == null || scope.isBlank()) {
                throw new BusinessException("scope 不能为空");
            }
            String value = scope.trim();
            if (!available.contains(value)) {
                throw new BusinessException("scope 不在用户权限范围内：" + value);
            }
            unique.add(value);
        }
        if (unique.size() > MAX_SCOPE_COUNT) {
            throw new BusinessException("scope 数量不能超过 " + MAX_SCOPE_COUNT);
        }
        return List.copyOf(unique);
    }

    /**
     * 把权限范围转换为数据库列值。
     *
     * @param scopes 权限码列表
     * @return 逗号分隔的列值；空列表返回 {@code null}
     */
    private String scopesCsv(List<String> scopes) {
        return scopes.isEmpty() ? null : String.join(",", scopes);
    }

    /**
     * 校验并规范化令牌名称。
     *
     * @param name 原始名称
     * @return 去除首尾空白后的名称
     * @throws BusinessException 名称为空或超长时抛出
     */
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("令牌名称不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("令牌名称长度不能超过 " + MAX_NAME_LENGTH + " 个字符");
        }
        return trimmed;
    }

    /**
     * 解析并校验有效期。
     *
     * @param expiresDays 请求的有效期，可为 {@code null}
     * @return 有效天数
     * @throws BusinessException 超出允许范围时抛出
     */
    private long resolveDays(Long expiresDays) {
        long days = expiresDays == null ? properties.getDefaultDays() : expiresDays;
        if (days < 1 || days > properties.getMaxDays()) {
            throw new BusinessException("有效期必须在 1 到 " + properties.getMaxDays() + " 天之间");
        }
        return days;
    }
}
