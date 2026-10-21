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
import jakarta.validation.constraints.Size;

/**
 * 审批决定请求 DTO。
 *
 * <p>通过时意见可选；驳回时由应用服务校验意见必填。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "审批决定请求")
public class ApprovalDecisionRequest {

    /**
     * 审批意见或驳回原因。
     */
    @Schema(description = "审批意见或驳回原因")
    @Size(max = 500, message = "审批意见长度不能超过500")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }
}
