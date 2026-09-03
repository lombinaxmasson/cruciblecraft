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
        ]
        for line in lines:
            self.assertEqual([], scanner.line_findings(line), line)

    def test_milestone_tokens_are_detected(self) -> None:
        cases = {
            "card_id = T49": "T49",
            "tools/t49_census_delta.json": "t49",
            "recipe/t49/bath/example.json": "/t49/",
            "cruciblecraft_t49": "cruciblecraft_t49",
            "-Pt49Recipes": "-Pt49",
            "portfolio:track_a/t49_bath": "portfolio:track_a/t49",
            'next_issue_id="T50"': "next_issue_id=\"T50",
            "T2ChainRules.ALL": "T2C",
            "isT5ChemicalRecipe": "T5C",
            "t39-shard-v1": "t39-",
            "ENVELOPE_T5_BRONZE = \"t5_bronze\"": "t5_",
            "status = T49_READY": "T49",
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
        self.assertTrue(scanner.is_quick_target("src/main/java/com/masson/cruciblecraft/logistics/fluidnet/FluidLogisticsNetwork.java"))
        self.assertTrue(scanner.is_quick_target("tools/capabilities/logistics/cover-net-r0/capability.json"))
        self.assertFalse(scanner.is_quick_target("src/main/java/com/masson/cruciblecraft/CrucibleCraft.java"))
        self.assertFalse(scanner.is_quick_target("src/recipe_generated/resources/data/cruciblecraft/recipe/t49.json"))
        self.assertFalse(scanner.is_quick_target("tools/build_t35_runtime_registry.py"))
        self.assertFalse(scanner.is_quick_target("src/t14Benchmark/java/example.java"))
        self.assertIn("src/t14Benchmark", {root.replace("\\", "/") for root in scanner.SCAN_ROOTS})
        self.assertIn("docs/decisions", scanner.SCAN_ROOTS)
        self.assertIn("README.md", scanner.SCAN_FILES)

    def test_archive_and_history_are_exempt(self) -> None:
        self.assertTrue(scanner.is_exempt("archive/sealed/T49/closeout_seal.json"))
        self.assertTrue(scanner.is_exempt("docs/history/INDEX.md"))
        self.assertTrue(scanner.is_exempt("docs/history/work-logs/T48-工作日志.md"))
        self.assertFalse(
            scanner.is_exempt(
                "docs/history/card-plans/active/Ordinary尾账收口与封板修复详细计划.md"
            )
        )
        self.assertFalse(scanner.is_exempt("tools/closeout_seal.py"))

    def test_allowlist_must_carry_owner_reason_expiry(self) -> None:
        self.assertEqual([], scanner.ALLOWLIST)
        self.assertEqual([], scanner.allowlist_errors())

    def test_active_plan_is_scanned(self) -> None:
        roots = {root.replace("\\", "/") for root in scanner.SCAN_ROOTS}
        self.assertIn("docs/history/card-plans/active", roots)
        self.assertIn("tools", roots)
        self.assertIn("src/main", roots)


if __name__ == "__main__":
    unittest.main()
