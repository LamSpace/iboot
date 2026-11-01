"""录制或校验只读接口的响应等价性。

迁移前从原服务录制（--record），迁移后经网关重放（--verify），两侧结果必须一致。
只覆盖只读请求；上传/删除的响应含自增 ID 与时间戳，由冒烟覆盖。

用法：python3 -I equivalence.py <base-url> <token> <output-file> [service]

service 取 file（默认）、notify、job、workflow 或 monitor，决定采集哪一组只读请求。
"""

import hashlib
import io
import json
import sys
import urllib.error
import urllib.request
import zipfile


def fetch(base, token, path):
    """发起请求，返回 (状态码, 内容类型, 字节)。"""
    request = urllib.request.Request(base + path, headers={"Authorization": "Bearer " + token})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, response.headers.get("Content-Type", ""), response.read()
    except urllib.error.HTTPError as error:
        return error.code, error.headers.get("Content-Type", ""), error.read()


def normalize(node):
    """递归归一化：去掉时间戳，掩掉预签名地址的查询参数。"""
    if isinstance(node, dict):
        is_envelope = node.get("specversion") is not None
        result = {}
        for key, value in sorted(node.items()):
            if key == "timestamp":
                result[key] = "<ts>"
            elif is_envelope and key in ("id", "time"):
                result[key] = "<volatile>"
            else:
                result[key] = normalize(value)
        return result
    if isinstance(node, list):
        return [normalize(item) for item in node]
    return node


def masked_url(value):
    """去掉预签名地址中的易变查询参数。"""
    index = value.find("?")
    return value if index < 0 else value[:index] + "?<masked>"


def evaluate(base, token, path):
    """执行一条请求并返回可比较的结果。"""
    status, content_type, body = fetch(base, token, path)
    if "json" in content_type:
        try:
            data = normalize(json.loads(body.decode("utf-8")))
            if path.startswith("/api/file/download-url/"):
                payload = data.get("data", {}).get("data", {})
                for key, value in list(payload.items()):
                    if isinstance(value, str) and value.startswith("http"):
                        payload[key] = masked_url(value)
            return {"status": status, "body": data}
        except (ValueError, AttributeError):
            return {"status": status, "sha256": hashlib.sha256(body).hexdigest()}
    if "spreadsheetml" in content_type:
        return {"status": status, "contentType": content_type, "sheetSha256": sheet_hash(body)}
    return {"status": status, "contentType": content_type, "sha256": hashlib.sha256(body).hexdigest()}


def sheet_hash(body):
    """只对工作表单元格内容取哈希，忽略文档属性里的生成时间。"""
    try:
        with zipfile.ZipFile(io.BytesIO(body)) as archive:
            names = sorted(name for name in archive.namelist() if name.startswith("xl/worksheets/"))
            digest = hashlib.sha256()
            for name in names:
                digest.update(name.encode("utf-8"))
                digest.update(archive.read(name))
            return digest.hexdigest()
    except zipfile.BadZipFile:
        return hashlib.sha256(body).hexdigest()


def collect_file(base, token):
    """文件服务：列表、详情、下载地址与导出。"""
    list_result = evaluate(base, token, "/api/file/list?pageNum=1&pageSize=10")
    results = {"list": list_result}

    rows = []
    try:
        rows = list_result["body"]["data"]["data"]["data"]
    except (KeyError, TypeError):
        pass
    if rows:
        file_id = rows[0]["id"]
        results["detail"] = evaluate(base, token, f"/api/file/{file_id}")
        results["downloadUrl"] = evaluate(base, token, f"/api/file/download-url/{file_id}")
    else:
        results["detail"] = {"skipped": "无文件可查"}
        results["downloadUrl"] = {"skipped": "无文件可查"}

    results["export"] = evaluate(base, token, "/api/file/export")
    return results


def collect_notify(base, token):
    """消息与通知服务：消息列表、收件箱、公告列表、已发布公告与检索。

    检索使用空关键字，返回稳定的空结果；带关键字的结果会随冒烟写入变化，
    不适合作为迁移前后的等价基线。
    """
    return {
        "messageList": evaluate(base, token, "/api/message/list?pageNum=1&pageSize=10"),
        "messageInbox": evaluate(base, token, "/api/message/inbox?pageNum=1&pageSize=10"),
        "noticeList": evaluate(base, token, "/api/notice/list?pageNum=1&pageSize=10"),
        "noticePublished": evaluate(base, token, "/api/notice/published"),
        "search": evaluate(base, token, "/api/search?pageNum=1&pageSize=10&keyword="),
    }


def collect_job(base, token):
    """调度服务：任务列表、任务详情、执行日志、备份记录、还原记录与备份汇总。

    只取只读接口；写操作（立即执行、触发备份）由冒烟覆盖，录制与校验之间不能有写入，
    否则记录数量变化会被当成差异。
    """
    list_result = evaluate(base, token, "/api/job/list?pageNum=1&pageSize=10")
    results = {"jobList": list_result, "jobLogList": evaluate(base, token,
                                                             "/api/job/log/list?pageNum=1&pageSize=10")}
    rows = []
    try:
        rows = list_result["body"]["data"]["data"]["data"]
    except (KeyError, TypeError):
        pass
    if rows:
        results["jobDetail"] = evaluate(base, token, f"/api/job/{rows[0]['id']}")
    else:
        results["jobDetail"] = {"skipped": "无任务可查"}

    results["backupRecords"] = evaluate(base, token, "/api/monitor/backup/records?pageNum=1&pageSize=10")
    results["restoreRecords"] = evaluate(base, token, "/api/monitor/backup/restores?pageNum=1&pageSize=10")
    results["backupSummary"] = evaluate(base, token, "/api/monitor/backup/summary")
    return results


def collect_workflow(base, token):
    """审批服务：我的申请、待办、已办与首条详情。

    只取只读接口；发起、通过、驳回由冒烟覆盖，录制与校验之间不能有写入，
    否则分页数量与列表内容变化会被当成差异。
    """
    mine = evaluate(base, token, "/api/approval/mine?pageNum=1&pageSize=10")
    results = {
        "mine": mine,
        "todo": evaluate(base, token, "/api/approval/todo?pageNum=1&pageSize=10"),
        "done": evaluate(base, token, "/api/approval/done?pageNum=1&pageSize=10"),
    }
    rows = []
    try:
        rows = mine["body"]["data"]["data"]["data"]
    except (KeyError, TypeError):
        pass
    if rows:
        results["detail"] = evaluate(base, token, f"/api/approval/{rows[0]['id']}")
    else:
        results["detail"] = {"skipped": "无申请可查"}
    return results


def collect_monitor(base, token):
    """监控与日志服务：操作日志列表与详情，以及三条必然为空的过滤查询。

    登录日志与在线会话会随回归过程中的登录而变化，运行日志由 ELK 持续写入，
    三者都不适合作为迁移前后的等价基线。这里取操作日志（两次采集之间没有新的
    带审计注解的请求）和三条非匹配过滤，验证过滤链路与响应结构未发生漂移。
    """
    page = evaluate(base, token, "/api/log/operate/list?pageNum=1&pageSize=10")
    results = {
        "operatePage": page,
        "loginFiltered": evaluate(base, token,
                                  "/api/log/login/list?pageNum=1&pageSize=10&username=__equivalence__"),
        "runFiltered": evaluate(base, token,
                                "/api/log/run/list?pageNum=1&pageSize=10&keyword=__equivalence__"),
        "onlineFiltered": evaluate(base, token, "/api/online/list?username=__equivalence__"),
    }
    rows = []
    try:
        rows = page["body"]["data"]["data"]["data"]
    except (KeyError, TypeError):
        pass
    if rows:
        results["operateDetail"] = evaluate(base, token, f"/api/log/operate/{rows[0]['id']}")
    else:
        results["operateDetail"] = {"skipped": "无操作日志可查"}
    return results


PROFILES = {
    "file": collect_file,
    "notify": collect_notify,
    "job": collect_job,
    "workflow": collect_workflow,
    "monitor": collect_monitor,
}


def main() -> int:
    if len(sys.argv) not in (4, 5):
        print("用法: equivalence.py <base-url> <token> <output-file> [file|notify|job|workflow|monitor]", file=sys.stderr)
        return 2
    base, token, output = sys.argv[1].rstrip("/"), sys.argv[2], sys.argv[3]
    service = sys.argv[4] if len(sys.argv) == 5 else "file"
    collector = PROFILES.get(service)
    if collector is None:
        print(f"未知服务: {service}", file=sys.stderr)
        return 2

    results = collector(base, token)

    with open(output, "w", encoding="utf-8") as handle:
        json.dump(results, handle, ensure_ascii=False, indent=2, sort_keys=True)
        handle.write("\n")
    print(f"{len(results)} 条只读请求已写入 {output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
