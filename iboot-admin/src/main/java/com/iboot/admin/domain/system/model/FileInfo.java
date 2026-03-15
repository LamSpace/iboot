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

package com.iboot.admin.domain.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息实体
 * 
 * @author iBoot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    
    /**
     * 文件ID
     */
    private Long id;
    
    /**
     * 原始文件名
     */
    private String fileName;
    
    /**
     * 存储路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * MIME类型
     */
    private String fileType;
    
    /**
     * 文件扩展名
     */
    private String fileExt;
    
    /**
     * 文件分类（关联字典 sys_file_category）
     */
    private String fileCategory;
    
    /**
     * 上传人
     */
    private String uploadBy;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 创建人
     */
    private String createBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新人
     */
    private String updateBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    private Integer deleted;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 获取可读的文件大小
     */
    public String getReadableSize() {
        if (fileSize == null || fileSize == 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSize;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    /**
     * 判断是否为图片文件
     */
    public boolean isImage() {
        return fileType != null && fileType.startsWith("image/");
    }
}
