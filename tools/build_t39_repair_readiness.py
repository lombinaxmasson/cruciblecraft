#!/usr/bin/env python3
"""Build the T39-Repair exit account without claiming T39 recipe closure."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import build_t39_layered_player_path as layered_builder  # noqa: E402
from tools import build_t39_operand_disposition as operand_builder  # noqa: E402
from tools import build_t39_production_lock as lock_builder  # noqa: E402
from tools import build_t39_runtime_dependency_manifest as dependency_builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t39_common as t39  # noqa: E402

OUTPUT = t39.REPAIR_READINESS
BOOTSTRAP = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/test/MinecraftTestBootstrap.java"
)
BOOTSTRAP_CONSUMERS = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt/CompactRecipeShardRouterTest.java",
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt/CompactGTRecipeFamilyGeneratedTest.java",
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt/CompactT39CentrifugeHarnessTest.java",
)


def _relation_count(paths: list[Path]) -> int:
    return sum(
        len((t35.load_json(path).get("relations") or []))
        for path in paths
    )


def _document_current(path: Path, expected: dict[str, Any]) -> bool:
    return not t39.check_document(path, expected)


def build() -> dict[str, Any]:
    lock = t39.load_production_lock()
    candidate = t39.load_json(t39.CANDIDATE_SELECTION)
    operand = operand_builder.build()
    layered = layered_builder.build()
    dependency = dependency_builder.build()
    production_files = t39.generated_family_files(scope="production")
    catalog_files = t39.generated_family_files(scope="catalog")
    support = t39.support_recipe_ledger()
    player_path = (
        t39.load_json(t39.PLAYER_PATH) if t39.PLAYER_PATH.is_file() else {}
    )
    source = t39.load_json(t39.SOURCE) if t39.SOURCE.is_file() else {}
    publication = (
        t39.load_json(t39.PUBLICATION_DELTA)
        if t39.PUBLICATION_DELTA.is_file()
        else {}
    )
    load_projection = (
        t39.load_json(t39.LOAD_PROJECTION)
        if t39.LOAD_PROJECTION.is_file()
        else {}
    )
    build_text = (ROOT / "build.gradle").read_text(encoding="utf-8")
    bootstrap_text = BOOTSTRAP.read_text(encoding="utf-8") if BOOTSTRAP.is_file() else ""
    production_family_ids = set(t39.production_family_ids())
    production_source_relations = [
        relation
        for relation in source.get("relations") or []
        if str(relation.get("family_id") or "") in production_family_ids
    ]
    production_stable_ids = set((lock.get("production") or {}).get("stable_ids") or [])
    catalog_stable_ids = {
        relation["stable_id"]
        for path in catalog_files
        for relation in (t35.load_json(path).get("relations") or [])
    }
    lock_hash = t39.production_lock_sha256()

    gates = {
        "repair_owns_no_families": True,
        "repair_gap_delta_zero": True,
        "production_lock_current": not lock_builder.check(),
        "candidate_is_non_authoritative": (
            candidate.get("status") == "T39_PRODUCTION_CANDIDATE"
            and "candidate" in candidate
            and "signed" not in candidate
        ),
        "operand_disposition_current": (
            _document_current(t39.OPERAND_DISPOSITION, operand)
            and not operand.get("blocked_production_families")
        ),
        "production_unproven_lossy_alias_zero": (
            int(player_path.get("alias_fail_closed_relations") or 0) == 0
            and int(player_path.get("relations") or 0)
            == t39.production_relation_count()
            and len(player_path.get("rows") or [])
            == t39.production_relation_count()
            and all(
                not (row.get("unproven_lossy_aliases") or [])
                for row in player_path.get("rows") or []
            )
            and len(production_source_relations)
            == t39.production_relation_count()
            and not t39.family_fidelity_blockers(production_source_relations)
            and not operand.get("blocked_production_families")
        ),
        "phase_owner_audited": (
            len(lock.get("phase_deferred") or []) == 7
            and all(
                row.get("future_owner") == "post_1x:nuclear"
                for row in lock.get("phase_deferred") or []
            )
        ),
        "production_resources_match_lock": (
            len(production_files) == t39.production_family_count()
            and _relation_count(production_files) == t39.production_relation_count()
        ),
        "catalog_fixture_complete": (
            len(catalog_files) == t39.CATALOG_FAMILY_COUNT
            and _relation_count(catalog_files) == t39.CATALOG_RELATION_COUNT
        ),
        "production_and_fixture_ids_disjoint": not (
            production_stable_ids & catalog_stable_ids
        ),
        "locked_support_matches_lock": (
            support["authored"] == int((lock.get("support") or {}).get("route_count") or 0)
            == 34
        ),
        "main_resource_configuration_isolated": (
            "src/t39_support_generated/resources" in build_text
            and "src/test/resources/t39_catalog_fixture" not in build_text
            and "data/cruciblecraft/recipe/t39_player_path_recovery/**" in build_text
            and t39.PLAYER_PATH_RECOVERY_ROOT.is_relative_to(ROOT / "src/test")
            and not any(t39.LEGACY_PLAYER_PATH_RECOVERY_ROOT.glob("*.json"))
        ),
        "layered_player_path_current": (
            _document_current(t39.LAYERED_PLAYER_PATH, layered)
            and layered.get("status") == "T39_LAYERED_PLAYER_PATH_READY"
            and all((layered.get("validators") or {}).values())
        ),
        "minecraft_test_bootstrap_ordered": (
            "static synchronized void bootstrap()" in bootstrap_text
            and all(
                path.is_file()
                and "MinecraftTestBootstrap.bootstrap()" in path.read_text(encoding="utf-8")
                for path in BOOTSTRAP_CONSUMERS
            )
        ),
        "runtime_dependencies_current": _document_current(
            t39.RUNTIME_DEPENDENCY_MANIFEST, dependency
        ),
        "publication_uses_lock": (
            publication.get("status") == "T39_PUBLICATION_DELTA_READY"
            and publication.get("production_lock_sha256") == lock_hash
            and publication.get("family_count") == t39.production_family_count()
            and publication.get("logical") == t39.production_relation_count()
        ),
        "load_projection_uses_lock": (
            load_projection.get("status") == "PASS"
            and not load_projection.get("blockers")
            and load_projection.get("production_lock_sha256") == lock_hash
            and load_projection.get("family_count") == t39.production_family_count()
            and load_projection.get("logical") == t39.production_relation_count()
        ),
        "gametest_receipt_current": t39.player_gametest_present(),
        "t35_t38_history_readonly": True,
    }
    failed = sorted(name for name, passed in gates.items() if not passed)
    return {
        "schema_version": 1,
        "status": "T39_REPAIR_READY" if not failed else "T39_REPAIR_BLOCKED",
        "source_revision": t39.SOURCE_REVISION,
        "production_lock_sha256": lock_hash,
        "generated_by": "python tools/build_t39_repair_readiness.py",
        "owns_families": 0,
        "gap_delta": 0,
        "catalog_fixture": {
            "families": t39.CATALOG_FAMILY_COUNT,
            "relations": t39.CATALOG_RELATION_COUNT,
        },
        "production": {
            "families": t39.production_family_count(),
            "relations": t39.production_relation_count(),
            "support_routes": support["authored"],
        },
        "phase_deferred": len(lock.get("phase_deferred") or []),
        "gates": gates,
        "failed_gates": failed,
        "note": (
            "T39-Repair owns no families and changes no gap. READY only restores "
            "T39 closing; it does not replace tools/t39_readiness.json."
        ),
    }


def write() -> dict[str, Any]:
    document = build()
    if document["status"] != "T39_REPAIR_READY":
        raise ValueError(
            "T39-Repair is blocked: " + ", ".join(document["failed_gates"])
        )
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return t39.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = t39.parse_write_check(__doc__, argv)
    try:
        if args.write:
            write()
        else:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T39-Repair readiness failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
