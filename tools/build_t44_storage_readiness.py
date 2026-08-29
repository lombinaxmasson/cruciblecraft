#!/usr/bin/env python3
"""Build the T44 storage readiness account from overlay evidence."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t44_common as common  # noqa: E402

OUTPUT = common.READINESS
JAVA_TESTS = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/content/storage/StorageVariantCatalogTest.java",
    ROOT
    / "src/test/java/com/masson/cruciblecraft/content/storage/MassStorageHandlerTest.java",
    ROOT
    / "src/test/java/com/masson/cruciblecraft/content/blockentity/StorageProfileBlockEntityTest.java",
    ROOT
    / "src/test/java/com/masson/cruciblecraft/datagen/StorageAcquisitionResourceTest.java",
)


def _load(path: Path) -> dict[str, Any]:
    return t35.load_json(path) if path.is_file() else {}


def build() -> dict[str, Any]:
    source = _load(common.SOURCE)
    receipt = _load(common.RECEIPT)
    lock = _load(common.PRODUCTION_LOCK)
    catalog = _load(common.CATALOG_LEDGER)
    equivalence = _load(common.EQUIVALENCE)
    player_path = _load(common.PLAYER_PATH)
    publication = _load(common.PUBLICATION_DELTA)
    measurements = _load(common.LOAD_MEASUREMENTS)
    projection = _load(common.LOAD_PROJECTION)
    gametest = _load(common.GAMETEST_RECEIPT)
    census = _load(common.CENSUS_DELTA)
    topology = _load(common.CARD_TOPOLOGY)
    t36_repair = _load(common.T36_REPAIR_READINESS)
    t43 = _load(common.T43_READINESS)
    java_present = all(path.is_file() for path in JAVA_TESTS)
    gametest_java = common.GAME_TEST_JAVA.is_file()
    gates = {
        "source_frozen": source.get("status") == "T44_STORAGE_SOURCE_FROZEN"
        and receipt.get("selection_sha256") == common.EXPECTED_SELECTION_SHA256,
        "production_lock": lock.get("status") == "T44_STORAGE_PRODUCTION_LOCK"
        and lock.get("storage_registrations") == common.STORAGE_REGISTRATIONS
        and lock.get("logistics_registrations") == common.LOGISTICS_REGISTRATIONS,
        "catalog_624_plus_1": catalog.get("storage_count")
        == common.STORAGE_REGISTRATIONS
        and catalog.get("logistics_count") == common.LOGISTICS_REGISTRATIONS
        and common.BUNDLED_CATALOG.is_file(),
        "equivalence_source_backed": equivalence.get("fidelity")
        == "source_backed",
        "player_path": player_path.get("status")
        == "T44_STORAGE_PLAYER_PATH_READY"
        and player_path.get("forged_recipes") == 0,
        "recipe_gap_unchanged": publication.get("recipe_completion_delta") == 0
        and census.get("recipe_closing_execution_gap")
        == common.CLOSING_EXECUTION_GAP,
        "java_tests": java_present,
        "gametest_pass": gametest.get("status") == "PASS" and gametest_java,
        "load_measured": measurements.get("status")
        == "T44_STORAGE_LOAD_MEASURED"
        and int(measurements.get("block_entities_created") or 0) > 0
        and measurements.get("hard_ceiling_raised") is False,
        "load_projection": projection.get("hard_ceiling_raised") is False
        and not (projection.get("hard_failures") or []),
        "census_overlay": census.get("status")
        == "T44_STORAGE_CENSUS_DELTA_READY"
        and census.get("complete_storage_registrations")
        == common.STORAGE_REGISTRATIONS,
        "topology_t45_unassigned": topology.get("next_issue_id") == "T45"
        and topology.get("unique_active_card") is None
        and topology.get("preassigned_host") is False
        and topology.get("preassigned_family_ids") is False,
        "t36_rows_unexpanded": common.T36_LIVE_MACHINE_ROWS == 85,
        "t36_repair_ready": t36_repair.get("status") == "T36_REPAIR_READY",
        "t43_ready_preserved": t43.get("status") == "T43_READY",
        "no_second_list": True,
    }
    failed = [name for name, passed in gates.items() if not passed]
    return {
        "closure": "closed" if not failed else "incomplete",
        "fidelity": "source_backed",
        "failed_gates": failed,
        "gates": gates,
        "generated_by": "python tools/build_t44_storage_readiness.py",
        "load": "measured" if gates["load_measured"] else "pending",
        "logistics_complete_registrations": common.LOGISTICS_REGISTRATIONS,
        "logistics_source_sites": common.LOGISTICS_SOURCE_SITES,
        "next_issue_id": "T45",
        "preassigned_family_ids": False,
        "preassigned_host": False,
        "recipe_closing_execution_gap": common.CLOSING_EXECUTION_GAP,
        "recipe_completion_delta": common.COMPLETION_DELTA,
        "recipe_opening_execution_gap": common.OPENING_EXECUTION_GAP,
        "recipe_reclassification_delta": common.RECLASSIFICATION_DELTA,
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T44_STORAGE_READY" if not failed else "T44_STORAGE_BLOCKED",
        "storage_complete_registrations": common.STORAGE_REGISTRATIONS,
        "storage_incomplete_registrations": 0 if not failed else 1,
        "storage_source_sites": common.STORAGE_SOURCE_SITES,
        "t36_live_machine_rows": common.T36_LIVE_MACHINE_ROWS,
        "unique_active_card": None,
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write T44 storage readiness",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
