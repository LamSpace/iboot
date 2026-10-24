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

package com.iboot.admin.application.service.backup;

import com.iboot.admin.domain.backup.model.BackupRecord;
import com.iboot.admin.domain.backup.model.BackupStatus;
import com.iboot.admin.domain.backup.model.RestoreRecord;
import com.iboot.admin.domain.backup.model.RestoreStatus;
import com.iboot.admin.domain.backup.model.TableCountMismatch;
import com.iboot.admin.domain.system.model.StorageObject;
import com.iboot.admin.infrastructure.backup.BackupCipher;
import com.iboot.admin.infrastructure.backup.BackupProperties;
import com.iboot.admin.infrastructure.backup.BackupCommandFactory;
import com.iboot.admin.infrastructure.backup.MysqldumpBackupExecutor;
import com.iboot.admin.infrastructure.backup.ProcessCommandRunner;
import com.iboot.admin.infrastructure.storage.InMemoryFileStorageStrategy;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据备份与恢复演练的端到端集成测试。
 *
 * <p>在真实 MySQL 8.0.45 容器上执行 V1–V21，通过 {@code docker exec} 调用容器内的
 * {@code mysqldump}/{@code mysql}，配合内存存储完成“备份→加密→解密→还原演练库→行数比对”
 * 全链路。测试只使用本机已有的 MySQL 与 Ryuk 镜像。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers
class BackupIntegrationTest {

    private static final int MYSQL_PORT = 3306;

    private static final String DATABASE = "iboot_backup_it";

    private static final String DRILL_SCHEMA = "iboot_restore_drill";

    private static final String KEY = Base64.getEncoder().encodeToString(filledKey());

    private static final Clock CLOCK = Clock.systemDefaultZone();

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", DATABASE)
            .waitingFor(Wait.forLogMessage(".*mysqld.*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private FakeBackupRecordRepository backupRepository;

    private FakeRestoreRecordRepository restoreRepository;

    private InMemoryFileStorageStrategy storage;

    private BackupCipher cipher;

    private MysqldumpBackupExecutor executor;

    private BackupApplicationService backupService;

    private RestoreDrillService restoreService;

    @BeforeAll
    static void migrate() throws Exception {
        Flyway.configure()
                .dataSource(url(DATABASE), "root", "root")
                .locations("classpath:db/migration")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url(DATABASE), "root", "root");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO sys_config (id, config_name, config_key, config_value, "
                    + "config_type, create_by, create_time, deleted, remark) "
                    + "VALUES (99999, '集成测试', 'it.backup.marker', '1', 1, 'system', NOW(), 0, 'it')");
        }
    }

    @BeforeEach
    void setUp() {
        BackupProperties properties = new BackupProperties();
        properties.setEnabled(true);
        properties.setEncryptionKey(KEY);
        properties.getDatabase().setHost(MYSQL.getHost());
        properties.getDatabase().setPort(MYSQL.getMappedPort(MYSQL_PORT));
        properties.getDatabase().setName(DATABASE);
        properties.getDatabase().setUsername("root");
        properties.getDatabase().setPassword("root");
        properties.getCommand().setHost("localhost");
        properties.getCommand().setPort(MYSQL_PORT);
        String containerId = MYSQL.getContainerId();
        properties.getCommand().setDumpPrefix(List.of("docker", "exec", "-i", "-e", "MYSQL_PWD",
                containerId, "mysqldump"));
        properties.getCommand().setRestorePrefix(List.of("docker", "exec", "-i", "-e", "MYSQL_PWD",
                containerId, "mysql"));

        backupRepository = new FakeBackupRecordRepository();
        restoreRepository = new FakeRestoreRecordRepository();
        storage = new InMemoryFileStorageStrategy();
        cipher = BackupCipher.fromBase64(KEY);
        executor = new MysqldumpBackupExecutor(new ProcessCommandRunner(),
                new BackupCommandFactory(properties), properties);
        AsyncTaskExecutor syncExecutor = Runnable::run;
        backupService = new BackupApplicationService(properties, backupRepository, executor, cipher, storage,
                syncExecutor, CLOCK);
        restoreService = new RestoreDrillService(properties, backupRepository, restoreRepository, executor,
                cipher, storage, syncExecutor, CLOCK);
    }

    /**
     * 验证备份产物加密可解密，演练成功且业务库行数不变。
     */
    @Test
    void backupAndDrillAreConsistent() throws Exception {
        BackupRecord saved = backup();
        assertThat(saved.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(saved.getTableStats()).containsKey("sys_config");
        assertThat(saved.getChecksumSha256()).hasSize(64);
        assertThat(storage.exists(saved.getStoragePath())).isTrue();

        ByteArrayOutputStream plaintext = new ByteArrayOutputStream();
        try (InputStream input = storage.getFileStream(saved.getStoragePath()).getInputStream()) {
            cipher.decrypt(input, plaintext);
        }
        assertThat(plaintext.toString(StandardCharsets.UTF_8))
                .contains("CREATE TABLE")
                .contains("sys_config");

        Map<String, Long> businessBefore = executor.tableRowCounts(DATABASE);
        RestoreRecord drill = restoreService.trigger(saved.getId(), "it");
        RestoreRecord finished = restoreRepository.findById(drill.getId()).orElseThrow();

        assertThat(finished.getStatus()).isEqualTo(RestoreStatus.SUCCESS);
        assertThat(finished.getTargetSchema()).isEqualTo(DRILL_SCHEMA);
        assertThat(finished.getMismatches()).isEmpty();
        assertThat(schemaExists(DRILL_SCHEMA)).isTrue();
        assertThat(executor.tableRowCounts(DRILL_SCHEMA)).isEqualTo(saved.getTableStats());
        assertThat(executor.tableRowCounts(DATABASE)).isEqualTo(businessBefore);
    }

    /**
     * 验证密文损坏时在创建演练库之前失败。
     */
    @Test
    void corruptBackupFailsBeforeDdl() throws Exception {
        BackupRecord saved = backup();
        dropSchema(DRILL_SCHEMA);

        byte[] encrypted;
        try (InputStream input = storage.getFileStream(saved.getStoragePath()).getInputStream()) {
            encrypted = input.readAllBytes();
        }
        encrypted[encrypted.length - 1] ^= 0x01;
        storage.delete(saved.getStoragePath());
        storage.save(new StorageObject(new ByteArrayInputStream(encrypted), (long) encrypted.length,
                "application/octet-stream", "tampered.enc"), saved.getStoragePath());

        RestoreRecord drill = restoreService.trigger(saved.getId(), "it");
        RestoreRecord finished = restoreRepository.findById(drill.getId()).orElseThrow();

        assertThat(finished.getStatus()).isEqualTo(RestoreStatus.FAILED);
        assertThat(schemaExists(DRILL_SCHEMA)).isFalse();
    }

    /**
     * 验证行数快照不一致时演练结果为不一致并列出差异表。
     */
    @Test
    void mismatchedSnapshotReportsMismatch() {
        BackupRecord saved = backup();
        Map<String, Long> wrong = new LinkedHashMap<>(saved.getTableStats());
        wrong.put("sys_config", wrong.getOrDefault("sys_config", 0L) + 1);
        saved.setTableStats(wrong);
        backupRepository.replace(saved);

        RestoreRecord drill = restoreService.trigger(saved.getId(), "it");
        RestoreRecord finished = restoreRepository.findById(drill.getId()).orElseThrow();

        assertThat(finished.getStatus()).isEqualTo(RestoreStatus.MISMATCH);
        assertThat(finished.getMismatches())
                .extracting(TableCountMismatch::tableName)
                .contains("sys_config");
    }

    private BackupRecord backup() {
        BackupRecord triggered = backupService.triggerManual("it");
        return backupRepository.findById(triggered.getId()).orElseThrow();
    }

    private boolean schemaExists(final String schema) throws Exception {
        try (Connection connection = DriverManager.getConnection(url(DATABASE), "root", "root");
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?")) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private void dropSchema(final String schema) throws Exception {
        try (Connection connection = DriverManager.getConnection(url(DATABASE), "root", "root");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `" + schema + "`");
        }
    }

    private static String url(final String database) {
        return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static byte[] filledKey() {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 5);
        return key;
    }
}
