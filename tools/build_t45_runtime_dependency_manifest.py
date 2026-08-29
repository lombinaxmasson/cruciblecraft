#!/usr/bin/env python3
"""Bind T45 runtime dependencies by path + SHA-256."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t45_common as common

JAVA_ROOT = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"


def _file(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise ValueError(f"missing T45 runtime dependency: {common.relative(path)}")
    return {
        "path": common.relative(path),
        "sha256": t35.sha256_file(path),
    }


def _tree(root: Path) -> dict[str, str]:
    digest = common._tree_sha256(root)
    if digest is None:
        raise ValueError(f"missing T45 runtime dependency tree: {common.relative(root)}")
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
    hosts = {
        "processing_machines": _file(JAVA_ROOT / "registry" / "ModProcessingMachines.java"),
        "recipe_maps": _file(JAVA_ROOT / "registry" / "ModRecipeMaps.java"),
        "blocks": _file(JAVA_ROOT / "registry" / "ModBlocks.java"),
        "items": _file(JAVA_ROOT / "registry" / "ModItems.java"),
    }
    t45_trees = {
        "generated_recipes": _tree(common.GENERATED_ROOT),
        "locked_support": {
            "path": common.relative(common.LOCKED_SUPPORT_ROOT),
            "sha256": common._tree_sha256(common.LOCKED_SUPPORT_ROOT) or "",
        },
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
        "block_object_catalog": _file(common.BLOCK_CATALOG),
        "production_lock": _file(common.PRODUCTION_LOCK),
    }
    return {
        "hosts": hosts,
        "compact_runtime": compact_runtime,
        "isolation": isolation,
        "overlays": overlays,
        "schema_version": 1,
        "status": "T45_RUNTIME_DEPENDENCIES_BOUND",
        "t45_trees": t45_trees,
        "verification": {
            "builder_policy": _file(ROOT / "tools" / "verification_builder_policy.json"),
            "profiles": _file(ROOT / "tools" / "verification_profiles.json"),
            "workflow": _file(ROOT / "docs" / "current" / "recipe-wave-workflow.md"),
        },
        "centrifuge_premerge_whitelist_relaxed": False,
        "historical_java_t45_whitelist": False,
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Bind T45 runtime dependencies",
        common.RUNTIME_DEPENDENCY_MANIFEST,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
