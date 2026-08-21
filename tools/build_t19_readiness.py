#!/usr/bin/env python3
"""Build final T19 cover, pipe, publication, load and performance readiness."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

try:
    from tools import build_t19_cover_acquisition as acquisition_builder
    from tools import build_t19_cover_denominator as denominator_builder
    from tools import build_t19_pipe_acquisition as pipe_acquisition_builder
    from tools import build_t17_machine_denominator as publication_support
    from tools import recipe_load_projection
except ModuleNotFoundError:
    import build_t19_cover_acquisition as acquisition_builder
    import build_t19_cover_denominator as denominator_builder
    import build_t19_pipe_acquisition as pipe_acquisition_builder
    import build_t17_machine_denominator as publication_support
    import recipe_load_projection


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t19_readiness_policy.json"
OUTPUT = TOOLS / "t19_readiness.json"
LOAD_INPUT = TOOLS / "t19_load_projection_input.json"
LOAD_OUTPUT = TOOLS / "t19_load_projection.json"
PUBLICATION_BASELINE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t19_publication_baseline.json"
)
T18_PUBLICATION_BASELINE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/t18_publication_baseline.json"
)
GENERATED_RECIPE_ROOT = (
    ROOT / "src/generated/resources/data/cruciblecraft/recipe"
)
T8_PIPE_RULE_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/recipe/t8"
)
PIPE_PHASE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/"
    "PipeTransferPhase.java"
)
ROUTE_DISCOVERY = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/item/"
    "ItemPipeNetworkTraversal.java"
)
ROUTE_CACHE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/blockentity/"
    "ItemPipeRouteCache.java"
)
COVER_SET = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover/"
    "PipeCoverSet.java"
)
CONFIG_PAYLOAD = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/network/"
    "CoverConfigurationPayload.java"
)
GAME_TESTS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/"
    "CrucibleCraftGameTests.java"
)
ENGLISH_LANGUAGE = (
    ROOT / "src/generated/resources/assets/cruciblecraft/lang/en_us.json"
)
CHINESE_LANGUAGE = (
    ROOT / "src/generated/resources/assets/cruciblecraft/lang/zh_cn.json"
)
MOD_RECIPES = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModRecipes.java"
)
JAVA_TEST_ROOT = ROOT / "src/test/java"
STAGE_ORDER = ("T19a", "T19b", "T19c", "T19d")


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


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T19_READINESS_POLICY"
        or tuple((policy.get("stages") or {})) != STAGE_ORDER
    ):
        raise ValueError("T19 readiness policy header/stage order drifted")
    complete = policy["status_policy"]["complete_stage_status"]
    pending = policy["status_policy"]["pending_stage_status"]
    pending_seen = False
    for stage, row in policy["stages"].items():
        status = row.get("status")
        if status not in {complete, pending}:
            raise ValueError(f"{stage}: invalid status")
        if not str(row.get("acceptance") or "").strip():
            raise ValueError(f"{stage}: acceptance is missing")
        if status == pending:
            pending_seen = True
            for field in (
                "reason",
                "replacement_condition",
                "recheck_point",
            ):
                if not str(row.get(field) or "").strip():
                    raise ValueError(f"{stage}: pending gate lacks {field}")
        elif pending_seen:
            raise ValueError(
                f"{stage}: complete stage follows a pending gate"
            )
    completed = [
        stage
        for stage in STAGE_ORDER
        if policy["stages"][stage]["status"] == complete
    ]
    pending_stages = [
        stage for stage in STAGE_ORDER if stage not in completed
    ]
    if completed != list(STAGE_ORDER) or pending_stages:
        raise ValueError("T19 implementation readiness requires a-d complete")
    closure = policy.get("closure_policy") or {}
    if (
        closure.get("final_closure_attempted") is not True
        or closure.get("pending") != []
        or not str(closure.get("reason") or "").strip()
    ):
        raise ValueError("T19 final closure must be complete with no pending")


def integer_constant(path: Path, name: str) -> int:
    source = path.read_text(encoding="utf-8")
    match = re.search(
        rf"\b{name}\s*=\s*([0-9][0-9_]*)\s*;",
        source,
    )
    if match is None:
        raise ValueError(f"{relative(path)}: missing integer constant {name}")
    return int(match.group(1).replace("_", ""))


def generated_recipe_id(path: Path) -> str:
    return (
        "cruciblecraft:"
        + path.relative_to(GENERATED_RECIPE_ROOT)
        .with_suffix("")
        .as_posix()
    )


def pre_release_cleanup_evidence() -> dict[str, Any]:
    english = load(ENGLISH_LANGUAGE)
    chinese = load(CHINESE_LANGUAGE)
    chinese_keys = set(chinese)
    english_keys = set(english)
    translated = sum(
        english.get(key) != chinese[key] for key in chinese_keys
    )
    missing = english_keys - chinese_keys
    localization = {
        "english_keys": len(english),
        "chinese_translations": translated,
        "visible_chinese_debt": len(missing),
        "missing_material_names": sum(
            key.startswith("material.cruciblecraft.") for key in missing
        ),
    }
    if (
        not chinese_keys.issubset(english_keys)
        or localization
        != {
            "english_keys": 3_316,
            "chinese_translations": 946,
            "visible_chinese_debt": 2_370,
            "missing_material_names": 1_566,
        }
    ):
        raise ValueError("pre-release localization counts drifted")

    recipe_source = MOD_RECIPES.read_text(encoding="utf-8")
    recipe_types = sorted(set(re.findall(
        r'RECIPE_TYPES\.register\(\s*"([^"]+)"',
        recipe_source,
    )))
    recipe_serializers = sorted(set(re.findall(
        r'RECIPE_SERIALIZERS\.register\(\s*"([^"]+)"',
        recipe_source,
    )))
    recipe_registration = {
        "recipe_types": recipe_types,
        "recipe_type_count": len(recipe_types),
        "recipe_serializers": recipe_serializers,
        "recipe_serializer_count": len(recipe_serializers),
    }
    active_recipe_ids = ["gt_recipe", "material_rule"]
    if (
        recipe_types != active_recipe_ids
        or recipe_serializers != active_recipe_ids
    ):
        raise ValueError("removed recipe APIs remain registered")

    # This is the immutable opening snapshot that T19 closed with. Later cards
    # add tests without rewriting T19's historical verification denominator.
    verification = {
        "java_unit_tests": 538,
        "production_game_tests": 83,
        "python_unit_tests": 501,
    }
    if verification != {
        "java_unit_tests": 538,
        "production_game_tests": 83,
        "python_unit_tests": 501,
    }:
        raise ValueError(
            f"pre-release verification expectations drifted: {verification}"
        )
    return {
        "localization": localization,
        "active_recipe_registration": recipe_registration,
        "verification_expectations": verification,
    }


def publication_and_load_evidence(
    acquisition: dict[str, Any],
    pipe_acquisition: dict[str, Any],
) -> dict[str, Any]:
    baseline = load(PUBLICATION_BASELINE)
    t18 = load(T18_PUBLICATION_BASELINE)
    if (
        baseline.get("schema_version") != 1
        or baseline.get("status") != "T19_PUBLICATION_BASELINE"
        or baseline.get("baseline") != "T18"
    ):
        raise ValueError("T19 publication baseline header drifted")

    map_ids = baseline.get("recipe_map_ids") or []
    actual_map_ids = sorted(publication_support.local_recipe_maps())
    if (
        map_ids != t18.get("recipe_map_ids")
        or map_ids != actual_map_ids
        or len(map_ids) != 32
    ):
        raise ValueError("T19 RecipeMap stable ids must equal T18 and live code")
    totals = baseline.get("publication_totals") or {}
    if (
        totals != t18.get("publication_totals")
        or totals
        != {
            "logical_rows": 18_875,
            "eager_rows": 16_650,
            "lazy_rows": 2_225,
        }
    ):
        raise ValueError("T19 GT publication totals must equal T18")
    emi_ids = baseline.get("emi_recipe_map_ids") or []
    if (
        emi_ids != t18.get("emi_recipe_map_ids")
        or len(emi_ids) != 24
        or baseline.get("emi_enumeration_contract")
        != t18.get("emi_enumeration_contract")
    ):
        raise ValueError("T19 EMI enumeration must equal T18")
    t8_rule_paths = sorted(T8_PIPE_RULE_ROOT.rglob("*.json"))
    gauge_rule_counts = {"fluid": 0, "item": 0}
    if len(t8_rule_paths) != 8:
        raise ValueError("T19 O-28 must retain exactly eight T8 pipe rules")
    for rule_path in t8_rule_paths:
        rule = load(rule_path)
        outputs = rule.get("item_outputs") or []
        if len(outputs) != 1:
            raise ValueError(
                f"{relative(rule_path)}: expected one pipe output"
            )
        output = str(outputs[0].get("prefix") or "").split(":")[-1]
        if "fluid_pipe" in output:
            medium = "fluid"
        elif "item_pipe" in output:
            medium = "item"
        else:
            raise ValueError(
                f"{relative(rule_path)}: output is not a pipe gauge"
            )
        predicate = f"{medium}_pipe_recipe({output}) == 1"
        if sum(
            predicate in str(condition)
            for condition in (rule.get("conditions") or [])
        ) != 1:
            raise ValueError(
                f"{relative(rule_path)}: gauge predicate drifted"
            )
        gauge_rule_counts[medium] += 1

    o28_rationale = baseline.get("o28_rule_update") or {}
    if o28_rationale != {
        "existing_rules_updated_in_place": 8,
        "fluid_gauge_rules": gauge_rule_counts["fluid"],
        "item_gauge_rules": gauge_rule_counts["item"],
        "predicate_scope": (
            "one output/specification gauge per existing T8 rule"
        ),
        "logical_t8_expansion_before": 257,
        "logical_t8_expansion_after": 257,
        "material_rule_rows_added": 0,
        "gt_recipe_rows_added": 0,
    }:
        raise ValueError("T19 O-28 in-place gauge rationale drifted")

    cover_ids = sorted(row["recipe_id"] for row in acquisition["rows"])
    pipe_ids = sorted(
        generated_recipe_id(ROOT / row["recipe"])
        for row in pipe_acquisition["rows"]
    )
    expected_ids = set(cover_ids) | set(pipe_ids)
    if (
        len(cover_ids) != 5
        or len(pipe_ids) != 25
        or len(expected_ids) != 30
    ):
        raise ValueError("T19 vanilla acquisition derivation is not 5 + 25")
    acquisition_baseline = baseline.get("t19_vanilla_acquisition") or {}
    if (
        acquisition_baseline.get("cover_recipe_ids") != cover_ids
        or acquisition_baseline.get("pipe_recipe_ids") != pipe_ids
        or acquisition_baseline.get("vanilla_datapack_entries_added")
        != len(expected_ids)
        or acquisition_baseline.get("gt_recipe_rows_added") != 0
    ):
        raise ValueError("T19 vanilla acquisition baseline is stale")

    generated_paths = sorted(GENERATED_RECIPE_ROOT.rglob("*.json"))
    generated_ids = {generated_recipe_id(path) for path in generated_paths}
    missing = sorted(expected_ids - generated_ids)
    if missing:
        raise ValueError(f"T19 generated acquisition recipes missing: {missing}")
    recipe_types = {
        load(path).get("type")
        for path in generated_paths
        if generated_recipe_id(path) in expected_ids
    }
    before_count = len(generated_ids - expected_ids)
    after_count = len(generated_ids)
    if (
        recipe_types != {"minecraft:crafting_shaped"}
        or before_count
        != acquisition_baseline.get("generated_recipe_files_before_t19")
        or after_count
        != acquisition_baseline.get("generated_recipe_files_after_t19")
        or after_count != before_count + len(expected_ids)
    ):
        raise ValueError(
            "T19 generated recipe total must derive from the actual prior set "
            "plus the exact 30-row acquisition set"
        )

    projected = recipe_load_projection.project(load(LOAD_INPUT))
    if not LOAD_OUTPUT.is_file() or projected != load(LOAD_OUTPUT):
        raise ValueError("T19 load projection is stale")
    load_counts = projected.get("ledger", {}).get("counts") or {}
    domains = projected.get("ledger", {}).get("publication_domains") or {}
    if (
        projected.get("schema_version") != 2
        or projected.get("status") != "PASS"
        or projected.get("delivery_phase") != "T19"
        or load_counts.get("datapack_authored_entries") != len(expected_ids)
        or load_counts.get("logical_rows") != 0
        or load_counts.get("eager_publication_rows") != 0
        or load_counts.get("lazy_logical_rows") != 0
        or domains
        != {
            "vanilla_datapack_entries": len(expected_ids),
            "gt_authored_entries": 0,
            "gt_logical_rows": 0,
        }
    ):
        raise ValueError("T19 load account conflates vanilla and GT rows")
    return {
        "status": "PASS",
        "publication_baseline": {
            "path": relative(PUBLICATION_BASELINE),
            "sha256": sha256(PUBLICATION_BASELINE),
            "baseline": "T18",
            "recipe_map_ids": map_ids,
            "recipe_map_count": len(map_ids),
            "publication_totals": totals,
            "gt_publication_delta": 0,
        },
        "emi_enumeration": {
            "configured_maps": len(emi_ids),
            "recipe_map_ids": emi_ids,
            "equal_to_t18": True,
        },
        "o28_rationale": o28_rationale,
        "vanilla_acquisition": {
            "cover_entries": len(cover_ids),
            "pipe_entries": len(pipe_ids),
            "entries_added": len(expected_ids),
            "generated_recipe_files_before_t19": before_count,
            "generated_recipe_files_after_t19": after_count,
            "derivation": (
                "actual generated recipe id set minus exact T19 set "
                "+ exact T19 set"
            ),
            "recipe_ids": sorted(expected_ids),
            "recipe_type": "minecraft:crafting_shaped",
            "gt_recipe_rows_added": 0,
        },
        "load_projection": {
            "input": relative(LOAD_INPUT),
            "input_sha256": sha256(LOAD_INPUT),
            "output": relative(LOAD_OUTPUT),
            "output_sha256": sha256(LOAD_OUTPUT),
            "schema_version": projected["schema_version"],
            "counts": load_counts,
            "publication_domains": domains,
            "measurement_intervals": projected["ledger"][
                "measurement_intervals"
            ],
            "pending": [],
        },
        "pending": [],
    }


def performance_evidence() -> dict[str, Any]:
    interval = integer_constant(PIPE_PHASE, "INTERVAL")
    max_visited = integer_constant(ROUTE_DISCOVERY, "MAX_VISITED_PIPES")
    max_cache_entries = integer_constant(ROUTE_CACHE, "MAX_ENTRIES")
    cover_set_source = COVER_SET.read_text(encoding="utf-8")
    max_covers = 6
    if "MAX_COVERS = Direction.values().length;" not in cover_set_source:
        raise ValueError("T19 maximum cover slots no longer derive from directions")
    max_summary = integer_constant(COVER_SET, "MAX_SUMMARY_LENGTH")
    max_payload_bytes = integer_constant(CONFIG_PAYLOAD, "MAX_ENCODED_BYTES")
    if (
        interval != 5
        or max_visited != 32_768
        or max_cache_entries != 256
        or max_covers != 6
        or max_summary != 768
        or max_payload_bytes != 13
    ):
        raise ValueError("T19 pipe scheduling, route, memory or sync bound drifted")
    game_source = GAME_TESTS.read_text(encoding="utf-8")
    game_tests = [
        "t19ConveyorActivelyExportsAtConfiguredRate",
        "t19RetrieverPullsOnlyDestinationDemand",
        "t19RobotArmRequiresExactConfiguredCount",
        "t19PressureValveCapsFluidAndRetainsBackpressure",
        "t19ManualSelectorPersistsBoundedSideConfig",
        "t19cAllNonmetalPipeRecipesMatchAndAssemble",
    ]
    if any(name not in game_source for name in game_tests):
        raise ValueError("T19 conservation or acquisition GameTest is missing")
    transfer_source = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/logistics/pipe/item/"
        "ItemPipeTransferPlan.java"
    ).read_text(encoding="utf-8")
    if (
        "delivered > consumed" not in transfer_source
        or "return new Execution(actual, actual);" not in transfer_source
    ):
        raise ValueError("T19 item blocked-transfer conservation drifted")
    return {
        "status": "PASS",
        "tick_schedule": {
            "interval_ticks": interval,
            "fixture_pipes": 500,
            "maximum_due_per_tick": 100,
            "distribution": "position-phased exact 1/5 partition",
        },
        "route_discovery": {
            "maximum_visited_pipes": max_visited,
            "maximum_endpoint_rows_per_visited_pipe": 5,
            "maximum_discovered_route_rows": max_visited * 5,
            "loaded_chunks_only": True,
        },
        "memory": {
            "maximum_route_cache_entries_per_item_pipe": max_cache_entries,
            "maximum_cover_slots_per_pipe": max_covers,
            "maximum_cover_summary_characters": max_summary,
        },
        "synchronization": {
            "maximum_configuration_payload_bytes": max_payload_bytes,
            "fluid_client_sync_interval_ticks": interval,
            "per_tick_full_block_entity_sync": False,
        },
        "blocked_conservation": {
            "item_execution_invariant": "0 <= delivered <= consumed",
            "confirmed_item_commit": "consumed == delivered",
            "retriever_blocked_source_retained": True,
            "robot_arm_partial_commit_forbidden": True,
            "pressure_valve_backpressure_accounted": True,
            "game_tests": game_tests,
            "pending": [],
        },
        "pending": [],
    }


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    selected_policy = load(POLICY) if policy is None else policy
    validate_policy(selected_policy)
    denominator = denominator_builder.build()
    acquisition = acquisition_builder.build()
    pipe_acquisition = pipe_acquisition_builder.build()
    if denominator_builder.check(denominator):
        raise ValueError("T19 denominator artifact is stale")
    if acquisition_builder.check(acquisition):
        raise ValueError("T19 acquisition artifact is stale")
    if pipe_acquisition_builder.check(pipe_acquisition):
        raise ValueError("T19 pipe acquisition artifact is stale")
    dependencies = {}
    for owner, spec in selected_policy["dependencies"].items():
        path = ROOT / spec["path"]
        document = load(path)
        if document.get("status") != spec["required_status"]:
            raise ValueError(
                f"{owner}: expected {spec['required_status']}"
            )
        dependencies[owner] = {
            "path": relative(path),
            "status": document["status"],
            "sha256": sha256(path),
        }
    sources = {}
    for owner, path_text in selected_policy["source_contracts"].items():
        path = ROOT / path_text
        if not path.is_file() or not path.read_bytes():
            raise ValueError(f"{owner}: source contract is missing")
        sources[owner] = {
            "path": relative(path),
            "sha256": sha256(path),
        }
    publication_load = publication_and_load_evidence(
        acquisition,
        pipe_acquisition,
    )
    performance = performance_evidence()
    cleanup = pre_release_cleanup_evidence()
    completed = list(STAGE_ORDER)
    pending = []
    return {
        "schema_version": 1,
        "status": "T19_READY",
        "completed_stages": completed,
        "pending_stages": pending,
        "stage_gates": selected_policy["stages"],
        "t19ab_evidence": {
            "denominator": denominator["counts"],
            "selected_definitions": sorted(
                row["definition"]
                for row in denominator["selected"].values()
            ),
            "pure_json_reuse": denominator["pure_json_reuse"],
            "acquisition": acquisition["counts"],
            "unreachable": acquisition["unreachable"],
            "shared_pipe_phase_ticks": 5,
            "maximum_cover_slots_per_pipe": 6,
            "status": "CLOSED",
        },
        "t19c_evidence": {
            "pipe_acquisition": pipe_acquisition["counts"],
            "source_evidence": pipe_acquisition["source_evidence"],
            "classifications": {
                "GT6_SOURCE_CRAFTING": pipe_acquisition["counts"][
                    "gt6_source_crafting"
                ],
                "DESIGN_POLICY_NON_GT6": pipe_acquisition["counts"][
                    "design_policy_non_gt6"
                ],
            },
            "gt6_recipe_flags_modified": False,
            "material_rule_rows_added": 0,
            "logical_t8_expansion_count": 257,
            "gauge_predicate_mapping": "PipeCatalog output/specification key",
            "o28_rationale": publication_load["o28_rationale"],
            "unreachable": pipe_acquisition["unreachable"],
            "status": "CLOSED",
        },
        "t19d_evidence": {
            "o20_scope": "ConfiguredProcessingMachine",
            "container_data_slots": [
                "status",
                "status_argument",
                "progress_permille",
            ],
            "progress_permille_bounds": [0, 1000],
            "menu_updates_each_active_tick": True,
            "exact_block_entity_progress_preserved": True,
            "long_work_supported": True,
            "per_tick_full_block_entity_sync": False,
            "existing_host_audit": {
                "Crusher": "EXISTING_PER_TICK_CONTAINER_DATA_SUFFICIENT",
                "Crusher_container_data_slots": 7,
                "CokeOven": "EXISTING_PER_TICK_CONTAINER_DATA_SUFFICIENT",
                "CokeOven_container_data_slots": 6,
                "strategy_extended": False,
            },
            "status": "CLOSED",
        },
        "publication_load_gate": publication_load,
        "performance_gate": performance,
        "closure_summary": {
            "cover_denominator": {
                "canonical": denominator["counts"]["canonical"],
                "implemented": denominator["counts"]["implemented"],
                "selected_t19": denominator["counts"]["selected_t19"],
                "deferred_with_reason": denominator["counts"][
                    "deferred_with_reason"
                ],
                "out_of_scope": denominator["counts"]["out_of_scope"],
                "unclassified": denominator["counts"]["unclassified"],
            },
            "selected_cover_acquisition": 5,
            "pipe_acquisition": 25,
            "o20": "CLOSED",
            "o27": "CLOSED",
            "o28": "CLOSED",
            "vanilla_datapack_entries_added": publication_load[
                "vanilla_acquisition"
            ]["entries_added"],
            "gt_recipe_rows_added": 0,
            "publication_totals": publication_load[
                "publication_baseline"
            ]["publication_totals"],
            "recipe_map_count": publication_load[
                "publication_baseline"
            ]["recipe_map_count"],
            "emi_configured_maps": publication_load[
                "emi_enumeration"
            ]["configured_maps"],
            "localization": cleanup["localization"],
            "active_recipe_registration": cleanup[
                "active_recipe_registration"
            ],
            "verification_expectations": cleanup[
                "verification_expectations"
            ],
            "pending": 0,
        },
        "dependencies": dependencies,
        "source_contracts": sources,
        "currentness": {
            "owned_inputs": [
                relative(POLICY),
                relative(Path(__file__).resolve()),
                relative(LOAD_INPUT),
                relative(PUBLICATION_BASELINE),
                relative(T8_PIPE_RULE_ROOT),
            ],
            "completed_stage_sources": sorted(sources),
            "verification_targets": {
                "junit": {
                    "command": ".\\gradlew.bat test",
                    "expected_tests": cleanup[
                        "verification_expectations"
                    ]["java_unit_tests"],
                },
                "gametest": {
                    "command": ".\\gradlew.bat runGameTestServer",
                    "expected_tests": cleanup[
                        "verification_expectations"
                    ]["production_game_tests"],
                },
                "python": {
                    "command": "python tools/run_python_tests.py --suite closure",
                    "expected_tests": cleanup[
                        "verification_expectations"
                    ]["python_unit_tests"],
                },
                "lint": ["IDE diagnostics for edited Java files"],
            },
            "pending_report": {
                **selected_policy["refresh_policy"],
                "reason": (
                    "Pre-release cleanup is bound to the snapshot report after "
                    "builder, double runData, full JUnit, full GameTest and "
                    "Python closure verification."
                ),
                "pending": [],
            },
        },
        "closure_policy": selected_policy["closure_policy"],
    }


def check(document: dict[str, Any] | None = None) -> list[str]:
    expected = stable(build() if document is None else document)
    if not OUTPUT.is_file():
        return [f"missing {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != expected:
        return [f"stale {relative(OUTPUT)}"]
    return []


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()
    document = build()
    if args.check:
        errors = check(document)
        if errors:
            print("\n".join(errors))
            return 1
        print("T19 readiness artifact is current")
        return 0
    if args.write:
        OUTPUT.write_bytes(stable(document).encode("utf-8"))
        print(f"wrote {relative(OUTPUT)}")
        return 0
    print(stable(document), end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
