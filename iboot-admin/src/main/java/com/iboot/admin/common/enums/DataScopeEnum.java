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

package com.iboot.admin.common.enums;

/**
 * 数据权限范围枚举
 *
 * @author iBoot
 */
public enum DataScopeEnum {

    /**
     * 全部数据权限
     */
    ALL(1, "全部数据权限"),
    /**
     * 自定义数据权限
     */
    CUSTOM(2, "自定义数据权限"),
    /**
     * 本部门数据权限
     */
    DEPT(3, "本部门数据权限"),
    /**
     * 本部门及以下数据权限
     */
    DEPT_AND_CHILD(4, "本部门及以下数据权限"),
    /**
     * 仅本人数据权限
     */
    SELF(5, "仅本人数据权限");

    /**
     * 权限范围码
     */
    private final Integer code;

    /**
     * 权限范围描述
     */
    private final String description;

    /**
     * Creates a new {@code DataScopeEnum} instance.
     *
     * @param code        权限范围码
     * @param description 权限范围描述
     */
    @SuppressWarnings("all")
    private DataScopeEnum(final Integer code, final String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 权限范围码
     *
     * @return 数据权限范围枚举
     */
    public static DataScopeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScopeEnum dataScopeEnum : DataScopeEnum.values()) {
            if (dataScopeEnum.getCode().equals(code)) {
                return dataScopeEnum;
            }
        }
        return null;
    }

    /**
     * 权限范围码
     */
    @SuppressWarnings("all")
    public Integer getCode() {
        return this.code;
    }

    /**
     * 权限范围描述
     */
    @SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

}
