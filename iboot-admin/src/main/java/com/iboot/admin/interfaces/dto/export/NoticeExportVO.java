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
 * 公告导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeExportVO {

    @ExcelColumn(name = "公告ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "公告标题", order = 2, width = 30)
    private String noticeTitle;

    @ExcelColumn(name = "公告类型", order = 3, width = 12, dictType = "sys_notice_type")
    private String noticeType;

    @ExcelColumn(name = "公告内容", order = 4, width = 50)
    private String noticeContent;

    @ExcelColumn(name = "状态", order = 5, width = 10, dictType = "sys_notice_status")
    private String status;

    @ExcelColumn(name = "是否置顶", order = 6, width = 10)
    private Integer isTop;

    @ExcelColumn(name = "创建人", order = 7, width = 15)
    private String createBy;

    @ExcelColumn(name = "备注", order = 8, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 9, width = 20)
    private LocalDateTime createTime;
}
