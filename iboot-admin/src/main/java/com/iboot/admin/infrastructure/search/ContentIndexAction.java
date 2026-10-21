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

package com.iboot.admin.infrastructure.search;

/**
 * 内容索引事件的动作。
 *
 * <p>动作只描述"重新读取源数据"还是"移除文档"，由监听器在执行时结合数据库中的最新状态决定
 * 最终写入还是删除，因此同一条变更被重复处理不会产生不同结果。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public enum ContentIndexAction {

    /**
     * 重新读取源数据并刷新索引，源数据已不可见时移除文档。
     */
    REFRESH_SOURCE,

    /**
     * 移除某条源数据对应的全部索引文档。
     */
    DELETE_SOURCE,

    /**
     * 移除某条消息对指定接收人的索引文档。
     */
    DELETE_RECEIVER

}
