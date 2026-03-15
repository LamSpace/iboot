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

package com.iboot.admin.interfaces.dto.export;

import com.iboot.admin.common.annotation.ExcelColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在线用户导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserExportVO {

    @ExcelColumn(name = "用户ID", order = 1, width = 10)
    private Long userId;

    @ExcelColumn(name = "用户名", order = 2, width = 15)
    private String username;

    @ExcelColumn(name = "昵称", order = 3, width = 15)
    private String nickname;

    @ExcelColumn(name = "部门名称", order = 4, width = 20)
    private String deptName;

    @ExcelColumn(name = "登录IP", order = 5, width = 18)
    private String loginIp;

    @ExcelColumn(name = "浏览器", order = 6, width = 20)
    private String browser;

    @ExcelColumn(name = "操作系统", order = 7, width = 20)
    private String os;

    @ExcelColumn(name = "登录时间", order = 8, width = 20)
    private String loginTime;

    @ExcelColumn(name = "角色", order = 9, width = 30)
    private String roles;
}
