from __future__ import annotations

import unittest

from tools import build_t30_phase5_1_contract as builder
from tools import t27_common as common


class T30Phase51ContractTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_work_set_is_t30_and_rc_stays_forbidden(self) -> None:
        document = builder.build()
        self.assertEqual("PHASE5_1_PRE_RC_LOGISTICS_CONTRACT", document["status"])
        self.assertEqual(common.SOURCE_REVISION, document["source_revision"])
        self.assertEqual("T30", document["work_set"]["current_active_t"])
        self.assertEqual("T31", document["work_set"]["next_t"])
        self.assertEqual("T30_READY", document["work_set"]["t31_depends_on"])
        self.assertEqual(1, document["work_set"]["maximum_active_t_cards"])
        self.assertEqual(
            "forbidden_until_T30_READY",
            document["rc_policy"]["rc_numbering"],
        )
        self.assertIsNone(document["rc_policy"]["rc_number"])
        self.assertEqual("0.1.0-beta.1", document["rc_policy"]["mod_version_remains"])
        self.assertTrue(document["rc_policy"]["f003_f005_remain_rc_recheck"])
        self.assertEqual("T29_READY", document["opening"]["t29_status"])
        self.assertEqual("READY", document["opening"]["report_status"])
        self.assertIsNone(document["opening"]["rc_number"])
        self.assertEqual(60, document["catalog"]["source_rows"])
        self.assertEqual(121, document["catalog"]["total_new_blocks"])
        self.assertEqual("cruciblecraft:steel_dust_funnel", document["catalog"]["dust_funnel_id"])
        self.assertIn("hopper", document["catalog"]["bare_ids_forbidden"])
        self.assertIn("queue_hopper", document["catalog"]["bare_ids_forbidden"])
        self.assertEqual(
            ["dust", "small_dust", "tiny_dust"],
            document["dust_forms"],
        )
        self.assertFalse(document["recipe_adaptation"]["plate_curved_runtime"])
        self.assertIn("tools/phase5_portfolio_contract.json", document["historical_artifacts"]["do_not_rewrite"])
        self.assertIn("item_barrels", document["post_1_0_exclusions"])
        self.assertIn("fluid_funnel", document["post_1_0_exclusions"])
        self.assertEqual(
            "frozen_legacy_baseline",
            document["legacy_machine_policy"]["tier_1_bare_id"],
        )


if __name__ == "__main__":
    unittest.main()
