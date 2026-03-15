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
 * 文件导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileExportVO {

    @ExcelColumn(name = "文件ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "文件名称", order = 2, width = 35)
    private String fileName;

    @ExcelColumn(name = "文件大小", order = 3, width = 15)
    private String readableSize;

    @ExcelColumn(name = "文件类型", order = 4, width = 15)
    private String fileType;

    @ExcelColumn(name = "扩展名", order = 5, width = 10)
    private String fileExt;

    @ExcelColumn(name = "文件分类", order = 6, width = 12, dictType = "sys_file_category")
    private String fileCategory;

    @ExcelColumn(name = "上传人", order = 7, width = 15)
    private String uploadBy;

    @ExcelColumn(name = "上传时间", order = 8, width = 20)
    private LocalDateTime uploadTime;

    @ExcelColumn(name = "备注", order = 9, width = 30)
    private String remark;

    @ExcelColumn(name = "创建时间", order = 10, width = 20)
    private LocalDateTime createTime;
}
