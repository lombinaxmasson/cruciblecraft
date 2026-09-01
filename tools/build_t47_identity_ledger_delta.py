#!/usr/bin/env python3
"""Emit the T47 forward-v2 identity ledger delta (283 blocks + 3 fluids)."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t47_common as common

OUTPUT = common.IDENTITY_DELTA
EXPECTED_BLOCK_IDENTITIES = 283
EXPECTED_FLUID_OVERLAY = 3
SOURCE_PREFIXES = ("gregtech:", "gregapi:")


def identity_source_keys(source_item: str, meta: int) -> list[str]:
    """Wave-prefixed item key plus the unprefixed item: form (T46 does both)."""
    suffix = f"@{meta}"
    return [
        f"T47|item:{source_item}{suffix}",
        f"item:{source_item}{suffix}",
    ]


def _assert_cc_target(target: str, *, source_key: str) -> str:
    runtime = str(target or "")
    if runtime.startswith(SOURCE_PREFIXES):
        raise ValueError(f"T47 identity {source_key} leaked source target {runtime}")
    if not runtime.startswith("minecraft:") and not runtime.startswith("cruciblecraft:"):
        raise ValueError(f"T47 identity {source_key} target is not published: {runtime}")
    return runtime


def _record(
    *,
    source_key: str,
    target: str,
    target_kind: str,
    mapping_class: str,
    evidence: str,
    input_path: Path,
) -> dict[str, Any]:
    return {
        "authorities": ["T47"],
        "blocker_reason": None,
        "disposition": "proven",
        "evidence": [evidence],
        "input_hashes": {common.relative(input_path): t35.sha256_file(input_path)},
        "mapping_class": mapping_class,
        "source_key": source_key,
        "target_identity": _assert_cc_target(target, source_key=source_key),
        "target_kind": target_kind,
    }


def _meta_suffix(meta: Any) -> str:
    return f"@{meta}" if meta is not None else ""


def build() -> dict[str, Any]:
    catalog = common.load_json(common.IDENTITY_CATALOG)
    fluids = common.load_json(common.FLUID_MAPPING)
    operand_map = (
        common.load_json(common.OPERAND_RUNTIME_MAP)
        if common.OPERAND_RUNTIME_MAP.is_file()
        else {"operands": []}
    )
    records: list[dict[str, Any]] = []
    seen: dict[str, str] = {}
    identities = list(catalog.get("identities") or [])
    if len(identities) != EXPECTED_BLOCK_IDENTITIES:
        raise ValueError(
            f"T47 identity catalog blocks {len(identities)} != {EXPECTED_BLOCK_IDENTITIES}"
        )
    for identity in identities:
        source_item = str(identity["source_item"])
        meta = int(identity["meta"])
        runtime = str(identity["runtime_id"])
        for key in identity_source_keys(source_item, meta):
            if key in seen:
                raise ValueError(f"duplicate T47 identity key {key}")
            seen[key] = runtime
            records.append(
                _record(
                    source_key=key,
                    target=runtime,
                    target_kind="item",
                    mapping_class="exact_item",
                    evidence="t47_identity_catalog",
                    input_path=common.IDENTITY_CATALOG,
                )
            )
    for row in operand_map.get("operands") or []:
        runtime = row.get("runtime") or {}
        source = row.get("source") or {}
        if runtime.get("kind") != "item":
            continue
        item = str(source.get("item") or "")
        target = str(runtime.get("id") or "")
        if not item or not target:
            continue
        key = f"T47|item:{item}{_meta_suffix(source.get('meta'))}"
        if key in seen:
            if seen[key] != target:
                raise ValueError(f"T47 operand map target drifted for {key}")
            continue
        seen[key] = target
        records.append(
            _record(
                source_key=key,
                target=target,
                target_kind="item",
                mapping_class="exact_item",
                evidence="t47_operand_runtime_map",
                input_path=common.OPERAND_RUNTIME_MAP,
            )
        )
    fluid_rows = list(fluids.get("mapping") or [])
    if len(fluid_rows) != EXPECTED_FLUID_OVERLAY:
        raise ValueError(
            f"T47 identity fluid overlay {len(fluid_rows)} != {EXPECTED_FLUID_OVERLAY}"
        )
    overlay_ids = {str(item.get("source_fluid")) for item in fluid_rows}
    for row in operand_map.get("operands") or []:
        runtime = row.get("runtime") or {}
        source = row.get("source") or {}
        if runtime.get("kind") != "fluid":
            continue
        fluid_id = str(source.get("fluid") or "")
        target = str(runtime.get("id") or "")
        if not fluid_id or not target:
            continue
        if any(str(existing.get("source_fluid") or "") == fluid_id for existing in fluid_rows):
            matched = next(
                item for item in fluid_rows if str(item.get("source_fluid") or "") == fluid_id
            )
            if str(matched.get("cc_fluid_id") or "") != target:
                raise ValueError(f"T47 fluid target drifted for {fluid_id}")
            continue
        fluid_rows.append({"cc_fluid_id": target, "source_fluid": fluid_id})
    for row in fluid_rows:
        source = str(row["source_fluid"])
        runtime = str(row["cc_fluid_id"])
        key = f"fluid:{source}"
        if key in seen:
            if seen[key] != runtime:
                raise ValueError(f"T47 fluid key target drifted for {key}")
            continue
        seen[key] = runtime
        overlay = source in overlay_ids
        records.append(
            _record(
                source_key=key,
                target=runtime,
                target_kind="fluid",
                mapping_class="fluid",
                evidence="t47_bath_fluid_mapping" if overlay else "t47_operand_runtime_map",
                input_path=common.FLUID_MAPPING if overlay else common.OPERAND_RUNTIME_MAP,
            )
        )
    item_count = sum(1 for row in records if row["mapping_class"] == "exact_item")
    fluid_count = sum(1 for row in records if row["mapping_class"] == "fluid")
    if item_count < EXPECTED_BLOCK_IDENTITIES * 2:
        raise ValueError(f"T47 identity item keys {item_count} < 566")
    if fluid_count < EXPECTED_FLUID_OVERLAY:
        raise ValueError(f"T47 identity fluid keys {fluid_count} < 3")
    records.sort(key=lambda row: str(row["source_key"]))
    return {
        "blockers": [],
        "generated_by": "python tools/build_t47_identity_ledger_delta.py",
        "note": (
            "T47 identity delta. 283 remainder block identities (wave-prefixed "
            "plus item: keys), lock-set operand map items, overlay fluids, and "
            "already-mapped Bath remainder fluids. Does not rewrite v1 or T46 "
            "records."
        ),
        "order": 47,
        "records": records,
        "schema_version": 1,
        "status": "T47_IDENTITY_LEDGER_DELTA",
        "wave_id": "T47",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the T47 identity ledger delta",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
