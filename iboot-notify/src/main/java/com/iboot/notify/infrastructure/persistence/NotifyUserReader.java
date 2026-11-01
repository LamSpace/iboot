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

package com.iboot.notify.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 只读的用户信息访问器。
 *
 * <p>消息与通知只需要用户的标识、邮箱与手机号：全量发送时枚举用户 ID，外部渠道投递时取收件地址。
 * 直接读管理端 schema 的 {@code iboot_admin.sys_user}，不引入完整的用户领域模型。
 *
 * <p>退出条件：{@code iboot-system} 拆分时改为调用系统服务的用户查询接口。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class NotifyUserReader {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建读取器。
     *
     * @param jdbcTemplate 共享库访问模板
     */
    public NotifyUserReader(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询全部未删除用户的 ID，按创建时间倒序，与管理端 {@code UserRepository#findAll} 的顺序一致。
     *
     * @return 用户 ID 列表
     */
    public List<Long> findAllUserIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM iboot_admin.sys_user WHERE deleted = 0 ORDER BY create_time DESC", Long.class);
    }

    /**
     * 按 ID 查询用户的通知相关字段。
     *
     * @param id 用户 ID
     * @return 用户信息；不存在或已删除时为空
     */
    public Optional<NotifyUser> findById(final Long id) {
        List<NotifyUser> users = jdbcTemplate.query(
                "SELECT id, username, nickname, email, phone FROM iboot_admin.sys_user WHERE id = ? AND deleted = 0",
                (rs, rowNum) -> new NotifyUser(rs.getLong("id"), rs.getString("username"),
                        rs.getString("nickname"), rs.getString("email"), rs.getString("phone")),
                id);
        return users.stream().findFirst();
    }

    /**
     * 通知域所需的用户最小视图。
     *
     * @param id 用户 ID
     * @param username 用户名
     * @param nickname 昵称
     * @param email 邮箱
     * @param phone 手机号
     * @author iBoot Team
     * @since 1.0.0
     */
    public record NotifyUser(Long id, String username, String nickname, String email, String phone) {
    }
}
