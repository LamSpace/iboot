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

package com.iboot.file.infrastructure.security;

import java.util.List;

/**
 * 当前用户的数据权限范围快照。
 *
 * <p>由管理端的只读 scope 接口提供。{@code dataScope} 为空表示用户没有可用角色，
 * 调用方必须按失败关闭处理。
 *
 * @param dataScope 五级数据权限码
 * @param deptId 用户所在部门 ID
 * @param visibleDeptIds 数据权限过滤使用的可见部门集合
 * @param superAdmin 是否超级管理员
 * @author iBoot Team
 * @since 1.0.0
 */
public record FilePermissionScope(Integer dataScope, Long deptId, List<Long> visibleDeptIds, boolean superAdmin) {

    /**
     * 全部数据权限码，与 {@code DataScopeEnum.ALL} 一致。
     */
    private static final int ALL_SCOPE = 1;

    /**
     * 规范化集合字段，避免调用方处理 {@code null}。
     */
    public FilePermissionScope {
        visibleDeptIds = visibleDeptIds == null ? List.of() : List.copyOf(visibleDeptIds);
    }

    /**
     * 是否拥有全部数据权限。
     *
     * @return 权限码为「全部数据」时返回 {@code true}
     */
    public boolean isAll() {
        return dataScope != null && dataScope == ALL_SCOPE;
    }
}
