"""Tests for the T22.5 B1 recipe-row classification builder."""
from __future__ import annotations

import hashlib
import json
import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t22_5_row_classification as builder  # noqa: E402


def _sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T225RowClassificationTest(unittest.TestCase):

    def test_universe_expansion_matches_ledger_2(self) -> None:
        ledger = builder._load(builder.LEDGER_2)
        classes = ledger["encoding"]["classes"]
        units = [
            u for u in ledger["units"]
            if classes[u[3]] == "ordinary_optional"
        ]
        self.assertEqual(len(units), 89643)
        expanded = sum(len(u[4]) for u in units)
        self.assertEqual(expanded, 146841)
        self.assertEqual(
            expanded,
            ledger["counts"]["expanded_row_diagnostics"][
                "ordinary_optional"
            ],
        )

    def test_committed_artifact_is_current(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        self.assertEqual(builder.check(), [])

    def test_all_146841_rows_classified(self) -> None:
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        counts = document["counts"]
        self.assertEqual(counts["unclassified"], 0)
        self.assertEqual(counts["total"], 146841)
        self.assertEqual(sum(counts["by_class"].values()), 146841)
        self.assertEqual(
            sum(
                sum(v.values()) for v in document["per_map"].values()
            ),
            146841,
        )
        mixer_rows = sum(
            r["rows"] for r in document["mixer_templates"]
        )
        self.assertEqual(
            mixer_rows + len(document["non_mixer_rows"]), 146841
        )

    def test_mutation_flips_fluid_decision(self) -> None:
        ctx = builder.load_context()
        # Construct a minimal recipe whose only signal is a no_cc_fluid
        # operand: for.honey (A2 no_cc_fluid).
        recipe = {
            "fluidInputs": [{"fluid": "for.honey", "amount": 1000}],
            "fluidOutputs": [],
            "inputs": [],
            "outputs": [],
        }
        self.assertEqual(
            builder.classify_recipe("gt.recipe.bath", recipe, ctx),
            "cross_mod_compat",
        )
        mutated = dict(ctx)
        a2_by_name = dict(ctx["a2_by_name"])
        row = dict(a2_by_name["for.honey"])
        row["disposition"] = "mapped"
        a2_by_name["for.honey"] = row
        mutated["a2_by_name"] = a2_by_name
        self.assertEqual(
            builder.classify_recipe("gt.recipe.bath", recipe, mutated),
            "ordinary_optional",
        )

    def test_nuclear_criterion_matches_ledger_1(self) -> None:
        ctx = builder.load_context()
        recipe = {
            "fluidInputs": [],
            "fluidOutputs": [],
            "inputs": [{"item": "gregtech:gt.meta.dust", "meta": 920}],
            "outputs": [{"item": "gregtech:gt.meta.dust", "meta": 900}],
        }
        # Uranium (920) and Thorium (900) are both actinides.
        self.assertEqual(
            builder.classify_recipe(
                "gt.recipe.electrolyzer", recipe, ctx
            ),
            "post_1_0_nuclear",
        )

    def test_ledger_2_is_read_only(self) -> None:
        before = _sha(builder.LEDGER_2)
        builder.load_context()
        self.assertEqual(_sha(builder.LEDGER_2), before)
        ledger = builder._load(builder.LEDGER_2)
        self.assertEqual(ledger["counts"]["denominator_units"], 93133)

    def test_no_status_name_erasure_in_t225_builders(self) -> None:
        anti = "0 if "
        for path in sorted(TOOLS.glob("build_t22_5_*.py")):
            source = path.read_text(encoding="utf-8")
            self.assertNotIn(
                "MATERIAL_RULE_COVERED\"",
                source,
                f"{path.name} erases counts via a status name",
            )
            # The T22 anti-pattern shape must not appear at all.
            self.assertNotIn(
                'f.get("status") == "MATERIAL_RULE_COVERED"',
                source,
                f"{path.name} contains the T22 count-erasure pattern",
            )

    def test_downstream_consumers_are_not_written(self) -> None:
        protected = (
            builder.LEDGER_2,
            builder.LEDGER_1,
            builder.A2_ARTIFACT,
            builder.A3_ARTIFACT,
        )
        before = {p: _sha(p) for p in protected}
        document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )
        for path, digest in before.items():
            self.assertEqual(
                _sha(path), digest, f"{path.name} was modified"
            )
        sync = document["downstream_sync"]
        for consumer in (
            "t21_readiness.json",
            "t22_petroleum_denominator.json",
            "t22_family_manifest.json",
            "t22_readiness.json",
            "C0",
            "C1",
            "full_verification_report.json",
        ):
            self.assertIn(consumer, sync)


if __name__ == "__main__":
    unittest.main()
