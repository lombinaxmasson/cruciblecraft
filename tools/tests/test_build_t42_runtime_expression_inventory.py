"""T42 inventory, identity helper, and B0 baseline contracts."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t42_reachability_baseline as baseline_builder
from tools import build_t42_runtime_expression_inventory as inventory_builder
from tools import t42_common as common


class T42InventoryAndBaselineTest(unittest.TestCase):
    def test_minecraft_prefix_is_not_proof(self) -> None:
        document = inventory_builder.build()
        allowlist = document.pop("_allowlist")
        self.assertTrue(document["minecraft_prefix_is_not_proof"])
        self.assertTrue(allowlist["minecraft_prefix_is_not_proof"])
        self.assertEqual("minecraft-1.21.1", allowlist["version"])
        self.assertTrue(allowlist["not_a_121_registry_scrape"])
        self.assertEqual("t37_t41_aliases_plus_explicit", allowlist["provenance"])
        self.assertNotIn("minecraft:anvil", allowlist["item_ids"])
        self.assertNotIn("minecraft:iron_ingot", allowlist["item_ids"])
        self.assertNotIn("minecraft:bucket", allowlist["item_ids"])
        self.assertEqual(["minecraft:oak_planks"], allowlist["explicit_item_ids"])
        self.assertIn("minecraft:oak_planks", allowlist["item_ids"])
        self.assertNotIn("minecraft:oak_planks", allowlist["alias_item_ids"])
        circuit = document["programmed_circuit"]
        self.assertEqual("gregapi:gt.integrated_circuit", circuit["mapped_from"])
        self.assertTrue(circuit["mapping_is_not_expression_proof"])
        self.assertFalse(circuit["present_in_t35_runtime_registry"])
        fluids = document["fluids"]
        self.assertGreater(fluids["generation_tag_molten_material_count"], 0)
        self.assertIn("top_level_molten_flag_count", fluids)
        self.assertTrue(all(item_id.startswith("minecraft:") for item_id in allowlist["item_ids"]))
        self.assertEqual(
            ["minecraft:lava", "minecraft:water"],
            sorted(allowlist["fluid_ids"]),
        )
        self.assertGreater(fluids["remaining_molten_fluids_in_mapping"], 0)
        self.assertTrue(fluids["molten_not_cc_registration_proof"])

    def test_logical_identity_is_not_ingredient_tostring(self) -> None:
        first = common.logical_relation_identity(
            {
                "item_inputs": [{"runtime_id": "minecraft:stone", "components": ""}],
                "item_input_counts": [1],
                "item_input_actions": [{"kind": "consume"}],
                "item_outputs": [{"runtime_id": "minecraft:iron_ingot", "count": 1}],
                "fluid_inputs": [],
                "fluid_outputs": [],
                "duration": 20,
                "eut": 16,
                "chances": [10000],
                "special_value": 0,
                "can_be_buffered": True,
            }
        )
        preserve = common.logical_relation_identity(
            {
                "item_inputs": [{"runtime_id": "minecraft:stone", "components": ""}],
                "item_input_counts": [1],
                "item_input_actions": [{"kind": "preserve"}],
                "item_outputs": [{"runtime_id": "minecraft:iron_ingot", "count": 1}],
                "fluid_inputs": [],
                "fluid_outputs": [],
                "duration": 20,
                "eut": 16,
                "chances": [10000],
                "special_value": 0,
                "can_be_buffered": True,
            }
        )
        self.assertIn("minecraft:stone", first)
        self.assertNotEqual(first, preserve)
        self.assertIn("PRESERVE", preserve)
        self.assertNotIn("Ingredient", first)

    def test_b0_excludes_t42_outputs_and_is_cumulative(self) -> None:
        if not common.REACHABILITY_BASELINE.is_file():
            document = baseline_builder.build()
        else:
            document = common.load_json(common.REACHABILITY_BASELINE)
        self.assertTrue(document["excludes_t42_outputs"])
        self.assertTrue(document["fluid_reachability_approximate"])
        layers = [row["layer"] for row in document["layers"]]
        self.assertEqual(["t21_base", "t39", "t40", "t41"], layers)
        self.assertGreater(document["b0_identity_count"], document["t21_identity_count"])


if __name__ == "__main__":
    unittest.main()
