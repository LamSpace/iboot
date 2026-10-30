"""校验接口路径与网关路由规则的覆盖关系。

规则（与 tasks.md 1.3 一致）：
- 每条路径必须至少被一条路由规则覆盖；
- 匹配采用最长前缀优先，因此同时命中兜底前缀与具体前缀不算冲突；
- 路由规则中不允许出现重复前缀。

用法：python3 -I route_coverage.py <golden 目录> <routes.txt>
"""

import json
import os
import sys


def load_rules(path):
    rules = []
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split()
            if len(parts) != 2:
                raise ValueError(f"路由规则格式错误: {line}")
            rules.append((parts[0], parts[1]))
    return rules


def resolve(path, rules):
    best = None
    for prefix, service in rules:
        normalized = prefix.rstrip("/")
        if path == normalized or path.startswith(normalized + "/"):
            if best is None or len(normalized) > len(best[0]):
                best = (normalized, service)
    return best


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    golden_dir, routes_file = sys.argv[1], sys.argv[2]
    rules = load_rules(routes_file)

    prefixes = set()
    for prefix, _ in rules:
        if prefix in prefixes:
            print(f"[ERROR] 路由规则存在重复前缀: {prefix}", file=sys.stderr)
            return 1
        prefixes.add(prefix)

    errors = []
    counts = {}
    for name in sorted(os.listdir(golden_dir)):
        if not name.endswith(".json"):
            continue
        with open(os.path.join(golden_dir, name), encoding="utf-8") as handle:
            document = json.load(handle)
        for path in document.get("paths", {}):
            match = resolve(path, rules)
            if match is None:
                errors.append(f"{name}: 未覆盖路径 {path}")
            else:
                counts[match[1]] = counts.get(match[1], 0) + 1

    for service in sorted(counts):
        print(f"  {service}: {counts[service]} 条路径")

    if errors:
        print(f"\n存在 {len(errors)} 条问题：", file=sys.stderr)
        for item in errors:
            print(f"  - {item}", file=sys.stderr)
        return 1

    print(f"覆盖检查通过，共 {sum(counts.values())} 条路径")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
