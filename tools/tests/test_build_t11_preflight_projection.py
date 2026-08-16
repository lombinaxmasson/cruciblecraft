from __future__ import annotations

import hashlib
import json
import unittest
from pathlib import Path

from tools import build_t11_preflight_projection as builder


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T11PreflightProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )

    def test_independent_expectation_selects_exact_pinned_rows(self):
        self.assertEqual("T11_READY", self.document["status"])
        self.assertEqual(
            [
                "crude_oil_distillation",
                "fuel_oil_engine",
                "methane_gas_fuel",
                "natural_gas_to_methane",
            ],
            self.document["independent_expectation"]["route_ids"],
        )
        routes = self.document["selected_routes"]
        self.assertEqual(872, routes["crude_oil_distillation"]["recipe_index"])
        self.assertEqual(14, routes["fuel_oil_engine"]["recipe_index"])
        self.assertEqual(20, routes["methane_gas_fuel"]["recipe_index"])
        self.assertEqual(
            553, routes["natural_gas_to_methane"]["recipe_index"]
        )
        self.assertEqual(
            [
                {
                    "amount": 25,
                    "id": "cruciblecraft:crude_oil",
                    "source_fluid": "liquid_medium_oil",
                }
            ],
            routes["crude_oil_distillation"]["runtime_projection"][
                "fluid_inputs"
            ],
        )
    def test_selected_source_row_hashes_match_raw_dump(self):
        routes = self.document["selected_routes"]
        source_paths = [
            builder.ROOT / route["source"]["path"].split("#", 1)[0]
            for route in routes.values()
        ]
        if any(not path.is_file() for path in source_paths):
            self.skipTest("gt6_dump is an explicit source-replay cache")
        for route in routes.values():
            source_path, row_ref = route["source"]["path"].split("#", 1)
            source = json.loads(
                (builder.ROOT / source_path).read_text(encoding="utf-8")
            )
            index = int(row_ref.removeprefix("recipes[").removesuffix("]"))
            self.assertEqual(
                builder.canonical_hash(source["recipes"][index]),
                route["source"]["row_sha256"],
            )

    def test_all_distillery_rows_have_an_explicit_t11a_disposition(self):
        ledger = self.document["distillery_ledger"]
        self.assertEqual(
            {
                "classified": 1_517,
                "deferred_outside_minimal_t11_set": 1_516,
                "selected_source_pinned_design_bridge": 1,
                "source_rows": 1_517,
                "unclassified": 0,
            },
            ledger["counts"],
        )
        self.assertEqual(872, ledger["selected_recipe_index"])
        selected = [
            row
            for row in ledger["rows"]
            if row["classification"]
            == "selected_source_pinned_design_bridge"
        ]
        self.assertEqual([872], [row["recipe_index"] for row in selected])
        self.assertTrue(all(
            row["reason_code"] for row in ledger["rows"]
        ))
        expected = {
            "natural_gas_to_methane": (10_236, 553),
            "fuel_oil_engine": (21, 14),
            "methane_gas_fuel": (49, 20),
        }
        for route, (source_rows, selected_index) in expected.items():
            route_ledger = self.document["source_map_ledgers"][route]
            self.assertEqual(source_rows, route_ledger["counts"][
                "classified"
            ])
            self.assertEqual(0, route_ledger["counts"]["unclassified"])
            self.assertEqual(
                selected_index, route_ledger["selected_recipe_index"]
            )

    def test_gate_and_primary_fluid_closure_are_fail_closed(self):
        gate = json.loads(builder.FLUID_GATE.read_text(encoding="utf-8"))
        self.assertEqual(1, gate["schema_version"])
        self.assertEqual(["crude_oil", "natural_gas"], [
            row["id"] for row in gate["fluids"]
        ])
        crude = gate["fluids"][0]
        self.assertEqual("crude_oil", crude["material"])
        self.assertEqual("liquid", crude["state"])
        self.assertFalse(crude["world_placeable"])
        self.assertEqual(
            "gt6_dump/gt6_recipe_dump/maps/"
            "gt.recipe.distillery.json#recipes[872]",
            crude["source"]["path"],
        )

        closure = self.document["fluid_identity_closure"]
        self.assertEqual(
            {
                "cruciblecraft:crude_oil",
                "cruciblecraft:fuel",
                "cruciblecraft:methane",
                "cruciblecraft:natural_gas",
            },
            set(closure["primary_fluids"]),
        )
        self.assertTrue(all(
            row["producer"] and row["logistics"] and row["consumer"]
            for row in closure["primary_fluids"].values()
        ))
        self.assertEqual(
            "t11_hydrocarbon_fluid_gate",
            closure["identity_providers"]["cruciblecraft:crude_oil"],
        )
        self.assertEqual(
            "t11_hydrocarbon_fluid_gate",
            closure["identity_providers"]["cruciblecraft:natural_gas"],
        )
        self.assertEqual(
            "natural_gas_to_methane",
            closure["primary_fluids"]["cruciblecraft:methane"][
                "producer"
            ],
        )
        bridge = closure["source_fluid_bridges"]["liquid_medium_oil"]
        self.assertEqual("DESIGN_POLICY", bridge["status"])
        self.assertEqual(
            "NO_DIRECT_BINDING_AT_FIXED_REVISION",
            bridge["gt6_equivalence"],
        )
        self.assertEqual("O-37", bridge["closed_item"])
        self.assertEqual(
            "O37_CLOSED_PERMANENT_DESIGN_POLICY",
            bridge["closure"],
        )
        self.assertTrue(bridge["permanent"])
        self.assertEqual(0, bridge["publication_delta"])
        self.assertFalse(
            closure["coproducts"]["cruciblecraft:lubricant"][
                "fake_voiding_allowed"
            ]
        )
        cell_gate = json.loads(
            builder.T11_CELL_GATE.read_text(encoding="utf-8")
        )
        self.assertEqual(
            [{
                "id": "cruciblecraft:natural_gas",
                "kind": "gas",
                "material": "natural_gas",
            }],
            cell_gate["fluids"],
        )

    def test_load_projection_preserves_independent_budgets(self):
        load = self.document["load_gate"]
        self.assertEqual("READY", load["status"])
        self.assertEqual(18_871, load["current"]["published_recipes"])
        self.assertEqual(18_875, load["projected"]["published_recipes"])
        self.assertEqual(
            3_243, load["projected"]["datapack_recipe_entries"]
        )
        self.assertEqual(
            154, load["projected"]["t5_plus_t11_chemical_recipes"]
        )
        self.assertEqual(
            0, load["budgets"]["t11_authored_material_rules"]
        )
        self.assertEqual(21_000, load["budgets"]["published_recipes"])
        self.assertGreaterEqual(
            load["projected"]["compression_ratio"],
            load["budgets"]["minimum_compression_ratio"],
        )
        runtime = self.document["runtime_policy"]
        self.assertEqual(
            "SOURCE_MATERIAL_LAYER_ONLY",
            runtime["raw_oil_identity"]["material_9852_role"],
        )
        self.assertEqual(
            0,
            runtime["publication_policy"][
                "t18_o37_fluid_registration_delta"
            ],
        )
        self.assertTrue(all(
            value
            for key, value in load["runtime_contract"].items()
            if key.endswith("_present")
        ))

    def test_committed_artifacts_are_current_and_check_is_read_only(self):
        tracked = [
            builder.OUTPUT,
            builder.FLUID_GATE,
            builder.T11_CELL_GATE,
        ]
        before = {path: digest(path) for path in tracked}
        self.assertEqual([], builder.reference_only_check())
        self.assertEqual(before, {path: digest(path) for path in tracked})
        self.assertEqual(
            self.document,
            json.loads(builder.OUTPUT.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
