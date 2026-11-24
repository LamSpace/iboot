# Spec Delta

## ADDED Requirements

### Requirement: CA 轮换回滚

CA 三阶段轮换 SHALL 提供回滚入口，可在 `prepared` 或 `activated` 阶段回到上一张 CA。

- `prepared` 回滚 MUST 丢弃下一张 CA、把信任 bundle 恢复为上一张 CA，MUST NOT 改变叶子证书。
- `activated` 回滚 MUST 恢复上一张 CA 为签名者并重签全部叶子证书，bundle MUST 只保留上一张 CA。
- 回滚后状态 MUST 回到 `initial`，`--status` MUST 报告 `rollback=unavailable`。
- `finalized` 后 MUST 拒绝自动回滚，并提示从备份恢复或重新轮换。
- 回滚 MUST 重建信任库与运行期副本，并按 `--restart` 重启运行时。
- 回归 MUST 在临时证书目录验证两种回滚路径与证书可验证性。

#### Scenario: prepared 后中止

- **WHEN** 执行 `--prepare` 后决定不激活新 CA
- **THEN** 执行 `--rollback` 后 bundle 只包含上一张 CA
- **AND** 叶子证书签发者与有效期不变

#### Scenario: activated 后回退

- **WHEN** 执行 `--activate` 后发现需要回到旧 CA
- **THEN** 执行 `--rollback` 后叶子证书由上一张 CA 重新签发
- **AND** bundle 只信任上一张 CA，叶子证书可验证

#### Scenario: finalized 后拒绝

- **WHEN** 执行 `--finalize` 后尝试 `--rollback`
- **THEN** 脚本以非零退出码失败
- **AND** 不修改任何证书或信任 bundle
