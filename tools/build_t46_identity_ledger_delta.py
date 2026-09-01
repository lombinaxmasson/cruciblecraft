#!/usr/bin/env python3
"""Emit the T46 forward-v2 identity ledger delta (118 MTE + 30 fluids)."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t46_common as common
from tools import t46_identities as identities

OUTPUT = common.IDENTITY_DELTA


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
        "authorities": ["T46"],
        "blocker_reason": None,
        "disposition": "proven",
        "evidence": [evidence],
        "input_hashes": {common.relative(input_path): t35.sha256_file(input_path)},
        "mapping_class": mapping_class,
        "source_key": source_key,
        "target_identity": target,
        "target_kind": target_kind,
    }


def build() -> dict[str, Any]:
    catalog = common.load_json(common.MTE_CATALOG)
    fluids = common.load_json(common.FLUID_MAPPING)
    operand_map = (
        common.load_json(common.OPERAND_RUNTIME_MAP)
        if common.OPERAND_RUNTIME_MAP.is_file()
        else {"operands": []}
    )
    records: list[dict[str, Any]] = []
    seen: set[str] = set()
    for identity in catalog.get("identities") or []:
        meta = int(identity["meta"])
        runtime = str(identity["runtime_id"])
        for key in identities.identity_source_keys(meta):
            if key in seen:
                raise ValueError(f"duplicate T46 identity key {key}")
            seen.add(key)
            records.append(
                _record(
                    source_key=key,
                    target=runtime,
                    target_kind="item",
                    mapping_class="exact_item",
                    evidence="t46_bath_mte_identity_catalog",
                    input_path=common.MTE_CATALOG,
                )
            )
    fluid_rows = list(fluids.get("mapping") or [])
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
            continue
        fluid_rows.append(
            {
                "cc_fluid_id": target,
                "source_fluid": fluid_id,
            }
        )
    overlay_ids = {
        str(item.get("source_fluid")) for item in fluids.get("mapping") or []
    }
    for row in fluid_rows:
        source = str(row["source_fluid"])
        runtime = str(row["cc_fluid_id"])
        key = f"fluid:{source}"
        if key in seen:
            continue
        seen.add(key)
        overlay = source in overlay_ids
        records.append(
            _record(
                source_key=key,
                target=runtime,
                target_kind="fluid",
                mapping_class="fluid",
                evidence="t46_bath_fluid_mapping" if overlay else "t46_operand_runtime_map",
                input_path=common.FLUID_MAPPING if overlay else common.OPERAND_RUNTIME_MAP,
            )
        )
    mte_count = sum(1 for row in records if row["mapping_class"] == "exact_item")
    fluid_count = sum(1 for row in records if row["mapping_class"] == "fluid")
    if mte_count != common.EXPECTED_MTE_METAS * 2:
        raise ValueError(f"T46 identity MTE keys {mte_count} != 236")
    if fluid_count < common.EXPECTED_FLUID_OVERLAY:
        raise ValueError(f"T46 identity fluid keys {fluid_count} < 30")
    records.sort(key=lambda row: str(row["source_key"]))
    return {
        "blockers": [],
        "generated_by": "python tools/build_t46_identity_ledger_delta.py",
        "note": (
            "T46 identity delta. 118 exact MTE metas (wave-prefixed plus mte: keys) "
            "plus overlay fluids and already-mapped Bath source fluids."
        ),
        "order": 46,
        "records": records,
        "schema_version": 1,
        "status": "T46_IDENTITY_LEDGER_DELTA",
        "wave_id": "T46",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the T46 identity ledger delta",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
