"""把 OpenAPI 文档归一化成可稳定 diff 的基线。

归一化规则（与 tasks.md 1.1 一致）：
- 丢弃 servers：它随部署环境变化，与接口契约无关；
- 丢弃 example / examples：示例值随实现调整，不构成契约；
- 路径按字典序排列：保证两次导出的键顺序一致。

用法：python3 -I normalize_openapi.py <输入 JSON> <输出文件>
"""

import json
import sys

DROPPED_KEYS = {"servers", "example", "examples", "x-examples"}


def clean(node):
    if isinstance(node, dict):
        return {k: clean(v) for k, v in node.items() if k not in DROPPED_KEYS}
    if isinstance(node, list):
        return [clean(item) for item in node]
    return node


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__.strip(), file=sys.stderr)
        return 2

    source, target = sys.argv[1], sys.argv[2]
    with open(source, encoding="utf-8") as handle:
        document = clean(json.load(handle))

    paths = document.get("paths") or {}
    document["paths"] = {path: paths[path] for path in sorted(paths)}

    text = json.dumps(document, ensure_ascii=False, sort_keys=True, indent=2)
    with open(target, "w", encoding="utf-8") as handle:
        handle.write(text + "\n")

    print(f"{len(document['paths'])} paths -> {target}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
