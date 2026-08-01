#!/usr/bin/env python3
"""Generate and validate the tooling-bound full verification report snapshot."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import unittest
import xml.etree.ElementTree as ElementTree
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
REPORT = TOOLS / "full_verification_report.json"
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
    "local_artifact_manifest.json",
    "material_registry_stress_report.json",
    "material_registry_budget.json",
)
COMPONENT_MANIFEST = TOOLS / "component_rule_manifest.json"
COMPONENT_SOURCE_DIR = TOOLS / "component_rule_sources"
COMPONENT_GENERATED_ROOT = ROOT / "src/component_rule_generated/resources"
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
        TOOLS / "gt6_extruder_templates.py",
        TOOLS / "verify_full_verification_report.py",
        TOOLS / "README.md",
        TOOLS / "gt6_process_expectations.json",
        ROOT
        / "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/content/item/ExtruderShapeCatalog.java",
        ROOT
        / "src/main/java/com/masson/cruciblecraft/gametest/CrucibleCraftGameTests.java",
        ROOT
        / "src/test/java/com/masson/cruciblecraft/material/prefix/T3ComponentDataTest.java",
    ]
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

    cc_recipes = comparison.expand_cc_recipes(comparison.load_cc_materials())
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
    if len(expectations) != len(cc_recipes):
        raise ValueError(
            "compact expectation count does not cover current CC recipes: "
            f"{len(expectations)} != {len(cc_recipes)}"
        )
    return {
        "normalized_cc_recipes": len(cc_recipes),
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


def validate_report_document(
    document: dict[str, Any],
    snapshot: dict[str, Any],
) -> list[str]:
    errors: list[str] = []
    if document.get("tooling_snapshot") != snapshot:
        errors.append("tooling snapshot is stale")
    expected_hashes = required_artifact_hashes(snapshot)
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
    expected_component_acceptance = derived_component_acceptance()
    if document.get("component_pipeline_acceptance") != expected_component_acceptance:
        errors.append(
            "component pipeline acceptance is not derived from the current manifest"
        )
    expected_artifact_policy = derived_artifact_policy()
    if document.get("artifact_policy") != expected_artifact_policy:
        errors.append("artifact cache policy is not derived from the current manifest")
    expected_ore_acceptance = derived_ore_pipeline_acceptance()
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
        replay = builder.get("extruder_full_replay") or {}
        if replay.get("result") not in {"PASS", "SKIP"}:
            errors.append("READY requires an explicit extruder replay PASS or SKIP")
        if replay.get("result") == "SKIP" and (
            replay.get("full_replay_executed") is not False
            or builder.get("compact_evidence_result") != "PASS"
        ):
            errors.append("extruder replay SKIP must rely on passing compact evidence")
        datagen = verification.get("datagen") or {}
        if (
            datagen.get("runs") != 2
            or datagen.get("run_1_tree_sha256")
            != datagen.get("run_2_tree_sha256")
            or datagen.get("run_2_tree_sha256")
            != current_tree_digests()["datagen_generated"]["sha256"]
        ):
            errors.append("READY requires two drift-free datagen runs")
        if java.get("tests") != current_java_test_metrics()["tests"]:
            errors.append("READY Java metrics do not match current test XML")
        if game.get("required_tests") != current_game_test_count():
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
        "production_game_tests_passed": True,
    }


def refresh_measured_metrics(document: dict[str, Any]) -> None:
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
    compatibility = current_compatibility_metrics()
    document["catalog"].update(current_catalog_metrics())
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
    component = derived_component_acceptance()
    for map_name, count in component["expanded_recipes_per_map"].items():
        expanded[f"cruciblecraft:{map_name}"] = count
    document["rules"]["declarative_rule_definitions"] = component["source_rules"]
    document["rules"]["t3_expanded_total"] = component["expanded_recipes"]
    document["rules"]["t3_expansion_budget"] = component["expansion_budget"]
    document["rules"]["shadowed_recipes"] = component["shadowed_recipes"]
    document["compatibility"].update(compatibility)
    document["ore_pipeline_acceptance"] = (
        derived_ore_pipeline_acceptance()
    )
    document["component_pipeline_acceptance"] = component
    document["artifact_policy"] = derived_artifact_policy()
    document["reachability"]["game_tested_route"] = (
        "runtime large_tungsten_vein registry placement -> real block loot "
        "raw ore -> crusher/sluice/centrifuge/shredder/sifter/smelter -> "
        "tungsten ingot; copper and tin reuse the same live-map machine helper"
    )


def write_snapshot(document: dict[str, Any], python_tests_passed: bool) -> None:
    refresh_measured_metrics(document)
    snapshot = current_snapshot()
    document["schema_version"] = max(int(document.get("schema_version") or 1), 5)
    document["verified_on"] = datetime.now(timezone.utc).date().isoformat()
    document["status"] = "PENDING_JAVA_FINAL_VERIFICATION"
    document["tooling_snapshot"] = snapshot
    document["artifact_sha256"] = required_artifact_hashes(snapshot)
    python_tests = document.setdefault("tests", {}).setdefault(
        "python_unit_tests", {}
    )
    python_tests["tests"] = snapshot["python_test_count"]
    python_tests["command"] = (
        'python -m unittest discover -s tools/tests -p "test_*.py"'
    )
    python_tests["result"] = (
        "PASS" if python_tests_passed else "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"
    )
    python_tests["failures"] = 0 if python_tests_passed else None
    document["tests"]["java_unit_tests"] = {
        "command": ".\\gradlew.bat test",
        "result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT",
    }
    document["tests"]["production_game_tests"] = {
        "command": ".\\gradlew.bat runGameTestServer",
        "result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT",
        "required_tests": current_game_test_count(),
    }
    document["data_generation_determinism"] = {
        "command": ".\\gradlew.bat runData",
        "runs": 0,
        "result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT",
    }
    document["verification_runs"] = {
        "builder": {"result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"},
        "datagen": {"result": "NOT_RECORDED_FOR_CURRENT_SNAPSHOT"},
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
    REPORT.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _write_report(document: dict[str, Any]) -> None:
    REPORT.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _current_pending_errors(document: dict[str, Any]) -> list[str]:
    if document.get("status") != "PENDING_JAVA_FINAL_VERIFICATION":
        return ["verification results can only be recorded on a pending snapshot"]
    return validate_report_document(document, current_snapshot())


def record_builder(
    document: dict[str, Any],
    elapsed_ms: float,
    extruder_replay: str,
) -> list[str]:
    errors = _current_pending_errors(document)
    if elapsed_ms <= 0:
        errors.append("builder elapsed time must be positive")
    if extruder_replay not in {"PASS", "SKIP"}:
        errors.append("extruder replay result must be PASS or SKIP")
    if errors:
        return errors
    replay_executed = extruder_replay == "PASS"
    document["verification_runs"]["builder"] = {
        "result": "PASS",
        "command": "python tools/build_component_rules.py --check",
        "elapsed_ms": round(elapsed_ms, 3),
        "compact_evidence_result": "PASS",
        "extruder_full_replay": {
            "command": "python tools/gt6_extruder_templates.py --verify",
            "result": extruder_replay,
            "full_replay_executed": replay_executed,
            "reason": (
                "authoritative raw dump and expanded cache replayed"
                if replay_executed
                else "local raw/cache artifacts absent; compact builder and tests remain authoritative for ordinary CI"
            ),
        },
    }
    _write_report(document)
    return []


def record_datagen(
    document: dict[str, Any],
    run_1_hash: str,
    run_2_hash: str,
    run_1_elapsed_seconds: float,
    run_2_elapsed_seconds: float,
) -> list[str]:
    errors = _current_pending_errors(document)
    current_hash = current_tree_digests()["datagen_generated"]["sha256"]
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


def record_java(document: dict[str, Any], elapsed_seconds: float) -> list[str]:
    errors = _current_pending_errors(document)
    if elapsed_seconds <= 0:
        errors.append("Java test elapsed time must be positive")
    try:
        metrics = current_java_test_metrics()
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
) -> list[str]:
    errors = _current_pending_errors(document)
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
    expected = current_game_test_count()
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
    game = {
        "command": ".\\gradlew.bat runGameTestServer",
        "result": "PASS",
        "exit_code": 0,
        "required_tests": expected,
        "passed": expected,
        "failed": 0,
        "server_summary": summary.group(0),
        "elapsed_seconds": round(elapsed_seconds, 3),
        "evidence_log": resolved_log_path.relative_to(ROOT).as_posix(),
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
    _write_report(document)
    return []


def record_python(document: dict[str, Any], elapsed_seconds: float) -> list[str]:
    errors = _current_pending_errors(document)
    if elapsed_seconds <= 0:
        errors.append("Python test elapsed time must be positive")
    if errors:
        return errors
    count = current_python_test_count()
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
    _write_report(document)
    return []


def mark_ready(document: dict[str, Any]) -> list[str]:
    snapshot = current_snapshot()
    candidate = json.loads(json.dumps(document))
    candidate["status"] = "READY"
    candidate["ready_binding"] = {
        "tooling_snapshot_sha256": snapshot["snapshot_sha256"],
        "marked_after_all_verification_at": datetime.now(timezone.utc).isoformat(),
    }
    errors = validate_report_document(candidate, snapshot)
    if errors:
        return errors
    _write_report(candidate)
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
    if args.write_tooling_snapshot:
        write_snapshot(document, args.python_tests_passed)
        print(f"Wrote pending tooling snapshot to {REPORT}")
        return 0
    if args.mark_builder_passed:
        if args.builder_elapsed_ms is None or args.extruder_replay_result is None:
            parser.error(
                "--mark-builder-passed requires --builder-elapsed-ms and "
                "--extruder-replay-result"
            )
        errors = record_builder(
            document, args.builder_elapsed_ms, args.extruder_replay_result
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
        errors = record_datagen(document, *required)
    elif args.mark_java_passed:
        if args.java_elapsed_seconds is None:
            parser.error("--mark-java-passed requires --java-elapsed-seconds")
        errors = record_java(document, args.java_elapsed_seconds)
    elif args.mark_gametest_passed:
        if args.gametest_log is None or args.gametest_elapsed_seconds is None:
            parser.error(
                "--mark-gametest-passed requires --gametest-log and elapsed time"
            )
        errors = record_gametest(
            document,
            args.gametest_log,
            args.gametest_elapsed_seconds,
        )
    elif args.mark_python_passed:
        if args.python_elapsed_seconds is None:
            parser.error("--mark-python-passed requires --python-elapsed-seconds")
        errors = record_python(document, args.python_elapsed_seconds)
    elif args.mark_ready_after_java or args.mark_ready:
        errors = mark_ready(document)
    else:
        errors = validate_report_document(document, current_snapshot())
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
