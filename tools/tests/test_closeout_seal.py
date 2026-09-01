"""Contract tests for closeout seals and closed-card monotonic --check."""
from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t46_census_delta as t46_census
from tools import closeout_seal
from tools import t35_common as t35
from tools import t46_common as t46
from tools import t47_common as t47


class CloseoutSealTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not closeout_seal.is_sealed("T46") or not closeout_seal.is_sealed("T47"):
            raise unittest.SkipTest("T46/T47 closeout seals are not on disk yet")
        cls.t46_seal = closeout_seal.load_seal("T46")
        cls.t47_seal = closeout_seal.load_seal("T47")

    def test_t46_and_t47_seals_pin_closing_ledger(self) -> None:
        self.assertEqual("SEALED", self.t46_seal["status"])
        self.assertEqual("T46", self.t46_seal["card_id"])
        self.assertEqual(803, self.t46_seal["complete_family_count"])
        self.assertEqual(1517, self.t46_seal["relation_count"])
        self.assertEqual(0, self.t46_seal["reclassification_delta"])
        self.assertEqual(1894, self.t46_seal["remaining_recipe_gap"])
        self.assertEqual("PASS", self.t46_seal["gametest_status"])
        self.assertEqual("SEALED", self.t47_seal["status"])
        self.assertEqual(395, self.t47_seal["complete_family_count"])
        self.assertEqual(13708, self.t47_seal["relation_count"])
        self.assertEqual(0, self.t47_seal["reclassification_delta"])
        self.assertEqual(1499, self.t47_seal["remaining_recipe_gap"])
        self.assertEqual("PASS", self.t47_seal["gametest_status"])
        self.assertTrue(self.t46_seal["hashes"]["census"])
        self.assertTrue(self.t47_seal["hashes"]["topology"])

    def test_closed_cards_check_current(self) -> None:
        self.assertEqual([], closeout_seal.check_census("T46"))
        self.assertEqual([], closeout_seal.check_census("T47"))
        self.assertEqual([], closeout_seal.check_topology("T47"))
        self.assertEqual([], t46_census.check())

    def test_t46_census_check_does_not_call_live_gametest_or_v2(self) -> None:
        with (
            mock.patch.object(
                t46,
                "player_gametest_present",
                side_effect=AssertionError("live GameTest"),
            ),
            mock.patch.object(
                t46,
                "bound_gametest_artifacts",
                side_effect=AssertionError("live bound artifacts"),
            ),
            mock.patch.object(
                t46_census,
                "build",
                side_effect=AssertionError("live census rebuild"),
            ),
        ):
            self.assertEqual([], t46_census.check())
        census = t35.load_json(t46.CENSUS_DELTA)
        self.assertEqual(803, census["complete_family_count"])

    def test_fake_composed_v2_group_keeps_t46_complete_count(self) -> None:
        runtime = json.loads(t47.RUNTIME_MANIFEST_V2.read_text(encoding="utf-8"))
        groups = list(runtime.get("groups") or [])
        fake = dict(groups[-1]) if groups else {"publication_group": "cruciblecraft:t48_fake"}
        fake["wave_id"] = "T48"
        fake["publication_group"] = "cruciblecraft:t48_fake"
        runtime["groups"] = groups + [fake]
        runtime["group_count"] = len(runtime["groups"])
        with tempfile.TemporaryDirectory() as tmp:
            fake_path = Path(tmp) / "compact_recipe_runtime_manifest.v2.json"
            fake_path.write_text(json.dumps(runtime), encoding="utf-8")
            with mock.patch.object(t46, "RUNTIME_MANIFEST_V2", fake_path):
                with mock.patch.object(t47, "RUNTIME_MANIFEST_V2", fake_path):
                    self.assertEqual([], closeout_seal.check_census("T46"))
                    self.assertEqual([], t46_census.check())
        census = t35.load_json(t46.CENSUS_DELTA)
        self.assertEqual(803, census["complete_family_count"])
        lock_before = t46.production_lock_sha256()
        self.assertEqual(
            "454a49b25ff329dbb053b9195841fe9bb51dcbc311fd293f990b354218a762f4",
            lock_before,
        )

    def test_corrupt_t46_seal_fails_closed(self) -> None:
        real_path = closeout_seal.seal_path
        with tempfile.TemporaryDirectory() as tmp:
            corrupt = Path(tmp) / "t46_closeout_seal.json"
            corrupt.write_text("{not-json", encoding="utf-8")

            def fake_path(card_id: str) -> Path:
                if card_id == "T46":
                    return corrupt
                return real_path(card_id)

            with mock.patch.object(closeout_seal, "seal_path", side_effect=fake_path):
                errors = closeout_seal.check_census("T46")
            self.assertTrue(any("CORRUPT" in error for error in errors), errors)
            self.assertNotEqual([], errors)

    def test_profile_bytes_are_not_live_pinned_on_receipts(self) -> None:
        from tools import wave_bath_tiny_purified as tiny_purified

        t46_bound = t46.bound_gametest_artifacts()
        t47_bound = t47.bound_gametest_artifacts()
        tiny_bound = tiny_purified.bound_gametest_artifacts()
        self.assertNotIn("identity_ledger_v2", t46_bound)
        self.assertNotIn("runtime_manifest_v2", t46_bound)
        self.assertNotIn("identity_ledger_v2", t47_bound)
        self.assertNotIn("runtime_manifest_v2", t47_bound)
        self.assertNotIn("identity_ledger_v2", tiny_bound)
        self.assertNotIn("runtime_manifest_v2", tiny_bound)
        # Later prefix/form cards may drift live gate hashes. Closed-card
        # monotonic --check is seal-backed and must not require a GameTest rerun.
        self.assertEqual([], closeout_seal.check_census("T46"))
        self.assertEqual([], closeout_seal.check_census("T47"))
        self.assertEqual([], closeout_seal.check_receipt_binding("T49"))
        self.assertEqual([], tiny_purified.gametest_receipt_errors())

    def test_unique_active_card_is_content_only(self) -> None:
        topology = t35.load_json(t47.CARD_TOPOLOGY)
        self.assertIsNone(topology.get("unique_active_card"))
        self.assertNotEqual("T47-VR", topology.get("unique_active_card"))
        self.assertEqual("T48", topology.get("next_issue_id"))
        self.assertTrue(topology.get("t47_complete"))

    def test_publication_and_shard_checks_are_seal_backed(self) -> None:
        from tools import build_t46_publication_group_manifest as t46_pub
        from tools import build_t46_shard_manifest as t46_shard

        self.assertEqual([], closeout_seal.check_publication_group_manifest("T46"))
        self.assertEqual([], closeout_seal.check_shard_manifest("T46"))
        self.assertEqual([], closeout_seal.check_publication_group_manifest("T47"))
        self.assertEqual([], closeout_seal.check_shard_manifest("T47"))
        self.assertEqual([], closeout_seal.check_production_lock("T46"))
        self.assertEqual([], closeout_seal.check_production_lock("T47"))
        from tools import build_t47_production_lock as t47_lock

        with mock.patch.object(
            t47_lock, "build", side_effect=AssertionError("live lock rebuild")
        ):
            self.assertEqual(0, t47_lock.main(["--check"]))
        with mock.patch.object(
            t46_pub, "build", side_effect=AssertionError("live publication rebuild")
        ):
            self.assertEqual(0, t46_pub.main(["--check"]))
        with mock.patch.object(
            t46_shard, "build", side_effect=AssertionError("live shard rebuild")
        ):
            self.assertEqual(0, t46_shard.main(["--check"]))

    def test_write_once_refuses_replace(self) -> None:
        before = closeout_seal.seal_path("T46").read_bytes()
        try:
            written = closeout_seal.write_seal("T46")
        except ValueError as exc:
            self.assertIn("write-once", str(exc))
            self.assertEqual(before, closeout_seal.seal_path("T46").read_bytes())
            return
        self.assertEqual("T46", written["card_id"])
        self.assertEqual(before, closeout_seal.seal_path("T46").read_bytes())


if __name__ == "__main__":
    unittest.main()
