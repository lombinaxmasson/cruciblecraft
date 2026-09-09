"""False-positive / false-negative contract for the zero-TXX scanner."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import check_zero_milestone_names as scanner


class ZeroMilestoneNameScannerTest(unittest.TestCase):
    def test_false_positives_are_ignored(self) -> None:
        lines = [
            "cobalt60 is a real isotope id",
            "class TankBlock extends Block",
            "ToolMaterial material = ToolMaterial.IRON",
            "TierProfile profile = TierProfile.DEFAULT",
            "gt6_recipe_dump/maps/gt.recipe.mixer.json",
            "gt6_oredict_materials_normalized.json",
            "import com.masson.cruciblecraft.content.block.Gt6StyleConnections;",
            "GT6MaterialMetadata.CODEC.parse(ops, metadataValue)",
            "Gt6BasicMachineGui.ui(1, 1, 1, 1)",
            '"chinese_name": "Circuit T1 (Basic)"',
            "T1 dynamo keeps registry id bronze_dynamo",
        ]
        for line in lines:
            self.assertEqual([], scanner.line_findings(line), line)

    def test_milestone_tokens_are_detected(self) -> None:
        milestone = "T" + "49"
        lower_milestone = milestone.lower()
        cases = {
            "card_id = " + milestone: milestone,
            "tools/" + lower_milestone + "_census_delta.json": lower_milestone,
            "recipe/" + lower_milestone + "/bath/example.json": "/" + lower_milestone + "/",
            "cruciblecraft_" + lower_milestone: "cruciblecraft_" + lower_milestone,
            "-P" + lower_milestone + "Recipes": "-P" + lower_milestone,
            "portfolio:track_a/" + lower_milestone + "_bath": "portfolio:track_a/" + lower_milestone,
            "next_issue_id=" + ("T" + "50"): "next_issue_id=" + ("T" + "50"),
            "T2ChainRules.ALL": "T2C",
            "isT5ChemicalRecipe": "T5C",
            ("t" + "39" + "-shard-v1"): "t" + "39" + "-",
            "ENVELOPE_T5_BRONZE = \"t5_bronze\"": "t5_",
            "status = " + milestone + "_READY": milestone,
        }
        for line, expected in cases.items():
            found = scanner.line_findings(line)
            self.assertTrue(
                any(expected in token or token in expected for token in found),
                f"{line!r} -> {found!r}, expected {expected!r}",
            )

    def test_letter_suffix_tokens_are_detected(self) -> None:
        self.assertTrue(
            any("t18b" in token or token.endswith("b") for token in scanner.line_findings("void t18bDynamo()"))
        )
        self.assertTrue(
            any("T13c" in token or "13c" in token.lower() for token in scanner.line_findings("build_t13c_exclusion"))
        )

    def test_quick_mode_scans_live_java_not_generated_trees(self) -> None:
        lower_milestone = ("T" + "49").lower()
        self.assertTrue(scanner.is_quick_target("src/main/java/com/masson/cruciblecraft/logistics/fluidnet/FluidLogisticsNetwork.java"))
        self.assertFalse(scanner.is_quick_target("tools/capabilities/logistics/cover-net-r0/capability.json"))
        self.assertTrue(
            scanner.is_quick_target("docs/history/card-plans/active/显示CPU详细计划.md")
        )
        self.assertTrue(
            scanner.is_quick_target("docs/history/card-plans/prep/辊压成型机详细计划.md")
        )
        self.assertFalse(scanner.is_quick_target("src/main/java/com/masson/cruciblecraft/CrucibleCraft.java"))
        self.assertFalse(scanner.is_quick_target("src/recipe_generated/resources/data/cruciblecraft/recipe/" + lower_milestone + ".json"))
        self.assertFalse(scanner.is_quick_target("tools/build_t" + "35_runtime_registry.py"))
        self.assertFalse(scanner.is_quick_target("src/recipeLoadBenchmark/java/example.java"))
        self.assertIn("src/recipeLoadBenchmark", {root.replace("\\", "/") for root in scanner.SCAN_ROOTS})
        self.assertIn("docs/history", scanner.SCAN_ROOTS)
        self.assertIn("docs/decisions", scanner.SCAN_ROOTS)
        self.assertIn("README.md", scanner.SCAN_FILES)

    def test_only_closed_card_plans_are_exempt(self) -> None:
        milestone = "T" + "49"
        self.assertTrue(
            scanner.is_exempt(
                "docs/history/card-plans/closed/" + milestone + "-ordinary-closeout.md"
            )
        )
        self.assertFalse(
            scanner.is_exempt(
                "docs/history/card-plans/active/显示CPU详细计划.md"
            )
        )
        self.assertFalse(
            scanner.is_exempt(
                "docs/history/card-plans/prep/辊压成型机详细计划.md"
            )
        )
        self.assertFalse(scanner.is_exempt("archive/sealed/" + milestone + "/archive_manifest.json"))
        self.assertFalse(scanner.is_exempt("docs/history/INDEX.md"))
        self.assertFalse(scanner.is_exempt("docs/history/work-logs/" + "T" + "48" + "-work-log.md"))
        self.assertFalse(scanner.is_exempt("tools/full_verification_report.json"))

    def test_allowlist_must_carry_owner_reason_expiry(self) -> None:
        self.assertEqual([], scanner.ALLOWLIST)
        self.assertEqual([], scanner.allowlist_errors())

    def test_history_root_is_scanned_outside_card_plans(self) -> None:
        roots = {root.replace("\\", "/") for root in scanner.SCAN_ROOTS}
        self.assertIn("docs/history", roots)
        self.assertIn("tools", roots)
        self.assertIn("src/main", roots)


if __name__ == "__main__":
    unittest.main()
