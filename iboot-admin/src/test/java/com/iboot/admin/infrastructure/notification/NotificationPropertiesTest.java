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

package com.iboot.admin.infrastructure.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NotificationProperties} 的配置绑定测试。
 *
 * <p>验证嵌套配置、默认值以及重试参数非法时的回退规则。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class NotificationPropertiesTest {

    /**
     * 验证嵌套配置可以绑定，未配置项使用默认值。
     */
    @Test
    void bindsNestedConfigurationAndDefaults() {
        Map<String, String> values = new HashMap<>();
        values.put("iboot.notification.dingtalk.enabled", "true");
        values.put("iboot.notification.dingtalk.webhook", "https://oapi.dingtalk.com/robot/send?access_token=x");
        values.put("iboot.notification.dingtalk.secret", "SEC-test");
        values.put("iboot.notification.sms.sign-name", "iBoot");

        NotificationProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("iboot.notification", NotificationProperties.class)
                .get();

        assertThat(properties.isInAppEnabled()).isTrue();
        assertThat(properties.getDingtalk().isEnabled()).isTrue();
        assertThat(properties.getDingtalk().getWebhook()).contains("access_token");
        assertThat(properties.getDingtalk().getSecret()).isEqualTo("SEC-test");
        assertThat(properties.getDingtalk().getKeywordPrefix()).isEqualTo("【iBoot】");
        assertThat(properties.getSms().isEnabled()).isFalse();
        assertThat(properties.getSms().getSignName()).isEqualTo("iBoot");
        assertThat(properties.getEmail().isEnabled()).isTrue();
        assertThat(properties.getRetry().normalizedMaxAttempts()).isEqualTo(5);
        assertThat(properties.getRetry().normalizedBackoffSeconds()).isEqualTo(60);
    }

    /**
     * 验证非法重试参数回退到默认值。
     */
    @Test
    void retryFallsBackToDefaultsForNonPositiveValues() {
        Map<String, String> values = Map.of(
                "iboot.notification.retry.max-attempts", "0",
                "iboot.notification.retry.backoff-seconds", "-3",
                "iboot.notification.retry.batch-size", "0");

        NotificationProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("iboot.notification", NotificationProperties.class)
                .get();

        assertThat(properties.getRetry().normalizedMaxAttempts()).isEqualTo(5);
        assertThat(properties.getRetry().normalizedBackoffSeconds()).isEqualTo(60);
        assertThat(properties.getRetry().normalizedBatchSize()).isEqualTo(100);
    }

    /**
     * 验证合法重试参数原样返回。
     */
    @Test
    void retryKeepsValidValues() {
        Map<String, String> values = Map.of(
                "iboot.notification.retry.max-attempts", "3",
                "iboot.notification.retry.backoff-seconds", "30",
                "iboot.notification.retry.batch-size", "20");

        NotificationProperties properties = new Binder(new MapConfigurationPropertySource(values))
                .bind("iboot.notification", NotificationProperties.class)
                .get();

        assertThat(properties.getRetry().normalizedMaxAttempts()).isEqualTo(3);
        assertThat(properties.getRetry().normalizedBackoffSeconds()).isEqualTo(30);
        assertThat(properties.getRetry().normalizedBatchSize()).isEqualTo(20);
    }
}
