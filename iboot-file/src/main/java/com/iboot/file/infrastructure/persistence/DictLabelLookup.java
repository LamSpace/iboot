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
 * 只读的字典标签查询。
 *
 * <p>Excel 导出需要把字典值翻译成标签（例如文件分类）。直接读管理端 schema 的
 * {@code iboot_admin.sys_dict_data}，与管理端 {@code DictApplicationService#getDictLabel} 的语义保持一致：
 * 查不到时返回空串。显式限定库名的原因见 {@link SysConfigReader}，退出条件相同。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DictLabelLookup {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建查询器。
     *
     * @param jdbcTemplate 共享库访问模板
     */
    public DictLabelLookup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 按字典类型与键值查询标签。
     *
     * @param dictType 字典类型
     * @param dictValue 字典键值
     * @return 字典标签；不存在时返回空串
     */
    public String labelOf(String dictType, String dictValue) {
        List<String> labels = jdbcTemplate.queryForList(
                "SELECT dict_label FROM iboot_admin.sys_dict_data WHERE dict_type = ? AND dict_value = ? AND deleted = 0 LIMIT 1",
                String.class, dictType, dictValue);
        return labels.isEmpty() ? "" : labels.get(0);
    }
}
