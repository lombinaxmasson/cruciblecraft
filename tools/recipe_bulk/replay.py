#!/usr/bin/env python3
"""Test-only smelter/stone semantic replay. Never rewrites production files."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from tools import smelter_stone_common as smelter_stone
from tools.recipe_bulk.emit import emit_family, semantic_replay_key


def replay_smelter_stone() -> dict[str, Any]:
    lock = smelter_stone.load_json(smelter_stone.PRODUCTION_LOCK)
    source = smelter_stone.load_json(smelter_stone.SOURCE)
    by_template = {
        str(row["template_key"]): row for row in source.get("relations") or []
    }
    mismatches: list[str] = []
    compared = 0
    lock_rows = {
        str(row["template_key"]): row
        for row in (lock.get("production") or {}).get("families") or []
    }
    for path in smelter_stone.generated_family_files():
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
            target_map=smelter_stone.TARGET_MAP,
            publication_group=smelter_stone.STONE_GROUP,
            source_revision=smelter_stone.SOURCE_REVISION,
        )
        if semantic_replay_key(replayed) != semantic_replay_key(produced):
            mismatches.append(template_key)
        compared += 1
    return {
        "compared": compared,
        "expected": smelter_stone.PRODUCTION_FAMILY_COUNT,
        "mismatches": mismatches,
        "ok": not mismatches and compared == smelter_stone.PRODUCTION_FAMILY_COUNT,
        "status": "SEMANTIC_REPLAY_SMELTER_STONE",
    }
