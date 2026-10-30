package com.iboot.spike.a;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 服务 A 的调用端点。
 *
 * <p>{@code /call-b} 通过服务发现解析服务 B 的实例地址后发起调用，用于验证 2.0-A 的「两服务互相发现」；
 * 该资源同时受 Sentinel 保护，用于验证限流规则。
 *
 * <p>这里使用 Boot 自动配置的 {@link RestClient.Builder} 并用 {@link DiscoveryClient} 显式解析实例地址，
 * 而不是给手工构建的客户端加 {@code @LoadBalanced}：只有自动配置的构建器带有观测定制，
 * 才能产生客户端 span 并把追踪上下文传给下游。手工构建的客户端会导致下游另起一条链路。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@RestController
public class CallController {

    private final RestClient restClient;

    private final DiscoveryClient discoveryClient;

    /**
     * 创建调用端点。
     *
     * @param builder         Boot 自动配置的构建器（已具备观测能力）
     * @param discoveryClient 服务发现客户端
     */
    public CallController(RestClient.Builder builder, DiscoveryClient discoveryClient) {
        this.restClient = builder.build();
        this.discoveryClient = discoveryClient;
    }

    /**
     * 按服务名解析实例后调用服务 B 的 {@code /hello}。
     *
     * @return 服务 B 的响应体
     * @throws IllegalStateException 服务 B 没有可用实例时抛出
     */
    @GetMapping("/call-b")
    public Map<?, ?> callB() {
        List<ServiceInstance> instances = discoveryClient.getInstances("spike-b");
        if (instances.isEmpty()) {
            throw new IllegalStateException("spike-b 没有可用实例");
        }
        ServiceInstance instance = instances.get(0);
        return restClient.get().uri(instance.getUri() + "/hello").retrieve().body(Map.class);
    }
}
