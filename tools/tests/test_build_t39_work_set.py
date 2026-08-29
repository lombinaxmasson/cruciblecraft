"""Contract tests for the T39 Centrifuge work-set freeze."""
from __future__ import annotations

import hashlib
import unittest

from tools import build_t39_work_set as builder
from tools import t39_common as common


class T39WorkSetTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build_work_set()

    def test_work_set_is_exactly_157_centrifuge_ordinary_families(self) -> None:
        self.assertEqual(common.FAMILY_COUNT, self.document["family_count"])
        self.assertEqual(common.SOURCE_ROWS, self.document["source_rows"])
        self.assertEqual(common.FAMILY_COUNT, len(self.document["family_ids"]))
        self.assertEqual(common.FAMILY_COUNT, len(self.document["template_keys"]))
        self.assertEqual("T39", self.document["unique_active_card"])
        self.assertEqual(common.HOST, self.document["host"])
        self.assertEqual(common.TARGET_MAP, self.document["target_map"])
        self.assertEqual(
            self.document["template_keys"],
            sorted(self.document["template_keys"]),
        )

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

    def test_publication_groups_split_123_and_34(self) -> None:
        groups = self.document["publication_groups"]
        singleton = groups[common.SINGLETON_GROUP]
        multi = groups[common.MULTI_GROUP]
        self.assertEqual(123, singleton["family_count"])
        self.assertEqual(123, singleton["expanded_count"])
        self.assertEqual(34, multi["family_count"])
        self.assertEqual(127, multi["expanded_count"])
        self.assertEqual(2, self.document["publication_group_count"])
        self.assertEqual(
            {"1": 123, "2": 16, "3": 8, "4": 1, "5": 3, "6": 2, "7": 1, "8": 1, "11": 1, "14": 1},
            self.document["expanded_count_distribution"],
        )

    def test_write_then_check_is_zero_drift(self) -> None:
        self.assertEqual(0, builder.main(["--write"]))
        self.assertEqual(0, builder.main(["--check"]))

    def test_missing_template_fails_closed(self) -> None:
        families_doc = common.load_json(common.RECIPE_FAMILIES)
        families_doc["families"] = [
            row
            for row in families_doc["families"]
            if row.get("template_key") != "gt.recipe.centrifuge#0002"
        ]
        with self.assertRaises(ValueError):
            builder.build_work_set(families_doc)
