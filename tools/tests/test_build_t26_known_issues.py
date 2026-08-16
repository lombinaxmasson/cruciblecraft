from __future__ import annotations

import copy
import json
import unittest
from unittest import mock

from tools import build_t26_known_issues as builder


class T26KnownIssuesTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        self.assertEqual([], builder.check())

    def test_ledger_covers_playtest_and_t25_without_beta_blockers(self) -> None:
        document = builder.build()
        self.assertEqual("T26_KNOWN_ISSUES_COMPLETE", document["status"])
        self.assertEqual(
            {
                "total": 15,
                "playtest": 10,
                "inherited_t25": 5,
                "post_beta_polish": 10,
                "non_blocking": 5,
                "blocks_beta": 0,
            },
            document["counts"],
        )
        ids = [row["id"] for row in document["issues"]]
        self.assertEqual(
            [f"CC-4.5-P{index}" for index in range(10)]
            + ["T24-F001", "T24-F002", "T24-F003", "T24-F004", "T24-F005"],
            ids,
        )
        for row in document["issues"]:
            self.assertFalse(row["blocks_beta"], row["id"])
            self.assertTrue(row["workaround"], row["id"])
            self.assertTrue(row["owner"], row["id"])
            self.assertIn(row["severity"], builder.SEVERITY_VOCABULARY)

    def test_f003_and_f005_transfer_to_t27_rc(self) -> None:
        rows = {row["id"]: row for row in builder.build()["issues"]}
        for finding_id in ("T24-F003", "T24-F005"):
            row = rows[finding_id]
            self.assertEqual("T27 RC", row["owner"])
            self.assertEqual(
                "T27 RC candidate",
                row["recheck_contract"]["recheck_point"],
            )
            self.assertTrue(row["recheck_contract"]["replacement_condition"])

    def test_freeze_closes_o15_and_parks_anvil_bend_and_crucible(self) -> None:
        freeze = builder.build()["freeze"]
        self.assertEqual("closed", freeze["o15"]["disposition"])
        self.assertEqual(
            ["cruciblecraft:anvil_bend_big", "cruciblecraft:anvil_bend_small"],
            [row["id"] for row in freeze["anvil_bend"]],
        )
        for row in freeze["anvil_bend"]:
            self.assertEqual("post_1_0", row["disposition"])
            self.assertTrue(row["v1_replacement"])
        crucible = freeze["crucible"]
        self.assertEqual("v1_required", crucible["disposition"])
        self.assertEqual("T27", crucible["owner"])
        self.assertEqual("none", crucible["implementation"])
        self.assertIn("cruciblecraft:crucible", crucible["naming_constraint"])

    def test_mutation_blocking_playtest_issue_fails_closed(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.POLICY:
                document = copy.deepcopy(real(path, {}) or {})
                document["playtest_issues"][0]["blocks_beta"] = True
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_missing_zero_gap_domain_fails_o15(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.LOCALIZATION:
                document = copy.deepcopy(real(path, {}) or {})
                document["domains"]["screen"]["zero_gap"] = False
                return document
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            with self.assertRaises(ValueError):
                builder.build()


if __name__ == "__main__":
    unittest.main()
