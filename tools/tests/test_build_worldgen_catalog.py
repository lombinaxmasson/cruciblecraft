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
        cls.surface_scatter = builder.load_surface_scatter()
        cls.readiness = json.loads(
            builder.READINESS.read_text(encoding="utf-8")
        )
        cls.veins = None
        cls.deposits = None
        cls.files = None
        if builder.ROASTER_COMPACT_ACQUISITION.is_file():
            (
                cls.veins,
                cls.deposits,
                cls.files,
                cls.readiness,
            ) = builder.build_documents()

    def _require_catalog_build(self) -> None:
        if self.veins is None:
            self.skipTest("roaster overlay JSON is not in this worktree")

    def test_t2c_vein_ledger_is_bidirectionally_closed(self):
        self._require_catalog_build()
        actual = {
            entry["material"]
            for vein in self.veins
            for layer in builder.vein_builder.LAYERS
            for entry in vein[layer]
        }
        t38_document = json.loads(
            builder.T38_ACQUISITION.read_text(encoding="utf-8")
        )
        t38_materials = {
            entry["material"]
            for vein in t38_document["veins"]
            for layer in builder.vein_builder.LAYERS
            for entry in vein[layer]
        }
        expected = builder.closure_vein_materials() - (
            t38_materials - actual
        )
        self.assertEqual(129, len(expected))
        self.assertEqual(expected, actual)
        self.assertEqual(0, self.readiness["counts"]["unclassified"])

    def test_registered_ore_domain_remains_two_host_and_fully_covered(self):
        counts = self.readiness["counts"]
        self.assertEqual(147, counts["registered_ore_materials"])
        self.assertEqual(2, counts["ore_host_types"])
        self.assertEqual(294, counts["registered_ore_blocks"])
        self.assertEqual(
            147,
            len(self.readiness["all_worldgen_ore_materials"]),
        )
        self.assertEqual(
            "keep_two_hosts",
            self.readiness["host_policy"]["decision"],
        )

    def test_every_closure_declaration_generates_configured_and_placed_feature(
        self,
    ):
        self._require_catalog_build()
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
        self.assertEqual(134, len(configured))
        self.assertEqual(134, len(placed))
        self.assertEqual(269, len(self.files))
        self.assertEqual(280, self.readiness["counts"]["all_worldgen_files"])
        modifier = json.loads(self.files[
            "data/cruciblecraft/neoforge/biome_modifier/"
            "add_worldgen_catalog.json"
        ])
        self.assertEqual(134, len(modifier["features"]))

    def test_fluid_deposits_are_source_state_qualified_and_finite(self):
        self._require_catalog_build()
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
        self._require_catalog_build()
        density = self.readiness["density"]
        expected = sum(
            vein["generation_chance"] / vein["region_size_chunks"] ** 2
            for vein in self.veins
        )
        self.assertAlmostEqual(
            expected,
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

    def test_t20_geometry_is_fully_classified_without_placeholder_debt(self):
        self._require_catalog_build()
        policy = self.readiness["geometry_policy"]
        self.assertEqual("T20_CLASSIFIED", policy["status"])
        self.assertIsNone(policy["open_item"])
        self.assertEqual(
            "CLASSIFIED_WITH_EXPLICIT_POLICY",
            policy["gt6_worldgen_import"],
        )
        self.assertEqual(0, policy["placeholder"])
        self.assertEqual(0, policy["unverified"])
        self.assertEqual(2, policy["profile_version"])
        self.assertEqual(
            {"DESIGN_POLICY": 56, "SOURCE_DERIVED": 73},
            policy["fidelity_statuses"],
        )
        self.assertGreater(policy["distinct_geometry_signatures"], 1)

    def test_wrong_expected_ledger_material_fails_loudly(self):
        self._require_catalog_build()
        capabilities = builder.vein_builder.material_capabilities()
        expected = builder.closure_vein_materials() | {"not_a_material"}
        with mock.patch.object(
            builder,
            "closure_vein_materials",
            return_value=expected,
        ):
            with self.assertRaisesRegex(ValueError, "ledger mismatch"):
                builder.load_closure_veins(capabilities)

    def test_surface_scatter_declaration_matches_runtime_json(self):
        self.assertEqual("surface_rock_scatter", self.surface_scatter["id"])
        self.assertEqual(
            builder.SURFACE_FEATURE_TYPE,
            self.surface_scatter["feature_type"],
        )
        self.assertEqual(2, self.surface_scatter["amount"])
        self.assertEqual(3, self.surface_scatter["probability"])
        self.assertEqual(
            "cruciblecraft:gt_surface_rock",
            self.surface_scatter["placer"],
        )
        self.assertEqual(
            builder.surface_rocks.WORLDGEN_LOOT,
            self.surface_scatter["worldgen_loot"],
        )
        self.assertEqual(
            builder.SURFACE_OVERWORLD_BIOMES,
            self.surface_scatter["biomes"],
        )
        self.assertEqual(
            builder.SURFACE_DECORATION_STEP,
            self.surface_scatter["decoration_step"],
        )
        self.assertEqual("DESIGN_POLICY", self.surface_scatter["design_policy"])
        self.assertEqual("check_only", self.surface_scatter["audit_mode"])
        self.assertGreater(
            self.readiness["surface_scatter_rock_tag_source"][
                "rock_material_count"
            ],
            0,
        )
        configured = json.loads(
            builder.CONFIGURED_SURFACE_RUNTIME.read_text(
                encoding="utf-8"
            )
        )
        placed = json.loads(
            builder.PLACED_SURFACE_RUNTIME.read_text(encoding="utf-8")
        )
        modifier = json.loads(
            builder.BIOME_MODIFIER_SURFACE_RUNTIME.read_text(
                encoding="utf-8"
            )
        )
        self.assertEqual(
            {
                "type": builder.SURFACE_FEATURE_TYPE,
                "config": {
                    "amount": 2,
                    "probability": 3,
                },
            },
            configured,
        )
        self.assertEqual(
            {
                "feature": "cruciblecraft:surface_rock_scatter",
                "placement": [],
            },
            placed,
        )
        self.assertEqual(
            {
                "type": "neoforge:add_features",
                "biomes": builder.SURFACE_OVERWORLD_BIOMES,
                "features": ["cruciblecraft:surface_rock_scatter"],
                "step": "top_layer_modification",
            },
            modifier,
        )

    def test_surface_scatter_runtime_drift_fails_closed(self):
        configured = json.loads(
            builder.CONFIGURED_SURFACE_RUNTIME.read_text(
                encoding="utf-8"
            )
        )
        configured["config"]["amount"] = 1
        with mock.patch.object(
            builder,
            "load",
            side_effect=lambda path: (
                configured
                if path == builder.CONFIGURED_SURFACE_RUNTIME
                else json.loads(path.read_text(encoding="utf-8"))
            ),
        ):
            with self.assertRaisesRegex(
                ValueError,
                "configured_feature runtime JSON drifted",
            ):
                builder.load_surface_scatter()

    def test_t20_core_counts_and_t38_additive_counts_are_current(self):
        self._require_catalog_build()
        counts = self.readiness["counts"]
        self.assertEqual(269, counts["catalog_generated_files"])
        self.assertEqual(280, counts["all_worldgen_files"])
        self.assertEqual(129, counts["closure_vein_classifications"])
        self.assertEqual(3, counts["roaster_player_path_source_backed_veins"])
        self.assertEqual(4, counts["roaster_player_path_materials"])
        self.assertIn(
            "surface_scatter_declarations",
            self.readiness["inputs"],
        )
        self.assertIn("surface_scatter", self.readiness)


if __name__ == "__main__":
    unittest.main()
