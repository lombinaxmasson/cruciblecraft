#!/usr/bin/env python3
"""Shared path and JSON helpers for live semantic tools."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"


def relative(path: Path) -> str:
    try:
        return path.resolve().relative_to(ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def write_stable(path: Path, document: Any) -> None:
    from tools import atomic_io

    payload = stable_json(document).encode("utf-8")
    if path.is_file() and path.read_bytes() == payload:
        return
    atomic_io.write_bytes(path, payload)


def stale_error(path: Path, expected_text: str, actual_text: str) -> str:
    rel = relative(path)
    if not path.is_file():
        return f"{rel} is stale (missing)"
    if expected_text == actual_text:
        return f"{rel} is stale"
    return f"{rel} is stale (content drifted)"
