#!/usr/bin/env python3
"""Build the deterministic T8 pipe source/budget readiness ledger."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

try:
    from tools import gt6_pipes
except ModuleNotFoundError:
    import gt6_pipes


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
SOURCE = TOOLS / "gt6_pipe_source.json"
POLICY = TOOLS / "t8_pipe_policy.json"
NORMALIZED_MATERIALS = TOOLS / "gt6_oredict_materials_normalized.json"
NORMALIZED_PREFIXES = TOOLS / "gt6_oredict_prefixes_normalized.json"
MATERIAL_ROOT = ROOT / "src/main/resources/data/cruciblecraft/materials"
MATERIAL_INDEX = MATERIAL_ROOT / "index.json"
OUTPUT = TOOLS / "t8_pipe_readiness.json"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stable_hash(value: Any) -> str:
    encoded = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def load_live_materials(
    index: list[str],
) -> tuple[list[dict[str, Any]], dict[str, dict[str, Any]]]:
    rows = []
    by_id = {}
    for filename in index:
        document = load(MATERIAL_ROOT / filename)
        material_id = str(document["id"])
        if filename != f"{material_id}.json":
            raise ValueError(
                f"material index filename/id mismatch: {filename}"
            )
        if material_id in by_id:
            raise ValueError(f"duplicate live material id: {material_id}")
        rows.append(document)
        by_id[material_id] = document
    return rows, by_id


def material_tree_hash(index: list[str]) -> str:
    return stable_hash(
        [(filename, sha256(MATERIAL_ROOT / filename)) for filename in index]
    )


def source_identity(document: dict[str, Any]) -> tuple[int, str]:
    metadata = document.get("gt6_metadata") or {}
    return int(metadata["source_id"]), str(metadata["source_name"])


def classify_domain(
    *,
    domain: str,
    raw_records: list[dict[str, Any]],
    live_materials: list[dict[str, Any]],
    direct_rows: list[dict[str, Any]],
    extension_rows: list[dict[str, Any]],
) -> dict[str, Any]:
    direct_names = {str(row["source_name"]) for row in direct_rows}
    extension_names = {str(row["source_name"]) for row in extension_rows}
    if len(direct_names) != len(direct_rows):
        raise ValueError(f"{domain}: duplicate direct source classification")
    if len(extension_names) != len(extension_rows):
        raise ValueError(
            f"{domain}: duplicate acceptance extension classification"
        )
    overlap = sorted(direct_names & extension_names)
    if overlap:
        raise ValueError(
            f"{domain}: acceptance extension overlaps direct source: "
            + ", ".join(overlap)
        )

    extension_by_name = {
        str(row["source_name"]): str(row["material"])
        for row in extension_rows
    }
    raw_by_name = {
        str(row["source_name"]): row for row in raw_records
    }
    if len(raw_by_name) != len(raw_records):
        raise ValueError("normalized raw material source_name is not unique")
    missing_raw_direct = sorted(direct_names - set(raw_by_name))
    missing_raw_extensions = sorted(extension_names - set(raw_by_name))
    if missing_raw_direct or missing_raw_extensions:
        raise ValueError(
            f"{domain}: source classification names are absent from raw "
            f"materials: direct={missing_raw_direct}, "
            f"extensions={missing_raw_extensions}"
        )

    live_by_source: dict[str, dict[str, Any]] = {}
    for document in live_materials:
        _, name = source_identity(document)
        if name in live_by_source:
            raise ValueError(f"duplicate live source_name: {name}")
        live_by_source[name] = document
    missing_live_direct = sorted(direct_names - set(live_by_source))
    missing_live_extensions = sorted(extension_names - set(live_by_source))
    if missing_live_direct or missing_live_extensions:
        raise ValueError(
            f"{domain}: source classification names are absent from live "
            f"materials: direct={missing_live_direct}, "
            f"extensions={missing_live_extensions}"
        )

    raw_direct = [
        {
            "source_id": int(raw_by_name[name]["source_id"]),
            "source_name": name,
        }
        for name in sorted(direct_names)
    ]
    raw_extensions = [
        {
            "material": extension_by_name[name],
            "source_id": int(raw_by_name[name]["source_id"]),
            "source_name": name,
            "classification": "cruciblecraft_acceptance_extension",
            "direct_gt6_source": False,
        }
        for name in sorted(extension_names)
    ]
    live_direct = []
    for name in sorted(direct_names):
        document = live_by_source[name]
        source_id, _ = source_identity(document)
        live_direct.append({
            "material": str(document["id"]),
            "source_id": source_id,
            "source_name": name,
        })
    live_extensions = []
    for name in sorted(extension_names):
        document = live_by_source[name]
        source_id, _ = source_identity(document)
        expected_material = extension_by_name[name]
        if document["id"] != expected_material:
            raise ValueError(
                f"{domain}: extension {name} maps to {document['id']}, "
                f"expected {expected_material}"
            )
        live_extensions.append({
            "material": expected_material,
            "source_id": source_id,
            "source_name": name,
            "classification": "cruciblecraft_acceptance_extension",
            "direct_gt6_source": False,
        })

    raw_classified = len(raw_direct) + len(raw_extensions)
    live_classified = len(live_direct) + len(live_extensions)
    return {
        "raw": {
            "denominator": len(raw_records),
            "counts": {
                "direct_source": len(raw_direct),
                "acceptance_extension": len(raw_extensions),
                "not_applicable": len(raw_records) - raw_classified,
                "unclassified": 0,
            },
            "direct_source_set": raw_direct,
            "acceptance_extension_set": raw_extensions,
        },
        "live": {
            "denominator": len(live_materials),
            "counts": {
                "direct_source": len(live_direct),
                "acceptance_extension": len(live_extensions),
                "not_applicable": len(live_materials) - live_classified,
                "unclassified": 0,
            },
            "direct_source_set": live_direct,
            "acceptance_extension_set": live_extensions,
        },
    }


def validate_gauges(
    policy: dict[str, Any],
    source: dict[str, Any],
    normalized_prefixes: dict[str, Any],
) -> dict[str, Any]:
    source_fluid = {
        str(row["name"]): row for row in source["fluid_gauges"]
    }
    source_item = {
        str(row["name"]): row for row in source["item_gauges"]
    }
    selected_fluid = policy["fluid_domain"]["selected_gauges"]
    selected_item = policy["item_domain"]["selected_gauges"]
    if [row["name"] for row in selected_fluid] != [
        "tiny",
        "small",
        "normal",
        "large",
        "huge",
    ]:
        raise ValueError("selected fluid gauge set drifted")
    if [row["name"] for row in selected_item] != [
        "normal",
        "large",
        "huge",
    ]:
        raise ValueError("selected item gauge set drifted")
    for row in selected_fluid:
        source_row = source_fluid[row["name"]]
        for field in ("source_prefix", "capacity_multiplier"):
            if row[field] != source_row[field]:
                raise ValueError(
                    f"fluid/{row['name']}: selected gauge facts drifted"
                )
    for row in selected_item:
        source_row = source_item[row["name"]]
        for field in (
            "source_prefix",
            "step_size_divisor",
            "inventory_multiplier",
        ):
            if row[field] != source_row[field]:
                raise ValueError(
                    f"item/{row['name']}: selected gauge facts drifted"
                )

    logical_prefixes = [
        str(row["logical_prefix"])
        for row in selected_fluid + selected_item
    ]
    required_count = int(
        policy["budget"]["required_selected_prefix_count"]
    )
    if (
        len(logical_prefixes) != required_count
        or len(set(logical_prefixes)) != required_count
    ):
        raise ValueError(
            "selected logical prefix count/identity drifted: "
            f"{len(logical_prefixes)} != {required_count}"
        )

    prefix_by_name = {
        str(row["source_name"]): row
        for row in normalized_prefixes["records"]
    }
    selected_source_prefixes = sorted({
        str(row["source_prefix"])
        for row in selected_fluid + selected_item
    })
    missing = sorted(set(selected_source_prefixes) - set(prefix_by_name))
    if missing:
        raise ValueError(
            "normalized GT6 pipe prefixes are missing: " + ", ".join(missing)
        )

    fluid_deferred = policy["fluid_domain"]["deferred_gauges"]
    if {row["name"] for row in fluid_deferred} != {
        "quadruple",
        "nonuple",
    }:
        raise ValueError("fluid deferred gauge classification drifted")
    item_deferred = policy["item_domain"]["deferred_gauges"]
    if {row["name"] for row in item_deferred} != {
        "small",
        "restrictive_small",
        "restrictive_normal",
        "restrictive_large",
        "restrictive_huge",
    }:
        raise ValueError("item deferred gauge classification drifted")
    source_restrictive = {
        name for name, row in source_item.items() if row["restrictive"]
    }
    if not source_restrictive.issubset({
        row["name"] for row in item_deferred
    }):
        raise ValueError("not all GT6 restrictive item gauges are deferred")

    return {
        "selected_prefix_count": len(logical_prefixes),
        "selected_logical_prefixes": logical_prefixes,
        "selected_source_prefixes": selected_source_prefixes,
        "normalized_prefix_denominator": len(
            normalized_prefixes["records"]
        ),
        "deferred_gauges": {
            "fluid": fluid_deferred,
            "item": item_deferred,
        },
    }


def extension_fluid_row(
    defaults: dict[str, Any],
) -> dict[str, Any]:
    if defaults.get("max_temperature_rule") != (
        "floor(source_melting_kelvin*1.25)"
    ):
        raise ValueError("fluid extension temperature rule drifted")
    expected = {
        "base_capacity": 100,
        "gas_proof": False,
        "acid_proof": False,
        "plasma_proof": False,
        "magic_proof": False,
        "contact_damage": True,
        "flammable": False,
        "recipe": True,
        "blocking": True,
        "max_temperature_rule":
            "floor(source_melting_kelvin*1.25)",
    }
    if defaults != expected:
        raise ValueError("fluid acceptance extension defaults drifted")
    return {
        key: value
        for key, value in defaults.items()
        if key != "max_temperature_rule"
    } | {"max_temperature_override": None}


def extension_item_row(defaults: dict[str, Any]) -> dict[str, Any]:
    expected = {
        "base_step_size": 16384,
        "base_stacks_per_second": 1,
        "recipe": True,
        "blocking": True,
    }
    if defaults != expected:
        raise ValueError("item acceptance extension defaults drifted")
    return {
        "base_step_size": defaults["base_step_size"],
        "base_inventory_size": defaults["base_stacks_per_second"],
        "recipe": defaults["recipe"],
        "blocking": defaults["blocking"],
    }


def melting_kelvin(document: dict[str, Any]) -> int:
    metadata = document.get("gt6_metadata") or {}
    thermal = metadata.get("source_thermal") or {}
    value = thermal.get("melting_point_kelvin")
    if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
        raise ValueError(
            f"{document.get('id')}: invalid source melting Kelvin"
        )
    return value


def build_domain_catalog(
    *,
    domain: str,
    classification: dict[str, Any],
    direct_rows: list[dict[str, Any]],
    gauges: list[dict[str, Any]],
    extension_defaults: dict[str, Any],
    live_by_id: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    direct_by_name = {
        str(row["source_name"]): row for row in direct_rows
    }
    materials = []
    for classified in classification["live"]["direct_source_set"]:
        material = str(classified["material"])
        document = live_by_id[material]
        source_row = direct_by_name[classified["source_name"]]
        if domain == "fluid":
            specifications = gt6_pipes.fluid_specifications(
                source_row,
                gauges,
                melting_kelvin(document),
            )
        else:
            specifications = gt6_pipes.item_specifications(
                source_row,
                gauges,
            )
        materials.append({
            **classified,
            "classification": "direct_gt6_source",
            "direct_gt6_source": True,
            "evidence": {
                "kind": (
                    "exact_addFluidPipes_call"
                    if domain == "fluid"
                    else "exact_addItemPipes_call"
                ),
                "source_path": (
                    "src/main/java/gregtech/loaders/b/"
                    "Loader_MultiTileEntities.java"
                ),
                "base_id": source_row["base_id"],
                "source_symbol": source_row["source_symbol"],
            },
            "specifications_by_gauge": specifications,
        })

    if domain == "fluid":
        extension_row = extension_fluid_row(extension_defaults)
    else:
        extension_row = extension_item_row(extension_defaults)
    for classified in classification["live"][
        "acceptance_extension_set"
    ]:
        material = str(classified["material"])
        document = live_by_id[material]
        if domain == "fluid":
            specifications = gt6_pipes.fluid_specifications(
                extension_row,
                gauges,
                melting_kelvin(document),
            )
        else:
            specifications = gt6_pipes.item_specifications(
                extension_row,
                gauges,
            )
        materials.append({
            **classified,
            "evidence": {
                "kind": "cruciblecraft_acceptance_extension",
                "source_path": "tools/t8_pipe_policy.json",
                "direct_gt6_source": False,
            },
            "specifications_by_gauge": specifications,
        })

    materials.sort(key=lambda row: row["material"])
    block_count = len(materials) * len(gauges)
    recipe_material_count = sum(
        any(
            specification["recipe"]
            for specification in row["specifications_by_gauge"].values()
        )
        for row in materials
    )
    return {
        "selected_gauges": gauges,
        "eligible_material_count": len(materials),
        "direct_material_count": len(
            classification["live"]["direct_source_set"]
        ),
        "acceptance_extension_material_count": len(
            classification["live"]["acceptance_extension_set"]
        ),
        "runtime_block_count": block_count,
        "recipe_enabled_material_count": recipe_material_count,
        "recipe_expansion_count": recipe_material_count * len(gauges),
        "material_catalog": materials,
    }


def build_acceptance(
    policy: dict[str, Any],
    fluid: dict[str, Any],
    item: dict[str, Any],
) -> list[dict[str, Any]]:
    fluid_by_material = {
        row["material"]: row for row in fluid["material_catalog"]
    }
    item_by_material = {
        row["material"]: row for row in item["material_catalog"]
    }
    result = []
    for material in policy["acceptance_materials"]:
        if material not in fluid_by_material or material not in item_by_material:
            raise ValueError(
                f"acceptance material is missing a pipe domain: {material}"
            )
        fluid_row = fluid_by_material[material]
        item_row = item_by_material[material]
        if fluid_row["source_name"] != item_row["source_name"]:
            raise ValueError(f"{material}: acceptance source_name mismatch")
        result.append({
            "material": material,
            "source_name": fluid_row["source_name"],
            "fluid_classification": fluid_row["classification"],
            "item_classification": item_row["classification"],
        })
    if {row["material"] for row in result} != {"copper", "tin", "iron"}:
        raise ValueError("acceptance material set drifted")
    if next(
        row for row in result if row["material"] == "copper"
    )["fluid_classification"] != "direct_gt6_source":
        raise ValueError("copper fluid pipe must retain direct GT6 evidence")
    return result


def build(
    policy_override: dict[str, Any] | None = None,
    source_override: dict[str, Any] | None = None,
) -> dict[str, Any]:
    policy = policy_override or load(POLICY)
    source = source_override or gt6_pipes.load(SOURCE)
    gt6_pipes.validate(source)
    if policy.get("schema_version") != 1:
        raise ValueError("T8 pipe policy schema_version must be 1")
    runtime_api_decisions = policy.get("runtime_api_decisions")
    if not isinstance(runtime_api_decisions, dict) or {
        key: (value or {}).get("status")
        for key, value in runtime_api_decisions.items()
    } != {
        "outgoing_cover_guard": "REMOVED",
        "cover_set_version": "REMOVED",
    }:
        raise ValueError("T8 runtime API cleanup decisions are incomplete")

    normalized_materials = load(NORMALIZED_MATERIALS)
    normalized_prefixes = load(NORMALIZED_PREFIXES)
    raw_records = normalized_materials["records"]
    index = load(MATERIAL_INDEX)
    live_materials, live_by_id = load_live_materials(index)
    source_to_live = {
        source_identity(row)[1]: row for row in live_materials
    }
    if len(source_to_live) != len(live_materials):
        raise ValueError("live material source_name domain is not unique")

    fluid_classification = classify_domain(
        domain="fluid",
        raw_records=raw_records,
        live_materials=live_materials,
        direct_rows=source["direct_addFluidPipes_calls"],
        extension_rows=policy["acceptance_extensions"]["fluid"],
    )
    item_classification = classify_domain(
        domain="item",
        raw_records=raw_records,
        live_materials=live_materials,
        direct_rows=source["direct_addItemPipes_calls"],
        extension_rows=policy["acceptance_extensions"]["item"],
    )
    prefix_facts = validate_gauges(
        policy,
        source,
        normalized_prefixes,
    )

    selected_fluid_names = [
        row["name"]
        for row in policy["fluid_domain"]["selected_gauges"]
    ]
    selected_item_names = [
        row["name"]
        for row in policy["item_domain"]["selected_gauges"]
    ]
    fluid_gauges_by_name = {
        row["name"]: row for row in source["fluid_gauges"]
    }
    item_gauges_by_name = {
        row["name"]: row for row in source["item_gauges"]
    }
    fluid_gauges = [
        fluid_gauges_by_name[name] for name in selected_fluid_names
    ]
    item_gauges = [
        item_gauges_by_name[name] for name in selected_item_names
    ]
    fluid_domain = build_domain_catalog(
        domain="fluid",
        classification=fluid_classification,
        direct_rows=source["direct_addFluidPipes_calls"],
        gauges=fluid_gauges,
        extension_defaults=policy["extension_defaults"]["fluid"],
        live_by_id=live_by_id,
    )
    item_domain = build_domain_catalog(
        domain="item",
        classification=item_classification,
        direct_rows=source["direct_addItemPipes_calls"],
        gauges=item_gauges,
        extension_defaults=policy["extension_defaults"]["item"],
        live_by_id=live_by_id,
    )

    connection_masks = 2 ** int(
        policy["budget"]["connection_directions"]
    )
    runtime_blocks = (
        fluid_domain["runtime_block_count"]
        + item_domain["runtime_block_count"]
    )
    logical_states = runtime_blocks * connection_masks
    selected_prefixes = prefix_facts["selected_prefix_count"]
    unclassified = sum(
        classification[scope]["counts"]["unclassified"]
        for classification in (
            fluid_classification,
            item_classification,
        )
        for scope in ("raw", "live")
    )
    actual_counts = {
        "raw_material_count": len(raw_records),
        "live_material_count": len(live_materials),
        "fluid_raw_direct": fluid_classification["raw"]["counts"][
            "direct_source"
        ],
        "fluid_raw_extensions": fluid_classification["raw"]["counts"][
            "acceptance_extension"
        ],
        "fluid_live_direct": fluid_classification["live"]["counts"][
            "direct_source"
        ],
        "fluid_live_extensions": fluid_classification["live"]["counts"][
            "acceptance_extension"
        ],
        "item_raw_direct": item_classification["raw"]["counts"][
            "direct_source"
        ],
        "item_raw_extensions": item_classification["raw"]["counts"][
            "acceptance_extension"
        ],
        "item_live_direct": item_classification["live"]["counts"][
            "direct_source"
        ],
        "item_live_extensions": item_classification["live"]["counts"][
            "acceptance_extension"
        ],
        "fluid_runtime_blocks": fluid_domain["runtime_block_count"],
        "item_runtime_blocks": item_domain["runtime_block_count"],
        "combined_runtime_blocks": runtime_blocks,
        "logical_states": logical_states,
        "selected_prefixes": selected_prefixes,
        "unclassified": unclassified,
    }
    if actual_counts != policy["expected_counts"]:
        raise ValueError(
            f"T8 expected counts drifted: {actual_counts} != "
            f"{policy['expected_counts']}"
        )

    budget = policy["budget"]
    if runtime_blocks > int(budget["max_combined_runtime_blocks"]):
        raise ValueError(
            "T8 runtime block budget exceeded: "
            f"{runtime_blocks} > {budget['max_combined_runtime_blocks']}"
        )
    if logical_states > int(budget["max_logical_states"]):
        raise ValueError(
            "T8 logical-state budget exceeded: "
            f"{logical_states} > {budget['max_logical_states']}"
        )
    if logical_states != runtime_blocks * 64:
        raise ValueError("T8 logical states must equal runtime blocks * 64")
    if unclassified:
        raise ValueError("T8 pipe classification is not closed")

    unobtainable_policy = policy["unobtainable_nonmetal_fluid_pipes"]
    fluid_by_material = {
        row["material"]: row for row in fluid_domain["material_catalog"]
    }
    unobtainable_materials = sorted(unobtainable_policy["materials"])
    unobtainable_forms = []
    for material in unobtainable_materials:
        row = fluid_by_material.get(material)
        if row is None:
            raise ValueError(
                f"O-27 material is absent from the fluid domain: {material}"
            )
        for gauge, specification in sorted(
            row["specifications_by_gauge"].items()
        ):
            if specification["recipe"]:
                raise ValueError(
                    f"O-27 material unexpectedly has a recipe: {material}/{gauge}"
                )
            unobtainable_forms.append({
                "material": material,
                "gauge": gauge,
            })
    if (
        len(fluid_gauges) != unobtainable_policy["expected_gauge_count"]
        or len(unobtainable_forms) != unobtainable_policy["expected_form_count"]
    ):
        raise ValueError("O-27 unobtainable pipe-form count drifted")
    unobtainable = {
        **unobtainable_policy,
        "materials": unobtainable_materials,
        "forms": unobtainable_forms,
        "form_count": len(unobtainable_forms),
    }

    recipe_projection = {
        **policy["recipe_projection"],
        "fluid_generic_rule_count": len(fluid_gauges),
        "item_generic_rule_count": len(item_gauges),
        "fluid_material_expansion_count": fluid_domain[
            "recipe_expansion_count"
        ],
        "item_material_expansion_count": item_domain[
            "recipe_expansion_count"
        ],
        "generic_rule_count": selected_prefixes,
        "material_expansion_count": (
            fluid_domain["recipe_expansion_count"]
            + item_domain["recipe_expansion_count"]
        ),
        "per_material_java_required": False,
    }
    if (
        recipe_projection["generic_rule_count"]
        != policy["recipe_projection"]["generic_rule_count"]
        or recipe_projection["material_expansion_count"]
        != policy["recipe_projection"]["material_expansion_count"]
    ):
        raise ValueError("T8 recipe projection counts drifted")
    if policy["import_architecture"]["per_material_java_required"]:
        raise ValueError("T8 must not require per-material Java")

    acceptance = build_acceptance(policy, fluid_domain, item_domain)
    status = policy["readiness_policy"]["ready_status"]
    input_paths = [
        SOURCE,
        POLICY,
        NORMALIZED_MATERIALS,
        NORMALIZED_PREFIXES,
        MATERIAL_INDEX,
    ]
    return {
        "schema_version": 1,
        "gate": policy["gate"],
        "status": status,
        "generated_by": "tools/build_t8_pipe_readiness.py",
        "input_sha256": {
            path.relative_to(ROOT).as_posix(): sha256(path)
            for path in input_paths
        },
        "material_tree_sha256": material_tree_hash(index),
        "sources": {
            "gt6": source["gt6_source"],
            "gtm_reference": source["gtm_reference"],
        },
        "semantic_model": source["semantic_model"],
        "classification_policy": policy["classification_policy"],
        "classification": {
            "fluid": fluid_classification,
            "item": item_classification,
        },
        "counts": actual_counts,
        "prefix_facts": prefix_facts,
        "acceptance_materials": acceptance,
        "fluid_domain": fluid_domain,
        "item_domain": item_domain,
        "runtime_budget": {
            **budget,
            "connection_masks_per_block": connection_masks,
            "fluid_runtime_blocks": fluid_domain["runtime_block_count"],
            "item_runtime_blocks": item_domain["runtime_block_count"],
            "combined_runtime_blocks": runtime_blocks,
            "logical_states": logical_states,
            "within_block_budget": True,
            "within_logical_state_budget": True,
        },
        "unobtainable_nonmetal_fluid_pipes": unobtainable,
        "recipe_projection": recipe_projection,
        "runtime_api_decisions": runtime_api_decisions,
        "import_architecture": policy["import_architecture"],
        "readiness": {
            "status": status,
            "blockers": [],
            "criteria": {
                "source_revisions_and_hashes_pinned": True,
                "direct_call_extraction_valid": True,
                "raw_and_live_classification_closed": True,
                "extensions_distinct_from_gt6_source": True,
                "selected_prefix_count_exact": True,
                "runtime_budget_within_caps": True,
                "deferred_gauges_explicit": True,
                "generic_recipe_projection": True,
                "unobtainable_nonmetal_pipe_debt_closed": True,
                "per_material_java_required": False,
            },
            "requirements": policy["readiness_policy"]["requirements"],
            "runtime_implementation_status": policy["readiness_policy"][
                "runtime_implementation_status"
            ],
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T8 pipe-readiness ledger is stale",
    )
    parser.add_argument(
        "--source-root",
        type=Path,
        help="optional pinned GT6 checkout; verify revision and source hashes",
    )
    args = parser.parse_args()
    try:
        source = gt6_pipes.load(SOURCE)
        if args.source_root is not None:
            gt6_pipes.validate_source_root(
                source,
                args.source_root.resolve(),
            )
        encoded = stable_json(build())
        if args.check:
            if not OUTPUT.is_file():
                raise ValueError(f"missing committed ledger: {OUTPUT}")
            if OUTPUT.read_text(encoding="utf-8") != encoded:
                raise ValueError(
                    "committed T8 pipe readiness ledger is stale"
                )
        else:
            OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
        result = json.loads(encoded)
        print(json.dumps({
            "output": OUTPUT.relative_to(ROOT).as_posix(),
            "status": result["status"],
            "counts": result["counts"],
        }, sort_keys=True))
        return 0
    except (
        OSError,
        ValueError,
        KeyError,
        TypeError,
        json.JSONDecodeError,
        gt6_pipes.PipeSourceError,
    ) as error:
        print(f"T8 pipe readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
