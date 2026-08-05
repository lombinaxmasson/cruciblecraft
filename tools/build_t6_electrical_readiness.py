#!/usr/bin/env python3
"""Build the deterministic T5.5/T6 electrical-readiness ledger."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

try:
    from tools import gt6_electrical
except ModuleNotFoundError:
    import gt6_electrical


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
SOURCE = TOOLS / "gt6_electrical_source.json"
POLICY = TOOLS / "t6_electrical_policy.json"
NORMALIZED_MATERIALS = TOOLS / "gt6_oredict_materials_normalized.json"
NORMALIZED_PREFIXES = TOOLS / "gt6_oredict_prefixes_normalized.json"
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
MATERIAL_INDEX = MATERIAL_ROOT / "index.json"
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
COMPONENT_RULES = TOOLS / "component_rule_sources/component_rules.json"
COMPONENT_MANIFEST = TOOLS / "component_rule_manifest.json"
OUTPUT = TOOLS / "t6_electrical_readiness.json"
RUNTIME_SOURCE_GUARDS = {
    ROOT / "src/main/java/com/masson/cruciblecraft/energy/cable/"
    "ElectricalConductorCatalog.java": [
        "EXPECTED_CABLE_BLOCKS = 115",
        "EXPECTED_WIRE_BLOCKS = 29",
        "electricalBySpecification",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/content/block/"
    "CableBlock.java": [
        "PROPERTY_BY_DIRECTION",
        "SHAPES_BY_WIDTH",
        "VoxelShape[] shapes",
        "level.hasChunkAt",
        "entityInside",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/energy/cable/"
    "CableNetworkTraversal.java": [
        "ArrayDeque<Frame>",
        "Set<BlockPos> visited",
        "level.hasChunkAt",
        "applySegmentLoss",
        "loadWouldOverload",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/energy/cable/"
    "CableTransferPlan.java": [
        "terminal simulation",
        "terminal execution",
        "EnergyTransferDiagnostics.warnOnce",
        "loadSnapshot(level.getGameTime())",
        "Cable state changed before execution",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/energy/cable/"
    "CableLoadState.java": [
        "BURN_LIMIT = 16",
        "DECAY_INTERVAL = 512L",
        "burnAtTick = safeAdd(tick, 1L)",
        "nextDecayTick",
        "public Change advance",
        "observableChanged",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/content/blockentity/"
    "CableBlockEntity.java": [
        "Blocks.FIRE.defaultBlockState()",
        "CheckpointTracker",
        "SYNC_INTERVAL = 20",
        "invalidateSimulationCache",
        'tag.putInt("burn_counter"',
        'tag.putLong("burn_next_decay"',
        "wattageLast",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/energy/"
    "EnergyEmitter.java": [
        "consumer.invalidateSimulationCache()",
        "safeInsert",
        "safeExtract",
        "EnergyTransferDiagnostics.warnOnce",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/energy/cable/"
    "ElectricalShock.java": [
        "Registries.DAMAGE_TYPE",
        "GT6VoltageTiers.contactDamage",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/energy/cable/"
    "GT6VoltageTiers.java": [
        "8_589_934_592L",
        "tierMax",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/registry/"
    "ModBlocks.java": [
        "ElectricalConductorCatalog.initialize",
        "ELECTRICAL_CONDUCTOR_BLOCKS",
    ],
    ROOT / "src/main/java/com/masson/cruciblecraft/gametest/"
    "CrucibleCraftGameTests.java": [
        "dynamoFeedsElectrolyzerThroughThreeCables",
        "cableOverloadBurnsOnTheSafeFollowingTick",
        "onlyContactDamageBareWireShocksFromLastTick",
        "anyRubberTagExecutesCableRecipe",
    ],
    ROOT / "src/test/java/com/masson/cruciblecraft/energy/cable/"
    "CableLoadStateTest.java": [
        "sixteenthHitSchedulesObservableNextTickBurnout",
        "burnCounterAndDecayPhaseRoundTripWithoutUnloadReset",
        "segmentLossPreservesSignAndStopsAtTheExactCutoff",
    ],
}


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


def material_directory_hash(index: list[str]) -> str:
    rows = [
        (filename, sha256(MATERIAL_ROOT / filename))
        for filename in index
    ]
    return stable_hash(rows)


def build_runtime_implementation_review() -> dict[str, Any]:
    source_hashes = {}
    for path, required_tokens in RUNTIME_SOURCE_GUARDS.items():
        if not path.is_file():
            raise ValueError(
                "T6 runtime implementation source is missing: "
                f"{path.relative_to(ROOT).as_posix()}"
            )
        text = path.read_text(encoding="utf-8")
        missing = [
            token for token in required_tokens if token not in text
        ]
        if missing:
            raise ValueError(
                "T6 runtime implementation guard drifted for "
                f"{path.relative_to(ROOT).as_posix()}: {missing}"
            )
        source_hashes[path.relative_to(ROOT).as_posix()] = sha256(path)
    return {
        "status": "T6_RUNTIME_IMPLEMENTED_AND_REVIEWED",
        "source_sha256": dict(sorted(source_hashes.items())),
        "contracts": [
            "144_source_backed_conductor_blocks",
            "six_direction_compact_blockstate",
            "loaded_only_injection_local_stable_dfs",
            "signed_exact_per_segment_loss",
            "serial_revalidation_source_first_bounded_dissipation",
            "same_tick_post_loss_overload_propagation",
            "persistent_16_hit_512_tick_burn_state",
            "safe_following_tick_observable_fire",
            "source_flagged_last_tick_contact_damage",
            "data_driven_any_rubber_recipe_tag",
            "phased_observable_sync_and_cached_shapes",
        ],
    }


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


def source_classification(
    source_name: str,
    conductor_by_source: dict[str, dict[str, Any]],
    insulation: dict[str, Any],
) -> tuple[str, dict[str, Any]]:
    conductor = conductor_by_source.get(source_name)
    if conductor is not None:
        return "conductor", {
            "kind": "explicit_addElectricWires",
            "source_path": (
                "src/main/java/gregtech/loaders/b/"
                "Loader_MultiTileEntities.java"
            ),
            "source_symbol": conductor["source_symbol"],
        }
    if source_name in insulation["material_source_names"]:
        return "insulator", {
            "kind": "ANY.Rubber_concrete_member",
            "source_path": "src/main/java/gregapi/data/ANY.java",
        }
    if source_name == insulation["group_source_name"]:
        return "insulator_group", {
            "kind": "ANY.Rubber_technical_group",
            "source_path": "src/main/java/gregapi/data/ANY.java",
        }
    return "not_applicable", {
        "kind": "exact_direct_source_exclusion",
        "reason": (
            "Not named by addElectricWires or the direct ANY.Rubber "
            "insulation group; no name/tag/OreDict inference is used."
        ),
    }


def classify_raw(
    records: list[dict[str, Any]],
    conductor_by_source: dict[str, dict[str, Any]],
    specs_by_source: dict[str, dict[str, dict[str, Any]]],
    insulation: dict[str, Any],
) -> tuple[list[dict[str, Any]], Counter[str]]:
    rows = []
    counts: Counter[str] = Counter()
    for index, record in enumerate(records):
        source_name = str(record["source_name"])
        classification, evidence = source_classification(
            source_name, conductor_by_source, insulation
        )
        imported = record.get("electrical_by_specification") or {}
        expected = specs_by_source.get(source_name, {})
        if imported != expected:
            raise ValueError(
                f"{source_name}: normalized electrical specifications drifted"
            )
        row = {
            "source_record_index": index,
            "source_id": int(record["source_id"]),
            "source_name": source_name,
            "classification": classification,
            "evidence": evidence,
            "electrical_specification_count": len(imported),
        }
        if imported:
            row["electrical_by_specification"] = imported
        rows.append(row)
        counts[classification] += 1
    return rows, counts


def classify_live(
    documents: list[dict[str, Any]],
    conductor_by_source: dict[str, dict[str, Any]],
    specs_by_source: dict[str, dict[str, dict[str, Any]]],
    insulation: dict[str, Any],
) -> tuple[list[dict[str, Any]], Counter[str]]:
    rows = []
    counts: Counter[str] = Counter()
    for document in sorted(documents, key=lambda value: value["id"]):
        metadata = document.get("gt6_metadata") or {}
        source_name = str(metadata.get("source_name") or "")
        classification, evidence = source_classification(
            source_name, conductor_by_source, insulation
        )
        imported = metadata.get("electrical_by_specification") or {}
        expected = specs_by_source.get(source_name, {})
        if imported != expected:
            raise ValueError(
                f"{document['id']}: live electrical specifications drifted"
            )
        row = {
            "material": str(document["id"]),
            "source_id": int(metadata["source_id"]),
            "source_name": source_name,
            "classification": classification,
            "evidence": evidence,
            "electrical_specification_count": len(imported),
        }
        if imported:
            row["electrical_by_specification"] = imported
        rows.append(row)
        counts[classification] += 1
    return rows, counts


def validate_prefix_facts(
    normalized_prefixes: dict[str, Any],
    source: dict[str, Any],
) -> dict[str, Any]:
    by_name = {
        str(row["source_name"]): row
        for row in normalized_prefixes["records"]
    }
    names = [
        specification["name"]
        for kind in ("wire", "cable")
        for specification in source["specifications"][kind]
    ]
    missing = sorted(set(names) - set(by_name))
    if missing:
        raise ValueError(
            "normalized GT6 electrical prefixes are missing: "
            + ", ".join(missing)
        )
    return {
        "normalized_prefix_denominator": len(
            normalized_prefixes["records"]
        ),
        "authoritative_wire_specifications": len(
            source["specifications"]["wire"]
        ),
        "authoritative_cable_specifications": len(
            source["specifications"]["cable"]
        ),
        "selected_prefix_registered_material_counts": {
            name: len(by_name[name]["registered_materials"])
            for name in names
        },
        "interpretation": (
            "Registered-material domains quantify OreDict forms only. Direct "
            "electrical eligibility comes from addElectricWires."
        ),
    }


def build_acceptance(
    policy: dict[str, Any],
    live_by_id: dict[str, dict[str, Any]],
    specs_by_source: dict[str, dict[str, dict[str, Any]]],
) -> list[dict[str, Any]]:
    required_specs = policy["acceptance"]["required_specifications"]
    rows = []
    for material_id in policy["acceptance"]["required_live_conductors"]:
        document = live_by_id.get(material_id)
        if document is None:
            raise ValueError(
                f"acceptance conductor is not live: {material_id}"
            )
        metadata = document.get("gt6_metadata") or {}
        source_name = str(metadata.get("source_name") or "")
        actual = metadata.get("electrical_by_specification") or {}
        expected = specs_by_source.get(source_name)
        if not expected:
            raise ValueError(
                f"{material_id}: no direct conductor specifications"
            )
        missing = sorted(set(required_specs) - set(actual))
        if missing:
            raise ValueError(
                f"{material_id}: missing acceptance specifications {missing}"
            )
        rows.append({
            "material": material_id,
            "source_name": source_name,
            "classification": "accepted_direct_conductor",
            "required_specifications": {
                name: actual[name] for name in required_specs
            },
        })
    return rows


def build_cable_domain(
    policy: dict[str, Any],
    source: dict[str, Any],
    conductor_by_source: dict[str, dict[str, Any]],
    live_rows: list[dict[str, Any]],
    live_by_id: dict[str, dict[str, Any]],
    registration_gate: dict[str, list[str]],
    component_rules: dict[str, Any],
    component_manifest: dict[str, Any],
) -> dict[str, Any]:
    gauge_forms = policy["cable_domain"]["gauge_forms"]
    cc_forms = [row["cc_form"] for row in gauge_forms]
    source_specs = [
        row["source_specification"] for row in gauge_forms
    ]
    expected_source_specs = [
        row["name"] for row in source["specifications"]["cable"]
    ]
    if source_specs != expected_source_specs:
        raise ValueError("T6 gauge/source specification mapping drifted")

    cable_conductor_sources = {
        name
        for name, conductor in conductor_by_source.items()
        if conductor["cable_generated"]
    }
    live_conductor_ids = {
        row["source_name"]: row["material"]
        for row in live_rows
        if row["classification"] == "conductor"
    }
    missing_live_sources = sorted(
        cable_conductor_sources - set(live_conductor_ids)
    )
    if missing_live_sources:
        raise ValueError(
            "source cable conductor is absent from live catalog: "
            + ", ".join(missing_live_sources)
        )
    eligible_ids = sorted(
        live_conductor_ids[source_name]
        for source_name in cable_conductor_sources
    )

    live_materials_by_form = {
        form: sorted(
            material
            for material, forms in registration_gate.items()
            if form in forms
        )
        for form in cc_forms
    }
    source_backed_by_form = {
        form: sorted(set(materials) & set(eligible_ids))
        for form, materials in live_materials_by_form.items()
    }
    non_electric_by_form = {
        form: sorted(set(materials) - set(eligible_ids))
        for form, materials in live_materials_by_form.items()
    }
    missing_by_form = {
        form: sorted(set(eligible_ids) - set(materials))
        for form, materials in live_materials_by_form.items()
    }

    rules = [
        row for row in component_rules["rules"]
        if row.get("kind") == "cable"
    ]
    rule_by_form = {str(row["cable"]): row for row in rules}
    if set(rule_by_form) != set(cc_forms):
        raise ValueError("component cable-rule form coverage drifted")
    per_rule = component_manifest["per_rule_expanded"]
    recipe_expansions_by_form = {}
    for form, rule in sorted(rule_by_form.items()):
        count = int(per_rule[rule["id"]])
        if count != len(live_materials_by_form[form]):
            raise ValueError(
                f"{form}: live form/recipe expansion count mismatch"
            )
        recipe_expansions_by_form[form] = count

    connection_directions = int(
        policy["cable_domain"]["connection_directions"]
    )
    connection_masks = 2 ** connection_directions
    live_form_pairs = sum(
        len(materials) for materials in live_materials_by_form.values()
    )
    runtime_cable_blocks = sum(
        len(materials) for materials in source_backed_by_form.values()
    )
    expected_runtime_blocks = int(
        policy["cable_domain"]["runtime_source_backed_block_count"]
    )
    if runtime_cable_blocks != expected_runtime_blocks:
        raise ValueError(
            "source-backed runtime cable block count drifted: "
            f"{runtime_cable_blocks} != {expected_runtime_blocks}"
        )
    expected_registered_pairs = int(
        policy["cable_domain"]["registered_recipe_item_form_count"]
    )
    if live_form_pairs != expected_registered_pairs:
        raise ValueError(
            "registered cable recipe/item form count drifted: "
            f"{live_form_pairs} != {expected_registered_pairs}"
        )
    source_spec_by_form = {
        row["cc_form"]: row["source_specification"]
        for row in gauge_forms
    }
    runtime_block_catalog = []
    for form, materials in sorted(source_backed_by_form.items()):
        specification = source_spec_by_form[form]
        for material in materials:
            electrical = (
                live_by_id[material]["gt6_metadata"][
                    "electrical_by_specification"
                ][specification]
            )
            runtime_block_catalog.append({
                "material": material,
                "form": form,
                "source_specification": specification,
                "electrical": electrical,
            })
    source_potential_pairs = len(eligible_ids) * len(cc_forms)
    all_live_cross_product = len(live_by_id) * len(cc_forms)
    representation = policy["future_cc_policy_decisions"][
        "block_representation"
    ]
    return {
        "source_electric_wire_materials": sorted(
            live_conductor_ids.values()
        ),
        "source_electric_wire_material_count": len(live_conductor_ids),
        "source_electric_cable_materials": eligible_ids,
        "source_electric_cable_material_count": len(eligible_ids),
        "gauge_count": len(gauge_forms),
        "gauge_forms": gauge_forms,
        "live_materials_by_form": live_materials_by_form,
        "source_backed_live_materials_by_form": source_backed_by_form,
        "non_electric_live_materials_by_form": non_electric_by_form,
        "source_eligible_missing_live_form_by_form": missing_by_form,
        "live_form_pairs": live_form_pairs,
        "registered_recipe_item_form_count": live_form_pairs,
        "runtime_source_backed_block_count": runtime_cable_blocks,
        "runtime_block_catalog": runtime_block_catalog,
        "generic_material_rule_count": len(rules),
        "live_recipe_expansions_by_form": recipe_expansions_by_form,
        "live_recipe_expansion_count": sum(
            recipe_expansions_by_form.values()
        ),
        "block_representation_projection": {
            "connection_directions": connection_directions,
            "connection_masks_per_material_form": connection_masks,
            "current_live_material_specific_blocks": live_form_pairs,
            "current_live_logical_blockstates": (
                live_form_pairs * connection_masks
            ),
            "runtime_source_backed_material_specific_blocks": (
                runtime_cable_blocks
            ),
            "runtime_source_backed_logical_blockstates": (
                runtime_cable_blocks * connection_masks
            ),
            "all_source_eligible_material_specific_blocks": (
                source_potential_pairs
            ),
            "all_source_eligible_logical_blockstates": (
                source_potential_pairs * connection_masks
            ),
            "naive_all_live_material_specific_blocks": (
                all_live_cross_product
            ),
            "naive_all_live_logical_blockstates": (
                all_live_cross_product * connection_masks
            ),
            "status": (
                "SELECTED"
                if representation.get("status") == "CLOSED"
                and representation.get("selected") is not None
                else "QUANTIFIED_NOT_SELECTED"
            ),
            "selected": representation.get("selected"),
        },
    }


def build_wire_domain(
    policy: dict[str, Any],
    live_rows: list[dict[str, Any]],
    live_by_id: dict[str, dict[str, Any]],
    registration_gate: dict[str, list[str]],
) -> dict[str, Any]:
    specification = str(policy["wire_domain"]["source_specification"])
    form = str(policy["wire_domain"]["cc_form"])
    gate_registered = sorted(
        material
        for material, forms in registration_gate.items()
        if form in forms
    )
    conductor_ids = {
        row["material"]
        for row in live_rows
        if row["classification"] == "conductor"
    }
    source_backed = []
    catalog = []
    for material in gate_registered:
        if material not in conductor_ids:
            continue
        electrical_by_specification = (
            live_by_id[material]["gt6_metadata"][
                "electrical_by_specification"
            ]
        )
        electrical = electrical_by_specification.get(specification)
        if electrical is None:
            raise ValueError(
                f"{material}: source conductor lacks {specification}"
            )
        source_backed.append(material)
        catalog.append({
            "material": material,
            "form": form,
            "source_specification": specification,
            "electrical": electrical,
        })
    expected = int(
        policy["wire_domain"]["runtime_source_backed_block_count"]
    )
    if len(source_backed) != expected:
        raise ValueError(
            "source-backed runtime wire block count drifted: "
            f"{len(source_backed)} != {expected}"
        )
    return {
        "source_specification": specification,
        "cc_form": form,
        "gate_registered_material_count": len(gate_registered),
        "source_backed_live_materials": source_backed,
        "runtime_source_backed_block_count": len(source_backed),
        "runtime_block_catalog": catalog,
        "deferred_source_specifications": policy["wire_domain"][
            "deferred_source_specifications"
        ],
    }


def build() -> dict[str, Any]:
    source = gt6_electrical.load(SOURCE)
    policy = load(POLICY)
    normalized_materials = load(NORMALIZED_MATERIALS)
    normalized_prefixes = load(NORMALIZED_PREFIXES)
    index = load(MATERIAL_INDEX)
    live_documents, live_by_id = load_live_materials(index)
    registration_gate_document = load(REGISTRATION_GATE)
    registration_gate = registration_gate_document["materials"]
    component_rules = load(COMPONENT_RULES)
    component_manifest = load(COMPONENT_MANIFEST)

    if policy.get("schema_version") != 1:
        raise ValueError("T6 electrical policy schema_version must be 1")
    if set(registration_gate) != set(live_by_id):
        raise ValueError(
            "registration gate does not cover the exact live material catalog"
        )
    conductor_by_source = {
        row["source_name"]: row for row in source["conductors"]
    }
    specs_by_source = gt6_electrical.specifications_by_source(source)
    raw_rows, raw_counts = classify_raw(
        normalized_materials["records"],
        conductor_by_source,
        specs_by_source,
        source["insulation"],
    )
    live_rows, live_counts = classify_live(
        live_documents,
        conductor_by_source,
        specs_by_source,
        source["insulation"],
    )
    raw_unclassified = len(raw_rows) - sum(raw_counts.values())
    live_unclassified = len(live_rows) - sum(live_counts.values())
    if raw_unclassified or live_unclassified:
        raise ValueError("electrical classification closure is incomplete")

    prefix_facts = validate_prefix_facts(normalized_prefixes, source)
    acceptance = build_acceptance(policy, live_by_id, specs_by_source)
    cable_domain = build_cable_domain(
        policy,
        source,
        conductor_by_source,
        live_rows,
        live_by_id,
        registration_gate,
        component_rules,
        component_manifest,
    )
    wire_domain = build_wire_domain(
        policy,
        live_rows,
        live_by_id,
        registration_gate,
    )
    runtime_block_count = (
        cable_domain["runtime_source_backed_block_count"]
        + wire_domain["runtime_source_backed_block_count"]
    )
    runtime_logical_states = runtime_block_count * (
        2 ** int(policy["cable_domain"]["connection_directions"])
    )
    expected_runtime = policy["runtime_domain"]
    if runtime_block_count != int(
        expected_runtime["conductor_block_count"]
    ):
        raise ValueError("combined runtime conductor block count drifted")
    if runtime_logical_states != int(
        expected_runtime["logical_blockstate_count"]
    ):
        raise ValueError("combined runtime conductor blockstate count drifted")
    runtime_domain = {
        **expected_runtime,
        "conductor_block_count": runtime_block_count,
        "logical_blockstate_count": runtime_logical_states,
        "cable_block_count": cable_domain[
            "runtime_source_backed_block_count"
        ],
        "wire_block_count": wire_domain[
            "runtime_source_backed_block_count"
        ],
    }
    runtime_implementation = build_runtime_implementation_review()

    decisions = policy["future_cc_policy_decisions"]
    open_decisions = sorted(
        name
        for name, decision in decisions.items()
        if decision.get("status") != "CLOSED"
        or decision.get("selected") is None
    )
    direct_source_blockers: list[str] = []
    policy_runtime_blockers = [
        *(f"CC_POLICY_OPEN:{name}" for name in open_decisions),
    ]
    blockers = direct_source_blockers + policy_runtime_blockers
    status = (
        policy["readiness_policy"]["ready_status"]
        if not blockers
        else policy["readiness_policy"]["blocked_status"]
    )

    input_paths = [
        SOURCE,
        POLICY,
        NORMALIZED_MATERIALS,
        NORMALIZED_PREFIXES,
        MATERIAL_INDEX,
        REGISTRATION_GATE,
        COMPONENT_RULES,
        COMPONENT_MANIFEST,
        *RUNTIME_SOURCE_GUARDS.keys(),
    ]
    return {
        "schema_version": 1,
        "gate": policy["gate"],
        "status": status,
        "generated_by": "tools/build_t6_electrical_readiness.py",
        "input_sha256": {
            path.relative_to(ROOT).as_posix(): sha256(path)
            for path in input_paths
        },
        "material_directory_sha256": material_directory_hash(index),
        "gt6_source": source["source"],
        "corrected_semantic_model": source["semantic_model"],
        "source_fact_status": "SOURCE_FACTS_READY",
        "classification_policy": policy["classification_policy"],
        "import_architecture": policy["import_architecture"],
        "counts": {
            "raw_source_materials": {
                "denominator": len(raw_rows),
                "classifications": dict(sorted(raw_counts.items())),
                "unclassified": raw_unclassified,
            },
            "live_materials": {
                "denominator": len(live_rows),
                "classifications": dict(sorted(live_counts.items())),
                "unclassified": live_unclassified,
            },
        },
        "prefix_facts": prefix_facts,
        "acceptance_conductors": acceptance,
        "cable_domain": cable_domain,
        "wire_domain": wire_domain,
        "runtime_domain": runtime_domain,
        "runtime_implementation": runtime_implementation,
        "future_cc_policy_decisions": decisions,
        "readiness": {
            "status": status,
            "direct_source_blockers": direct_source_blockers,
            "policy_runtime_blockers": policy_runtime_blockers,
            "blockers": blockers,
            "criteria": {
                "authoritative_source_revision_and_hashes_match": True,
                "classification_closure": True,
                "direct_conductor_specifications_imported": True,
                "acceptance_conductors_have_required_specs": True,
                "invented_physical_resistance_count": 0,
                "future_cc_policy_decisions_closed": not open_decisions,
                "runtime_architecture_reviewed": not open_decisions,
                "runtime_cable_implementation_reviewed": True,
            },
            "requirements": policy["readiness_policy"]["requirements"],
        },
        "raw_source_materials": raw_rows,
        "live_materials": live_rows,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T6 electrical ledger is stale",
    )
    parser.add_argument(
        "--source-root",
        type=Path,
        help=(
            "optional pinned GT6 checkout; verify authoritative source hashes"
        ),
    )
    args = parser.parse_args()
    try:
        source = gt6_electrical.load(SOURCE)
        if args.source_root is not None:
            gt6_electrical.validate_source_root(
                source, args.source_root.resolve()
            )
        encoded = stable_json(build())
        if args.check:
            if not OUTPUT.is_file():
                raise ValueError(f"missing committed ledger: {OUTPUT}")
            if OUTPUT.read_text(encoding="utf-8") != encoded:
                raise ValueError(
                    "committed T6 electrical readiness ledger is stale"
                )
        else:
            OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
        print(json.dumps({
            "output": OUTPUT.relative_to(ROOT).as_posix(),
            "status": json.loads(encoded)["status"],
        }, sort_keys=True))
        return 0
    except (
        OSError,
        ValueError,
        KeyError,
        TypeError,
        json.JSONDecodeError,
        gt6_electrical.ElectricalSourceError,
    ) as error:
        print(f"T6 electrical readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
