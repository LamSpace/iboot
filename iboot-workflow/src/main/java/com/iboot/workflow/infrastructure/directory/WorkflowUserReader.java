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

package com.iboot.workflow.infrastructure.directory;

import com.iboot.kernel.web.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 审批服务的系统用户只读适配器。
 *
 * <p>跨库读取 {@code iboot_admin.sys_user} 的四个字段，替代管理端原有的 {@code UserRepository}。
 * 数据访问失败时抛出 {@link BusinessException}（失败关闭），不允许在用户目录不可用时放行发起请求。
 *
 * <p>这是拆分过渡期的临时约定，退出条件：{@code iboot-system} 拆分后改调系统服务。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class WorkflowUserReader {

    private static final String SELECT_USER =
            "SELECT id, status, username, nickname FROM iboot_admin.sys_user WHERE id = ? AND deleted = 0";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建用户只读适配器。
     *
     * @param jdbcTemplate 本服务数据源上的只读访问模板
     */
    public WorkflowUserReader(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按用户 ID 查询未删除的用户。
     *
     * @param userId 用户 ID
     * @return 用户快照；不存在时为空
     * @throws BusinessException 用户目录不可用时抛出（失败关闭）
     */
    public Optional<WorkflowUser> findById(final Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        try {
            List<WorkflowUser> users = jdbcTemplate.query(SELECT_USER,
                    (rs, rowNum) -> new WorkflowUser(
                            rs.getLong("id"),
                            rs.getObject("status", Integer.class),
                            rs.getString("username"),
                            rs.getString("nickname")),
                    userId);
            return users.stream().findFirst();
        } catch (DataAccessException e) {
            throw new BusinessException(503, "用户目录不可用");
        }
    }
}
