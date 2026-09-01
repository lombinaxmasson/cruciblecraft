#!/usr/bin/env python3
"""Emit the T49 forward-v2 identity ledger delta. Records may be empty."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import wave_bath_tiny_purified as common

OUTPUT = common.IDENTITY_DELTA
SOURCE_PREFIXES = ("gregtech:", "gregapi:")


def _assert_cc_target(target: str, *, source_key: str) -> str:
    runtime = str(target or "")
    if runtime.startswith(SOURCE_PREFIXES):
        raise ValueError(f"T49 identity {source_key} leaked source target {runtime}")
    if not runtime.startswith("minecraft:") and not runtime.startswith("cruciblecraft:"):
        raise ValueError(f"T49 identity {source_key} target is not published: {runtime}")
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
        "authorities": ["T49"],
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


def _prior_index() -> tuple[dict[str, dict[str, Any]], dict[str, dict[str, Any]]]:
    from tools.recipe_bulk import identity_v2

    original = identity_v2.load_delta

    def load_prior(wave_id: str) -> dict[str, Any]:
        if wave_id == "T49":
            return {
                "blockers": [],
                "order": 49,
                "records": [],
                "schema_version": 1,
                "status": "T49_IDENTITY_LEDGER_DELTA",
                "wave_id": "T49",
            }
        return original(wave_id)

    identity_v2.load_delta = load_prior
    try:
        composed = identity_v2.compose()
    finally:
        identity_v2.load_delta = original
    records = {str(row["source_key"]): row for row in composed.get("records") or []}
    blockers = {str(row["source_key"]): row for row in composed.get("blockers") or []}
    return records, blockers


def _accept_key(
    *,
    key: str,
    target: str,
    seen: dict[str, str],
    prior_records: dict[str, dict[str, Any]],
    prior_blockers: dict[str, dict[str, Any]],
) -> bool:
    if key in seen:
        if seen[key] != target:
            raise ValueError(f"T49 identity key target drifted for {key}")
        return False
    existing = prior_records.get(key)
    if existing is not None:
        if str(existing.get("target_identity") or "") != target:
            raise ValueError(
                f"T49 identity {key} conflicts with proven target "
                f"{existing.get('target_identity')} vs {target}"
            )
        return False
    if key in prior_blockers:
        if key.startswith("T49|"):
            raise ValueError(f"T49 identity {key} overlaps a frozen typed blocker")
        return False
    seen[key] = target
    return True


def build() -> dict[str, Any]:
    catalog = common.load_json(common.IDENTITY_CATALOG)
    fluids = common.load_json(common.FLUID_MAPPING)
    operand_map = (
        common.load_json(common.OPERAND_RUNTIME_MAP)
        if common.OPERAND_RUNTIME_MAP.is_file()
        else {"operands": []}
    )
    prior_records, prior_blockers = _prior_index()
    records: list[dict[str, Any]] = []
    seen: dict[str, str] = {}
    identities = list(catalog.get("identities") or [])
    expected = int(catalog.get("identity_count") or 0)
    if len(identities) != expected:
        raise ValueError("T49 identity catalog count drifted")

    def add(
        *,
        key: str,
        target: str,
        target_kind: str,
        mapping_class: str,
        evidence: str,
        input_path: Path,
    ) -> None:
        if not _accept_key(
            key=key,
            target=target,
            seen=seen,
            prior_records=prior_records,
            prior_blockers=prior_blockers,
        ):
            return
        records.append(
            _record(
                source_key=key,
                target=target,
                target_kind=target_kind,
                mapping_class=mapping_class,
                evidence=evidence,
                input_path=input_path,
            )
        )

    for identity in identities:
        source_item = str(identity["source_item"])
        meta = int(identity["meta"])
        runtime = str(identity["runtime_id"])
        add(
            key=f"T49|item:{source_item}@{meta}",
            target=runtime,
            target_kind="item",
            mapping_class="exact_item",
            evidence="t49_identity_catalog",
            input_path=common.IDENTITY_CATALOG,
        )
        add(
            key=f"item:{source_item}@{meta}",
            target=runtime,
            target_kind="item",
            mapping_class="exact_item",
            evidence="t49_identity_catalog",
            input_path=common.IDENTITY_CATALOG,
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
        add(
            key=f"T49|item:{item}{_meta_suffix(source.get('meta'))}",
            target=target,
            target_kind="item",
            mapping_class="exact_item",
            evidence="t49_operand_runtime_map",
            input_path=common.OPERAND_RUNTIME_MAP,
        )
        add(
            key=f"item:{item}{_meta_suffix(source.get('meta'))}",
            target=target,
            target_kind="item",
            mapping_class="exact_item",
            evidence="t49_operand_runtime_map",
            input_path=common.OPERAND_RUNTIME_MAP,
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
        add(
            key=f"fluid:{fluid_id}",
            target=target,
            target_kind="fluid",
            mapping_class="fluid",
            evidence="t49_operand_runtime_map",
            input_path=common.OPERAND_RUNTIME_MAP,
        )
    records.sort(key=lambda row: str(row["source_key"]))
    return {
        "blockers": [],
        "generated_by": "python tools/build_t49_identity_ledger_delta.py",
        "note": (
            "T49 identity delta. Tiny-washed keys already proven in T48 stay out. "
            "Empty records are valid. Does not rewrite v1 or T46-T48 records."
        ),
        "order": 49,
        "records": records,
        "schema_version": 1,
        "status": "T49_IDENTITY_LEDGER_DELTA",
        "wave_id": "T49",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the T49 identity ledger delta",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
