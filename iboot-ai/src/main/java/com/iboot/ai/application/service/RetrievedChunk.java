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

package com.iboot.ai.application.service;

/**
 * 检索命中的分块。
 *
 * @param chunkId    分块 ID
 * @param kbId       知识库 ID
 * @param docId      文档 ID
 * @param docTitle   文档标题
 * @param headingPath 标题路径
 * @param content    分块正文
 * @param pageNo     页码
 * @param chunkIndex 分块序号
 * @param score      融合得分
 * @author iBoot Team
 * @since 1.0.0
 */
public record RetrievedChunk(String chunkId, Long kbId, Long docId, String docTitle, String headingPath,
                             String content, Integer pageNo, Integer chunkIndex, Double score) {
}
