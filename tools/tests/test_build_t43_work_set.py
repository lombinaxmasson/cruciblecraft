"""Contract tests for the T43 Smelter stone work-set freeze."""
from __future__ import annotations

import hashlib
import unittest

from tools import build_t43_work_set as builder
from tools import t43_common as common


class T43WorkSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build_work_set()

    def test_work_set_is_exactly_407_smelter_stone_families(self) -> None:
        self.assertEqual(common.CATALOG_FAMILY_COUNT, self.document["family_count"])
        self.assertEqual(common.CATALOG_RELATION_COUNT, self.document["source_rows"])
        self.assertEqual(common.CATALOG_FAMILY_COUNT, len(self.document["family_ids"]))
        self.assertEqual("T43", self.document["unique_active_card"])
        self.assertEqual(common.HOST, self.document["host"])
        self.assertEqual("T43_WORK_SET_FROZEN", self.document["status"])
        self.assertEqual({"1": 407}, self.document["expanded_count_distribution"])
        self.assertEqual(406, self.document["cohorts"]["A"]["family_count"])
        self.assertEqual(1, self.document["cohorts"]["B"]["family_count"])
        self.assertEqual(
            "portfolio:track_a/cruciblecraft:smelter/gt.recipe.smelter#2932",
            self.document["cohorts"]["B"]["family_ids"][0],
        )

    def test_selection_hash_is_canonical_ids_with_trailing_newlines(self) -> None:
        payload = "".join(f"{family_id}\n" for family_id in self.document["family_ids"])
        self.assertEqual(
            common.EXPECTED_SELECTION_SHA256,
            hashlib.sha256(payload.encode("utf-8")).hexdigest(),
        )
        self.assertEqual(common.EXPECTED_SELECTION_SHA256, self.document["selection_sha256"])

    def test_hazmat_storage_and_0111_stay_out(self) -> None:
        templates = set(self.document["template_keys"])
        self.assertNotIn("gt.recipe.smelter#0024", templates)
        self.assertNotIn("gt.recipe.smelter#0029", templates)
        self.assertNotIn("gt.recipe.smelter#0111", templates)
        self.assertNotIn("gt.recipe.smelter#0112", templates)
        self.assertIn("gt.recipe.smelter#2438", templates)
        self.assertIn("gt.recipe.smelter#2843", templates)

    def test_write_then_check_is_zero_drift(self) -> None:
        self.assertEqual(0, builder.main(["--write"]))
        self.assertEqual(0, builder.main(["--check"]))
