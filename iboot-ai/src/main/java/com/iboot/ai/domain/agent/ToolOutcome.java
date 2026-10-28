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

package com.iboot.ai.domain.agent;

/**
 * 工具执行结果。
 *
 * <p>内容会进入模型上下文，因此实现必须返回经过裁剪的文本，并区分成功、待确认、
 * 无权限与失败四种状态，避免模型把失败当成事实继续推理。
 *
 * @param status  结果状态
 * @param content 面向模型的结果文本
 * @author iBoot Team
 * @since 1.0.0
 */
public record ToolOutcome(Status status, String content) {

    /**
     * 工具结果状态。
     */
    public enum Status {

        /** 执行成功。 */
        OK,

        /** 写操作等待用户确认，未产生下游副作用。 */
        PENDING,

        /** 调用者缺少所需权限，未发起下游请求。 */
        DENIED,

        /** 执行失败。 */
        FAILED
    }

    /**
     * 构造成功结果。
     *
     * @param content 结果文本
     * @return 成功结果
     */
    public static ToolOutcome ok(String content) {
        return new ToolOutcome(Status.OK, content);
    }

    /**
     * 构造待确认结果。
     *
     * @param content 提示文本
     * @return 待确认结果
     */
    public static ToolOutcome pending(String content) {
        return new ToolOutcome(Status.PENDING, content);
    }

    /**
     * 构造无权限结果。
     *
     * @param content 提示文本
     * @return 无权限结果
     */
    public static ToolOutcome denied(String content) {
        return new ToolOutcome(Status.DENIED, content);
    }

    /**
     * 构造失败结果。
     *
     * @param content 失败说明
     * @return 失败结果
     */
    public static ToolOutcome failed(String content) {
        return new ToolOutcome(Status.FAILED, content);
    }
}
