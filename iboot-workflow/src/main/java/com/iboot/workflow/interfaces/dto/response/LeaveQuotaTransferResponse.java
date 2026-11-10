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

package com.iboot.workflow.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 额度划转响应。
 *
 * <p>接口返回时划转已完成 Try 并处于 {@code TRYING}；后续由协调器确认或取消，
 * 最终状态以额度服务的划转记录为准。
 *
 * @param transferId 划转业务标识
 * @param status     接口返回时的划转状态
 * @author iBoot Team
 * @since 1.0.0
 */
@Schema(description = "额度划转响应")
public record LeaveQuotaTransferResponse(
        @Schema(description = "划转业务标识") String transferId,
        @Schema(description = "划转状态") String status) {
}
