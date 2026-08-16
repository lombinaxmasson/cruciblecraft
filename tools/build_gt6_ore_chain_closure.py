#!/usr/bin/env python3
"""Classify every current ore-chain coverage debt with auditable evidence."""
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

from tools import build_gt6_material_form_gate as gate_builder  # noqa: E402
from tools import build_gt6_veins as vein_builder  # noqa: E402

ORE_CHAIN = TOOLS / "gt6_ore_chain.json"
OUTPUT = TOOLS / "gt6_ore_chain_closure.json"
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
# The gate (#5) is a pure projection of these <=#4 sources; the closure pins
# them instead of the gate's own bytes so the builder graph stays acyclic.
REGISTRATION_GATE_SOURCES = {
    "l1b_selected": TOOLS / "gt6_l1b_selected.json",
    "cross_reference": TOOLS / "gt6_oredict_cross_reference.json",
    "prefix_mapping": TOOLS / "gt6_prefix_mapping.json",
    "material_activation_policy": TOOLS / "gt6_material_activation_policy.json",
    "ore_chain_operands": TOOLS / "gt6_ore_chain_operands.json",
    "acceptance_form_corrections": (
        TOOLS / "component_rule_sources" / "acceptance_form_corrections.json"
    ),
}
VEIN_ROOT = ROOT / "src/main/resources/data/cruciblecraft/veins"
L3_PLAN = TOOLS / "gt6_l3_prefix_plan.json"
T5_CHEMICAL_POLICY = TOOLS / "t5_chemical_policy.json"

CRUSHER_CLASSIFICATIONS = {"vein", "byproduct_only", "t5_chemical"}
SIFTER_CLASSIFICATIONS = {"add_smelter", "t5_chemical", "gt6_dead_end"}
CHEMICAL_TAG_TERMS = (
    "CENTRIF",
    "CHEMICAL",
    "CRYSTALL",
    "DECOMPOS",
    "ELECTROLY",
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stable_json(value: Any, *, compact: bool = False) -> str:
    if compact:
        return json.dumps(
            value,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
        ) + "\n"
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def directory_sha256(root: Path) -> str:
    entries = [
        (path.relative_to(root).as_posix(), sha256(path))
        for path in sorted(root.rglob("*.json"))
    ]
    payload = json.dumps(
        entries,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def material_path(material: str) -> str:
    return (
        "src/main/resources/data/cruciblecraft/materials/"
        f"{material}.json"
    )


def material_evidence(
    material: str,
    field: str,
    value: Any,
) -> dict[str, Any]:
    return {
        "path": material_path(material),
        "field": field,
        "value": value,
    }


def processing_evidence(
    material: str,
    targets: dict[str, Any],
) -> dict[str, Any]:
    preferred = (
        "smelting",
        "crushing",
        "pulver",
        "burning",
        "generifying",
    )
    selected = {
        key: targets[key]
        for key in preferred
        if key in targets
    }
    if not selected:
        selected = {
            key: targets[key]
            for key in sorted(targets)[:3]
        }
    return material_evidence(
        material,
        "gt6_metadata.processing_targets",
        selected,
    )


def source_dead_end(
    material: str,
    policy: dict[str, Any],
) -> dict[str, Any]:
    source = policy["source"]
    return {
        "material": material,
        "classification": policy["classification"],
        "evidence": [{
            "path": source["index"],
            "field": "recipe_input_replay",
            "value": {
                "revision": source["revision"],
                "recipe_count": source["recipe_count"],
                "matching_maps": policy["excluded_maps"],
                "meaningful_material_transform": False,
            },
        }],
        "rationale": policy["reason"],
        "target_phase": "T5_source_dead_end",
    }


def byproduct_referrers(
    documents: dict[str, dict[str, Any]],
) -> dict[str, list[str]]:
    result: dict[str, list[str]] = defaultdict(list)
    for referrer, document in documents.items():
        for row in (
            document.get("gt6_metadata", {}).get("byproducts") or []
        ):
            material = row.get("material")
            if isinstance(material, str):
                result[material].append(referrer)
    return {
        material: sorted(set(referrers))
        for material, referrers in result.items()
    }


def classify_crusher(
    material: str,
    document: dict[str, Any],
    factual_forms: set[str],
    registered_forms: set[str],
    authored_vein_materials: set[str],
    referrers: list[str],
) -> dict[str, Any]:
    metadata = document.get("gt6_metadata") or {}
    tags = metadata.get("material_tags") or []
    factual_registered_ore = (
        "ore" in factual_forms and "ore" in registered_forms
    )
    if factual_registered_ore:
        if material in authored_vein_materials:
            raise ValueError(
                f"{material}: crusher debt unexpectedly has authored worldgen"
            )
        evidence = [
            material_evidence(
                material,
                "resolved_factual_forms",
                {"ore": True},
            ),
            {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "material_registration_gate.json"
                ),
                "field": f"materials.{material}",
                "value": {"ore_registered": True},
            },
            {
                "path": "src/main/resources/data/cruciblecraft/veins",
                "field": "authored_materials",
                "value": {"material": material, "present": False},
            },
        ]
        if "PROPERTIES.COMMON_ORE" in tags:
            evidence.append(material_evidence(
                material,
                "gt6_metadata.material_tags",
                ["PROPERTIES.COMMON_ORE"],
            ))
        return {
            "material": material,
            "classification": "vein",
            "evidence": evidence,
            "rationale": (
                "The material has a factual, gate-registered ORE form and GT6 "
                "marks it as COMMON_ORE, but no current authored vein names it."
            ),
            "target_phase": "future_worldgen_content",
        }

    if referrers:
        return {
            "material": material,
            "classification": "byproduct_only",
            "evidence": [{
                "path": (
                    "src/main/resources/data/cruciblecraft/materials"
                ),
                "field": "gt6_metadata.byproducts[].material",
                "value": {
                    "referenced_material": material,
                    "referrers": referrers,
                },
            }],
            "rationale": (
                "The material is directly named as a GT6 ore-processing "
                "byproduct and has no factual registered ORE form requiring an "
                "independent vein."
            ),
            "target_phase": "T5_byproduct_closure",
        }

    composition = document.get("composition")
    if composition:
        evidence = [
            material_evidence(material, "composition", composition),
        ]
        rationale = (
            "The declared material composition supplies a direct chemical "
            "closure input for T5 processing design."
        )
    else:
        targets = metadata.get("processing_targets") or {}
        if targets:
            evidence = [processing_evidence(material, targets)]
            rationale = (
                "GT6 processing targets directly describe transformations for "
                "this non-ore, non-byproduct material and make it a T5 "
                "processing input."
            )
        elif tags:
            evidence = [
                material_evidence(
                    material,
                    "gt6_metadata.material_tags",
                    tags,
                ),
            ]
            rationale = (
                "GT6 material tags are the direct source metadata available "
                "for deciding this material's T5 processing route."
            )
        else:
            raise ValueError(
                f"{material}: no direct evidence supports a crusher classification"
            )
    return {
        "material": material,
        "classification": "t5_chemical",
        "evidence": evidence,
        "rationale": rationale,
        "target_phase": "T5_chemical_closure",
    }


def smelter_projection(
    material: str,
    document: dict[str, Any],
    registered_forms: dict[str, set[str]],
) -> dict[str, Any]:
    target = (
        document.get("gt6_metadata", {})
        .get("processing_targets", {})
        .get("smelting")
    )
    if not isinstance(target, dict):
        return {
            "target": None,
            "target_ingot_registered": False,
            "units_representable": False,
        }
    target_material = target.get("material")
    target_units = target.get("cc_units")
    target_ingot_registered = (
        isinstance(target_material, str)
        and "ingot" in registered_forms.get(target_material, set())
    )
    units_representable = (
        isinstance(target_units, int)
        and not isinstance(target_units, bool)
        and target_units > 0
    )
    result = {
        "target": target,
        "target_ingot_registered": target_ingot_registered,
        "units_representable": units_representable,
    }
    if units_representable:
        divisor = math.gcd(target_units, 144)
        result["projected_counts"] = {
            "dust_input": 144 // divisor,
            "ingot_output": target_units // divisor,
        }
    return result


def classify_sifter(
    material: str,
    document: dict[str, Any],
    registered_forms: dict[str, set[str]],
    source_dead_end_policy: dict[str, Any] | None = None,
) -> dict[str, Any]:
    if (
        source_dead_end_policy is not None
        and material in source_dead_end_policy["materials"]
    ):
        return source_dead_end(material, source_dead_end_policy)
    metadata = document.get("gt6_metadata") or {}
    projection = smelter_projection(material, document, registered_forms)
    target = projection["target"]
    if (
        target is not None
        and projection["target_ingot_registered"]
        and projection["units_representable"]
    ):
        return {
            "material": material,
            "classification": "add_smelter",
            "evidence": [
                material_evidence(
                    material,
                    "gt6_metadata.processing_targets.smelting",
                    target,
                ),
                {
                    "path": (
                        "src/main/resources/data/cruciblecraft/"
                        "material_registration_gate.json"
                    ),
                    "field": f"materials.{target['material']}",
                    "value": {
                        "ingot_registered": True,
                        **projection["projected_counts"],
                    },
                },
            ],
            "rationale": (
                "GT6 supplies a positive smelting conversion to a registered "
                "ingot and its 144-unit ratio has an exact integer projection, "
                "so the concrete ore-chain builder must emit this smelter."
            ),
            "target_phase": "T2c_concrete_smelter",
        }

    composition = document.get("composition")
    tags = metadata.get("material_tags") or []
    chemical_tags = sorted({
        tag
        for tag in tags
        if any(term in tag for term in CHEMICAL_TAG_TERMS)
    })
    if target is not None or composition or chemical_tags:
        evidence: list[dict[str, Any]] = []
        if target is not None:
            evidence.append(material_evidence(
                material,
                "gt6_metadata.processing_targets.smelting",
                {
                    **target,
                    "target_ingot_registered": projection[
                        "target_ingot_registered"
                    ],
                    "units_representable": projection[
                        "units_representable"
                    ],
                },
            ))
        if composition:
            evidence.append(
                material_evidence(material, "composition", composition)
            )
        if chemical_tags:
            evidence.append(material_evidence(
                material,
                "gt6_metadata.material_tags",
                chemical_tags,
            ))
        return {
            "material": material,
            "classification": "t5_chemical",
            "evidence": evidence,
            "rationale": (
                "Direct GT6 smelting/composition/chemical metadata exists, but "
                "the target ingot registration or positive unit conversion "
                "required by the ordinary concrete smelter is absent."
            ),
            "target_phase": "T5_chemical_closure",
        }

    return {
        "material": material,
        "classification": "gt6_dead_end",
        "evidence": [
            material_evidence(
                material,
                "gt6_metadata.processing_targets.smelting",
                {"present": False},
            ),
            material_evidence(
                material,
                "composition",
                {"present": False},
            ),
            material_evidence(
                material,
                "gt6_metadata.material_tags",
                {
                    "chemical_tags": [],
                    "recorded_tags": tags,
                },
            ),
        ],
        "rationale": (
            "GT6 records no smelting target, composition, or chemical "
            "processing tag from which an ore-chain closure can be derived."
        ),
        "target_phase": "T5_dead_end_review",
    }


def validate_ledger(
    name: str,
    debts: list[str],
    rows: list[dict[str, Any]],
    allowed: set[str],
) -> None:
    expected = set(debts)
    materials = [row.get("material") for row in rows]
    if len(materials) != len(set(materials)):
        raise ValueError(f"{name}: duplicate closure classifications")
    actual = set(materials)
    if actual != expected:
        missing = sorted(expected - actual)
        stale = sorted(actual - expected)
        raise ValueError(
            f"{name}: bidirectional mismatch; missing={missing}, stale={stale}"
        )
    for row in rows:
        material = row["material"]
        if row.get("classification") not in allowed:
            raise ValueError(
                f"{name}/{material}: invalid classification "
                f"{row.get('classification')!r}"
            )
        evidence = row.get("evidence")
        if not isinstance(evidence, list) or not evidence:
            raise ValueError(f"{name}/{material}: evidence must be non-empty")
        if not all(isinstance(entry, dict) and entry for entry in evidence):
            raise ValueError(f"{name}/{material}: evidence rows must be objects")
        for field in ("rationale", "target_phase"):
            value = row.get(field)
            if not isinstance(value, str) or not value.strip():
                raise ValueError(f"{name}/{material}: {field} must be non-empty")


def build_document() -> dict[str, Any]:
    ore_chain = load(ORE_CHAIN)
    coverage = ore_chain["coverage_ledger"]
    documents, factual_forms = gate_builder.material_documents()
    gate = load(REGISTRATION_GATE).get("materials") or {}
    if set(documents) != set(gate):
        raise ValueError(
            "registration gate does not cover the factual material catalog"
        )
    registered_forms = {
        material: set(forms)
        for material, forms in gate.items()
    }
    veins = vein_builder.load_veins()
    authored_vein_materials = {
        entry["material"]
        for vein in veins
        for layer in vein_builder.LAYERS
        for entry in vein[layer]
    }
    referrers = byproduct_referrers(documents)
    source_dead_end_policy = load(T5_CHEMICAL_POLICY)[
        "source_dead_end_policy"
    ]

    crusher_debts = coverage["crusher_without_worldgen"]
    sifter_debts = coverage["sifter_dust_without_smelter"]
    crusher_rows = [
        classify_crusher(
            material,
            documents[material],
            factual_forms[material],
            registered_forms[material],
            authored_vein_materials,
            referrers.get(material, []),
        )
        for material in crusher_debts
    ]
    sifter_rows = [
        classify_sifter(
            material,
            documents[material],
            registered_forms,
            source_dead_end_policy,
        )
        for material in sifter_debts
    ]
    validate_ledger(
        "crusher_without_worldgen",
        crusher_debts,
        crusher_rows,
        CRUSHER_CLASSIFICATIONS,
    )
    validate_ledger(
        "sifter_dust_without_smelter",
        sifter_debts,
        sifter_rows,
        SIFTER_CLASSIFICATIONS,
    )

    crusher_counts = Counter(
        row["classification"] for row in crusher_rows
    )
    sifter_counts = Counter(
        row["classification"] for row in sifter_rows
    )
    unclassified = (
        len(crusher_debts)
        + len(sifter_debts)
        - len(crusher_rows)
        - len(sifter_rows)
    )
    if unclassified:
        raise ValueError(f"closure has {unclassified} unclassified debts")

    return {
        "schema_version": 1,
        "inputs": {
            "ore_chain_coverage_ledger": {
                "path": "tools/gt6_ore_chain.json",
                "sha256": sha256(ORE_CHAIN),
            },
            "material_catalog": {
                "path": (
                    "src/main/resources/data/cruciblecraft/materials"
                ),
                "sha256": directory_sha256(MATERIAL_ROOT),
            },
            "material_registration_sources": {
                name: {
                    "path": path.relative_to(ROOT).as_posix(),
                    "sha256": sha256(path),
                }
                for name, path in REGISTRATION_GATE_SOURCES.items()
            },
            "material_registration_gate": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "material_registration_gate.json"
                ),
                "note": (
                    "informational; gate is a pure projection of "
                    "material_registration_sources"
                ),
            },
            "factual_form_plan": {
                "path": "tools/gt6_l3_prefix_plan.json",
                "sha256": sha256(L3_PLAN),
            },
            "t5_source_dead_end_policy": {
                "path": "tools/t5_chemical_policy.json",
                "sha256": sha256(T5_CHEMICAL_POLICY),
            },
            "vein_author_sources": {
                "path": "src/main/resources/data/cruciblecraft/veins",
                "sha256": directory_sha256(VEIN_ROOT),
            },
        },
        "counts": {
            "crusher_without_worldgen": len(crusher_rows),
            "crusher_classifications": {
                key: crusher_counts.get(key, 0)
                for key in sorted(CRUSHER_CLASSIFICATIONS)
            },
            "sifter_dust_without_smelter": len(sifter_rows),
            "sifter_classifications": {
                key: sifter_counts.get(key, 0)
                for key in sorted(SIFTER_CLASSIFICATIONS)
            },
        },
        "crusher_without_worldgen": crusher_rows,
        "sifter_dust_without_smelter": sifter_rows,
        "unclassified_count": unclassified,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--review", action="store_true")
    args = parser.parse_args()
    if sum((args.write, args.check, args.review)) != 1:
        parser.error("choose exactly one of --write, --check, or --review")

    document = build_document()
    if args.review:
        print(stable_json({
            "counts": document["counts"],
            "unclassified_count": document["unclassified_count"],
        }), end="")
        return 0
    expected = stable_json(document, compact=True)
    if args.write:
        OUTPUT.write_text(expected, encoding="utf-8", newline="\n")
        print(f"Wrote {OUTPUT}")
        return 0
    if not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != expected:
        print(
            f"Ore-chain closure artifact is stale: {OUTPUT}",
            file=sys.stderr,
        )
        return 1
    print("Ore-chain closure artifact is current.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
