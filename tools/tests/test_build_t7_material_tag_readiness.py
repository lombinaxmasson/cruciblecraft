import copy
import importlib.util
import json
import subprocess
import sys
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "build_t7_material_tag_readiness",
    TOOLS / "build_t7_material_tag_readiness.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class T7MaterialTagReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = MODULE.build()

    def test_source_and_classification_closure_are_pinned(self):
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            self.document["gt6_source"]["revision"],
        )
        self.assertEqual(62, self.document["classified"])
        self.assertEqual(0, self.document["unclassified"])
        self.assertEqual(
            {
                "build-time-only": 7,
                "deferred": 21,
                "not-applicable": 4,
                "rule-input": 30,
            },
            self.document["classification_counts"],
        )
        self.assertEqual(23, self.document["counts"]["processing_tag_count"])
        self.assertEqual(39, self.document["counts"]["property_tag_count"])
        self.assertEqual(6752, self.document["counts"]["tag_assignment_count"])

    def test_material_facts_and_rule_sets_are_exact(self):
        counts = self.document["counts"]
        self.assertEqual(1773, counts["material_count"])
        self.assertEqual(952, counts["formula_count"])
        self.assertEqual(0, counts["nonzero_explosion_damage_count"])
        self.assertEqual(0, counts["nonzero_heat_damage_count"])
        self.assertEqual(603, counts["mortar_tagged_material_count"])
        self.assertEqual(133, counts["mortar_registered_ingot_count"])
        self.assertEqual(126, counts["mortar_ingot_to_dust_count"])
        self.assertEqual(94, counts["mortar_gem_to_dust_count"])
        self.assertEqual(220, counts["new_mortar_rule_expansion_count"])
        rules = self.document["mortar_rules"]
        self.assertEqual(126, len(rules["ingot_to_dust"]["materials"]))
        self.assertEqual(94, len(rules["gem_to_dust"]["materials"]))
        self.assertIn("iron", rules["ingot_to_dust"]["materials"])
        self.assertIn("amber", rules["gem_to_dust"]["materials"])
        self.assertEqual(
            [
                "annealed_copper",
                "cast_iron",
                "iron_compressed",
                "iron_magnetic",
                "neodymium_magnetic",
                "steel_magnetic",
                "wrought_iron",
            ],
            rules["tagged_registered_ingot_without_dust"],
        )

    def test_expected_sets_are_independently_rederived(self):
        index = json.loads(MODULE.MATERIAL_INDEX.read_text(encoding="utf-8"))
        gate = json.loads(
            MODULE.REGISTRATION_GATE.read_text(encoding="utf-8")
        )["materials"]
        expected = {"ingot_to_dust": [], "gem_to_dust": []}
        for filename in index:
            material = json.loads(
                (MODULE.MATERIAL_ROOT / filename).read_text(encoding="utf-8")
            )
            material_id = material["id"]
            tags = set(
                (material.get("gt6_metadata") or {}).get("material_tags") or []
            )
            forms = set(gate[material_id])
            if (
                "PROCESSING.MORTAR_GRINDABLE" not in tags
                or "dust" not in forms
            ):
                continue
            if "ingot" in forms:
                expected["ingot_to_dust"].append(material_id)
            if "gem" in forms:
                expected["gem_to_dust"].append(material_id)
        for rule_id, material_ids in expected.items():
            self.assertEqual(
                sorted(material_ids),
                self.document["mortar_rules"][rule_id]["materials"],
            )

    def test_wrong_expected_count_mutations_fail_loudly(self):
        for field, wrong in (
            ("mortar_ingot_to_dust_count", 125),
            ("mortar_gem_to_dust_count", 93),
            ("new_mortar_rule_expansion_count", 219),
        ):
            with self.subTest(field=field):
                policy = copy.deepcopy(MODULE.load(MODULE.POLICY))
                policy["acceptance"][field] = wrong
                with self.assertRaisesRegex(
                    ValueError, "acceptance counts drifted"
                ):
                    MODULE.build(policy)
        policy = copy.deepcopy(MODULE.load(MODULE.POLICY))
        policy["tags"].pop()
        with self.assertRaisesRegex(
            ValueError, "classification is not closed"
        ):
            MODULE.build(policy)

    def test_runtime_contract_and_energy_type_decision_are_closed(self):
        runtime = self.document["rule_language"]
        self.assertTrue(runtime["thermal_numeric_comparison"])
        self.assertTrue(runtime["damage_numeric_comparison"])
        self.assertTrue(runtime["zero_loss_cable_domain"])
        self.assertEqual(
            ["HEAT", "KINETIC", "AIR", "ELECTRIC"],
            runtime["energy_types"],
        )
        self.assertEqual(
            {"ROTATION", "MAGNETIC", "COOLING"},
            set(self.document["energy_type_decision"]["removed"]),
        )
        publication = self.document["runtime_publication_acceptance"]
        self.assertEqual(691, publication["post_t7_mortar_recipes"])
        self.assertEqual(
            17189, publication["post_t7_all_published_recipes"]
        )
        self.assertLess(
            publication["post_t7_all_published_recipes"],
            publication["all_published_recipe_budget"],
        )
        self.assertEqual(0, publication["shadowed_input_signatures"])

    def test_committed_ledger_is_current_and_check_is_read_only(self):
        encoded = MODULE.stable_json(self.document)
        self.assertEqual(encoded, MODULE.OUTPUT.read_text(encoding="utf-8"))
        before = MODULE.sha256(MODULE.OUTPUT)
        completed = subprocess.run(
            [
                sys.executable,
                str(TOOLS / "build_t7_material_tag_readiness.py"),
                "--check",
            ],
            cwd=MODULE.ROOT,
            capture_output=True,
            text=True,
            timeout=120,
        )
        self.assertEqual(
            0, completed.returncode, completed.stdout + completed.stderr
        )
        self.assertEqual(before, MODULE.sha256(MODULE.OUTPUT))


if __name__ == "__main__":
    unittest.main()
