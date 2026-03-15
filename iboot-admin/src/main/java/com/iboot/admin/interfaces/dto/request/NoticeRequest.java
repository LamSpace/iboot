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

package com.iboot.admin.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告请求 DTO
 *
 * @author iBoot
 */
@Data
@Schema(description = "公告请求")
public class NoticeRequest {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "公告标题")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题长度不能超过200")
    private String noticeTitle;

    @Schema(description = "公告类型（字典 sys_notice_type）")
    @NotBlank(message = "公告类型不能为空")
    private String noticeType;

    @Schema(description = "公告内容")
    @NotBlank(message = "公告内容不能为空")
    private String noticeContent;

    @Schema(description = "是否置顶：0-否，1-是")
    private Integer isTop;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}
