import importlib.util
import json
import sys
import unittest


TOOLS = __import__("pathlib").Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "build_t5_chemical_readiness",
    TOOLS / "build_t5_chemical_readiness.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class T5ChemicalReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.policy = json.loads(MODULE.POLICY.read_text(encoding="utf-8"))
        cls.document = MODULE.build()

    def test_committed_ledger_is_current_and_deterministic(self):
        encoded = MODULE.stable_json(self.document)
        self.assertEqual(
            encoded,
            MODULE.OUTPUT.read_text(encoding="utf-8"),
        )
        self.assertEqual(encoded, MODULE.stable_json(self.document))
        self.assertEqual(1, self.document["schema_version"])
        self.assertEqual("SOURCE_REPLAY_VERIFIED", self.document["status"])
        self.assertEqual(0, self.document["counts"]["unclassified"])

    def test_gt6_loader_source_and_semantics_are_pinned(self):
        source = self.document["gt6_source"]
        self.assertEqual("GregTech6/gregtech6", source["repository"])
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            source["revision"],
        )
        self.assertEqual(
            "src/main/java/gregtech/loaders/c/"
            "Loader_Recipes_Decomp.java",
            source["loader_path"],
        )
        semantics = self.document["loader_semantics"]
        self.assertEqual(
            [
                "components_present",
                "COMPOUNDS.DECOMPOSABLE",
                "component_common_divider_known",
                "component_common_divider_lte_64",
                "at_least_one_route_tag",
            ],
            semantics["conjunction"],
        )
        self.assertEqual(
            64, semantics["common_divider_gate"]["maximum"]
        )
        for row in [
            semantics["components_gate"],
            semantics["decomposable_gate"],
            semantics["common_divider_gate"],
            semantics["output_phase_rule"],
            semantics["input_selection_rule"],
            *semantics["route_tags"].values(),
        ]:
            self.assertTrue(row["reason"].strip())
            self.assertTrue(row["source"])

    def test_exact_debt_domains_and_partitions_are_locked(self):
        counts = self.document["counts"]
        self.assertEqual(
            {
                "terminal_dust_t5_chemical": 145,
                "crusher_t5_chemical": 110,
                "chemical_overlap": 31,
                "unique_chemical_union": 224,
                "byproduct_only_debt": 110,
                "byproduct_materials_also_in_chemical_union": 60,
                "byproduct_materials_outside_chemical_union": 50,
            },
            counts["input_ledgers"],
        )
        self.assertEqual(
            {
                "crusher_only": 79,
                "overlap": 31,
                "terminal_dust_only": 114,
            },
            counts["origin_partitions"],
        )
        self.assertEqual(
            {
                "decomposable_without_route": 43,
                "route_ready": 27,
                "route_tagged_but_quarantined": 57,
                "unresolved_deferred": 97,
            },
            counts["material_classifications"],
        )
        self.assertEqual(84, counts["source_loader_eligible"])
        self.assertEqual(177, counts["no_decompose"])
        self.assertEqual(
            {
                "gt.recipe.centrifuge": 18,
                "gt.recipe.electrolyzer": 66,
            },
            counts["route_map_memberships"],
        )
        self.assertEqual(
            {
                "at_least_one_route_tag": 140,
                "component_common_divider_known": 90,
                "component_common_divider_lte_64": 90,
                "components_present": 90,
                "decomposable_tag": 97,
            },
            counts["failed_loader_conjuncts"],
        )

    def test_every_material_obeys_the_loader_conjunction(self):
        rows = self.document["chemical_materials"]
        self.assertEqual(224, len(rows))
        self.assertEqual(
            len(rows), len({row["material"] for row in rows})
        )
        route_tags = {
            "PROCESSING.CENTRIFUGABLE": "gt.recipe.centrifuge",
            "PROCESSING.ELECTROLYSABLE": "gt.recipe.electrolyzer",
        }
        for row in rows:
            with self.subTest(material=row["material"]):
                self.assertTrue(row["reason"].strip())
                self.assertTrue(row["reason_code"])
                self.assertTrue(row["source"])
                self.assertTrue(row["source_evidence"])
                self.assertEqual(
                    self.document["gt6_source"]["revision"],
                    row["source"]["revision"],
                )
                expected_maps = {
                    map_id
                    for tag, map_id in route_tags.items()
                    if tag in row["material_tags"]
                }
                actual_maps = {
                    route["map"] for route in row["map_destinations"]
                }
                self.assertEqual(expected_maps, actual_maps)
                self.assertNotIn(
                    "no_decompose",
                    MODULE.stable_json(row["map_destinations"]),
                )

                conjunction_passes = all(
                    row["loader_conjunction"].values()
                )
                self.assertEqual(
                    conjunction_passes,
                    row["source_loader_eligible"],
                )
                classification = row["classification"]
                if classification == "route_ready":
                    self.assertTrue(conjunction_passes)
                    self.assertFalse(row["no_decompose"])
                    self.assertTrue(actual_maps)
                elif classification == "route_tagged_but_quarantined":
                    self.assertTrue(conjunction_passes)
                    self.assertTrue(row["no_decompose"])
                    self.assertTrue(actual_maps)
                elif classification == "decomposable_without_route":
                    self.assertTrue(
                        row["loader_conjunction"][
                            "components_present"
                        ]
                    )
                    self.assertTrue(
                        row["loader_conjunction"]["decomposable_tag"]
                    )
                    self.assertTrue(
                        row["loader_conjunction"][
                            "component_common_divider_lte_64"
                        ]
                    )
                    self.assertFalse(actual_maps)
                else:
                    self.assertEqual(
                        "unresolved_deferred", classification
                    )
                    self.assertFalse(conjunction_passes)

    def test_byproduct_only_debt_has_complete_evidence(self):
        rows = self.document["byproduct_only_debt"]
        self.assertEqual(110, len(rows))
        self.assertEqual(
            len(rows), len({row["material"] for row in rows})
        )
        chemical_ids = {
            row["material"]
            for row in self.document["chemical_materials"]
        }
        self.assertEqual(
            60,
            len(chemical_ids & {row["material"] for row in rows}),
        )
        for row in rows:
            self.assertEqual(
                "byproduct_only_debt", row["classification"]
            )
            self.assertTrue(row["reason"].strip())
            self.assertTrue(row["source"])
            self.assertTrue(row["source_evidence"])

    def test_non_molten_fluid_inventory_is_exhaustive(self):
        rows = self.document["non_molten_fluid_candidates"]
        counts = self.document["counts"]["fluids"]
        self.assertEqual(322, counts["all_normalized_fluid_records"])
        self.assertEqual(203, counts["excluded_molten_candidates"])
        self.assertEqual(119, counts["non_molten_candidates"])
        self.assertEqual(
            {
                "closure_required": 8,
                "not_required_by_loader_projection": 111,
            },
            counts["classifications"],
        )
        self.assertEqual(
            {"gas": 35, "liquid": 73, "solid": 11},
            counts["phases"],
        )
        self.assertEqual(14, counts["projected_source_materials"])
        self.assertEqual(67, counts["projected_relations"])
        self.assertEqual(
            3, counts["phase_test_without_fluid_candidate_count"]
        )
        self.assertEqual(0, counts["missing_component_source_count"])
        self.assertEqual(119, len(rows))
        self.assertEqual(
            len(rows), len({row["fluid"] for row in rows})
        )
        self.assertEqual(
            {
                "argon",
                "bromine",
                "chlorine",
                "fluorine",
                "hydrogen",
                "mercury",
                "sulfurtrioxide",
                "water",
            },
            {
                row["fluid"]
                for row in rows
                if row["classification"] == "closure_required"
            },
        )
        for row in rows:
            with self.subTest(fluid=row["fluid"]):
                self.assertFalse(
                    row["fluid"].startswith(("molten.", "molten "))
                )
                self.assertIn(row["phase"], {"solid", "liquid", "gas"})
                self.assertIsInstance(row["source_material_id"], int)
                self.assertTrue(row["source_material_name"])
                self.assertTrue(row["reason"].strip())
                self.assertTrue(row["source"])
                self.assertTrue(row["source_evidence"])
                if row["classification"] == "closure_required":
                    self.assertEqual(
                        1, len(row["candidate_ids_for_source_material"])
                    )
                    self.assertTrue(row["projected_relations"])
                else:
                    self.assertTrue(row["reason_code"])

    def test_required_pinned_recipe_maps_are_verified(self):
        coverage = self.document["template_coverage"]
        self.assertEqual(
            {
                "gt.recipe.assembler",
                "gt.recipe.autoclave",
                "gt.recipe.bath",
                "gt.recipe.centrifuge",
                "gt.recipe.compressor",
                "gt.recipe.drying",
                "gt.recipe.distillery",
                "gt.recipe.electrolyzer",
                "gt.recipe.mixer",
                "gt.recipe.smelter",
            },
            set(coverage),
        )
        for map_id, row in coverage.items():
            with self.subTest(map_id=map_id):
                self.assertEqual(
                    "pinned_source_present", row["classification"]
                )
                self.assertTrue(
                    (MODULE.ROOT / row["pinned_dump_path"]).is_file()
                )
                self.assertRegex(row["pinned_dump_sha256"], r"^[0-9a-f]{64}$")
                self.assertTrue(row["reason"].strip())
                self.assertTrue(row["source"])
        replay = self.document["recipe_replay"]
        self.assertEqual("pinned_dump_verified", replay["status"])
        self.assertEqual(set(coverage), set(replay["required_maps"]))
        self.assertEqual([], self.document["blockers"])

    def test_input_hashes_are_locked(self):
        self.assertEqual(
            {
                "gitignore": (
                    "1d8924c3342e65373bd08c93ee90dca23d4731ac8e4f1937"
                    "3e9453c631aad929"
                ),
                "gt6_map_roadmap": (
                    "7a4c2a844a2cc5379954927f899660061e1168c10bf76b178"
                    "a96373c29c29bc0"
                ),
                "gt6_ore_chain_closure": (
                    "9632faf5365d44097900fbdd8f05d7716d90a36bfcfe3665"
                    "f9e37d0aee7ceb97"
                ),
                "gt6_oredict_fluids_normalized": (
                    "3857607584cc7b26ea29f7c7b024157ffb18b2b3652dca0b"
                    "dbc9aadb11bdac5a"
                ),
                "gt6_oredict_materials_normalized": (
                    "4ac8e9d2bc6cc398bb02205ee1b396554f289ab50d5def48c"
                    "1152a7ca1100982"
                ),
                "gt6_recipe_dump_index": (
                    "04a1ddf9f0bee4432206d2e89890839c10e4a9ad0b66aa3cd"
                    "67f7385c31abc7f"
                ),
                "gt6_recipe_templates_index": (
                    "5859464792f80d836d14ca4a823b1ae17e1628ac9e1121e33"
                    "b28d7d7f8fda584"
                ),
                "gt6_recipe_templates_report": (
                    "befcd40bb9287f568e83a2a2d50846f31be689d3dcbea59af"
                    "78a7835dadb03b9"
                ),
                "material_catalog": (
                    "07bfb2d0c2067f8b5f00a92b8c419b1711ffae1fdb6c3ff3"
                    "f03ca42502ce8965"
                ),
                "material_index": (
                    "ff3ecf3970191f4feaddb84eb7795ea7714cab665df3fb682"
                    "2f61c8382e66e05"
                ),
                "material_registration_gate": (
                    "95a19d45c16965b2facbef85eaf9c9c2b5e725d51a64adbe"
                    "efef5fbdf9e1c284"
                ),
                "policy": (
                    "5e366cf4824d5f55fe79ce48451d35b031b964a4ef6023a21"
                    "227b1282cfaebbc"
                ),
            },
            self.document["source_hashes"],
        )


if __name__ == "__main__":
    unittest.main()
