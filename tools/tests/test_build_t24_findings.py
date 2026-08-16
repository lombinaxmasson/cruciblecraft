from __future__ import annotations

import copy
import json
import unittest
from unittest import mock

from tools import build_t24_findings as builder


class T24FindingsTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        self.assertEqual([], builder.check())

    def test_ledger_is_ready_with_derived_counts(self) -> None:
        document = builder.build()
        self.assertEqual("FINDINGS_LEDGER_READY", document["status"])
        rows = document["findings"]
        self.assertEqual(5, len(rows))
        self.assertEqual(
            {
                "total": 5,
                "blocking": 0,
                "non_blocking": 5,
                "skipped_measurement": 2,
            },
            document["counts"],
        )

    def test_no_finding_blocks_beta(self) -> None:
        document = builder.build()
        for row in document["findings"]:
            self.assertFalse(row["blocks_beta"], row["id"])
            self.assertEqual("non_blocking", row["disposition"], row["id"])
            self.assertEqual("T25", row["owner"], row["id"])

    def test_every_finding_carries_a_reproducible_contract(self) -> None:
        document = builder.build()
        for row in document["findings"]:
            self.assertTrue(row["title"], row["id"])
            self.assertTrue(row["reproduce_command"], row["id"])
            self.assertTrue(row["failure_boundary"], row["id"])
            self.assertIn(
                row["evidence_class"],
                (
                    "STATIC_INFERENCE",
                    "SYNTHETIC_BENCHMARK",
                    "MEASURED_AT_SCALE",
                ),
                row["id"],
            )
            self.assertIn("#", row["evidence_artifact"], row["id"])

    def test_mutation_blocking_finding_is_rejected(self) -> None:
        rows = copy.deepcopy(builder.FINDINGS)
        rows[0]["blocks_beta"] = True
        with mock.patch.object(builder, "FINDINGS", rows):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_unresolvable_pointer_is_rejected(self) -> None:
        rows = copy.deepcopy(builder.FINDINGS)
        rows[0]["evidence_artifact"] = (
            "tools/t24_scale_evidence.json#/no/such/path"
        )
        with mock.patch.object(builder, "FINDINGS", rows):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_missing_field_is_rejected(self) -> None:
        rows = copy.deepcopy(builder.FINDINGS)
        del rows[0]["failure_boundary"]
        with mock.patch.object(builder, "FINDINGS", rows):
            with self.assertRaises(ValueError):
                builder.build()


if __name__ == "__main__":
    unittest.main()
