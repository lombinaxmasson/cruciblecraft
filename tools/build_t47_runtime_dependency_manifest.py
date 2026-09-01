#!/usr/bin/env python3
"""Bind T47 runtime dependencies by path + SHA-256."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t47_common as common
from tools.recipe_bulk import runtime as runtime_mod

JAVA_ROOT = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"


def _file(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise ValueError(f"missing T47 runtime dependency: {common.relative(path)}")
    return {
        "path": common.relative(path),
        "sha256": t35.sha256_file(path),
    }


def _tree(root: Path) -> dict[str, str]:
    digest = common._tree_sha256(root)
    if digest is None or digest == common.EMPTY_TREE_SHA256:
        raise ValueError(
            f"missing or empty T47 runtime dependency tree: {common.relative(root)}"
        )
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
        "publication_policy": _file(
            JAVA_ROOT / "recipe" / "gt" / "CompactPublicationPolicy.java"
        ),
        "publication_policy_definition": _file(
            JAVA_ROOT / "recipe" / "gt" / "CompactPublicationPolicyDefinition.java"
        ),
        "schema": _file(common.SCHEMA),
        "publication_policy_schema": _file(common.PUBLICATION_POLICY_SCHEMA),
        "shard_router": _file(
            JAVA_ROOT / "recipe" / "gt" / "CompactRecipeShardRouter.java"
        ),
        "component_index": _file(
            JAVA_ROOT / "recipe" / "gt" / "ComponentIngredientIndex.java"
        ),
        "gt_block_object_catalog": _file(
            JAVA_ROOT / "content" / "item" / "GtBlockObjectCatalog.java"
        ),
    }
    compact_runtime.update(runtime_mod.compact_cutover_runtime_files(_file, _tree))
    hosts = {
        "processing_machines": _file(JAVA_ROOT / "registry" / "ModProcessingMachines.java"),
        "recipe_maps": _file(JAVA_ROOT / "registry" / "ModRecipeMaps.java"),
        "blocks": _file(JAVA_ROOT / "registry" / "ModBlocks.java"),
        "items": _file(JAVA_ROOT / "registry" / "ModItems.java"),
        "fluids": _file(JAVA_ROOT / "registry" / "ModFluids.java"),
        "t47_block_object_catalog_java": _file(
            JAVA_ROOT / "content" / "item" / "BathRemainderBlockObjectCatalog.java"
        ),
        "bath_remainder_fluid_catalog_java": _file(
            JAVA_ROOT / "content" / "item" / "BathRemainderFluidCatalog.java"
        ),
    }
    t47_trees = {
        "generated_recipes": _tree(common.GENERATED_ROOT),
        "locked_support": _tree(common.LOCKED_SUPPORT_RECIPE_ROOT),
    }
    isolation = {
        "build_gradle": _file(ROOT / "build.gradle"),
        "gitignore": _file(ROOT / ".gitignore"),
        "gametest_java": _file(common.GAME_TEST_JAVA),
    }
    overlays = {
        "publication_group_manifest": _file(common.PUBLICATION_GROUP_MANIFEST)
        if common.PUBLICATION_GROUP_MANIFEST.is_file()
        else {
            "path": common.relative(common.PUBLICATION_GROUP_MANIFEST),
            "sha256": "",
        },
        "shard_manifest": _file(common.SHARD_MANIFEST)
        if common.SHARD_MANIFEST.is_file()
        else {
            "path": common.relative(common.SHARD_MANIFEST),
            "sha256": "",
        },
        "source": _file(common.SOURCE),
        "catalog_work_set": _file(common.WORK_SET),
        "candidate_selection": _file(common.CANDIDATE_SELECTION),
        "identity_catalog": _file(common.IDENTITY_CATALOG),
        "bundled_identity_catalog": _file(common.BUNDLED_IDENTITY_CATALOG),
        "fluid_mapping": _file(common.FLUID_MAPPING),
        "production_lock": _file(common.PRODUCTION_LOCK),
    }
    return {
        "hosts": hosts,
        "compact_runtime": compact_runtime,
        "isolation": isolation,
        "overlays": overlays,
        "schema_version": 1,
        "status": "T47_RUNTIME_DEPENDENCIES_BOUND",
        "t47_trees": t47_trees,
        "verification": {
            "builder_policy": _file(ROOT / "tools" / "verification_builder_policy.json"),
            "profiles": _file(ROOT / "tools" / "verification_profiles.json"),
            "workflow": _file(ROOT / "docs" / "current" / "recipe-wave-workflow.md"),
        },
        "centrifuge_premerge_whitelist_relaxed": False,
        "historical_java_t47_whitelist": False,
    }


def main(argv: list[str] | None = None) -> int:
    from tools import closeout_seal

    return common.run_managed(
        "Bind T47 runtime dependencies",
        common.RUNTIME_DEPENDENCY_MANIFEST,
        build=build,
        check=lambda: closeout_seal.live_or_sealed_errors(
            "T47",
            "runtime_dependency_manifest",
            lambda: common.check_document(common.RUNTIME_DEPENDENCY_MANIFEST, build()),
        ),
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
