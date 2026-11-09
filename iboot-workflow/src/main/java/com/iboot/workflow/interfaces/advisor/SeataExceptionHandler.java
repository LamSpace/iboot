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

package com.iboot.workflow.interfaces.advisor;

import com.iboot.kernel.web.Result;
import org.apache.seata.common.exception.FrameworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Seata 框架异常处理器。
 *
 * <p>全局事务协调器不可用、RM/TM 注册失败等框架异常统一映射为 503，避免把
 * 技术异常当作 500 内部错误返回。审批业务此时未产生任何写入。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestControllerAdvice
public class SeataExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SeataExceptionHandler.class);

    /**
     * 处理 Seata 框架异常。
     *
     * @param exception 框架异常
     * @return 503 响应
     */
    @ExceptionHandler(FrameworkException.class)
    public ResponseEntity<Result<Void>> handle(final FrameworkException exception) {
        log.error("分布式事务协调器不可用", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(503, "分布式事务协调器不可用，审批未完成"));
    }
}
