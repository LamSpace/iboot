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

package com.iboot.admin.infrastructure.job.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 示例定时任务
 * 用于演示定时任务的调用方式
 *
 * @author iBoot
 */
@Component("sampleTask")
public class SampleTask {

    private static final Logger log = LoggerFactory.getLogger(SampleTask.class);

    /**
     * 无参任务方法
     * 调用方式: sampleTask.noParams
     */
    public void noParams() {
        log.info("执行无参示例任务");
    }

    /**
     * 单参数任务方法
     * 调用方式: sampleTask.withParams('参数值')
     *
     * @param param 字符串参数
     */
    public void withParams(String param) {
        log.info("执行有参示例任务，参数: {}", param);
    }

    /**
     * 多参数任务方法
     * 调用方式: sampleTask.multipleParams('字符串', true, 2000L, 316.50D, 100)
     *
     * @param s      字符串参数
     * @param b      布尔参数
     * @param l      长整型参数
     * @param d      双精度浮点参数
     * @param i      整型参数
     */
    public void multipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        log.info("执行多参示例任务，字符串: {}, 布尔: {}, 长整型: {}, 双精度: {}, 整型: {}", s, b, l, d, i);
    }
}
