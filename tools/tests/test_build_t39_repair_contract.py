"""Contract tests for the T39-Repair production authority."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t39_layered_player_path as layered_builder  # noqa: E402
from tools import build_t39_operand_disposition as operand_builder  # noqa: E402
from tools import build_t39_production_lock as lock_builder  # noqa: E402
from tools import t39_common as t39  # noqa: E402


class T39RepairContractTest(unittest.TestCase):
    def test_production_lock_is_current_and_not_auto_resigned(self) -> None:
        document = lock_builder.build()
        self.assertEqual("T39_PRODUCTION_LOCKED", document["status"])
        self.assertEqual(t39.production_family_count(), document["production"]["family_count"])
        self.assertEqual(
            t39.production_relation_count(),
            document["production"]["relation_count"],
        )
        self.assertEqual(34, document["support"]["route_count"])
        self.assertEqual(7, len(document["phase_deferred"]))
        self.assertEqual([], lock_builder.check())
        with self.assertRaises(ValueError):
            lock_builder.write(approve_resign=False)

    def test_operand_disposition_blocks_no_production_family(self) -> None:
        document = operand_builder.build()
        self.assertEqual("T39_OPERAND_DISPOSITION_REVIEWED", document["status"])
        self.assertEqual([], document["blocked_production_families"])
        deferred = [
            row for row in document["rows"] if row["disposition"] == "phase_deferred"
        ]
        self.assertEqual(7, len({row["family_id"] for row in deferred}))
        self.assertTrue(
            all(row["future_owner"] == "post_1x:nuclear" for row in deferred)
        )

    def test_layered_player_path_is_non_circular_and_lock_bound(self) -> None:
        document = layered_builder.build()
        self.assertEqual("T39_LAYERED_PLAYER_PATH_READY", document["status"])
        self.assertEqual(t39.production_lock_sha256(), document["production_lock_sha256"])
        self.assertTrue(document["layers"]["b0"]["excludes_t39_production"])
        self.assertTrue(document["layers"]["b0"]["excludes_t39_support"])
        self.assertEqual(34, document["layers"]["b1"]["route_count"])
        self.assertEqual(
            t39.production_relation_count(),
            document["layers"]["b2"]["relation_count"],
        )
        self.assertTrue(all(document["validators"].values()))

    def test_catalog_fixture_is_outside_main_resource_roots(self) -> None:
        self.assertTrue(t39.CATALOG_FIXTURE_ROOT.is_relative_to(ROOT / "src/test"))
        self.assertFalse(t39.GENERATED_ROOT.is_relative_to(ROOT / "src/test"))
        self.assertNotEqual(t39.CATALOG_FIXTURE_ROOT, t39.GENERATED_ROOT)
        self.assertNotEqual(t39.LOCKED_SUPPORT_ROOT, t39.PLAYER_PATH_RECOVERY_ROOT)
        self.assertTrue(t39.PLAYER_PATH_RECOVERY_ROOT.is_relative_to(ROOT / "src/test"))
        self.assertFalse(t39.PLAYER_PATH_RECOVERY_ROOT.is_relative_to(ROOT / "src/main"))
        self.assertFalse(any(t39.LEGACY_PLAYER_PATH_RECOVERY_ROOT.glob("*.json")))


if __name__ == "__main__":
    unittest.main()
