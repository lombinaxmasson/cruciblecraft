#!/usr/bin/env python3
"""Bind T39 runtime dependencies by path + SHA-256."""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t39_common as common  # noqa: E402
from tools.recipe_bulk import runtime as runtime_mod  # noqa: E402

JAVA_ROOT = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
TEST_JAVA_ROOT = ROOT / "src" / "test" / "java" / "com" / "masson" / "cruciblecraft"


def _file(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise ValueError(f"missing T39 runtime dependency: {common.relative(path)}")
    return {
        "path": common.relative(path),
        "sha256": t35.sha256_file(path),
    }


def _tree(root: Path) -> dict[str, str]:
    digest = common._tree_sha256(root)
    if digest is None:
        raise ValueError(f"missing T39 runtime dependency tree: {common.relative(root)}")
    return {
        "path": common.relative(root),
        "sha256": digest,
    }


def build() -> dict[str, Any]:
    compact_runtime = {
        "codec": _file(
            JAVA_ROOT / "recipe" / "gt" / "CompactGTRecipeFamilyDefinition.java"
        ),
        "loader": _file(JAVA_ROOT / "recipe" / "gt" / "GTRecipeMapLoader.java"),
        "provider": _file(
            JAVA_ROOT / "recipe" / "gt" / "CompactRecipeFamilyProvider.java"
        ),
        "publication_group_key": _file(
            JAVA_ROOT / "recipe" / "gt" / "PublicationGroupKey.java"
        ),
        "schema": _file(common.SCHEMA),
        "shard_router": _file(
            JAVA_ROOT / "recipe" / "gt" / "CompactRecipeShardRouter.java"
        ),
    }
    compact_runtime.update(runtime_mod.compact_cutover_runtime_files(_file, _tree))
    historical_groups = {
        "t37_assembler_generated": _tree(
            ROOT / "src" / "t37_recipe_generated" / "resources"
        ),
        "t38_roaster_generated": _tree(
            ROOT / "src" / "t38_recipe_generated" / "resources"
        ),
        "t37_policy": _file(runtime_mod.t37_policy_resource()),
    }
    centrifuge_host = {
        "energy_placement": _file(
            JAVA_ROOT / "machine" / "processing" / "ProcessingMachineEnergyPlacement.java"
        ),
        "hopper_variants_schema": _file(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "schema"
            / "hopper_variants.schema.json"
        ),
        "processing_machines": _file(JAVA_ROOT / "registry" / "ModProcessingMachines.java"),
        "recipe_maps": _file(JAVA_ROOT / "registry" / "ModRecipeMaps.java"),
    }
    materials = {
        "gate": _file(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "material_registration_gate.json"
        ),
        "prefix_catalog": _file(
            JAVA_ROOT / "material" / "prefix" / "MaterialPrefixCatalog.java"
        ),
        "prefix_index": _file(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "material_prefixes"
            / "index.json"
        ),
        "registration_gate_java": _file(
            JAVA_ROOT / "material" / "MaterialRegistrationGate.java"
        ),
    }
    t39_trees = {
        "generated_recipes": _tree(common.GENERATED_ROOT),
        "locked_support": {
            "path": common.relative(common.LOCKED_SUPPORT_ROOT),
            "sha256": common._tree_sha256(common.LOCKED_SUPPORT_ROOT) or "",
        },
    }
    catalog_fixture = {
        **_tree(common.CATALOG_FIXTURE_ROOT),
        "runtime_classpath": False,
        "purpose": "router/load fixture only",
    }
    verification = {
        "builder_policy": _file(ROOT / "tools" / "verification_builder_policy.json"),
        "profiles": _file(ROOT / "tools" / "verification_profiles.json"),
        "workflow": _file(ROOT / "docs" / "current" / "recipe-wave-workflow.md"),
    }
    isolation = {
        "build_gradle": _file(ROOT / "build.gradle"),
        "gitignore": _file(ROOT / ".gitignore"),
        "gametest_java": (
            _file(common.GAME_TEST_JAVA)
            if common.GAME_TEST_JAVA.is_file()
            else {
                "path": common.relative(common.GAME_TEST_JAVA),
                "sha256": "",
            }
        ),
    }
    overlays = {
        "publication_group_manifest": (
            _file(common.PUBLICATION_GROUP_MANIFEST)
            if common.PUBLICATION_GROUP_MANIFEST.is_file()
            else {
                "path": common.relative(common.PUBLICATION_GROUP_MANIFEST),
                "sha256": "",
            }
        ),
        "shard_manifest": (
            _file(common.SHARD_MANIFEST)
            if common.SHARD_MANIFEST.is_file()
            else {
                "path": common.relative(common.SHARD_MANIFEST),
                "sha256": "",
            }
        ),
        "source": _file(common.SOURCE),
        "catalog_work_set": _file(common.WORK_SET),
        "candidate_selection": _file(common.CANDIDATE_SELECTION),
        "layered_player_path": _file(common.LAYERED_PLAYER_PATH),
        "operand_disposition": _file(common.OPERAND_DISPOSITION),
        "production_lock": _file(common.PRODUCTION_LOCK),
    }
    document = {
        "centrifuge_host": centrifuge_host,
        "compact_runtime": compact_runtime,
        "catalog_fixture": catalog_fixture,
        "historical_groups": historical_groups,
        "isolation": isolation,
        "materials": materials,
        "overlays": overlays,
        "schema_version": 1,
        "status": "T39_RUNTIME_DEPENDENCIES_BOUND",
        "t39_trees": t39_trees,
        "verification": verification,
    }
    document["aggregate_root_sha256"] = hashlib.sha256(
        t35.stable_json(document).encode("utf-8")
    ).hexdigest()
    return document


def main(argv: list[str] | None = None) -> int:
    args = common.parse_write_check("Bind T39 runtime dependencies.", argv)
    document = build()
    if args.check:
        from tools import closeout_seal

        errors = closeout_seal.live_or_sealed_errors(
            "T39",
            "runtime_dependency_manifest",
            lambda: common.check_document(common.RUNTIME_DEPENDENCY_MANIFEST, document),
        )
        if errors:
            print("T39 runtime dependency manifest is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T39 runtime dependency manifest is current.")
        return 0
    t35.write_stable(common.RUNTIME_DEPENDENCY_MANIFEST, document)
    print("Wrote T39 runtime dependency manifest.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
