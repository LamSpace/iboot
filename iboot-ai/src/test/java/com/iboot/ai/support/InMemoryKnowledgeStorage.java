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

package com.iboot.ai.support;

import com.iboot.ai.domain.knowledge.KnowledgeStorage;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试用内存对象存储。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class InMemoryKnowledgeStorage implements KnowledgeStorage {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, byte[] content, String contentType) {
        store.put(key, content.clone());
    }

    @Override
    public byte[] get(String key) {
        byte[] content = store.get(key);
        if (content == null) {
            throw new IllegalStateException("对象不存在：" + key);
        }
        return content.clone();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        return "memory://" + key;
    }
}
