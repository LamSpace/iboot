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

package com.iboot.admin.infrastructure.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据备份配置。
 *
 * <p>绑定 {@code application.yml} 中的 {@code iboot.backup.*}，覆盖功能开关、主密钥、
 * 数据库连接、导出/还原命令前缀、演练库命名与超时阈值。密钥只从环境变量注入，
 * 不写入配置文件。命令前缀以列表形式配置，避免任何 shell 拼接。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "iboot.backup")
public class BackupProperties {

    /**
     * 是否启用数据备份能力，默认关闭。
     */
    private boolean enabled = false;

    /**
     * AES-256-GCM 主密钥，Base64 编码，解码后必须为 32 字节。
     */
    private String encryptionKey;

    /**
     * 备份源数据库连接。
     */
    private Database database = new Database();

    /**
     * 导出与还原命令前缀。
     */
    private Command command = new Command();

    /**
     * 恢复演练配置。
     */
    private Restore restore = new Restore();

    /**
     * 运行中记录超过该分钟数视为超时，下次执行时重置为失败。
     */
    private int staleRunningMinutes = 30;

    /**
     * 是否启用备份能力。
     *
     * @return 启用返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用备份能力。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取主密钥。
     *
     * @return Base64 编码的密钥
     */
    public String getEncryptionKey() {
        return encryptionKey;
    }

    /**
     * 设置主密钥。
     *
     * @param encryptionKey Base64 编码的密钥
     */
    public void setEncryptionKey(final String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    /**
     * 获取数据库连接配置。
     *
     * @return 数据库连接配置
     */
    public Database getDatabase() {
        return database;
    }

    /**
     * 设置数据库连接配置。
     *
     * @param database 数据库连接配置
     */
    public void setDatabase(final Database database) {
        this.database = database;
    }

    /**
     * 获取命令配置。
     *
     * @return 命令配置
     */
    public Command getCommand() {
        return command;
    }

    /**
     * 设置命令配置。
     *
     * @param command 命令配置
     */
    public void setCommand(final Command command) {
        this.command = command;
    }

    /**
     * 获取恢复演练配置。
     *
     * @return 恢复演练配置
     */
    public Restore getRestore() {
        return restore;
    }

    /**
     * 设置恢复演练配置。
     *
     * @param restore 恢复演练配置
     */
    public void setRestore(final Restore restore) {
        this.restore = restore;
    }

    /**
     * 获取超时分钟数。
     *
     * @return 运行中记录的超时分钟数
     */
    public int getStaleRunningMinutes() {
        return staleRunningMinutes;
    }

    /**
     * 设置超时分钟数。
     *
     * @param staleRunningMinutes 超时分钟数
     */
    public void setStaleRunningMinutes(final int staleRunningMinutes) {
        this.staleRunningMinutes = staleRunningMinutes;
    }

    /**
     * 备份源数据库连接配置。
     *
     * <p>与业务数据源分开配置，原因是导出/还原命令在数据库外部执行，且恢复演练需要
     * 连接服务器级会话创建独立库。默认值与业务数据源的环境变量保持一致。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    public static class Database {

        /**
         * 数据库主机。
         */
        private String host = "localhost";

        /**
         * 数据库端口。
         */
        private int port = 3306;

        /**
         * 数据库名。
         */
        private String name = "iboot_admin";

        /**
         * 用户名。
         */
        private String username = "root";

        /**
         * 密码。
         */
        private String password = "root";

        /**
         * 获取主机。
         *
         * @return 主机名
         */
        public String getHost() {
            return host;
        }

        /**
         * 设置主机。
         *
         * @param host 主机名
         */
        public void setHost(final String host) {
            this.host = host;
        }

        /**
         * 获取端口。
         *
         * @return 端口
         */
        public int getPort() {
            return port;
        }

        /**
         * 设置端口。
         *
         * @param port 端口
         */
        public void setPort(final int port) {
            this.port = port;
        }

        /**
         * 获取数据库名。
         *
         * @return 数据库名
         */
        public String getName() {
            return name;
        }

        /**
         * 设置数据库名。
         *
         * @param name 数据库名
         */
        public void setName(final String name) {
            this.name = name;
        }

        /**
         * 获取用户名。
         *
         * @return 用户名
         */
        public String getUsername() {
            return username;
        }

        /**
         * 设置用户名。
         *
         * @param username 用户名
         */
        public void setUsername(final String username) {
            this.username = username;
        }

        /**
         * 获取密码。
         *
         * @return 密码
         */
        public String getPassword() {
            return password;
        }

        /**
         * 设置密码。
         *
         * @param password 密码
         */
        public void setPassword(final String password) {
            this.password = password;
        }
    }

    /**
     * 导出与还原命令前缀配置。
     *
     * <p>前缀按原样作为进程参数传递，例如本地默认的
     * {@code docker exec -i -e MYSQL_PWD iboot-mysql mysqldump}。
     * 密码通过 {@code MYSQL_PWD} 进程环境传递，不出现在参数列表中。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    public static class Command {

        /**
         * 导出命令前缀。
         */
        private List<String> dumpPrefix = new ArrayList<>(List.of("mysqldump"));

        /**
         * 还原命令前缀。
         */
        private List<String> restorePrefix = new ArrayList<>(List.of("mysql"));

        /**
         * 命令视角的数据库主机；为空时使用 {@link Database#getHost()}。
         */
        private String host;

        /**
         * 命令视角的数据库端口；为空时使用 {@link Database#getPort()}。
         */
        private Integer port;

        /**
         * 获取导出命令前缀。
         *
         * @return 命令参数列表
         */
        public List<String> getDumpPrefix() {
            return dumpPrefix;
        }

        /**
         * 获取命令视角的数据库主机。
         *
         * @return 主机名，未配置时为 {@code null}
         */
        public String getHost() {
            return host;
        }

        /**
         * 设置命令视角的数据库主机。
         *
         * @param host 主机名
         */
        public void setHost(final String host) {
            this.host = host;
        }

        /**
         * 获取命令视角的数据库端口。
         *
         * @return 端口，未配置时为 {@code null}
         */
        public Integer getPort() {
            return port;
        }

        /**
         * 设置命令视角的数据库端口。
         *
         * @param port 端口
         */
        public void setPort(final Integer port) {
            this.port = port;
        }

        /**
         * 设置导出命令前缀。
         *
         * @param dumpPrefix 命令参数列表
         */
        public void setDumpPrefix(final List<String> dumpPrefix) {
            this.dumpPrefix = dumpPrefix;
        }

        /**
         * 获取还原命令前缀。
         *
         * @return 命令参数列表
         */
        public List<String> getRestorePrefix() {
            return restorePrefix;
        }

        /**
         * 设置还原命令前缀。
         *
         * @param restorePrefix 命令参数列表
         */
        public void setRestorePrefix(final List<String> restorePrefix) {
            this.restorePrefix = restorePrefix;
        }
    }

    /**
     * 恢复演练配置。
     *
     * @author iBoot Team
     * @since 1.0.0
     */
    public static class Restore {

        /**
         * 固定演练库名。
         */
        private String schema = "iboot_restore_drill";

        /**
         * 允许的演练库名前缀。
         */
        private String prefix = "iboot_restore_";

        /**
         * 获取演练库名。
         *
         * @return 演练库名
         */
        public String getSchema() {
            return schema;
        }

        /**
         * 设置演练库名。
         *
         * @param schema 演练库名
         */
        public void setSchema(final String schema) {
            this.schema = schema;
        }

        /**
         * 获取演练库前缀。
         *
         * @return 库名前缀
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * 设置演练库前缀。
         *
         * @param prefix 库名前缀
         */
        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }
    }
}
