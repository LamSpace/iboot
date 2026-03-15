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
 * 消息导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageExportVO {

    @ExcelColumn(name = "消息ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "消息标题", order = 2, width = 30)
    private String title;

    @ExcelColumn(name = "消息内容", order = 3, width = 50)
    private String content;

    @ExcelColumn(name = "消息类型", order = 4, width = 12, dictType = "sys_message_type")
    private String messageType;

    @ExcelColumn(name = "优先级", order = 5, width = 10, dictType = "sys_message_priority")
    private String priority;

    @ExcelColumn(name = "接收类型", order = 6, width = 12, dictType = "sys_message_receiver_type")
    private String receiverType;

    @ExcelColumn(name = "状态", order = 7, width = 10, dictType = "sys_message_status")
    private String status;

    @ExcelColumn(name = "创建人", order = 8, width = 15)
    private String createBy;

    @ExcelColumn(name = "备注", order = 9, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 10, width = 20)
    private LocalDateTime createTime;
}
