package com.iboot.gateway.support;

import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 网关统一错误响应。
 *
 * <p>把上游不可用、上游超时与其它异常映射为**可区分**的 JSON 响应，避免前端只能看到一个
 * 无信息的 500。鉴权相关的 401 / 403 由认证中心接管后另行处理。
 *
 * <p>该处理器注册在默认错误处理器之前（{@code Order} 小于默认值），仅处理本服务转发阶段的
 * 异常；上游自身返回的状态码原样透传，不在此改写。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final ObjectMapper objectMapper;

    /**
     * 创建错误处理器。
     *
     * @param objectMapper JSON 序列化器
     */
    public GatewayErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 输出统一错误响应。
     *
     * @param exchange 当前请求
     * @param throwable 异常
     * @return 完成的信号
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(throwable);
        }

        HttpStatus status;
        String message;
        if (hasCause(throwable, TimeoutException.class)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "上游服务响应超时";
        } else if (hasCause(throwable, ConnectException.class) || hasCause(throwable, java.io.IOException.class)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "上游服务不可用";
        } else if (throwable instanceof ResponseStatusException statusException) {
            status = HttpStatus.resolve(statusException.getStatusCode().value());
            message = statusException.getReason() == null ? "请求处理失败" : statusException.getReason();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "网关内部错误";
        }

        log.warn("网关请求失败：{} {} -> {} ({})", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(), status.value(), throwable.toString());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status.value());
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());

        byte[] bytes;
        try {
            // Boot 4 默认使用 Jackson 3，序列化失败为运行时异常
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (RuntimeException e) {
            bytes = "{\"code\":500,\"message\":\"网关内部错误\"}".getBytes(StandardCharsets.UTF_8);
        }

        response.setStatusCode(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    /**
     * 沿异常链查找指定类型的异常。
     *
     * @param throwable 异常
     * @param type      目标类型
     * @return 找到时返回 {@code true}
     */
    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
