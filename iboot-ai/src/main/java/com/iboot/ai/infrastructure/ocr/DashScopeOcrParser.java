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

import com.iboot.ai.domain.knowledge.OcrParser;
import com.iboot.ai.infrastructure.config.RagProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.Base64;

/**
 * 百炼 Qwen-OCR 适配器。
 *
 * <p>通过 OpenAI 兼容 {@code /chat/completions} 接口发送图片 data URL，识别图片文字。
 * OCR 默认关闭；调用前由上层检查开关与配额。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class DashScopeOcrParser implements OcrParser {

    private static final String PROMPT = "请逐字提取图片中的全部文字，保持原有段落结构。";

    private final RestClient restClient;

    private final RagProperties properties;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final String endpoint;

    /**
     * 创建 OCR 适配器。
     *
     * @param builder        REST 客户端构建器
     * @param properties     知识库配置
     * @param fallbackBaseUrl 对话接入点，作为 OCR 接入点缺省值
     */
    @Autowired
    public DashScopeOcrParser(RestClient.Builder builder, RagProperties properties,
                              @Value("${spring.ai.openai.chat.base-url}") String fallbackBaseUrl) {
        this(builderWithTimeout(builder, properties).build(), properties, endpoint(properties, fallbackBaseUrl));
    }

    DashScopeOcrParser(RestClient restClient, RagProperties properties, String endpoint) {
        this.restClient = restClient;
        this.properties = properties;
        this.endpoint = endpoint;
    }

    private static RestClient.Builder builderWithTimeout(RestClient.Builder builder, RagProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout((int) Duration.ofSeconds(
                Math.max(1, properties.getOcr().getTimeoutSeconds())).toMillis());
        return builder.requestFactory(factory);
    }

    private static String endpoint(RagProperties properties, String fallbackBaseUrl) {
        String baseUrl = StringUtils.isBlank(properties.getOcr().getBaseUrl())
                ? fallbackBaseUrl
                : properties.getOcr().getBaseUrl();
        baseUrl = StringUtils.removeEnd(StringUtils.trim(baseUrl), "/");
        if (!baseUrl.endsWith("/v1")) {
            baseUrl = baseUrl + "/v1";
        }
        return baseUrl + "/chat/completions";
    }

    @Override
    public String recognize(byte[] imageBytes, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new OcrException("OCR_FAILED", "OCR 图片内容为空");
        }
        if (StringUtils.isBlank(properties.getOcr().getApiKey())) {
            throw new OcrException("OCR_CONFIG", "OCR API Key 未配置");
        }
        String body;
        try {
            body = restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getOcr().getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload(imageBytes, contentType))
                    .retrieve()
                    .body(String.class);
        } catch (OcrException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OcrException("OCR_FAILED", "OCR 调用失败", e);
        }
        return parse(body);
    }

    private String payload(byte[] imageBytes, String contentType) {
        String mime = StringUtils.isBlank(contentType) ? "image/jpeg" : contentType;
        ObjectNode root = jsonMapper.createObjectNode();
        root.put("model", properties.getOcr().getModel());
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        ObjectNode image = content.addObject();
        image.put("type", "image_url");
        image.putObject("image_url").put("url",
                "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imageBytes));
        ObjectNode text = content.addObject();
        text.put("type", "text");
        text.put("text", PROMPT);
        try {
            return jsonMapper.writeValueAsString(root);
        } catch (RuntimeException e) {
            throw new OcrException("OCR_FAILED", "OCR 请求构造失败", e);
        }
    }

    private String parse(String body) {
        if (StringUtils.isBlank(body)) {
            throw new OcrException("OCR_FAILED", "OCR 响应为空");
        }
        try {
            JsonNode content = jsonMapper.readTree(body).path("choices").path(0).path("message").path("content");
            StringBuilder text = new StringBuilder();
            if (content.isArray()) {
                content.forEach(item -> text.append(item.path("text").asString("")));
            } else {
                text.append(content.asString(""));
            }
            String result = text.toString().trim();
            if (result.isEmpty()) {
                throw new OcrException("OCR_FAILED", "OCR 未识别到文字");
            }
            return result;
        } catch (OcrException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new OcrException("OCR_FAILED", "OCR 响应解析失败", e);
        }
    }
}
