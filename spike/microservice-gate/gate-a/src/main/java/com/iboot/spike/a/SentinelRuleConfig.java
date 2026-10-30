package com.iboot.spike.a;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sentinel 限流规则（仅用于门禁演示）。
 *
 * <p>为 {@code /call-b} 资源加载一条 QPS 规则；正式接入时规则应持久化到 Nacos，
 * 而不是像这里一样写死在代码里。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Configuration
public class SentinelRuleConfig {

    private final double qps;

    /**
     * 创建规则配置。
     *
     * @param qps 每秒允许通过的请求数
     */
    public SentinelRuleConfig(@Value("${spike.sentinel.qps:2}") double qps) {
        this.qps = qps;
    }

    /**
     * 加载限流规则。
     */
    @PostConstruct
    public void loadRules() {
        FlowRule rule = new FlowRule("/call-b");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(qps);
        FlowRuleManager.loadRules(List.of(rule));
    }
}
