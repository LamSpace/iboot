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

package com.iboot.ai.infrastructure.graph;

import com.iboot.ai.application.service.graph.GraphScopeUnavailableException;
import com.iboot.ai.domain.graph.QueryScope;
import com.iboot.ai.infrastructure.security.PermissionScope;
import com.iboot.ai.infrastructure.security.PermissionScopeClient;
import com.iboot.ai.infrastructure.security.PermissionScopeUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 把调用者的数据权限范围转换为图查询范围。
 *
 * <p>范围服务不可用时按失败关闭处理：抛出 {@link GraphScopeUnavailableException}，
 * 绝不以“不过滤”的结果兜底。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class GraphScopeResolver {

    private final PermissionScopeClient scopeClient;

    /**
     * 创建解析器。
     *
     * @param scopeClient 数据权限范围客户端
     */
    public GraphScopeResolver(PermissionScopeClient scopeClient) {
        this.scopeClient = scopeClient;
    }

    /**
     * 解析调用者的图查询范围。
     *
     * @param userId 调用者用户 ID
     * @param token  调用者令牌
     * @return 查询范围
     * @throws GraphScopeUnavailableException 范围不可用时抛出
     */
    public QueryScope resolve(long userId, String token) {
        try {
            PermissionScope scope = scopeClient.fetchScope(userId, token);
            return new QueryScope(scope.superAdmin(), scope.visibleDeptIds());
        } catch (PermissionScopeUnavailableException e) {
            throw new GraphScopeUnavailableException("数据权限范围不可用，已拒绝图查询", e);
        }
    }
}
