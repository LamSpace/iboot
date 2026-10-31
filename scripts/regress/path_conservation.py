"""校验服务迁移的路径守恒。

规则：
- 全部服务快照的路径并集在迁移前后必须完全相同；
- 并集中消失的路径必须由 migrations.txt 的登记解释，且确实出现在目标服务快照里；
- 并集中新出现的路径同样必须由登记解释（即它是迁移的到达端）；
- 已登记的迁移必须在源服务中不再存在，否则视为「迁移没做完」。

用法：python3 -I path_conservation.py <当前快照目录> <基线目录> <migrations.txt>
"""

import json
import os
import sys


def load_paths(directory):
    """读取目录下全部快照，返回 {服务名: 路径集合}。"""
    result = {}
    for name in sorted(os.listdir(directory)):
        if not name.endswith(".json"):
            continue
        service = name[: -len(".json")]
        with open(os.path.join(directory, name), encoding="utf-8") as handle:
            document = json.load(handle)
        if "paths" not in document:
            # 目录里还可能有等价性基线等非快照文件，跳过
            continue
        result[service] = set(document.get("paths", {}))
    return result


def load_migrations(path):
    """读取迁移登记，返回 [(前缀, 源服务, 目标服务, 状态)]。"""
    entries = []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split()
            if len(parts) != 4 or parts[3] not in ("transitional", "done"):
                raise ValueError(f"迁移登记格式错误（需 4 列且状态为 transitional/done）: {line}")
            entries.append((parts[0].rstrip("/"), parts[1], parts[2], parts[3]))
    return entries


def covers(prefix, path):
    """判断路径是否落在登记的前缀下。"""
    return path == prefix or path.startswith(prefix + "/")


def main() -> int:
    if len(sys.argv) != 4:
        print("用法: path_conservation.py <当前快照目录> <基线目录> <migrations.txt>", file=sys.stderr)
        return 2

    current = load_paths(sys.argv[1])
    baseline = load_paths(sys.argv[2])
    migrations = load_migrations(sys.argv[3])

    current_union = set().union(*current.values()) if current else set()
    baseline_union = set().union(*baseline.values()) if baseline else set()

    failures = []

    for path in sorted(baseline_union - current_union):
        matched = [entry for entry in migrations if covers(entry[0], path)]
        if not matched:
            failures.append(f"路径消失且未登记迁移: {path}")
            continue
        prefix, _, target, _ = matched[0]
        if path not in current.get(target, set()):
            failures.append(f"登记迁移到 {target} 但目标快照中没有: {path}")

    for path in sorted(current_union - baseline_union):
        matched = [entry for entry in migrations if covers(entry[0], path)]
        if not matched:
            failures.append(f"路径凭空出现且未登记迁移: {path}")

    for prefix, source, target, state in migrations:
        still_in_source = sorted(p for p in current.get(source, set()) if covers(prefix, p))
        in_target_now = sorted(p for p in current.get(target, set()) if covers(prefix, p))
        if not in_target_now:
            failures.append(f"登记迁移未落地: {target} 中没有 {prefix} 路径")
        elif state == "done" and still_in_source:
            failures.append(
                f"登记迁移未完成: {source} 仍有 {len(still_in_source)} 条 {prefix} 路径，"
                f"应把状态改为 done 之前先删除旧实现"
            )
        elif state == "transitional" and still_in_source:
            print(f"[INFO] 过渡期保留: {source} 仍有 {len(still_in_source)} 条 {prefix} 路径（路由指向 {target}）")

    if failures:
        for message in failures:
            print(f"[FAIL] {message}")
        return 1

    print(f"路径守恒通过：{len(current_union)} 条路径，{len(migrations)} 条迁移登记")
    return 0


if __name__ == "__main__":
    sys.exit(main())
