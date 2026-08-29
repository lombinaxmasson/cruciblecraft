from __future__ import annotations

import json
import unittest
from pathlib import Path
from unittest import mock

from tools import build_t35_runtime_registry as builder
from tools import t27_common as common


def frozen() -> dict:
    return json.loads(builder.OUTPUT.read_text(encoding="utf-8"))


def _frozen_worldgen_sample(directory: Path) -> Path:
    files = sorted(path for path in directory.glob("*.json") if path.is_file())
    if not files:
        raise AssertionError(f"empty worldgen dir: {directory}")
    preferred = directory / "surface_rock_scatter.json"
    if preferred.is_file():
        return preferred
    return files[0]


class T35RuntimeRegistryTest(unittest.TestCase):
    def test_committed_artifact_is_stable_and_check_is_read_only(self) -> None:
        expected = frozen()
        self.assertEqual(20_553, expected["total_expected_ids"])
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_representative_id_per_category(self) -> None:
        document = frozen()
        categories = document["categories"]
        representatives = {
            "blocks": "cruciblecraft:firebrick",
            "items": "cruciblecraft:extruder_shape_plate",
            "fluids": "cruciblecraft:creosote",
            "fluid_types": "cruciblecraft:creosote",
            "block_entity_types": "cruciblecraft:firebox",
            "menu_types": "cruciblecraft:hopper",
            "recipe_types": "cruciblecraft:gt_recipe",
            "recipe_serializers": "cruciblecraft:gt_recipe",
            "recipe_maps": "cruciblecraft:assembler",
            "worldgen_features": "cruciblecraft:large_vein",
            "configured_features": "cruciblecraft:surface_rock_scatter",
            "placed_features": "cruciblecraft:surface_rock_scatter",
            "multiblock_structures": "cruciblecraft:coke_oven",
            "multiblock_plugins": "cruciblecraft:processing_host",
            "creative_tabs": "cruciblecraft:main",
            "cover_behaviors": "cruciblecraft:conveyor",
        }
        for category, ident in representatives.items():
            ids = [row["id"] for row in categories[category]]
            self.assertIn(ident, ids, msg=f"missing representative in {category}")
            sample = next(row for row in categories[category] if row["id"] == ident)
            self.assertEqual(category, sample["category"])
            self.assertTrue(sample["source"]["evidence"])

    def test_dynamic_item_catalogs_are_enumerated(self) -> None:
        document = frozen()
        item_ids = {row["id"] for row in document["categories"]["items"]}
        self.assertIn("cruciblecraft:aluminium_electrolyzer", item_ids)
        self.assertIn("cruciblecraft:extruder_shape_plate", item_ids)
        self.assertIn("cruciblecraft:tool_pattern_pickaxe", item_ids)
        self.assertIn("cruciblecraft:bolt_mold", item_ids)
        self.assertEqual(
            34,
            sum(
                ident.startswith("cruciblecraft:extruder_shape_")
                for ident in item_ids
            ),
        )
        self.assertEqual(
            12,
            sum(
                ident.startswith("cruciblecraft:tool_pattern_")
                for ident in item_ids
            ),
        )

    def test_hopper_and_machine_tier_counts(self) -> None:
        document = frozen()
        block_ids = {row["id"] for row in document["categories"]["blocks"]}
        hopper_ids = sorted(
            ident for ident in block_ids if ident.endswith("_hopper") or ident.endswith("_queue_hopper")
        )
        self.assertEqual(120, len(hopper_ids))
        self.assertIn("cruciblecraft:bronze_hopper", block_ids)
        self.assertIn("cruciblecraft:steel_centrifuge", block_ids)

    def test_runtime_catalog_projections_are_complete(self) -> None:
        document = frozen()
        categories = {
            name: {row["id"] for row in rows}
            for name, rows in document["categories"].items()
        }
        self.assertEqual(20_553, document["total_expected_ids"])
        self.assertEqual(2_055, document["counts_by_category"]["blocks"])
        self.assertEqual(17_147, document["counts_by_category"]["items"])
        self.assertEqual(316, document["counts_by_category"]["fluid_types"])
        self.assertEqual(632, document["counts_by_category"]["fluids"])
        self.assertEqual(14, document["counts_by_category"]["creative_tabs"])
        self.assertIn("cruciblecraft:aluminium_electrolyzer", categories["items"])
        self.assertIn("cruciblecraft:extruder_shape_plate", categories["items"])
        self.assertIn("cruciblecraft:tool_pattern_pickaxe", categories["items"])
        self.assertIn("cruciblecraft:bolt_mold", categories["items"])
        self.assertIn("cruciblecraft:ammonia", categories["fluid_types"])
        self.assertIn("cruciblecraft:flowing_ammonia", categories["fluids"])
        self.assertNotIn("cruciblecraft:flowing_", categories["fluids"])
        self.assertIn("cruciblecraft:cables", categories["creative_tabs"])

    def test_java_runtime_gate_schema(self) -> None:
        document = frozen()
        gate = document["java_runtime_gate"]
        self.assertEqual(1, gate["compatible_schema_version"])
        self.assertEqual("cruciblecraft", gate["namespace"])
        self.assertEqual("bidirectional_set", gate["equality_mode"])
        self.assertEqual(
            document["total_expected_ids"],
            sum(document["counts_by_category"].values()),
        )

    def test_material_inputs_are_hashed(self) -> None:
        document = frozen()
        hashes = document["currentness"]["source_hashes"]
        for material_index_path in builder.MATERIAL_INDEXES:
            material_index = json.loads(
                material_index_path.read_text(encoding="utf-8")
            )
            sample = material_index_path.parent / material_index[0]
            self.assertIn(common.relative(sample), hashes)
            self.assertEqual(
                common.sha256_file(sample),
                hashes[common.relative(sample)],
            )
        for gate_path in builder.CHEMICAL_FLUID_GATES:
            rel = common.relative(gate_path)
            self.assertIn(rel, hashes)
            self.assertEqual(common.sha256_file(gate_path), hashes[rel])

    def test_dynamic_java_catalogs_are_hashed(self) -> None:
        document = frozen()
        hashes = document["currentness"]["source_hashes"]
        for name in (
            "processing_machines",
            "material_creative_tabs",
            "extruder_shapes",
            "tool_patterns",
        ):
            path = builder.JAVA_SOURCES[name]
            rel = common.relative(path)
            self.assertIn(rel, hashes)

    def test_worldgen_source_dirs_are_hashed(self) -> None:
        document = frozen()
        hashes = document["currentness"]["source_hashes"]
        for directory in builder.DATA_SOURCES["configured_features_dirs"]:
            sample = _frozen_worldgen_sample(directory)
            rel = common.relative(sample)
            self.assertIn(rel, hashes)
            self.assertEqual(common.sha256_file(sample), hashes[rel])
        for directory in builder.DATA_SOURCES["placed_features_dirs"]:
            sample = _frozen_worldgen_sample(directory)
            rel = common.relative(sample)
            self.assertIn(rel, hashes)
            self.assertEqual(common.sha256_file(sample), hashes[rel])

    def test_generated_worldgen_ids_are_enumerated(self) -> None:
        document = frozen()
        configured = {
            row["id"] for row in document["categories"]["configured_features"]
        }
        placed = {row["id"] for row in document["categories"]["placed_features"]}
        self.assertIn("cruciblecraft:large_copper_vein", configured)
        self.assertIn("cruciblecraft:crude_oil_deposit", configured)
        self.assertIn("cruciblecraft:large_copper_vein", placed)
        self.assertEqual(137, document["counts_by_category"]["configured_features"])
        self.assertEqual(137, document["counts_by_category"]["placed_features"])

    def test_gate_fixture_matches_full_artifact(self) -> None:
        document = frozen()
        fixture = builder._gate_fixture(document)
        self.assertEqual(1, fixture["compatible_schema_version"])
        self.assertEqual("cruciblecraft", fixture["namespace"])
        self.assertEqual(
            document["total_expected_ids"],
            fixture["total_expected_ids"],
        )
        self.assertEqual(
            common.sha256_bytes(
                common.stable_json(document).encode("utf-8")
            ),
            fixture["full_artifact_sha256"],
        )
        for category in document["java_runtime_gate"]["expected_categories"]:
            expected_ids = sorted(
                row["id"] for row in document["categories"][category]
            )
            self.assertEqual(expected_ids, fixture["categories"][category])

    def test_gate_fixture_is_current_when_committed(self) -> None:
        if not builder.GATE_FIXTURE.is_file():
            self.skipTest("gate fixture not yet generated")
        document = frozen()
        expected = common.stable_json(builder._gate_fixture(document))
        actual = builder.GATE_FIXTURE.read_text(encoding="utf-8")
        self.assertEqual(expected, actual)

    def test_stale_output_is_detected(self) -> None:
        from tools.tests.support import authority_sandbox

        original = json.loads(builder.OUTPUT.read_bytes().decode("utf-8"))
        tampered = dict(original)
        tampered["total_expected_ids"] += 1
        with authority_sandbox.patch_builder_path(builder, "OUTPUT") as output:
            output.write_bytes(common.stable_json(tampered).encode("utf-8"))
            errors = builder.check()
        self.assertTrue(
            any("drifted" in error or "stale" in error for error in errors)
        )

    def test_missing_source_file_fails_closed(self) -> None:
        patched = dict(builder.DATA_SOURCES)
        patched["machine_tiers"] = builder.ROOT / "missing/machine_tiers.json"
        with mock.patch.object(builder, "DATA_SOURCES", patched):
            with self.assertRaises(FileNotFoundError):
                builder.build()


if __name__ == "__main__":
    unittest.main()
