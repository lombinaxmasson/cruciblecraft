from __future__ import annotations

import json
import unittest

from tools import build_t10_preflight_projection as builder


class T10PreflightProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = builder.build()

    def test_known_routes_are_rederived_from_tags_and_registered_ingots(self):
        self.assertEqual("T10_READY", self.document["status"])
        routes = self.document["route_projections"]
        self.assertEqual(321, routes["hot_ingot"]["material_count"])
        self.assertEqual(321, routes["hot_ingot"]["recipe_count"])
        self.assertEqual(323, routes["multi_ingot"]["material_count"])
        self.assertEqual(646, routes["multi_ingot"]["recipe_count"])
        self.assertEqual(1, len(routes["hot_ingot"]["routes"]))
        self.assertEqual(2, len(routes["multi_ingot"]["routes"]))

    def test_container_domains_close_without_recipe_publication(self):
        containers = self.document["container_domains"]
        self.assertEqual(
            "CLOSED_WITH_COMPONENT_CELLS",
            containers["status"],
        )
        self.assertEqual(
            {
                "ITEMGENERATOR.CONTAINERS": 95,
                "ITEMGENERATOR.CONTAINERS_FLUID": 61,
                "ITEMGENERATOR.CONTAINERS_GAS": 48,
            },
            containers["counts"],
        )
        self.assertEqual(204, containers["membership_count"])
        self.assertEqual(123, containers["union_material_count"])
        self.assertEqual(
            "READY",
            containers["runtime_acceptance"]["status"],
        )
        self.assertEqual(
            93,
            containers["runtime_acceptance"]["counts"][
                "new_t10_chemical_fluids"
            ],
        )

    def test_global_budget_uses_known_projection_and_rounding_policy(self):
        budget = self.document["budget_projection"]
        self.assertEqual(17_583, budget["post_t8_published_recipes"])
        self.assertEqual(0, budget["t9_projected_recipe_additions"])
        self.assertEqual(967, budget["known_t10_recipe_additions"])
        self.assertEqual(18_550, budget["known_post_t10_published_recipes"])
        self.assertEqual(21_000, budget["global_budget"])
        self.assertEqual(2_450, budget["remaining_after_known_t10"])

    def test_prefix_fact_layer_is_pinned_with_exact_bulk_registration(self):
        facts = self.document["prefix_facts"]
        self.assertEqual(57, facts["startup_prefix_count"])
        self.assertEqual(1_773, facts["material_count"])
        self.assertEqual(1_830, facts["handshake_entry_count"])
        self.assertEqual(
            {
                "double_ingot": 323,
                "triple_ingot": 323,
                "ingot_hot": 321,
            },
            facts["gate_registration_counts"],
        )
        self.assertEqual(
            {"ingotHot": 3.0},
            facts["nonzero_source_heat_damage"],
        )
        self.assertEqual(
            {"cruciblecraft:ingot_hot": 3.0},
            facts["nonzero_runtime_heat_damage"],
        )
        self.assertEqual(
            {
                "ingotDouble": "cruciblecraft:double_ingot",
                "ingotTriple": "cruciblecraft:triple_ingot",
                "ingotHot": "cruciblecraft:ingot_hot",
            },
            facts["source_to_cruciblecraft"],
        )

    def test_source_blobs_and_runtime_publication_are_closed(self):
        evidence = self.document["source_evidence"]
        self.assertEqual(
            "f915645f3009d3dbe61abbafe77791000be32747",
            evidence["op_java"]["git_blob_sha1"],
        )
        self.assertEqual(
            "e1a89b2c04e1183fda13a490c5a035512acf7f14",
            evidence["ut_java"]["git_blob_sha1"],
        )
        publication = self.document["runtime_publication"]
        self.assertEqual(3, publication["current_t10_datapack_entries"])
        self.assertEqual(967, publication["current_t10_recipe_additions"])
        self.assertEqual(18_550, publication["post_t10_published_recipes"])
        self.assertEqual(0, publication["known_future_t10_recipe_additions"])
        self.assertEqual(
            1_500, publication["known_form_material_rule_budget"]
        )
        self.assertTrue(
            publication["within_known_form_material_rule_budget"]
        )

    def test_load_gate_closes_datapack_publication_and_runtime_contract(self):
        load = self.document["load_gate"]
        self.assertEqual("READY", load["status"])
        self.assertEqual(3_360, load["datapack_recipe_entries"])
        self.assertEqual(18_550, load["published_recipes"])
        self.assertGreaterEqual(load["compression_ratio"], 3.0)
        self.assertTrue(
            load["runtime_contract"]["budget_constants_present"]
        )
        self.assertTrue(
            load["runtime_contract"]["runtime_assertions_present"]
        )

    def test_committed_projection_is_current(self):
        self.assertEqual(
            self.document,
            json.loads(builder.OUTPUT.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
