"""Tests for the T22.5 A2 GT6-fluid-name -> CC-fluid-id mapping builder."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_fluid_mapping as builder  # noqa: E402


class T225FluidMappingTest(unittest.TestCase):

    def test_fixtures_derive_verbatim(self) -> None:
        derivation = builder.derive()
        by_name = {r["fluid"]: r for r in derivation["records"]}
        fixtures = {
            "molten.brass": "cruciblecraft:molten_brass",
            "molten.asphalt": "cruciblecraft:molten_asphalt",
            "ic2constructionfoam": "cruciblecraft:construction_foam",
            "ic2distilledwater": "cruciblecraft:water_distilled",
        }
        for name, expected_id in fixtures.items():
            self.assertIn(name, by_name)
            self.assertEqual(by_name[name]["disposition"], "mapped")
            self.assertEqual(by_name[name]["cc_fluid_id"], expected_id)

    def test_derivation_is_recomputable_and_complete(self) -> None:
        first = builder.derive()
        second = builder.derive()
        self.assertEqual(first, second)
        counts = first["counts"]
        self.assertEqual(counts["total"], 322)
        self.assertEqual(
            counts["mapped"]
            + counts["no_cc_fluid"]
            + counts["out_of_scope"],
            322,
        )
        self.assertEqual(counts["unclassified"], 0)
        self.assertEqual(counts["out_of_scope"], 2)
        # Every record has exactly one vocabulary disposition.
        for record in first["records"]:
            self.assertIn(
                record["disposition"], builder.DISPOSITION_VOCABULARY
            )

    def test_mutation_flips_material_status(self) -> None:
        baseline = builder.derive()
        brass = [
            r for r in baseline["records"] if r["fluid"] == "molten.brass"
        ]
        self.assertEqual(len(brass), 1)
        self.assertEqual(brass[0]["disposition"], "mapped")
        policy = builder._load(builder.ACTIVATION_POLICY)["records"]
        brass_record = next(
            r for r in policy if int(r["source_id"]) == 8620
        )
        mutated = builder.derive(
            policy_override={
                8620: {
                    "source_id": -1,
                    "cc_id": None,
                    "status": "OUT_OF_SCOPE",
                    "source_name": brass_record["source_name"],
                }
            }
        )
        by_name = {r["fluid"]: r for r in mutated["records"]}
        self.assertEqual(by_name["molten.brass"]["disposition"],
                         "out_of_scope")
        self.assertEqual(
            mutated["counts"]["out_of_scope"],
            baseline["counts"]["out_of_scope"] + 1,
        )
        self.assertEqual(
            mutated["counts"]["mapped"], baseline["counts"]["mapped"] - 1
        )

    def test_publication_delta_is_zero_and_output_stays_in_tools(self) -> None:
        document = builder.build()
        self.assertEqual(document["publication_delta"], 0)
        self.assertTrue(
            builder.OUTPUT.as_posix().startswith(
                (builder.TOOLS / "").as_posix()
            )
        )

    def test_unknown_decisions_have_full_definition(self) -> None:
        for name, decision in builder.UNKNOWN_IDENTITY_DECISIONS.items():
            self.assertIn("class", decision)
            self.assertIn("rationale", decision)
            self.assertIn("recheck_point", decision)
        self.assertIn("spectral_dew", builder.UNKNOWN_IDENTITY_DECISIONS)
        self.assertIn(
            "potion.mineralwater", builder.UNKNOWN_IDENTITY_DECISIONS
        )
        self.assertIn(
            "liquid_medium_oil", builder.UNKNOWN_IDENTITY_DECISIONS
        )

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        errors = builder.check()
        self.assertEqual(errors, [])

    def test_committed_artifact_fixtures_block(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        self.assertEqual(document["status"], "T22_5_FLUID_MAPPING_READY")
        self.assertEqual(document["counts"]["unclassified"], 0)
        self.assertEqual(document["publication_delta"], 0)


if __name__ == "__main__":
    unittest.main()
