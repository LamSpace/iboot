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

package com.iboot.workflow.application.service.quota;

/**
 * 请假额度快照。
 *
 * @param totalCount     年度总额度
 * @param usedCount      已用次数
 * @param frozenCount    冻结次数
 * @param remainingCount 剩余次数
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public record LeaveQuotaSnapshot(int totalCount, int usedCount, int frozenCount, int remainingCount) {
}
