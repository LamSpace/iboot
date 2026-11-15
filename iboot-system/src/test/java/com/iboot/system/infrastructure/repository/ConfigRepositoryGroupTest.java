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

package com.iboot.system.infrastructure.repository;

import com.iboot.dbinit.SchemaMigrator;
import com.iboot.system.domain.system.model.Config;
import com.iboot.system.infrastructure.persistence.mapper.ConfigMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数配置仓储分组筛选测试。
 *
 * <p>在真实 MySQL 上执行迁移，验证按分组筛选、计数与导出查询使用同一条件。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Testcontainers(disabledWithoutDocker = true)
class ConfigRepositoryGroupTest {

    private static final int MYSQL_PORT = 3306;

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>("mysql:8.0.45")
            .withExposedPorts(MYSQL_PORT)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_DATABASE", "iboot_config_group")
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private ConfigRepositoryImpl repository;

    private SqlSession session;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource dataSource = dataSource();
        migrateWithRetry(dataSource);

        Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        try (InputStream input = Resources.getResourceAsStream("mapper/ConfigMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/ConfigMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(configuration);
        session = factory.openSession(true);
        repository = new ConfigRepositoryImpl(session.getMapper(ConfigMapper.class));
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM sys_config");
        insert("basic.a", "basic");
        insert("basic.b", "basic");
        insert("storage.a", "storage");
        insert("other.a", "other");
    }

    @AfterEach
    void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    /**
     * 验证分组筛选只返回该分组，计数与列表一致。
     */
    @Test
    void filtersByGroupAndCountMatches() {
        List<Config> configs = repository.findPageByCondition(null, null, null, "basic", 1, 10);

        assertThat(configs).extracting(Config::getConfigKey)
                .containsExactlyInAnyOrder("basic.a", "basic.b");
        assertThat(configs.get(0).getConfigGroup()).isEqualTo("basic");
        assertThat(repository.countByCondition(null, null, null, "basic")).isEqualTo(2);
    }

    /**
     * 验证不传分组时保持原有全量查询行为。
     */
    @Test
    void noGroupFilterKeepsAllConfigs() {
        List<Config> configs = repository.findPageByCondition(null, null, null, null, 1, 10);

        assertThat(configs).hasSize(4);
        assertThat(repository.countByCondition(null, null, null, null)).isEqualTo(4);
    }

    /**
     * 验证导出查询与分页查询使用同一分组条件。
     */
    @Test
    void exportQueryUsesSameGroupFilter() {
        assertThat(repository.findAllByCondition(null, null, null, "storage"))
                .extracting(Config::getConfigKey)
                .containsExactly("storage.a");
    }

    private void insert(String configKey, String configGroup) {
        jdbcTemplate.update("INSERT INTO sys_config "
                        + "(config_name, config_key, config_value, config_type, config_group, create_by, deleted) "
                        + "VALUES (?, ?, ?, 1, ?, 'system', 0)",
                configKey, configKey, "value", configGroup);
    }

    private DriverManagerDataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MYSQL_PORT)
                + "/iboot_config_group?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        return dataSource;
    }

    private void migrateWithRetry(final DriverManagerDataSource dataSource) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 30; attempt++) {
            try {
                SchemaMigrator.migrate(dataSource);
                return;
            } catch (RuntimeException e) {
                last = e;
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }
}
