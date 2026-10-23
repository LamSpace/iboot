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

/**
 * 短信发送客户端端口。
 *
 * <p>抽象云厂商调用，便于单元测试与以后替换供应商。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface SmsClient {

    /**
     * 发送模板短信。
     *
     * @param phone        手机号
     * @param signName     短信签名
     * @param templateCode 云侧模板 Code
     * @param paramsJson   模板参数 JSON
     */
    void send(String phone, String signName, String templateCode, String paramsJson);
}
