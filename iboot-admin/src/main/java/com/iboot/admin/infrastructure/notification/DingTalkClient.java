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
 * 钉钉发送客户端端口。
 *
 * <p>抽象 HTTP 调用，便于单元测试注入 Fake；实现不得在异常中暴露 Webhook 或 Secret。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface DingTalkClient {

    /**
     * 发送文本消息。
     *
     * @param webhook Webhook 地址
     * @param secret  加签 Secret
     * @param content 消息正文
     */
    void send(String webhook, String secret, String content);
}
