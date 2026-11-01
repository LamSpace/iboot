-- iBoot 数据库初始化脚本
-- 业务库由 iboot-db-init 创建并执行全部迁移；这里只保证容器初始化时库存在

CREATE DATABASE IF NOT EXISTS iboot CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE iboot;

-- 表结构与种子数据由 iboot-db-init 统一管理
-- 详见 iboot-db-init/src/main/resources/db/migration 目录

-- 插入默认管理员用户 (密码为明文 'admin123'，实际部署时应使用加密密码)
-- 由于表将在 Flyway 迁移中创建，此处不再插入数据