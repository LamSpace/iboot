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

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.iboot.admin.domain.notification.service.NotificationDeliveryException;
import org.springframework.stereotype.Component;

/**
 * 阿里云 Dysmsapi 短信客户端。
 *
 * <p>客户端延迟初始化，只有渠道真正启用并发送时才创建，缺少凭证不会阻止应用启动。
 * 返回码非 {@code OK} 时按可重试失败处理。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class AliyunSmsClient implements SmsClient {

    private static final String ENDPOINT = "dysmsapi.aliyuncs.com";

    private final NotificationProperties properties;

    private volatile Client client;

    @SuppressWarnings("all")
    public AliyunSmsClient(final NotificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void send(final String phone, final String signName, final String templateCode, final String paramsJson) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam(paramsJson == null ? "{}" : paramsJson);
            SendSmsResponse response = client().sendSms(request);
            if (response == null || response.getBody() == null
                    || !"OK".equalsIgnoreCase(response.getBody().getCode())) {
                String code = response == null || response.getBody() == null
                        ? "UNKNOWN" : response.getBody().getCode();
                String message = response == null || response.getBody() == null
                        ? "" : response.getBody().getMessage();
                throw new NotificationDeliveryException("短信发送失败: code=" + code + ", message=" + message, true);
            }
        } catch (NotificationDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new NotificationDeliveryException(NotificationSanitizer.sanitizeError(e.getMessage()), true);
        }
    }

    private Client client() {
        Client local = client;
        if (local == null) {
            synchronized (this) {
                local = client;
                if (local == null) {
                    try {
                        NotificationProperties.Sms config = properties.getSms();
                        local = new Client(new Config()
                                .setAccessKeyId(config.getAccessKeyId())
                                .setAccessKeySecret(config.getAccessKeySecret())
                                .setEndpoint(ENDPOINT));
                    } catch (Exception e) {
                        throw new NotificationDeliveryException("短信客户端初始化失败", false);
                    }
                    client = local;
                }
            }
        }
        return local;
    }
}
