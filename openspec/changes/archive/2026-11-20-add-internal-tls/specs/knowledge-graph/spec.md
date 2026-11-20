# Spec Delta

## ADDED Requirements

### Requirement: Neo4j 连接使用加密 Bolt

启用知识图谱时，系统 SHALL 通过加密的 Bolt 或 Neo4j 协议连接图数据库。

- 连接 URI MUST 使用 `bolt+s://` 或 `neo4j+s://`。
- 驱动 MUST 使用本地 CA 校验服务端证书。
- 明文 `bolt://` 连接 MUST 被拒绝，系统 MUST NOT 回退为明文。
- 图同步在加密连接下 MUST 保持原有的幂等与原子语义。

#### Scenario: 加密连接完成同步

- **WHEN** 知识图谱同步连接 `bolt+s://` Neo4j
- **THEN** 连接建立并完成一次幂等同步
- **AND** 服务端证书通过本地 CA 校验

#### Scenario: 明文连接被拒绝

- **WHEN** 客户端以 `bolt://` 连接 Neo4j 的加密端口
- **THEN** 连接失败
- **AND** 系统不会改用明文重试
