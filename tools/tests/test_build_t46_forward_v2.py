#!/usr/bin/env python3
"""Contract tests for T46 history freeze and forward-v2 authority composition."""
from __future__ import annotations

import unittest

from tools import build_forward_recipe_authority_v2_readiness as readiness
from tools import build_global_build_identity_ledger_v2 as identity_v2
from tools import build_t14_load_budget_policy_v2 as policy_v2
from tools import build_t46_history_freeze as freeze
from tools import t35_common as t35
from tools import t46_common as common
from tools.recipe_bulk import runtime_v2


class T46ForwardV2Test(unittest.TestCase):
    def test_opening_is_t45_closing(self) -> None:
        opening = common.opening_from_t45()
        self.assertEqual(2697, opening["execution_gap"])
        self.assertEqual(4865, opening["authored_entries"])
        self.assertEqual(16659, opening["eager_rows"])
        self.assertEqual(3466, opening["lazy_rows"])
        self.assertEqual(174, opening["cache_ceiling_rows"])

    def test_history_freeze_pins_v1_byte_hashes(self) -> None:
        document = freeze.build()
        self.assertEqual("T46_HISTORY_FREEZE", document["status"])
        self.assertEqual("byte_hash_only", document["check_mode"])
        self.assertEqual(
            t35.sha256_file(common.V1_LOAD_POLICY),
            document["v1_authorities"]["t14_load_budget_policy"],
        )
        self.assertEqual(
            t35.sha256_file(common.V1_IDENTITY_LEDGER),
            document["v1_authorities"]["global_build_identity_ledger"],
        )
        self.assertEqual(
            t35.sha256_file(common.V1_RUNTIME_MANIFEST),
            document["v1_authorities"]["compact_recipe_runtime_manifest"],
        )
        self.assertGreaterEqual(document["file_count"], 3)
        self.assertFalse(
            any(
                str(row.get("path") or "").endswith("_closeout_seal.json")
                for row in document.get("files") or []
            )
        )
        frozen = t35.load_json(common.HISTORY_FREEZE)
        self.assertEqual(frozen["file_count"], document["file_count"])

    def test_v2_policy_only_overrides_authored_verdict(self) -> None:
        v1 = common.load_json(common.V1_LOAD_POLICY)
        v2 = policy_v2.build()
        self.assertEqual("FORWARD_LOAD_BUDGET_POLICY_V2", v2["status"])
        self.assertEqual(
            "REPORT_ONLY",
            v2["budgets"]["datapack_authored_entries"]["readiness_verdict"],
        )
        self.assertEqual(
            v1["budgets"]["eager_publication_rows"],
            v2["budgets"]["eager_publication_rows"],
        )
        self.assertEqual(
            t35.sha256_file(common.V1_LOAD_POLICY),
            v2["v1_base"]["file_sha256"],
        )

    def test_identity_v2_keeps_v1_records(self) -> None:
        v1 = common.load_json(common.V1_IDENTITY_LEDGER)
        composed = identity_v2.build()
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V2", composed["status"])
        self.assertEqual(
            v1["record_count"],
            composed["composition"]["base_record_count"],
        )
        self.assertGreaterEqual(composed["record_count"], v1["record_count"])
        self.assertEqual("pass", composed["composition"]["conflict_check"])

    def test_runtime_v2_keeps_twelve_historical_groups(self) -> None:
        v1 = common.load_json(common.V1_RUNTIME_MANIFEST)
        composed = runtime_v2.build()
        self.assertEqual(12, composed["composition"]["base_group_count"])
        self.assertTrue(composed["composition"]["historical_membership_roots_unchanged"])
        for left, right in zip(v1["groups"], composed["groups"][:12]):
            self.assertEqual(left["publication_group"], right["publication_group"])
            self.assertEqual(
                left["membership_root_sha256"],
                right["membership_root_sha256"],
            )

    def test_forward_v2_readiness_requires_history_freeze(self) -> None:
        document = readiness.build()
        self.assertIn(
            document["status"],
            {
                "FORWARD_RECIPE_AUTHORITY_V2_READY",
                "FORWARD_RECIPE_AUTHORITY_V2_BLOCKED",
            },
        )
        self.assertTrue(document["gates"]["policy_override_bounded"])
        self.assertTrue(document["gates"]["identity_composition_legal"])
        self.assertTrue(document["gates"]["runtime_composition_legal"])


if __name__ == "__main__":
    unittest.main()
