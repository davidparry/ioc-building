#!/usr/bin/env python3
"""Demo log monitor: tails a log file and posts bug reports to the code-review
bug-fix agent webhook when it sees ERROR lines or exceptions.

Usage:
    python3 log_monitor.py --file app.log --token <bugreport-token> \
        [--url http://localhost:8090/webhook/bugreport] [--service iot-gateway]

The token must match pragent.webhook.bugreport-token in application.properties.
A report is sent once per detected failure: the triggering ERROR line, up to
CONTEXT_LINES of surrounding log, and any following stack-trace lines.
"""

import argparse
import json
import re
import sys
import time
import urllib.request
from collections import deque
from datetime import datetime, timezone

ERROR_PATTERN = re.compile(r"\bERROR\b|\bFATAL\b|Exception|Traceback", re.IGNORECASE)
STACK_LINE_PATTERN = re.compile(r"^\s+(at\s|\.{3}\s|Caused by:)|^\S+(Exception|Error)[:\s]")
CONTEXT_LINES = 20
STACK_SETTLE_SECONDS = 1.0


def post_report(url: str, token: str, service: str, message: str,
                log_excerpt: str, stack_trace: str) -> None:
    body = json.dumps({
        "service": service,
        "severity": "error",
        "message": message[:500],
        "logExcerpt": log_excerpt,
        "stackTrace": stack_trace,
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }).encode()
    request = urllib.request.Request(
        f"{url}?token={token}", data=body,
        headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            print(f"-> {response.status} {response.read().decode().strip()}")
    except Exception as e:  # noqa: BLE001 - a demo monitor must keep tailing
        print(f"-> failed to post bug report: {e}", file=sys.stderr)


def follow(path: str):
    """Yield lines appended to the file, or None on idle ticks so callers
    waiting for more input (e.g. a stack trace) can time out."""
    with open(path, "r", errors="replace") as handle:
        handle.seek(0, 2)
        while True:
            line = handle.readline()
            if line:
                yield line.rstrip("\n")
            else:
                time.sleep(0.2)
                yield None


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", required=True, help="log file to tail")
    parser.add_argument("--token", required=True, help="pragent.webhook.bugreport-token value")
    parser.add_argument("--url", default="http://localhost:8090/webhook/bugreport")
    parser.add_argument("--service", default="iot-gateway")
    args = parser.parse_args()

    print(f"Tailing {args.file}; reporting failures to {args.url}")
    recent: deque[str] = deque(maxlen=CONTEXT_LINES)
    lines = follow(args.file)
    for line in lines:
        if line is None:
            continue
        recent.append(line)
        if not ERROR_PATTERN.search(line):
            continue

        # Collect the stack trace that usually follows the error line. Read
        # until lines stop looking like a stack frame (with a short settle
        # window so slow writers are not cut off mid-trace).
        stack: list[str] = []
        deadline = time.time() + STACK_SETTLE_SECONDS
        while time.time() < deadline:
            next_line = next(lines)
            if next_line is None:
                continue
            recent.append(next_line)
            if STACK_LINE_PATTERN.search(next_line):
                stack.append(next_line)
                deadline = time.time() + STACK_SETTLE_SECONDS
            elif stack:
                break

        print(f"Detected failure: {line.strip()[:120]}")
        post_report(args.url, args.token, args.service,
                    message=line.strip(),
                    log_excerpt="\n".join(recent),
                    stack_trace="\n".join(stack))


if __name__ == "__main__":
    main()
