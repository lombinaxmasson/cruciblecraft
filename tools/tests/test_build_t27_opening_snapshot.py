from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t27_opening_snapshot as builder
from tools import t27_common as common


def _valid_identity() -> dict:
    return {
        "table": "multiblock_kinds",
        "canonical_id": "crucible",
        "source": {
            "revision": common.SOURCE_REVISION,
            "artifact": "tools/t13_denominators/multiblock_kinds.json",
            "artifact_sha256": "a" * 64,
            "record_sha256": "b" * 64,
        },
        "t13_classification": "in_scope",
        "cc_implementation": "none",
        "disposition": "v1_required",
        "reason": "Only unimplemented T23 v1_required multiblock.",
        "owner": "portfolio:v1/crucible",
        "dependencies": [
            {
                "kind": "open_item_id",
                "id": "O-36",
            }
        ],
        "replacement_condition": "Implement as a bounded thermal/steelmaking card.",
        "recheck_point": "generated T28+ thermal card",
        "axes": {
            "closure": {
                "status": "incomplete",
                "evidence": "cc_implementation is none",
            },
            "fidelity": {
                "status": "source_backed",
                "evidence": "T13 multiblock_kinds crucible row",
            },
            "load": {
                "status": "pending",
                "verdict": "BLOCKED_PENDING_MEASUREMENT",
                "evidence": "No T27 measurement of crucible load",
            },
        },
    }


class T27OpeningSnapshotTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_opening_uses_t26_ready_report_and_t26_5_current_publication(self) -> None:
        document = builder.build()
        self.assertEqual("T27_OPENING_SNAPSHOT", document["status"])
        self.assertEqual("READY", document["t26_gate"]["report_status"])
        self.assertEqual("T26_READY", document["t26_gate"]["t26_status"])
        current = document["publication"]["current"]
        self.assertEqual(19087, current["logical"])
        self.assertEqual(16862, current["eager"])
        self.assertEqual(2225, current["lazy"])
        historical = document["historical_baselines"]["t23_ledger"]
        self.assertEqual(18882, historical["logical"])
        self.assertEqual(16657, historical["eager"])
        self.assertFalse(historical["current_baseline"])
        self.assertNotEqual(current["logical"], historical["logical"])
        self.assertEqual(0, document["publication_delta"]["logical"])
        self.assertEqual(0, document["publication_delta"]["eager"])
        self.assertEqual(0, document["publication_delta"]["lazy"])
        self.assertIsNone(document["t28_plus_card_count"])
        self.assertEqual(765, document["t13_tables"]["canonical_identities_total"])
        self.assertEqual(21000, document["t14_budgets"]["eager_hard_ceiling"])
        self.assertEqual(78682, document["t22_5"]["ordinary_optional"])
        self.assertEqual(582, document["tests"]["java_unit_tests"])
        self.assertEqual(121, document["tests"]["production_game_tests"])
        self.assertEqual(3243, document["datapack"]["report_counter"]["value"])
        self.assertEqual(0, document["t26_known_issues"]["blocks_beta"])

    def test_mutating_an_input_hash_fails_closed(self) -> None:
        real_sha = common.sha256_file

        def override(path):
            if path.name == "t20_readiness.json":
                return "0" * 64
            return real_sha(path)

        with mock.patch.object(common, "sha256_file", side_effect=override):
            errors = builder.check()
        self.assertTrue(any("stale" in error for error in errors))

    def test_later_report_rewrite_keeps_frozen_t26_opening(self) -> None:
        real_sha = common.sha256_file

        def override(path):
            if path.name == "full_verification_report.json":
                return "0" * 64
            return real_sha(path)

        with mock.patch.object(common, "sha256_file", side_effect=override):
            self.assertEqual([], builder.check())
            document = builder.build()
        self.assertEqual(
            "8aa91164bffdc2e157b0bd86e4fa9e7bdbfd1f1908a16fad766d2b5c91bd449e",
            document["t26_gate"]["report_sha256"],
        )
        self.assertEqual(
            "20260817T204031.938332Z-c27e1677755d-03920af9",
            document["t26_gate"]["session"],
        )
        self.assertEqual(723, document["tests"]["python_unit_tests"])

    def test_schema_rejects_unknown_disposition(self) -> None:
        record = _valid_identity()
        record["disposition"] = "deferred"
        errors = common.validate_identity_record(record)
        self.assertTrue(any("disposition" in error for error in errors))

    def test_schema_rejects_empty_owner(self) -> None:
        record = _valid_identity()
        record["owner"] = ""
        errors = common.validate_identity_record(record)
        self.assertTrue(any("owner" in error for error in errors))

    def test_schema_rejects_pending_load_filled_with_zero(self) -> None:
        record = _valid_identity()
        record["axes"]["load"]["eager"] = 0
        errors = common.validate_identity_record(record)
        self.assertTrue(any("eager=0" in error for error in errors))

    def test_schema_rejects_free_text_dependencies(self) -> None:
        record = _valid_identity()
        record["dependencies"] = ["depends on later implementation"]
        errors = common.validate_identity_record(record)
        self.assertTrue(any("free-text" in error for error in errors))

    def test_valid_identity_record_is_accepted(self) -> None:
        self.assertEqual([], common.validate_identity_record(_valid_identity()))

    def test_contract_does_not_publish_content_or_t28_counts(self) -> None:
        contract = common.load_json(builder.CONTRACT)
        self.assertEqual("T27", contract["execution_policy"]["current_active_t"])
        self.assertIsNone(contract["execution_policy"]["next_t"])
        self.assertIsNone(contract["execution_policy"]["t28_plus_card_count"])
        self.assertEqual(
            {"eager": 0, "lazy": 0, "logical": 0},
            contract["publication_delta"],
        )
        self.assertFalse(any(track["started"] for track in contract["tracks"].values()))


if __name__ == "__main__":
    unittest.main()
