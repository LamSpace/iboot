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
 * 菜单导出VO
 *
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuExportVO {

    @ExcelColumn(name = "菜单ID", order = 1, width = 10)
    private Long id;

    @ExcelColumn(name = "层级", order = 2, width = 8)
    private Integer level;

    @ExcelColumn(name = "菜单名称", order = 3, width = 20)
    private String menuName;

    @ExcelColumn(name = "菜单类型", order = 4, width = 10)
    private String menuType;

    @ExcelColumn(name = "路由地址", order = 5, width = 25)
    private String path;

    @ExcelColumn(name = "组件路径", order = 6, width = 30)
    private String component;

    @ExcelColumn(name = "权限标识", order = 7, width = 25)
    private String perms;

    @ExcelColumn(name = "图标", order = 8, width = 15)
    private String icon;

    @ExcelColumn(name = "排序", order = 9, width = 8)
    private Integer orderNum;

    @ExcelColumn(name = "是否外链", order = 10, width = 10)
    private Integer isFrame;

    @ExcelColumn(name = "是否缓存", order = 11, width = 10)
    private Integer isCache;

    @ExcelColumn(name = "是否可见", order = 12, width = 10)
    private Integer visible;

    @ExcelColumn(name = "状态", order = 13, width = 8, dictType = "sys_normal_disable")
    private Integer status;

    @ExcelColumn(name = "创建时间", order = 14, width = 20)
    private LocalDateTime createTime;
}
