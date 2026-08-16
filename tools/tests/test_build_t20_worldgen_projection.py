import copy
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t20_worldgen_projection as builder  # noqa: E402


class T20WorldgenProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.expected = builder.build_expected()
        cls.authored = builder.load(builder.AUTHORING)

    def test_129_catalog_rows_have_complete_explicit_dispositions(self):
        counts = self.expected["counts"]
        self.assertEqual(129, counts["catalog_entries"])
        self.assertEqual({
            "DESIGN_POLICY_NO_GT6_WORLDGEN_FACT": 56,
            "EXPLICIT_SMALL_SOURCE": 39,
            "RANDOM_SMALL_GEM_SOURCE": 28,
            "UNIQUE_LARGE_ROLE_SOURCE": 6,
        }, counts["classifications"])
        self.assertEqual(
            {"DESIGN_POLICY": 56, "SOURCE_DERIVED": 73},
            counts["statuses"],
        )
        self.assertEqual(0, counts["unclassified"])
        self.assertEqual(0, counts["placeholder"])
        self.assertEqual(0, counts["unverified"])

    def test_expected_and_authored_sets_are_byte_exact_per_row(self):
        self.assertEqual(self.expected["rows"], self.authored["veins"])
        self.assertEqual([], builder.compare_authored(self.expected))
        self.assertEqual(
            {row["id"] for row in self.expected["rows"]},
            {
                f"large_{material}_vein"
                for material in builder.catalog_materials()
            },
        )

    def test_every_row_has_profile_v2_unique_stable_identity(self):
        rows = self.expected["rows"]
        self.assertEqual(129, len({row["id"] for row in rows}))
        self.assertEqual(129, len({row["salt"] for row in rows}))
        for row in rows:
            self.assertEqual(2, row["profile_version"])
            self.assertEqual(
                builder.stable_salt(row["id"]),
                row["salt"],
            )
            self.assertNotIn(
                "PLACEHOLDER",
                row["provenance"]["field_status"].values(),
            )
            self.assertNotIn(
                "UNVERIFIED",
                row["provenance"]["field_status"].values(),
            )

    def test_design_policy_rows_do_not_claim_gt6_parity(self):
        design = [
            row for row in self.expected["rows"]
            if row["provenance"]["status"] == "DESIGN_POLICY"
        ]
        self.assertEqual(56, len(design))
        for row in design:
            provenance = row["provenance"]
            self.assertIsNone(provenance["source_fact_id"])
            self.assertTrue(provenance["reason"])
            self.assertIn(
                "design_policy_hash_v1",
                provenance["transformations"],
            )

    def test_wrong_authored_field_makes_bidirectional_gate_fail(self):
        changed = copy.deepcopy(self.authored)
        changed["veins"][0]["max_y"] += 1
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "ore_veins.json"
            path.write_text(json.dumps(changed), encoding="utf-8")
            with mock.patch.object(builder, "AUTHORING", path):
                errors = builder.compare_authored(self.expected)
        self.assertEqual(
            [f"changed:{changed['veins'][0]['id']}"],
            errors,
        )

    def test_load_projection_keeps_recipe_publication_at_zero(self):
        load = self.expected["load_projection"]
        self.assertEqual(259, load["generated_files"])
        self.assertEqual(0, load["recipe_map_delta"])
        self.assertEqual(0, load["logical_row_delta"])
        self.assertEqual(0, load["eager_row_delta"])
        self.assertEqual(0, load["lazy_row_delta"])


if __name__ == "__main__":
    unittest.main()
