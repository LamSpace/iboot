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

package com.iboot.system.application.service;

import com.iboot.kernel.security.Constants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 个人访问令牌的批量撤销器。
 *
 * <p>用户停用或删除时，系统域在同一事务内撤销该用户的全部 PAT：先删除共享存储中的
 * {@code pat_tokens:<jti>} 与 {@code pat_last_used:<jti>} 使请求立即失效，再标记
 * {@code sys_user_pat} 元数据。Redis 删除失败时抛出异常，让用户状态变更回滚，
 * 避免出现“已停用但令牌仍可用”的窗口。
 *
 * <p>本类直连共享库与共享存储，是拆分完成前的过渡实现；退出条件是在系统域与认证域
 * 分离后改为事件或内部接口。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Service
public class PersonalAccessTokenRevoker {

    private final JdbcTemplate jdbcTemplate;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建撤销器。
     *
     * @param jdbcTemplate 主数据源访问模板
     * @param redisTemplate 共享存储访问模板
     */
    public PersonalAccessTokenRevoker(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate,
                                      @Qualifier("redisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 撤销用户全部未撤销的个人访问令牌。
     *
     * @param userId 用户 ID
     */
    public void revokeAll(Long userId) {
        List<String> tokenIds = jdbcTemplate.queryForList(
                "SELECT token_id FROM sys_user_pat WHERE user_id = ? AND revoked = 0",
                String.class, userId);
        if (tokenIds.isEmpty()) {
            return;
        }
        List<String> keys = new ArrayList<>(tokenIds.size() * 2);
        for (String tokenId : tokenIds) {
            keys.add(Constants.PAT_TOKEN_KEY + tokenId);
            keys.add(Constants.PAT_LAST_USED_KEY + tokenId);
        }
        redisTemplate.delete(keys);
        jdbcTemplate.update(
                "UPDATE sys_user_pat SET revoked = 1, revoked_at = NOW() "
                        + "WHERE user_id = ? AND revoked = 0",
                userId);
    }
}
