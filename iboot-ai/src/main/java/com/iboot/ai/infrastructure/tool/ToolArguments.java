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

package com.iboot.ai.infrastructure.tool;

import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 工具参数读取与摘要工具。
 *
 * <p>参数 JSON 在计算摘要前按键名递归排序，保证同一组参数得到相同摘要；
 * 摘要用于把一次确认绑定到确定的工具调用上。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public final class ToolArguments {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ToolArguments() {
    }

    /**
     * 读取字符串参数。
     *
     * @param arguments 参数表
     * @param key       键名
     * @return 非空字符串，缺失或空白时返回 {@code null}
     */
    public static String string(Map<String, Object> arguments, String key) {
        if (arguments == null) {
            return null;
        }
        Object value = arguments.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 读取整数参数。
     *
     * @param arguments    参数表
     * @param key          键名
     * @param defaultValue 缺失或非法时的默认值
     * @return 整数值
     */
    public static Integer integer(Map<String, Object> arguments, String key, Integer defaultValue) {
        if (arguments == null) {
            return defaultValue;
        }
        Object value = arguments.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 把参数表序列化为键名有序的 JSON。
     *
     * @param arguments 参数表
     * @return JSON 文本
     */
    public static String canonicalJson(Map<String, Object> arguments) {
        return MAPPER.writeValueAsString(sortValue(arguments == null ? Map.of() : arguments));
    }

    /**
     * 计算参数表的 SHA-256 摘要。
     *
     * @param arguments 参数表
     * @return 64 位十六进制摘要
     */
    public static String digest(Map<String, Object> arguments) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(canonicalJson(arguments).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(Character.forDigit((value >> 4) & 0xF, 16));
                builder.append(Character.forDigit(value & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }

    private static Object sortValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), sortValue(item)));
            return new TreeMap<>(sorted);
        }
        if (value instanceof List<?> list) {
            List<Object> sorted = new ArrayList<>(list.size());
            list.forEach(item -> sorted.add(sortValue(item)));
            return sorted;
        }
        return value;
    }

    /**
     * 按键名有序读取参数，供测试与诊断使用。
     *
     * @param arguments 参数表
     * @return 有序参数副本
     */
    public static Map<String, Object> ordered(Map<String, Object> arguments) {
        Map<String, Object> copy = new LinkedHashMap<>();
        new TreeMap<>(arguments).forEach(copy::put);
        return copy;
    }
}
