#!/usr/bin/env python3
"""宿主机回环 TCP relay。

原生 Docker 无法通过 ``host.docker.internal`` 访问宿主机 ``127.0.0.1``。该脚本只绑定 Docker
网桥网关地址，把同一端口转发到 ``127.0.0.1``，让 Prometheus/blackbox 仍能抓取只监听回环的宿主机
Java 进程。它不做 TLS 终止，客户端证书与 SPIFFE 身份由业务服务原样校验。

安全约束：

- 拒绝绑定 ``0.0.0.0``、``::`` 或 ``*``，避免把回环端口扩大到全部网卡；
- 只转发 ``--ports`` 指定的端口，默认 ``8080-8089``；
- 任一监听地址绑定失败即退出，不静默降级。

用法::

    scripts/host-loopback-relay.py --bind 172.17.0.1,172.18.0.1 --ports 8080-8089 \
        --pid-file logs/host-loopback-relay.pid --log-file logs/host-loopback-relay.log
"""

from __future__ import annotations

import argparse
import asyncio
import logging
import os
import signal
import sys
from pathlib import Path

LOGGER = logging.getLogger("host-loopback-relay")

FORBIDDEN_BINDS = {"", "0.0.0.0", "::", "[::]", "*"}


def parse_ports(value: str) -> list[int]:
    """解析逗号分隔的端口或端口范围。"""
    ports: set[int] = set()
    for part in value.split(","):
        part = part.strip()
        if not part:
            continue
        if "-" in part:
            start_text, end_text = part.split("-", 1)
            start, end = int(start_text), int(end_text)
            if start > end or start < 1 or end > 65535:
                raise ValueError(f"端口范围不合法：{part}")
            ports.update(range(start, end + 1))
        else:
            port = int(part)
            if port < 1 or port > 65535:
                raise ValueError(f"端口不合法：{part}")
            ports.add(port)
    if not ports:
        raise ValueError("没有可用端口")
    return sorted(ports)


def parse_binds(value: str) -> list[str]:
    """解析逗号分隔的绑定地址，并拒绝会扩大暴露面的地址。"""
    binds = [item.strip() for item in value.split(",") if item.strip()]
    if not binds:
        raise ValueError("至少需要一个绑定地址")
    forbidden = [item for item in binds if item in FORBIDDEN_BINDS]
    if forbidden:
        raise ValueError(f"拒绝绑定全部网卡地址：{','.join(forbidden)}")
    return binds


async def _pipe(reader: asyncio.StreamReader, writer: asyncio.StreamWriter) -> None:
    """单向搬运字节，连接关闭或对端重置时安静结束。"""
    try:
        while True:
            data = await reader.read(65536)
            if not data:
                break
            writer.write(data)
            await writer.drain()
    except (ConnectionError, asyncio.CancelledError):
        pass
    finally:
        writer.close()
        try:
            await writer.wait_closed()
        except Exception:
            pass


async def _handle(reader: asyncio.StreamReader, writer: asyncio.StreamWriter, port: int) -> None:
    """处理一个接入连接：连接到 127.0.0.1 的同端口并双向转发。"""
    try:
        target_reader, target_writer = await asyncio.open_connection("127.0.0.1", port)
    except OSError as exc:
        LOGGER.warning("连接 127.0.0.1:%s 失败：%s", port, exc)
        writer.close()
        return
    await asyncio.gather(
        _pipe(reader, target_writer),
        _pipe(target_reader, writer),
    )


async def _run(binds: list[str], ports: list[int], pid_file: Path | None) -> int:
    servers: list[asyncio.AbstractServer] = []
    try:
        for bind in binds:
            for port in ports:
                try:
                    server = await asyncio.start_server(
                        lambda reader, writer, p=port: _handle(reader, writer, p),
                        bind,
                        port,
                    )
                except OSError as exc:
                    LOGGER.error("绑定 %s:%s 失败：%s", bind, port, exc)
                    return 1
                servers.append(server)
                LOGGER.info("转发 %s:%s -> 127.0.0.1:%s", bind, port, port)
        if pid_file is not None:
            pid_file.parent.mkdir(parents=True, exist_ok=True)
            pid_file.write_text(f"{os.getpid()}\n", encoding="utf-8")

        stop = asyncio.Event()
        loop = asyncio.get_running_loop()
        for sig in (signal.SIGTERM, signal.SIGINT):
            try:
                loop.add_signal_handler(sig, stop.set)
            except NotImplementedError:
                pass
        await stop.wait()
        return 0
    finally:
        for server in servers:
            server.close()
        for server in servers:
            await server.wait_closed()
        if pid_file is not None:
            try:
                pid_file.unlink()
            except FileNotFoundError:
                pass


def main() -> int:
    parser = argparse.ArgumentParser(description="Docker 网关到宿主机回环的 TCP relay")
    parser.add_argument("--bind", required=True, help="逗号分隔的绑定地址，只能是网桥网关")
    parser.add_argument("--ports", default="8080-8089", help="端口或范围，逗号分隔，默认 8080-8089")
    parser.add_argument("--pid-file", default="", help="PID 文件")
    parser.add_argument("--log-file", default="", help="日志文件；留空输出到 stderr")
    args = parser.parse_args()

    logging.basicConfig(
        filename=args.log_file or None,
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
    )
    try:
        binds = parse_binds(args.bind)
        ports = parse_ports(args.ports)
    except ValueError as exc:
        LOGGER.error("%s", exc)
        return 2
    pid_file = Path(args.pid_file) if args.pid_file else None
    try:
        return asyncio.run(_run(binds, ports, pid_file))
    except KeyboardInterrupt:
        return 0


if __name__ == "__main__":
    sys.exit(main())
