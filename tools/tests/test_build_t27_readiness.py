from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t27_portfolio as portfolio
from tools import build_t27_readiness as builder
from tools import t27_common as common


class T27ReadinessTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        stripped = lambda document: {
            key: value
            for key, value in document.items()
            if key not in builder.REPORT_OWNED
        }
        self.assertEqual(stripped(expected), stripped(on_disk))
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_three_axis_freeze_evidence_is_complete(self) -> None:
        document = builder.build()
        closure = document["closure"]
        self.assertTrue(closure["opening_bound_to_t26_ready"])
        self.assertEqual(765, closure["canonical_total"])
        self.assertEqual(0, closure["unclassified"])
        self.assertEqual(765, closure["aggregate_canonical"])
        self.assertTrue(closure["aggregate_validators_clear"])
        self.assertEqual(0, closure["open_item_orphan"])
        self.assertTrue(closure["tracks_not_started"])
        self.assertTrue(closure["t28_plus_covers_work_set"])
        self.assertTrue(closure["t28_plus_not_started"])
        self.assertEqual(0, closure["work_set_size"])
        fidelity = document["fidelity"]
        self.assertTrue(fidelity["source_revision_pinned"])
        self.assertTrue(fidelity["t13_classification_kept"])
        self.assertTrue(fidelity["crucible_collision_forbidden"])
        self.assertTrue(fidelity["o36_v1_required"])
        self.assertTrue(fidelity["o41_post_1_0"])
        self.assertTrue(fidelity["anvil_bend_post_1_0"])
        self.assertTrue(fidelity["f003_f005_rc_contract"])
        self.assertTrue(fidelity["map_level_not_row_level"])
        self.assertTrue(fidelity["t28_plus_card_count_unfilled"])
        self.assertTrue(fidelity["rc_number_absent"])
        load_data = document["load"]
        self.assertTrue(load_data["eager_hard_ceiling_is_21000"])
        self.assertEqual(19087, load_data["opening_publication"]["logical"])
        self.assertEqual(16862, load_data["opening_publication"]["eager"])
        self.assertEqual(2225, load_data["opening_publication"]["lazy"])
        self.assertEqual(
            {"eager": 0, "lazy": 0, "logical": 0, "scope": "T27_CARD"},
            load_data["publication_delta"],
        )
        self.assertTrue(load_data["pending_not_filled_with_zero"])
        self.assertEqual("run_full_verification", document["status_owner"])

    def test_status_is_ready_only_when_runtime_and_t26_pass(self) -> None:
        document = builder.build()
        if document.get("status") == "T27_READY":
            self.assertTrue(document["runtime"]["gametest_passing"])
            self.assertIn(document["runtime"]["gametest_total"], (121, 131, 137))
            self.assertEqual(list(builder.STAGES), document["completed_stages"])
        else:
            self.assertNotIn("status", document)

    def test_mutating_portfolio_or_topology_hash_fails_closed(self) -> None:
        real = common.sha256_file

        def override(path):
            if path in {portfolio.AGGREGATE, builder.topology.OUTPUT}:
                return "0" * 64
            return real(path)

        with mock.patch.object(common, "sha256_file", side_effect=override):
            errors = builder.check()
        self.assertTrue(any("stale" in error for error in errors))

    def test_status_owner_forbids_hand_written_ready(self) -> None:
        document = builder.build()
        self.assertEqual("run_full_verification", document["status_owner"])
        self.assertIn("status", builder.REPORT_OWNED)
        self.assertIn("currentness", builder.REPORT_OWNED)


if __name__ == "__main__":
    unittest.main()
