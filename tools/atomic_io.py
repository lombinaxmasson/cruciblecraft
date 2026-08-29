#!/usr/bin/env python3
"""Atomic UTF-8 LF writers for committed verification artifacts."""
from __future__ import annotations

import os
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def write_bytes(path: Path, payload: bytes) -> None:
    """Write bytes via unique tmp -> flush/fsync -> os.replace -> finally cleanup."""
    destination = Path(path)
    destination.parent.mkdir(parents=True, exist_ok=True)
    tmp = destination.with_name(
        f".{destination.name}.{os.getpid()}.{uuid.uuid4().hex}.tmp"
    )
    try:
        with open(tmp, "wb") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(tmp, destination)
    finally:
        if tmp.exists():
            try:
                tmp.unlink()
            except OSError:
                pass


def write_text(path: Path, text: str) -> None:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    write_bytes(path, normalized.encode("utf-8"))


def write_json(path: Path, text: str) -> None:
    """Write a pre-serialized stable JSON document (UTF-8 LF)."""
    write_text(path, text)
