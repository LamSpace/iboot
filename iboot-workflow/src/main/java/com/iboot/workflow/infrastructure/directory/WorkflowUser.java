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

package com.iboot.workflow.infrastructure.directory;

/**
 * 审批流程所需的系统用户只读快照。
 *
 * <p>只包含审批校验与展示真正使用的四个字段，不复制系统用户的完整模型；
 * 用户目录由管理端持有，本服务通过 {@link WorkflowUserReader} 跨库只读。
 *
 * @param id       用户 ID
 * @param status   状态：0-停用，1-正常
 * @param username 用户名
 * @param nickname 昵称，可为空
 * @author iBoot Team
 * @since 1.0.0
 */
public record WorkflowUser(Long id, Integer status, String username, String nickname) {
}
