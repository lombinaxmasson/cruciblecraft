import json
import sys
import unittest
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_gt6_ore_chain_closure as builder  # noqa: E402


class OreChainClosureBuilderTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = builder.build_document()
        cls.ore_chain = builder.load(builder.ORE_CHAIN)

    def test_both_ledgers_are_bidirectionally_and_uniquely_closed(self):
        coverage = self.ore_chain["coverage_ledger"]
        for name, allowed in (
            (
                "crusher_without_worldgen",
                builder.CRUSHER_CLASSIFICATIONS,
            ),
            (
                "sifter_dust_without_smelter",
                builder.SIFTER_CLASSIFICATIONS,
            ),
        ):
            with self.subTest(name=name):
                debts = coverage[name]
                rows = self.document[name]
                materials = [row["material"] for row in rows]
                self.assertEqual(set(debts), set(materials))
                self.assertEqual(len(materials), len(set(materials)))
                self.assertLessEqual(
                    {row["classification"] for row in rows},
                    allowed,
                )
                for row in rows:
                    self.assertTrue(row["evidence"])
                    self.assertTrue(row["rationale"].strip())
                    self.assertTrue(row["target_phase"].strip())
        self.assertEqual(0, self.document["unclassified_count"])

    def test_reported_classification_counts_are_recomputed(self):
        counts = self.document["counts"]
        for name, summary_name in (
            ("crusher_without_worldgen", "crusher_classifications"),
            ("sifter_dust_without_smelter", "sifter_classifications"),
        ):
            actual = Counter(
                row["classification"] for row in self.document[name]
            )
            self.assertEqual(len(self.document[name]), counts[name])
            self.assertEqual(
                dict(actual),
                {
                    key: value
                    for key, value in counts[summary_name].items()
                    if value
                },
            )

    def test_crusher_rules_preserve_ore_and_byproduct_priority(self):
        documents, factual = builder.gate_builder.material_documents()
        gate = builder.load(builder.REGISTRATION_GATE)["materials"]
        referrers = builder.byproduct_referrers(documents)
        for row in self.document["crusher_without_worldgen"]:
            material = row["material"]
            registered_ore = (
                "ore" in factual[material]
                and "ore" in set(gate[material])
            )
            if row["classification"] == "vein":
                self.assertTrue(registered_ore)
                self.assertIn(
                    "PROPERTIES.COMMON_ORE",
                    documents[material]["gt6_metadata"]["material_tags"],
                )
            elif row["classification"] == "byproduct_only":
                self.assertFalse(registered_ore)
                self.assertTrue(referrers[material])
            else:
                self.assertFalse(registered_ore)
                self.assertFalse(referrers.get(material))

    def test_sifter_add_smelter_requires_an_exact_registered_projection(self):
        documents, _ = builder.gate_builder.material_documents()
        gate = {
            material: set(forms)
            for material, forms in builder.load(
                builder.REGISTRATION_GATE
            )["materials"].items()
        }
        for row in self.document["sifter_dust_without_smelter"]:
            projection = builder.smelter_projection(
                row["material"],
                documents[row["material"]],
                gate,
            )
            eligible = (
                projection["target"] is not None
                and projection["target_ingot_registered"]
                and projection["units_representable"]
            )
            self.assertEqual(
                eligible,
                row["classification"] == "add_smelter",
            )

    def test_artifact_has_no_default_classification_language(self):
        serialized = json.dumps(
            {
                "crusher": self.document["crusher_without_worldgen"],
                "sifter": self.document["sifter_dust_without_smelter"],
            },
            ensure_ascii=False,
        ).lower()
        self.assertNotIn("fallback", serialized)
        self.assertNotIn("default", serialized)


if __name__ == "__main__":
    unittest.main()
