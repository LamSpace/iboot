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

package com.iboot.file.infrastructure.interceptor;

import com.iboot.file.common.enums.DataScopeEnum;
import com.iboot.file.infrastructure.security.FilePermissionScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据权限 SQL 片段构造器。
 *
 * <p>把已解析的 {@link FilePermissionScope} 转换为追加到 MyBatis 查询中的过滤片段；
 * 不访问数据库，保证五级范围生成的 SQL 可被单独测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DataScopeSqlBuilder {

    /**
     * 构造数据权限 SQL 片段。
     *
     * @param scope         用户数据权限范围
     * @param userAlias     SQL 中用户表别名
     * @param deptTable     当前查询是否为部门表
     * @param currentUserId 当前用户 ID
     * @return SQL 片段；无需过滤时返回 {@code null}
     */
    public String build(FilePermissionScope scope, String userAlias, boolean deptTable, Long currentUserId) {
        if (scope == null || scope.dataScope() == null) {
            return null;
        }
        Integer dataScope = scope.dataScope();
        String deptColumn = deptTable ? "id" : "dept_id";
        if (DataScopeEnum.ALL.getCode().equals(dataScope)) {
            return null;
        }
        if (DataScopeEnum.CUSTOM.getCode().equals(dataScope)) {
            List<Long> deptIds = scope.visibleDeptIds();
            if (deptIds == null || deptIds.isEmpty()) {
                return "1=0";
            }
            return userAlias + "." + deptColumn + " IN (" + buildIdList(deptIds) + ")";
        }
        if (DataScopeEnum.DEPT.getCode().equals(dataScope)) {
            if (deptTable) {
                return scope.deptId() == null ? "1=0" : userAlias + "." + deptColumn + " = " + scope.deptId();
            }
            return userAlias + ".dept_id = (SELECT dept_id FROM sys_user WHERE id = " + currentUserId + ")";
        }
        if (DataScopeEnum.DEPT_AND_CHILD.getCode().equals(dataScope)) {
            List<Long> deptIds = scope.visibleDeptIds();
            if (deptIds == null || deptIds.isEmpty()) {
                return "1=0";
            }
            return userAlias + "." + deptColumn + " IN (" + buildIdList(deptIds) + ")";
        }
        if (DataScopeEnum.SELF.getCode().equals(dataScope)) {
            return userAlias + ".id = " + currentUserId;
        }
        return null;
    }

    /**
     * 构建 ID 列表字符串。
     *
     * @param ids ID 集合
     * @return 逗号分隔的 ID 列表
     */
    private String buildIdList(List<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(ids.get(i));
        }
        return builder.toString();
    }
}
