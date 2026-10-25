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

package com.iboot.ai.interfaces.advisor;

import com.iboot.ai.application.service.ConversationNotFoundException;
import com.iboot.ai.application.service.KnowledgeAccessDeniedException;
import com.iboot.ai.application.service.ModelCallException;
import com.iboot.ai.application.service.QuotaExceededException;
import com.iboot.ai.application.service.RetrievalUnavailableException;
import com.iboot.ai.infrastructure.parsing.DocumentParseException;
import com.iboot.ai.infrastructure.security.PermissionScopeUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 接口异常映射。
 *
 * <p>把配额、会话与模型错误转换为稳定的 HTTP 状态码与错误码，避免前端依赖异常文本。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestControllerAdvice
public class AiExceptionHandler {

    /**
     * 配额超限。
     *
     * @param exception 异常
     * @return 429
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuota(QuotaExceededException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "quota_exceeded");
        body.put("scope", exception.getScope());
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    /**
     * 会话不存在。
     *
     * @param exception 异常
     * @return 404
     */
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ConversationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "conversation_not_found", "message", exception.getMessage()));
    }

    /**
     * 模型调用失败。
     *
     * @param exception 异常
     * @return 503
     */
    @ExceptionHandler(ModelCallException.class)
    public ResponseEntity<Map<String, Object>> handleModelCall(ModelCallException exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.getError().code());
        body.put("message", exception.getError().message());
        body.put("retryable", exception.getError().retryable());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * 知识库访问被拒绝。
     *
     * @param exception 异常
     * @return 403
     */
    @ExceptionHandler(KnowledgeAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(KnowledgeAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "knowledge_access_denied", "message", exception.getMessage()));
    }

    /**
     * 检索后端或权限范围不可用。
     *
     * @param exception 异常
     * @return 503
     */
    @ExceptionHandler({RetrievalUnavailableException.class, PermissionScopeUnavailableException.class})
    public ResponseEntity<Map<String, Object>> handleRetrievalUnavailable(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "retrieval_unavailable", "message", exception.getMessage()));
    }

    /**
     * 上传或解析参数错误。
     *
     * @param exception 异常
     * @return 400
     */
    @ExceptionHandler({DocumentParseException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException exception) {
        String code = exception instanceof DocumentParseException parseException
                ? parseException.code()
                : "invalid_request";
        return ResponseEntity.badRequest().body(Map.of("code", code, "message", exception.getMessage()));
    }

    /**
     * 参数校验失败。
     *
     * @param exception 异常
     * @return 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest().body(Map.of("code", "invalid_request", "message", message));
    }
}
