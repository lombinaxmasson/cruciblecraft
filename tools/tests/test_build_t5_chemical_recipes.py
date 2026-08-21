import hashlib
import importlib.util
import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "tools/build_t5_chemical_recipes.py"
SPEC = importlib.util.spec_from_file_location(
    "build_t5_chemical_recipes",
    MODULE_PATH,
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


_PREFIX_NAMESPACES: dict[str, str] = {}


def tag_namespace(prefix: str) -> str:
    if prefix not in _PREFIX_NAMESPACES:
        definition = json.loads(
            (
                ROOT
                / "src/main/resources/data/cruciblecraft/material_prefixes"
                / f"{prefix}.json"
            ).read_text(encoding="utf-8")
        )
        _PREFIX_NAMESPACES[prefix] = definition.get(
            "tag_namespace", "cruciblecraft"
        )
    return _PREFIX_NAMESPACES[prefix]


class T5ChemicalRecipeProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads(
            MODULE.MANIFEST.read_text(encoding="utf-8")
        )
        cls.fluid_gate = json.loads(
            MODULE.FLUID_GATE.read_text(encoding="utf-8")
        )
        cls.input_hashes = {
            path.replace("\\", "/"): value
            for path, value in cls.manifest["inputs"].items()
        }
        cls.distillery_ledger = json.loads(
            MODULE.DISTILLERY_LEDGER.read_text(encoding="utf-8")
        )

    def test_pinned_source_domain_and_bounded_projection(self):
        counts = self.manifest["counts"]
        self.assertEqual(152, counts["generated_recipes"])
        self.assertEqual(
            {
                "assembler": 1,
                "autoclave": 17,
                "bath": 6,
                "centrifuge": 14,
                "compressor": 5,
                "drying": 5,
                "distillery": 1,
                "electrolyzer": 62,
                "mixer": 34,
                "smelter": 7,
            },
            counts["map_recipes"],
        )
        self.assertEqual(
            {"centrifuge": 1, "mixer": 3, "smelter": 2},
            counts["fluid_closure_map_recipes"],
        )
        self.assertEqual(6, counts["fluid_closure_recipes"])
        self.assertEqual(15, counts["chemical_fluids"])
        self.assertEqual(145, counts["terminal_dust_denominator"])
        self.assertEqual(145, counts["terminal_dust_recipes"])
        self.assertEqual(145, counts["terminal_dust_recipe_ready"])
        self.assertEqual(0, counts["terminal_dust_unresolved"])
        self.assertEqual(17, counts["source_dead_ends"])
        self.assertEqual("closure_ready", self.manifest["status"])
        self.assertEqual(
            MODULE.GT6_REVISION,
            self.manifest["policy"]["revision"],
        )

    def test_every_generated_recipe_has_source_and_registered_fluid(self):
        registered_fluids = {
            f"cruciblecraft:{entry['id']}"
            for entry in self.fluid_gate["fluids"]
        } | {
            "minecraft:water",
            "minecraft:lava",
            "cruciblecraft:creosote",
            "cruciblecraft:steam",
        }
        registered_fluids.update(
            f"cruciblecraft:molten_{document['id']}"
            for path in MODULE.MATERIAL_DIR.glob("*.json")
            if path.name != "index.json"
            for document in [
                json.loads(path.read_text(encoding="utf-8"))
            ]
            if document.get("molten_fluid")
        )
        generated = {
            row["material"]: row
            for row in self.manifest["generated"]
        }
        closure_ids = {
            row["id"] for row in self.manifest["fluid_closure_routes"]
        }
        distillery_ids = {
            row["id"] for row in self.manifest["distillery_routes"]
        }
        paths = sorted(MODULE.RECIPE_ROOT.glob("*/*.json"))
        self.assertEqual(
            self.manifest["counts"]["generated_recipes"],
            len(paths),
        )
        for path in paths:
            with self.subTest(recipe=path.relative_to(ROOT)):
                document = json.loads(path.read_text(encoding="utf-8"))
                material = path.stem
                source = generated.get(material)
                if source is not None:
                    self.assertEqual(path.parent.name, source["route"])
                    self.assertTrue(
                        source["source"]["direct_terminal_dust"],
                        material,
                    )
                    self.assertTrue(
                        any(
                            row.get("material") == material
                            and row.get("prefix")
                            in {"dust", "small_dust", "tiny_dust"}
                            for row in source["source_inputs"]["items"]
                        ),
                        material,
                    )
                    expected_tags = {
                        (
                            f"{tag_namespace(row['prefix'])}:"
                            f"{row['prefix']}s/{material}"
                        )
                        for row in source["source_inputs"]["items"]
                        if row.get("material") == material
                        and row.get("prefix")
                        in {"dust", "small_dust", "tiny_dust"}
                    }
                    self.assertTrue(
                        expected_tags
                        & {
                            ingredient["tag"]
                            for ingredient in document["item_inputs"]
                            if "tag" in ingredient
                        },
                        material,
                    )
                else:
                    self.assertIn(
                        "cruciblecraft:t5/"
                        f"{path.parent.name}/{path.stem}",
                        closure_ids | distillery_ids,
                    )
                self.assertEqual(
                    "gt6_pinned_dump_projection",
                    document["provenance"]["source_kind"],
                )
                self.assertEqual(
                    [MODULE.GT6_REVISION],
                    document["provenance"]["evidence_hashes"],
                )
                selected_source = document["provenance"][
                    "selected_source_recipe"
                ]
                source_path = selected_source.split("#", 1)[0]
                if source_path in self.input_hashes:
                    self.assertRegex(
                        self.input_hashes[source_path],
                        r"^[0-9a-f]{64}$",
                    )
                else:
                    distillery_source = next(
                        (
                            row
                            for row in self.distillery_ledger["generated"]
                            if row["source_path"] == source_path
                        ),
                        None,
                    )
                    self.assertIsNotNone(distillery_source)
                    self.assertRegex(
                        distillery_source["source_row_sha256"],
                        r"^[0-9a-f]{64}$",
                    )
                for key in ("fluid_inputs", "fluid_outputs"):
                    for stack in document.get(key, []):
                        self.assertIn(stack["id"], registered_fluids)
                        self.assertGreater(stack["amount"], 0)

    def test_six_source_driven_fluid_closure_routes_are_accepted(self):
        routes = self.manifest["fluid_closure_routes"]
        self.assertEqual(6, len(routes))
        self.assertEqual(
            {
                ("gt.recipe.centrifuge", 1283),
                ("gt.recipe.mixer", 4093),
                ("gt.recipe.mixer", 27026),
                ("gt.recipe.mixer", 35115),
                ("gt.recipe.smelter", 4808),
                ("gt.recipe.smelter", 19589),
            },
            {
                (row["source"]["map"], row["source"]["recipe_index"])
                for row in routes
            },
        )
        acceptance = self.manifest["acceptance"]["fluid_closure"]
        self.assertTrue(
            acceptance["all_previously_input_only_have_producers"]
        )
        self.assertEqual([], acceptance["missing_producers"])
        self.assertEqual(
            {
                "cruciblecraft:glue",
                "cruciblecraft:hydrochloric_acid",
                "cruciblecraft:hydrogen_fluoride",
                "cruciblecraft:molten_aluminium",
                "cruciblecraft:molten_aluminium_fluoride",
                "cruciblecraft:molten_cryolite",
            },
            set(acceptance["previously_input_only_fluids"]),
        )
        self.assertTrue(all(
            acceptance["producer_recipe_ids"][fluid]
            for fluid in acceptance["previously_input_only_fluids"]
        ))
        water = acceptance["water_distilled"]
        self.assertEqual(
            "recoverable_surplus_byproduct",
            water["classification"],
        )
        self.assertFalse(water["fake_voided"])
        self.assertGreater(
            water["terminal_output_amount"],
            water["closure_consumption_amount"],
        )
        self.assertEqual(
            water["terminal_output_amount"]
            - water["closure_consumption_amount"],
            water["recoverable_surplus_amount"],
        )
        hydrochloric_acid = json.loads(
            (
                MODULE.RECIPE_ROOT
                / "mixer/fluid_closure_hydrochloric_acid.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(
            [
                {"amount": 1000, "id": "cruciblecraft:hydrochloric_acid"},
                {"amount": 250, "id": "cruciblecraft:oxygen"},
            ],
            hydrochloric_acid["fluid_outputs"],
        )
        glue = json.loads(
            (
                MODULE.RECIPE_ROOT
                / "centrifuge/fluid_closure_glue_and_latex.json"
            ).read_text(encoding="utf-8")
        )
        self.assertEqual(
            [{"item": "minecraft:slime_ball"}],
            glue["item_inputs"],
        )

    def test_fluid_gate_is_non_placeable_and_physically_complete(self):
        self.assertEqual(1, self.fluid_gate["schema_version"])
        self.assertEqual(15, len(self.fluid_gate["fluids"]))
        ids = [entry["id"] for entry in self.fluid_gate["fluids"]]
        self.assertEqual(len(set(ids)), len(ids))
        for entry in self.fluid_gate["fluids"]:
            with self.subTest(fluid=entry["id"]):
                self.assertIn(entry["state"], {"gas", "liquid"})
                self.assertFalse(entry["world_placeable"])
                self.assertNotEqual(0, entry["density"])
                self.assertGreater(entry["viscosity"], 0)
                self.assertRegex(entry["color"], r"^#[0-9A-F]{6}$")
                self.assertEqual(
                    MODULE.GT6_REVISION,
                    entry["source"]["revision"],
                )

    def test_source_dead_ends_are_explicit_and_excluded(self):
        dead_ends = self.manifest["dead_ends"]
        self.assertEqual(17, len(dead_ends))
        self.assertTrue(all(row["reason"] for row in dead_ends))
        self.assertEqual(
            set(),
            {row["material"] for row in dead_ends}
            & {row["material"] for row in self.manifest["generated"]},
        )

    def test_full_replay_generation_is_deterministic(self):
        tracked = [MODULE.MANIFEST, MODULE.FLUID_GATE] + sorted(
            MODULE.RECIPE_ROOT.glob("*/*.json")
        )
        before = {path: digest(path) for path in tracked}
        MODULE.build()
        after = {path: digest(path) for path in tracked}
        self.assertEqual(before, after)

    def test_check_is_bidirectional_and_read_only(self):
        tracked = [MODULE.MANIFEST, MODULE.FLUID_GATE] + sorted(
            MODULE.RECIPE_ROOT.glob("*/*.json")
        )
        before = {path: digest(path) for path in tracked}
        self.assertEqual([], MODULE.reference_only_check())
        self.assertEqual(before, {path: digest(path) for path in tracked})


if __name__ == "__main__":
    unittest.main()
