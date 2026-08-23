#!/usr/bin/env python3
"""Project frozen T36 target rows into the canonical machine_tiers catalog."""
from __future__ import annotations

import argparse
import copy
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t36_common as t36  # noqa: E402

OPENING_ONLY_SOURCE_IDS = {
    "cruciblecraft:assembler": 91001,
    "cruciblecraft:bender": 91002,
    "cruciblecraft:mortar": 91003,
    "cruciblecraft:welder": 91004,
}
KIND_DISPLAY = {
    "drying": "dryer",
    "bronze_crusher": "bronze_crusher",
    "roaster": "smelter",
    "coagulator": "autoclave",
}


def _texture_profile(kind: str) -> str:
    return KIND_DISPLAY.get(kind, kind.split(":")[-1] if ":" in kind else kind)


def _opening_only_energy(variant_id: str) -> str:
    return "KINETIC"


def _catalog_semantics(target: dict[str, Any]) -> str:
    semantics = str(target.get("variant_semantics") or "material")
    if semantics == "opening_only":
        # Java catalog only carries the KINETIC adapter; LU stays on the target row.
        return "material"
    return semantics


def catalog_row(target: dict[str, Any], opening: dict[str, Any] | None) -> dict[str, Any]:
    kind = str(target["canonical_kind"])
    kind_path = kind.split(":", 1)[-1]
    profile = {
        "sharedModel": "processing_machine",
        "textureProfile": _texture_profile(kind_path),
        "skipGenericRegistration": target.get("registry_adapter") == "crusher_block"
        and target["variant_id"] == "cruciblecraft:bronze_crusher",
    }
    if opening is not None:
        row = copy.deepcopy(opening)
        row["variantSemantics"] = _catalog_semantics(target)
        row["resourceProfile"] = profile
        if target.get("opening_only_reason"):
            row["energy"] = _opening_only_energy(str(target["variant_id"]))
        return row
    energy = str(target.get("energy_java") or "KINETIC_ROTATION")
    if target.get("opening_only_reason"):
        energy = _opening_only_energy(str(target["variant_id"]))
    material = target.get("material") or "cruciblecraft:steel"
    source_id = target.get("source_id")
    if source_id is None:
        source_id = OPENING_ONLY_SOURCE_IDS[str(target["variant_id"])]
    nominal = int(target.get("input_nominal") or 32)
    minimum = int(target.get("input_minimum") or max(1, nominal // 2))
    maximum = int(target.get("input_maximum") or nominal * 2)
    row: dict[str, Any] = {
        "id": target["variant_id"],
        "kind": kind,
        "tierBand": target.get("tier_band") or f"cruciblecraft:opening_{kind_path}",
        "material": material,
        "energy": energy,
        "sourceId": int(source_id),
        "sourceTier": int(target.get("source_tier") or 1),
        "overclock": target.get("overclock") or "STANDARD",
        "parallelDuration": bool(target.get("parallel_duration")),
        "inputMinimum": minimum,
        "inputNominal": nominal,
        "inputMaximum": maximum,
        "energyCapacity": int(target.get("energy_capacity") or maximum),
        "parallel": int(target.get("parallel") or 1),
        "efficiency": int(target.get("efficiency") or 10000),
        "variantSemantics": _catalog_semantics(target),
        "resourceProfile": profile,
    }
    if energy == "HEAT":
        row["sourceMaterial"] = str(target.get("source_material") or material)
        row["materialRegistered"] = True
    return row


def build() -> dict[str, Any]:
    catalog = t35.load_json(t36.MACHINE_TIERS)
    target = t35.load_json(t36.MACHINE_TARGET)
    opening = {
        str(row["id"]): row
        for row in catalog.get("variants") or []
        if isinstance(row, dict)
    }
    opening_ids = set(catalog.get("source", {}).get("variant_rows", {}).keys())
    if len(opening_ids) < 33 and len(opening) >= 33:
        opening_ids = {row["id"] for row in catalog["variants"][:33]}
    # After the first write, opening 33 are the rows still tagged opening in target.
    opening_ids = set(target["freeze"]["opening_ids"])
    variants = [catalog_row(row, opening.get(row["variant_id"])) for row in target["rows"]]
    variant_rows = dict(catalog.get("source", {}).get("variant_rows") or {})
    for row in target["rows"]:
        variant_rows[row["variant_id"]] = row.get("source_row") or row["variant_id"]
    document = copy.deepcopy(catalog)
    document["schemaVersion"] = 3
    document["variants"] = variants
    source = document.setdefault("source", {})
    source["variant_rows"] = variant_rows
    document["namingPolicy"] = {
        "tier1BareId": "frozen_legacy_baseline",
        "newSubsystemId": "<material>_<kind>",
        "automaticKindTierCompletion": False,
        "euVoltageId": "casing material prefix; voltage_band is execution identity",
        "tuHostId": "<kind>",
    }
    if len([row for row in variants if row["id"] in opening_ids]) != 33:
        raise ValueError("catalog projection dropped opening 33 ids")
    if len(variants) != int(target["freeze"]["target_row_count"]):
        raise ValueError("catalog projection drifted from frozen target")
    return document


def write() -> dict[str, Any]:
    document = build()
    t36.MACHINE_TIERS.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(document, indent=2, ensure_ascii=False) + "\n"
    tmp = t36.MACHINE_TIERS.with_name(t36.MACHINE_TIERS.name + ".tmp")
    tmp.write_text(payload, encoding="utf-8")
    tmp.replace(t36.MACHINE_TIERS)
    return document


def check() -> list[str]:
    errors: list[str] = []
    expected = json.dumps(build(), indent=2, ensure_ascii=False) + "\n"
    actual = t36.MACHINE_TIERS.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(f"{t36.relative(t36.MACHINE_TIERS)} drifted from T36 target projection")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        if args.write:
            document = write()
            print(json.dumps({"schemaVersion": document["schemaVersion"], "variants": len(document["variants"])}))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t36.relative(t36.MACHINE_TIERS)} matches T36 target")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T36 machine catalog failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
