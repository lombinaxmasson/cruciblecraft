#!/usr/bin/env python3
"""Build T20 worldgen closure, fidelity and load readiness."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

try:
    from tools import build_t20_worldgen_projection as projection_builder
    from tools import build_t20_worldgen_source as source_builder
    from tools import build_worldgen_catalog as catalog_builder
except ModuleNotFoundError:
    import build_t20_worldgen_projection as projection_builder
    import build_t20_worldgen_source as source_builder
    import build_worldgen_catalog as catalog_builder


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t20_readiness_policy.json"
OUTPUT = TOOLS / "t20_readiness.json"
T19 = TOOLS / "t19_readiness.json"
SOURCE = TOOLS / "t20_gt6_worldgen_source.json"
EXPECTED = TOOLS / "t20_worldgen_expected.json"
WORLDGEN_READINESS = TOOLS / "worldgen_catalog_readiness.json"
AUTHORING_SCHEMA = (
    ROOT
    / "src/main/resources/data/cruciblecraft/schema/"
    "worldgen_ore_veins.schema.json"
)
CONFIGURATION = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/worldgen/"
    "LargeVeinConfiguration.java"
)
GAME_TESTS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest/"
    "CrucibleCraftGameTests.java"
)
CONFIGURATION_TEST = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/worldgen/"
    "LargeVeinConfigurationTest.java"
)
RESOURCE_TEST = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/worldgen/"
    "WorldgenCatalogResourceTest.java"
)
PYTHON_PROJECTION_TEST = (
    TOOLS / "tests/test_build_t20_worldgen_projection.py"
)
PHASE4_PLAN = ROOT / "CrucibleCraft-第四阶段总体规划.md"
T20_ARCHIVE = ROOT / "CrucibleCraft-阶段档案-T20.md"
PHASE4_CONTRACT = TOOLS / "phase4_v1_planning_contract.json"
STAGE_ORDER = ("T20a", "T20b", "T20c", "T20d", "T20e")


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
        or policy.get("status") != "T20_READINESS_POLICY"
        or tuple((policy.get("stages") or {})) != STAGE_ORDER
    ):
        raise ValueError("T20 readiness policy header/stage order drifted")
    for stage in STAGE_ORDER:
        row = policy["stages"][stage]
        if row.get("status") != "COMPLETE":
            raise ValueError(f"{stage}: stage is not complete")
        if not str(row.get("acceptance") or "").strip():
            raise ValueError(f"{stage}: acceptance is missing")
    closure = policy.get("closure_policy") or {}
    if (
        closure.get("final_closure_attempted") is not True
        or closure.get("pending") != []
        or not str(closure.get("reason") or "").strip()
    ):
        raise ValueError("T20 closure policy is incomplete")


def dependency_evidence(
    policy: dict[str, Any],
    source: dict[str, Any],
    expected: dict[str, Any],
    worldgen: dict[str, Any],
) -> dict[str, Any]:
    t19 = load(T19)
    dependencies = {
        "T19": {
            "path": relative(T19),
            "status": t19.get("status"),
            "required_status": policy["dependencies"]["T19"][
                "required_status"
            ],
            "sha256": sha256(T19),
        },
        "source": {
            "path": relative(SOURCE),
            "status": source.get("status"),
            "required_status": policy["dependencies"]["source"][
                "required_status"
            ],
            "sha256": sha256(SOURCE),
        },
        "expected": {
            "path": relative(EXPECTED),
            "status": expected.get("status"),
            "required_status": policy["dependencies"]["expected"][
                "required_status"
            ],
            "sha256": sha256(EXPECTED),
        },
        "runtime_catalog": {
            "path": relative(WORLDGEN_READINESS),
            "geometry_status": worldgen["geometry_policy"]["status"],
            "required_geometry_status": policy["dependencies"][
                "runtime_catalog"
            ]["required_geometry_status"],
            "sha256": sha256(WORLDGEN_READINESS),
        },
    }
    for name, row in dependencies.items():
        actual = row.get("status", row.get("geometry_status"))
        required = row.get(
            "required_status", row.get("required_geometry_status")
        )
        if actual != required:
            raise ValueError(
                f"T20 dependency {name} is {actual}, expected {required}"
            )
    return dependencies


def generated_runtime_evidence(
    expected: dict[str, Any],
    generated: dict[str, str],
) -> dict[str, Any]:
    configured = {
        path: json.loads(content)
        for path, content in generated.items()
        if "/worldgen/configured_feature/large_" in path
    }
    placed = {
        path: json.loads(content)
        for path, content in generated.items()
        if "/worldgen/placed_feature/large_" in path
    }
    profiles = [
        document["config"]
        for document in configured.values()
    ]
    if (
        len(configured) != 129
        or len(placed) != 129
        or sum(row["profile_version"] == 2 for row in profiles) != 129
    ):
        raise ValueError("generated T20 profile-v2 resource set is incomplete")
    expected_ids = {row["id"] for row in expected["rows"]}
    configured_ids = {
        Path(path).stem for path in configured
    }
    placed_ids = {Path(path).stem for path in placed}
    if expected_ids != configured_ids or expected_ids != placed_ids:
        raise ValueError("generated configured/placed ids differ from expected")
    profile_ids = {
        row["profile_id"].removeprefix("cruciblecraft:")
        for row in profiles
    }
    if profile_ids != expected_ids:
        raise ValueError("runtime profile ids differ from catalog ids")
    modifier = json.loads(generated[
        "data/cruciblecraft/neoforge/biome_modifier/"
        "add_worldgen_catalog.json"
    ])
    if len(modifier["features"]) != 131:
        raise ValueError("T20 biome modifier must include 129 ores and 2 fluids")
    return {
        "profile_version": 2,
        "configured_features": len(configured),
        "placed_features": len(placed),
        "profile_ids": len(profile_ids),
        "biome_modifier_features": len(modifier["features"]),
        "configured_decode_fields": [
            "profile_version",
            "profile_id",
            "top",
            "bottom",
            "between",
            "spread",
            "min_y",
            "max_y",
            "horizontal_radius",
            "vertical_radius",
            "density",
            "replaceable",
            "region_size_chunks",
            "generation_chance",
            "salt",
        ],
    }


def runtime_and_save_boundary(
    expected: dict[str, Any],
) -> dict[str, Any]:
    config_source = CONFIGURATION.read_text(encoding="utf-8")
    game_test_source = GAME_TESTS.read_text(encoding="utf-8")
    configuration_test = CONFIGURATION_TEST.read_text(encoding="utf-8")
    resource_test = RESOURCE_TEST.read_text(encoding="utf-8")
    required_runtime_fragments = (
        'Codec.intRange(1, 2).fieldOf("profile_version")',
        'ResourceLocation.CODEC.fieldOf("profile_id")',
    )
    required_game_test_fragments = (
        "worldgenVeins.size() == 134",
        "profileV2 == 129",
        "config.profileId().equals(id)",
        "assertLargeVeinPlaces(",
    )
    if any(value not in config_source for value in required_runtime_fragments):
        raise ValueError("profile-v2 runtime codec boundary is incomplete")
    if any(value not in game_test_source for value in required_game_test_fragments):
        raise ValueError("profile-v2 runtime registry GameTest is incomplete")
    if (
        "QuarantinesUnknownFutureProfile" not in configuration_test
        or 'assertEquals(2, config.get("profile_version")' not in resource_test
    ):
        raise ValueError("profile-v2 codec/resource JUnit evidence is incomplete")
    ids = [row["id"] for row in expected["rows"]]
    salts = [row["salt"] for row in expected["rows"]]
    stable_salts = all(
        row["salt"] == projection_builder.stable_salt(row["id"])
        for row in expected["rows"]
    )
    if len(set(ids)) != 129 or len(set(salts)) != 129 or not stable_salts:
        raise ValueError("profile identity/salt migration boundary drifted")
    return {
        "configured_and_placed_ids_preserved": True,
        "stable_salts_preserved": True,
        "existing_generated_chunks_rewritten": False,
        "existing_ore_blocks_require_migration": False,
        "unexplored_chunks_profile_version": 2,
        "supported_profile_versions": [1, 2],
        "unknown_profile_version": "CODEC_REJECTED",
        "unknown_profile_identity": "REGISTRY_LOOKUP_REJECTED",
        "persisted_profile_bytes_per_ore_block": 0,
        "reason": (
            "Vanilla-like ore blocks do not persist generator geometry. Existing "
            "chunks remain unchanged; current datapack profiles govern only "
            "future placement, while ids and salts remain stable."
        ),
    }


def load_evidence(
    expected: dict[str, Any],
    worldgen: dict[str, Any],
) -> dict[str, Any]:
    t19 = load(T19)
    rows = expected["rows"]
    generated_paths = list(
        catalog_builder.OUTPUT_RESOURCE_ROOT.rglob("*.json")
    )
    generated_bytes = sum(path.stat().st_size for path in generated_paths)
    weighted_states = sum(
        len(row[role])
        for row in rows
        for role in catalog_builder.vein_builder.LAYERS
    )
    maximum_role_states = max(
        len(row[role])
        for row in rows
        for role in catalog_builder.vein_builder.LAYERS
    )
    publication = t19["closure_summary"]["publication_totals"]
    expected_load = expected["load_projection"]
    if (
        expected_load["recipe_map_delta"] != 0
        or expected_load["logical_row_delta"] != 0
        or expected_load["eager_row_delta"] != 0
        or expected_load["lazy_row_delta"] != 0
    ):
        raise ValueError("T20 expected publication delta is non-zero")
    return {
        "worldgen_resources": {
            "t20_authored_rules": len(rows),
            "catalog_generated_files": worldgen["counts"][
                "catalog_generated_files"
            ],
            "all_worldgen_files": worldgen["counts"]["all_worldgen_files"],
            "catalog_generated_bytes": generated_bytes,
            "configured_features": expected_load["configured_features"],
            "placed_features": expected_load["placed_features"],
            "biome_modifiers": expected_load["biome_modifiers"],
        },
        "codec_startup": {
            "evidence_kind": "STATIC_INFERENCE",
            "profile_decodes": len(rows),
            "weighted_state_decodes": weighted_states,
            "maximum_states_per_role": maximum_role_states,
            "unbounded_selector_or_cache": False,
        },
        "save": {
            "evidence_kind": "STATIC_INFERENCE",
            "persisted_profile_bytes_per_ore_block": 0,
            "worldgen_resource_bytes": generated_bytes,
        },
        "density": {
            "expected_catalog_ore_veins_per_chunk": expected_load[
                "expected_ore_veins_per_chunk"
            ],
            "combined_expected_ore_veins_per_chunk": worldgen["density"][
                "combined_expected_ore_veins_per_chunk"
            ],
            "hard_ceiling": 0.15,
        },
        "publication": {
            "recipe_map_count": t19["closure_summary"]["recipe_map_count"],
            "logical_rows": publication["logical_rows"],
            "eager_rows": publication["eager_rows"],
            "lazy_rows": publication["lazy_rows"],
            "recipe_map_delta": 0,
            "logical_row_delta": 0,
            "eager_row_delta": 0,
            "lazy_row_delta": 0,
        },
    }


def build() -> dict[str, Any]:
    policy = load(POLICY)
    validate_policy(policy)
    schema = load(AUTHORING_SCHEMA)
    if (
        schema.get("$schema")
        != "https://json-schema.org/draft/2020-12/schema"
        or schema.get("properties", {})
        .get("veins", {})
        .get("minItems") != 129
        or schema["properties"]["veins"]["maxItems"] != 129
        or schema["$defs"]["vein"]["properties"][
            "profile_version"
        ]["const"] != 2
    ):
        raise ValueError("T20 authored worldgen schema drifted")
    source = load(SOURCE)
    expected = projection_builder.build_expected()
    source_builder.validate_compact(
        load(source_builder.POLICY), source
    )
    if (
        not EXPECTED.is_file()
        or EXPECTED.read_text(encoding="utf-8") != stable(expected)
    ):
        raise ValueError("T20 expected artifact is stale")
    projection_errors = projection_builder.compare_authored(expected)
    if projection_errors:
        raise ValueError(
            "T20 authored projection differs: " + ", ".join(projection_errors)
        )
    _, deposits, generated, worldgen = catalog_builder.build_documents()
    output_errors = catalog_builder.check_outputs(generated)
    if output_errors:
        raise ValueError(
            "T20 generated resources are stale: "
            + ", ".join(output_errors)
        )
    if (
        not WORLDGEN_READINESS.is_file()
        or load(WORLDGEN_READINESS) != worldgen
    ):
        raise ValueError("worldgen catalog readiness is stale")
    dependencies = dependency_evidence(
        policy, source, expected, worldgen
    )
    runtime = generated_runtime_evidence(expected, generated)
    save_boundary = runtime_and_save_boundary(expected)
    load_gate = load_evidence(expected, worldgen)
    fluid_identities = sorted(
        deposit["material"] for deposit in deposits
    )
    if fluid_identities != [
        "cruciblecraft:crude_oil",
        "cruciblecraft:natural_gas",
    ]:
        raise ValueError("T11 fluid-deposit identities changed during T20")
    counts = expected["counts"]
    return {
        "schema_version": 1,
        "status": policy["status_policy"]["ready"],
        "source_revision": source["source"]["revision"],
        "completed_stages": list(STAGE_ORDER),
        "pending_stages": [],
        "closure": {
            "source_large_facts": source["counts"]["large_source_facts"],
            "source_explicit_small_facts": source["counts"][
                "explicit_small_source_facts"
            ],
            "source_dynamic_small_rules": source["counts"][
                "dynamic_random_small_rules"
            ],
            "catalog_identities": counts["catalog_entries"],
            "configured_features": runtime["configured_features"],
            "placed_features": runtime["placed_features"],
            "biome_modifier_features": runtime[
                "biome_modifier_features"
            ],
            "registered_ore_materials": worldgen["counts"][
                "registered_ore_materials"
            ],
            "runtime_large_veins": 134,
            "t11_fluid_identities": fluid_identities,
            "unclassified": counts["unclassified"],
            "pending": 0,
        },
        "fidelity": {
            "classifications": counts["classifications"],
            "statuses": counts["statuses"],
            "placeholder": counts["placeholder"],
            "unverified": counts["unverified"],
            "distinct_geometry_signatures": counts[
                "distinct_geometry_signatures"
            ],
            "expected_equals_authored": True,
            "authored_equals_generated": True,
            "mutation_test": relative(PYTHON_PROJECTION_TEST),
            "non_claim": worldgen["geometry_policy"]["non_claim"],
        },
        "runtime": runtime,
        "save_boundary": save_boundary,
        "load": load_gate,
        "dependencies": dependencies,
        "currentness": {
            "owned_inputs": {
                relative(Path(__file__).resolve()): sha256(
                    Path(__file__).resolve()
                ),
                relative(POLICY): sha256(POLICY),
                relative(source_builder.POLICY): sha256(
                    source_builder.POLICY
                ),
                relative(Path(source_builder.__file__).resolve()): sha256(
                    Path(source_builder.__file__).resolve()
                ),
                relative(SOURCE): sha256(SOURCE),
                relative(
                    Path(projection_builder.__file__).resolve()
                ): sha256(Path(projection_builder.__file__).resolve()),
                relative(EXPECTED): sha256(EXPECTED),
                relative(AUTHORING_SCHEMA): sha256(AUTHORING_SCHEMA),
                relative(
                    Path(catalog_builder.__file__).resolve()
                ): sha256(Path(catalog_builder.__file__).resolve()),
                relative(catalog_builder.ORE_DECLARATIONS): sha256(
                    catalog_builder.ORE_DECLARATIONS
                ),
                relative(WORLDGEN_READINESS): sha256(WORLDGEN_READINESS),
                relative(CONFIGURATION): sha256(CONFIGURATION),
                relative(GAME_TESTS): sha256(GAME_TESTS),
                relative(CONFIGURATION_TEST): sha256(CONFIGURATION_TEST),
                relative(RESOURCE_TEST): sha256(RESOURCE_TEST),
                relative(PYTHON_PROJECTION_TEST): sha256(
                    PYTHON_PROJECTION_TEST
                ),
                relative(PHASE4_PLAN): sha256(PHASE4_PLAN),
                relative(T20_ARCHIVE): sha256(T20_ARCHIVE),
                relative(PHASE4_CONTRACT): sha256(PHASE4_CONTRACT),
            },
            "source_replay_command": (
                "python tools/build_t20_worldgen_source.py "
                "--check --full-replay"
            ),
            "full_verification": {
                **policy["refresh_policy"],
                "pending": [],
            },
        },
        "closure_policy": policy["closure_policy"],
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
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    args = parser.parse_args()
    try:
        document = build()
        if args.write:
            OUTPUT.write_text(
                stable(document), encoding="utf-8", newline="\n"
            )
            print(f"Wrote {relative(OUTPUT)}")
            return 0
        errors = check(document)
        if errors:
            print("\n".join(errors))
            return 1
        print("T20 readiness is current.")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"T20 readiness build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
