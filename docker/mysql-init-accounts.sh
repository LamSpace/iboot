#!/usr/bin/env bash
# 首次初始化或手工引导：删除远程 root，创建迁移账号与账号管理账号。
#
# 由 MySQL 官方镜像的 /docker-entrypoint-initdb.d 在空数据卷首次启动时执行；
# 已有数据卷可在容器内手工执行一次：
#   docker exec iboot-mysql /scripts/mysql-init-accounts.sh
#
# 环境变量：
#   MYSQL_ROOT_PASSWORD（仅容器内 local socket 引导）
#   MYSQL_MIGRATOR_PASSWORD / MYSQL_PROVISIONER_PASSWORD
#   MYSQL_DATABASE（默认 iboot）

set -euo pipefail

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD 必须在 .env 中设置}"
MYSQL_MIGRATOR_PASSWORD="${MYSQL_MIGRATOR_PASSWORD:?MYSQL_MIGRATOR_PASSWORD 必须在 .env 中设置}"
MYSQL_PROVISIONER_PASSWORD="${MYSQL_PROVISIONER_PASSWORD:?MYSQL_PROVISIONER_PASSWORD 必须在 .env 中设置}"
MYSQL_DATABASE="${MYSQL_DATABASE:-iboot}"

sql_escape() {
    printf '%s' "$1" | sed "s/'/''/g"
}

ROOT_PASSWORD_SQL="$(sql_escape "$MYSQL_ROOT_PASSWORD")"
MIGRATOR_PASSWORD_SQL="$(sql_escape "$MYSQL_MIGRATOR_PASSWORD")"
PROVISIONER_PASSWORD_SQL="$(sql_escape "$MYSQL_PROVISIONER_PASSWORD")"
DATABASE_SQL="$(sql_escape "$MYSQL_DATABASE")"

MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot --default-character-set=utf8mb4 <<SQL
DROP USER IF EXISTS 'root'@'%';

-- skip_name_resolve=ON 时 127.0.0.1 不会映射到 localhost；只补一个回环 TCP 入口。
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY '${ROOT_PASSWORD_SQL}';
ALTER USER 'root'@'127.0.0.1' IDENTIFIED BY '${ROOT_PASSWORD_SQL}';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;

CREATE USER IF NOT EXISTS 'iboot_migrator'@'%' IDENTIFIED BY '${MIGRATOR_PASSWORD_SQL}';
ALTER USER 'iboot_migrator'@'%' IDENTIFIED BY '${MIGRATOR_PASSWORD_SQL}';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'iboot_migrator'@'%';
GRANT ALL PRIVILEGES ON \`${DATABASE_SQL}\`.* TO 'iboot_migrator'@'%';

CREATE USER IF NOT EXISTS 'iboot_provisioner'@'%' IDENTIFIED BY '${PROVISIONER_PASSWORD_SQL}';
ALTER USER 'iboot_provisioner'@'%' IDENTIFIED BY '${PROVISIONER_PASSWORD_SQL}';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'iboot_provisioner'@'%';
GRANT CREATE USER ON *.* TO 'iboot_provisioner'@'%';
GRANT RELOAD ON *.* TO 'iboot_provisioner'@'%';
GRANT PROCESS, REPLICATION CLIENT ON *.* TO 'iboot_provisioner'@'%' WITH GRANT OPTION;
GRANT SELECT ON \`performance_schema\`.* TO 'iboot_provisioner'@'%' WITH GRANT OPTION;
GRANT SELECT, INSERT, UPDATE, DELETE ON \`${DATABASE_SQL}\`.* TO 'iboot_provisioner'@'%' WITH GRANT OPTION;

FLUSH PRIVILEGES;
SQL

printf '[mysql-init-accounts] 已创建 iboot_migrator 与 iboot_provisioner，并移除 root@%%\n'
