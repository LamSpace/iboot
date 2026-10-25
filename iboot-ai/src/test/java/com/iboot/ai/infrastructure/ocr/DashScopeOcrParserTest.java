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

package com.iboot.ai.infrastructure.ocr;

import com.iboot.ai.infrastructure.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link DashScopeOcrParser} 的单元测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class DashScopeOcrParserTest {

    private static final String ENDPOINT = "http://localhost:8080/v1/chat/completions";

    private MockRestServiceServer server;

    private RagProperties properties;

    private DashScopeOcrParser parser;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.getOcr().setEnabled(true);
        properties.getOcr().setApiKey("test-key");
        properties.getOcr().setModel("qwen3.5-ocr");
        properties.getOcr().setBaseUrl("http://localhost:8080/v1");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        parser = new DashScopeOcrParser(builder.build(), properties, ENDPOINT);
    }

    /**
     * 解析字符串内容并转发 JWT。
     */
    @Test
    void recognizesText() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"识别文本\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThat(parser.recognize(new byte[]{1, 2, 3}, "image/jpeg")).isEqualTo("识别文本");
        server.verify();
    }

    /**
     * 拼接数组形式的内容。
     */
    @Test
    void joinsArrayContent() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":["
                        + "{\"type\":\"text\",\"text\":\"第一段\"},"
                        + "{\"type\":\"text\",\"text\":\"第二段\"}]}}]}", MediaType.APPLICATION_JSON));

        assertThat(parser.recognize(new byte[]{1}, "image/png")).isEqualTo("第一段第二段");
    }

    /**
     * 缺少 Key 时不发起请求。
     */
    @Test
    void rejectsMissingApiKey() {
        properties.getOcr().setApiKey("");

        assertThatThrownBy(() -> parser.recognize(new byte[]{1}, "image/jpeg"))
                .isInstanceOf(OcrException.class)
                .extracting(error -> ((OcrException) error).code())
                .isEqualTo("OCR_CONFIG");
        server.verify();
    }

    /**
     * 空响应返回可辨识错误。
     */
    @Test
    void rejectsBlankResponse() {
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> parser.recognize(new byte[]{1}, "image/jpeg"))
                .isInstanceOf(OcrException.class)
                .extracting(error -> ((OcrException) error).code())
                .isEqualTo("OCR_FAILED");
    }
}
