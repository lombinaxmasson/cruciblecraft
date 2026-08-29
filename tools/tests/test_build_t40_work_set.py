"""Contract tests for the T40 Electrolyzer catalog work-set freeze."""
from __future__ import annotations

import hashlib
import unittest

from tools import build_t40_work_set as builder
from tools import t40_common as common


class T40WorkSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build_work_set()

    def test_work_set_is_exactly_61_electrolyzer_ordinary_families(self) -> None:
        self.assertEqual(common.CATALOG_FAMILY_COUNT, self.document["family_count"])
        self.assertEqual(common.CATALOG_RELATION_COUNT, self.document["source_rows"])
        self.assertEqual(common.CATALOG_FAMILY_COUNT, len(self.document["family_ids"]))
        self.assertEqual(common.CATALOG_FAMILY_COUNT, len(self.document["template_keys"]))
        self.assertTrue(self.document["catalog_only"])
        self.assertEqual("T40", self.document["unique_active_card"])
        self.assertEqual(common.HOST, self.document["host"])
        self.assertEqual(common.TARGET_MAP, self.document["target_map"])
        self.assertEqual(
            self.document["template_keys"],
            sorted(self.document["template_keys"]),
        )
        self.assertEqual("T40_CATALOG_WORK_SET_FROZEN", self.document["status"])

    def test_selection_hash_is_canonical_ids_with_trailing_newlines(self) -> None:
        payload = "".join(f"{family_id}\n" for family_id in self.document["family_ids"])
        self.assertEqual(
            common.EXPECTED_SELECTION_SHA256,
            hashlib.sha256(payload.encode("utf-8")).hexdigest(),
        )
        self.assertEqual(
            common.EXPECTED_SELECTION_SHA256,
            self.document["selection_sha256"],
        )

    def test_publication_groups_split_catalog_34_25_2(self) -> None:
        groups = self.document["publication_groups"]
        singleton = groups[common.SINGLETON_GROUP]
        multi = groups[common.MULTI_GROUP]
        combinatorial = groups[common.COMBINATORIAL_GROUP]
        self.assertEqual(34, singleton["family_count"])
        self.assertEqual(34, singleton["expanded_count"])
        self.assertEqual(25, multi["family_count"])
        self.assertEqual(82, multi["expanded_count"])
        self.assertEqual(2, combinatorial["family_count"])
        self.assertEqual(35, combinatorial["expanded_count"])
        self.assertEqual(3, self.document["publication_group_count"])
        self.assertEqual(
            {"1": 34, "2": 7, "3": 12, "4": 2, "5": 1, "6": 2, "7": 1, "15": 1, "20": 1},
            self.document["expanded_count_distribution"],
        )
        self.assertEqual(
            ["gt.recipe.electrolyzer#0000", "gt.recipe.electrolyzer#0001"],
            [
                row["template_key"]
                for row in self.document["families"]
                if row["publication_group"] == common.COMBINATORIAL_GROUP
            ],
        )

    def test_write_then_check_is_zero_drift(self) -> None:
        self.assertEqual(0, builder.main(["--write"]))
        self.assertEqual(0, builder.main(["--check"]))

    def test_missing_template_fails_closed(self) -> None:
        families_doc = common.load_json(common.RECIPE_FAMILIES)
        families_doc["families"] = [
            row
            for row in families_doc["families"]
            if row.get("template_key") != "gt.recipe.electrolyzer#0002"
        ]
        with self.assertRaises(ValueError):
            builder.build_work_set(families_doc)
