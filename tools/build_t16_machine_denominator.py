#!/usr/bin/env python3
"""Build the selected/deferred T16 RU/KU machine denominator."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
try:
    from tools import t36_common as t36
except ModuleNotFoundError:
    import t36_common as t36
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t16_machine_denominator_policy.json"
OUTPUT = TOOLS / "t16_machine_denominator.json"
MACHINE_DENOMINATOR = TOOLS / "t13_denominators/machine_kinds.json"
ENERGY_DENOMINATOR = TOOLS / "t13_denominators/energy_identities.json"
MACHINE_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
RECIPE_MAPS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModRecipeMaps.java"
)
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
IMPLEMENTED = {
    "IMPLEMENTED_PRE_T16",
    "IMPLEMENTED_T16A",
    "IMPLEMENTED_T16B",
}
POST_T16_CATALOG_KINDS = {
    "cruciblecraft:distillery",
    "cruciblecraft:drying",
    "cruciblecraft:smelter",
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def require_explanation(row: dict[str, Any], owner: str) -> None:
    for field in ("reason", "replacement_condition", "recheck_point"):
        if not str(row.get(field) or "").strip():
            raise ValueError(f"{owner}: missing {field}")


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T16_MACHINE_DENOMINATOR_POLICY"
        or policy.get("source_revision") != SOURCE_REVISION
        or policy.get("owner") != "T16"
        or policy.get("accepted_energy_identities")
        != {"KU": "KINETIC_PUSH", "RU": "KINETIC_ROTATION"}
    ):
        raise ValueError("T16 machine denominator policy header drifted")
    kinds = policy.get("kinds")
    if not isinstance(kinds, dict) or len(kinds) != 20:
        raise ValueError("T16 policy must classify exactly 20 machine kinds")
    allowed = set(policy.get("dispositions") or [])
    if allowed != {
        "IMPLEMENTED_PRE_T16",
        "IMPLEMENTED_T16A",
        "IMPLEMENTED_T16B",
        "MAPPED_DEFERRED",
        "T13_ONLY_DEFERRED",
    }:
        raise ValueError("T16 machine disposition vocabulary drifted")
    for recipe_map, row in kinds.items():
        if row.get("accepted_energy") not in {"RU", "KU"}:
            raise ValueError(f"{recipe_map}: invalid T16 energy")
        if row.get("disposition") not in allowed:
            raise ValueError(f"{recipe_map}: invalid disposition")
        require_explanation(row, recipe_map)
        tier4 = row.get("tier4")
        if not isinstance(tier4, dict):
            raise ValueError(f"{recipe_map}: tier 4 policy is missing")
        require_explanation(tier4, f"{recipe_map} tier 4")
        mapped = row.get("local_map") is not None
        if mapped != (
            row["disposition"] != "T13_ONLY_DEFERRED"
        ):
            raise ValueError(
                f"{recipe_map}: mapped/T13-only disposition is inconsistent"
            )
        if row["disposition"] in IMPLEMENTED:
            if not str(row.get("catalog_kind") or "").strip():
                raise ValueError(
                    f"{recipe_map}: implemented kind lacks catalog identity"
                )
        elif "catalog_kind" in row:
            raise ValueError(
                f"{recipe_map}: deferred kind claims live catalog identity"
            )


def local_recipe_maps() -> set[str]:
    source = RECIPE_MAPS.read_text(encoding="utf-8")
    rows = re.findall(
        r"public\s+static\s+final\s+RecipeMap\s+[A-Z0-9_]+"
        r"\s*=\s*create\(\s*\"([a-z0-9_]+)\"\s*\)",
        source,
        flags=re.DOTALL,
    )
    if len(rows) != len(set(rows)):
        raise ValueError("local RecipeMap registrations are duplicated")
    return {f"cruciblecraft:{path}" for path in rows}


def t16_source_rows(
    machine_document: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    if machine_document.get("source", {}).get("revision") != SOURCE_REVISION:
        raise ValueError("T13 machine denominator revision drifted")
    rows = {
        row["recipe_map"]: row
        for row in machine_document.get("canonical_kinds", [])
        if row.get("owner") == "T16"
        and row.get("accepted_energy") in {"RU", "KU"}
    }
    if len(rows) != 20:
        raise ValueError("T13 must expose exactly 20 T16 RU/KU kinds")
    if set(rows) != set(policy["kinds"]):
        raise ValueError(
            "T16 policy and T13 machine denominator are not bidirectional: "
            f"{sorted(set(rows) - set(policy['kinds']))=} "
            f"{sorted(set(policy['kinds']) - set(rows))=}"
        )
    return rows


def energy_rows(
    energy_document: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    if energy_document.get("source", {}).get("revision") != SOURCE_REVISION:
        raise ValueError("T13 energy denominator revision drifted")
    result: dict[str, dict[str, Any]] = {}
    for row in energy_document.get("rows", []):
        if row.get("owner") != "T16":
            continue
        aliases = set(row.get("aliases") or [])
        matching = aliases & {"RU", "KU"}
        if len(matching) != 1:
            raise ValueError("T16 energy row must identify exactly one RU/KU alias")
        identity = matching.pop()
        result[identity] = row
    if set(result) != set(policy["accepted_energy_identities"]):
        raise ValueError("T13 energy denominator does not bidirectionally cover RU/KU")
    for identity, row in result.items():
        expected = policy["accepted_energy_identities"][identity]
        if (
            row.get("classification") != "in_scope"
            or row.get("local_energy_type") != expected
        ):
            raise ValueError(f"{identity}: T13 energy mapping drifted")
    return result


def validate_local_mappings(
    policy: dict[str, Any],
    registered: set[str],
) -> None:
    for recipe_map, row in policy["kinds"].items():
        local_map = row.get("local_map")
        if local_map is None:
            continue
        if local_map not in registered:
            raise ValueError(
                f"{recipe_map}: declared local map {local_map} is not registered"
            )


def source_tiers(row: dict[str, Any]) -> list[dict[str, Any]]:
    variants = row.get("variants")
    if (
        row.get("classification") != "in_scope"
        or row.get("variant_classification") != "tiered"
        or row.get("tier_arrays") != ["Kinetic_T"]
        or not isinstance(variants, list)
        or len(variants) != 4
    ):
        raise ValueError(f"{row.get('recipe_map')}: source tier set drifted")
    by_tier = {int(variant["tier_expression"]): variant for variant in variants}
    if set(by_tier) != {1, 2, 3, 4}:
        raise ValueError(f"{row['recipe_map']}: source tiers must be 1-4")
    return [by_tier[tier] for tier in range(1, 5)]


def int_field(value: Any, owner: str) -> int:
    try:
        return int(str(value))
    except ValueError as error:
        raise ValueError(f"{owner}: expected integer source field") from error


def validate_variant_source(
    kind: dict[str, Any],
    variant: dict[str, Any],
    tier: int,
) -> dict[str, Any]:
    owner = f"{kind['recipe_map']} tier {tier}"
    nominal = int_field(variant["input_window"]["nominal"], owner)
    expected_nominal = 32 * (4 ** (tier - 1))
    if (
        nominal != expected_nominal
        or variant["input_window"]["minimum"] != f"({nominal})/2"
        or variant["input_window"]["maximum"] != f"({nominal})*2"
        or variant.get("tier_array") != "Kinetic_T"
        or variant.get("overclock_policy") != kind["overclock_policy"]
        or variant.get("policy_flags") != kind["policy_flags"]
    ):
        raise ValueError(f"{owner}: numeric or policy source fields drifted")
    return {
        "source_id": int_field(variant["source_id_expression"], owner),
        "source_tier": tier,
        "source_line": variant["source_line"],
        "material_expression": variant["material_expression"],
        "input_minimum": nominal // 2,
        "input_nominal": nominal,
        "input_maximum": nominal * 2,
        "parallel": int_field(variant["parallel"], owner),
        "efficiency": int_field(variant["efficiency"], owner),
        "overclock": variant["overclock_policy"],
        "parallel_duration": variant["policy_flags"][
            "NBT_PARALLEL_DURATION"
        ],
    }


def catalog_rows(document: dict[str, Any]) -> dict[str, dict[str, Any]]:
    if (
        document.get("schemaVersion") not in {2, 3}
        or document.get("source", {}).get("revision") != SOURCE_REVISION
    ):
        raise ValueError("machine tier catalog header drifted")
    variants = t36.opening_variants(document)
    all_ids = [row["id"] for row in variants]
    if len(all_ids) != len(set(all_ids)):
        raise ValueError("machine tier catalog variant ids are duplicated")
    t16_variants = [
        row for row in variants
        if row.get("kind") not in POST_T16_CATALOG_KINDS
    ]
    result = {row["id"]: row for row in t16_variants}
    if len(result) != 24:
        raise ValueError(
            "T16b historical catalog slice must contain 24 variants"
        )
    return result


def validate_catalog_kind(
    policy_row: dict[str, Any],
    source_rows: list[dict[str, Any]],
    catalog: dict[str, dict[str, Any]],
) -> list[str]:
    kind_id = policy_row["catalog_kind"]
    actual = sorted(
        (row for row in catalog.values() if row["kind"] == kind_id),
        key=lambda row: row["sourceTier"],
    )
    if len(actual) != 3:
        raise ValueError(f"{kind_id}: catalog must expose source tiers 1-3")
    energy = policy_row["accepted_energy"]
    local_energy = (
        "KINETIC_ROTATION" if energy == "RU" else "KINETIC_PUSH"
    )
    materials = (
        "cruciblecraft:bronze",
        "cruciblecraft:steel",
        "cruciblecraft:titanium",
    )
    tier_prefix = "ru" if energy == "RU" else "ku"
    for index, (source, row) in enumerate(zip(source_rows[:3], actual), 1):
        expected = {
            "kind": kind_id,
            "tierBand": f"cruciblecraft:{tier_prefix}_tier_{index}",
            "material": materials[index - 1],
            "energy": local_energy,
            "sourceId": source["source_id"],
            "sourceTier": index,
            "overclock": source["overclock"],
            "parallelDuration": source["parallel_duration"],
            "inputMinimum": source["input_minimum"],
            "inputNominal": source["input_nominal"],
            "inputMaximum": source["input_maximum"],
            "energyCapacity": source["input_maximum"],
            "parallel": source["parallel"],
            "efficiency": source["efficiency"],
        }
        drift = {
            field: (row.get(field), value)
            for field, value in expected.items()
            if row.get(field) != value
        }
        if drift:
            raise ValueError(f"{row['id']}: catalog/source drift {drift}")
    return [row["id"] for row in actual]


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    validate_policy(policy)
    machine_document = load(MACHINE_DENOMINATOR)
    energy_document = load(ENERGY_DENOMINATOR)
    machine_rows = t16_source_rows(machine_document, policy)
    energies = energy_rows(energy_document, policy)
    validate_local_mappings(policy, local_recipe_maps())
    catalog = catalog_rows(load(MACHINE_TIERS))

    rows: list[dict[str, Any]] = []
    tier4_rows: list[dict[str, Any]] = []
    catalog_variant_ids: list[str] = []
    for recipe_map in sorted(machine_rows):
        source_kind = machine_rows[recipe_map]
        selection = policy["kinds"][recipe_map]
        if source_kind["accepted_energy"] != selection["accepted_energy"]:
            raise ValueError(f"{recipe_map}: policy energy differs from T13")
        variants = [
            validate_variant_source(source_kind, variant, tier)
            for tier, variant in enumerate(source_tiers(source_kind), 1)
        ]
        if selection["disposition"] in IMPLEMENTED:
            catalog_variant_ids.extend(
                validate_catalog_kind(selection, variants, catalog)
            )
        selected_tiers = []
        for variant in variants[:3]:
            if selection["disposition"] in IMPLEMENTED:
                status = "IMPLEMENTED"
            else:
                status = "DEFERRED"
            selected_tiers.append({**variant, "status": status})
        tier4 = {
            **variants[3],
            "recipe_map": recipe_map,
            "accepted_energy": source_kind["accepted_energy"],
            "status": "DEFERRED_GT6_TIER4",
            **selection["tier4"],
        }
        tier4_rows.append(tier4)
        rows.append({
            "recipe_map": recipe_map,
            "canonical_key": source_kind["canonical_key"],
            "accepted_energy": source_kind["accepted_energy"],
            "local_energy_type": policy[
                "accepted_energy_identities"
            ][source_kind["accepted_energy"]],
            "disposition": selection["disposition"],
            "local_map": selection.get("local_map"),
            "catalog_kind": selection.get("catalog_kind"),
            "planned_catalog_kind": selection.get("planned_catalog_kind"),
            "reason": selection["reason"],
            "replacement_condition": selection["replacement_condition"],
            "recheck_point": selection["recheck_point"],
            "source_identity": source_kind["source_identity"],
            "tiers_1_3": selected_tiers,
            "tier_4": tier4,
        })

    expected_catalog_kinds = {
        row["catalog_kind"]
        for row in policy["kinds"].values()
        if row.get("catalog_kind")
    }
    actual_t16_catalog_kinds = {
        row["kind"]
        for row in catalog.values()
        if row["energy"] in {"KINETIC_ROTATION", "KINETIC_PUSH"}
    }
    if actual_t16_catalog_kinds != expected_catalog_kinds:
        raise ValueError(
            "live RU/KU catalog kinds differ from implemented denominator kinds"
        )
    dispositions = Counter(row["disposition"] for row in rows)
    selected = sum(
        dispositions[name]
        for name in ("IMPLEMENTED_T16A", "IMPLEMENTED_T16B")
    )
    preimplemented = dispositions["IMPLEMENTED_PRE_T16"]
    deferred = (
        dispositions["MAPPED_DEFERRED"]
        + dispositions["T13_ONLY_DEFERRED"]
    )
    if (selected, preimplemented, deferred) != (5, 2, 13):
        raise ValueError(
            "T16 selected/preimplemented/deferred denominator drifted"
        )
    if len(tier4_rows) != 20:
        raise ValueError("every T16 kind must carry one explicit tier-4 row")
    return {
        "schema_version": 1,
        "status": "T16_MACHINE_DENOMINATOR_READY",
        "source_revision": SOURCE_REVISION,
        "counts": {
            "t16_owner_ru_ku_kinds": len(rows),
            "classified": len(rows),
            "unclassified": 0,
            "energy_identities": len(energies),
            "selected_kinds": selected,
            "preimplemented_kinds": preimplemented,
            "deferred_kinds": deferred,
            "catalog_variants_pre_t16_and_selected": len(
                catalog_variant_ids
            ),
            "gt6_tier4_deferred": len(tier4_rows),
            "dispositions": dict(sorted(dispositions.items())),
        },
        "energy_identities": {
            identity: {
                "symbol": row["symbol"],
                "local_energy_type": row["local_energy_type"],
                "topology": row["topology"],
                "source_identity": row["source_identity"],
            }
            for identity, row in sorted(energies.items())
        },
        "rows": rows,
        "gt6_tier4_deferred": tier4_rows,
        "currentness": {
            "owned_inputs": {
                relative(BUILDER): sha256(BUILDER),
                relative(POLICY): sha256(POLICY),
            },
            "t13_denominators": {
                relative(MACHINE_DENOMINATOR): sha256(MACHINE_DENOMINATOR),
                relative(ENERGY_DENOMINATOR): sha256(ENERGY_DENOMINATOR),
            },
            "runtime_projection": {
                relative(MACHINE_TIERS): sha256(MACHINE_TIERS),
                relative(RECIPE_MAPS): sha256(RECIPE_MAPS),
            },
        },
    }


def check() -> list[str]:
    encoded = stable(build())
    if not OUTPUT.is_file():
        return [f"missing generated file: {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [f"stale generated file: {relative(OUTPUT)}"]
    return []


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T16 denominator artifact is stale",
    )
    args = parser.parse_args()
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T16 machine denominator failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "kinds": document["counts"]["t16_owner_ru_ku_kinds"],
        "unclassified": document["counts"]["unclassified"],
        "tier4_deferred": document["counts"]["gt6_tier4_deferred"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
