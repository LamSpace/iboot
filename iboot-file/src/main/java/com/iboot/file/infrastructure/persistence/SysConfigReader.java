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

package com.iboot.file.infrastructure.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 只读的系统参数访问器。
 *
 * <p>直接读取管理端 schema 的 {@code iboot_admin.sys_config}：文件的大小上限与允许扩展名
 * 仍由管理端的「参数配置」页维护，改成 Nacos 会让该页面失效，属于用户可见的行为变化。
 * 本服务的数据源已是独立的 {@code iboot_file}，MySQL 没有跨库搜索路径，故显式限定库名。
 *
 * <p>退出条件：{@code iboot-system} 拆分时改为调用系统服务的配置查询接口。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class SysConfigReader {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建读取器。
     *
     * @param jdbcTemplate 共享库访问模板
     */
    public SysConfigReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按键读取配置值。
     *
     * @param configKey 配置键
     * @return 配置值；不存在时返回 {@code null}
     */
    public String getString(String configKey) {
        List<String> values = jdbcTemplate.queryForList(
                "SELECT config_value FROM iboot_admin.sys_config WHERE config_key = ? AND deleted = 0 LIMIT 1",
                String.class, configKey);
        return values.isEmpty() ? null : values.get(0);
    }
}
