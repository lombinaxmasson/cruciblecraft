#!/usr/bin/env python3
"""Classify all T20 catalog identities and build independent expected rows."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(ROOT))

from tools import t20_worldgen_rows  # noqa: E402
POLICY = TOOLS / "t20_worldgen_source_policy.json"
SOURCE = TOOLS / "t20_gt6_worldgen_source.json"
CLOSURE = TOOLS / "gt6_ore_chain_closure.json"
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
AUTHORING = (
    ROOT
    / "src/main/resources/data/cruciblecraft/worldgen_catalog/ore_veins.json"
)
OUTPUT = TOOLS / "t20_worldgen_expected.json"
ROLES = ("top", "bottom", "between", "spread")
RANDOM_SMALL_GEM = "PROPERTIES.RANDOM_SMALL_GEM_ORE"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any, *, compact: bool = False) -> str:
    if compact:
        return json.dumps(
            value,
            ensure_ascii=False,
            separators=(",", ":"),
            sort_keys=True,
        ) + "\n"
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stable_salt(identifier: str) -> int:
    digest = hashlib.sha256(
        f"cruciblecraft:worldgen_catalog:{identifier}".encode("utf-8")
    ).digest()
    return int.from_bytes(digest[:4], byteorder="big", signed=True)


def catalog_materials() -> list[str]:
    closure = load(CLOSURE)
    rows = closure.get("crusher_without_worldgen")
    if not isinstance(rows, list):
        raise ValueError("T2c closure rows are missing")
    materials = sorted({
        row["material"]
        for row in rows
        if isinstance(row, dict) and row.get("classification") == "vein"
    })
    reported = (
        closure.get("counts", {})
        .get("crusher_classifications", {})
        .get("vein")
    )
    if reported != 129 or len(materials) != 129:
        raise ValueError(
            f"T20 requires 129 T2c vein identities, got {reported}/{len(materials)}"
        )
    return materials


def material_documents(materials: list[str]) -> dict[str, dict[str, Any]]:
    documents: dict[str, dict[str, Any]] = {}
    for material in materials:
        path = MATERIAL_ROOT / f"{material}.json"
        if not path.is_file():
            raise ValueError(f"catalog material document is missing: {material}")
        document = load(path)
        if document.get("id") != material:
            raise ValueError(f"material id drifted: {material}")
        documents[material] = document
    return documents


def registered_ore_materials() -> set[str]:
    gate = load(REGISTRATION_GATE).get("materials") or {}
    return {
        material
        for material, forms in gate.items()
        if isinstance(forms, list) and "ore" in forms
    }


def source_indexes(
    source: dict[str, Any],
) -> tuple[
    dict[str, list[dict[str, Any]]],
    dict[str, list[dict[str, Any]]],
]:
    small: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in source["explicit_small_ores"]:
        material = row["material"]["cc_material"]
        if material is not None:
            small[material].append(row)
    large: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in source["large_veins"]:
        for role in ROLES:
            material = row["layers"][role]["cc_material"]
            if material is not None and row not in large[material]:
                large[material].append(row)
    return small, large


def prefer_unique_overworld(
    rows: list[dict[str, Any]], marker: str
) -> dict[str, Any] | None:
    overworld = [
        row for row in rows if marker in row["dimension_lists"]
    ]
    if len(overworld) == 1:
        return overworld[0]
    if not overworld and len(rows) == 1:
        return rows[0]
    return None


def clamp(value: float, minimum: float, maximum: float) -> float:
    return min(maximum, max(minimum, value))


def rounded(value: float) -> float:
    return float(f"{value:.6f}")


def design_geometry(material: str) -> dict[str, Any]:
    digest = hashlib.sha256(
        f"cruciblecraft:t20:{material}".encode("utf-8")
    ).digest()
    return {
        "min_y": -48 + digest[0] % 32,
        "max_y": 32 + digest[1] % 64,
        "horizontal_radius": 4 + digest[2] % 5,
        "vertical_radius": 2 + digest[3] % 3,
        "density": rounded(0.12 + (digest[4] % 16) / 100.0),
        "region_size_chunks": 28 + digest[5] % 5,
        "generation_chance": rounded(
            0.50 + (digest[6] % 25) / 100.0
        ),
    }


def same_material_layers(material: str) -> dict[str, list[dict[str, Any]]]:
    return {
        role: [{"material": material, "weight": 1}]
        for role in ROLES
    }


def small_projection(
    material: str,
    row: dict[str, Any],
    source_kind: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    amount = row["amount"]
    geometry = {
        "min_y": row["min_y"],
        "max_y": row["max_y"],
        "horizontal_radius": 4 + min(amount, 24) // 6,
        "vertical_radius": 2 + min(amount, 16) // 8,
        "density": rounded(
            clamp(0.10 + amount / 256.0, 0.10, 0.35)
        ),
        "region_size_chunks": 32,
        "generation_chance": rounded(
            clamp(0.45 + amount / 128.0, 0.45, 0.95)
        ),
    }
    provenance = {
        "status": "SOURCE_DERIVED",
        "source_kind": source_kind,
        "source_fact_id": row["source_fact_id"],
        "source_line": row["source_line"],
        "source_material_id": row["material"]["source_id"],
        "source_material_name": row["material"]["source_name"],
        "transformations": [
            (
                "random_small_gem_geometry_v1"
                if source_kind == "RANDOM_SMALL_GEM_SOURCE"
                else "small_geometry_v1"
            ),
            "identity_preserving_profile_v2",
            "DESIGN_POLICY_OVERWORLD_ACCESS",
        ],
        "field_status": {
            "material": "SOURCE_BACKED",
            "min_y": "SOURCE_BACKED",
            "max_y": "SOURCE_BACKED",
            "layers": "SOURCE_DERIVED",
            "geometry": "SOURCE_DERIVED",
            "distribution": "DESIGN_POLICY",
            "dimension": "DESIGN_POLICY",
        },
    }
    return {**same_material_layers(material), **geometry}, provenance


def large_projection(
    material: str,
    row: dict[str, Any],
    registered: set[str],
) -> tuple[dict[str, Any], dict[str, Any]]:
    layers: dict[str, list[dict[str, Any]]] = {}
    substitutions: list[dict[str, Any]] = []
    for role in ROLES:
        source_material = row["layers"][role]["cc_material"]
        runtime_material = (
            source_material
            if source_material in registered
            else material
        )
        layers[role] = [{"material": runtime_material, "weight": 1}]
        if runtime_material != source_material:
            substitutions.append({
                "role": role,
                "source_material": source_material,
                "runtime_material": runtime_material,
                "reason": "source role lacks a registered CrucibleCraft ore block",
            })
    geometry = {
        "min_y": row["min_y"],
        "max_y": row["max_y"],
        "horizontal_radius": min(row["size"], 23),
        "vertical_radius": 3,
        "density": rounded(
            clamp(row["density"] / row["size"], 0.01, 1.0)
        ),
        "region_size_chunks": 32,
        "generation_chance": rounded(
            clamp(0.25 + row["weight"] / 240.0, 0.25, 0.95)
        ),
    }
    provenance = {
        "status": "SOURCE_DERIVED",
        "source_kind": "UNIQUE_LARGE_ROLE_SOURCE",
        "source_fact_id": row["source_fact_id"],
        "source_line": row["source_line"],
        "transformations": [
            "large_geometry_v1",
            "identity_preserving_profile_v2",
            "DESIGN_POLICY_OVERWORLD_ACCESS",
        ],
        "field_status": {
            "material_layers": (
                "SOURCE_BACKED" if not substitutions else "SOURCE_DERIVED"
            ),
            "min_y": "SOURCE_BACKED",
            "max_y": "SOURCE_BACKED",
            "geometry": "SOURCE_DERIVED",
            "distribution": "SOURCE_DERIVED",
            "dimension": "DESIGN_POLICY",
        },
        "role_substitutions": substitutions,
    }
    return {**layers, **geometry}, provenance


def design_projection(
    material: str,
    small_candidates: list[dict[str, Any]],
    large_candidates: list[dict[str, Any]],
) -> tuple[dict[str, Any], dict[str, Any]]:
    provenance = {
        "status": "DESIGN_POLICY",
        "source_kind": "DESIGN_POLICY_NO_GT6_WORLDGEN_FACT",
        "source_fact_id": None,
        "transformations": [
            "design_policy_hash_v1",
            "identity_preserving_profile_v2",
            "DESIGN_POLICY_OVERWORLD_ACCESS",
        ],
        "field_status": {
            "material": "SOURCE_BACKED_CC_IDENTITY",
            "layers": "DESIGN_POLICY",
            "geometry": "DESIGN_POLICY",
            "distribution": "DESIGN_POLICY",
            "dimension": "DESIGN_POLICY",
        },
        "candidate_source_facts": sorted({
            row["source_fact_id"]
            for row in small_candidates + large_candidates
        }),
        "reason": (
            "GT6 has no unique directly applicable worldgen fact for this "
            "catalog identity; deterministic CC values preserve acquisition "
            "without claiming GT6 parity."
        ),
    }
    return {
        **same_material_layers(material),
        **design_geometry(material),
    }, provenance


def build_expected() -> dict[str, Any]:
    policy = load(POLICY)
    source = load(SOURCE)
    if (
        policy.get("status") != "T20_WORLDGEN_SOURCE_POLICY"
        or source.get("status") != "T20_GT6_WORLDGEN_SOURCE_READY"
        or source["source"]["revision"] != policy["source"]["revision"]
    ):
        raise ValueError("T20 policy/source evidence mismatch")
    materials = catalog_materials()
    documents = material_documents(materials)
    registered = registered_ore_materials()
    small_index, large_index = source_indexes(source)
    rows: list[dict[str, Any]] = []
    for material in materials:
        small_candidates = small_index.get(material, [])
        large_candidates = large_index.get(material, [])
        small = prefer_unique_overworld(
            small_candidates, "GEN_OVERWORLD"
        )
        random_small = RANDOM_SMALL_GEM in (
            documents[material].get("gt6_metadata", {})
            .get("material_tags", [])
        )
        large = prefer_unique_overworld(
            large_candidates, "ORE_OVERWORLD"
        )
        if small is not None:
            values, provenance = small_projection(
                material, small, "EXPLICIT_SMALL_SOURCE"
            )
        elif random_small:
            dynamic = source["semantics"]["dynamic_random_small_gem"]
            synthetic = {
                "source_fact_id": dynamic["source_fact_id"],
                "source_line": dynamic["source_line"],
                "min_y": dynamic["min_y"],
                "max_y": dynamic["max_y"],
                "amount": dynamic["amount"],
                "material": {
                    "source_id": (
                        documents[material]
                        .get("gt6_metadata", {})
                        .get("source_id")
                    ),
                    "source_name": (
                        documents[material]
                        .get("gt6_metadata", {})
                        .get("source_name")
                    ),
                },
            }
            values, provenance = small_projection(
                material, synthetic, "RANDOM_SMALL_GEM_SOURCE"
            )
        elif large is not None:
            values, provenance = large_projection(
                material, large, registered
            )
        else:
            values, provenance = design_projection(
                material, small_candidates, large_candidates
            )
        vein_id = f"large_{material}_vein"
        rows.append({
            "id": vein_id,
            "catalog_material": material,
            "profile_version": 2,
            **values,
            "salt": stable_salt(vein_id),
            "provenance": provenance,
        })

    ids = [row["id"] for row in rows]
    salts = [row["salt"] for row in rows]
    if len(ids) != 129 or len(set(ids)) != 129:
        raise ValueError("T20 expected ids are not a 129-entry set")
    if len(set(salts)) != 129:
        raise ValueError("T20 expected salts are not unique")
    counts = t20_worldgen_rows.row_count_summary(rows)
    expected_hits = sum(
        row["generation_chance"] / row["region_size_chunks"] ** 2
        for row in rows
    )
    return {
        "schema_version": 1,
        "status": "T20_WORLDGEN_EXPECTED_READY",
        "source": {
            "revision": policy["source"]["revision"],
            "policy": {
                "path": POLICY.relative_to(ROOT).as_posix(),
                "sha256": sha256(POLICY),
            },
            "normalized_worldgen": {
                "path": SOURCE.relative_to(ROOT).as_posix(),
                "sha256": sha256(SOURCE),
            },
            "closure_ledger": {
                "path": CLOSURE.relative_to(ROOT).as_posix(),
                "sha256": sha256(CLOSURE),
            },
            "authored_catalog": {
                "path": AUTHORING.relative_to(ROOT).as_posix(),
                "sha256": sha256(AUTHORING),
            },
        },
        "counts": counts,
        "load_projection": {
            "authored_rules": len(rows),
            "configured_features": len(rows),
            "placed_features": len(rows),
            "biome_modifiers": 1,
            "generated_files": len(rows) * 2 + 1,
            "expected_ore_veins_per_chunk": rounded(expected_hits),
            "recipe_map_delta": 0,
            "logical_row_delta": 0,
            "eager_row_delta": 0,
            "lazy_row_delta": 0,
        },
        "rows": rows,
    }


def authored_document(expected: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema_version": 2,
        "id": "ore_vein_closure",
        "profile_version": 2,
        "source_ledger": {
            "path": "tools/gt6_ore_chain_closure.json",
            "classification": "vein",
        },
        "source_contract": {
            "policy": "tools/t20_worldgen_source_policy.json",
            "expected": "tools/t20_worldgen_expected.json",
            "revision": expected["source"]["revision"],
            "fidelity_contract": (
                "GT6-backed fields use declared transformations; rows without "
                "a unique GT6 worldgen fact are explicit DESIGN_POLICY and are "
                "excluded from GT6 parity claims."
            ),
        },
        "veins": expected["rows"],
    }


def compare_authored(expected: dict[str, Any]) -> list[str]:
    if not AUTHORING.is_file():
        return [AUTHORING.relative_to(ROOT).as_posix()]
    authored = load(AUTHORING)
    errors: list[str] = []
    if (
        authored.get("schema_version") != 2
        or authored.get("profile_version") != 2
    ):
        errors.append("authored worldgen schema/profile is not v2")
    expected_rows = {row["id"]: row for row in expected["rows"]}
    actual_rows = {
        row.get("id"): row
        for row in authored.get("veins", [])
        if isinstance(row, dict)
    }
    missing = sorted(set(expected_rows) - set(actual_rows))
    stale = sorted(set(actual_rows) - set(expected_rows))
    changed = sorted(
        identifier
        for identifier in set(expected_rows) & set(actual_rows)
        if expected_rows[identifier] != actual_rows[identifier]
    )
    errors.extend(f"missing:{value}" for value in missing)
    errors.extend(f"stale:{value}" for value in stale)
    errors.extend(f"changed:{value}" for value in changed)
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    parser.add_argument("--bootstrap-authoring", action="store_true")
    args = parser.parse_args()
    if args.bootstrap_authoring and not args.write:
        parser.error("--bootstrap-authoring requires --write")
    try:
        expected = build_expected()
        encoded = stable(expected)
        if args.write:
            OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
            if args.bootstrap_authoring:
                AUTHORING.write_text(
                    stable(authored_document(expected)),
                    encoding="utf-8",
                    newline="\n",
                )
            print(
                "Wrote T20 expected worldgen evidence"
                + (" and explicit authored catalog." if args.bootstrap_authoring else ".")
            )
            return 0
        errors: list[str] = []
        if (
            not OUTPUT.is_file()
            or OUTPUT.read_text(encoding="utf-8") != encoded
        ):
            errors.append(OUTPUT.relative_to(ROOT).as_posix())
        errors.extend(compare_authored(expected))
        if errors:
            print(
                "T20 worldgen projection is stale:\n"
                + "\n".join(f"- {error}" for error in errors)
            )
            return 1
        print("T20 worldgen expected and authored sets are current.")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"T20 worldgen projection failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
