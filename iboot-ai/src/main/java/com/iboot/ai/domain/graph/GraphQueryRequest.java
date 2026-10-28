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

package com.iboot.ai.domain.graph;

/**
 * 一次图查询请求。
 *
 * <p>只承载模板与查询参数，不包含调用者身份；身份与数据范围由调用方单独提供。
 *
 * @param template   查询模板
 * @param userId     目标用户主键
 * @param deptId     目标部门主键
 * @param permission 目标权限标识
 * @param fromDeptId 路径起点部门
 * @param toDeptId   路径终点部门
 * @author iBoot Team
 * @since 1.0.0
 */
public record GraphQueryRequest(GraphQueryTemplate template, Long userId, Long deptId, String permission,
                                Long fromDeptId, Long toDeptId) {

    /**
     * 构造同部门同事查询。
     *
     * @param userId 目标用户
     * @return 查询请求
     */
    public static GraphQueryRequest colleagues(Long userId) {
        return new GraphQueryRequest(GraphQueryTemplate.COLLEAGUES, userId, null, null, null, null);
    }

    /**
     * 构造部门成员查询。
     *
     * @param deptId 目标部门
     * @return 查询请求
     */
    public static GraphQueryRequest deptMembers(Long deptId) {
        return new GraphQueryRequest(GraphQueryTemplate.DEPT_MEMBERS, null, deptId, null, null, null);
    }

    /**
     * 构造用户权限查询。
     *
     * @param userId 目标用户
     * @return 查询请求
     */
    public static GraphQueryRequest userPermissions(Long userId) {
        return new GraphQueryRequest(GraphQueryTemplate.USER_PERMISSIONS, userId, null, null, null, null);
    }

    /**
     * 构造权限持有人查询。
     *
     * @param permission 权限标识
     * @return 查询请求
     */
    public static GraphQueryRequest permHolders(String permission) {
        return new GraphQueryRequest(GraphQueryTemplate.PERM_HOLDERS, null, null, permission, null, null);
    }

    /**
     * 构造跨部门路径查询。
     *
     * @param fromDeptId 起点部门
     * @param toDeptId   终点部门
     * @return 查询请求
     */
    public static GraphQueryRequest deptPath(Long fromDeptId, Long toDeptId) {
        return new GraphQueryRequest(GraphQueryTemplate.DEPT_PATH, null, null, null, fromDeptId, toDeptId);
    }
}
