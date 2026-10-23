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

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 钉钉自定义机器人加签器。
 *
 * <p>按钉钉规范对 {@code timestamp + "\n" + secret} 做 HMAC-SHA256，Base64 后
 * URL 编码。异常信息不包含 Secret。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DingTalkSigner {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 计算加签参数。
     *
     * @param secret    加签 Secret
     * @param timestamp 毫秒时间戳
     * @return URL 编码后的签名
     */
    public String sign(final String secret, final long timestamp) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getEncoder().encodeToString(raw);
            return URLEncoder.encode(base64, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("钉钉加签失败", e);
        }
    }
}
