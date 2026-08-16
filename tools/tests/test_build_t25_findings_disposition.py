from __future__ import annotations

import copy
import json
import unittest
from unittest import mock

from tools import build_t25_findings_disposition as builder


class T25FindingsDispositionTest(unittest.TestCase):
    def setUp(self) -> None:
        self.ledger = json.loads(
            builder.T24_FINDINGS.read_text(encoding="utf-8")
        )

    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        self.assertEqual([], builder.check())

    def test_zero_selected_close_with_complete_dispositions(self) -> None:
        document = builder.build()
        self.assertEqual("T25_DISPOSITIONS_COMPLETE", document["status"])
        self.assertEqual(
            {
                "total": 5,
                "selected_fixed": 0,
                "non_blocking": 5,
                "invalid_measurement": 0,
                "post_1_0": 0,
            },
            document["counts"],
        )

    def test_dispositions_are_a_bijection_with_the_t24_ledger(self) -> None:
        document = builder.build()
        ledger_ids = {
            row["id"] for row in self.ledger["findings"]
        }
        disposition_ids = {
            row["id"] for row in document["dispositions"]
        }
        self.assertEqual(ledger_ids, disposition_ids)

    def test_every_row_is_complete_and_in_vocabulary(self) -> None:
        document = builder.build()
        for row in document["dispositions"]:
            self.assertIn(
                row["disposition"], builder.DISPOSITION_VOCABULARY,
                row["id"])
            self.assertTrue(row["reason"], row["id"])
            self.assertTrue(row["owner"], row["id"])
            self.assertTrue(row["evidence_artifact"], row["id"])
            self.assertFalse(row["blocks_beta"], row["id"])

    def test_skipped_findings_carry_the_t24_recheck_contract(self) -> None:
        document = builder.build()
        rows = {row["id"]: row for row in document["dispositions"]}
        for finding_id in builder.SKIPPED_FINDINGS:
            contract = rows[finding_id].get("recheck_contract")
            self.assertIsNotNone(contract, finding_id)
            self.assertTrue(
                contract["replacement_condition"], finding_id)
            self.assertEqual(
                "T26 Beta candidate",
                contract["recheck_point"],
                finding_id)

    def test_mutation_blocking_finding_must_be_selected(self) -> None:
        ledger = copy.deepcopy(self.ledger)
        ledger["findings"][0]["blocks_beta"] = True
        with self._ledger_override(ledger):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_unknown_finding_is_rejected(self) -> None:
        ledger = copy.deepcopy(self.ledger)
        ledger["findings"][0]["id"] = "T24-F099"
        with self._ledger_override(ledger):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_missing_finding_is_rejected(self) -> None:
        ledger = copy.deepcopy(self.ledger)
        ledger["findings"] = ledger["findings"][:-1]
        with self._ledger_override(ledger):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_skipped_finding_without_contract_is_rejected(
        self,
    ) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.T24_SCALE_EVIDENCE:
                document = copy.deepcopy(real(path, {}) or {})
                document["measured_at_scale"] = {
                    name: {"status": "MEASURED_AT_SCALE"}
                    for name in document.get("measured_at_scale", {})
                }
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            with self.assertRaises(ValueError):
                builder.build()

    def _ledger_override(self, ledger):
        real = builder._load_if_exists
        return mock.patch.object(
            builder,
            "_load_if_exists",
            side_effect=lambda p, d=None: (
                ledger if p == builder.T24_FINDINGS else real(p, d)
            ),
        )


if __name__ == "__main__":
    unittest.main()
