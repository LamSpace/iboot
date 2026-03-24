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

package com.iboot.admin.common.exception;

import com.iboot.admin.common.i18n.I18nUtil;
import com.iboot.admin.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author iBoot
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常处理
     *
     * @param e 业务异常
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * SSE 客户端断开异常处理 当客户端(浏览器)关闭连接时触发,属于正常行为,不需要返回响应
     *
     * @param e SSE 异步请求不可用异常
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e) {
        // 客户端断开连接是正常行为,只记录 DEBUG 级别日志
        log.debug("SSE 客户端断开连接: {}", e.getMessage());
    }

    /**
     * 客户端中止异常处理 当客户端主动断开连接时触发(如刷新页面、关闭标签等)
     *
     * @param e 客户端中止异常
     */
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e) {
        // 客户端主动断开连接是正常行为,只记录 DEBUG 级别日志
        log.debug("客户端主动断开连接: {}", e.getMessage());
    }

    /**
     * 参数校验异常处理 - @Valid 注解校验失败
     *
     * @param e 方法参数校验异常
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数校验异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 参数绑定异常处理
     *
     * @param e 参数绑定异常
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数绑定异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 约束违反异常处理 - @Validated 注解校验失败
     *
     * @param e 约束违反异常
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
        log.error("约束违反异常: {}", message);
        return Result.error(400, message);
    }

    /**
     * 非法参数异常处理
     *
     * @param e 非法参数异常
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常: {}", e.getMessage(), e);
        return Result.error(400, e.getMessage());
    }

    /**
     * 数据库唯一键冲突异常处理
     *
     * @param e 唯一键冲突异常
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据库唯一键冲突: {}", e.getMessage(), e);
        return Result.error(400, I18nUtil.getMessage("dict.value.exists"));
    }

    /**
     * Spring Security 权限拒绝异常处理 当用户已认证但无权访问特定资源时触发（@PreAuthorize 校验失败等）
     *
     * @param e        权限拒绝异常
     * @param response HTTP响应对象
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletResponse response) {
        log.warn("权限拒绝: {}", e.getMessage());
        resetContentTypeToJson(response);
        return Result.error(403, I18nUtil.getMessage("result.forbidden"));
    }

    /**
     * Spring Security 认证异常处理 当用户认证失败时触发
     *
     * @param e 认证异常
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return Result.error(401, I18nUtil.getMessage("result.unauthorized"));
    }

    /**
     * 运行时异常处理
     *
     * @param e        运行时异常
     * @param response HTTP响应对象
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletResponse response) {
        log.error("运行时异常: {}", e.getMessage(), e);
        resetContentTypeToJson(response);
        return Result.error(500, "系统内部错误: " + e.getMessage());
    }

    /**
     * 通用异常处理
     *
     * @param e        通用异常
     * @param response HTTP响应对象
     *
     * @return 错误响应结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletResponse response) {
        log.error("系统异常: {}", e.getMessage(), e);
        resetContentTypeToJson(response);
        return Result.error(500, "系统异常，请联系管理员");
    }

    /**
     * 重置响应的Content-Type为JSON格式 用于处理导出等操作中发生异常时，响应头已被设置为非JSON格式的情况
     *
     * @param response HTTP响应对象
     */
    private void resetContentTypeToJson(HttpServletResponse response) {
        if (response != null && !response.isCommitted()) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        }
    }

}
