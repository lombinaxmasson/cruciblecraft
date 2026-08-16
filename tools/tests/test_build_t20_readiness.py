import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t20_readiness as builder  # noqa: E402


class T20ReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = builder.build()

    def test_artifact_matches_current_builder(self):
        self.assertEqual([], builder.check(self.document))
        self.assertEqual("T20_READY", self.document["status"])
        self.assertEqual(
            ["T20a", "T20b", "T20c", "T20d", "T20e"],
            self.document["completed_stages"],
        )
        self.assertEqual([], self.document["pending_stages"])

    def test_closure_and_fidelity_keep_separate_denominators(self):
        closure = self.document["closure"]
        fidelity = self.document["fidelity"]
        self.assertEqual(40, closure["source_large_facts"])
        self.assertEqual(75, closure["source_explicit_small_facts"])
        self.assertEqual(1, closure["source_dynamic_small_rules"])
        self.assertEqual(129, closure["catalog_identities"])
        self.assertEqual(134, closure["runtime_large_veins"])
        self.assertEqual(0, closure["unclassified"])
        self.assertEqual(
            {"DESIGN_POLICY": 56, "SOURCE_DERIVED": 73},
            fidelity["statuses"],
        )
        self.assertEqual(0, fidelity["placeholder"])
        self.assertEqual(0, fidelity["unverified"])
        self.assertTrue(fidelity["expected_equals_authored"])
        self.assertTrue(fidelity["authored_equals_generated"])

    def test_runtime_profile_and_save_boundary_fail_closed(self):
        runtime = self.document["runtime"]
        save = self.document["save_boundary"]
        self.assertEqual(2, runtime["profile_version"])
        self.assertEqual(129, runtime["configured_features"])
        self.assertEqual(129, runtime["placed_features"])
        self.assertEqual(129, runtime["profile_ids"])
        self.assertEqual([1, 2], save["supported_profile_versions"])
        self.assertEqual("CODEC_REJECTED", save["unknown_profile_version"])
        self.assertFalse(save["existing_generated_chunks_rewritten"])
        self.assertEqual(0, save["persisted_profile_bytes_per_ore_block"])

    def test_load_and_publication_are_independently_accounted(self):
        load = self.document["load"]
        self.assertEqual(
            263,
            load["worldgen_resources"]["catalog_generated_files"],
        )
        self.assertGreater(
            load["worldgen_resources"]["catalog_generated_bytes"], 0
        )
        self.assertEqual(
            129, load["codec_startup"]["profile_decodes"]
        )
        self.assertEqual(
            516, load["codec_startup"]["weighted_state_decodes"]
        )
        self.assertLess(
            load["density"]["combined_expected_ore_veins_per_chunk"],
            load["density"]["hard_ceiling"],
        )
        self.assertEqual(0, load["publication"]["recipe_map_delta"])
        self.assertEqual(0, load["publication"]["logical_row_delta"])
        self.assertEqual(0, load["publication"]["eager_row_delta"])
        self.assertEqual(0, load["publication"]["lazy_row_delta"])


if __name__ == "__main__":
    unittest.main()
