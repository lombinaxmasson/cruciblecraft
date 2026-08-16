import copy
import importlib.util
import json
import sys
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "build_t8_pipe_readiness",
    TOOLS / "build_t8_pipe_readiness.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class T8PipeReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = MODULE.gt6_pipes.load(MODULE.SOURCE)
        cls.policy = MODULE.load(MODULE.POLICY)
        cls.document = MODULE.build()

    def test_source_revisions_hashes_and_licenses_are_pinned(self):
        sources = self.document["sources"]
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            sources["gt6"]["revision"],
        )
        self.assertEqual("LGPL-3.0-or-later", sources["gt6"]["license"])
        self.assertEqual(
            MODULE.gt6_pipes.GT6_SOURCE_FILE_HASHES,
            {
                path: row["sha256"]
                for path, row in sources["gt6"]["files"].items()
            },
        )
        self.assertEqual(
            "de5d2c4a4c863b94a10bfb5d0839df2de8246628",
            sources["gtm_reference"]["revision"],
        )
        self.assertEqual("LGPL-3.0", sources["gtm_reference"]["license"])
        self.assertEqual(
            MODULE.gt6_pipes.GTM_SOURCE_FILE_HASHES,
            {
                path: row["sha256"]
                for path, row in sources["gtm_reference"]["files"].items()
            },
        )
        self.assertEqual(
            40, len(self.source["direct_addFluidPipes_calls"])
        )
        self.assertEqual(
            21, len(self.source["direct_addItemPipes_calls"])
        )

    def test_raw_and_live_direct_and_extension_sets_close(self):
        fluid = self.document["classification"]["fluid"]
        item = self.document["classification"]["item"]
        self.assertEqual(
            {
                "acceptance_extension": 2,
                "direct_source": 40,
                "not_applicable": 2172,
                "unclassified": 0,
            },
            fluid["raw"]["counts"],
        )
        self.assertEqual(
            {
                "acceptance_extension": 2,
                "direct_source": 40,
                "not_applicable": 1731,
                "unclassified": 0,
            },
            fluid["live"]["counts"],
        )
        self.assertEqual(
            {
                "acceptance_extension": 3,
                "direct_source": 21,
                "not_applicable": 2190,
                "unclassified": 0,
            },
            item["raw"]["counts"],
        )
        self.assertEqual(
            {
                "acceptance_extension": 3,
                "direct_source": 21,
                "not_applicable": 1749,
                "unclassified": 0,
            },
            item["live"]["counts"],
        )
        self.assertEqual(2214, fluid["raw"]["denominator"])
        self.assertEqual(1773, fluid["live"]["denominator"])
        self.assertEqual(0, self.document["counts"]["unclassified"])

    def test_acceptance_extensions_are_never_mislabeled_as_gt6(self):
        acceptance = {
            row["material"]: row
            for row in self.document["acceptance_materials"]
        }
        self.assertEqual({"copper", "tin", "iron"}, set(acceptance))
        self.assertEqual(
            "direct_gt6_source",
            acceptance["copper"]["fluid_classification"],
        )
        self.assertEqual(
            "cruciblecraft_acceptance_extension",
            acceptance["tin"]["fluid_classification"],
        )
        self.assertEqual(
            "cruciblecraft_acceptance_extension",
            acceptance["iron"]["fluid_classification"],
        )
        for material in ("copper", "tin", "iron"):
            self.assertEqual(
                "cruciblecraft_acceptance_extension",
                acceptance[material]["item_classification"],
            )
        for domain in ("fluid_domain", "item_domain"):
            for row in self.document[domain]["material_catalog"]:
                if row["classification"] == (
                    "cruciblecraft_acceptance_extension"
                ):
                    self.assertFalse(row["direct_gt6_source"])
                    self.assertFalse(
                        row["evidence"]["direct_gt6_source"]
                    )

    def test_fluid_and_item_properties_are_derived_exactly(self):
        fluid = {
            row["material"]: row
            for row in self.document["fluid_domain"]["material_catalog"]
        }
        tin = fluid["tin"]["specifications_by_gauge"]
        self.assertEqual(100, tin["tiny"]["capacity_mb"])
        self.assertEqual(600, tin["normal"]["capacity_mb"])
        self.assertEqual(2400, tin["huge"]["capacity_mb"])
        self.assertEqual(631, tin["normal"]["max_temperature_kelvin"])
        self.assertTrue(tin["normal"]["contact_damage"])
        self.assertTrue(tin["normal"]["recipe"])
        self.assertTrue(tin["normal"]["blocking"])
        for field in (
            "gas_proof",
            "acid_proof",
            "plasma_proof",
            "magic_proof",
            "flammable",
        ):
            self.assertFalse(tin["normal"][field])
        self.assertEqual(
            2263,
            fluid["iron"]["specifications_by_gauge"]["normal"][
                "max_temperature_kelvin"
            ],
        )
        copper = fluid["copper"]
        self.assertEqual("direct_gt6_source", copper["classification"])
        self.assertEqual(
            1696,
            copper["specifications_by_gauge"]["normal"][
                "max_temperature_kelvin"
            ],
        )

        item = {
            row["material"]: row
            for row in self.document["item_domain"]["material_catalog"]
        }
        for material in ("copper", "tin", "iron"):
            specifications = item[material]["specifications_by_gauge"]
            self.assertEqual(
                (16384, 1),
                (
                    specifications["normal"]["step_size"],
                    specifications["normal"]["stacks_per_second"],
                ),
            )
            self.assertEqual(
                (8192, 2),
                (
                    specifications["large"]["step_size"],
                    specifications["large"]["stacks_per_second"],
                ),
            )
            self.assertEqual(
                (4096, 4),
                (
                    specifications["huge"]["step_size"],
                    specifications["huge"]["stacks_per_second"],
                ),
            )

    def test_nonmetal_fluid_pipe_acquisition_closure_is_exact(self):
        acquisition = self.document[
            "nonmetal_fluid_pipe_acquisition"
        ]
        self.assertEqual("O-27", acquisition["open_item"])
        self.assertEqual("CLOSED_T19C", acquisition["status"])
        # Equality with the T19 runtime rows is proven by
        # build_t19_pipe_acquisition (index 48) as a forward edge; T8 records
        # only its own derived forms.
        self.assertEqual(
            "tools/build_t19_pipe_acquisition.py (index 48)",
            acquisition["validation_owner"],
        )
        self.assertEqual(
            ["carbon", "plastic", "rubber", "wood", "wood_treated"],
            acquisition["materials"],
        )
        self.assertEqual(25, acquisition["form_count"])
        self.assertEqual(0, acquisition["unreachable"])
        self.assertEqual(
            25,
            len({
                (row["material"], row["gauge"])
                for row in acquisition["forms"]
            }),
        )

    def test_dead_runtime_api_declarations_are_explicitly_removed(self):
        decisions = self.document["runtime_api_decisions"]
        self.assertEqual(
            "REMOVED",
            decisions["outgoing_cover_guard"]["status"],
        )
        self.assertEqual(
            "REMOVED",
            decisions["cover_set_version"]["status"],
        )

    def test_prefix_budget_deferred_and_recipe_projection_are_exact(self):
        counts = self.document["counts"]
        self.assertEqual(8, counts["selected_prefixes"])
        self.assertEqual(210, counts["fluid_runtime_blocks"])
        self.assertEqual(72, counts["item_runtime_blocks"])
        self.assertEqual(282, counts["combined_runtime_blocks"])
        self.assertEqual(18048, counts["logical_states"])
        budget = self.document["runtime_budget"]
        self.assertEqual(64, budget["connection_masks_per_block"])
        self.assertLessEqual(
            budget["combined_runtime_blocks"],
            budget["max_combined_runtime_blocks"],
        )
        self.assertLessEqual(
            budget["logical_states"],
            budget["max_logical_states"],
        )
        deferred = self.document["prefix_facts"]["deferred_gauges"]
        self.assertEqual(
            {"quadruple", "nonuple"},
            {row["name"] for row in deferred["fluid"]},
        )
        self.assertEqual(
            {
                "small",
                "restrictive_small",
                "restrictive_normal",
                "restrictive_large",
                "restrictive_huge",
            },
            {row["name"] for row in deferred["item"]},
        )
        recipe = self.document["recipe_projection"]
        self.assertEqual(8, recipe["generic_rule_count"])
        self.assertEqual(185, recipe["fluid_material_expansion_count"])
        self.assertEqual(72, recipe["item_material_expansion_count"])
        self.assertEqual(257, recipe["material_expansion_count"])
        self.assertEqual(
            37,
            self.document["fluid_domain"][
                "recipe_enabled_material_count"
            ],
        )
        self.assertEqual(
            24,
            self.document["item_domain"][
                "recipe_enabled_material_count"
            ],
        )
        self.assertFalse(recipe["per_material_java_required"])
        self.assertFalse(
            self.document["import_architecture"][
                "per_material_java_required"
            ]
        )

    def test_wrong_count_mutation_fails(self):
        policy = copy.deepcopy(self.policy)
        policy["expected_counts"]["combined_runtime_blocks"] = 281
        with self.assertRaisesRegex(
            ValueError, "T8 expected counts drifted"
        ):
            MODULE.build(policy)

    def test_budget_mutations_fail(self):
        policy = copy.deepcopy(self.policy)
        policy["budget"]["max_combined_runtime_blocks"] = 281
        with self.assertRaisesRegex(ValueError, "block budget exceeded"):
            MODULE.build(policy)
        policy = copy.deepcopy(self.policy)
        policy["budget"]["max_logical_states"] = 18047
        with self.assertRaisesRegex(
            ValueError, "logical-state budget exceeded"
        ):
            MODULE.build(policy)

    def test_source_classification_mutation_fails(self):
        policy = copy.deepcopy(self.policy)
        policy["acceptance_extensions"]["fluid"][0] = {
            "material": "copper",
            "source_name": "Copper",
        }
        with self.assertRaisesRegex(
            ValueError, "acceptance extension overlaps direct source"
        ):
            MODULE.build(policy)

    def test_committed_ledger_is_current_and_check_is_read_only(self):
        encoded = MODULE.stable_json(self.document)
        self.assertEqual(encoded, MODULE.OUTPUT.read_text(encoding="utf-8"))
        before = MODULE.sha256(MODULE.OUTPUT)
        self.assertEqual(encoded, MODULE.stable_json(self.document))
        self.assertEqual(before, MODULE.sha256(MODULE.OUTPUT))
        self.assertEqual("READY", self.document["status"])


if __name__ == "__main__":
    unittest.main()
