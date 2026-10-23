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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DingTalkSigner} 的签名向量测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class DingTalkSignerTest {

    /**
     * 验证固定 timestamp/secret 的签名结果。
     */
    @Test
    void signsKnownVector() {
        String sign = new DingTalkSigner().sign("SECtestsecret", 1600000000000L);

        assertThat(sign).isEqualTo("vrmK359oxoAU8yhn3VznSEiUP41P6ofw2rcxyiGRGAM%3D");
    }

    /**
     * 验证不同 secret 产生不同签名。
     */
    @Test
    void differentSecretProducesDifferentSign() {
        DingTalkSigner signer = new DingTalkSigner();

        assertThat(signer.sign("SEC-a", 1600000000000L))
                .isNotEqualTo(signer.sign("SEC-b", 1600000000000L));
    }
}
