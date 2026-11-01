# Tasks

> 删除与保留的逐段依据见 `design.md` 附录 A；两份文档的分工见 `design.md` D3。

## 1. ROADMAP 重写

- [ ] 1.1 生成能力一句话索引（约 14 行，字段：能力 / 一句话 / 归属服务 / 对应文档），逐项对照 `openspec list --specs` 的 19 份能力与 `doc/` 目录，确认没有能力漏项、没有指向不存在的文档
- [ ] 1.2 把阶段 2 未完成的收尾项写成独立一节：Seata、Sentinel 业务接入、RocketMQ、`iboot_admin` 改名、遗留表清理；每项都要在代码里复核仍是未完成状态（如 `grep -rn seata` 无命中），并在该节内注明证据方式
- [ ] 1.3 保留阶段 3、阶段 4 两节原内容，重画总览图（只保留阶段 3/4 路径），确认文中不再出现「阶段 0/1/2 已完成」的叙述
- [ ] 1.4 保留版本矩阵、成本红线，风险登记只留 R2/R4/R6/R12/R13；确认每条保留的风险处置列不是「已消除」或「已规避」
- [ ] 1.5 在决策记录中新增 7.5（认证服务不签发刷新令牌，会话靠滑动续期）与 7.6（系统服务是唯一连带点，故障隔离判据为「失败有界」）；确认跨 schema 只读的长期接受结论仍指向 `doc/23` §11.12
- [ ] 1.6 按 `design.md` 附录 A 删除 §1、§4.0、§4.1、§4.2 的交付叙述与 §5；确认 `wc -l ROADMAP.md` ≤ 230，且 `grep -E "add-(job|notify|workflow|monitor)-service|remove-.*-from-admin" ROADMAP.md` 无命中
- [ ] 1.7 重新生成目录并更新「最近更新」日期；确认目录每个锚点都能跳到对应标题

## 2. README 重写

- [ ] 2.1 写架构总览：ASCII 拓扑图 + 服务/端口表（网关与 8 个业务服务）+ schema 表；确认端口与各模块 `application.yml` 的 `server.port` 逐一相符（8088 / 8080–8087）
- [ ] 2.2 技术栈逐条与 `pom.xml` 对齐，删除 PostgreSQL 等不存在的条目；确认 `grep -i postgres README.md` 无命中，且列出的每项在 pom 中存在对应坐标
- [ ] 2.3 功能清单按服务分组（9 组），每组只写用户可见功能；确认与原 README 的「日志管理/安全管理」重复段落已合并为一处
- [ ] 2.4 快速开始改为以 `scripts/dev-init.sh` 为主路径，前端命令统一 cnpm，并写明密钥位于 `scripts/dev.env`（与仓库现状一致）；确认文中出现的每个脚本路径都真实存在
- [ ] 2.5 目录结构补齐十个模块并删除重复的第二份目录树；确认树中目录与 `ls -d iboot-*/` 的输出一致
- [ ] 2.6 补「构建 / 测试 / 回归」与「镜像与部署现状」两节；部署一节如实说明仅 `iboot-system`、`iboot-portal` 有 Dockerfile，并指向 ROADMAP 阶段 3
- [ ] 2.7 补文档导航（指向 `doc/README.md` 与 OpenSpec 目录）与许可证说明；确认 `wc -l README.md` ≤ 320

## 3. 环境模板与链接

- [ ] 3.1 修正 `.env.example` 中指向 `http://iboot-admin` 的 `IBOOT_JOB_SCOPE_URL`；确认 `grep -n "iboot-admin" .env.example` 无命中
- [ ] 3.2 修复含空格的文档链接（如 `doc/22-AI 工作台操作指南.md`），统一用 `<...>` 包裹或 `%20` 编码；写一个一次性检查命令遍历 README 与 ROADMAP 的相对链接，确认每个目标文件都存在

## 4. 一致性复核

- [ ] 4.1 交叉核对 ROADMAP 能力索引与 README 功能清单：确认两者字段差异符合 `design.md` D3（索引带归属与文档指针，功能清单带用户可见行为），不存在同一句话在两处重复
- [ ] 4.2 核对 ROADMAP 中每个「未完成」项在 README 中都没有被描述为「已具备」，反之亦然；确认阶段 2 收尾清单的 5 项在 README 中均未出现
- [ ] 4.3 确认工作区只改动了 `ROADMAP.md`、`README.md`、`.env.example` 三个文件（`git status --short`），没有误改代码或文档目录下的其他文件
