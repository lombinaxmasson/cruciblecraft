#!/usr/bin/python3
"""Explicit dump-row remainder. Hashes are the only membership authority."""
from __future__ import annotations

from typing import Any


def expand(
    handler: dict[str, Any], dump_relations: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    wanted = list(handler.get("covered_source_row_sha256") or [])
    index = {
        str(row.get("source_row_sha256") or ""): row for row in dump_relations
    }
    missing = [digest for digest in wanted if digest not in index]
    if missing:
        raise ValueError(
            f"{handler.get('handler_id')}: remainder hashes missing from dump: "
            f"{missing[0]}"
        )
    return [index[digest] for digest in wanted]
