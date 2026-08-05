import hashlib
import importlib.util
import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "tools/build_t5_distillery_projection.py"
SPEC = importlib.util.spec_from_file_location(
    "build_t5_distillery_projection",
    MODULE_PATH,
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T5DistilleryProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ledger = json.loads(
            MODULE.LEDGER.read_text(encoding="utf-8")
        )
        cls.source = json.loads(
            MODULE.SOURCE_MAP.read_text(encoding="utf-8")
        )

    def test_all_1517_rows_are_classified_by_defined_categories(self):
        counts = self.ledger["counts"]
        self.assertEqual(1_517, counts["source_rows"])
        self.assertEqual(1_517, counts["classified"])
        self.assertEqual(0, counts["unclassified"])
        self.assertEqual(1, counts["projectable"])
        self.assertEqual(1_516, counts["missing_identity"])
        self.assertEqual(0, counts["out_of_scope"])
        self.assertEqual(
            {
                "missing_identity",
                "out_of_scope",
                "projectable",
            },
            set(self.ledger["classification_definitions"]),
        )
        self.assertEqual(1_517, len(self.ledger["rows"]))
        self.assertEqual(
            set(range(1_517)),
            {row["recipe_index"] for row in self.ledger["rows"]},
        )
        self.assertTrue(all(
            row["classification"]
            in self.ledger["classification_definitions"]
            and row["reason"]
            and row["reason_code"]
            for row in self.ledger["rows"]
        ))

    def test_source_and_every_row_hash_are_pinned(self):
        source = self.ledger["source"]
        self.assertEqual(MODULE.GT6_REVISION, source["revision"])
        self.assertEqual(1_517, source["recipe_count"])
        self.assertEqual(digest(MODULE.SOURCE_MAP), source["sha256"])
        self.assertEqual(1_517, self.source["recipeCount"])
        for row, recipe in zip(
            self.ledger["rows"],
            self.source["recipes"],
            strict=True,
        ):
            with self.subTest(recipe_index=row["recipe_index"]):
                self.assertEqual(
                    MODULE.canonical_hash(recipe),
                    row["source"]["row_sha256"],
                )
                self.assertEqual(
                    "gt6_dump/gt6_recipe_dump/maps/"
                    "gt.recipe.distillery.json"
                    f"#recipes[{row['recipe_index']}]",
                    row["source"]["path"],
                )

    def test_every_and_only_projectable_row_is_generated(self):
        projectable = [
            row
            for row in self.ledger["rows"]
            if row["classification"] == "projectable"
        ]
        self.assertEqual([1_138], [
            row["recipe_index"] for row in projectable
        ])
        self.assertEqual(1, self.ledger["counts"]["generated_recipes"])
        self.assertEqual(
            [
                "cruciblecraft:t5/distillery/"
                "water_to_water_distilled"
            ],
            [row["id"] for row in self.ledger["generated"]],
        )
        row = projectable[0]
        self.assertEqual(
            ["builtin_fluid_root"],
            row["closure"]["fluid_input_producers"]["minecraft:water"],
        )
        self.assertIn(
            "cruciblecraft:t5/mixer/fluid_closure_hydrochloric_acid",
            row["closure"]["fluid_output_consumers"][
                "cruciblecraft:water_distilled"
            ],
        )
        resource = (
            MODULE.DISTILLERY_ROOT
            / "water_to_water_distilled.json"
        )
        recipe = json.loads(resource.read_text(encoding="utf-8"))
        self.assertEqual("cruciblecraft:distillery", recipe["map"])
        self.assertEqual(
            [{"amount": 10, "id": "minecraft:water"}],
            recipe["fluid_inputs"],
        )
        self.assertEqual(
            [{"amount": 8, "id": "cruciblecraft:water_distilled"}],
            recipe["fluid_outputs"],
        )

    def test_reason_partition_is_stable_and_identity_bounded(self):
        self.assertEqual(
            {
                "projection_fluid_mapping": 1_501,
                "projection_item_mapping": 6,
                "registered_and_internally_reachable": 1,
                "runtime_fluid_identity_unregistered": 9,
            },
            self.ledger["counts"]["reason_codes"],
        )
        manifest = json.loads(
            (
                ROOT / "tools/t5_chemical_recipe_manifest.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(1, manifest["counts"]["distillery_recipes"])
        self.assertEqual(1_517, manifest["counts"]["distillery_source_rows"])
        self.assertEqual(
            self.ledger["generated"],
            manifest["distillery_routes"],
        )
        roadmap = json.loads(
            (
                ROOT / "tools/gt6_map_roadmap.json"
            ).read_text(encoding="utf-8")
        )["maps"]
        for route in manifest["counts"]["map_recipes"]:
            row = roadmap[f"gt.recipe.{route}"]
            self.assertEqual("BOUNDED_SUBSET_PORTED", row["status"])
            self.assertEqual(
                "FULL_MAP_REPLAY_DEFERRED",
                row["deferral_category"],
            )

    def test_check_mode_is_deterministic_and_read_only(self):
        tracked = [MODULE.LEDGER] + sorted(
            MODULE.DISTILLERY_ROOT.glob("*.json")
        )
        before = {path: digest(path) for path in tracked}
        self.assertEqual([], MODULE.check())
        self.assertEqual(before, {path: digest(path) for path in tracked})


if __name__ == "__main__":
    unittest.main()
