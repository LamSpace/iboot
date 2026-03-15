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

import java.time.LocalDateTime;

/**
 * 登录日志导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLogExportVO {

    @ExcelColumn(name = "日志ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "用户名", order = 2, width = 15)
    private String username;

    @ExcelColumn(name = "登录IP", order = 3, width = 18)
    private String ipAddress;

    @ExcelColumn(name = "登录地点", order = 4, width = 20)
    private String loginLocation;

    @ExcelColumn(name = "浏览器", order = 5, width = 20)
    private String browser;

    @ExcelColumn(name = "操作系统", order = 6, width = 20)
    private String os;

    @ExcelColumn(name = "登录状态", order = 7, width = 10, dictType = "sys_common_status")
    private Integer status;

    @ExcelColumn(name = "提示消息", order = 8, width = 30)
    private String msg;

    @ExcelColumn(name = "登录时间", order = 9, width = 20)
    private LocalDateTime loginTime;
}
