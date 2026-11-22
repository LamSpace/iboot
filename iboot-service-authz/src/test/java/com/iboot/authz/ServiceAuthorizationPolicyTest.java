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

package com.iboot.authz;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务间授权策略测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class ServiceAuthorizationPolicyTest {

    @Test
    void extraRulesWinOverCommonRules() {
        ServiceAuthorizationProperties properties = new ServiceAuthorizationProperties();
        properties.setExtraRules(List.of(rule(List.of("/api/auth/scope"), List.of("iboot-file"))));
        properties.setRules(List.of(rule(List.of("/api/**"), List.of("iboot-gateway"))));

        ServiceAuthorizationPolicy policy = new ServiceAuthorizationPolicy(properties);

        assertThat(policy.isDenied("/api/auth/scope", "iboot-file")).isFalse();
        assertThat(policy.isDenied("/api/auth/scope", "iboot-ai")).isTrue();
        assertThat(policy.isDenied("/api/user/list", "iboot-gateway")).isFalse();
        assertThat(policy.isDenied("/api/user/list", "iboot-file")).isTrue();
    }

    @Test
    void unmatchedPathIsPassedThrough() {
        ServiceAuthorizationProperties properties = new ServiceAuthorizationProperties();
        properties.setRules(List.of(rule(List.of("/actuator/**"), List.of("prometheus"))));

        ServiceAuthorizationPolicy policy = new ServiceAuthorizationPolicy(properties);

        assertThat(policy.isDenied("/api/user/list", null)).isFalse();
    }

    @Test
    void missingCallerIsDeniedWhenRuleMatches() {
        ServiceAuthorizationProperties properties = new ServiceAuthorizationProperties();
        properties.setRules(List.of(rule(List.of("/actuator/**"), List.of("prometheus"))));

        ServiceAuthorizationPolicy policy = new ServiceAuthorizationPolicy(properties);

        assertThat(policy.isDenied("/actuator/health", null)).isTrue();
        assertThat(policy.isDenied("/actuator/health", "prometheus")).isFalse();
    }

    @Test
    void firstMatchingRuleWins() {
        ServiceAuthorizationProperties properties = new ServiceAuthorizationProperties();
        properties.setRules(List.of(
                rule(List.of("/api/user/**"), List.of("iboot-ai")),
                rule(List.of("/api/**"), List.of("iboot-gateway"))));

        ServiceAuthorizationPolicy policy = new ServiceAuthorizationPolicy(properties);

        assertThat(policy.isDenied("/api/user/list", "iboot-ai")).isFalse();
        assertThat(policy.isDenied("/api/user/list", "iboot-gateway")).isTrue();
        assertThat(policy.isDenied("/api/file/list", "iboot-gateway")).isFalse();
    }

    private static ServiceAuthorizationRule rule(final List<String> paths, final List<String> callers) {
        ServiceAuthorizationRule rule = new ServiceAuthorizationRule();
        rule.setPaths(paths);
        rule.setCallers(callers);
        return rule;
    }
}
