#!/usr/bin/env python3
"""Freeze T44 storage source from T35 lineage and optional gregtech6_w oracle.

Modes:
  --full-replay           rebuild source, receipt, and review from the oracle tree
  --check                 validate committed source and receipt without the tree
  --check --full-replay   rebuild from the oracle and compare to committed source
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t44_common as common  # noqa: E402

OUTPUT = common.SOURCE
RECEIPT = common.RECEIPT
REVIEW = common.REVIEW


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def build_source(*, oracle_hashes: dict[str, str] | None = None) -> dict[str, Any]:
    sites = common.reclaim_storage_sites()
    variants = common.expand_variants(sites)
    runtime_ids = common.variant_ids(variants)
    digest = common.selection_sha256(runtime_ids)
    if digest != common.EXPECTED_SELECTION_SHA256:
        raise ValueError("T44 selection_sha256 drifted")
    compact_sites = []
    for site in sites:
        compact_sites.append(
            {
                "site_key": site["site_key"],
                "canonical_family": site["canonical_family"],
                "behavior_class": site["behavior_class"],
                "source_id_expression": site["source_id_expression"],
                "multiplicity": site["multiplicity"],
                "source_path": site["source_identity"]["source_path"],
                "source_revision": site["source_identity"]["source_revision"],
                "source_blob": site["source_identity"]["source_blob"],
            }
        )
    return {
        "generated_by": "python tools/build_t44_storage_source.py",
        "normalization_schema_version": common.NORMALIZATION_SCHEMA_VERSION,
        "schema_version": 1,
        "selection_sha256": digest,
        "source_revision": common.SOURCE_REVISION,
        "status": "T44_STORAGE_SOURCE_FROZEN",
        "storage_source_sites": common.STORAGE_SOURCE_SITES,
        "storage_registrations": common.STORAGE_REGISTRATIONS,
        "logistics_source_sites": common.LOGISTICS_SOURCE_SITES,
        "logistics_registrations": common.LOGISTICS_REGISTRATIONS,
        "blockers": [],
        "source_sites": compact_sites,
        "variants": variants,
        "oracle_file_hashes": oracle_hashes or {},
    }


def build_receipt(
    source: dict[str, Any],
    *,
    oracle_revision: str | None,
    oracle_dirty: bool,
    full_replay: bool,
) -> dict[str, Any]:
    compact = t35.stable_json(source)
    return {
        "generated_by": "python tools/build_t44_storage_source.py",
        "schema_version": 1,
        "status": "T44_STORAGE_SOURCE_RECEIPT",
        "denominator_source_revision": common.SOURCE_REVISION,
        "source_oracle_path": common.ORACLE_RELATIVE,
        "source_oracle_revision": oracle_revision,
        "source_oracle_dirty": oracle_dirty,
        "source_file_hashes": source.get("oracle_file_hashes") or {},
        "normalization_schema_version": common.NORMALIZATION_SCHEMA_VERSION,
        "selection_sha256": source["selection_sha256"],
        "compact_sha256": sha256_text(compact),
        "full_replay": full_replay,
        "storage_source_sites": common.STORAGE_SOURCE_SITES,
        "storage_registrations": common.STORAGE_REGISTRATIONS,
        "logistics_source_sites": common.LOGISTICS_SOURCE_SITES,
        "logistics_registrations": common.LOGISTICS_REGISTRATIONS,
    }


def build_review(source: dict[str, Any]) -> dict[str, Any]:
    visible = [
        row["runtime_id"]
        for row in source["variants"]
        if row["visibility"] == "source_visible"
    ]
    hidden = [
        row["runtime_id"]
        for row in source["variants"]
        if row["visibility"] == "source_hidden"
    ]
    return {
        "generated_by": "python tools/build_t44_storage_source.py",
        "schema_version": 1,
        "status": "T44_STORAGE_SOURCE_REVIEW",
        "selection_sha256": source["selection_sha256"],
        "source_visible": len(visible),
        "source_hidden": len(hidden),
        "notes": [
            "+aID metalset sites stay multiplicity 1 with steel as DESIGN_POLICY representative.",
            "Plank loops keep 300+300 identities; only oak index 0 is source_visible.",
            "mass_storage_logistics is a separate 1/1 and does not count toward 624.",
        ],
    }


def require_oracle() -> tuple[str, dict[str, str]]:
    if not common.ORACLE_ROOT.is_dir():
        raise FileNotFoundError(
            f"source oracle missing: {common.ORACLE_RELATIVE}"
        )
    if common.oracle_dirty():
        raise RuntimeError("source oracle is dirty; full replay fail closed")
    revision = common.oracle_revision()
    if not revision:
        raise RuntimeError("could not read source oracle revision")
    return revision, common.oracle_file_hashes()


def write(*, full_replay: bool) -> None:
    oracle_revision = None
    oracle_dirty = False
    hashes: dict[str, str] = {}
    if full_replay:
        oracle_revision, hashes = require_oracle()
        oracle_dirty = False
    source = build_source(oracle_hashes=hashes if full_replay else None)
    if not full_replay and OUTPUT.is_file() and RECEIPT.is_file():
        previous = common.load_json(RECEIPT)
        hashes = dict(previous.get("source_file_hashes") or {})
        source["oracle_file_hashes"] = hashes
        oracle_revision = previous.get("source_oracle_revision")
        oracle_dirty = bool(previous.get("source_oracle_dirty"))
        source = build_source(oracle_hashes=hashes)
    t35.write_stable(OUTPUT, source)
    t35.write_stable(
        RECEIPT,
        build_receipt(
            source,
            oracle_revision=oracle_revision,
            oracle_dirty=oracle_dirty,
            full_replay=full_replay or bool(hashes),
        ),
    )
    t35.write_stable(REVIEW, build_review(source))


def validate_committed(source: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if source.get("status") != "T44_STORAGE_SOURCE_FROZEN":
        errors.append("compact status is not FROZEN")
    if source.get("source_revision") != common.SOURCE_REVISION:
        errors.append("denominator source_revision drifted")
    if source.get("blockers"):
        errors.append("compact source still has blockers")
    variants = list(source.get("variants") or [])
    try:
        common.validate_variants(variants)
    except ValueError as error:
        errors.append(str(error))
    digest = common.selection_sha256(common.variant_ids(variants))
    if digest != source.get("selection_sha256"):
        errors.append("source selection_sha256 does not match variants")
    if digest != common.EXPECTED_SELECTION_SHA256:
        errors.append("source selection_sha256 drifted from expected pin")
    return errors


def reference_only_check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing compact source: {common.relative(OUTPUT)}"]
    if not RECEIPT.is_file():
        return [f"missing receipt: {common.relative(RECEIPT)}"]
    source = common.load_json(OUTPUT)
    receipt = common.load_json(RECEIPT)
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != t35.stable_json(source):
        errors.append(f"{common.relative(OUTPUT)} is not canonical JSON")
    if receipt.get("compact_sha256") != sha256_text(actual):
        errors.append("receipt.compact_sha256 does not match compact source")
    if receipt.get("denominator_source_revision") != common.SOURCE_REVISION:
        errors.append("receipt denominator_source_revision drifted")
    if receipt.get("source_oracle_path") != common.ORACLE_RELATIVE:
        errors.append("receipt source_oracle_path drifted")
    if receipt.get("normalization_schema_version") != common.NORMALIZATION_SCHEMA_VERSION:
        errors.append("receipt normalization_schema_version drifted")
    if receipt.get("selection_sha256") != source.get("selection_sha256"):
        errors.append("receipt selection_sha256 drifted")
    errors.extend(validate_committed(source))
    return errors


def full_replay_check() -> list[str]:
    errors = reference_only_check()
    try:
        revision, hashes = require_oracle()
        expected = build_source(oracle_hashes=hashes)
    except (OSError, RuntimeError) as error:
        return [str(error)]
    if OUTPUT.is_file() and OUTPUT.read_text(encoding="utf-8") != t35.stable_json(expected):
        errors.append(f"{common.relative(OUTPUT)} is stale under full replay")
    receipt = common.load_json(RECEIPT) if RECEIPT.is_file() else {}
    if receipt.get("source_oracle_revision") != revision:
        errors.append("receipt source_oracle_revision is stale")
    if receipt.get("source_oracle_dirty") is not False:
        errors.append("receipt source_oracle_dirty must be false")
    if (receipt.get("source_file_hashes") or {}) != hashes:
        errors.append("receipt source_file_hashes are stale")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Freeze T44 storage source")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--check", action="store_true")
    group.add_argument("--write", action="store_true")
    group.add_argument("--rebind-currentness-only", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    if args.rebind_currentness_only:
        from tools import currentness

        currentness.rebind_sidecar(OUTPUT)
        print(f"rebound currentness sidecar for {common.relative(OUTPUT)}")
        return 0
    if args.write:
        write(full_replay=args.full_replay)
        print(f"Wrote {common.relative(OUTPUT)}")
        return 0
    errors = full_replay_check() if args.full_replay else reference_only_check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"{common.relative(OUTPUT)} is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
