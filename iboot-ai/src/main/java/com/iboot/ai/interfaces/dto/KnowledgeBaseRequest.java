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

package com.iboot.ai.interfaces.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 知识库创建/更新请求。
 *
 * @param name         名称
 * @param description  描述
 * @param visibility   可见范围：1-全部、2-自定义、3-本部门、4-本部门及以下、5-仅本人
 * @param sharedDeptIds 自定义共享部门
 * @author iBoot Team
 * @since 1.0.0
 */
public record KnowledgeBaseRequest(@NotBlank(message = "知识库名称不能为空") String name,
                                   String description,
                                   @Min(1) @Max(5) Integer visibility,
                                   List<Long> sharedDeptIds) {
}
