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

package com.iboot.ai.domain.knowledge;

import java.time.LocalDateTime;

/**
 * 知识库文档。
 *
 * @param id            文档 ID
 * @param kbId          知识库 ID
 * @param fileName      原始文件名
 * @param fileExt       扩展名
 * @param fileSize      文件大小
 * @param mimeType      MIME 类型
 * @param storageKey    对象存储路径
 * @param contentHash   内容 SHA-256
 * @param ownerUserId   上传人用户 ID
 * @param ownerDeptId   上传人部门 ID
 * @param status        入库状态
 * @param errorCode     失败原因码
 * @param errorMessage  失败说明
 * @param chunkCount    分块数量
 * @param attempts      已尝试次数
 * @param nextAttemptAt 下次重试时间
 * @param createTime    创建时间
 * @param updateTime    更新时间
 * @author iBoot Team
 * @since 1.0.0
 */
public record KnowledgeDocument(Long id, Long kbId, String fileName, String fileExt, Long fileSize, String mimeType,
                                String storageKey, String contentHash, Long ownerUserId, Long ownerDeptId,
                                DocumentStatus status, String errorCode, String errorMessage, Integer chunkCount,
                                Integer attempts, LocalDateTime nextAttemptAt, LocalDateTime createTime,
                                LocalDateTime updateTime) {
}
