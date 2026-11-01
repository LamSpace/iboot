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

package com.iboot.job.infrastructure.backup;

import com.iboot.job.domain.system.model.StorageObject;
import com.iboot.job.domain.system.service.FileStorageStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 备份能力的启动自检。
 *
 * <p>备份的价值在于"出事时它真的在"，因此配置问题必须在启动时暴露，而不是等到第一个
 * 定时备份执行失败留下一条失败记录。启用备份时自检两项：
 *
 * <ul>
 *   <li>存储提供方可写：写入并删除一个探针对象；</li>
 *   <li>导出命令可达：执行 {@code mysqldump --version}。</li>
 * </ul>
 *
 * <p>密钥的合法性由 {@code BackupConfig} 在创建加解密组件时校验，不在此重复。
 * 备份功能关闭时本组件不做任何动作。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
public class BackupStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BackupStartupCheck.class);

    private static final String PROBE_PATH = "backup/.startup-probe";

    private static final byte[] PROBE_BODY = "probe".getBytes(StandardCharsets.UTF_8);

    private final BackupProperties properties;

    private final FileStorageStrategy fileStorageStrategy;

    private final CommandRunner commandRunner;

    private final BackupCommandFactory commandFactory;

    /**
     * 创建自检组件。
     *
     * @param properties          备份配置
     * @param fileStorageStrategy 当前生效的存储提供方
     * @param commandRunner       外部命令执行器
     * @param commandFactory      命令与执行环境工厂
     */
    public BackupStartupCheck(final BackupProperties properties,
                              final FileStorageStrategy fileStorageStrategy,
                              final CommandRunner commandRunner,
                              final BackupCommandFactory commandFactory) {
        this.properties = properties;
        this.fileStorageStrategy = fileStorageStrategy;
        this.commandRunner = commandRunner;
        this.commandFactory = commandFactory;
    }

    /**
     * 应用启动后执行自检。
     *
     * @param args 启动参数
     * @throws IllegalStateException 备份已启用但存储不可写或导出命令不可达时抛出
     */
    @Override
    public void run(final ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        checkStorageWritable();
        checkDumpCommand();
        log.info("备份启动自检通过：存储 {}，导出命令可达", fileStorageStrategy.getStorageType());
    }

    /**
     * 写入并删除探针对象，确认存储提供方可写。
     */
    private void checkStorageWritable() {
        String written;
        try {
            written = fileStorageStrategy.save(
                    new StorageObject(new ByteArrayInputStream(PROBE_BODY), (long) PROBE_BODY.length,
                            "text/plain", "startup-probe"), PROBE_PATH);
        } catch (Exception e) {
            throw new IllegalStateException("备份存储不可写（提供方 " + fileStorageStrategy.getStorageType()
                    + "）：" + e.getMessage(), e);
        }
        if (!fileStorageStrategy.delete(written)) {
            log.warn("备份存储探针清理失败，路径：{}", written);
        }
    }

    /**
     * 执行 {@code mysqldump --version}，确认导出命令可达。
     */
    private void checkDumpCommand() {
        List<String> command = new ArrayList<>(properties.getCommand().getDumpPrefix());
        command.add("--version");
        CommandResult result;
        try {
            result = commandRunner.run(command, commandFactory.environment(), null, null);
        } catch (Exception e) {
            throw new IllegalStateException("备份导出命令不可达：" + e.getMessage(), e);
        }
        if (result.exitCode() != 0) {
            throw new IllegalStateException("备份导出命令不可达，退出码 " + result.exitCode()
                    + "：" + result.stderr());
        }
    }
}
