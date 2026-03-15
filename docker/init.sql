-- iBoot 数据库初始化脚本
-- 由于使用 Flyway 进行数据库版本管理，这里只创建数据库

CREATE DATABASE IF NOT EXISTS iboot_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE iboot_admin;

-- 由于使用 Flyway 进行数据库迁移，具体的表结构将在应用启动时自动创建
-- 详见 src/main/resources/db/migration 目录下的迁移脚本

-- 插入默认管理员用户 (密码为明文 'admin123'，实际部署时应使用加密密码)
-- 由于表将在 Flyway 迁移中创建，此处不再插入数据