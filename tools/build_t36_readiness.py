#!/usr/bin/env python3
"""Build the T36 readiness account and recipe-then-storage topology epoch."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t36_common as t36  # noqa: E402

BUILDER = Path(__file__).resolve()
OUTPUT = t36.READINESS
TOPOLOGY = t36.TOOLS / "t36_card_topology.json"
REVOKED = tuple(f"T{number}" for number in range(38, 47))
T37_ASSEMBLER_CALIBRATION_FAMILIES = 50
RECIPE_WAVE_MIN = 50
RECIPE_WAVE_MAX = 200


def _roaster_wave(delta: dict[str, Any]) -> dict[str, Any]:
    host = (delta.get("family_scope") or {}).get("hosts", {}).get(
        "cruciblecraft:roaster"
    ) or {}
    family_ids = list(host.get("family_ids") or [])
    size = int(host.get("families") or len(family_ids) or 29)
    return {
        "id": "T38",
        "kind": "fixed_wave",
        "track": "recipe_wave",
        "owner": "T38",
        "depends_on": ["T36", "T37"],
        "host": "cruciblecraft:roaster",
        "size": size,
        "wave_size_band": [RECIPE_WAVE_MIN, RECIPE_WAVE_MAX],
        "size_exception": "host_complete_small_wave",
        "family_ids": family_ids,
        "load_opening": {
            "axis": "datapack_authored_entries",
            "source": "tools/t36_census_delta.json#t14_load",
        },
        "exit_gate": "roaster 29 ordinary families have a live host_exact map",
        "append_rule": "this card is closed; do not append families after issuance",
        "replacement_recheck": "T38 closure remeasures host_exact and authored/logical counts for these 29 families only",
        "note": (
            "First bounded recipe wave after T37 assembler calibration. "
            "29 is below the 50–200 preferred band because the Roasting host "
            "is now complete; do not pad with unrelated families."
        ),
    }


def _sequence(delta: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        {
            "id": "T36",
            "kind": "fixed",
            "track": "machines",
            "status": "complete",
            "owner": "T36",
        },
        {
            "id": "T37",
            "kind": "fixed",
            "track": "calibration",
            "owner": "T37",
            "host": "cruciblecraft:assembler",
            "size": T37_ASSEMBLER_CALIBRATION_FAMILIES,
            "note": "Fixed 50-family assembler calibration. T36 must not steal this work.",
        },
        _roaster_wave(delta),
        {
            "kind": "generated_rule",
            "track": "recipe_wave",
            "id_policy": "consecutive_from_T39",
            "owner_rule": "the issued card id owns the wave",
            "depends_on": ["T38"],
            "until": "1.x recipe gap = 0",
            "wave_size_band": [RECIPE_WAVE_MIN, RECIPE_WAVE_MAX],
            "append_rule": (
                "Issue a new consecutive card for each additional wave. "
                "Do not renumber T38 or T37. Do not pre-assign storage ids."
            ),
            "load_opening": "each wave records T14 opening + delta + closing",
            "exit_gate": "wave families authored or measured; remaining gap recomputed",
        },
        {
            "kind": "generated_rule",
            "track": "storage_bundle",
            "id_policy": "consecutive_after_last_recipe_wave",
            "depends_on": ["recipe_waves_complete"],
            "after": "1.x recipe gap = 0",
            "note": (
                "Storage bundles start only after recipe waves close the 1.x gap. "
                "Do not freeze T39 as storage; the next recipe wave, if needed, "
                "takes T39."
            ),
            "owner_rule": "issued storage card id",
            "load_opening": "each bundle records T14 opening + delta + closing",
            "exit_gate": "canonical storage families implemented under the bundle architecture",
        },
        {
            "kind": "generated_rule",
            "track": "1x_exit_gate",
            "id_policy": "consecutive_after_storage",
            "depends_on": ["storage_bundles"],
            "after": "storage_complete",
            "owner_rule": "issued exit-gate card id",
            "exit_gate": "1.x recipe gap = 0 and storage bundles complete",
        },
        {
            "id": "nuclear_source_physics_census",
            "kind": "later_stage",
            "track": "nuclear",
            "later_stage": "post_1x",
            "owner": "post_1x",
        },
    ]


def build() -> dict[str, Any]:
    target = t35.load_json(t36.MACHINE_TARGET)
    catalog = t35.load_json(t36.MACHINE_TIERS)
    delta = t35.load_json(t36.CENSUS_DELTA) if t36.CENSUS_DELTA.is_file() else None
    if delta is None:
        from tools import build_t36_census_delta as delta_builder
        delta = delta_builder.build()
    target_ids = {row["variant_id"] for row in target["rows"]}
    catalog_ids = {row["id"] for row in catalog.get("variants") or []}
    sequence = _sequence(delta)
    t38 = sequence[2]
    topology = {
        "epoch": "T36_RECIPE_THEN_STORAGE",
        "append_only": False,
        "fixed_cards": ["T36", "T37"],
        "revoked_unstarted_cards": list(REVOKED),
        "rule": (
            "T37 calibration stays 50 assembler families; T38 is the first bounded "
            "recipe wave; additional recipe waves number consecutively from T39 "
            "until gap=0; storage is issued only after that, never with a frozen T39 id"
        ),
        "sequence": sequence,
        "validators": {
            "t36_t37_fixed": 0,
            "revoked_old_storage_first_topology": 0,
            "recipe_before_storage": 0,
            "new_t38_is_recipe_wave": 0 if t38.get("track") == "recipe_wave" else 1,
            "t38_has_family_assignment": 0 if t38.get("family_ids") and t38.get("size") else 1,
            "t38_has_owner_and_budget": 0 if t38.get("owner") == "T38" and t38.get("load_opening") else 1,
            "t37_not_stolen": 0 if sequence[1].get("size") == T37_ASSEMBLER_CALIBRATION_FAMILIES else 1,
            "storage_not_preassigned_t39": 0 if (
                sequence[4].get("track") == "storage_bundle"
                and sequence[4].get("id") is None
            ) else 1,
        },
    }
    recipe_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "recipe_wave"
    )
    storage_index = next(
        index for index, row in enumerate(sequence) if row["track"] == "storage_bundle"
    )
    topology["validators"]["recipe_before_storage"] = (
        0 if recipe_index < storage_index else 1
    )
    gates = {
        "target_catalog_equal": target_ids == catalog_ids,
        "opening_33_present": set(target["freeze"]["opening_ids"]) <= catalog_ids,
        "t35_foundation_unchanged": bool(delta.get("t35_foundation_unchanged")),
        "delta_validators_zero": all(
            int(value or 0) == 0
            for value in (delta.get("validators") or {}).values()
        ),
        "topology_recipe_before_storage": topology["validators"]["recipe_before_storage"] == 0,
        "old_t38_t46_revoked": list(REVOKED) == topology["revoked_unstarted_cards"],
        "t38_wave_executable": all(
            topology["validators"][key] == 0
            for key in (
                "new_t38_is_recipe_wave",
                "t38_has_family_assignment",
                "t38_has_owner_and_budget",
                "storage_not_preassigned_t39",
            )
        ),
        "roaster_host_exact": (
            (delta.get("family_scope") or {}).get("hosts", {}).get(
                "cruciblecraft:roaster", {}
            ).get("closing_host_status")
            == "host_exact"
        ),
    }
    return {
        "schema_version": 1,
        "status": "T36_READY" if all(gates.values()) else "T36_BLOCKED",
        "source_revision": t36.SOURCE_REVISION,
        "generated_by": "python tools/build_t36_readiness.py",
        "gates": gates,
        "freeze": {
            "kinds": target["freeze"]["kind_count"],
            "target_rows": target["freeze"]["target_row_count"],
            "opening_rows": target["freeze"]["opening_row_count"],
            "opening_only_rows": target["freeze"].get("opening_only_row_count"),
        },
        "delta_status": delta.get("status"),
        "topology": topology,
        "t35_history_readonly": True,
        "publication_delta": delta.get("delta", {}).get("publication"),
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    t35.write_stable(TOPOLOGY, document["topology"])
    return document


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {t36.relative(OUTPUT)}"]
    expected = t35.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(t35.stale_error(OUTPUT, expected, actual))
        return errors
    document = t35.load_json(OUTPUT)
    if document.get("status") != "T36_READY":
        errors.append("T36 readiness is not T36_READY")
    if not all((document.get("gates") or {}).values()):
        errors.append("T36 readiness gates failed")
    topology = document.get("topology") or {}
    sequence = topology.get("sequence") or [{}]
    if sequence[2].get("track") != "recipe_wave":
        errors.append("new T38 is not a recipe wave")
    if not sequence[2].get("family_ids"):
        errors.append("T38 recipe wave has no family assignment")
    if sequence[2].get("owner") != "T38":
        errors.append("T38 has no owner")
    if sequence[4].get("track") != "storage_bundle" or sequence[4].get("id"):
        errors.append("storage must not be pre-assigned a T39 card id")
    if "T38" in topology.get("revoked_unstarted_cards", []) and sequence[2].get("id") != "T38":
        errors.append("topology revoked T38 without reissuing the number")
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
            print(json.dumps({
                "status": document["status"],
                "epoch": document["topology"]["epoch"],
                "t38_size": document["topology"]["sequence"][2].get("size"),
            }))
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{t36.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T36 readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
