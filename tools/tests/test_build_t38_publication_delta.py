from __future__ import annotations

import json
import unittest

from tools import build_t38_publication_delta as builder
from tools import t38_common as t38


class T38PublicationDeltaTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_on_demand_winner_counts_and_cache_are_card_level(self) -> None:
        document = builder.build()
        self.assertEqual("T38_PUBLICATION_DELTA_READY", document["status"])
        self.assertEqual("on_demand", document["production_winner"])
        self.assertEqual(t38.FAMILY_COUNT, document["family_count"])
        self.assertEqual(t38.FAMILY_COUNT, document["authored_compact_family_entries"])
        self.assertEqual(t38.SUPPORT_AUTHORED_COUNT, document["authored_support_entries"])
        self.assertEqual(t38.AUTHORED_DATAPACK_COUNT, document["authored_datapack_entries"])
        self.assertEqual(t38.SOURCE_ROWS, document["logical"])
        self.assertEqual(t38.SUPPORT_EAGER_COUNT, document["eager"])
        self.assertEqual(t38.SOURCE_ROWS, document["lazy"])
        self.assertEqual(0, sum(row["eager_publication_rows"] or 0 for row in document["families"]))
        support = document["support_recipes"]
        self.assertEqual(5, support["gt_recovery_count"])
        self.assertEqual(10, support["ore_chain_count"])
        self.assertEqual(4, support["crafting_count"])
        self.assertEqual(15, support["eager"])
        self.assertEqual(
            t38.SOURCE_ROWS,
            sum(row["lazy_logical_rows"] or 0 for row in document["families"]),
        )
        card = document["card_level_shared_costs"]
        self.assertEqual(16, card["lazy_cache_ceiling_rows"])
        self.assertTrue(card["recorded_once"])
        self.assertFalse(card["copied_per_family"])
        self.assertFalse(card["averaged_into_family_rows"])

    def test_family_source_rows_match_frozen_roaster_work_set(self) -> None:
        document = builder.build()
        by_template = {
            row["template_key"]: row["source_rows"]
            for row in document["families"]
        }
        self.assertEqual(38, by_template["gt.recipe.roaster#0000"])
        self.assertEqual(3, by_template["gt.recipe.roaster#0001"])
        self.assertEqual(2, by_template["gt.recipe.roaster#0003"])
        self.assertEqual(3, by_template["gt.recipe.roaster#0010"])
        self.assertEqual(3, by_template["gt.recipe.roaster#0014"])
        singletons = [
            key for key, count in by_template.items()
            if key not in {
                "gt.recipe.roaster#0000",
                "gt.recipe.roaster#0001",
                "gt.recipe.roaster#0003",
                "gt.recipe.roaster#0010",
                "gt.recipe.roaster#0014",
            }
        ]
        self.assertEqual(24, len(singletons))
        self.assertTrue(all(by_template[key] == 1 for key in singletons))
        self.assertNotIn("gt.recipe.roaster#0002", by_template)

    def test_does_not_copy_t37_hybrid_or_cache_eight(self) -> None:
        document = builder.build()
        encoded = json.dumps(document)
        self.assertNotIn('"eager": 14', encoded)
        self.assertNotIn('"lazy": 36', encoded)
        self.assertNotIn('"lazy_cache_ceiling_rows": 8', encoded)
        for row in document["families"]:
            self.assertNotIn("#0002", row["template_key"])
            self.assertEqual("on_demand", row["strategy"])


if __name__ == "__main__":
    unittest.main()
