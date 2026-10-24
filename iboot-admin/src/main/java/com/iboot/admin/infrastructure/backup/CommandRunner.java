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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.infrastructure.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * 外部命令执行器。
 *
 * <p>参数以列表形式传递，不经过 shell 解释；支持把 stdin 送入进程、读取 stdout 并
 * 收集 stderr。生产实现启动本地进程，集成测试实现可以委托给 Testcontainers 容器。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public interface CommandRunner {

    /**
     * 执行命令。
     *
     * @param command     命令与参数列表
     * @param environment 追加到进程环境中的变量
     * @param stdin       标准输入；为 {@code null} 时立即关闭进程输入
     * @param stdout      标准输出目标；为 {@code null} 时丢弃输出
     * @return 退出码与截断后的标准错误
     * @throws IOException          命令无法启动或读写流失败时抛出
     * @throws InterruptedException 等待进程时被中断
     */
    CommandResult run(List<String> command, Map<String, String> environment, InputStream stdin,
                      OutputStream stdout) throws IOException, InterruptedException;
}
