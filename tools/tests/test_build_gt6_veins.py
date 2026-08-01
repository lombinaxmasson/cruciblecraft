import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_gt6_veins as builder  # noqa: E402


class VeinBuilderTest(unittest.TestCase):
    CAPABILITIES = {
        "copper": {
            "factual": {"ore", "raw_ore"},
            "registered": {"ore", "raw_ore"},
        },
        "tin": {
            "factual": {"ore", "raw_ore"},
            "registered": {"ore", "raw_ore"},
        },
        "ungated": {
            "factual": {"ore", "raw_ore"},
            "registered": {"raw_ore"},
        },
    }

    @staticmethod
    def source(vein_id: str, material: str = "copper", salt: int = 101):
        return {
            "schema_version": 1,
            "id": vein_id,
            "top": [{"material": material, "weight": 4}],
            "bottom": [{"material": material, "weight": 3}],
            "between": [{"material": material, "weight": 2}],
            "spread": [{"material": material, "weight": 1}],
            "min_y": -32,
            "max_y": 48,
            "horizontal_radius": builder.MAX_SAFE_HORIZONTAL_RADIUS,
            "vertical_radius": 6,
            "density": 0.2,
            "region_size_chunks": 8,
            "generation_chance": 0.5,
            "salt": salt,
            "provenance": "unit_test",
        }

    @staticmethod
    def write(root: Path, document):
        path = root / f"{document['id']}.json"
        path.write_text(json.dumps(document), encoding="utf-8")

    def test_single_author_file_generates_three_layer_wiring_without_java(self):
        with tempfile.TemporaryDirectory() as temporary:
            author_root = Path(temporary)
            self.write(author_root, self.source("large_test_vein"))
            veins, files = builder.build_documents(
                author_root,
                self.CAPABILITIES,
            )

        self.assertEqual(["large_test_vein"], [vein["id"] for vein in veins])
        configured_path = (
            "data/cruciblecraft/worldgen/configured_feature/"
            "large_test_vein.json"
        )
        placed_path = (
            "data/cruciblecraft/worldgen/placed_feature/"
            "large_test_vein.json"
        )
        modifier_path = (
            "data/cruciblecraft/neoforge/biome_modifier/"
            "add_large_veins.json"
        )
        self.assertEqual(
            {configured_path, placed_path, modifier_path},
            set(files),
        )
        configured = json.loads(files[configured_path])
        for layer in builder.LAYERS:
            self.assertEqual(
                "cruciblecraft:copper_ore",
                configured["config"][layer][0]["state"]["Name"],
            )
        self.assertEqual(
            "cruciblecraft:large_test_vein",
            json.loads(files[placed_path])["feature"],
        )
        self.assertEqual(
            ["cruciblecraft:large_test_vein"],
            json.loads(files[modifier_path])["features"],
        )

    def test_unknown_material_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            author_root = Path(temporary)
            self.write(
                author_root,
                self.source("large_invalid_vein", material="missing"),
            )
            with self.assertRaisesRegex(ValueError, "unknown material missing"):
                builder.build_documents(author_root, self.CAPABILITIES)

    def test_factual_but_unregistered_ore_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            author_root = Path(temporary)
            self.write(
                author_root,
                self.source("large_ungated_vein", material="ungated"),
            )
            with self.assertRaisesRegex(ValueError, "not gate-registered ORE"):
                builder.build_documents(author_root, self.CAPABILITIES)

    def test_horizontal_radius_accepts_23_and_rejects_24(self):
        with tempfile.TemporaryDirectory() as temporary:
            author_root = Path(temporary)
            accepted = self.source("large_safe_vein")
            self.write(author_root, accepted)
            veins, _ = builder.build_documents(author_root, self.CAPABILITIES)
            self.assertEqual(23, veins[0]["horizontal_radius"])

            rejected = self.source("large_unsafe_vein")
            rejected["horizontal_radius"] = 24
            self.write(author_root, rejected)
            with self.assertRaisesRegex(
                ValueError,
                "horizontal_radius must be between 4 and 23",
            ):
                builder.build_documents(author_root, self.CAPABILITIES)

    def test_duplicate_salt_is_rejected_across_author_files(self):
        with tempfile.TemporaryDirectory() as temporary:
            author_root = Path(temporary)
            self.write(author_root, self.source("large_first_vein", salt=404))
            self.write(
                author_root,
                self.source("large_second_vein", material="tin", salt=404),
            )
            with self.assertRaisesRegex(ValueError, "duplicate salt 404"):
                builder.build_documents(author_root, self.CAPABILITIES)

    def test_repository_sources_define_five_families_and_tungsten(self):
        veins = builder.load_veins()
        self.assertEqual(
            {
                "large_copper_vein",
                "large_tin_vein",
                "large_iron_vein",
                "large_gold_vein",
                "large_tungsten_vein",
            },
            {vein["id"] for vein in veins},
        )
        tungsten = next(
            vein for vein in veins if vein["id"] == "large_tungsten_vein"
        )
        self.assertEqual("t2_acceptance", tungsten["provenance"])
        self.assertIn(
            "tungsten",
            {
                entry["material"]
                for layer in builder.LAYERS
                for entry in tungsten[layer]
            },
        )


if __name__ == "__main__":
    unittest.main()
