import copy
import json
import unittest

from tools import build_t13_denominator_readiness as readiness


class T13DenominatorReadinessTest(unittest.TestCase):
    def test_committed_manifest_and_readiness_are_current(self):
        manifest = readiness.build_manifest()
        document = readiness.build_readiness(manifest)
        self.assertEqual(
            readiness.stable(manifest),
            readiness.MANIFEST.read_text(encoding="utf-8"),
        )
        self.assertEqual(
            readiness.stable(document),
            readiness.READINESS.read_text(encoding="utf-8"),
        )

    def test_all_seven_denominators_close_without_unclassified(self):
        manifest = readiness.build_manifest()
        self.assertEqual(7, manifest["totals"]["tables"])
        self.assertEqual(0, manifest["totals"]["unclassified"])
        self.assertEqual(0, manifest["totals"]["normalization_blockers"])
        self.assertEqual(set(readiness.TABLES), set(manifest["tables"]))
        for table, row in manifest["tables"].items():
            with self.subTest(table=table):
                self.assertEqual(0, row["unclassified"])
                self.assertIn(row["uniform_status"], {"PASS", "UNIFORM_PASS"})
                self.assertGreater(row["raw_count"], 0)
                self.assertGreater(row["canonical_count"], 0)

    def test_fixed_source_and_recipe_denominator_are_exact(self):
        manifest = readiness.build_manifest()
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            manifest["source"]["revision"],
        )
        self.assertEqual(1229, manifest["source"]["selected_java_blobs"])
        self.assertEqual(1546, manifest["source"]["symbol_declarations"])
        self.assertEqual(95, manifest["totals"]["recipe_maps"])
        self.assertEqual(720_841, manifest["totals"]["recipe_rows"])

    def test_t13_ready_is_a_zero_content_denominator_gate(self):
        document = readiness.build_readiness(readiness.build_manifest())
        self.assertEqual("T13_READY", document["status"])
        self.assertEqual("CLOSED", document["acceptance"]["o_33"])
        self.assertEqual(0, document["zero_content_delta"]["datapack_delta"])
        self.assertEqual(
            0, document["zero_content_delta"]["publication_delta"]
        )
        self.assertEqual(
            6_025,
            document["zero_content_delta"]["datapack_recipe_entries"],
        )
        self.assertEqual(
            18_875,
            document["zero_content_delta"]["published_recipes"],
        )
        self.assertNotIn(
            "18,875 / 720,841",
            document["acceptance"]["canonical_completion_metric"],
        )
        currentness = document["currentness"]
        self.assertEqual(
            [],
            currentness["pending_report"]["pending"],
        )
        self.assertEqual(
            {"tools/t12_closure_readiness.json",
             "tools/t13_denominator_manifest.json"},
            set(currentness["dependencies"]),
        )
        self.assertEqual(
            "PROCESSING_MACHINE_ENERGY_AUDIT_READY",
            currentness["processing_machine_energy_audit"]["status"],
        )
        self.assertEqual(
            0,
            currentness["processing_machine_energy_audit"][
                "implicit_energy_arguments"
            ],
        )

    def test_unclassified_mutation_is_observable(self):
        recipe = json.loads(
            readiness.TABLES["recipe_maps"]["artifact"].read_text(
                encoding="utf-8"
            )
        )
        mutated = copy.deepcopy(recipe)
        mutated["counts"]["unclassified"] = 1
        _, unclassified = readiness.classification_counts(
            "recipe_maps", mutated
        )
        self.assertEqual(1, unclassified)


if __name__ == "__main__":
    unittest.main()
