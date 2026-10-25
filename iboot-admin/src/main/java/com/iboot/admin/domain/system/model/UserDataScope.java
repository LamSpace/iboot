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

package com.iboot.admin.domain.system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iboot.admin.common.enums.DataScopeEnum;

import java.util.List;

/**
 * 用户数据权限范围。
 *
 * <p>由 {@code UserDataScopeResolver} 按用户所有角色计算得出，供 MyBatis 数据权限拦截器
 * 生成 SQL 过滤条件，也供只读 scope 接口向其他服务暴露当前用户的可见范围。
 *
 * <p>{@code visibleDeptIds} 是数据权限过滤使用的部门集合：“全部”与“仅本人”不依赖部门集合，
 * 故为空列表。{@code kbVisibleDeptIds} 是知识库共享判断使用的部门集合：“全部”与“仅本人”
 * 会展开为用户部门及其下级，供知识库可见范围计算使用。
 *
 * @param dataScope        数据权限码，取值见 {@link DataScopeEnum}；用户无角色时可为 {@code null}
 * @param deptId           用户所在部门 ID，可为 {@code null}
 * @param visibleDeptIds   数据权限过滤使用的可见部门集合，不为 {@code null}
 * @param kbVisibleDeptIds 知识库共享判断使用的部门集合，不为 {@code null}
 * @param superAdmin       是否为超级管理员；超级管理员跳过数据权限过滤
 * @author iBoot Team
 * @since 1.0.0
 */
public record UserDataScope(Integer dataScope, Long deptId, List<Long> visibleDeptIds,
                            List<Long> kbVisibleDeptIds, boolean superAdmin) {

    /**
     * 规范化集合字段，避免调用方处理 {@code null}。
     */
    public UserDataScope {
        visibleDeptIds = visibleDeptIds == null ? List.of() : List.copyOf(visibleDeptIds);
        kbVisibleDeptIds = kbVisibleDeptIds == null ? List.of() : List.copyOf(kbVisibleDeptIds);
    }

    /**
     * 判断是否为全部数据权限。
     *
     * @return 数据权限码为“全部”时返回 {@code true}
     */
    @JsonIgnore
    public boolean isAll() {
        return DataScopeEnum.ALL.getCode().equals(dataScope);
    }
}
