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
 * 岗位导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostExportVO {

    @ExcelColumn(name = "岗位ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "岗位编码", order = 2, width = 15)
    private String postCode;

    @ExcelColumn(name = "岗位名称", order = 3, width = 20)
    private String postName;

    @ExcelColumn(name = "排序", order = 4, width = 8)
    private Integer orderNum;

    @ExcelColumn(name = "状态", order = 5, width = 8, dictType = "sys_normal_disable")
    private Integer status;

    @ExcelColumn(name = "备注", order = 6, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 7, width = 20)
    private LocalDateTime createTime;
}
