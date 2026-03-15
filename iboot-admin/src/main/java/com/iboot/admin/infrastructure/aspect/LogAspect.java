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

package com.iboot.admin.infrastructure.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iboot.admin.application.service.OperateLogApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.domain.system.model.OperateLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志切面
 *
 * @author iBoot
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final OperateLogApplicationService operateLogApplicationService;
    private final ObjectMapper objectMapper;

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 记录开始时间
     */
    @Before("@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) {
        START_TIME.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    /**
     * 处理日志
     */
    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            // 获取请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();

            // 获取当前用户
            String operatorName = "";
            String deptName = "";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                try {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> loginUser = (Map<String, Object>) principal;
                        operatorName = loginUser.get("username") != null ? loginUser.get("username").toString() : "";
                        deptName = loginUser.get("deptName") != null ? loginUser.get("deptName").toString() : "";
                    }
                } catch (Exception ex) {
                    log.debug("获取用户信息失败：{}", ex.getMessage());
                }
            }

            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = method.getName();

            // 计算耗时
            long costTime = 0;
            if (START_TIME.get() != null) {
                costTime = System.currentTimeMillis() - START_TIME.get();
                START_TIME.remove();
            }

            // 构建日志对象
            OperateLog operateLog = OperateLog.builder()
                    .title(controllerLog.title())
                    .businessType(controllerLog.businessType().getCode())
                    .method(className + "." + methodName + "()")
                    .requestMethod(request.getMethod())
                    .operatorName(operatorName)
                    .deptName(deptName)
                    .url(request.getRequestURI())
                    .ip(getIpAddress(request))
                    .location("")
                    .status(e != null ? 0 : 1)
                    .costTime(costTime)
                    .operateTime(LocalDateTime.now())
                    .build();

            // 保存请求参数
            if (controllerLog.isSaveRequestData()) {
                setRequestValue(joinPoint, operateLog);
            }

            // 保存响应数据
            if (controllerLog.isSaveResponseData() && jsonResult != null) {
                String resultStr = objectMapper.writeValueAsString(jsonResult);
                operateLog.setResult(StringUtils.substring(resultStr, 0, 2000));
            }

            // 保存异常信息
            if (e != null) {
                operateLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }

            // 异步保存日志
            operateLogApplicationService.recordOperateLog(operateLog);

        } catch (Exception ex) {
            log.error("记录操作日志异常：{}", ex.getMessage());
        }
    }

    /**
     * 获取请求参数并设置到日志中
     */
    private void setRequestValue(JoinPoint joinPoint, OperateLog operateLog) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String params = objectMapper.writeValueAsString(args);
                operateLog.setParam(StringUtils.substring(params, 0, 2000));
            }
        } catch (Exception ex) {
            log.debug("获取请求参数失败：{}", ex.getMessage());
        }
    }

    /**
     * 获取真实 IP 地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于多个代理的情况，第一个 IP 为客户端真实 IP
        if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
