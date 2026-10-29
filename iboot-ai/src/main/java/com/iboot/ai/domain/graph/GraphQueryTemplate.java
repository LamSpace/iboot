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
 * 受支持的图查询模板。
 *
 * <p>模型只能从这些固定模板中选择，不能提供自由 Cypher。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum GraphQueryTemplate {

    /**
     * 同部门同事：2 跳。
     */
    COLLEAGUES("colleagues"),

    /**
     * 部门成员，包含全部下级部门。
     */
    DEPT_MEMBERS("dept_members"),

    /**
     * 某用户的角色与菜单权限。
     */
    USER_PERMISSIONS("user_permissions"),

    /**
     * 持有某菜单权限的用户：3 跳。
     */
    PERM_HOLDERS("perm_holders"),

    /**
     * 两个部门之间的组织路径。
     */
    DEPT_PATH("dept_path"),

    /**
     * 按名称解析图节点，返回查询所需的主键或权限码。
     */
    SEARCH_NODES("search_nodes");

    private final String wireName;

    GraphQueryTemplate(String wireName) {
        this.wireName = wireName;
    }

    /**
     * 返回工具参数中使用的名称。
     *
     * @return 模板名
     */
    public String wireName() {
        return wireName;
    }

    /**
     * 按工具参数中的名称解析模板。
     *
     * @param name 模板名
     * @return 对应模板
     * @throws IllegalArgumentException 名称缺失或未知时抛出
     */
    public static GraphQueryTemplate fromWire(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("缺少查询模板");
        }
        for (GraphQueryTemplate template : values()) {
            if (template.wireName.equals(name)) {
                return template;
            }
        }
        throw new IllegalArgumentException("不支持的查询模板：" + name);
    }
}
