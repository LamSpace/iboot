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

package com.iboot.workflow.interfaces.advisor;

import org.apache.seata.common.exception.FrameworkException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seata 框架异常处理器测试。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
class SeataExceptionHandlerTest {

    /**
     * 验证框架异常映射为 503 与明确文案。
     */
    @Test
    void mapsFrameworkExceptionTo503() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FailingController())
                .setControllerAdvice(new SeataExceptionHandler())
                .build();

        mockMvc.perform(get("/boom"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("分布式事务协调器不可用，审批未完成"));
    }

    /**
     * 抛出 Seata 框架异常的测试控制器。
     */
    @RestController
    static class FailingController {

        /**
         * 触发异常。
         */
        @GetMapping("/boom")
        public void boom() {
            throw new FrameworkException("can not connect to services-server");
        }
    }
}
