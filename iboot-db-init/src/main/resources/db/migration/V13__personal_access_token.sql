-- 个人访问令牌（PAT）元数据。
--
-- 只保存令牌标识与生命周期信息，不保存令牌原文或其哈希：
-- 令牌本身是认证服务签名的 JWT，离开签名私钥无法伪造。
-- 撤销以共享存储中删除的键 + revoked 标记共同表达，服务端不依赖令牌自然过期。

CREATE TABLE `sys_user_pat`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '令牌记录ID',
    `user_id`     bigint      NOT NULL COMMENT '用户ID',
    `name`        varchar(64) NOT NULL COMMENT '令牌名称',
    `token_id`    varchar(64) NOT NULL COMMENT '令牌唯一标识（JWT jti）',
    `expires_at`  datetime    NOT NULL COMMENT '过期时间',
    `revoked`     tinyint     NOT NULL DEFAULT 0 COMMENT '撤销标记：0-有效，1-已撤销',
    `revoked_at`  datetime             DEFAULT NULL COMMENT '撤销时间',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pat_token_id` (`token_id`),
    KEY `idx_pat_user` (`user_id`, `revoked`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='个人访问令牌表';
