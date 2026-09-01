from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "tools" / "t10_preflight_policy.json"
OUTPUT = ROOT / "tools" / "t10_preflight_projection.json"
MATERIAL_ROOT = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
)
MATERIAL_INDEX = MATERIAL_ROOT / "index.json"
REGISTRATION_GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
PREFIX_ROOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_prefixes"
)
PREFIX_INDEX = PREFIX_ROOT / "index.json"
PREFIX_SOURCE = ROOT / "tools" / "gt6_oredict_prefixes_normalized.json"
RECIPE_ROOT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)
CONTAINER_READINESS = ROOT / "tools" / "t10_container_readiness.json"
PROCESSING_MACHINES = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry"
    / "ModProcessingMachines.java"
)
GAME_TESTS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/gametest"
    / "CrucibleCraftGameTests.java"
)
RESOURCE_ROOTS = (
    ROOT / "src/main/resources",
    ROOT / "src/generated/resources",
    ROOT / "src/ore_chain_generated/resources",
    ROOT / "src/worldgen_generated/resources",
    ROOT / "src/worldgen_catalog_generated/resources",
    ROOT / "src/component_rule_generated/resources",
    ROOT / "src/t5_chemical_generated/resources",
)
T11_MACHINE_RECIPE_PATHS = {
    "data/cruciblecraft/recipe/machines/generifier.json",
    "data/cruciblecraft/recipe/machines/fluid_deposit_extractor.json",
    "data/cruciblecraft/recipe/machines/fuel_engine.json",
    "data/cruciblecraft/recipe/machines/burning_gas_generator.json",
}
T12_RECIPE_PATHS = {
    "data/cruciblecraft/recipe/machines/electric_motor.json",
    "data/cruciblecraft/recipe/machines/rotational_axle.json",
    "data/cruciblecraft/recipe/machines/rotational_gearbox.json",
    "data/cruciblecraft/recipe/machines/steel_centrifuge.json",
    "data/cruciblecraft/recipe/machines/titanium_centrifuge.json",
    "data/cruciblecraft/recipe/machines/steel_sifter.json",
    "data/cruciblecraft/recipe/machines/titanium_sifter.json",
    "data/cruciblecraft/recipe/machines/aluminium_electrolyzer.json",
    "data/cruciblecraft/recipe/machines/stainless_steel_electrolyzer.json",
    "data/cruciblecraft/recipe/machines/large_centrifuge.json",
    "data/cruciblecraft/recipe/machines/multiblock_casing.json",
    "data/cruciblecraft/recipe/machines/multiblock_item_fluid_port.json",
    "data/cruciblecraft/recipe/machines/multiblock_energy_input_port.json",
    "data/cruciblecraft/recipe/components/bronze_double_machine_casing.json",
    "data/cruciblecraft/recipe/components/steel_double_machine_casing.json",
    "data/cruciblecraft/recipe/components/titanium_double_machine_casing.json",
    "data/cruciblecraft/recipe/components/steel_galvanized_machine_casing.json",
    "data/cruciblecraft/recipe/components/aluminium_machine_casing.json",
    "data/cruciblecraft/recipe/components/stainless_steel_machine_casing.json",
}
T16_RECIPE_PATHS = {
    "data/cruciblecraft/recipe/machines/steel_lathe.json",
    "data/cruciblecraft/recipe/machines/titanium_lathe.json",
    "data/cruciblecraft/recipe/machines/steel_rollingmill.json",
    "data/cruciblecraft/recipe/machines/titanium_rollingmill.json",
    "data/cruciblecraft/recipe/machines/steel_wiremill.json",
    "data/cruciblecraft/recipe/machines/titanium_wiremill.json",
    "data/cruciblecraft/recipe/machines/steel_shredder.json",
    "data/cruciblecraft/recipe/machines/titanium_shredder.json",
    "data/cruciblecraft/recipe/machines/steel_press.json",
    "data/cruciblecraft/recipe/machines/titanium_press.json",
}
T18_RECIPE_PATHS = {
    "data/cruciblecraft/recipe/machines/firebox.json",
}
PINNED_SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
PINNED_OP_BLOB = "f915645f3009d3dbe61abbafe77791000be32747"
PINNED_UT_BLOB = "e1a89b2c04e1183fda13a490c5a035512acf7f14"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value, indent=2, ensure_ascii=False, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def material_tree_sha256(index: list[str]) -> str:
    digest = hashlib.sha256()
    for filename in index:
        path = MATERIAL_ROOT / filename
        digest.update(filename.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def prefix_tree_sha256(index: list[str]) -> str:
    digest = hashlib.sha256()
    for filename in index:
        path = PREFIX_ROOT / filename
        digest.update(filename.encode("utf-8"))
        digest.update(b"\0")
        digest.update(path.read_bytes())
        digest.update(b"\0")
    return digest.hexdigest()


def round_up(value: int, unit: int) -> int:
    return ((value + unit - 1) // unit) * unit


def datapack_recipe_entries() -> dict[str, Path]:
    entries: dict[str, Path] = {}
    contents: dict[str, bytes] = {}
    for root in RESOURCE_ROOTS:
        if not root.is_dir():
            continue
        for path in sorted(root.glob("data/*/recipe/**/*.json")):
            relative = path.relative_to(root).as_posix()
            if relative in (
                T11_MACHINE_RECIPE_PATHS
                | T12_RECIPE_PATHS
                | T16_RECIPE_PATHS
                | T18_RECIPE_PATHS
            ):
                continue
            content = path.read_bytes()
            previous = contents.get(relative)
            if previous is not None and previous != content:
                raise ValueError(
                    f"conflicting duplicate datapack recipe entry: {relative}"
                )
            entries.setdefault(relative, path)
            contents.setdefault(relative, content)
    return entries


def build() -> dict[str, Any]:
    policy = load(POLICY)
    index = load(MATERIAL_INDEX)
    gate = load(REGISTRATION_GATE)["materials"]
    prefix_index = load(PREFIX_INDEX)
    prefix_definitions = {
        document["id"]: document
        for document in (
            load(PREFIX_ROOT / filename) for filename in prefix_index
        )
    }
    source_evidence = policy["source_evidence"]
    if policy["source_revision"] != PINNED_SOURCE_REVISION:
        raise ValueError("GT6 source revision drifted")
    if source_evidence["op_java"]["git_blob_sha1"] != PINNED_OP_BLOB:
        raise ValueError("GT6 OP.java blob drifted")
    if source_evidence["ut_java"]["git_blob_sha1"] != PINNED_UT_BLOB:
        raise ValueError("GT6 UT.java blob drifted")
    if source_evidence["op_java"]["path"] != "gregapi/data/OP.java":
        raise ValueError("GT6 OP.java path drifted")
    if source_evidence["ut_java"]["path"] != "gregapi/util/UT.java":
        raise ValueError("GT6 UT.java path drifted")
    normalized_source = source_evidence["normalized_prefix_dump"]
    if normalized_source["path"] != PREFIX_SOURCE.relative_to(ROOT).as_posix():
        raise ValueError("normalized prefix source path drifted")
    if sha256(PREFIX_SOURCE) != normalized_source["sha256"]:
        raise ValueError("normalized prefix source digest drifted")
    if (
        normalized_source["extraction_key"]
        != "records[source_name=ingotHot].heat_damage"
    ):
        raise ValueError("normalized prefix extraction key drifted")

    prefix_policy = policy["prefix_fact_policy"]
    source_prefixes = load(PREFIX_SOURCE)["records"]
    nonzero_source_heat_damage = {
        row["source_name"]: float(row.get("heat_damage") or 0.0)
        for row in source_prefixes
        if float(row.get("heat_damage") or 0.0) != 0.0
    }
    if (
        nonzero_source_heat_damage
        != prefix_policy["expected_nonzero_source_heat_damage"]
    ):
        raise ValueError("GT6 nonzero prefix heat-damage facts drifted")
    nonzero_runtime_heat_damage = {
        prefix_id: float(document.get("heat_damage") or 0.0)
        for prefix_id, document in prefix_definitions.items()
        if float(document.get("heat_damage") or 0.0) != 0.0
    }
    if (
        nonzero_runtime_heat_damage
        != prefix_policy["expected_nonzero_runtime_heat_damage"]
    ):
        raise ValueError("runtime nonzero prefix heat-damage facts drifted")

    runtime_prefixes: dict[str, Any] = {}
    if set(prefix_policy["source_to_cruciblecraft"].values()) != set(
        prefix_policy["expected_runtime_prefixes"]
    ):
        raise ValueError("source-to-runtime prefix mapping drifted")
    if any(
        source_name.lower()
        != prefix_policy["expected_runtime_prefixes"][prefix_id][
            "source_alias"
        ]
        for source_name, prefix_id in prefix_policy[
            "source_to_cruciblecraft"
        ].items()
    ):
        raise ValueError("source-to-runtime prefix aliases drifted")
    for prefix_id, expected in prefix_policy[
        "expected_runtime_prefixes"
    ].items():
        document = prefix_definitions.get(prefix_id)
        if document is None:
            raise ValueError(f"missing T10a prefix definition: {prefix_id}")
        actual = {
            "units": int(document["units"]),
            "source_alias": expected["source_alias"],
            "heat_damage": float(document.get("heat_damage") or 0.0),
        }
        if expected["source_alias"] not in (document.get("aliases") or []):
            raise ValueError(f"missing source alias for {prefix_id}")
        if actual != expected:
            raise ValueError(f"T10a prefix facts drifted for {prefix_id}")
        runtime_prefixes[prefix_id] = actual

    startup_prefix_count = len(prefix_index)
    material_count = len(index)
    handshake_entry_count = startup_prefix_count + material_count
    if startup_prefix_count != prefix_policy["expected_startup_prefix_count"]:
        raise ValueError(f"startup prefix count drifted: {startup_prefix_count}")
    if material_count != prefix_policy["expected_material_count"]:
        raise ValueError(f"material count drifted: {material_count}")
    if (
        handshake_entry_count
        != prefix_policy["expected_handshake_entry_count"]
    ):
        raise ValueError(
            f"handshake entry count drifted: {handshake_entry_count}"
        )
    gate_registration_counts = {
        prefix: sum(prefix in forms for forms in gate.values())
        for prefix in prefix_policy["expected_gate_registration_counts"]
    }
    if (
        gate_registration_counts
        != prefix_policy["expected_gate_registration_counts"]
    ):
        raise ValueError(
            f"T10a gate registrations drifted: {gate_registration_counts}"
        )
    generation_tags: dict[str, set[str]] = {}
    for filename in index:
        material = load(MATERIAL_ROOT / filename)
        metadata = material.get("gt6_metadata") or {}
        generation_tags[material["id"]] = set(
            metadata.get("generation_tags") or []
        )

    route_projections: dict[str, Any] = {}
    known_t10_recipes = 0
    for route_id, route in policy["route_projections"].items():
        tag = route["generation_tag"]
        required_form = route["required_registered_form"]
        materials = sorted(
            material_id
            for material_id, tags in generation_tags.items()
            if tag in tags and required_form in gate[material_id]
        )
        recipe_count = len(materials) * len(route["routes"])
        if len(materials) != route["expected_material_count"]:
            raise ValueError(
                f"{route_id} material count drifted: {len(materials)}"
            )
        if recipe_count != route["expected_recipe_count"]:
            raise ValueError(
                f"{route_id} recipe count drifted: {recipe_count}"
            )
        known_t10_recipes += recipe_count
        route_projections[route_id] = {
            **route,
            "material_count": len(materials),
            "materials": materials,
            "material_set_sha256": hashlib.sha256(
                stable_json(materials).encode("utf-8")
            ).hexdigest(),
            "recipe_count": recipe_count,
        }

    container_sets = {
        tag: {
            material_id
            for material_id, tags in generation_tags.items()
            if tag in tags
        }
        for tag in policy["container_domains"]
    }
    container_memberships = sum(len(values) for values in container_sets.values())
    container_union = set().union(*container_sets.values())
    container_acceptance = policy["container_acceptance"]
    readiness_path = ROOT / container_acceptance["readiness_path"]
    container_readiness = load(readiness_path)
    container_counts = container_readiness["counts"]
    expected_container_counts = {
        "fluid_domain": container_acceptance["expected_fluid_domain"],
        "gas_domain": container_acceptance["expected_gas_domain"],
        "containers_only_denied": container_acceptance[
            "expected_containers_only_denied"
        ],
        "new_t10_chemical_fluids": container_acceptance[
            "expected_new_chemical_fluids"
        ],
        "cell_gate_entries": container_acceptance[
            "expected_cell_gate_entries"
        ],
    }
    if (
        container_readiness.get("status") != "READY"
        or any(
            container_counts[key] != value
            for key, value in expected_container_counts.items()
        )
    ):
        raise ValueError("T10 container readiness is not closed")

    current = policy["current_post_t8_publication"]
    t9 = policy["t9_projected_recipe_additions"]
    known_post_t10 = current + t9 + known_t10_recipes
    t10a_publication = policy["t10a_publication"]
    t10_recipe_files = (
        sorted((RECIPE_ROOT / "ingot_form").rglob("*.json"))
        if (RECIPE_ROOT / "ingot_form").is_dir()
        else []
    )
    if (
        len(t10_recipe_files)
        != t10a_publication["expected_current_recipe_additions"]
    ):
        raise ValueError(
            f"T10a unexpectedly publishes {len(t10_recipe_files)} recipes"
        )
    if (
        known_t10_recipes
        != t10a_publication["known_future_recipe_additions"]
    ):
        raise ValueError("known T10 form projection drifted")
    if (
        known_t10_recipes
        > t10a_publication["known_form_material_rule_budget"]
    ):
        raise ValueError("known T10 form projection exceeds its stage budget")
    budget_policy = policy["budget_policy"]
    margin_ceiling = (
        known_post_t10 * budget_policy["margin_numerator"]
        + budget_policy["margin_denominator"]
        - 1
    ) // budget_policy["margin_denominator"]
    global_budget = round_up(
        margin_ceiling, budget_policy["round_up_to"]
    )
    if global_budget != budget_policy["expected_global_budget"]:
        raise ValueError(f"global budget drifted: {global_budget}")

    load_policy = policy["load_policy"]
    recipe_entries = datapack_recipe_entries()
    datapack_count = len(recipe_entries)
    if datapack_count != load_policy["expected_datapack_recipe_entries"]:
        raise ValueError(
            f"datapack recipe entry count drifted: {datapack_count}"
        )
    if known_post_t10 != load_policy["expected_published_recipes"]:
        raise ValueError("T10 publication load gate drifted")
    compression_ratio = known_post_t10 / datapack_count
    if (
        datapack_count > load_policy["datapack_recipe_entry_budget"]
        or compression_ratio < load_policy["minimum_compression_ratio"]
    ):
        raise ValueError("T10 datapack or compression load gate failed")
    processing_source = PROCESSING_MACHINES.read_text(encoding="utf-8")
    game_test_source = GAME_TESTS.read_text(encoding="utf-8")
    runtime_contract = {
        "reload_budget_ms": load_policy["reload_budget_ms"],
        "index_build_budget_ms": load_policy["index_build_budget_ms"],
        "budget_constants_present": (
            "RECIPE_RELOAD_BUDGET_MS = 10_000L" in processing_source
            and "RECIPE_INDEX_BUILD_BUDGET_MS = 1_000L"
            in processing_source
        ),
        "runtime_assertions_present": (
            "metrics.reloadMillis()" in game_test_source
            and "metrics.indexMillis()" in game_test_source
            and "metrics.allPublishedRecipes()"
            in game_test_source
            and "ALL_PUBLISHED_RECIPE_BUDGET"
            in game_test_source
        ),
    }
    if not all((
        runtime_contract["budget_constants_present"],
        runtime_contract["runtime_assertions_present"],
    )):
        raise ValueError("T10 reload/index runtime contract is incomplete")

    return {
        "schema_version": 3,
        "status": "T10_READY",
        "source_revision": policy["source_revision"],
        "source_evidence": source_evidence,
        "sources": {
            "policy": {
                "path": POLICY.relative_to(ROOT).as_posix(),
                "sha256": sha256(POLICY),
            },
            "material_index": {
                "path": MATERIAL_INDEX.relative_to(ROOT).as_posix(),
                "sha256": sha256(MATERIAL_INDEX),
            },
            "material_tree_sha256": material_tree_sha256(index),
            "prefix_index": {
                "path": PREFIX_INDEX.relative_to(ROOT).as_posix(),
                "sha256": sha256(PREFIX_INDEX),
            },
            "prefix_tree_sha256": prefix_tree_sha256(prefix_index),
            "normalized_prefix_dump": {
                "path": PREFIX_SOURCE.relative_to(ROOT).as_posix(),
                "sha256": sha256(PREFIX_SOURCE),
            },
            "registration_gate": {
                "path": REGISTRATION_GATE.relative_to(ROOT).as_posix(),
                "sha256": sha256(REGISTRATION_GATE),
            },
            "container_readiness": {
                "path": readiness_path.relative_to(ROOT).as_posix(),
                "sha256": sha256(readiness_path),
            },
        },
        "prefix_facts": {
            "source_to_cruciblecraft": prefix_policy[
                "source_to_cruciblecraft"
            ],
            "runtime_prefixes": runtime_prefixes,
            "nonzero_source_heat_damage": nonzero_source_heat_damage,
            "nonzero_runtime_heat_damage": nonzero_runtime_heat_damage,
            "startup_prefix_count": startup_prefix_count,
            "material_count": material_count,
            "handshake_entry_count": handshake_entry_count,
            "gate_registration_counts": gate_registration_counts,
        },
        "route_projections": route_projections,
        "container_domains": {
            "status": policy["container_recipe_status"],
            "counts": {
                tag: len(values)
                for tag, values in sorted(container_sets.items())
            },
            "membership_count": container_memberships,
            "union_material_count": len(container_union),
            "union_materials": sorted(container_union),
            "runtime_acceptance": container_readiness,
            "note": (
                "Cell contents are runtime fluid state and intentionally add "
                "no MaterialRule publication."
            ),
        },
        "budget_projection": {
            "post_t8_published_recipes": current,
            "t9_projected_recipe_additions": t9,
            "known_t10_recipe_additions": known_t10_recipes,
            "known_post_t10_published_recipes": known_post_t10,
            "margin_numerator": budget_policy["margin_numerator"],
            "margin_denominator": budget_policy["margin_denominator"],
            "round_up_to": budget_policy["round_up_to"],
            "global_budget": global_budget,
            "remaining_after_known_t10": global_budget - known_post_t10,
        },
        "runtime_publication": {
            "post_t8_published_recipes": current,
            "current_t10_datapack_entries": len(t10_recipe_files),
            "current_t10_recipe_additions": known_t10_recipes,
            "post_t10_published_recipes": known_post_t10,
            "known_future_t10_recipe_additions": 0,
            "known_form_material_rule_budget": t10a_publication[
                "known_form_material_rule_budget"
            ],
            "within_known_form_material_rule_budget": (
                known_t10_recipes
                <= t10a_publication["known_form_material_rule_budget"]
            ),
        },
        "load_gate": {
            "status": "READY",
            "datapack_recipe_entries": datapack_count,
            "datapack_recipe_entry_budget": load_policy[
                "datapack_recipe_entry_budget"
            ],
            "published_recipes": known_post_t10,
            "published_recipe_budget": global_budget,
            "compression_ratio": round(compression_ratio, 6),
            "minimum_compression_ratio": load_policy[
                "minimum_compression_ratio"
            ],
            "runtime_contract": runtime_contract,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    encoded = stable_json(build())
    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != encoded:
            raise SystemExit("T10 preflight projection is stale")
        print("T10 preflight projection is current.")
        return 0
    OUTPUT.write_bytes(encoded.encode("utf-8"))
    print(f"wrote {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
