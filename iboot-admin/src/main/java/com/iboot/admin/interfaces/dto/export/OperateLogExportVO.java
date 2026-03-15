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
 * 操作日志导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperateLogExportVO {

    @ExcelColumn(name = "日志ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "模块标题", order = 2, width = 15)
    private String title;

    @ExcelColumn(name = "业务类型", order = 3, width = 10, dictType = "sys_oper_type")
    private Integer businessType;

    @ExcelColumn(name = "请求方式", order = 4, width = 10)
    private String requestMethod;

    @ExcelColumn(name = "操作人员", order = 5, width = 15)
    private String operatorName;

    @ExcelColumn(name = "部门名称", order = 6, width = 15)
    private String deptName;

    @ExcelColumn(name = "请求URL", order = 7, width = 40)
    private String operUrl;

    @ExcelColumn(name = "操作IP", order = 8, width = 18)
    private String operIp;

    @ExcelColumn(name = "操作地点", order = 9, width = 20)
    private String operLocation;

    @ExcelColumn(name = "操作状态", order = 10, width = 10, dictType = "sys_common_status")
    private Integer status;

    @ExcelColumn(name = "耗时(ms)", order = 11, width = 12)
    private Long costTime;

    @ExcelColumn(name = "操作时间", order = 12, width = 20)
    private LocalDateTime operTime;
}
