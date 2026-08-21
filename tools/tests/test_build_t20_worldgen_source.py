import json
import subprocess
import sys
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t20_worldgen_source as builder  # noqa: E402


class T20WorldgenSourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.policy = builder.load(builder.POLICY)
        cls.document = builder.load(builder.OUTPUT)

    def test_fixed_revision_tree_blobs_and_anchors_are_pinned(self):
        builder.validate_policy(self.policy)
        builder.validate_compact(self.policy, self.document)
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            self.document["source"]["revision"],
        )
        self.assertEqual(
            "a164302f62a326208fd2076de6fb7cdb0b4602ee",
            self.document["source"]["tree_sha1"],
        )
        for record in self.document["source"]["files"].values():
            self.assertRegex(record["git_blob_sha1"], r"^[0-9a-f]{40}$")
            self.assertRegex(record["sha256"], r"^[0-9a-f]{64}$")
            self.assertGreater(record["line_count"], 0)
            self.assertTrue(record["anchors"])

    def test_source_cardinalities_are_observed_not_inferred_from_catalog(self):
        self.assertEqual(
            {
                "dynamic_random_small_rules": 1,
                "explicit_small_source_facts": 75,
                "large_source_facts": 40,
                "source_material_tokens_without_cc_identity": 0,
            },
            self.document["counts"],
        )
        self.assertEqual([], self.document["unresolved_cc_material_tokens"])

    def test_large_copper_fact_preserves_exact_fields_and_material_roles(self):
        row = next(
            row
            for row in self.document["large_veins"]
            if row["source_fact_id"] == "ore.large.copper"
        )
        self.assertEqual(916, row["source_line"])
        self.assertEqual((10, 30, 80, 4, 24), (
            row["min_y"],
            row["max_y"],
            row["weight"],
            row["density"],
            row["size"],
        ))
        self.assertEqual("chalcopyrite", row["layers"]["top"]["cc_material"])
        self.assertEqual("hematite", row["layers"]["bottom"]["cc_material"])
        self.assertEqual("pyrite", row["layers"]["between"]["cc_material"])
        self.assertEqual("copper", row["layers"]["spread"]["cc_material"])

    def test_full_revision_source_replay_matches_projection(self):
        local_root = ROOT / "gt6_code" / "gregtech6"
        revision = self.policy["source"]["revision"]
        if local_root.is_dir():
            sources = {}
            for key, record in builder.source_records(self.policy).items():
                sources[key] = subprocess.check_output(
                    [
                        "git",
                        "-C",
                        str(local_root),
                        "show",
                        f"{revision}:{record['path']}",
                    ]
                )
        else:
            sources = builder.download_sources(self.policy)
        replay = builder.build_from_sources(self.policy, sources)
        self.assertEqual(
            json.dumps(self.document, sort_keys=True),
            json.dumps(replay, sort_keys=True),
        )


if __name__ == "__main__":
    unittest.main()
