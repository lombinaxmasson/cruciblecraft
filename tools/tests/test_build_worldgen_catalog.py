import json
import sys
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_worldgen_catalog as builder  # noqa: E402


class WorldgenCatalogBuilderTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        (
            cls.veins,
            cls.deposits,
            cls.files,
            cls.readiness,
        ) = builder.build_documents()

    def test_t2c_vein_ledger_is_bidirectionally_closed(self):
        expected = builder.closure_vein_materials()
        actual = {
            entry["material"]
            for vein in self.veins
            for layer in builder.vein_builder.LAYERS
            for entry in vein[layer]
        }
        self.assertEqual(129, len(expected))
        self.assertEqual(expected, actual)
        self.assertEqual(0, self.readiness["counts"]["unclassified"])

    def test_registered_ore_domain_remains_two_host_and_fully_covered(self):
        counts = self.readiness["counts"]
        self.assertEqual(137, counts["registered_ore_materials"])
        self.assertEqual(2, counts["ore_host_types"])
        self.assertEqual(274, counts["registered_ore_blocks"])
        self.assertEqual(
            137,
            len(self.readiness["all_worldgen_ore_materials"]),
        )
        self.assertEqual(
            "keep_two_hosts",
            self.readiness["host_policy"]["decision"],
        )

    def test_every_closure_declaration_generates_configured_and_placed_feature(
        self,
    ):
        configured = {
            path
            for path in self.files
            if "/worldgen/configured_feature/" in path
        }
        placed = {
            path
            for path in self.files
            if "/worldgen/placed_feature/" in path
        }
        self.assertEqual(131, len(configured))
        self.assertEqual(131, len(placed))
        self.assertEqual(263, len(self.files))
        self.assertEqual(274, self.readiness["counts"]["all_worldgen_files"])
        modifier = json.loads(self.files[
            "data/cruciblecraft/neoforge/biome_modifier/"
            "add_worldgen_catalog.json"
        ])
        self.assertEqual(131, len(modifier["features"]))

    def test_fluid_deposits_are_source_state_qualified_and_finite(self):
        self.assertEqual(
            {
                "cruciblecraft:crude_oil",
                "cruciblecraft:natural_gas",
            },
            {deposit["material"] for deposit in self.deposits},
        )
        self.assertEqual(
            {"liquid", "gas"},
            {deposit["material_state"] for deposit in self.deposits},
        )
        for deposit in self.deposits:
            self.assertGreater(deposit["min_amount_mb"], 0)
            self.assertGreaterEqual(
                deposit["max_amount_mb"],
                deposit["min_amount_mb"],
            )
            self.assertGreater(deposit["production_amount_mb"], 0)
            self.assertEqual(
                20, deposit["production_interval_ticks"]
            )
            self.assertEqual(
                1_000, deposit["accumulation_cap_mb"]
            )
            self.assertEqual(
                deposit["material_state"] == "gas",
                deposit["vent_overflow"],
            )
            configured = json.loads(self.files[
                "data/cruciblecraft/worldgen/configured_feature/"
                f"{deposit['id']}.json"
            ])
            self.assertEqual(
                builder.FLUID_FEATURE_TYPE,
                configured["type"],
            )

    def test_density_budget_matches_declared_region_math(self):
        density = self.readiness["density"]
        self.assertAlmostEqual(
            129 * 0.75 / (32**2),
            density["closure_expected_ore_veins_per_chunk"],
        )
        self.assertLess(
            density["combined_expected_ore_veins_per_chunk"],
            0.15,
        )
        self.assertEqual(
            256.0,
            density["expected_chunks_per_fluid_deposit"],
        )

    def test_uniform_geometry_is_explicitly_tracked_as_o29_debt(self):
        policy = self.readiness["geometry_policy"]
        self.assertEqual("UNIFORM_PLACEHOLDER", policy["status"])
        self.assertEqual("O-29", policy["open_item"])
        self.assertEqual("DEFERRED", policy["gt6_worldgen_import"])
        self.assertEqual(
            "single_material_all_layers",
            policy["role_material_policy"],
        )
        self.assertEqual(5, policy["defaults"]["horizontal_radius"])
        self.assertEqual(2, policy["defaults"]["vertical_radius"])

    def test_wrong_expected_ledger_material_fails_loudly(self):
        capabilities = builder.vein_builder.material_capabilities()
        expected = builder.closure_vein_materials() | {"not_a_material"}
        with mock.patch.object(
            builder,
            "closure_vein_materials",
            return_value=expected,
        ):
            with self.assertRaisesRegex(ValueError, "ledger mismatch"):
                builder.load_closure_veins(capabilities)


if __name__ == "__main__":
    unittest.main()
