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
 * 性别枚举
 *
 * @author iBoot
 */
public enum GenderEnum {

    /**
     * 未知
     */
    UNKNOWN(0, "未知"),
    /**
     * 男
     */
    MALE(1, "男"),
    /**
     * 女
     */
    FEMALE(2, "女");

    /**
     * 性别码
     */
    private final Integer code;

    /**
     * 性别描述
     */
    private final String description;

    /**
     * Creates a new {@code GenderEnum} instance.
     *
     * @param code        性别码
     * @param description 性别描述
     */
    @SuppressWarnings("all")
    private GenderEnum(final Integer code, final String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 性别码
     *
     * @return 性别枚举
     */
    public static GenderEnum getByCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (GenderEnum genderEnum : GenderEnum.values()) {
            if (genderEnum.getCode().equals(code)) {
                return genderEnum;
            }
        }
        return UNKNOWN;
    }

    /**
     * 性别码
     */
    @SuppressWarnings("all")
    public Integer getCode() {
        return this.code;
    }

    /**
     * 性别描述
     */
    @SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

}
