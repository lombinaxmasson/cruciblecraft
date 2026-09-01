#!/usr/bin/env python3
"""Bind T48 runtime dependencies by path + SHA-256."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t48_common as common
from tools.recipe_bulk import runtime as runtime_mod

JAVA_ROOT = ROOT / "src" / "main" / "java" / "com" / "masson" / "cruciblecraft"
OUTPUT = common.RUNTIME_DEPENDENCY_MANIFEST


def _file(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise ValueError(f"missing T48 runtime dependency: {common.relative(path)}")
    return {
        "path": common.relative(path),
        "sha256": t35.sha256_file(path),
    }


def _optional_file(path: Path) -> dict[str, str]:
    if not path.is_file():
        return {
            "path": common.relative(path),
            "sha256": "",
        }
    return _file(path)


def _tree(root: Path, *, allow_empty: bool = False) -> dict[str, str]:
    digest = common.tree_sha256(root) if root.exists() else common.EMPTY_TREE_SHA256
    if digest is None:
        digest = common.EMPTY_TREE_SHA256
    if not allow_empty and digest == common.EMPTY_TREE_SHA256:
        raise ValueError(
            f"missing or empty T48 runtime dependency tree: {common.relative(root)}"
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
        "t48_identity_catalog": _file(
            JAVA_ROOT / "content" / "item" / "BathIdentityCatalog.java"
        ),
    }
    compact_runtime.update(runtime_mod.compact_cutover_runtime_files(_file, _tree))
    hosts = {
        "processing_machines": _file(JAVA_ROOT / "registry" / "ModProcessingMachines.java"),
        "recipe_maps": _file(JAVA_ROOT / "registry" / "ModRecipeMaps.java"),
        "blocks": _file(JAVA_ROOT / "registry" / "ModBlocks.java"),
        "items": _file(JAVA_ROOT / "registry" / "ModItems.java"),
        "fluids": _file(JAVA_ROOT / "registry" / "ModFluids.java"),
        "t48_identity_catalog_java": _file(
            JAVA_ROOT / "content" / "item" / "BathIdentityCatalog.java"
        ),
        "t48_identity_catalog_json": _file(common.BUNDLED_IDENTITY_CATALOG),
    }
    t48_trees = {
        "generated_recipes": _tree(common.GENERATED_ROOT),
        "locked_support": _tree(common.LOCKED_SUPPORT_RECIPE_ROOT, allow_empty=True),
    }
    isolation = {
        "build_gradle": _file(ROOT / "build.gradle"),
        "gitignore": _file(ROOT / ".gitignore"),
        "gametest_java": _optional_file(common.GAME_TEST_JAVA),
    }
    overlays = {
        "publication_group_manifest": _optional_file(common.PUBLICATION_GROUP_MANIFEST),
        "shard_manifest": _optional_file(common.SHARD_MANIFEST),
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
        "status": "T48_RUNTIME_DEPENDENCIES_BOUND",
        "t48_trees": t48_trees,
        "verification": {
            "builder_policy": _file(ROOT / "tools" / "verification_builder_policy.json"),
        },
        "centrifuge_premerge_whitelist_relaxed": False,
        "historical_java_t48_whitelist": False,
    }


def main(argv: list[str] | None = None) -> int:
    from tools import closeout_seal

    return common.run_managed(
        "Bind T48 runtime dependencies",
        OUTPUT,
        build=build,
        check=lambda: closeout_seal.live_or_sealed_errors(
            "T48",
            "runtime_dependency_manifest",
            lambda: common.check_document(OUTPUT, build()),
        ),
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
