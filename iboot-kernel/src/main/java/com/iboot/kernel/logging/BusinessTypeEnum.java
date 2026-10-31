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

package com.iboot.kernel.logging;

/**
 * 业务操作类型枚举。
 *
 * <p>用于标识操作日志中记录的业务动作，与
 * {@link com.iboot.kernel.logging.Log#businessType()} 配合使用。
 * {@code code} 持久化到数据库，{@code description} 用于展示和日志文本。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see com.iboot.kernel.logging.Log
 */
public enum BusinessTypeEnum {

    /**
     * 其他未分类的业务操作。
     */
    OTHER(0, "其它"),
    /**
     * 新增数据。
     */
    INSERT(1, "新增"),
    /**
     * 修改数据。
     */
    UPDATE(2, "修改"),
    /**
     * 删除数据。
     */
    DELETE(3, "删除"),
    /**
     * 授权操作，例如分配角色或权限。
     */
    GRANT(4, "授权"),
    /**
     * 导出数据。
     */
    EXPORT(5, "导出"),
    /**
     * 导入数据。
     */
    IMPORT(6, "导入"),
    /**
     * 强制退出在线用户。
     */
    FORCE_LOGOUT(7, "强退"),
    /**
     * 清空业务数据。
     */
    CLEAN(8, "清空");

    /**
     * 类型码
     */
    private final Integer code;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * Creates a new {@code BusinessTypeEnum} instance.
     *
     * @param code        类型码
     * @param description 类型描述
     */
    @SuppressWarnings("all")
    private BusinessTypeEnum(final Integer code, final String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 类型码
     */
    @SuppressWarnings("all")
    public Integer getCode() {
        return this.code;
    }

    /**
     * 类型描述
     */
    @SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

}
