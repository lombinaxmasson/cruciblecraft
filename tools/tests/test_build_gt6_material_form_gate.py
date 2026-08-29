import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(TOOLS))

import gt6_l3_materials  # noqa: E402
import build_gt6_material_form_gate as gate_builder  # noqa: E402

T8_PIPE_FORMS = {
    "tiny_fluid_pipe",
    "small_fluid_pipe",
    "fluid_pipe",
    "large_fluid_pipe",
    "huge_fluid_pipe",
    "item_pipe",
    "large_item_pipe",
    "huge_item_pipe",
}
T38_REQUIRED_FORMS = {
    material: set(forms)
    for material, forms in json.loads(
        (TOOLS / "t38_required_forms.json").read_text(encoding="utf-8")
    )["required_forms"].items()
}
T38_ACQUISITION_FORMS = {
    material: set(forms)
    for material, forms in json.loads(
        (TOOLS / "t38_source_backed_acquisition.json").read_text(
            encoding="utf-8"
        )
    )["required_forms"].items()
}
T39_REQUIRED_FORMS = {
    material: set(forms)
    for material, forms in json.loads(
        (TOOLS / "t39_required_forms.json").read_text(encoding="utf-8")
    )["required_forms"].items()
}
T40_REQUIRED_FORMS = {
    material: set(forms)
    for material, forms in json.loads(
        (TOOLS / "t40_required_forms.json").read_text(encoding="utf-8")
    )["required_forms"].items()
}


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

    def test_committed_outputs_are_current_without_raw_replay(self):
        self.assertEqual([], gate_builder.check_committed_outputs())

    def test_gate_covers_catalog_and_only_adds_bounded_source_forms(self):
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
            t10_forms = set(
                self.gate["t10_known_forms"].get(material["id"], ())
            )
            t38_forms = T38_REQUIRED_FORMS.get(material["id"], set())
            t38_acquisition_forms = T38_ACQUISITION_FORMS.get(
                material["id"], set()
            )
            t39_forms = T39_REQUIRED_FORMS.get(material["id"], set())
            t40_forms = T40_REQUIRED_FORMS.get(material["id"], set())
            self.assertLessEqual(
                registered - factual,
                T8_PIPE_FORMS
                | t10_forms
                | t38_forms
                | t38_acquisition_forms
                | t39_forms
                | t40_forms,
                material["id"],
            )
            if material.get("metadata_only"):
                metadata_only += 1
                self.assertFalse(factual, material["id"])
                self.assertFalse(registered, material["id"])
            else:
                self.assertTrue(factual, material["id"])
        self.assertEqual(663, metadata_only)
        self.assertEqual(282, self.gate["counts"]["t8_pipe_forms"])
        self.assertEqual(967, self.gate["counts"]["t10_known_forms"])
        self.assertEqual(12, self.gate["counts"]["t38_required_forms"])
        self.assertEqual(18, self.gate["counts"]["t39_required_forms"])
        self.assertEqual(3, self.gate["counts"]["t40_required_forms"])
        self.assertEqual(
            sum(map(len, T38_ACQUISITION_FORMS.values())),
            self.gate["counts"]["t38_acquisition_forms"],
        )
        self.assertEqual(
            {
                material: sorted(forms)
                for material, forms in sorted(T38_REQUIRED_FORMS.items())
            },
            self.gate["t38_required_forms"],
        )
        self.assertEqual(
            {
                material: sorted(forms)
                for material, forms in sorted(T39_REQUIRED_FORMS.items())
            },
            self.gate["t39_required_forms"],
        )
        self.assertEqual(
            {
                material: sorted(forms)
                for material, forms in sorted(T40_REQUIRED_FORMS.items())
            },
            self.gate["t40_required_forms"],
        )
        self.assertEqual(
            {
                material: sorted(forms)
                for material, forms in sorted(T38_ACQUISITION_FORMS.items())
            },
            self.gate["t38_source_backed_acquisition_forms"],
        )
        self.assertEqual(
            323,
            sum(
                "double_ingot" in forms
                for forms in self.gate["t10_known_forms"].values()
            ),
        )
        self.assertEqual(
            323,
            sum(
                "triple_ingot" in forms
                for forms in self.gate["t10_known_forms"].values()
            ),
        )
        self.assertEqual(
            321,
            sum(
                "ingot_hot" in forms
                for forms in self.gate["t10_known_forms"].values()
            ),
        )

    def test_gate_records_recipe_and_compatibility_sources(self):
        counts = self.gate["counts"]
        self.assertGreater(counts["recipe_gated_forms"], 0)
        self.assertGreater(counts["compatibility_forms"], 0)
        self.assertGreater(counts["ore_chain_forms"], 0)
        self.assertEqual(1, counts["t3_acceptance_forms"])
        self.assertEqual({"iron": ["wire"]}, self.gate["t3_acceptance_forms"])
        self.assertEqual(
            "t3_acceptance_required_not_gt6_original_gate",
            self.gate["sources"]["t3_acceptance_form_corrections"][
                "classification"
            ],
        )
        self.assertEqual(
            "t38_compact_output_runtime_required",
            self.gate["sources"]["t38_compact_required_forms"][
                "classification"
            ],
        )
        self.assertEqual(
            "t39_compact_output_runtime_required",
            self.gate["sources"]["t39_compact_required_forms"][
                "classification"
            ],
        )
        self.assertEqual(
            "t40_compact_output_runtime_required",
            self.gate["sources"]["t40_compact_required_forms"][
                "classification"
            ],
        )
        self.assertEqual(
            counts["registered_forms"],
            sum(map(len, self.gate["materials"].values())),
        )
        self.assertEqual(
            counts["compatibility_forms"],
            sum(map(len, self.gate["compatibility_forms"].values())),
        )
        self.assertEqual(29, counts["t6_electrical_wire_forms"])
        self.assertEqual(
            29, len(self.gate["t6_electrical_wire_forms"])
        )
        for material, forms in self.gate[
            "t6_electrical_wire_forms"
        ].items():
            self.assertEqual(["wire"], forms, material)
            self.assertIn("wire", self.gate["materials"][material])
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
            self.gate["counts"]["ore_source_materials"]
            * len(required_source_forms),
            self.gate["counts"]["ore_source_forms"],
        )
        for material_id in ore_materials:
            self.assertLessEqual(
                required_source_forms,
                set(self.gate["materials"][material_id]),
                material_id,
            )
        self.assertEqual(
            self.gate["counts"]["ore_source_materials"] * 2,
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
        ore_block_crusher_materials = {
            row["material"]
            for row in ore_chain["recipes"]
            if row["family"] == "crush_ore_block_to_crushed"
        }
        self.assertEqual(
            len(crusher_materials) + len(ore_block_crusher_materials),
            ore_chain["counts"]["recipes_by_map"]["crusher"],
        )
        self.assertEqual(357, len(crusher_materials))
        self.assertEqual(ore_materials, ore_block_crusher_materials)
        self.assertEqual(
            ore_materials,
            ore_materials & crusher_materials,
        )
        self.assertEqual(set(), ore_materials - crusher_materials)
        self.assertEqual(210, len(crusher_materials - ore_materials))
        self.assertLessEqual(
            ore_materials
            - set(self.gate["t38_source_backed_acquisition_forms"]),
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
