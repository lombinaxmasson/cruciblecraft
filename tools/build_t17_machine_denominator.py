#!/usr/bin/env python3
"""Build the fixed-source T17 HU/EU machine denominator."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t17_machine_denominator_policy.json"
OUTPUT = TOOLS / "t17_machine_denominator.json"
MACHINE_DENOMINATOR = TOOLS / "t13_denominators/machine_kinds.json"
ENERGY_DENOMINATOR = TOOLS / "t13_denominators/energy_identities.json"
T16_POLICY = TOOLS / "t16_machine_denominator_policy.json"
MACHINE_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
)
RECIPE_MAPS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModRecipeMaps.java"
)
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
DISPOSITIONS = {
    "IMPLEMENTED_T17A",
    "PREIMPLEMENTED_REFERENCE",
    "MAPPED_DEFERRED",
    "T13_ONLY_DEFERRED",
    "CROSS_OWNER_DEFERRED",
}
OWNED_DISPOSITIONS = DISPOSITIONS - {"CROSS_OWNER_DEFERRED"}
LIVE = {"IMPLEMENTED_T17A", "PREIMPLEMENTED_REFERENCE"}


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
        or policy.get("status") != "T17_MACHINE_DENOMINATOR_POLICY"
        or policy.get("source_revision") != SOURCE_REVISION
        or policy.get("owner") != "T17"
        or policy.get("accepted_energy_identities")
        != {"EU": "ELECTRIC", "HU": "HEAT"}
    ):
        raise ValueError("T17 machine denominator policy header drifted")
    kinds = policy.get("kinds")
    if not isinstance(kinds, dict) or len(kinds) != 28:
        raise ValueError("T17 policy must classify exactly 28 machine kinds")
    if set(policy.get("dispositions") or []) != DISPOSITIONS:
        raise ValueError("T17 machine disposition vocabulary drifted")
    for recipe_map, row in kinds.items():
        if row.get("accepted_energy") not in {"HU", "EU"}:
            raise ValueError(f"{recipe_map}: invalid T17 energy")
        if row.get("disposition") not in OWNED_DISPOSITIONS:
            raise ValueError(f"{recipe_map}: invalid disposition")
        require_explanation(row, recipe_map)
        require_explanation(
            row.get("deferred_tiers") or {},
            f"{recipe_map} deferred tiers",
        )
        mapped = row.get("local_map") is not None
        if mapped != (
            row["disposition"] != "T13_ONLY_DEFERRED"
        ):
            raise ValueError(
                f"{recipe_map}: mapped/T13-only disposition is inconsistent"
            )
        if row["disposition"] in LIVE:
            if not str(row.get("catalog_kind") or "").strip():
                raise ValueError(
                    f"{recipe_map}: live kind lacks catalog identity"
                )
        elif "catalog_kind" in row:
            raise ValueError(
                f"{recipe_map}: deferred kind claims live catalog identity"
            )

    cross_owner = policy.get("cross_owner_dispositions")
    if not isinstance(cross_owner, dict) or set(cross_owner) != {
        "RM.Compressor"
    }:
        raise ValueError(
            "T17 must carry exactly the T16-owned Compressor disposition"
        )
    compressor = cross_owner["RM.Compressor"]
    require_explanation(compressor, "RM.Compressor cross-owner disposition")
    if (
        compressor.get("owner") != "T16"
        or compressor.get("accepted_energy") != "KU"
        or compressor.get("local_map") != "cruciblecraft:compressor"
        or compressor.get("current_local_energy_type") != "ELECTRIC"
        or compressor.get("source_local_energy_type") != "KINETIC_PUSH"
        or compressor.get("disposition") != "CROSS_OWNER_DEFERRED"
    ):
        raise ValueError("RM.Compressor cross-owner disposition drifted")
    t16_policy = load(T16_POLICY)
    t16_compressor = (t16_policy.get("kinds") or {}).get("RM.Compressor") or {}
    if (
        t16_policy.get("owner") != "T16"
        or t16_compressor.get("accepted_energy") != "KU"
        or t16_compressor.get("local_map") != "cruciblecraft:compressor"
        or t16_compressor.get("disposition") != "MAPPED_DEFERRED"
    ):
        raise ValueError("RM.Compressor no longer matches its T16 owner ledger")

    mappings = policy.get("heat_material_mappings")
    expected = {"ANY.Steel", "Invar", "Ti"}
    if not isinstance(mappings, dict) or set(mappings) != expected:
        raise ValueError("T17 Heat_T source material mappings drifted")
    for source_material, mapping in mappings.items():
        local_id = str(mapping.get("local_material") or "")
        material_path = ROOT / str(mapping.get("registration_path") or "")
        if (
            not local_id.startswith("cruciblecraft:")
            or mapping.get("registered") is not True
            or mapping.get("acquisition_blocker") is not None
            or not material_path.is_file()
            or load(material_path).get("id") != local_id.split(":", 1)[1]
        ):
            raise ValueError(
                f"{source_material}: invalid registered source mapping"
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


def t17_source_rows(
    machine_document: dict[str, Any],
    policy: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    if machine_document.get("source", {}).get("revision") != SOURCE_REVISION:
        raise ValueError("T13 machine denominator revision drifted")
    rows = {
        row["recipe_map"]: row
        for row in machine_document.get("canonical_kinds", [])
        if row.get("owner") == "T17"
        and row.get("accepted_energy") in {"HU", "EU"}
    }
    if len(rows) != 28:
        raise ValueError(
            f"T13 exposes {len(rows)} T17 HU/EU kinds, expected 28"
        )
    if set(rows) != set(policy["kinds"]):
        raise ValueError(
            "T17 policy and T13 machine denominator are not bidirectional: "
            f"missing={sorted(set(rows) - set(policy['kinds']))} "
            f"extra={sorted(set(policy['kinds']) - set(rows))}"
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
        if row.get("owner") != "T17":
            continue
        matching = set(row.get("aliases") or []) & {"HU", "EU"}
        if len(matching) != 1:
            raise ValueError(
                "T17 energy row must identify exactly one HU/EU alias"
            )
        result[matching.pop()] = row
    if set(result) != set(policy["accepted_energy_identities"]):
        raise ValueError(
            "T13 energy denominator does not bidirectionally cover HU/EU"
        )
    expected_topologies = {
        "EU": "EU_CABLE_NETWORK",
        "HU": "HU_ADJACENT_HEAT",
    }
    for identity, row in result.items():
        if (
            row.get("classification") != "in_scope"
            or row.get("local_energy_type")
            != policy["accepted_energy_identities"][identity]
            or row.get("topology") != expected_topologies[identity]
        ):
            raise ValueError(f"{identity}: T13 energy mapping drifted")
    return result


def validate_local_mappings(
    policy: dict[str, Any], registered: set[str]
) -> None:
    for recipe_map, row in policy["kinds"].items():
        local_map = row.get("local_map")
        if local_map is not None and local_map not in registered:
            raise ValueError(
                f"{recipe_map}: local map {local_map} is not registered"
            )


def int_expr(value: Any, owner: str) -> int:
    expression = str(value).replace(" ", "")
    while (
        expression.startswith("(")
        and expression.endswith(")")
        and expression.count("(") == expression.count(")")
    ):
        expression = expression[1:-1]
    match = re.fullmatch(r"\(?(\d+)\)?([*/])(\d+)", expression)
    if match:
        left, operator, right = match.groups()
        return (
            int(left) // int(right)
            if operator == "/"
            else int(left) * int(right)
        )
    try:
        return int(expression)
    except ValueError as error:
        raise ValueError(
            f"{owner}: expected integer source expression, got {value!r}"
        ) from error


def tier_array_values(
    machine_document: dict[str, Any], name: str
) -> list[str]:
    row = (machine_document.get("tier_arrays") or {}).get(name) or {}
    identity = row.get("source_identity") or {}
    values = row.get("values")
    if (
        identity.get("source_revision") != SOURCE_REVISION
        or identity.get("source_symbol_or_extraction_key")
        != f"MT.DATA.{name}"
        or not isinstance(values, list)
    ):
        raise ValueError(f"T13 {name} material array drifted")
    return values


def source_variants(
    kind: dict[str, Any],
    machine_document: dict[str, Any],
    policy: dict[str, Any],
) -> list[dict[str, Any]]:
    variants = kind.get("variants")
    if not isinstance(variants, list) or not variants:
        raise ValueError(f"{kind.get('recipe_map')}: source variants missing")
    result: list[dict[str, Any]] = []
    seen_tiers: set[int] = set()
    for variant in variants:
        owner = (
            f"{kind['recipe_map']} source id "
            f"{variant.get('source_id_expression')}"
        )
        nominal = int_expr(variant["input_window"]["nominal"], owner)
        minimum = int_expr(variant["input_window"]["minimum"], owner)
        maximum = int_expr(variant["input_window"]["maximum"], owner)
        if minimum != nominal // 2 or maximum != nominal * 2:
            raise ValueError(f"{owner}: source input window drifted")
        tier = (
            int_expr(variant["tier_expression"], owner)
            if variant.get("tier_expression") is not None
            else None
        )
        tier_array = variant.get("tier_array")
        source_material = variant["material_expression"]
        if tier_array is not None:
            if tier is None or tier in seen_tiers:
                raise ValueError(f"{owner}: invalid or duplicate source tier")
            seen_tiers.add(tier)
            values = tier_array_values(machine_document, tier_array)
            if tier < 0 or tier >= len(values):
                raise ValueError(f"{owner}: tier material is out of range")
            source_material = values[tier]
        material_mapping = None
        if tier_array == "Heat_T" and tier in {1, 2, 3}:
            mapping = policy["heat_material_mappings"].get(source_material)
            if mapping is None:
                raise ValueError(
                    f"{owner}: Heat_T source material lacks explicit mapping"
                )
            material_mapping = {
                "source_material": source_material,
                "local_material": mapping["local_material"],
                "registered": mapping["registered"],
                "acquisition_blocker": mapping["acquisition_blocker"],
            }
        result.append({
            "source_id": int_expr(variant["source_id_expression"], owner),
            "source_line": variant["source_line"],
            "source_tier": tier,
            "tier_array": tier_array,
            "material_expression": variant["material_expression"],
            "source_material": source_material,
            "material_mapping": material_mapping,
            "input_minimum": minimum,
            "input_nominal": nominal,
            "input_maximum": maximum,
            "parallel": int_expr(variant["parallel"], owner),
            "efficiency": int_expr(variant["efficiency"], owner),
            "overclock": variant["overclock_policy"],
            "parallel_duration": variant["policy_flags"][
                "NBT_PARALLEL_DURATION"
            ],
        })
    return sorted(
        result,
        key=lambda row: (
            row["source_tier"] is None,
            row["source_tier"] or 0,
            row["source_id"],
        ),
    )


def catalog_rows(document: dict[str, Any]) -> dict[str, dict[str, Any]]:
    if (
        document.get("schemaVersion") != 2
        or document.get("source", {}).get("revision") != SOURCE_REVISION
    ):
        raise ValueError("machine tier catalog header drifted")
    variants = document.get("variants")
    if not isinstance(variants, list) or len(variants) != 33:
        raise ValueError("T17a machine tier catalog must contain 33 variants")
    result = {row["id"]: row for row in variants}
    if len(result) != len(variants):
        raise ValueError("machine tier catalog variant ids are duplicated")
    return result


def validate_catalog_kind(
    selection: dict[str, Any],
    source: list[dict[str, Any]],
    catalog: dict[str, dict[str, Any]],
) -> list[str]:
    kind_id = selection["catalog_kind"]
    actual = sorted(
        (row for row in catalog.values() if row["kind"] == kind_id),
        key=lambda row: row["sourceTier"],
    )
    if len(actual) != 3:
        raise ValueError(f"{kind_id}: catalog must expose source tiers 1-3")
    energy = selection["accepted_energy"]
    for expected_source, row in zip(source[:3], actual):
        tier_band_prefix = "heat" if energy == "HU" else "electric"
        expected = {
            "tierBand": (
                "cruciblecraft:"
                f"{tier_band_prefix}_tier_"
                f"{expected_source['source_tier']}"
            ),
            "energy": "HEAT" if energy == "HU" else "ELECTRIC",
            "sourceId": expected_source["source_id"],
            "sourceTier": expected_source["source_tier"],
            "overclock": expected_source["overclock"],
            "parallelDuration": expected_source["parallel_duration"],
            "inputMinimum": expected_source["input_minimum"],
            "inputNominal": expected_source["input_nominal"],
            "inputMaximum": expected_source["input_maximum"],
            "energyCapacity": expected_source["input_maximum"],
            "parallel": expected_source["parallel"],
            "efficiency": expected_source["efficiency"],
        }
        mapping = expected_source["material_mapping"]
        if mapping is not None:
            expected.update({
                "material": mapping["local_material"],
                "sourceMaterial": mapping["source_material"],
                "materialRegistered": mapping["registered"],
            })
            if row.get("acquisitionBlocker") is not None:
                raise ValueError(
                    f"{row['id']}: registered material claims blocker"
                )
        drift = {
            field: (row.get(field), value)
            for field, value in expected.items()
            if row.get(field) != value
        }
        if drift:
            raise ValueError(f"{row['id']}: catalog/source drift {drift}")
    return [row["id"] for row in actual]


def status_for_variant(
    disposition: str, variant: dict[str, Any]
) -> str:
    tier = variant["source_tier"]
    if disposition == "IMPLEMENTED_T17A" and tier in {1, 2, 3}:
        return "IMPLEMENTED"
    if disposition == "PREIMPLEMENTED_REFERENCE" and tier in {1, 2, 3}:
        return "REFERENCE_IMPLEMENTED"
    if variant["tier_array"] == "Heat_T" and tier == 4:
        return "DEFERRED_HEAT_TIER4"
    if variant["tier_array"] == "Electric_T" and tier in {4, 5}:
        return f"DEFERRED_ELECTRIC_TIER{tier}"
    return "DEFERRED"


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    validate_policy(policy)
    machine_document = load(MACHINE_DENOMINATOR)
    energy_document = load(ENERGY_DENOMINATOR)
    machine_rows = t17_source_rows(machine_document, policy)
    energies = energy_rows(energy_document, policy)
    validate_local_mappings(policy, local_recipe_maps())
    catalog = catalog_rows(load(MACHINE_TIERS))

    rows: list[dict[str, Any]] = []
    deferred_source_tiers: list[dict[str, Any]] = []
    catalog_variant_ids: list[str] = []
    for recipe_map in sorted(machine_rows):
        source_kind = machine_rows[recipe_map]
        selection = policy["kinds"][recipe_map]
        if source_kind["accepted_energy"] != selection["accepted_energy"]:
            raise ValueError(f"{recipe_map}: policy energy differs from T13")
        variants = source_variants(source_kind, machine_document, policy)
        if selection["disposition"] in LIVE:
            catalog_variant_ids.extend(
                validate_catalog_kind(selection, variants, catalog)
            )
        projected_variants = []
        for variant in variants:
            status = status_for_variant(
                selection["disposition"], variant
            )
            projected = {**variant, "status": status}
            if status.startswith("DEFERRED_") and (
                (variant["tier_array"] == "Heat_T"
                 and variant["source_tier"] == 4)
                or (variant["tier_array"] == "Electric_T"
                    and variant["source_tier"] in {4, 5})
            ):
                projected.update(selection["deferred_tiers"])
                deferred_source_tiers.append({
                    "recipe_map": recipe_map,
                    "accepted_energy": source_kind["accepted_energy"],
                    **projected,
                })
            projected_variants.append(projected)
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
            "reason": selection["reason"],
            "replacement_condition": selection["replacement_condition"],
            "recheck_point": selection["recheck_point"],
            "source_identity": source_kind["source_identity"],
            "source_variants": projected_variants,
        })

    cross_owner_rows = [
        {
            "recipe_map": recipe_map,
            **row,
        }
        for recipe_map, row in sorted(
            policy["cross_owner_dispositions"].items()
        )
    ]

    dispositions = Counter(row["disposition"] for row in rows)
    selected = dispositions["IMPLEMENTED_T17A"]
    reference = dispositions["PREIMPLEMENTED_REFERENCE"]
    deferred = (
        dispositions["MAPPED_DEFERRED"]
        + dispositions["T13_ONLY_DEFERRED"]
    )
    if (selected, reference, deferred) != (3, 1, 24):
        raise ValueError(
            "T17 selected/reference/deferred denominator drifted"
        )
    energy_counts = Counter(row["accepted_energy"] for row in rows)
    if energy_counts != {"EU": 16, "HU": 12}:
        raise ValueError(
            "T13 T17 owner energy counts drifted from 16 EU + 12 HU"
        )
    heat_tier4 = [
        row for row in deferred_source_tiers
        if row["status"] == "DEFERRED_HEAT_TIER4"
    ]
    electric_tier4_5 = [
        row for row in deferred_source_tiers
        if row["status"].startswith("DEFERRED_ELECTRIC_")
    ]
    if len(heat_tier4) != 10 or len(electric_tier4_5) != 32:
        raise ValueError(
            "T17 deferred Heat_T/Electric_T source tier counts drifted"
        )
    expected_live_kinds = {
        row["catalog_kind"]
        for row in policy["kinds"].values()
        if row.get("catalog_kind")
    }
    actual_live_kinds = {
        row["kind"]
        for row in catalog.values()
        if row["energy"] in {"HEAT", "ELECTRIC"}
    }
    if actual_live_kinds != expected_live_kinds:
        raise ValueError(
            "live HU/EU catalog kinds differ from T17 policy"
        )

    extruder = next(
        row for row in rows if row["recipe_map"] == "RM.Extruder"
    )
    extruder_policy = policy["kinds"]["RM.Extruder"]
    if (
        extruder["accepted_energy"] != "HU"
        or extruder["disposition"] != "MAPPED_DEFERRED"
        or extruder_policy.get("current_local_energy_type") != "KINETIC"
        or extruder_policy.get("source_local_energy_type") != "HEAT"
    ):
        raise ValueError("RM.Extruder T17 disposition drifted")
    disposition_rows = [
        {
            "recipe_map": "RM.Extruder",
            "owner": "T17",
            "accepted_energy": extruder["accepted_energy"],
            "current_local_energy_type": extruder_policy[
                "current_local_energy_type"
            ],
            "source_local_energy_type": extruder_policy[
                "source_local_energy_type"
            ],
            "disposition": extruder["disposition"],
            "reason": extruder["reason"],
            "replacement_condition": extruder["replacement_condition"],
            "recheck_point": extruder["recheck_point"],
        },
        cross_owner_rows[0],
    ]

    material_paths = {
        mapping["registration_path"]
        for mapping in policy["heat_material_mappings"].values()
    }
    return {
        "schema_version": 1,
        "status": "T17_MACHINE_DENOMINATOR_READY",
        "source_revision": SOURCE_REVISION,
        "counts": {
            "t17_owner_hu_eu_kinds": len(rows),
            "energy_kinds": dict(sorted(energy_counts.items())),
            "classified": len(rows),
            "unclassified": 0,
            "energy_identities": len(energies),
            "selected_kinds": selected,
            "preimplemented_reference_kinds": reference,
            "deferred_kinds": deferred,
            "cross_owner_deferred_kinds": len(cross_owner_rows),
            "catalog_variants_selected_and_reference": len(
                catalog_variant_ids
            ),
            "catalog_variants_total": len(catalog),
            "heat_tier4_deferred": len(heat_tier4),
            "electric_tier4_5_deferred": len(electric_tier4_5),
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
        "heat_material_mappings": policy["heat_material_mappings"],
        "rows": rows,
        "cross_owner_dispositions": cross_owner_rows,
        "energy_disposition_audit": {
            "status": "AUDITED",
            "criterion": (
                "INCORRECT_ENERGY_IDENTITIES_HAVE_EXPLICIT_DISPOSITIONS"
            ),
            "implemented": 0,
            "deferred": len(disposition_rows),
            "rows": disposition_rows,
        },
        "deferred_source_tiers": deferred_source_tiers,
        "currentness": {
            "owned_inputs": {
                relative(BUILDER): sha256(BUILDER),
                relative(POLICY): sha256(POLICY),
            },
            "t13_denominators": {
                relative(MACHINE_DENOMINATOR): sha256(MACHINE_DENOMINATOR),
                relative(ENERGY_DENOMINATOR): sha256(ENERGY_DENOMINATOR),
            },
            "cross_owner_ledger": {
                relative(T16_POLICY): sha256(T16_POLICY),
            },
            "runtime_projection": {
                relative(MACHINE_TIERS): sha256(MACHINE_TIERS),
                relative(RECIPE_MAPS): sha256(RECIPE_MAPS),
            },
            "registered_heat_materials": {
                path: sha256(ROOT / path) for path in sorted(material_paths)
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
    parser.add_argument("--check", action="store_true")
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
        print(f"T17 machine denominator failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "kinds": document["counts"]["t17_owner_hu_eu_kinds"],
        "energy_kinds": document["counts"]["energy_kinds"],
        "unclassified": document["counts"]["unclassified"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
