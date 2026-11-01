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

package com.iboot.monitor.infrastructure.directory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读的部门名称查询。
 *
 * <p>在线会话列表需要用部门 ID 翻译出部门名称，而部门数据属于系统域。跨 schema 只读
 * {@code iboot_admin.sys_dept}，行为与拆分前一致；不引入完整的部门领域模型。
 *
 * <p>退出条件：{@code iboot-system} 拆分时改为调用系统服务的部门查询接口。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DeptNameReader {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建读取器。
     *
     * @param jdbcTemplate 共享库访问模板
     */
    public DeptNameReader(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 读取全部部门 ID 到名称的映射。
     *
     * @return 部门 ID 到名称的映射；无数据时返回空 Map
     */
    public Map<Long, String> allDeptNames() {
        Map<Long, String> names = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, dept_name FROM iboot_admin.sys_dept WHERE deleted = 0");
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            Object deptName = row.get("dept_name");
            if (id == null || deptName == null) {
                continue;
            }
            names.put(Long.valueOf(id.toString()), deptName.toString());
        }
        return names;
    }
}
