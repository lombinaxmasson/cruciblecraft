#!/usr/bin/env python3
"""Test-only T43 semantic replay. Never rewrites T43 production files."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import t43_common as t43
from tools.recipe_bulk.emit import emit_family, semantic_replay_key


def replay_t43() -> dict[str, Any]:
    lock = t43.load_json(t43.PRODUCTION_LOCK)
    source = t43.load_json(t43.SOURCE)
    by_template = {
        str(row["template_key"]): row for row in source.get("relations") or []
    }
    mismatches: list[str] = []
    compared = 0
    lock_rows = {
        str(row["template_key"]): row
        for row in (lock.get("production") or {}).get("families") or []
    }
    for path in t43.generated_family_files():
        produced = json.loads(path.read_text(encoding="utf-8"))
        template_key = str(produced.get("family_id") or "")
        relation = by_template.get(template_key)
        lock_row = lock_rows.get(template_key)
        if relation is None or lock_row is None:
            mismatches.append(f"missing lock/source for {template_key}")
            continue
        replayed = emit_family(
            relation,
            lock_row,
            target_map=t43.TARGET_MAP,
            publication_group=t43.STONE_GROUP,
            source_revision=t43.SOURCE_REVISION,
        )
        if semantic_replay_key(replayed) != semantic_replay_key(produced):
            mismatches.append(template_key)
        compared += 1
    return {
        "compared": compared,
        "expected": t43.PRODUCTION_FAMILY_COUNT,
        "mismatches": mismatches,
        "ok": not mismatches and compared == t43.PRODUCTION_FAMILY_COUNT,
        "status": "T45_T43_SEMANTIC_REPLAY",
    }
