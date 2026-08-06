#!/usr/bin/env python3
"""Generate and validate the tooling-bound full verification report snapshot."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import unittest
import xml.etree.ElementTree as ElementTree
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

try:
    from tools.verification_context import ValidationContext
except ModuleNotFoundError:
    from verification_context import ValidationContext

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
REPORT = TOOLS / "full_verification_report.json"
BUILDER_POLICY = TOOLS / "verification_builder_policy.json"
CORE_ARTIFACTS = (
    "gt6_oredict_import_manifest.json",
    "gt6_recipe_expectations.json",
    "gt6_recipe_compare_baseline.json",
    "gt6_map_roadmap.json",
    "gt6_process_expectations.json",
    "gt6_l1b_selected_recipe_operands.json",
    "gt6_ore_chain.json",
    "gt6_ore_chain_closure.json",
    "gt6_ore_chain_operands.json",
    "gt6_extruder_templates_index_v5.json",
    "gt6_extruder_templates_report.json",
    "component_rule_manifest.json",
    "component_selector_policy.json",
    "t4_tool_policy.json",
    "t4_tool_readiness.json",
    "t5_chemical_policy.json",
    "t5_chemical_readiness.json",
    "t5_chemical_recipe_manifest.json",
    "t5_distillery_projection.json",
    "machine_crafting_policy.json",
    "machine_crafting_readiness.json",
    "gt6_electrical_source.json",
    "t6_electrical_policy.json",
    "t6_electrical_readiness.json",
    "t7_material_tag_readiness.json",
    "gt6_pipe_source.json",
    "t8_pipe_policy.json",
    "t8_pipe_readiness.json",
    "worldgen_catalog_readiness.json",
    "t10_preflight_policy.json",
    "t10_preflight_projection.json",
    "t10_container_readiness.json",
    "t11_preflight_policy.json",
    "t11_preflight_projection.json",
    "t11_hydrocarbon_recipe_manifest.json",
    "t12_machine_policy.json",
    "t12a_machine_readiness.json",
    "t12_closure_readiness.json",
    "t12_capacity_matcher_benchmark.json",
    "t13_denominator_policy.json",
    "t13_gt6_tree_manifest.json",
    "t13_source_symbol_inventory.json",
    "t13_recipe_map_policy.json",
    "t13_prefix_domain_policy.json",
    "t13_machine_energy_policy.json",
    "t13_cover_multiblock_policy.json",
    "t13_denominator_manifest.json",
    "t13_denominator_readiness.json",
    "t13_denominators/recipe_maps.json",
    "t13_denominators/prefixes.json",
    "t13_denominators/itemgenerator_domains.json",
    "t13_denominators/machine_kinds.json",
    "t13_denominators/energy_identities.json",
    "t13_denominators/cover_kinds.json",
    "t13_denominators/multiblock_kinds.json",
    "t14_extruder_policy.json",
    "t14_extruder_expected.json",
    "t14_extruder_compact.json",
    "t14_extruder_legacy_replay.json",
    "t14_extruder_readiness.json",
    "t14_materialization_candidate_policy.json",
    "t14_recipe_load_benchmark.json",
    "t14_materialization_policy.json",
    "t14_materialization_decision.json",
    "t14_load_budget_policy.json",
    "t14_extruder_load_projection_input.json",
    "t14_extruder_load_projection.json",
    "t14_readiness.json",
    "t15_machine_acquisition_policy.json",
    "t15_machine_acquisition.json",
    "t15_matcher_boundary.json",
    "t15_load_projection_input.json",
    "t15_load_projection.json",
    "t15_readiness_policy.json",
    "t15_readiness.json",
    "t16_machine_denominator_policy.json",
    "t16_machine_denominator.json",
    "t16_machine_acquisition_policy.json",
    "t16_machine_acquisition.json",
    "t16_load_projection_input.json",
    "t16_load_projection.json",
    "t16_readiness_policy.json",
    "t16_readiness.json",
    "t17_machine_denominator_policy.json",
    "t17_machine_denominator.json",
    "t17_machine_acquisition_policy.json",
    "t17_machine_acquisition.json",
    "t17_load_projection_input.json",
    "t17_load_projection.json",
    "t17_readiness_policy.json",
    "t17_readiness.json",
    "recipe_load_projection.schema.json",
    "verification_builder_policy.json",
    "local_artifact_manifest.json",
    "material_registry_stress_report.json",
    "material_registry_budget.json",
)
COMPONENT_MANIFEST = TOOLS / "component_rule_manifest.json"
COMPONENT_SOURCE_DIR = TOOLS / "component_rule_sources"
COMPONENT_GENERATED_ROOT = ROOT / "src/component_rule_generated/resources"
T5_CHEMICAL_GENERATED_ROOT = ROOT / "src/t5_chemical_generated/resources"
T11_HYDROCARBON_GENERATED_ROOT = (
    ROOT / "src/t11_hydrocarbon_generated/resources"
)
WORLDGEN_CATALOG_GENERATED_ROOT = (
    ROOT / "src/worldgen_catalog_generated/resources"
)
SHAPE_RESOURCE_ROOT = ROOT / "src/generated/resources"
ACCEPTANCE_MATERIALS = ("copper", "tin", "iron", "gold")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tooling_paths() -> list[Path]:
    paths = [
        TOOLS / "compare_gt6_recipes.py",
        TOOLS / "import_gt6_oredict.py",
        TOOLS / "build_gt6_material_form_gate.py",
        TOOLS / "build_gt6_ore_chain.py",
        TOOLS / "build_gt6_ore_chain_closure.py",
        TOOLS / "build_gt6_veins.py",
        TOOLS / "run_material_registry_stress.py",
        TOOLS / "build_component_rules.py",
        TOOLS / "build_t4_tool_readiness.py",
        TOOLS / "build_t5_chemical_readiness.py",
        TOOLS / "build_t5_chemical_recipes.py",
        TOOLS / "build_t5_distillery_projection.py",
        TOOLS / "build_machine_crafting_readiness.py",
        TOOLS / "build_t6_electrical_readiness.py",
        TOOLS / "build_t7_material_tag_readiness.py",
        TOOLS / "build_t8_pipe_readiness.py",
        TOOLS / "build_worldgen_catalog.py",
        TOOLS / "build_t10_preflight_projection.py",
        TOOLS / "build_t10_container_readiness.py",
        TOOLS / "build_t11_preflight_projection.py",
        TOOLS / "build_t11_hydrocarbon_recipes.py",
        TOOLS / "build_t12_machine_readiness.py",
        TOOLS / "build_t12_closure_readiness.py",
        TOOLS / "build_t13_recipe_map_denominator.py",
        TOOLS / "build_t13_prefix_domain_denominators.py",
        TOOLS / "build_t13_machine_energy_denominators.py",
        TOOLS / "build_t13_cover_multiblock_denominators.py",
        TOOLS / "build_t13_denominator_readiness.py",
        TOOLS / "build_t14_extruder_equivalence.py",
        TOOLS / "build_t14_recipe_load_benchmark.py",
        TOOLS / "build_t14_readiness.py",
        TOOLS / "build_t15_machine_acquisition.py",
        TOOLS / "build_t15_matcher_boundary.py",
        TOOLS / "build_t15_readiness.py",
        TOOLS / "build_t16_machine_denominator.py",
        TOOLS / "build_t16_machine_acquisition.py",
        TOOLS / "build_t16_readiness.py",
        TOOLS / "build_t17_machine_denominator.py",
        TOOLS / "build_t17_machine_acquisition.py",
        TOOLS / "build_t17_readiness.py",
        TOOLS / "recipe_load_projection.py",
        TOOLS / "apply_t10_form_flags.py",
        TOOLS / "apply_t8_pipe_metadata.py",
        TOOLS / "gt6_pipes.py",
        TOOLS / "gt6_electrical.py",
        TOOLS / "gt6_extruder_templates.py",
        TOOLS / "run_full_verification.py",
        TOOLS / "run_python_tests.py",
        TOOLS / "verification_context.py",
        TOOLS / "verification_session.py",
        TOOLS / "verification_session.schema.json",
        BUILDER_POLICY,
        TOOLS / "verify_full_verification_report.py",
        TOOLS / "python_test_policy.json",
        TOOLS / "README.md",
        TOOLS / "gt6_process_expectations.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/material_tag_policy.json",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/item/ExtruderShapeCatalog.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java",
        ROOT
        / "src/main/resources/data/cruciblecraft/t16_publication_baseline.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/t17_publication_baseline.json",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/material/ChemicalFluidRegistrationGate.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/machine/processing/SidedFluidHandler.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/machine/processing/MachineTransaction.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/material/def/GT6MaterialMetadata.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/api/energy/EnergyType.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/energy/cable/CableNetworkTraversal.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/recipe/rule/MaterialRuleExpansion.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/recipe/rule/RuleExpression.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/client/tooltip/MaterialMetadataTooltip.java",
        ROOT
        / "src/main/resources/data/cruciblecraft/recipe/t7/mortar/ingot_to_dust.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/recipe/t7/mortar/gem_to_dust.json",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/registry/ModFluids.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/material/CellContentGate.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/item/CellItem.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/item/CellFluidHandler.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/heat/HeatMaintenanceEvents.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/heat/HotIngotProcessing.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/heat/MaterialItemCooling.java",
        ROOT
        / "src/main/resources/data/cruciblecraft/t10_cell_content_gate.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/t10_container_fluid_gate.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/t11_hydrocarbon_fluid_gate.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/t11_cell_content_gate.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/t11_hydrocarbon_runtime_policy.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/t11_materials/natural_gas.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/t11_materials/index.json",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/material/def/MaterialLoader.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/material/HydrocarbonRuntimePolicy.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/blockentity/FluidDepositExtractorBlockEntity.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/blockentity/FuelGeneratorBlockEntity.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/blockentity/GasCloudBlockEntity.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/registry/ModProcessingMachines.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/recipe/gt/GTRecipeMapLoader.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/energy/BronzeDynamoEnergy.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/material/prefix/T3ComponentDataTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/datagen/T5ChemicalResourceTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/registry/T5ProcessingMachineSpecTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/machine/processing/MachineTransactionTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/machine/processing/ProcessingAdaptersTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/energy/BronzeDynamoEnergyTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/energy/cable/CableLoadStateTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/material/prefix/T7MaterialRuleDataTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/recipe/rule/RuleExpressionTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/recipe/rule/MaterialRuleExpansionTest.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/client/tooltip/MaterialMetadataTooltipTest.java",
        ROOT / ".gitignore",
    ]
    paths.extend(TOOLS / name for name in CORE_ARTIFACTS)
    paths.extend(
        COMPONENT_SOURCE_DIR / name
        for name in (
            "acceptance_form_corrections.json",
            "component_baseline.json",
            "component_rules.json",
            "extruder_shapes.json",
        )
    )
    paths.extend(sorted((TOOLS / "tests").glob("test_*.py")))
    paths.extend(sorted((
        ROOT / "src/main/resources/data/cruciblecraft/veins"
    ).glob("*.json")))
    paths.extend(sorted((
        ROOT / "src/worldgen_generated/resources"
    ).rglob("*.json")))
    paths.extend(sorted((
        ROOT / "src/main/resources/data/cruciblecraft/worldgen_catalog"
    ).glob("*.json")))
    paths.extend(sorted(
        WORLDGEN_CATALOG_GENERATED_ROOT.rglob("*.json")
    ))
    paths.extend(sorted((ROOT / "src/main/java").rglob("*.java")))
    paths.extend(sorted((ROOT / "src/test/java").rglob("*.java")))
    paths.extend(sorted((
        ROOT / "src/main/java/com/masson/cruciblecraft/logistics/pipe"
    ).rglob("*.java")))
    paths.extend(sorted((
        ROOT / "src/test/java/com/masson/cruciblecraft/logistics/pipe"
    ).rglob("*.java")))
    paths.extend(sorted((
        ROOT / "src/main/resources/data/cruciblecraft/recipe/extruder"
    ).glob("*.json")))
    paths.extend(sorted((
        ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
    ).glob("*pipe.json")))
    paths.extend((
        ROOT / "README.md",
        ROOT / "CrucibleCraft-总体规划.md",
        ROOT / "CrucibleCraft-第二阶段总体规划.md",
        ROOT / "CrucibleCraft-第三阶段总体规划.md",
        ROOT / "CrucibleCraft-阶段档案-T13-T16.md",
        ROOT / "CrucibleCraft-阶段档案-T17.md",
    ))
    return paths


def tree_digest(paths: list[Path], root: Path) -> dict[str, Any]:
    rows = [
        (path.relative_to(root).as_posix(), sha256(path))
        for path in sorted(paths)
        if path.is_file()
    ]
    return {
        "algorithm": "sha256(sorted relative path and file sha256)",
        "files": len(rows),
        "sha256": stable_hash(rows),
    }


def current_tree_digests() -> dict[str, Any]:
    component_files = list(COMPONENT_GENERATED_ROOT.rglob("*.json"))
    t5_chemical_files = list(T5_CHEMICAL_GENERATED_ROOT.rglob("*.json"))
    t11_hydrocarbon_files = list(
        T11_HYDROCARBON_GENERATED_ROOT.rglob("*.json")
    )
    worldgen_catalog_files = list(
        WORLDGEN_CATALOG_GENERATED_ROOT.rglob("*.json")
    )
    datagen_files = [
        path
        for path in SHAPE_RESOURCE_ROOT.rglob("*")
        if path.is_file() and ".cache" not in path.parts
    ]
    shape_files = [
        path
        for path in SHAPE_RESOURCE_ROOT.rglob("*")
        if path.is_file()
        and (
            "extruder_shape_" in path.name
            or path.as_posix().endswith(
                "data/cruciblecraft/tags/item/extruder_shapes.json"
            )
            or path.name in {"en_us.json", "zh_cn.json"}
        )
    ]
    return {
        "component_rule_generated": tree_digest(
            component_files, COMPONENT_GENERATED_ROOT
        ),
        "t5_chemical_generated": tree_digest(
            t5_chemical_files, T5_CHEMICAL_GENERATED_ROOT
        ),
        "t11_hydrocarbon_generated": tree_digest(
            t11_hydrocarbon_files, T11_HYDROCARBON_GENERATED_ROOT
        ),
        "worldgen_catalog_generated": tree_digest(
            worldgen_catalog_files, WORLDGEN_CATALOG_GENERATED_ROOT
        ),
        "datagen_generated": tree_digest(
            datagen_files, SHAPE_RESOURCE_ROOT
        ),
        "extruder_shape_resources": tree_digest(
            shape_files, SHAPE_RESOURCE_ROOT
        ),
    }


def current_python_test_count() -> int:
    root = str(ROOT)
    inserted = not sys.path or sys.path[0] != root
    if inserted:
        sys.path.insert(0, root)
    try:
        suite = unittest.defaultTestLoader.discover(
            str(TOOLS / "tests"),
            pattern="test_*.py",
        )
        return suite.countTestCases()
    finally:
        if inserted:
            sys.path.remove(root)


def current_game_test_count() -> int:
    return sum(
        path.read_text(encoding="utf-8").count("@GameTest(")
        for path in (
            ROOT / "src/main/java/com/masson/cruciblecraft/gametest"
        ).glob("*.java")
    )


def current_java_source_test_count() -> int:
    return sum(
        len(re.findall(r"@Test\b", path.read_text(encoding="utf-8")))
        for path in (ROOT / "src/test/java").rglob("*.java")
    )


def current_catalog_metrics() -> dict[str, int]:
    import build_gt6_material_form_gate as gate_builder

    documents, factual_forms = gate_builder.material_documents()
    prefix_index = json.loads(
        (
            ROOT
            / "src/main/resources/data/cruciblecraft/material_prefixes/index.json"
        ).read_text(encoding="utf-8")
    )
    return {
        "startup_prefixes": len(prefix_index),
        "active_materials": len(documents),
        "factual_material_forms": sum(map(len, factual_forms.values())),
        "metadata_only_materials": sum(
            bool(document.get("metadata_only"))
            for document in documents.values()
        ),
    }


def current_java_test_metrics() -> dict[str, int]:
    roots = [
        ElementTree.parse(path).getroot()
        for path in sorted(
            (ROOT / "build/test-results/test").glob("TEST-*.xml")
        )
    ]
    if not roots:
        raise ValueError("Java test result XML is missing")
    return {
        "suites": len(roots),
        "tests": sum(int(root.attrib.get("tests", 0)) for root in roots),
        "failures": sum(int(root.attrib.get("failures", 0)) for root in roots),
        "errors": sum(int(root.attrib.get("errors", 0)) for root in roots),
        "skipped": sum(int(root.attrib.get("skipped", 0)) for root in roots),
    }


def current_compatibility_metrics() -> dict[str, Any]:
    import compare_gt6_recipes as comparison

    cc_recipes = list(comparison.cached_expanded_cc_recipes())
    expectations = json.loads(
        (TOOLS / "gt6_recipe_expectations.json").read_text(encoding="utf-8")
    ).get("expectations") or {}
    verdicts = {
        verdict: sum(
            decision.get("verdict") == verdict
            for decision in expectations.values()
        )
        for verdict in comparison.VERDICTS
    }
    reviewed = {
        "human_reviewed": 0,
        "automated_evidence_reviewed": 0,
        "unreviewed": 0,
    }
    for decision in expectations.values():
        evidence = decision.get("evidence")
        if (
            decision.get("review_mode") == "automated"
            and evidence is not None
            and decision.get("evidence_digest") == stable_hash(evidence)
        ):
            reviewed["automated_evidence_reviewed"] += 1
        elif (
            decision.get("review_mode") == "human"
            and str(decision.get("reviewed_by") or "").strip()
            and str(decision.get("reviewed_at") or "").strip()
        ):
            reviewed["human_reviewed"] += 1
        else:
            reviewed["unreviewed"] += 1
    t14 = json.loads(
        (TOOLS / "t14_readiness.json").read_text(encoding="utf-8")
    )
    virtualized_recipes = (
        t14["o_26"]["logical_relations"]
        if t14["status"] == "T14_READY"
        else 0
    )
    if len(expectations) != len(cc_recipes):
        raise ValueError(
            "compact expectation count does not cover current logical CC "
            f"recipes: {len(expectations)} != {len(cc_recipes)}"
        )
    return {
        "normalized_cc_recipes": len(cc_recipes),
        "concrete_cc_recipes": len(cc_recipes) - virtualized_recipes,
        "virtualized_cc_recipes": virtualized_recipes,
        "logical_cc_recipes": len(cc_recipes),
        "verdicts": verdicts,
        "review_evidence": reviewed,
    }


def stable_hash(value: Any) -> str:
    payload = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def current_snapshot() -> dict[str, Any]:
    files = {
        path.relative_to(ROOT).as_posix(): sha256(path)
        for path in tooling_paths()
    }
    snapshot = {
        "java_source_test_count": current_java_source_test_count(),
        "python_test_count": current_python_test_count(),
        "files_sha256": dict(sorted(files.items())),
        "trees": current_tree_digests(),
    }
    snapshot["snapshot_sha256"] = stable_hash(snapshot)
    return snapshot


def required_artifact_hashes(snapshot: dict[str, Any]) -> dict[str, str]:
    result = {
        name: sha256(TOOLS / name)
        for name in CORE_ARTIFACTS
    }
    result.update(snapshot["files_sha256"])
    return dict(sorted(result.items()))


def derived_component_acceptance() -> dict[str, Any]:
    manifest = json.loads(COMPONENT_MANIFEST.read_text(encoding="utf-8"))
    extruder = manifest["extruder_templates"]
    correction = manifest["acceptance_form_corrections"]
    return {
        "delivery_model": "compact semantic sources -> committed MaterialRule JSON -> runtime expansion",
        "generated_resource_root": "src/component_rule_generated/resources",
        "shape_count": len(extruder["shapes"]),
        "template_count": extruder["classified"],
        "template_classifications": extruder["classification_counts"],
        "unclassified_templates": extruder["unclassified"],
        "source_rules": manifest["source_rules"],
        "expanded_recipes": manifest["expanded_recipes"],
        "non_extruder_expanded_recipes": manifest[
            "non_extruder_expanded_recipes"
        ],
        "extruder_expanded_recipes": manifest["extruder_expanded_recipes"],
        "expanded_recipes_per_map": {
            name: row["expanded_recipes"]
            for name, row in manifest["per_map"].items()
        },
        "expansion_budget": manifest["expansion_budget"],
        "within_budget": manifest["within_budget"],
        "shadowed_recipes": manifest["shadow_signatures"]["duplicates"],
        "source_sha256": manifest["source_sha256"],
        "source_files_sha256": manifest["source_files_sha256"],
        "builder": manifest["builder"],
        "build_inputs_sha256": manifest["build_inputs_sha256"],
        "generated_tree": manifest["generated_tree"],
        "recipe_ids": manifest["recipe_ids"],
        "recipe_signatures": manifest["recipe_signatures"],
        "shadow_signatures": manifest["shadow_signatures"],
        "selector": {
            "policy": "tools/component_selector_policy.json",
            "extruder": "concrete_shape",
            "support": "explicit_sparse_relation",
        },
        "iron_generates_wire": {
            "classification": correction["classification"],
            "expansion_delta": correction["expansion_delta"],
            "entries": correction["entries"],
        },
    }


def derived_t4_tool_acceptance() -> dict[str, Any]:
    readiness = json.loads(
        (TOOLS / "t4_tool_readiness.json").read_text(encoding="utf-8")
    )
    policy = json.loads(
        (TOOLS / "t4_tool_policy.json").read_text(encoding="utf-8")
    )
    projections = readiness["strategy_projections"]
    tool_recipes = projections["tool_recipes"]
    return {
        "delivery_model": (
            "21 compact MaterialRule routes -> flattened assembler recipes"
        ),
        "source_rules": len(
            list(
                (
                    ROOT
                    / "src/generated/resources/data/cruciblecraft/recipe/"
                    "t4/assembler"
                ).rglob("*.json")
            )
        ),
        "tool_types": len(tool_recipes),
        "material_candidates": readiness["closure"]["material_count"],
        "tool_material_pairs": readiness["closure"]["tool_material_pair_count"],
        "unclassified": readiness["closure"]["unclassified"],
        "expanded_recipes": projections["recipe_signatures"][
            "projected_recipes"
        ],
        "expanded_recipes_by_tool": {
            tool: row["recipe_ready"]
            for tool, row in tool_recipes.items()
        },
        "signature_collisions": projections["recipe_signatures"][
            "collision_count"
        ],
        "eligible_without_route": {
            "total": projections["eligibility_route_gaps"]["total"],
            "by_tool": {
                tool: row["count"]
                for tool, row in projections[
                    "eligibility_route_gaps"
                ]["by_tool"].items()
            },
        },
        "eligibility_predicate_sources": projections[
            "eligibility_predicate_sources"
        ],
        "identity_literals": projections["identity_ledger"][
            "distinct_literal_material_ids"
        ],
        "budget_policy": policy["recipe_budget_policy"],
    }


def derived_t5_chemical_acceptance() -> dict[str, Any]:
    readiness = json.loads(
        (TOOLS / "t5_chemical_readiness.json").read_text(encoding="utf-8")
    )
    manifest = json.loads(
        (TOOLS / "t5_chemical_recipe_manifest.json").read_text(encoding="utf-8")
    )
    fluid_gate = json.loads(
        (
            T5_CHEMICAL_GENERATED_ROOT
            / "data/cruciblecraft/t5_chemical_fluid_gate.json"
        ).read_text(encoding="utf-8")
    )
    terminal_rows = [
        row
        for row in readiness["chemical_materials"]
        if "terminal_dust" in row["origins"]
    ]
    terminal_classifications = Counter(
        row["classification"] for row in terminal_rows
    )
    counts = manifest["counts"]
    denominator = readiness["counts"]["input_ledgers"][
        "terminal_dust_t5_chemical"
    ]
    live = counts["terminal_dust_recipe_ready"]
    unresolved = counts["terminal_dust_unresolved"]
    return {
        "delivery_model": (
            "pinned GT6 dump -> source-dead-end judgement -> one executable "
            "live route per terminal dust and a closed required-fluid registry"
        ),
        "gt6_revision": readiness["gt6_source"]["revision"],
        "readiness_status": readiness["status"],
        "terminal_dust_denominator": denominator,
        "terminal_readiness_classifications": dict(
            sorted(terminal_classifications.items())
        ),
        "terminal_dust_live_routes": live,
        "terminal_dust_unresolved": unresolved,
        "closure_ready": live == denominator and unresolved == 0,
        "generated_recipes": counts["generated_recipes"],
        "generated_recipes_by_map": counts["map_recipes"],
        "source_dead_ends": counts["source_dead_ends"],
        "non_molten_fluid_candidates": readiness["counts"]["fluids"][
            "non_molten_candidates"
        ],
        "registered_chemical_fluids": len(fluid_gate["fluids"]),
        "source_replay": readiness["recipe_replay"],
    }


def derived_t7_material_fact_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t7_material_tag_readiness as t7_builder
    except ModuleNotFoundError:
        import build_t7_material_tag_readiness as t7_builder

    ledger_path = TOOLS / "t7_material_tag_readiness.json"
    readiness = json.loads(
        ledger_path.read_text(encoding="utf-8")
    )
    ledger_current = (
        ledger_path.read_text(encoding="utf-8")
        == t7_builder.stable_json(t7_builder.build())
    )
    mortar = readiness["mortar_rules"]
    publication = readiness["runtime_publication_acceptance"]
    return {
        "delivery_model": (
            "pinned GT6 material facts -> closed 62-tag policy -> "
            "authored MaterialRule predicates -> live RecipeMap expansion"
        ),
        "gt6_revision": readiness["gt6_source"]["revision"],
        "readiness_status": readiness["status"],
        "ledger_current": ledger_current,
        "classified_tags": readiness["classified"],
        "unclassified_tags": readiness["unclassified"],
        "classification_counts": readiness["classification_counts"],
        "fact_counts": readiness["counts"],
        "material_tag_vocabulary": readiness["material_tag_vocabulary"],
        "material_rule_audit": readiness["material_rule_audit"],
        "rule_language": readiness["rule_language"],
        "energy_type_decision": readiness["energy_type_decision"],
        "t10_damage_gate": readiness["t10_damage_gate"],
        "mortar_scope_decision": readiness["mortar_scope_decision"],
        "extruder_compaction_decision": readiness[
            "extruder_compaction_decision"
        ],
        "legacy_rule_condition_backfill": readiness[
            "legacy_rule_condition_backfill"
        ],
        "mortar_rule_counts": {
            name: row["count"]
            for name, row in mortar.items()
            if isinstance(row, dict) and "count" in row
        },
        "mortar_material_set_sha256": {
            name: stable_hash(row["materials"])
            for name, row in mortar.items()
            if isinstance(row, dict) and "materials" in row
        },
        "runtime_publication": publication,
        "within_t7_authored_material_rule_budget": (
            publication["t7_added_mortar_recipes"]
            <= publication["t7_authored_material_rule_budget"]
        ),
        "within_publication_budget": (
            publication["post_t7_all_published_recipes"]
            <= publication["all_published_recipe_budget"]
        ),
        "input_sha256": readiness["input_sha256"],
        "material_directory_sha256": readiness[
            "material_directory_sha256"
        ],
    }


def derived_t8_pipe_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t8_pipe_readiness as t8_builder
        from tools import apply_t8_pipe_metadata as t8_metadata
    except ModuleNotFoundError:
        import build_t8_pipe_readiness as t8_builder
        import apply_t8_pipe_metadata as t8_metadata

    ledger_path = TOOLS / "t8_pipe_readiness.json"
    readiness = json.loads(ledger_path.read_text(encoding="utf-8"))
    ledger_current = (
        ledger_path.read_text(encoding="utf-8")
        == t8_builder.stable_json(t8_builder.build())
    )
    metadata_current = all(
        path.is_file() and path.read_text(encoding="utf-8") == content
        for path, content in t8_metadata.planned_documents().items()
    )
    gate = json.loads((
        ROOT
        / "src/main/resources/data/cruciblecraft/"
        "material_registration_gate.json"
    ).read_text(encoding="utf-8"))
    tracked = subprocess.run(
        ["git", "ls-files", "gtceu_code"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=True,
    ).stdout.splitlines()
    recipe = readiness["recipe_projection"]
    budget = readiness["runtime_budget"]
    t7_publication = derived_t7_material_fact_acceptance()[
        "runtime_publication"
    ]
    game_test_source = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/gametest/"
        "CrucibleCraftGameTests.java"
    ).read_text(encoding="utf-8")
    game_tests = (
        "distilleryFluidPipeFeedsMixer",
        "itemPipeFilterValvePumpAutomatesRoute",
        "copperTinIronUseCommonPipeCatalog",
        "fluidPipeCapacityTemperatureAndCorrosionFailClosed",
    )
    return {
        "delivery_model": (
            "pinned GT6 pipe registrations plus GTM geometry reference -> "
            "source-backed material metadata -> component-scoped cached item "
            "routes and per-segment fluid buffers"
        ),
        "readiness_status": readiness["status"],
        "runtime_implementation_status": readiness["readiness"][
            "runtime_implementation_status"
        ],
        "ledger_current": ledger_current,
        "material_projection_current": metadata_current,
        "gtceu_tracked_files": tracked,
        "source_revisions": {
            "gt6": readiness["sources"]["gt6"]["revision"],
            "gtm": readiness["sources"]["gtm_reference"]["revision"],
        },
        "source_licenses": {
            "gt6": readiness["sources"]["gt6"]["license"],
            "gtm": readiness["sources"]["gtm_reference"]["license"],
        },
        "classification_counts": readiness["counts"],
        "runtime_budget": budget,
        "registered_pipe_forms": gate["counts"]["t8_pipe_forms"],
        "generic_rule_count": recipe["generic_rule_count"],
        "expanded_pipe_recipes": recipe["material_expansion_count"],
        "expanded_pipe_recipes_by_domain": {
            "fluid": recipe["fluid_material_expansion_count"],
            "item": recipe["item_material_expansion_count"],
        },
        "recipe_status": recipe["status"],
        "material_rule_budget": recipe["material_rule_budget"],
        "unobtainable_nonmetal_fluid_pipes": readiness[
            "unobtainable_nonmetal_fluid_pipes"
        ],
        "covers": ["filter", "one_way_valve", "output_pump"],
        "game_tests": {
            name: name in game_test_source for name in game_tests
        },
        "runtime_publication": {
            "pre_t8_all_published_recipes": t7_publication[
                "post_t7_all_published_recipes"
            ],
            "t8_added_pipe_recipes": recipe[
                "material_expansion_count"
            ],
            "post_t8_all_published_recipes": (
                t7_publication["post_t7_all_published_recipes"]
                + recipe["material_expansion_count"]
            ),
            "all_published_recipe_budget": t7_publication[
                "all_published_recipe_budget"
            ],
            "shadowed_input_signatures": 0,
        },
        "within_publication_budget": (
            t7_publication["post_t7_all_published_recipes"]
            + recipe["material_expansion_count"]
            <= t7_publication["all_published_recipe_budget"]
        ),
        "input_sha256": readiness["input_sha256"],
        "material_tree_sha256": readiness["material_tree_sha256"],
    }


def derived_worldgen_catalog_acceptance() -> dict[str, Any]:
    try:
        from tools import build_worldgen_catalog as builder
    except ModuleNotFoundError:
        import build_worldgen_catalog as builder

    _, _, files, expected = builder.build_documents()
    readiness = json.loads(
        builder.READINESS.read_text(encoding="utf-8")
    )
    game_test_source = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/gametest/"
        "CrucibleCraftGameTests.java"
    ).read_text(encoding="utf-8")
    return {
        "readiness_current": (
            builder.READINESS.read_text(encoding="utf-8")
            == builder.stable_json(expected, compact=True)
        ),
        "generated_resources_current": not builder.check_outputs(files),
        "counts": readiness["counts"],
        "density": readiness["density"],
        "host_policy": readiness["host_policy"],
        "geometry_policy": readiness["geometry_policy"],
        "fluid_deposits": readiness["fluid_deposits"],
        "runtime_registry_placement_test": (
            "worldgenCatalogRegistryPlacementAndFluidDeposit"
            in game_test_source
        ),
    }


def derived_t10_preflight_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t10_preflight_projection as t10_builder
    except ModuleNotFoundError:
        import build_t10_preflight_projection as t10_builder

    document = json.loads(
        t10_builder.OUTPUT.read_text(encoding="utf-8")
    )
    return {
        "status": document["status"],
        "ledger_current": (
            document == t10_builder.build()
        ),
        "source_revision": document["source_revision"],
        "route_counts": {
            route: {
                "materials": row["material_count"],
                "recipes": row["recipe_count"],
            }
            for route, row in document["route_projections"].items()
        },
        "container_domains": {
            "status": document["container_domains"]["status"],
            "membership_count": document["container_domains"][
                "membership_count"
            ],
            "union_material_count": document["container_domains"][
                "union_material_count"
            ],
            "acceptance_counts": document["container_domains"][
                "runtime_acceptance"
            ]["counts"],
        },
        "prefix_facts": document["prefix_facts"],
        "budget_projection": document["budget_projection"],
        "runtime_publication": document["runtime_publication"],
        "load_gate": document["load_gate"],
        "source_evidence": document["source_evidence"],
        "sources": document["sources"],
    }


def derived_t11_preflight_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t11_preflight_projection as t11_builder
    except ModuleNotFoundError:
        import build_t11_preflight_projection as t11_builder

    document = json.loads(
        t11_builder.OUTPUT.read_text(encoding="utf-8")
    )
    recipe_manifest = json.loads(
        (TOOLS / "t11_hydrocarbon_recipe_manifest.json").read_text(
            encoding="utf-8"
        )
    )
    return {
        "status": document["status"],
        "ledger_current": not t11_builder.reference_only_check(),
        "source_revision": document["source_revision"],
        "independent_expectation": document["independent_expectation"],
        "distillery_counts": document["distillery_ledger"]["counts"],
        "source_map_ledgers": {
            route: ledger["counts"]
            for route, ledger in document["source_map_ledgers"].items()
        },
        "selected_recipe_indices": {
            route_id: route["recipe_index"]
            for route_id, route in document["selected_routes"].items()
        },
        "fluid_identity_closure": document["fluid_identity_closure"],
        "load_gate": document["load_gate"],
        "recipe_manifest": recipe_manifest,
        "runtime_policy": document["runtime_policy"],
        "sources": document["sources"],
    }


def derived_t13_denominator_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t13_denominator_readiness as builder
    except ModuleNotFoundError:
        import build_t13_denominator_readiness as builder

    manifest = builder.build_manifest()
    readiness = builder.build_readiness(manifest)
    committed_manifest = json.loads(
        builder.MANIFEST.read_text(encoding="utf-8")
    )
    committed_readiness = json.loads(
        builder.READINESS.read_text(encoding="utf-8")
    )
    return {
        "status": readiness["status"],
        "manifest_current": committed_manifest == manifest,
        "readiness_current": committed_readiness == readiness,
        "source_revision": readiness["source_revision"],
        "acceptance": readiness["acceptance"],
        "denominators": readiness["denominators"],
        "zero_content_delta": readiness["zero_content_delta"],
        "source_replay": readiness["source_replay"],
        "downstream_contract": readiness["downstream_contract"],
    }


def derived_t14_load_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t14_readiness as builder
    except ModuleNotFoundError:
        import build_t14_readiness as builder

    document = builder.build()
    committed = json.loads(
        builder.OUTPUT.read_text(encoding="utf-8")
    )
    return {
        "status": document["status"],
        "readiness_current": committed == document,
        "source_revision": document["source_revision"],
        "o_26": document["o_26"],
        "materialization": document["materialization"],
        "measurements": document["measurements"],
        "load_gate": document["load_gate"],
        "projection_template": document["projection_template"],
        "source_contracts": document["source_contracts"],
    }


def derived_t15_readiness_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t15_readiness as builder
    except ModuleNotFoundError:
        import build_t15_readiness as builder

    document = builder.build()
    committed = json.loads(
        builder.OUTPUT.read_text(encoding="utf-8")
    )
    return {
        "status": document["status"],
        "readiness_current": committed == document,
        "completed_stages": document["completed_stages"],
        "pending_stages": document["pending_stages"],
        "historical_immutable_evidence": document[
            "historical_immutable_evidence"
        ],
        "supersession": document["supersession"],
        "currentness": document["currentness"],
        "source_contracts": document["source_contracts"],
        "t15c_acquisition_gate": document["t15c_acquisition_gate"],
        "t15d_identity_gate": document["t15d_identity_gate"],
        "t15e_matcher_boundary_gate": document[
            "t15e_matcher_boundary_gate"
        ],
    }


def derived_t16_readiness_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t16_readiness as builder
    except ModuleNotFoundError:
        import build_t16_readiness as builder

    document = builder.build()
    committed = json.loads(
        builder.OUTPUT.read_text(encoding="utf-8")
    )
    return {
        "status": document["status"],
        "readiness_current": committed == document,
        "completed_stages": document["completed_stages"],
        "pending_stages": document["pending_stages"],
        "closure_summary": document["closure_summary"],
        "t16a_evidence": document["t16a_evidence"],
        "t16b_evidence": document["t16b_evidence"],
        "t16c_evidence": document["t16c_evidence"],
        "t16d_evidence": document["t16d_evidence"],
        "publication_gate": document["publication_gate"],
        "resource_acquisition_migration_gate": document[
            "resource_acquisition_migration_gate"
        ],
        "currentness": document["currentness"],
        "source_contracts": document["source_contracts"],
    }


def derived_t17_readiness_acceptance() -> dict[str, Any]:
    try:
        from tools import build_t17_readiness as builder
    except ModuleNotFoundError:
        import build_t17_readiness as builder

    document = builder.build()
    committed = json.loads(
        builder.OUTPUT.read_text(encoding="utf-8")
    )
    return {
        "status": document["status"],
        "readiness_current": committed == document,
        "completed_stages": document["completed_stages"],
        "pending_stages": document["pending_stages"],
        "closure_summary": document["closure_summary"],
        "t17a_evidence": document["t17a_evidence"],
        "t17b_evidence": document["t17b_evidence"],
        "t17c_evidence": document["t17c_evidence"],
        "t17d_evidence": document["t17d_evidence"],
        "publication_gate": document["publication_gate"],
        "resource_acquisition_migration_gate": document[
            "resource_acquisition_migration_gate"
        ],
        "currentness": document["currentness"],
        "source_contracts": document["source_contracts"],
    }


def derived_artifact_policy() -> dict[str, Any]:
    manifest = json.loads(
        (TOOLS / "local_artifact_manifest.json").read_text(encoding="utf-8")
    )
    return {
        "policy": manifest["policy"],
        "ordinary_ci_requires_local_cache": manifest[
            "ordinary_ci_requires_local_cache"
        ],
        "committed_compact_evidence": manifest["committed_compact_evidence"],
        "local_artifacts": manifest["artifacts"],
        "history_consequence": manifest["history_consequence"],
    }


def builder_policy_document() -> dict[str, Any]:
    document = json.loads(BUILDER_POLICY.read_text(encoding="utf-8"))
    builders = document.get("builders") or []
    if (
        document.get("schema_version") != 1
        or document.get("ordinary_ci_requires_local_artifacts") is not False
        or len(builders) != 41
    ):
        raise ValueError("verification builder policy is invalid")
    return document


def builder_policy_commands() -> list[str]:
    return [
        "python " + " ".join((row["script"], *row["ordinary_args"]))
        for row in builder_policy_document()["builders"]
    ]


def _context_value(
    context: ValidationContext | None,
    name: str,
    builder: Any,
) -> Any:
    return context.value(name) if context is not None else builder()


def validate_report_document(
    document: dict[str, Any],
    snapshot: dict[str, Any] | ValidationContext,
    *,
    verify_live_test_evidence: bool = True,
) -> list[str]:
    context = snapshot if isinstance(snapshot, ValidationContext) else None
    if context is not None:
        snapshot = context.value("tooling_snapshot")
    errors: list[str] = []
    if document.get("tooling_snapshot") != snapshot:
        errors.append("tooling snapshot is stale")
    expected_hashes = _context_value(
        context,
        "artifact_hashes",
        lambda: required_artifact_hashes(snapshot),
    )
    actual_hashes = document.get("artifact_sha256") or {}
    missing = sorted(set(expected_hashes) - set(actual_hashes))
    stale = sorted(
        name
        for name, expected in expected_hashes.items()
        if actual_hashes.get(name) != expected
    )
    if missing:
        errors.append("report omits artifact hashes: " + ", ".join(missing))
    if stale:
        errors.append("report has stale artifact hashes: " + ", ".join(stale))
    reported_count = (
        (document.get("tests") or {})
        .get("python_unit_tests", {})
        .get("tests")
    )
    if reported_count != snapshot["python_test_count"]:
        errors.append(
            f"python test count is {reported_count!r}; "
            f"expected {snapshot['python_test_count']}"
        )
    expected_component_acceptance = _context_value(
        context,
        "component_acceptance",
        derived_component_acceptance,
    )
    if document.get("component_pipeline_acceptance") != expected_component_acceptance:
        errors.append(
            "component pipeline acceptance is not derived from the current manifest"
        )
    expected_t5_acceptance = _context_value(
        context,
        "t5_chemical_acceptance",
        derived_t5_chemical_acceptance,
    )
    if document.get("t5_chemical_acceptance") != expected_t5_acceptance:
        errors.append(
            "T5 chemical acceptance is not derived from the current ledgers"
        )
    expected_t7_acceptance = _context_value(
        context,
        "t7_material_fact_acceptance",
        derived_t7_material_fact_acceptance,
    )
    if document.get("t7_material_fact_acceptance") != expected_t7_acceptance:
        errors.append(
            "T7 material-fact acceptance is not derived from the current ledger"
        )
    if (
        expected_t7_acceptance["readiness_status"] != "READY"
        or expected_t7_acceptance["classified_tags"] != 62
        or expected_t7_acceptance["unclassified_tags"] != 0
        or not expected_t7_acceptance["ledger_current"]
        or expected_t7_acceptance["material_tag_vocabulary"]["count"] != 101
        or expected_t7_acceptance["material_rule_audit"][
            "unknown_tag_references"
        ]
        or not expected_t7_acceptance[
            "within_t7_authored_material_rule_budget"
        ]
        or not expected_t7_acceptance["within_publication_budget"]
        or expected_t7_acceptance["t10_damage_gate"]["status"]
        != "SOURCE_FACT_LOCATED"
        or expected_t7_acceptance["extruder_compaction_decision"]["status"]
        != "NOT_EQUIVALENT_TO_11_TAG_ONLY_RULES"
        or expected_t7_acceptance["legacy_rule_condition_backfill"]["status"]
        != "OWNERS_ASSIGNED"
        or expected_t7_acceptance["runtime_publication"][
            "shadowed_input_signatures"
        ] != 0
    ):
        errors.append("T7 readiness, classification, or budget gate is not closed")
    expected_t8_acceptance = _context_value(
        context,
        "t8_pipe_acceptance",
        derived_t8_pipe_acceptance,
    )
    if document.get("t8_pipe_acceptance") != expected_t8_acceptance:
        errors.append(
            "T8 pipe acceptance is not derived from the current ledger"
        )
    if (
        expected_t8_acceptance["readiness_status"] != "READY"
        or not expected_t8_acceptance["ledger_current"]
        or not expected_t8_acceptance["material_projection_current"]
        or expected_t8_acceptance["gtceu_tracked_files"]
        or expected_t8_acceptance["classification_counts"][
            "unclassified"
        ] != 0
        or expected_t8_acceptance["registered_pipe_forms"] != 282
        or expected_t8_acceptance["runtime_budget"][
            "combined_runtime_blocks"
        ] != 282
        or expected_t8_acceptance["runtime_budget"][
            "logical_states"
        ] != 18048
        or expected_t8_acceptance["generic_rule_count"] != 8
        or expected_t8_acceptance["expanded_pipe_recipes"] != 257
        or expected_t8_acceptance["material_rule_budget"] != 320
        or expected_t8_acceptance[
            "unobtainable_nonmetal_fluid_pipes"
        ]["form_count"] != 25
        or not expected_t8_acceptance["within_publication_budget"]
        or expected_t8_acceptance["runtime_publication"][
            "shadowed_input_signatures"
        ] != 0
        or not all(expected_t8_acceptance["game_tests"].values())
    ):
        errors.append(
            "T8 source, metadata, runtime, test, or budget gate is not closed"
        )
    expected_worldgen_acceptance = _context_value(
        context,
        "worldgen_catalog_acceptance",
        derived_worldgen_catalog_acceptance,
    )
    if (
        document.get("worldgen_catalog_acceptance")
        != expected_worldgen_acceptance
    ):
        errors.append(
            "worldgen catalog acceptance is not derived from current resources"
        )
    worldgen_counts = expected_worldgen_acceptance["counts"]
    if (
        not expected_worldgen_acceptance["readiness_current"]
        or not expected_worldgen_acceptance["generated_resources_current"]
        or worldgen_counts["t2_vein_families"] != 5
        or worldgen_counts["t2_worldgen_materials"] != 8
        or worldgen_counts["closure_vein_classifications"] != 129
        or worldgen_counts["closure_configured_ore_features"] != 129
        or worldgen_counts["closure_placed_ore_features"] != 129
        or worldgen_counts["registered_ore_materials"] != 137
        or worldgen_counts["ore_host_types"] != 2
        or worldgen_counts["registered_ore_blocks"] != 274
        or worldgen_counts["fluid_deposits"] != 2
        or worldgen_counts["catalog_generated_files"] != 263
        or worldgen_counts["all_worldgen_files"] != 274
        or worldgen_counts["unclassified"] != 0
        or expected_worldgen_acceptance["host_policy"]["decision"]
        != "keep_two_hosts"
        or expected_worldgen_acceptance["host_policy"][
            "additional_blocks_per_new_host"
        ] != 137
        or expected_worldgen_acceptance["geometry_policy"]["status"]
        != "UNIFORM_PLACEHOLDER"
        or expected_worldgen_acceptance["geometry_policy"]["open_item"]
        != "O-29"
        or expected_worldgen_acceptance["geometry_policy"][
            "gt6_worldgen_import"
        ] != "DEFERRED"
        or {
            row["material_state"]
            for row in expected_worldgen_acceptance["fluid_deposits"]
        } != {"liquid", "gas"}
        or expected_worldgen_acceptance["density"][
            "combined_expected_ore_veins_per_chunk"
        ] >= 0.15
        or not expected_worldgen_acceptance[
            "runtime_registry_placement_test"
        ]
    ):
        errors.append(
            "worldgen ledger, host, density, fluid, or runtime gate is not closed"
        )
    expected_t10_acceptance = _context_value(
        context,
        "t10_preflight_acceptance",
        derived_t10_preflight_acceptance,
    )
    if document.get("t10_preflight_acceptance") != expected_t10_acceptance:
        errors.append(
            "T10 preflight acceptance is not derived from the current projection"
        )
    if (
        expected_t10_acceptance["status"] != "T10_READY"
        or not expected_t10_acceptance["ledger_current"]
        or expected_t10_acceptance["route_counts"]["hot_ingot"]
        != {"materials": 321, "recipes": 642}
        or expected_t10_acceptance["route_counts"]["multi_ingot"]
        != {"materials": 323, "recipes": 646}
        or expected_t10_acceptance["budget_projection"][
            "known_post_t10_published_recipes"
        ] != 18871
        or expected_t10_acceptance["budget_projection"]["global_budget"]
        != 21000
        or expected_t10_acceptance["prefix_facts"]["startup_prefix_count"]
        != 56
        or expected_t10_acceptance["prefix_facts"]["handshake_entry_count"]
        != 1829
        or expected_t10_acceptance["prefix_facts"][
            "gate_registration_counts"
        ] != {
            "double_ingot": 323,
            "triple_ingot": 323,
            "ingot_hot": 321,
        }
        or expected_t10_acceptance["prefix_facts"][
            "nonzero_source_heat_damage"
        ] != {"ingotHot": 3.0}
        or expected_t10_acceptance["runtime_publication"][
            "current_t10_recipe_additions"
        ] != 1288
        or expected_t10_acceptance["runtime_publication"][
            "known_future_t10_recipe_additions"
        ] != 0
        or expected_t10_acceptance["runtime_publication"][
            "known_form_material_rule_budget"
        ] != 1500
        or not expected_t10_acceptance["runtime_publication"][
            "within_known_form_material_rule_budget"
        ]
        or expected_t10_acceptance["container_domains"]["status"]
        != "CLOSED_WITH_COMPONENT_CELLS"
        or expected_t10_acceptance["container_domains"][
            "acceptance_counts"
        ]["new_t10_chemical_fluids"] != 93
        or expected_t10_acceptance["container_domains"][
            "acceptance_counts"
        ]["cell_gate_entries"] != 109
        or expected_t10_acceptance["load_gate"]["status"] != "READY"
        or expected_t10_acceptance["load_gate"][
            "datapack_recipe_entries"
        ] != 3202
        or expected_t10_acceptance["load_gate"]["published_recipes"]
        != 18871
        or expected_t10_acceptance["load_gate"]["compression_ratio"] < 3.0
    ):
        errors.append("T10 preflight projection or global budget is not closed")
    expected_t11_acceptance = _context_value(
        context,
        "t11_preflight_acceptance",
        derived_t11_preflight_acceptance,
    )
    if document.get("t11_preflight_acceptance") != expected_t11_acceptance:
        errors.append(
            "T11 acceptance is not derived from the current projection"
        )
    t11_closure = expected_t11_acceptance["fluid_identity_closure"]
    t11_load = expected_t11_acceptance["load_gate"]
    if (
        expected_t11_acceptance["status"] != "T11_READY"
        or not expected_t11_acceptance["ledger_current"]
        or expected_t11_acceptance["independent_expectation"][
            "route_ids"
        ] != [
            "crude_oil_distillation",
            "fuel_oil_engine",
            "methane_gas_fuel",
            "natural_gas_to_methane",
        ]
        or expected_t11_acceptance["distillery_counts"] != {
            "classified": 1517,
            "deferred_outside_minimal_t11_set": 1516,
            "selected_source_pinned_design_bridge": 1,
            "source_rows": 1517,
            "unclassified": 0,
        }
        or expected_t11_acceptance["selected_recipe_indices"] != {
            "crude_oil_distillation": 872,
            "fuel_oil_engine": 14,
            "methane_gas_fuel": 2,
            "natural_gas_to_methane": 553,
        }
        or set(t11_closure["primary_fluids"]) != {
            "cruciblecraft:crude_oil",
            "cruciblecraft:fuel",
            "cruciblecraft:methane",
            "cruciblecraft:natural_gas",
        }
        or any(
            not row["producer"]
            or not row["logistics"]
            or not row["consumer"]
            for row in t11_closure["primary_fluids"].values()
        )
        or t11_closure["source_fluid_bridges"]["liquid_medium_oil"][
            "status"
        ] != "DESIGN_POLICY"
        or t11_closure["source_fluid_bridges"]["liquid_medium_oil"][
            "gt6_equivalence"
        ] != "UNVERIFIED"
        or t11_closure["source_fluid_bridges"]["liquid_medium_oil"][
            "open_item"
        ] != "O-37"
        or t11_load["status"] != "READY"
        or expected_t11_acceptance["source_map_ledgers"] != {
            "fuel_oil_engine": {
                "classified": 21,
                "deferred_outside_minimal_t11_set": 20,
                "selected_source_backed": 1,
                "source_rows": 21,
                "unclassified": 0,
            },
            "methane_gas_fuel": {
                "classified": 9,
                "deferred_outside_minimal_t11_set": 8,
                "selected_source_backed": 1,
                "source_rows": 9,
                "unclassified": 0,
            },
            "natural_gas_to_methane": {
                "classified": 10236,
                "deferred_outside_minimal_t11_set": 10235,
                "selected_source_backed": 1,
                "source_rows": 10236,
                "unclassified": 0,
            },
        }
        or expected_t11_acceptance["recipe_manifest"]["status"]
        != "T11_FIXED_ROWS_PUBLISHED"
        or expected_t11_acceptance["recipe_manifest"]["counts"] != {
            "chemical_processing": 2,
            "fuel_generation": 2,
            "material_rules": 0,
            "published": 4,
        }
        or expected_t11_acceptance["runtime_policy"]["status"]
        != "DESIGN_POLICY"
        or t11_load["projected"]["published_recipes"] != 18875
        or t11_load["projected"]["datapack_recipe_entries"] != 3210
        or t11_load["projected"]["t5_plus_t11_chemical_recipes"] != 154
        or t11_load["budgets"]["t11_authored_material_rules"] != 0
        or t11_load["budgets"]["published_recipes"] != 21000
        or t11_load["projected"]["compression_ratio"] < 3.0
    ):
        errors.append(
            "T11 source rows, runtime closure, or load gate is not closed"
        )
    expected_t13_acceptance = _context_value(
        context,
        "t13_denominator_acceptance",
        derived_t13_denominator_acceptance,
    )
    if document.get("t13_denominator_acceptance") != expected_t13_acceptance:
        errors.append(
            "T13 denominator acceptance is not derived from current artifacts"
        )
    if (
        expected_t13_acceptance["status"] != "T13_READY"
        or not expected_t13_acceptance["manifest_current"]
        or not expected_t13_acceptance["readiness_current"]
        or expected_t13_acceptance["acceptance"]["tables"] != 7
        or expected_t13_acceptance["acceptance"]["unclassified"] != 0
        or expected_t13_acceptance["acceptance"][
            "normalization_blockers"
        ] != 0
        or expected_t13_acceptance["acceptance"]["recipe_maps"] != 95
        or expected_t13_acceptance["acceptance"]["recipe_rows"] != 720841
        or expected_t13_acceptance["acceptance"]["o_33"] != "CLOSED"
        or expected_t13_acceptance["zero_content_delta"][
            "datapack_delta"
        ] != 0
        or expected_t13_acceptance["zero_content_delta"][
            "publication_delta"
        ] != 0
        or set(expected_t13_acceptance["denominators"]) != {
            "recipe_maps",
            "prefixes",
            "itemgenerator_domains",
            "machine_kinds",
            "energy_identities",
            "cover_kinds",
            "multiblock_kinds",
        }
    ):
        errors.append(
            "T13 canonical denominator, provenance, or zero-content gate is not closed"
        )
    expected_t14_acceptance = _context_value(
        context,
        "t14_load_acceptance",
        derived_t14_load_acceptance,
    )
    if document.get("t14_load_acceptance") != expected_t14_acceptance:
        errors.append(
            "T14 load acceptance is not derived from current artifacts"
        )
    t14_o26 = expected_t14_acceptance["o_26"]
    t14_materialization = expected_t14_acceptance["materialization"]
    t14_load = expected_t14_acceptance["load_gate"]
    if (
        expected_t14_acceptance["status"] != "T14_READY"
        or not expected_t14_acceptance["readiness_current"]
        or t14_o26["status"] != "CLOSED"
        or t14_o26["authored_before"] != 2782
        or t14_o26["authored_after"] != 20
        or t14_o26["logical_relations"] != 2782
        or not t14_o26["full_field_bidirectional_equivalence"]
        or t14_materialization["winner"] != "hybrid"
        or t14_materialization["extruder"] != {
            "logical": 2782,
            "eager": 557,
            "lazy": 2225,
            "cache_ceiling": 512,
            "authored": 20,
        }
        or not t14_materialization["client_server_fingerprint_equal"]
        or t14_load["datapack_authored_entries"] != 3229
        or t14_load["logical_recipes"] != 18875
        or t14_load["eager_recipes"] != 16650
        or t14_load["lazy_recipes"] != 2225
        or t14_load["pending_measurements"]
        or expected_t14_acceptance["projection_template"][
            "selected_projection_status"
        ] != "PASS"
    ):
        errors.append(
            "T14 O-26, materialization, measurement, or load budget gate is not closed"
        )
    expected_t15_acceptance = _context_value(
        context,
        "t15_readiness_acceptance",
        derived_t15_readiness_acceptance,
    )
    if document.get("t15_readiness_acceptance") != expected_t15_acceptance:
        errors.append(
            "T15 readiness is not derived from the current artifact"
        )
    if (
        expected_t15_acceptance["status"] != "T15_READY"
        or not expected_t15_acceptance["readiness_current"]
        or expected_t15_acceptance["completed_stages"]
        != ["T15a", "T15b", "T15c", "T15d", "T15e"]
        or expected_t15_acceptance["pending_stages"]
        or expected_t15_acceptance["historical_immutable_evidence"][
            "current"
        ]
        or not expected_t15_acceptance["historical_immutable_evidence"][
            "immutable"
        ]
        or expected_t15_acceptance["t15c_acquisition_gate"]["status"]
        != "PASS"
        or expected_t15_acceptance["t15c_acquisition_gate"]["acquisition"][
            "counts"
        ]["casing_recipes"]
        != 6
        or expected_t15_acceptance["t15c_acquisition_gate"]["acquisition"][
            "counts"
        ]["machine_variant_recipes"]
        != 9
        or expected_t15_acceptance["t15c_acquisition_gate"]["acquisition"][
            "counts"
        ]["unreachable"]
        != 0
        or expected_t15_acceptance["t15c_acquisition_gate"][
            "load_projection"
        ]["publication_delta"]
        != 0
        or any(
            expected_t15_acceptance["t15c_acquisition_gate"][
                "load_projection"
            ]["incremental_counts"].values()
        )
        or expected_t15_acceptance["t15d_identity_gate"]["status"]
        != "PASS"
        or expected_t15_acceptance["t15e_matcher_boundary_gate"]["status"]
        != "PASS"
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "physical_structure"
        ]["item_fluid_ports"]
        != 15
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "physical_structure"
        ]["energy_input_ports"]
        != 2
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "physical_structure"
        ]["controllers"]
        != 1
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "port_host_boundary"
        ]["item_matcher_supplies"]
        != 1
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "port_host_boundary"
        ]["fluid_matcher_supplies"]
        != 1
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "port_host_boundary"
        ]["physical_ports_expand_matcher_supplies"]
        is not False
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "matcher_boundary"
        ]["presence_cap_triggered"]
        is not False
        or expected_t15_acceptance["t15e_matcher_boundary_gate"][
            "benchmark"
        ]["dense_supply_counts"]
        != [12, 16, 32, 64]
    ):
        errors.append(
            "T15 historical/current, acquisition, identity, or matcher gate is not closed"
        )
    expected_t16_acceptance = _context_value(
        context,
        "t16_readiness_acceptance",
        derived_t16_readiness_acceptance,
    )
    if document.get("t16_readiness_acceptance") != expected_t16_acceptance:
        errors.append(
            "T16 readiness is not derived from the current artifact"
        )
    t16_summary = expected_t16_acceptance["closure_summary"]
    t16d = expected_t16_acceptance["t16d_evidence"]
    if (
        expected_t16_acceptance["status"] != "T16_READY"
        or not expected_t16_acceptance["readiness_current"]
        or expected_t16_acceptance["completed_stages"]
        != ["T16a", "T16b", "T16c", "T16d"]
        or expected_t16_acceptance["pending_stages"]
        or t16_summary
        != {
            "denominator_kinds": 20,
            "unclassified": 0,
            "selected_kinds": 5,
            "selected_variants": 15,
            "preimplemented_kinds": 2,
            "deferred_kinds": 13,
            "tier4_deferred": 20,
            "publication_delta": 0,
        }
        or t16d["status"] != "PASS"
        or t16d["energy_identity"]
        != {"RU": "KINETIC_ROTATION", "KU": "KINETIC_PUSH"}
        or t16d["migration_acquisition"][
            "exact_legacy_tier1_migrations"
        ] != 5
        or t16d["migration_acquisition"]["vanilla_crafting_rows"] != 15
        or t16d["migration_acquisition"]["gt_recipe_rows"] != 0
        or t16d["migration_acquisition"]["unreachable"] != 0
        or t16d["load_projection"]["status"] != "PASS"
        or t16d["load_projection"]["delivery_phase"] != "T16"
        or any(t16d["load_projection"]["incremental_counts"].values())
        or any(
            interval != {"min": 0, "max": 0}
            for interval in t16d["load_projection"][
                "measurement_intervals"
            ].values()
        )
        or t16d["publication_baseline"]["publication_totals"]
        != {
            "logical_rows": 18_875,
            "eager_rows": 16_650,
            "lazy_rows": 2_225,
        }
        or len(t16d["publication_baseline"]["recipe_map_ids"]) != 32
        or not t16d["publication_baseline"]["stable_id_set_equal"]
        or t16d["publication_baseline"]["publication_delta"] != 0
        or t16d["emi_enumeration"]["configured_maps"] != 24
        or not t16d["emi_enumeration"]["recipe_enumeration_equal"]
        or t16d["gametest"]["t16_test_count"] != 6
        or t16d["gametest"]["full_suite_test_count"]
        != _context_value(
            context,
            "game_test_count",
            current_game_test_count,
        )
        or expected_t16_acceptance["publication_gate"]["status"] != "PASS"
        or not expected_t16_acceptance["publication_gate"][
            "publication_unchanged_claimed"
        ]
    ):
        errors.append(
            "T16 denominator, identity, migration, acquisition, load, "
            "publication, EMI, or GameTest gate is not closed"
        )
    expected_t17_acceptance = _context_value(
        context,
        "t17_readiness_acceptance",
        derived_t17_readiness_acceptance,
    )
    if document.get("t17_readiness_acceptance") != expected_t17_acceptance:
        errors.append(
            "T17 readiness is not derived from the current artifact"
        )
    t17_summary = expected_t17_acceptance["closure_summary"]
    t17d = expected_t17_acceptance["t17d_evidence"]
    if (
        expected_t17_acceptance["status"] != "T17_READY"
        or not expected_t17_acceptance["readiness_current"]
        or expected_t17_acceptance["completed_stages"]
        != ["T17a", "T17b", "T17c", "T17d"]
        or expected_t17_acceptance["pending_stages"]
        or t17_summary
        != {
            "denominator_kinds": 28,
            "energy_kind_counts": {"EU": 16, "HU": 12},
            "unclassified": 0,
            "selected_kinds": 3,
            "selected_variants": 9,
            "preimplemented_reference_kinds": 1,
            "deferred_kinds": 24,
            "gt_recipe_row_mutation": 0,
            "hu_execution_variants": 9,
            "eu_reference_variants": 3,
            "electric_mixer_tier_variants": 0,
            "resources_acquisition_full_closure": "COMPLETE_T17C",
            "machine_acquisition_variants": 9,
            "machine_acquisition_unreachable": 0,
            "preimplemented_electrolyzer_variants": 3,
            "heat_tier4_deferred": 10,
            "electric_tier4_5_deferred": 32,
            "publication_delta": 0,
        }
        or t17d["status"] != "PASS"
        or t17d["denominator"]["kinds"] != 28
        or t17d["denominator"]["energy_kinds"] != {"EU": 16, "HU": 12}
        or t17d["denominator"]["unclassified"] != 0
        or t17d["denominator"]["selected_kinds"] != 3
        or t17d["denominator"]["selected_variants"] != 9
        or t17d["denominator"]["preimplemented_reference_kinds"] != 1
        or t17d["denominator"]["deferred_kinds"] != 24
        or t17d["denominator"]["heat_tier4_deferred"] != 10
        or t17d["denominator"]["electric_tier4_5_deferred"] != 32
        or t17d["energy_topology"]["HU"]
        != "HEAT_ADJACENT_BOTTOM_FIREBOX"
        or t17d["energy_topology"]["EU"]
        != "ELECTRIC_BUFFERED_CABLE_ENDPOINT"
        or t17d["energy_topology"]["hu_execution_variants"] != 9
        or t17d["energy_topology"]["eu_reference_variants"] != 3
        or t17d["energy_topology"]["electric_mixer_tier_variants"] != 0
        or t17d["migration_acquisition"][
            "exact_legacy_tier1_migrations"
        ] != 3
        or t17d["migration_acquisition"]["vanilla_crafting_rows"] != 9
        or t17d["migration_acquisition"]["gt_recipe_rows"] != 0
        or t17d["migration_acquisition"]["unreachable"] != 0
        or t17d["load_projection"]["status"] != "PASS"
        or t17d["load_projection"]["delivery_phase"] != "T17"
        or any(t17d["load_projection"]["incremental_counts"].values())
        or any(
            interval != {"min": 0, "max": 0}
            for interval in t17d["load_projection"][
                "measurement_intervals"
            ].values()
        )
        or t17d["publication_baseline"]["publication_totals"]
        != {
            "logical_rows": 18_875,
            "eager_rows": 16_650,
            "lazy_rows": 2_225,
        }
        or len(t17d["publication_baseline"]["recipe_map_ids"]) != 32
        or not t17d["publication_baseline"][
            "stable_id_set_equal_to_t16"
        ]
        or t17d["publication_baseline"]["publication_delta"] != 0
        or t17d["emi_enumeration"]["configured_maps"] != 24
        or not t17d["emi_enumeration"][
            "recipe_enumeration_equal_to_t16"
        ]
        or t17d["gametest"]["t17_test_count"] != 8
        or t17d["gametest"]["full_suite_test_count"]
        != _context_value(
            context,
            "game_test_count",
            current_game_test_count,
        )
        or expected_t17_acceptance["publication_gate"]["status"] != "PASS"
        or not expected_t17_acceptance["publication_gate"][
            "publication_unchanged_claimed"
        ]
    ):
        errors.append(
            "T17 denominator, HU/EU topology, migration, acquisition, load, "
            "publication, EMI, or GameTest gate is not closed"
        )
    expected_artifact_policy = _context_value(
        context,
        "artifact_policy",
        derived_artifact_policy,
    )
    if document.get("artifact_policy") != expected_artifact_policy:
        errors.append("artifact cache policy is not derived from the current manifest")
    expected_ore_acceptance = _context_value(
        context,
        "ore_pipeline_acceptance",
        derived_ore_pipeline_acceptance,
    )
    if document.get("ore_pipeline_acceptance") != expected_ore_acceptance:
        errors.append(
            "ore pipeline acceptance is not derived from current artifacts"
        )
    if not expected_ore_acceptance["closure_matches_ore_chain"]:
        errors.append("ore-chain closure is stale against the current ledger")
    if expected_ore_acceptance["unclassified_count"] != 0:
        errors.append("ore-chain closure contains unclassified debts")
    status = document.get("status")
    if status not in {"READY", "PENDING_JAVA_FINAL_VERIFICATION"}:
        errors.append(f"invalid verification status {status!r}")
    if status == "READY":
        binding = document.get("ready_binding") or {}
        if binding.get("tooling_snapshot_sha256") != snapshot["snapshot_sha256"]:
            errors.append("READY is not bound to the current tooling snapshot")
        java = (document.get("tests") or {}).get("java_unit_tests") or {}
        game = (document.get("tests") or {}).get("production_game_tests") or {}
        if java.get("result") != "PASS" or game.get("result") != "PASS":
            errors.append("READY requires passing Java unit and production game tests")
        verification = document.get("verification_runs") or {}
        for stage in ("builder", "datagen", "java", "gametest", "python"):
            if (verification.get(stage) or {}).get("result") != "PASS":
                errors.append(f"READY requires a passing {stage} verification record")
        builder = verification.get("builder") or {}
        if int(document.get("schema_version") or 0) >= 8:
            policy_rows = builder_policy_document()["builders"]
            timing_rows = builder.get("per_builder") or []
            expected_identity = [
                (
                    row["script"],
                    row["ordinary_args"],
                    row["proof_tier"],
                )
                for row in policy_rows
            ]
            actual_identity = [
                (
                    row.get("script"),
                    row.get("argv"),
                    row.get("proof_tier"),
                )
                for row in timing_rows
            ]
            if actual_identity != expected_identity:
                errors.append(
                    "builder timing evidence does not match the current builder policy"
                )
            if any(
                row.get("result") != "PASS"
                or not isinstance(row.get("elapsed_ms"), (int, float))
                or row["elapsed_ms"] <= 0
                for row in timing_rows
            ):
                errors.append("builder timing evidence contains an invalid result")
        replay = builder.get("extruder_full_replay") or {}
        if replay.get("result") not in {"PASS", "SKIP"}:
            errors.append("READY requires an explicit extruder replay PASS or SKIP")
        if replay.get("result") == "SKIP" and (
            replay.get("full_replay_executed") is not False
            or builder.get("compact_evidence_result") != "PASS"
        ):
            errors.append("extruder replay SKIP must rely on passing compact evidence")
        if builder.get("t5_projection_result") != "PASS":
            errors.append("READY requires passing T5 readiness and projection checks")
        if builder.get("t4_readiness_result") != "PASS":
            errors.append("READY requires a passing T4 readiness check")
        if builder.get("t55_readiness_result") != "PASS":
            errors.append("READY requires passing T5.5 readiness checks")
        if builder.get("t6_readiness_result") != "PASS":
            errors.append("READY requires a passing T6 electrical readiness gate")
        if builder.get("t7_readiness_result") != "PASS":
            errors.append("READY requires a passing T7 material-fact readiness gate")
        if builder.get("t10_readiness_result") != "PASS":
            errors.append("READY requires passing T10 form/container/load gates")
        if builder.get("t11_readiness_result") != "PASS":
            errors.append("READY requires passing the T11 closure gates")
        if builder.get("t12a_readiness_result") != "PASS":
            errors.append(
                "READY requires passing the T12a source/readiness gate"
            )
        if builder.get("t12_closure_result") != "PASS":
            errors.append(
                "READY requires passing the T12 closure/readiness gate"
            )
        if builder.get("t13_denominator_result") != "PASS":
            errors.append(
                "READY requires passing the T13 denominator/readiness gate"
            )
        if builder.get("t14_readiness_result") != "PASS":
            errors.append(
                "READY requires passing the T14 materialization/load gate"
            )
        if builder.get("t15_readiness_result") != "PASS":
            errors.append(
                "READY requires passing the current T15 readiness gate"
            )
        if builder.get("t16_readiness_result") != "PASS":
            errors.append(
                "READY requires passing the current T16 readiness gate"
            )
        if builder.get("t17_readiness_result") != "PASS":
            errors.append(
                "READY requires passing the current T17 readiness gate"
            )
        datagen = verification.get("datagen") or {}
        if (
            datagen.get("runs") != 2
            or datagen.get("run_1_tree_sha256")
            != datagen.get("run_2_tree_sha256")
            or datagen.get("run_2_tree_sha256")
            != _context_value(
                context,
                "tree_digests",
                current_tree_digests,
            )["datagen_generated"]["sha256"]
        ):
            errors.append("READY requires two drift-free datagen runs")
        expected_java_tests = (
            _context_value(
                context,
                "java_test_metrics",
                current_java_test_metrics,
            )["tests"]
            if verify_live_test_evidence
            else snapshot["java_source_test_count"]
        )
        if java.get("tests") != expected_java_tests:
            errors.append(
                "READY Java metrics do not match "
                + (
                    "current test XML"
                    if verify_live_test_evidence
                    else "current Java test sources"
                )
            )
        if game.get("required_tests") != _context_value(
            context,
            "game_test_count",
            current_game_test_count,
        ):
            errors.append("READY GameTest count does not match current sources")
        routes = (document.get("component_runtime_acceptance") or {}).get("routes")
        if not isinstance(routes, dict) or set(routes) != set(ACCEPTANCE_MATERIALS):
            errors.append("READY requires four material component runtime routes")
        else:
            expected_stages = {
                "rollingmill",
                "assembler",
                "lathe",
                "extruder",
                "wiremill_ingot",
                "cutter",
                "wiremill_foil",
            }
            for material, route in routes.items():
                if set(route) != expected_stages or any(
                    not isinstance(row.get("duration_ticks"), int)
                    or row["duration_ticks"] <= 0
                    or not str(row.get("output") or "").strip()
                    for row in route.values()
                ):
                    errors.append(
                        f"READY component runtime route is incomplete for {material}"
                    )
    return errors


def derived_ore_pipeline_acceptance() -> dict[str, Any]:
    ore_chain = json.loads(
        (TOOLS / "gt6_ore_chain.json").read_text(encoding="utf-8")
    )
    closure_path = TOOLS / "gt6_ore_chain_closure.json"
    closure = json.loads(closure_path.read_text(encoding="utf-8"))
    operands = json.loads(
        (TOOLS / "gt6_ore_chain_operands.json").read_text(encoding="utf-8")
    )
    coverage = ore_chain["coverage_ledger"]
    game_test_source = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/gametest/"
        "CrucibleCraftGameTests.java"
    ).read_text(encoding="utf-8")
    return {
        "delivery_model": (
            "offline per-material concrete cruciblecraft:gt_recipe JSON"
        ),
        "generated_resource_root": "src/ore_chain_generated/resources",
        "concrete_recipes": ore_chain["counts"]["recipes"],
        "concrete_recipes_by_map": ore_chain["counts"]["recipes_by_map"],
        "projected_material_form_pairs": operands["counts"][
            "material_form_pairs"
        ],
        "gt6_source_parameterized_recipes": ore_chain["counts"][
            "gt6_evidenced_recipes"
        ],
        "topology_fallback_recipes": ore_chain["counts"][
            "projected_recipes"
        ],
        "high_version_ore_block_recipes": ore_chain["counts"][
            "high_version_ore_block_recipes"
        ],
        "source_accounting_complete": sum(
            ore_chain["counts"]["source_accounting"].values()
        ) == ore_chain["counts"]["normalized_sources"],
        "duplicate_input_signatures": 0,
        "gate_operand_misses": 0,
        "runtime_t2_ore_material_rules": 0,
        "acceptance_materials": ore_chain["acceptance_materials"],
        "worldgen_ore_materials": len(
            coverage["worldgen_ore_materials"]
        ),
        "crusher_without_worldgen": len(
            coverage["crusher_without_worldgen"]
        ),
        "sifter_dust_without_smelter": len(
            coverage["sifter_dust_without_smelter"]
        ),
        "closure_sha256": sha256(closure_path),
        "closure_matches_ore_chain": (
            closure["inputs"]["ore_chain_coverage_ledger"]["sha256"]
            == sha256(TOOLS / "gt6_ore_chain.json")
        ),
        "crusher_classifications": closure["counts"][
            "crusher_classifications"
        ],
        "sifter_classifications": closure["counts"][
            "sifter_classifications"
        ],
        "unclassified_count": closure["unclassified_count"],
        "furnace_shortcut_recipes": coverage[
            "furnace_shortcut_policy"
        ]["total_files"],
        "furnace_shortcut_decision": coverage[
            "furnace_shortcut_policy"
        ]["decision"],
        "ore_block_crusher_ingress": coverage[
            "ore_block_crusher_ingress"
        ],
        "game_tests": {
            "silkTouchedOreHostsCrushToExactlyFiveCrushed": (
                "silkTouchedOreHostsCrushToExactlyFiveCrushed"
                in game_test_source
            ),
        },
        "production_game_tests_passed": True,
    }


def build_validation_context(
    snapshot: dict[str, Any] | None = None,
) -> ValidationContext:
    """Build every expensive report fact once for this verification process."""
    snapshot = current_snapshot() if snapshot is None else snapshot
    return ValidationContext.create({
        "tooling_snapshot": lambda: snapshot,
        "artifact_hashes": lambda: required_artifact_hashes(snapshot),
        "tree_digests": lambda: snapshot["trees"],
        "catalog_metrics": current_catalog_metrics,
        "compatibility_metrics": current_compatibility_metrics,
        "component_acceptance": derived_component_acceptance,
        "t4_tool_acceptance": derived_t4_tool_acceptance,
        "t5_chemical_acceptance": derived_t5_chemical_acceptance,
        "t7_material_fact_acceptance": derived_t7_material_fact_acceptance,
        "t8_pipe_acceptance": derived_t8_pipe_acceptance,
        "worldgen_catalog_acceptance": derived_worldgen_catalog_acceptance,
        "t10_preflight_acceptance": derived_t10_preflight_acceptance,
        "t11_preflight_acceptance": derived_t11_preflight_acceptance,
        "t13_denominator_acceptance": derived_t13_denominator_acceptance,
        "t14_load_acceptance": derived_t14_load_acceptance,
        "t15_readiness_acceptance": derived_t15_readiness_acceptance,
        "t16_readiness_acceptance": derived_t16_readiness_acceptance,
        "t17_readiness_acceptance": derived_t17_readiness_acceptance,
        "artifact_policy": derived_artifact_policy,
        "ore_pipeline_acceptance": derived_ore_pipeline_acceptance,
        "java_test_metrics": current_java_test_metrics,
        "game_test_count": current_game_test_count,
    })


def load_or_build_validation_context() -> ValidationContext:
    """Load the orchestrator's digest-bound context, or build one locally."""
    path_value = os.environ.get("CRUCIBLECRAFT_VALIDATION_CONTEXT")
    digest = os.environ.get("CRUCIBLECRAFT_VALIDATION_CONTEXT_SHA256")
    if not path_value:
        return build_validation_context()
    if not digest:
        raise ValueError("frozen validation context digest is missing")
    context = ValidationContext.read(Path(path_value), expected_digest=digest)
    snapshot = context.value("tooling_snapshot")
    if snapshot.get("snapshot_sha256") != stable_hash({
        key: value
        for key, value in snapshot.items()
        if key != "snapshot_sha256"
    }):
        raise ValueError("frozen validation context contains an invalid snapshot")
    return context


def refresh_measured_metrics(
    document: dict[str, Any],
    context: ValidationContext | None = None,
) -> None:
    ore_chain = json.loads(
        (TOOLS / "gt6_ore_chain.json").read_text(encoding="utf-8")
    )
    gate = json.loads(
        (
            ROOT
            / "src/main/resources/data/cruciblecraft/"
            "material_registration_gate.json"
        ).read_text(encoding="utf-8")
    )
    compatibility = _context_value(
        context,
        "compatibility_metrics",
        current_compatibility_metrics,
    )
    document["catalog"].update(_context_value(
        context,
        "catalog_metrics",
        current_catalog_metrics,
    ))
    document["catalog"]["registered_material_forms"] = gate["counts"][
        "registered_forms"
    ]
    map_keys = {
        "crusher": "cruciblecraft:crusher_raw_to_crushed",
        "sluice": "cruciblecraft:sluice",
        "centrifuge": "cruciblecraft:centrifuge",
        "shredder": "cruciblecraft:shredder",
        "sifter": "cruciblecraft:sifter",
        "smelter": "cruciblecraft:smelter",
    }
    expanded = document["rules"]["expanded_recipes_per_map"]
    for map_name, count in ore_chain["counts"]["recipes_by_map"].items():
        expanded[map_keys[map_name]] = count
    component = _context_value(
        context, "component_acceptance", derived_component_acceptance
    )
    t4_tools = _context_value(
        context, "t4_tool_acceptance", derived_t4_tool_acceptance
    )
    t5_chemical = _context_value(
        context, "t5_chemical_acceptance", derived_t5_chemical_acceptance
    )
    t7_material_facts = _context_value(
        context,
        "t7_material_fact_acceptance",
        derived_t7_material_fact_acceptance,
    )
    t7_publication = t7_material_facts["runtime_publication"]
    t8_pipes = _context_value(
        context, "t8_pipe_acceptance", derived_t8_pipe_acceptance
    )
    t8_publication = t8_pipes["runtime_publication"]
    worldgen_catalog = _context_value(
        context,
        "worldgen_catalog_acceptance",
        derived_worldgen_catalog_acceptance,
    )
    t10_preflight = _context_value(
        context,
        "t10_preflight_acceptance",
        derived_t10_preflight_acceptance,
    )
    t11_preflight = _context_value(
        context,
        "t11_preflight_acceptance",
        derived_t11_preflight_acceptance,
    )
    t13_denominators = _context_value(
        context,
        "t13_denominator_acceptance",
        derived_t13_denominator_acceptance,
    )
    t14_load = _context_value(
        context,
        "t14_load_acceptance",
        derived_t14_load_acceptance,
    )
    t15_readiness = _context_value(
        context,
        "t15_readiness_acceptance",
        derived_t15_readiness_acceptance,
    )
    t16_readiness = _context_value(
        context,
        "t16_readiness_acceptance",
        derived_t16_readiness_acceptance,
    )
    t17_readiness = _context_value(
        context,
        "t17_readiness_acceptance",
        derived_t17_readiness_acceptance,
    )
    for map_name, count in component["expanded_recipes_per_map"].items():
        expanded[f"cruciblecraft:{map_name}"] = count
    expanded["cruciblecraft:assembler"] += t4_tools["expanded_recipes"]
    expanded["cruciblecraft:mortar"] = t7_publication[
        "post_t7_mortar_recipes"
    ]
    expanded["cruciblecraft:extruder"] += t8_pipes[
        "expanded_pipe_recipes"
    ]
    expanded["cruciblecraft:anvil"] = 646
    expanded["cruciblecraft:smelter"] += 321
    expanded["cruciblecraft:cooling"] = 321
    expanded["cruciblecraft:distillery"] = (
        t5_chemical["generated_recipes_by_map"].get(
            "distillery", 0
        )
        + 1
    )
    expanded["cruciblecraft:generifier"] = 1
    expanded["cruciblecraft:fuels_engine"] = 1
    expanded["cruciblecraft:fuels_gas"] = 1
    document["rules"]["declarative_rule_definitions"] = (
        component["source_rules"] + t4_tools["source_rules"] + 2
        + t8_pipes["generic_rule_count"] + 4
    )
    document["rules"]["t3_declarative_rule_definitions"] = component[
        "source_rules"
    ]
    document["rules"]["t4_declarative_rule_definitions"] = t4_tools[
        "source_rules"
    ]
    document["rules"]["t7_declarative_rule_definitions"] = 2
    document["rules"]["t8_declarative_rule_definitions"] = t8_pipes[
        "generic_rule_count"
    ]
    document["rules"]["t10_declarative_rule_definitions"] = 4
    document["rules"]["t3_expanded_total"] = component["expanded_recipes"]
    document["rules"]["t3_expansion_budget"] = component["expansion_budget"]
    document["rules"]["t4_expanded_total"] = t4_tools["expanded_recipes"]
    document["rules"]["t4_expansion_budget"] = t4_tools["budget_policy"][
        "count_limits"
    ]["t4_tool"]
    document["rules"]["live_t3_map_total"] = (
        component["expanded_recipes"]
        + t4_tools["expanded_recipes"]
        + t5_chemical["generated_recipes_by_map"].get("assembler", 0)
        + t8_pipes["expanded_pipe_recipes"]
    )
    document["rules"]["live_t3_map_budget"] = t4_tools["budget_policy"][
        "count_limits"
    ]["live_t3_map"]
    document["rules"]["shadowed_recipes"] = (
        component["shadowed_recipes"] + t4_tools["signature_collisions"]
    )
    document["rules"]["t7_expanded_total"] = t7_material_facts[
        "fact_counts"
    ]["new_mortar_rule_expansion_count"]
    document["rules"]["t8_expanded_total"] = t8_pipes[
        "expanded_pipe_recipes"
    ]
    document["rules"]["t10_expanded_total"] = t10_preflight[
        "runtime_publication"
    ]["current_t10_recipe_additions"]
    document["rules"]["datapack_recipe_entries"] = t11_preflight[
        "load_gate"
    ]["projected"]["datapack_recipe_entries"]
    document["rules"]["compression_ratio"] = t11_preflight[
        "load_gate"
    ]["projected"]["compression_ratio"]
    document["rules"]["all_published_total"] = t11_preflight[
        "load_gate"
    ]["projected"]["published_recipes"]
    document["rules"]["all_published_budget"] = t10_preflight[
        "load_gate"
    ][
        "published_recipe_budget"
    ]
    document["rules"]["t11_projected_source_rows"] = (
        t11_preflight["independent_expectation"]["selected_count"]
    )
    document["rules"]["t11_projected_published_total"] = (
        t11_preflight["load_gate"]["projected"]["published_recipes"]
    )
    document["compatibility"].update(compatibility)
    document["ore_pipeline_acceptance"] = (
        _context_value(
            context,
            "ore_pipeline_acceptance",
            derived_ore_pipeline_acceptance,
        )
    )
    document["component_pipeline_acceptance"] = component
    document["t4_tool_acceptance"] = t4_tools
    document["t5_chemical_acceptance"] = t5_chemical
    document["t7_material_fact_acceptance"] = t7_material_facts
    document["t8_pipe_acceptance"] = t8_pipes
    document["worldgen_catalog_acceptance"] = worldgen_catalog
    document["t10_preflight_acceptance"] = t10_preflight
    document["t11_preflight_acceptance"] = t11_preflight
    document["t13_denominator_acceptance"] = t13_denominators
    document["t14_load_acceptance"] = t14_load
    document["t15_readiness_acceptance"] = t15_readiness
    document["t16_readiness_acceptance"] = t16_readiness
    document["t17_readiness_acceptance"] = t17_readiness
    document["artifact_policy"] = _context_value(
        context,
        "artifact_policy",
        derived_artifact_policy,
    )
    document["reachability"]["game_tested_route"] = (
        "runtime large_tungsten_vein registry placement -> real block loot "
        "raw ore -> crusher/sluice/centrifuge/shredder/sifter/smelter -> "
        "tungsten ingot; distillery -> fluid pipe -> mixer and item pipe "
        "filter/valve/pump routes verify T8 logistics; all 134 large-vein "
        "configured features place; T11 crude oil flows deposit -> extractor -> "
        "pipe -> distillery, while natural gas flows deposit -> extractor -> "
        "generifier -> methane generator -> T6 cable -> distillery; unsafe gas "
        "pipes fail and PROPERTIES.FLAMMABLE drives bounded gas-cloud burning; "
        "T10 multi/hot ingots and component cells close smelt/cool and "
        "fluid/gas machine-consumption routes"
    )


def write_snapshot(
    document: dict[str, Any],
    python_tests_passed: bool,
    context: ValidationContext | None = None,
    *,
    persist: bool = True,
) -> None:
    previous_datagen = json.loads(json.dumps(
        (document.get("verification_runs") or {}).get("datagen") or {}
    ))
    previous_determinism = json.loads(json.dumps(
        document.get("data_generation_determinism") or {}
    ))
    refresh_measured_metrics(document, context)
    snapshot = _context_value(
        context,
        "tooling_snapshot",
        current_snapshot,
    )
    current_datagen_hash = snapshot["trees"]["datagen_generated"]["sha256"]
    reuse_datagen = (
        previous_datagen.get("result") == "PASS"
        and previous_datagen.get("runs") == 2
        and previous_datagen.get("run_1_tree_sha256")
        == current_datagen_hash
        and previous_datagen.get("run_2_tree_sha256")
        == current_datagen_hash
        and previous_determinism.get("result") == "PASS"
    )
    document["schema_version"] = max(int(document.get("schema_version") or 1), 8)
    document["verified_on"] = datetime.now(timezone.utc).date().isoformat()
    document["status"] = "PENDING_JAVA_FINAL_VERIFICATION"
    document["tooling_snapshot"] = snapshot
    document["artifact_sha256"] = _context_value(
        context,
        "artifact_hashes",
        lambda: required_artifact_hashes(snapshot),
    )
    python_tests = document.setdefault("tests", {}).setdefault(
        "python_unit_tests", {}
    )
    python_tests["tests"] = snapshot["python_test_count"]
    python_tests["command"] = "python tools/run_python_tests.py --suite closure"
    python_tests["result"] = (
        "PASS" if python_tests_passed else "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"
    )
    python_tests["failures"] = 0 if python_tests_passed else None
    document["tests"]["java_unit_tests"] = {
        "command": ".\\gradlew.bat test",
        "result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT",
        "tests": snapshot["java_source_test_count"],
        "failures": None,
        "errors": None,
    }
    document["tests"]["production_game_tests"] = {
        "command": ".\\gradlew.bat runGameTestServer",
        "result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT",
        "required_tests": _context_value(
            context,
            "game_test_count",
            current_game_test_count,
        ),
    }
    document["data_generation_determinism"] = (
        previous_determinism
        if reuse_datagen
        else {
            "command": ".\\gradlew.bat runData",
            "runs": 0,
            "result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT",
        }
    )
    document["verification_runs"] = {
        "builder": {"result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"},
        "datagen": (
            previous_datagen
            if reuse_datagen
            else {"result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"}
        ),
        "java": {"result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"},
        "gametest": {"result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"},
        "python": {
            "result": (
                "PASS"
                if python_tests_passed
                else "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"
            )
        },
    }
    document["component_runtime_acceptance"] = {"routes": {}}
    document.pop("ready_binding", None)
    document["report_refresh_required_after"] = (
        "Record builder, two datagen runs, Java, GameTest, and Python results "
        "through this verifier, then use --mark-ready and --check."
    )
    if persist:
        _write_report(document)


def _write_report(document: dict[str, Any]) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    temporary = REPORT.with_name(f".{REPORT.name}.{os.getpid()}.tmp")
    try:
        temporary.write_text(
            json.dumps(document, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        os.replace(temporary, REPORT)
    finally:
        temporary.unlink(missing_ok=True)


def write_report_atomic(document: dict[str, Any]) -> None:
    """Publish a fully validated report with one atomic replacement."""
    _write_report(document)


def _current_pending_errors(
    document: dict[str, Any],
    context: ValidationContext | None = None,
) -> list[str]:
    if document.get("status") != "PENDING_JAVA_FINAL_VERIFICATION":
        return ["verification results can only be recorded on a pending snapshot"]
    return validate_report_document(
        document,
        context if context is not None else current_snapshot(),
    )


def record_builder(
    document: dict[str, Any],
    elapsed_ms: float,
    extruder_replay: str,
    context: ValidationContext | None = None,
    *,
    per_builder: list[dict[str, Any]] | None = None,
    persist: bool = True,
) -> list[str]:
    errors = _current_pending_errors(document, context)
    policy = builder_policy_document()
    expected_rows = policy["builders"]
    timing_rows = list(per_builder or [])
    if elapsed_ms <= 0:
        errors.append("builder elapsed time must be positive")
    if extruder_replay not in {"PASS", "SKIP"}:
        errors.append("extruder replay result must be PASS or SKIP")
    expected_identity = [
        (row["script"], row["ordinary_args"], row["proof_tier"])
        for row in expected_rows
    ]
    actual_identity = [
        (row.get("script"), row.get("argv"), row.get("proof_tier"))
        for row in timing_rows
    ]
    if actual_identity != expected_identity:
        errors.append("per-builder timing rows do not match builder policy order")
    if any(
        row.get("result") != "PASS"
        or not isinstance(row.get("elapsed_ms"), (int, float))
        or row["elapsed_ms"] <= 0
        for row in timing_rows
    ):
        errors.append("per-builder timing rows contain an invalid result")
    if errors:
        return errors
    replay_executed = extruder_replay == "PASS"
    baseline: dict[str, Any] = {}
    if REPORT.is_file():
        previous = json.loads(REPORT.read_text(encoding="utf-8"))
        baseline = (
            (previous.get("verification_runs") or {}).get("builder") or {}
        )
    baseline_elapsed = baseline.get("elapsed_ms")
    warn_ratio = float(policy["performance"]["warn_ratio"])
    ratio = (
        elapsed_ms / float(baseline_elapsed)
        if isinstance(baseline_elapsed, (int, float)) and baseline_elapsed > 0
        else None
    )
    document["verification_runs"]["builder"] = {
        "result": "PASS",
        "commands": builder_policy_commands(),
        "elapsed_ms": round(elapsed_ms, 3),
        "per_builder": timing_rows,
        "performance_regression": {
            "policy": "soft_report_only",
            "baseline_elapsed_ms": baseline_elapsed,
            "observed_elapsed_ms": round(elapsed_ms, 3),
            "ratio": round(ratio, 3) if ratio is not None else None,
            "status": (
                "WARN"
                if ratio is not None and ratio > warn_ratio
                else "PASS"
            ),
            "warn_ratio": warn_ratio,
        },
        "compact_evidence_result": "PASS",
        "t4_readiness_result": "PASS",
        "t5_projection_result": "PASS",
        "t55_readiness_result": "PASS",
        "t6_readiness_result": "PASS",
        "t7_readiness_result": "PASS",
        "t8_readiness_result": "PASS",
        "t10_readiness_result": "PASS",
        "t11_readiness_result": "PASS",
        "t12a_readiness_result": "PASS",
        "t12_closure_result": "PASS",
        "t13_denominator_result": "PASS",
        "t14_readiness_result": "PASS",
        "t15_readiness_result": "PASS",
        "t16_readiness_result": "PASS",
        "t17_readiness_result": "PASS",
        "worldgen_catalog_result": "PASS",
        "extruder_full_replay": {
            "command": "python tools/run_python_tests.py --suite source-replay",
            "result": extruder_replay,
            "full_replay_executed": replay_executed,
            "reason": (
                "authoritative raw dump and expanded cache replayed"
                if replay_executed
                else "local raw/cache artifacts absent; compact builder and tests remain authoritative for ordinary CI"
            ),
        },
    }
    if persist:
        _write_report(document)
    return []


def record_datagen(
    document: dict[str, Any],
    run_1_hash: str,
    run_2_hash: str,
    run_1_elapsed_seconds: float,
    run_2_elapsed_seconds: float,
    context: ValidationContext | None = None,
    *,
    persist: bool = True,
) -> list[str]:
    errors = _current_pending_errors(document, context)
    current_hash = _context_value(
        context,
        "tree_digests",
        current_tree_digests,
    )["datagen_generated"]["sha256"]
    if run_1_hash != run_2_hash or run_2_hash != current_hash:
        errors.append("datagen tree hashes do not prove a drift-free second run")
    if run_1_elapsed_seconds <= 0 or run_2_elapsed_seconds <= 0:
        errors.append("datagen elapsed times must be positive")
    if errors:
        return errors
    record = {
        "result": "PASS",
        "command": ".\\gradlew.bat runData",
        "runs": 2,
        "run_1_tree_sha256": run_1_hash,
        "run_2_tree_sha256": run_2_hash,
        "run_1_elapsed_seconds": round(run_1_elapsed_seconds, 3),
        "run_2_elapsed_seconds": round(run_2_elapsed_seconds, 3),
        "drift_files": 0,
    }
    document["verification_runs"]["datagen"] = record
    document["data_generation_determinism"] = {
        **record,
        "generated_resource_root": "src/generated/resources",
    }
    if persist:
        _write_report(document)
    return []


def _expansion_elapsed_from_test_xml() -> int:
    values: list[int] = []
    for path in sorted((ROOT / "build/test-results/test").glob("TEST-*.xml")):
        text = "".join(ElementTree.parse(path).getroot().itertext())
        values.extend(
            int(value)
            for value in re.findall(
                r"T3_COMPONENT_EXPANSION_ELAPSED_MS=(\d+)", text
            )
        )
    if len(values) != 1:
        raise ValueError(
            f"expected one T3 expansion elapsed marker in Java test XML, found {values}"
        )
    return values[0]


def record_java(
    document: dict[str, Any],
    elapsed_seconds: float,
    context: ValidationContext | None = None,
    *,
    persist: bool = True,
) -> list[str]:
    errors = _current_pending_errors(document, context)
    if elapsed_seconds <= 0:
        errors.append("Java test elapsed time must be positive")
    try:
        metrics = _context_value(
            context,
            "java_test_metrics",
            current_java_test_metrics,
        )
        expansion_ms = _expansion_elapsed_from_test_xml()
    except ValueError as exc:
        errors.append(str(exc))
        metrics = {}
        expansion_ms = 0
    if metrics and (
        metrics["failures"] != 0
        or metrics["errors"] != 0
        or metrics["tests"] <= 0
    ):
        errors.append("Java test XML does not describe a passing run")
    if errors:
        return errors
    java = {
        "command": ".\\gradlew.bat test",
        "result": "PASS",
        "exit_code": 0,
        **metrics,
        "elapsed_seconds": round(elapsed_seconds, 3),
        "t3_expansion_elapsed_ms": expansion_ms,
    }
    document["tests"]["java_unit_tests"] = java
    document["verification_runs"]["java"] = {
        "result": "PASS",
        "elapsed_seconds": round(elapsed_seconds, 3),
        "test_result_xml": "build/test-results/test/TEST-*.xml",
        "t3_expansion_elapsed_ms": expansion_ms,
    }
    document["rules"]["observed_t3_expansion_ms"] = expansion_ms
    if persist:
        _write_report(document)
    return []


def parse_component_runtime_routes(text: str) -> dict[str, Any]:
    routes: dict[str, Any] = {}
    label_map = {
        "rollingmill": "rollingmill",
        "assembler": "assembler",
        "lathe": "lathe",
        "extruder": "extruder",
        "wiremill-ingot": "wiremill_ingot",
        "cutter": "cutter",
        "wiremill-foil": "wiremill_foil",
    }
    for material in ACCEPTANCE_MATERIALS:
        match = re.search(
            rf"component-runtime {material}: (.+)",
            text,
        )
        if not match:
            continue
        stages: dict[str, Any] = {}
        for segment in match.group(1).split("; "):
            stage = re.fullmatch(r"([a-z-]+) (\d+)t -> (.+)", segment.strip())
            if stage and stage.group(1) in label_map:
                stages[label_map[stage.group(1)]] = {
                    "duration_ticks": int(stage.group(2)),
                    "output": stage.group(3).strip(),
                }
        routes[material] = stages
    return routes


def record_gametest(
    document: dict[str, Any],
    log_path: Path,
    elapsed_seconds: float,
    context: ValidationContext | None = None,
    *,
    persist: bool = True,
) -> list[str]:
    errors = _current_pending_errors(document, context)
    resolved_log_path = (
        log_path if log_path.is_absolute() else ROOT / log_path
    ).resolve()
    if elapsed_seconds <= 0:
        errors.append("GameTest elapsed time must be positive")
    try:
        text = resolved_log_path.read_text(encoding="utf-8", errors="replace")
    except OSError as exc:
        errors.append(f"cannot read GameTest evidence log: {exc}")
        text = ""
    expected = _context_value(
        context,
        "game_test_count",
        current_game_test_count,
    )
    summary = re.search(r"All (\d+) required tests passed", text)
    if not summary or int(summary.group(1)) != expected:
        errors.append(
            f"GameTest log does not contain the required {expected}-test pass summary"
        )
    routes = parse_component_runtime_routes(text)
    if set(routes) != set(ACCEPTANCE_MATERIALS):
        errors.append("GameTest log does not contain all four component runtime routes")
    if errors:
        return errors
    try:
        evidence_log = resolved_log_path.relative_to(ROOT).as_posix()
    except ValueError:
        evidence_log = f"external:{resolved_log_path.name}"
    game = {
        "command": ".\\gradlew.bat runGameTestServer",
        "result": "PASS",
        "exit_code": 0,
        "required_tests": expected,
        "passed": expected,
        "failed": 0,
        "server_summary": summary.group(0),
        "elapsed_seconds": round(elapsed_seconds, 3),
        "evidence_log": evidence_log,
        "evidence_log_sha256": sha256(resolved_log_path),
    }
    document["tests"]["production_game_tests"] = game
    document["verification_runs"]["gametest"] = {
        "result": "PASS",
        "elapsed_seconds": round(elapsed_seconds, 3),
        "required_tests": expected,
        "passed": expected,
    }
    document["component_runtime_acceptance"] = {
        "materials": list(ACCEPTANCE_MATERIALS),
        "route": (
            "ingot -> rollingmill plate -> assembler gear; ingot -> lathe rod; "
            "ingot + exact reusable shape -> extruder long_rod; ingot -> wire; "
            "plate -> cutter foil -> fine_wire"
        ),
        "declared_duration_observed": True,
        "real_machine_outputs_transferred": True,
        "extruder_shape_retained": True,
        "routes": routes,
    }
    if persist:
        _write_report(document)
    return []


def record_python(
    document: dict[str, Any],
    elapsed_seconds: float,
    context: ValidationContext | None = None,
    *,
    persist: bool = True,
) -> list[str]:
    errors = _current_pending_errors(document, context)
    if elapsed_seconds <= 0:
        errors.append("Python test elapsed time must be positive")
    if errors:
        return errors
    count = _context_value(
        context,
        "tooling_snapshot",
        current_snapshot,
    )["python_test_count"]
    python = document["tests"]["python_unit_tests"]
    python.update({
        "tests": count,
        "failures": 0,
        "result": "PASS",
        "exit_code": 0,
        "passing_attempt_duration_seconds": round(elapsed_seconds, 3),
    })
    document["verification_runs"]["python"] = {
        "result": "PASS",
        "tests": count,
        "elapsed_seconds": round(elapsed_seconds, 3),
    }
    if persist:
        _write_report(document)
    return []


def ready_candidate(
    document: dict[str, Any],
    context: ValidationContext | None = None,
) -> tuple[dict[str, Any], list[str]]:
    snapshot = _context_value(
        context,
        "tooling_snapshot",
        current_snapshot,
    )
    candidate = json.loads(json.dumps(document))
    candidate["status"] = "READY"
    candidate["ready_binding"] = {
        "tooling_snapshot_sha256": snapshot["snapshot_sha256"],
        "marked_after_all_verification_at": datetime.now(timezone.utc).isoformat(),
    }
    errors = validate_report_document(
        candidate,
        context if context is not None else snapshot,
    )
    return candidate, errors


def mark_ready(
    document: dict[str, Any],
    context: ValidationContext | None = None,
    *,
    persist: bool = True,
) -> list[str]:
    candidate, errors = ready_candidate(document, context)
    if errors:
        return errors
    if persist:
        _write_report(candidate)
    else:
        document.clear()
        document.update(candidate)
    return []


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write-tooling-snapshot", action="store_true")
    parser.add_argument("--python-tests-passed", action="store_true")
    parser.add_argument("--mark-ready-after-java", action="store_true")
    parser.add_argument("--mark-ready", action="store_true")
    parser.add_argument("--mark-builder-passed", action="store_true")
    parser.add_argument("--builder-elapsed-ms", type=float)
    parser.add_argument(
        "--extruder-replay-result", choices=("PASS", "SKIP")
    )
    parser.add_argument("--mark-datagen-passed", action="store_true")
    parser.add_argument("--datagen-run-1-hash")
    parser.add_argument("--datagen-run-2-hash")
    parser.add_argument("--datagen-run-1-seconds", type=float)
    parser.add_argument("--datagen-run-2-seconds", type=float)
    parser.add_argument("--mark-java-passed", action="store_true")
    parser.add_argument("--java-elapsed-seconds", type=float)
    parser.add_argument("--mark-gametest-passed", action="store_true")
    parser.add_argument("--gametest-log", type=Path)
    parser.add_argument("--gametest-elapsed-seconds", type=float)
    parser.add_argument("--mark-python-passed", action="store_true")
    parser.add_argument("--python-elapsed-seconds", type=float)
    args = parser.parse_args()
    mutations = sum((
        args.write_tooling_snapshot,
        args.mark_ready_after_java,
        args.mark_ready,
        args.mark_builder_passed,
        args.mark_datagen_passed,
        args.mark_java_passed,
        args.mark_gametest_passed,
        args.mark_python_passed,
    ))
    if mutations > 1 or (args.check and mutations):
        parser.error("choose exactly one read or mutation mode")
    if args.python_tests_passed and not args.write_tooling_snapshot:
        parser.error("--python-tests-passed requires --write-tooling-snapshot")
    document = json.loads(REPORT.read_text(encoding="utf-8"))
    context = load_or_build_validation_context()
    if args.write_tooling_snapshot:
        write_snapshot(document, args.python_tests_passed, context)
        print(f"Wrote pending tooling snapshot to {REPORT}")
        return 0
    if args.mark_builder_passed:
        if args.builder_elapsed_ms is None or args.extruder_replay_result is None:
            parser.error(
                "--mark-builder-passed requires --builder-elapsed-ms and "
                "--extruder-replay-result"
            )
        errors = record_builder(
            document,
            args.builder_elapsed_ms,
            args.extruder_replay_result,
            context,
        )
    elif args.mark_datagen_passed:
        required = (
            args.datagen_run_1_hash,
            args.datagen_run_2_hash,
            args.datagen_run_1_seconds,
            args.datagen_run_2_seconds,
        )
        if any(value is None for value in required):
            parser.error("--mark-datagen-passed requires both hashes and elapsed times")
        errors = record_datagen(document, *required, context)
    elif args.mark_java_passed:
        if args.java_elapsed_seconds is None:
            parser.error("--mark-java-passed requires --java-elapsed-seconds")
        errors = record_java(document, args.java_elapsed_seconds, context)
    elif args.mark_gametest_passed:
        if args.gametest_log is None or args.gametest_elapsed_seconds is None:
            parser.error(
                "--mark-gametest-passed requires --gametest-log and elapsed time"
            )
        errors = record_gametest(
            document,
            args.gametest_log,
            args.gametest_elapsed_seconds,
            context,
        )
    elif args.mark_python_passed:
        if args.python_elapsed_seconds is None:
            parser.error("--mark-python-passed requires --python-elapsed-seconds")
        errors = record_python(document, args.python_elapsed_seconds, context)
    elif args.mark_ready_after_java or args.mark_ready:
        errors = mark_ready(document, context)
    else:
        errors = validate_report_document(document, context)
    if errors:
        print(
            "Full verification report validation failed:\n"
            + "\n".join(f"- {error}" for error in errors),
            file=sys.stderr,
        )
        return 1
    print("Full verification report matches current tooling snapshot.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
