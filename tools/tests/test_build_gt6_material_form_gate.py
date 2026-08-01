import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(TOOLS))

import gt6_l3_materials  # noqa: E402
import build_gt6_material_form_gate as gate_builder  # noqa: E402


class MaterialFormGateTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.gate = json.loads(
            (
                ROOT
                / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
            ).read_text(encoding="utf-8")
        )
        cls.material_root = (
            ROOT / "src/main/resources/data/cruciblecraft/materials"
        )
        cls.l3 = json.loads(gt6_l3_materials.OUT.read_text(encoding="utf-8"))

    def test_gate_covers_catalog_and_never_exceeds_factual_forms(self):
        index = json.loads(
            (self.material_root / "index.json").read_text(encoding="utf-8")
        )
        self.assertEqual(1773, len(index))
        self.assertEqual(
            {Path(filename).stem for filename in index},
            set(self.gate["materials"]),
        )
        metadata_only = 0
        for filename in index:
            material = json.loads(
                (self.material_root / filename).read_text(encoding="utf-8")
            )
            factual = gt6_l3_materials.resolve_material_forms(material, self.l3)
            registered = set(self.gate["materials"][material["id"]])
            self.assertLessEqual(registered, factual, material["id"])
            if material.get("metadata_only"):
                metadata_only += 1
                self.assertFalse(factual, material["id"])
                self.assertFalse(registered, material["id"])
            else:
                self.assertTrue(factual, material["id"])
        self.assertEqual(663, metadata_only)

    def test_gate_records_recipe_and_compatibility_sources(self):
        counts = self.gate["counts"]
        self.assertGreater(counts["recipe_gated_forms"], 0)
        self.assertGreater(counts["compatibility_forms"], 0)
        self.assertGreater(counts["ore_chain_forms"], 0)
        self.assertEqual(
            counts["registered_forms"],
            sum(map(len, self.gate["materials"].values())),
        )
        self.assertEqual(
            counts["compatibility_forms"],
            sum(map(len, self.gate["compatibility_forms"].values())),
        )
        baseline = self.gate["compatibility_baseline"]
        self.assertFalse(baseline["recursive"])
        self.assertEqual(
            "records[].pre_gate_registered_forms", baseline["field"]
        )
        self.assertNotIn(
            "material_registration_gate.json",
            {
                value.get("path")
                for value in self.gate.get("inputs", {}).values()
                if isinstance(value, dict)
            },
        )
        self.assertNotIn(
            "GATE_OUT",
            gate_builder.build_documents.__code__.co_names,
            "gate construction must not read the previous committed gate",
        )

    def test_ore_source_gate_registers_only_factual_ores(self):
        budget = json.loads(
            (TOOLS / "material_registry_budget.json").read_text(encoding="utf-8")
        )["required_assertions"]
        ore_materials = {
            material_id
            for material_id, forms in self.gate["materials"].items()
            if "ore" in forms
        }
        self.assertEqual(budget["registered_ore_materials"], len(ore_materials))
        self.assertEqual(137, self.gate["counts"]["ore_source_materials"])
        required_source_forms = gate_builder.ore_source_required_forms()
        self.assertEqual(
            len(ore_materials) * len(required_source_forms),
            self.gate["counts"]["ore_source_forms"],
        )
        for material_id in ore_materials:
            self.assertLessEqual(
                required_source_forms,
                set(self.gate["materials"][material_id]),
                material_id,
            )
        self.assertEqual(
            budget["registered_ore_blocks"],
            self.gate["counts"]["ore_source_blocks"],
        )
        ore_chain = json.loads(
            (TOOLS / "gt6_ore_chain.json").read_text(encoding="utf-8")
        )
        crusher_materials = {
            row["material"]
            for row in ore_chain["recipes"]
            if row["family"] == "crush_raw_to_crushed"
        }
        self.assertEqual(
            len(crusher_materials),
            ore_chain["counts"]["recipes_by_map"]["crusher"],
        )
        self.assertNotEqual(
            len(crusher_materials),
            self.gate["counts"]["ore_source_materials"],
        )
        self.assertEqual(
            ore_materials,
            {
                material_id
                for material_id, forms in (
                    (
                        json.loads(
                            (self.material_root / filename).read_text(
                                encoding="utf-8"
                            )
                        )["id"],
                        gt6_l3_materials.resolve_material_forms(
                            json.loads(
                                (self.material_root / filename).read_text(
                                    encoding="utf-8"
                                )
                            ),
                            self.l3,
                        ),
                    )
                    for filename in json.loads(
                        (self.material_root / "index.json").read_text(
                            encoding="utf-8"
                        )
                    )
                )
                if "ore" in forms
            },
        )

    def test_ore_chain_operands_are_closed_by_gate(self):
        projection = json.loads(
            (TOOLS / "gt6_ore_chain_operands.json").read_text(
                encoding="utf-8"
            )
        )
        for row in projection["recipes"]:
            for operand in row["operands"]:
                self.assertIn(
                    operand["form"],
                    self.gate["materials"][operand["material"]],
                    row["recipe"],
                )


if __name__ == "__main__":
    unittest.main()
