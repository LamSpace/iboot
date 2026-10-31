package com.iboot.gateway.support;

import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * 在响应头回显当前请求的 TraceID。
 *
 * <p>属于入口支撑组件，与路由转发无关：只读取当前请求的服务端观测所对应的 span 标识，写入
 * {@code X-Trace-Id} 响应头，不改变状态码、响应体与转发目标。追踪未开启时观测上没有追踪上下文，
 * 本过滤器自动退化为空操作。
 *
 * <p>取值来源是反应式上下文中的观测对象，而不是 {@code Tracer.currentSpan()}：WebFlux 的服务端
 * 观测由 {@code HttpWebHandlerAdapter} 创建后放进反应式上下文，并未以线程局部作用域的形式暴露，
 * 直接取当前 span 会得到 {@code null}。
 *
 * <p>TraceID 在过滤器执行时即取出，实际写入交给 {@code response.beforeCommit(...)}；提交回调
 * 可能在别的线程执行，提前取值可以避免依赖上下文传播。写入过程中的任何异常只记日志，不影响请求。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class TraceIdResponseFilter implements WebFilter, Ordered {

    /**
     * 回显 TraceID 的响应头名称。
     *
     * <p>取值为 32 位小写十六进制，与 W3C Trace Context 中的 trace-id 一致。
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final Logger log = LoggerFactory.getLogger(TraceIdResponseFilter.class);

    /** 追踪导出开关：关闭后不再回显标识，避免给出无法在追踪后端查到的 TraceID。 */
    private final boolean tracingEnabled;

    /**
     * 创建响应头过滤器。
     *
     * @param tracingEnabled 追踪导出开关，对应 {@code management.tracing.export.enabled}
     */
    public TraceIdResponseFilter(@Value("${management.tracing.export.enabled:true}") boolean tracingEnabled) {
        this.tracingEnabled = tracingEnabled;
    }

    /**
     * 在响应提交前写入 TraceID 响应头，然后继续过滤链。
     *
     * <p>本过滤器不短路请求，任何情况下都会把请求交给后续过滤器。
     *
     * @param exchange 当前请求
     * @param chain    后续过滤链
     * @return 请求处理完成的信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.deferContextual(contextView -> {
            String traceId = tracingEnabled ? currentTraceId(contextView) : null;
            if (traceId != null) {
                exchange.getResponse().beforeCommit(() -> {
                    try {
                        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
                    } catch (RuntimeException e) {
                        log.debug("回显 TraceID 失败：{}", exchange.getRequest().getPath(), e);
                    }
                    return Mono.empty();
                });
            }
            return chain.filter(exchange);
        });
    }

    /**
     * 取最高优先级，保证在跨域等可能直接应答的过滤器之前执行。
     *
     * @return 过滤器顺序
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 从反应式上下文中取出当前请求的 TraceID。
     *
     * @param contextView 反应式上下文
     * @return 32 位十六进制 TraceID；追踪未开启或读取失败时返回 {@code null}
     */
    private String currentTraceId(ContextView contextView) {
        try {
            Observation observation = contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
            if (observation == null) {
                return null;
            }
            TracingObservationHandler.TracingContext tracingContext =
                    observation.getContext().get(TracingObservationHandler.TracingContext.class);
            Span span = tracingContext == null ? null : tracingContext.getSpan();
            return span == null ? null : span.context().traceId();
        } catch (RuntimeException e) {
            log.debug("读取当前 TraceID 失败", e);
            return null;
        }
    }
}
