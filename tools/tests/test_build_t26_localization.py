from __future__ import annotations

import copy
import json
import unittest
from unittest import mock

from tools import build_t26_localization as builder


class T26LocalizationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.table = json.loads(
            builder.TABLE.read_text(encoding="utf-8")
        )

    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        self.assertEqual([], builder.check())

    def test_every_domain_is_accounted_separately(self) -> None:
        document = builder.build()
        domains = document["domains"]
        self.assertEqual(
            set(builder.DOMAIN_PREFIXES), set(domains)
        )
        for domain, counts in domains.items():
            self.assertEqual(
                counts["gap"], counts["en_keys"] - counts["zh_keys"],
                domain)
            self.assertEqual(
                counts["zero_gap"], counts["gap"] == 0, domain)

    def test_table_is_a_catalog_subset_without_english_copies(self) -> None:
        document = builder.build()
        self.assertGreater(document["totals"]["table_materials"], 0)
        self.assertGreater(
            document["totals"]["post_1_0_materials"], 0,
            "the material long tail must be explicitly post_1_0")

    def test_o15_reclassification_is_present(self) -> None:
        document = builder.build()
        o15 = document["o15_reclassification"]
        self.assertEqual("O-15", o15["item"])
        self.assertEqual("v1_required", o15["classification"])
        self.assertEqual("T26", o15["owner"])
        self.assertEqual("post_1_0", o15["material_domain"][
            "long_tail_disposition"])
        self.assertTrue(o15["material_domain"][
            "no_english_copy_fake_translations"])

    def test_status_is_derived_fail_closed(self) -> None:
        document = builder.build()
        zero_gap = all(
            document["domains"][domain]["zero_gap"]
            for domain in builder.ZERO_GAP_DOMAINS
        )
        if zero_gap:
            self.assertEqual("T26_LOCALIZATION_ACCOUNTED",
                             document["status"])
        else:
            self.assertIsNone(document["status"])

    def test_mutation_stale_table_id_is_rejected(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.TABLE:
                table = copy.deepcopy(real(path, {}) or {})
                table["materials"]["no_such_material_xyz"] = "不存在"
                return table
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_english_copy_is_rejected(self) -> None:
        real = builder._load_if_exists

        def override(path, default=None):
            if path == builder.TABLE:
                table = copy.deepcopy(real(path, {}) or {})
                first = next(iter(table["materials"]))
                table["materials"][first] = "English Copy"
                return table
            return real(path, default)

        with mock.patch.object(
                builder, "_load_if_exists", side_effect=override):
            with self.assertRaises(ValueError):
                builder.build()

    def test_mutation_missing_inputs_is_rejected(self) -> None:
        with mock.patch.object(
                builder, "_load_if_exists", return_value=None):
            with self.assertRaises(ValueError):
                builder.build()


if __name__ == "__main__":
    unittest.main()
